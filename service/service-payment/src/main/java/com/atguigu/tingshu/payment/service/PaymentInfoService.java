package com.atguigu.tingshu.payment.service;

import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PaymentInfoService extends IService<PaymentInfo> {
    /**
     * 保存本地交易记录
     *
     * @param paymentType 支付类型  支付类型：1301-订单 1302-充值
     * @param orderNo     订单编号
     * @param userId      用户ID
     * @return
     */
    PaymentInfo savePaymentInfo(String paymentType, String orderNo, Long userId);
}
