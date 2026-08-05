package com.atguigu.tingshu.user.api;

import cn.hutool.core.util.ObjectUtil;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

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

	/**
	 * 更新当前用户收听声音播放进度
	 * @param userListenProcessVo
	 * @return
	 */
	@GuiGuLogin(required = false)
	@Operation(summary = "更新当前用户收听声音播放进度")
	@PostMapping("/userListenProcess/updateListenProcess")
	public Result updateListenProcess(@RequestBody UserListenProcessVo userListenProcessVo){
		Long userId = AuthContextHolder.getUserId();
		if(ObjectUtil.isNotEmpty(userId)){
			userListenProcessService.updateListenProcess(userId, userListenProcessVo);
		}
		return Result.ok();
	}

	/**
	 * 获取当前用户上次播放专辑声音记录
	 *
	 * @return
	 */
	@GuiGuLogin
	@GetMapping("/userListenProcess/getLatelyTrack")
	public Result<Map<String, Long>> getLatelyTrack(){
		Long userId = AuthContextHolder.getUserId();
		Map<String, Long> map = userListenProcessService.getLatelyTrack(userId);
		return Result.ok(map);
	}

}

