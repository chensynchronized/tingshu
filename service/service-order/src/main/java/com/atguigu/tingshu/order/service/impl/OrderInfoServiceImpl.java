package com.atguigu.tingshu.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.delay.DelayMsgService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderDerate;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderDerateService;
import com.atguigu.tingshu.order.service.OrderDetailService;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.BeanUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Resource
    private UserFeignClient userFeignClient;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private AlbumFeignClient albumFeignClient;
    @Resource
    private AccountFeignClient accountFeignClient;
    @Resource
    private OrderDetailService orderDetailService;
    @Resource
    private OrderDerateService orderDerateService;
    @Autowired
    private DelayMsgService delayMsgService;

    /**
     * 处理订单结算页面数据汇总（VIP会员、专辑、声音）
     *
     * @param userId  用户ID
     * @param tradeVo 选择下单购买项目信息
     * @return
     */
    @Override
    public OrderInfoVo tradeOrderData(Long userId, TradeVo tradeVo) {
        //1.创建订单数据确认页对象OrderInfoVo
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        //1.1.设置购买项目类型
        orderInfoVo.setItemType(tradeVo.getItemType());
        //1.2.初始化金额
        BigDecimal originalAmount = new BigDecimal("0.0");
        BigDecimal derateAmount = new BigDecimal("0.0");
        BigDecimal orderAmount = new BigDecimal("0.0");

        //1.3.初始化订单明细列表和订单减免明细列表
        ArrayList<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        ArrayList<OrderDerateVo> orderDerateVoList = new ArrayList<>();
        //2.处理订单选择页数据-vip会员
        if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(tradeVo.getItemType())){
            //2.1.远程调用用户服务，查询套餐信息
            VipServiceConfig vipServiceConfig = userFeignClient.getVipServiceConfig(tradeVo.getItemId()).getData();
            Assert.notNull(vipServiceConfig, "套餐信息不存在");
            //2.2.设置三个金额
            originalAmount = vipServiceConfig.getPrice();
            orderAmount = vipServiceConfig.getDiscountPrice();
            derateAmount = originalAmount.subtract(orderAmount);
            //2.3.添加订单明细
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName(vipServiceConfig.getName());
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
            orderDetailVo.setItemPrice(vipServiceConfig.getPrice());
            orderDetailVoList.add(orderDetailVo);
            //2.4.添加订单减免明细
            OrderDerateVo orderDerateVo = new OrderDerateVo();
            orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
            orderDerateVo.setDerateAmount(derateAmount);
            orderDerateVo.setRemarks("VIP限时优惠：" + derateAmount);
            orderDerateVoList.add(orderDerateVo);
        }else if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(tradeVo.getItemType())){
            //3.处理订单选择页数据-专辑
            //3.1.远程调用用户服务，判读是否已购买
            Boolean isPaidAlbum = userFeignClient.isPaidAlbum( tradeVo.getItemId()).getData();
            if (isPaidAlbum){
                throw new GuiguException(400,"当前用户已购买该专辑");
            }
            //3.2.远程调用专辑服务，查询专辑信息
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(tradeVo.getItemId()).getData();
            Assert.notNull(albumInfo, "专辑信息不存在");
            //3.3.远程调用用户服务，查询用户信息
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVoById(userId).getData();
            Assert.notNull(userInfoVo, "用户信息不存在");
            //3.4.计算专辑价格
            originalAmount = albumInfo.getPrice();
            //3.4.1.用户为vip用户
            if (userInfoVo.getIsVip() == 1 && userInfoVo.getVipExpireTime().before(new Date())){
                BigDecimal vipDiscount = albumInfo.getVipDiscount();
                if (vipDiscount.compareTo(new BigDecimal("-1")) == 0){
                    orderAmount = originalAmount;
                }else {
                    orderAmount = originalAmount.multiply(vipDiscount).divide(new BigDecimal("10"),2, RoundingMode.HALF_UP);
                    derateAmount = originalAmount.subtract(orderAmount);
                }
            }else {
                //3.4.2.用户为普通用户
                //普通用户没有折扣
                BigDecimal discount = albumInfo.getDiscount();
                if (discount.compareTo(new BigDecimal("-1")) == 0){
                    orderAmount = originalAmount;
                }else {
                    //普通用户有折扣
                    orderAmount = originalAmount.multiply(discount).divide(new BigDecimal("10"),2, RoundingMode.HALF_UP);
                    derateAmount = originalAmount.subtract(orderAmount);
                }
            }
            //3.4.添加订单明细
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName(albumInfo.getAlbumTitle());
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemPrice(albumInfo.getPrice());
            orderDetailVoList.add(orderDetailVo);
            //3.5.添加订单减免明细
            if (derateAmount.compareTo(new BigDecimal("0.0")) != 0){
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setRemarks("购买专辑优惠：" + derateAmount);
                orderDerateVoList.add(orderDerateVo);
            }
        }else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(tradeVo.getItemType())){
            //4.处理订单选择页数据-声音
            //4.1.获取待购买的声音列表
            List<TrackInfo> waitBuyTrackList = albumFeignClient.findPaidTrackInfoList(tradeVo.getItemId(), tradeVo.getTrackCount()).getData();
            Assert.notNull(waitBuyTrackList, "没有待购买的声音");
            //4.2获取专辑信息
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(waitBuyTrackList.get(0).getAlbumId()).getData();
            Assert.notNull(albumInfo, "专辑信息不存在");
            //4.3.计算金额
            BigDecimal price = albumInfo.getPrice();
            originalAmount = price.multiply(new BigDecimal(tradeVo.getTrackCount()));
            orderAmount = originalAmount;
            //4.4.封装订单明细
            waitBuyTrackList.stream().forEach(trackInfo -> {
                OrderDetailVo orderDetailVo = new OrderDetailVo();
                orderDetailVo.setItemId(trackInfo.getId());
                orderDetailVo.setItemName(trackInfo.getTrackTitle());
                orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                orderDetailVo.setItemPrice(price);
                orderDetailVoList.add(orderDetailVo);
            });
        }
        //5.封装订单数据确认页对象OrderInfoVo
        orderInfoVo.setItemType(tradeVo.getItemType());
        orderInfoVo.setOrderAmount(orderAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);
        //6.生成交易流水号
        String key = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        String tradeNo = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(key, tradeNo, RedisConstant.ORDER_TRADE_EXPIRE, TimeUnit.SECONDS);
        orderInfoVo.setTradeNo(tradeNo);
        //7.本次结算时间戳
        orderInfoVo.setTimestamp(System.currentTimeMillis());
        //8.签名
        Map<String, Object> map = BeanUtil.beanToMap(orderInfoVo, false, true);
        String sign = SignHelper.getSign(map);
        orderInfoVo.setSign(sign);
        return orderInfoVo;
    }

    @Override
//    @GlobalTransactional(rollbackFor = Exception.class)
    public Map<String, String> submitOrder(OrderInfoVo orderInfoVo, Long userId) {
        //1.校验防止订单重复提交
        String key = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        String redisScript = "if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
        script.setScriptText(redisScript);
        script.setResultType(Boolean.class);
        Boolean result =(Boolean) redisTemplate.execute(script, Arrays.asList(key), orderInfoVo.getTradeNo());
        if (!result){
            throw new GuiguException(400, "订单流水号异常");
        }
        //2.校验签名
        Map<String, Object> map = BeanUtil.beanToMap(orderInfoVo, false, true);
        map.remove("payWay");
        SignHelper.checkSign(map);
        //3.保存订单，订单明细，优惠明细
        OrderInfo orderInfo = this.saveOrderInfo(orderInfoVo, userId);
        if (SystemConstant.ORDER_PAY_ACCOUNT.equals(orderInfoVo.getPayWay())){
            //4.扣减账户余额
            AccountDeductVo accountDeductVo = new AccountDeductVo();
            accountDeductVo.setUserId(userId);
            accountDeductVo.setOrderNo(orderInfo.getOrderNo());
            accountDeductVo.setAmount(orderInfo.getOrderAmount());
            accountDeductVo.setContent(orderInfo.getOrderTitle());
            Result checkAndDeductResult = accountFeignClient.checkAndDeduct(accountDeductVo);
            if(checkAndDeductResult.getCode() != 200){
                throw new GuiguException(ResultCodeEnum.ACCOUNT_LESS);
            }
//            //5.虚拟发货
//            UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
//            userPaidRecordVo.setUserId(userId);
//            userPaidRecordVo.setOrderNo(orderInfo.getOrderNo());
//            userPaidRecordVo.setItemType(orderInfo.getItemType());
//            List<Long> itemIdList = orderInfo.getOrderDetailList().stream().map(OrderDetail::getItemId).collect(Collectors.toList());
//            userPaidRecordVo.setItemIdList(itemIdList);
//            Result savePaidRecordResult = userFeignClient.savePaidRecord(userPaidRecordVo);
//            if (savePaidRecordResult.getCode() != 200){
//                throw new GuiguException(400, "新增购买记录异常");
//            }
            //6.修改订单状态
            orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
            this.updateById(orderInfo);
        }
        //7.封装返回结果
        Map<String, String> mapResult = new HashMap<>();
        mapResult.put("orderNo", orderInfo.getOrderNo());
        //8.发送延迟消息
        delayMsgService.sendDelayMessage(KafkaConstant.QUEUE_ORDER_CANCEL, orderInfo.getId().toString(), 30);
        return mapResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderInfo saveOrderInfo(OrderInfoVo orderInfoVo, Long userId) {
        //1.保存订单信息
        OrderInfo orderInfo = BeanUtil.copyProperties(orderInfoVo, OrderInfo.class);
        orderInfo.setUserId(userId);
        String orderNo = DateUtil.today().replace("-","") + IdUtil.getSnowflakeNextIdStr();
        orderInfo.setOrderNo(orderNo);
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_UNPAID);
        String itemType = orderInfoVo.getItemType();
        if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(itemType)){
            orderInfo.setOrderTitle(userId+"购买专辑");
        }else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(itemType)){
            orderInfo.setOrderTitle(userId+"购买声音");
        }else if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(itemType)){
            orderInfo.setOrderTitle(userId+"购买VIP会员");
        }
        orderInfoMapper.insert(orderInfo);
        //2.保存订单明细
        List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
        if (CollUtil.isNotEmpty(orderDetailVoList)){
            List<OrderDetail> orderDetailList = orderDetailVoList.stream().map(orderDetailVo -> {
                OrderDetail orderDetail = BeanUtil.copyProperties(orderDetailVo, OrderDetail.class);
                orderDetail.setOrderId(orderInfo.getId());
                return orderDetail;
            }).collect(Collectors.toList());
            orderDetailService.saveBatch(orderDetailList);
            orderInfo.setOrderDetailList(orderDetailList);
        }
        //3.保存优惠明细
        List<OrderDerateVo> orderDerateVoList = orderInfoVo.getOrderDerateVoList();
        if (CollUtil.isNotEmpty(orderDerateVoList)){
            List<OrderDerate> orderDerateList = orderDerateVoList.stream().map(orderDerateVo -> {
                OrderDerate orderDerate = BeanUtil.copyProperties(orderDerateVo, OrderDerate.class);
                orderDerate.setOrderId(orderInfo.getId());
                return orderDerate;
            }).collect(Collectors.toList());
            orderDerateService.saveBatch(orderDerateList);
            orderInfo.setOrderDerateList(orderDerateList);
        }
        return orderInfo;
    }

    @Override
    public OrderInfo getOrderInfo(String orderNo, Long userId) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = Wrappers.lambdaQuery(OrderInfo.class).eq(OrderInfo::getOrderNo, orderNo).eq(OrderInfo::getUserId, userId);
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        if(ObjectUtil.isNotEmpty(orderInfo)){
            LambdaQueryWrapper<OrderDetail> detailQueryWrapper = Wrappers.lambdaQuery(OrderDetail.class).eq(OrderDetail::getOrderId, orderInfo.getId());
            List<OrderDetail> orderDetailList = orderDetailService.list(detailQueryWrapper);
            orderInfo.setOrderDetailList(orderDetailList);
            LambdaQueryWrapper<OrderDerate> derateQueryWrapper = Wrappers.lambdaQuery(OrderDerate.class).eq(OrderDerate::getOrderId, orderInfo.getId());
            List<OrderDerate> orderDerateList = orderDerateService.list(derateQueryWrapper);
            orderInfo.setOrderDerateList(orderDerateList);
            orderInfo.setOrderStatusName(getOrderStatusName(orderInfo.getOrderStatus()));
            orderInfo.setPayWayName(getPayWayName(orderInfo.getPayWay()));
            return orderInfo;


        }
        return null;
    }

    @Override
    public Page<OrderInfo> getUserOrderByPage(Page<OrderInfo> pageParam, Long userId) {
        pageParam = orderInfoMapper.getUserOrderByPage1(pageParam,userId);
        pageParam.getRecords().forEach(orderInfo -> {
            orderInfo.setOrderStatusName(getOrderStatusName(orderInfo.getOrderStatus()));
            orderInfo.setPayWayName(getPayWayName(orderInfo.getPayWay()));
        });
        return pageParam;
    }

    @Override
    public void orderCanncal(Long valueOf) {
        OrderInfo orderInfo = orderInfoMapper.selectById(valueOf);
        if (ObjectUtil.isEmpty(orderInfo)){
            throw new GuiguException(400, "订单不存在");
        }
        if (SystemConstant.ORDER_STATUS_UNPAID.equals(orderInfo.getOrderStatus())){
            orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_CANCEL);
            orderInfoMapper.updateById(orderInfo);
        }
    }

    @Override
    public void orderPaySuccess(String orderNo) {
        //1.修改订单状态
        LambdaQueryWrapper<OrderInfo> lambdaQueryWrapper = Wrappers.lambdaQuery(OrderInfo.class).eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo orderInfo = orderInfoMapper.selectOne(lambdaQueryWrapper);
        if (SystemConstant.ORDER_STATUS_PAID.equals(orderInfo.getOrderStatus())){
            return;
        }
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
        orderInfoMapper.updateById(orderInfo);
        //2.发货
        UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
        userPaidRecordVo.setUserId(orderInfo.getUserId());
        userPaidRecordVo.setOrderNo(orderInfo.getOrderNo());
        userPaidRecordVo.setItemType(orderInfo.getItemType());
        List<Long> itemIdList = orderInfo.getOrderDetailList().stream().map(OrderDetail::getItemId).collect(Collectors.toList());
        userPaidRecordVo.setItemIdList(itemIdList);
        Result savePaidRecordResult = userFeignClient.savePaidRecord(userPaidRecordVo);
        if (savePaidRecordResult.getCode() != 200){
            throw new GuiguException(500, "新增购买记录异常");
        }
    }

    private String getOrderStatusName(String orderStatus) {
        if (SystemConstant.ORDER_STATUS_UNPAID.equals(orderStatus)) {
            return "未支付";
        } else if (SystemConstant.ORDER_STATUS_PAID.equals(orderStatus)) {
            return "已支付";
        } else if (SystemConstant.ORDER_STATUS_CANCEL.equals(orderStatus)) {
            return "取消";
        }
        return null;
    }

    /**
     * 根据支付方式编号得到支付名称
     *
     * @param payWay
     * @return
     */
    private String getPayWayName(String payWay) {
        if (SystemConstant.ORDER_PAY_WAY_WEIXIN.equals(payWay)) {
            return "微信";
        } else if (SystemConstant.ORDER_PAY_ACCOUNT.equals(payWay)) {
            return "余额";
        } else if (SystemConstant.ORDER_PAY_WAY_ALIPAY.equals(payWay)) {
            return "支付宝";
        }
        return "";
    }
}
