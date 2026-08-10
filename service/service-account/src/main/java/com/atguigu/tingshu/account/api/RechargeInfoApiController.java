package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "充值管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class RechargeInfoApiController {

	@Autowired
	private RechargeInfoService rechargeInfoService;
	/**
	 * 根据充值订单编号查询充值记录
	 * @param orderNo
	 * @return
	 */
	@Operation(summary = "根据充值订单编号查询充值记录")
	@GetMapping("/rechargeInfo/getRechargeInfo/{orderNo}")
	public Result<RechargeInfo> getRechargeInfo(@PathVariable String orderNo){
		return Result.ok(rechargeInfoService.getRechargeInfo(orderNo));
	}
	/**
	 * 新增充值记录
	 * @param rechargeInfoVo
	 * @return {orderNo:"充值交易订单编号"}
	 */
	@Operation(summary = "新增充值记录")
	@GuiGuLogin
	@PostMapping("/rechargeInfo/submitRecharge")
	public Result<Map> submitRecharge(@RequestBody RechargeInfoVo rechargeInfoVo){
		Map result = rechargeInfoService.submitRecharge(rechargeInfoVo);
		return Result.ok(result);
	}
}

