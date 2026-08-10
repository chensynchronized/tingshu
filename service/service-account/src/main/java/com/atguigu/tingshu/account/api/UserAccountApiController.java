package com.atguigu.tingshu.account.api;


import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class UserAccountApiController {

	@Autowired
	private UserAccountService userAccountService;

	/**
	 * 获取当前登录用户账户可用余额
	 *
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "获取当前登录用户账户可用余额")
	@GetMapping("/userAccount/getAvailableAmount")
	public Result<BigDecimal> getAvailableAmount(){
		Long userId = AuthContextHolder.getUserId();
		BigDecimal availableAmount = userAccountService.getAvailableAmount(userId);
		return Result.ok(availableAmount);
	}

	/**
	 * 检查及扣减账户余额
	 *
	 * @param accountDeductVo
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "检查及扣减账户余额")
	@PostMapping("/userAccount/checkAndDeduct")
	public Result checkAndDeduct(@RequestBody AccountDeductVo accountDeductVo){
		Long userId = AuthContextHolder.getUserId();
		if (userId != null){
			accountDeductVo.setUserId(userId);
		}
		userAccountService.checkAndDeduct( accountDeductVo);
		return Result.ok();
	}

	/**
	 * 用户充值，支付成功后，充值业务处理
	 * @param orderNo
	 * @return
	 */
	@Operation(summary = "用户充值，支付成功后，充值业务处理")
	@GetMapping("/rechargeInfo/rechargePaySuccess/{orderNo}")
	public Result rechargePaySuccess(@PathVariable String orderNo){
		userAccountService.rechargePaySuccess(orderNo);
		return Result.ok();
	}

	/**
	 * 分页查询当前用户充值记录
	 *
	 * @param page
	 * @param limit
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "分页查询当前用户充值记录")
	@GetMapping("/userAccount/findUserRechargePage/{page}/{limit}")
	public Result<Page<RechargeInfo>> findUserRechargePage(@PathVariable Long page, @PathVariable Long limit){
		Page<RechargeInfo> pageParam = new Page<RechargeInfo>(page, limit);
		pageParam = userAccountService.findUserRechargePage(pageParam);
		return Result.ok(pageParam);
	}

	/**
	 * 分页查询当前用户消费记录
	 *
	 * @param page
	 * @param limit
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "分页查询当前用户消费记录")
	@GetMapping("/userAccount/findUserConsumePage/{page}/{limit}")
	public Result<Page<UserAccountDetail>> getUserConsumePage(@PathVariable int page, @PathVariable int limit){
		Page<UserAccountDetail> pageParam = new Page<UserAccountDetail>(page, limit);
		pageParam = userAccountService.getUserAccountDetailPage(pageParam);
		return Result.ok(pageParam);
	}

}

