package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount> {

    /**
     * 初始化用户账户记录
     * @param userId 用户ID
     */
    void saveUserAccount(Long userId);
    /**
     * 新增账户变动日志
     * @param userId 用户ID
     * @param title 资金变动缘由
     * @param tradeType 交易类型
     * @param amount 变动金额
     * @param orderNo 订单编号
     */
    void saveUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo);
    /**
     * 获取当前登录用户账户可用余额
     *
     * @param userId 用户ID
     * @return
     */
    BigDecimal getAvailableAmount(Long userId);
}
