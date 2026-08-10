package com.atguigu.tingshu.payment.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.order.client.OrderFeignClient;
import com.atguigu.tingshu.payment.mapper.PaymentInfoMapper;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"all"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    @Autowired
    private AccountFeignClient accountFeignClient;
    @Autowired
    private OrderFeignClient orderFeignClient;

    @Override
    public PaymentInfo savePaymentInfo(String paymentType, String orderNo, Long userId) {

        //1.根据订单号查询交易信息
        LambdaQueryWrapper<PaymentInfo> queryWrapper = Wrappers.lambdaQuery(PaymentInfo.class).eq(PaymentInfo::getOrderNo, orderNo)
                .eq(PaymentInfo::getUserId, userId)
                .eq(PaymentInfo::getPaymentType, paymentType);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNotEmpty(paymentInfo)){
            throw new GuiguException(400,"交易信息已存在");
        }
        //2.构建交易单对象
        paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setUserId(userId);
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN);
        paymentInfo.setPaymentStatus(SystemConstant.ORDER_STATUS_UNPAID);
        if (SystemConstant.PAYMENT_TYPE_RECHARGE.equals(paymentType)){
            RechargeInfo rechargeInfo = accountFeignClient.getRechargeInfo(orderNo).getData();
            Assert.notNull(rechargeInfo, "充值信息不存在");
            if (!SystemConstant.ORDER_STATUS_UNPAID.equals(rechargeInfo.getRechargeStatus())){
                throw new GuiguException(400,"充值订单状态错误");
            }
            paymentInfo.setAmount(rechargeInfo.getRechargeAmount());
            paymentInfo.setContent(rechargeInfo.getUserId() + "充值：" + rechargeInfo.getRechargeAmount());
        }else if (SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)){
            OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderNo).getData();
            Assert.notNull(orderInfo, "订单信息不存在");
            if (!SystemConstant.ORDER_STATUS_UNPAID.equals(orderInfo.getOrderStatus())){
                throw new GuiguException(203,"订单状态错误");
            }
            paymentInfo.setAmount(orderInfo.getOrderAmount());
            paymentInfo.setContent(orderInfo.getOrderTitle());
        }
        //3.保存交易信息
        paymentInfoMapper.insert(paymentInfo);
        return paymentInfo;
    }
}
