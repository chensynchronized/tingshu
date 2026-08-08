package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
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

}

