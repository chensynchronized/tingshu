package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface OrderInfoService extends IService<OrderInfo> {


    OrderInfoVo tradeOrderData(Long userId, TradeVo tradeVo);

    /**
     * 提交订单，处理余额付款情况
     * @param orderInfoVo 订单VO信息
     * @param userId 用户ID
     * @return
     */
    Map<String, String> submitOrder(OrderInfoVo orderInfoVo, Long userId);

    /**
     * 保存订单及订单商品明细优惠明细
     * @param orderInfoVo 订单信息VO对象
     * @param userId 用户ID
     * @return 保存后订单对象
     */
    OrderInfo saveOrderInfo(OrderInfoVo orderInfoVo, Long userId);

    OrderInfo getOrderInfo(String orderNo, Long userId);

    Page<OrderInfo> getUserOrderByPage(Page<OrderInfo> pageParam, Long userId);

    void orderCanncal(Long valueOf);

    void orderPaySuccess(String orderNo);

}
