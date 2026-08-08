package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserInfoApiController {

	@Autowired
	private UserInfoService userInfoService;

	/**
	 * 根据用户ID获取用户（主播）基本信息
	 *
	 * @param userId
	 * @return
	 */
	@Operation(summary = "根据用户ID获取用户（主播）基本信息")
	@GetMapping("/userInfo/getUserInfoVo/{userId}")
	public Result<UserInfoVo> getUserInfoVoById(@PathVariable Long userId) {
		UserInfoVo userInfoVo = userInfoService.getUserInfoVoById(userId);
		return Result.ok(userInfoVo);
	}
	/**
	 * 该接口提供给给专辑服务，展示声音列表动态判断付费标识
	 * 判断当前用户某一页中声音列表购买情况
	 *
	 * @param userId               用户ID
	 * @param albumId              专辑ID
	 * @param needCheckTrackIdList 待检查购买情况声音列表
	 * @return data:{声音ID：购买结果}   结果：1（已购）0（未购买）
	 */
	@Operation(summary = "判断当前用户某一页中声音列表购买情况")
	@PostMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
	public Result<Map<Long, Integer>> userIsPaidTrack(@PathVariable Long userId, @PathVariable Long albumId, @RequestBody List<Long> needCheckTrackIdList){
		Map<Long, Integer> resultMap = userInfoService.userIsPaidTrack(userId, albumId, needCheckTrackIdList);
		return Result.ok(resultMap);
	}
	/**
	 * 提供给订单服务调用，验证当前用户是否购买过专辑
	 * @param albumId
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "提供给订单服务调用，验证当前用户是否购买过专辑")
	@GetMapping("/userInfo/isPaidAlbum/{albumId}")
	public Result<Boolean> isPaidAlbum(@PathVariable Long albumId){
		Long userId = AuthContextHolder.getUserId();
		Boolean isPaidAlbum = userInfoService.isPaidAlbum(userId,albumId);
		return Result.ok(isPaidAlbum);
	}

	/**
	 * 提供给专辑服务调用，获取当前用户已购声音集合
	 *
	 * @param albumId
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "提供给专辑服务调用，获取当前用户已购声音集合")
	@GetMapping("/userInfo/findUserPaidTrackList/{albumId}")
	public Result<List<Long>> findUserPaidTrackList(@PathVariable Long albumId){
		Long userId = AuthContextHolder.getUserId();
		List<Long> userPaidTrackList = userInfoService.findUserPaidTrackList(userId, albumId);
		return Result.ok(userPaidTrackList);
	}

}

