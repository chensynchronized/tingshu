package com.atguigu.tingshu.account.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@SuppressWarnings({"all"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

	@Autowired
	private RechargeInfoMapper rechargeInfoMapper;

	@Override
	public RechargeInfo getRechargeInfo(String orderNo) {
		LambdaQueryWrapper<RechargeInfo> queryWrapper = Wrappers.lambdaQuery(RechargeInfo.class).eq(RechargeInfo::getOrderNo, orderNo);
		return rechargeInfoMapper.selectOne(queryWrapper);
	}

	@Override
	public Map submitRecharge(RechargeInfoVo rechargeInfoVo) {
		RechargeInfo rechargeInfo = new RechargeInfo();
		rechargeInfo.setUserId(AuthContextHolder.getUserId());
		String orderNo = "CZ" + DateUtil.today().replace("-","") + IdUtil.getSnowflakeNextIdStr();
		rechargeInfo.setOrderNo(orderNo);
		rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
		rechargeInfo.setRechargeAmount(rechargeInfoVo.getAmount());
		rechargeInfo.setPayWay(rechargeInfoVo.getPayWay());
		rechargeInfoMapper.insert(rechargeInfo);
		HashMap<String, String> result = new HashMap<>();
		result.put("orderNo",orderNo);
		return result;
	}
}
