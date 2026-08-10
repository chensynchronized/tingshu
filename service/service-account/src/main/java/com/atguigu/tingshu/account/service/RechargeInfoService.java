package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface RechargeInfoService extends IService<RechargeInfo> {

    RechargeInfo getRechargeInfo(String orderNo);
    /**
     * 新增充值记录
     * @param rechargeInfoVo
     * @return {orderNo:"充值交易订单编号"}
     */
    Map submitRecharge(RechargeInfoVo rechargeInfoVo);
}
