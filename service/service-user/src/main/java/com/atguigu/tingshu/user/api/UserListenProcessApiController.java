package com.atguigu.tingshu.user.api;

import cn.hutool.core.util.ObjectUtil;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserListenProcessApiController {

	@Autowired
	private UserListenProcessService userListenProcessService;

	/**
	 * 获取当前用户收听声音播放进
	 *
	 * @param trackId
	 * @return
	 */
	@GuiGuLogin(required = false)
	@Operation(summary = "获取当前用户收听声音播放进度")
	@GetMapping("/userListenProcess/getTrackBreakSecond/{trackId}")
	public Result<BigDecimal> getTrackBreakSecond(@PathVariable Long trackId){
		Long userId = AuthContextHolder.getUserId();
		if (ObjectUtil.isNotEmpty(userId)){
			BigDecimal breakSecond = userListenProcessService.getTrackBreakSecond(userId, trackId);
			return Result.ok(breakSecond);
		}
		return Result.ok();
	}

}

