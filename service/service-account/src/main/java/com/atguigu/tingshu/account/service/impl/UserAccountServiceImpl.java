package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

	@Autowired
	private UserAccountMapper userAccountMapper;
	@Autowired
	private UserAccountDetailMapper userAccountDetailMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveUserAccount(Long userId) {
		UserAccount userAccount = new UserAccount();
		userAccount.setUserId(userId);
		userAccount.setTotalAmount(new BigDecimal("100"));
		userAccount.setLockAmount(new BigDecimal("0"));
		userAccount.setAvailableAmount(new BigDecimal("100"));
		userAccount.setTotalPayAmount(new BigDecimal("0"));
		userAccount.setTotalIncomeAmount(new BigDecimal("100"));
		userAccountMapper.insert(userAccount);
		this.saveUserAccountDetail(userId,"赠送", SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT, new BigDecimal("100"), null);
		log.info("用户账户初始化成功！");


	}

	@Override
	public void saveUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo) {
		UserAccountDetail userAccountDetail = new UserAccountDetail();
		userAccountDetail.setUserId(userId);
		userAccountDetail.setTitle(title);
		userAccountDetail.setTradeType(tradeType);
		userAccountDetail.setAmount(amount);
		userAccountDetail.setOrderNo(orderNo);
		userAccountDetailMapper.insert(userAccountDetail);
	}
}
