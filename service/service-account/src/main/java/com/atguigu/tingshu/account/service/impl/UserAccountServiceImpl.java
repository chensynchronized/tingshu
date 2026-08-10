package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
	@Autowired
	private RechargeInfoMapper rechargeInfoMapper;

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
	/**
	 * 获取当前登录用户账户可用余额
	 *
	 * @param userId 用户ID
	 * @return
	 */
	@Override
	public BigDecimal getAvailableAmount(Long userId) {
		LambdaQueryWrapper<UserAccount> queryWrapper = Wrappers.lambdaQuery(UserAccount.class).eq(UserAccount::getUserId, userId);
		UserAccount userAccount = userAccountMapper.selectOne(queryWrapper);
		return userAccount.getAvailableAmount();

	}
	/**
	 * 检查及扣减账户余额；增加账户变动日志
	 * @param accountDeductVo
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void checkAndDeduct(AccountDeductVo accountDeductVo) {
		int count = userAccountMapper.checkAndDeduct(accountDeductVo.getUserId(),accountDeductVo.getAmount());
		if (count == 0){
			throw new GuiguException(400,"账户余额不足");
		}
		this.saveUserAccountDetail(accountDeductVo.getUserId(),accountDeductVo.getContent(), SystemConstant.ACCOUNT_TRADE_TYPE_MINUS, accountDeductVo.getAmount(), accountDeductVo.getOrderNo());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void rechargePaySuccess(String orderNo) {
		LambdaQueryWrapper<RechargeInfo> lambdaQueryWrapper = Wrappers.lambdaQuery(RechargeInfo.class).eq(RechargeInfo::getOrderNo, orderNo);
		RechargeInfo rechargeInfo = rechargeInfoMapper.selectOne(lambdaQueryWrapper);
		Assert.notNull(rechargeInfo, "充值订单不存在");
		if (SystemConstant.ORDER_STATUS_PAID.equals(rechargeInfo.getRechargeStatus())){
			log.info("订单已支付，无需重复处理");
			return;
		}
		int count = userAccountMapper.updateUserAccount(rechargeInfo.getUserId(), rechargeInfo.getRechargeAmount());
		if (count == 0){
			throw new GuiguException(500, "充值异常");
		}
		this.saveUserAccountDetail(rechargeInfo.getUserId(), "充值", SystemConstant.PAYMENT_TYPE_RECHARGE, rechargeInfo.getRechargeAmount(), orderNo);
		rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_PAID);
		rechargeInfoMapper.updateById(rechargeInfo);

	}
}
