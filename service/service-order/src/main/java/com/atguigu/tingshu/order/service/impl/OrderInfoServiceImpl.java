package com.atguigu.tingshu.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.toolkit.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        }else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(tradeVo.getItemType())){
            //4.处理订单选择页数据-声音
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
        String tradeNo = IdUtil.fastUUID();
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
}
