package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class TrackInfoApiController {

	@Autowired
	private TrackInfoService trackInfoService;

	@Autowired
	private VodService vodService;

	/**
	 * 将音频文件上传到腾讯云点播平台
	 *
	 * @param file 音频文件
	 * @return {mediaFileId:"文件唯一标识",mediaUrl:"播放地址"}
	 */

	@Operation(summary = "将音频文件上传到腾讯云点播平台")
	@PostMapping("/trackInfo/uploadTrack")
	public Result<Map<String,String>> uploadTrack(MultipartFile file) {
		Map<String, String> map = vodService.uploadTrack(file);
		return Result.ok(map);
	}
	/**
	 * TODO:该接口需要登录才能访问
	 * 保存专辑下声音
	 *
	 * @param trackInfoVo 声音信息VO对象
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "保存专辑下声音")
	@PostMapping("/trackInfo/saveTrackInfo")
	public Result saveTrackInfo(@RequestBody @Validated TrackInfoVo trackInfoVo){
		Long userId = AuthContextHolder.getUserId();
		trackInfoService.saveTrackInfo(userId,trackInfoVo);
		return Result.ok();
	}

	/**
	 * TODO 必须登录才可以访问
	 * 获取当前登录声音分页列表
	 *
	 * @param page           页码
	 * @param limit          页大小
	 * @param trackInfoQuery 查询条件
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "获取当前登录声音分页列表")
	@PostMapping("/trackInfo/findUserTrackPage/{page}/{limit}")
	public Result<Page<TrackListVo>> findUserTrackPage(@PathVariable Long page,
													   @PathVariable Long limit,
													   @RequestBody TrackInfoQuery trackInfoQuery){
		Long userId = AuthContextHolder.getUserId();
		trackInfoQuery.setUserId(userId);
		Page<TrackListVo> pageInfo = new Page<>(page, limit);
		pageInfo = trackInfoService.findUserTrackPage(pageInfo, trackInfoQuery);
		return Result.ok(pageInfo);
	}
	/**
	 * 根据声音ID查询声音信息
	 *
	 * @param id
	 * @return
	 */
	@Operation(summary = "根据声音ID查询声音信息")
	@GetMapping("/trackInfo/getTrackInfo/{id}")
	public Result<TrackInfo> getTrackInfo(@PathVariable Long id){
		TrackInfo trackInfo = trackInfoService.getById(id);
		return Result.ok(trackInfo);
	}
	/**
	 * TODO 该接口登录才可访问
	 * 修改声音信息
	 * @param id
	 * @param trackInfoVo
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "修改声音信息")
	@PutMapping("/trackInfo/updateTrackInfo/{id}")
	public Result updateTrackInfo(@PathVariable Long id,
								  @RequestBody @Validated TrackInfoVo trackInfoVo){
		trackInfoService.updateTrackInfo(id,trackInfoVo);
		return Result.ok();
	}
	/**
	 * TODO 该接口登录才可访问
	 * 根据ID删除声音
	 * @param id
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "根据ID删除声音")
	@DeleteMapping("/trackInfo/removeTrackInfo/{id}")
	public Result removeTrackInfo(@PathVariable Long id){
		trackInfoService.removeTrackInfo(id);
		return Result.ok();
	}

	/**
	 * 用于小程序端专辑页面展示分页声音列表，动态根据用户展示声音付费标识
	 *
	 * @param albumId
	 * @param page
	 * @param limit
	 * @return
	 */
	@GuiGuLogin(required = false)
	@Operation(summary = "用于小程序端专辑页面展示分页声音列表，动态根据用户展示声音付费标识")
	@GetMapping("/trackInfo/findAlbumTrackPage/{albumId}/{page}/{limit}")
	public Result<Page<AlbumTrackListVo>> findAlbumTrackPage(@PathVariable Long albumId,
													   @PathVariable Long page,
													   @PathVariable Long limit){
		Long userId = AuthContextHolder.getUserId();
		Page<AlbumTrackListVo> pageParam = new Page<>(page, limit);
		pageParam = trackInfoService.findAlbumTrackPage(pageParam, albumId, userId);
		return Result.ok(pageParam);
	}

	/**
	 * 根据声音ID，获取声音统计信息
	 * @param trackId
	 * @return
	 */
	@Operation(summary = "根据声音ID，获取声音统计信息")
	@GetMapping("/trackInfo/getTrackStatVo/{trackId}")
	public Result<TrackStatVo> getTrackStatVo(@PathVariable Long trackId){
		TrackStatVo trackStatVo = trackInfoService.getTrackStatVo(trackId);
		return Result.ok(trackStatVo);
	}

	/**
	 * 获取当前用户分集购买声音列表
	 *
	 * @param trackId 声音ID
	 * @return [{name:"本集", price:0.2, trackCount:1},{name:"后10集", price:2, trackCount:10},...,{name:"全集", price:*, trackCount:*}]
	 */
	@GuiGuLogin
	@Operation(summary = "获取当前用户分集购买声音列表")
	@GetMapping("/trackInfo/findUserTrackPaidList/{trackId}")
	public Result<List<Map<String,Object>>> findUserTrackPaidList(@PathVariable Long trackId){
		Long userId = AuthContextHolder.getUserId();
		List<Map<String,Object>> mapList = trackInfoService.findUserTrackPaidList(userId, trackId);
		return Result.ok(mapList);
	}
	/**
	 * 提供给订单服务渲染购买商品（声音）列表-查询当前用户待购买声音列表
	 *
	 * @param trackId    声音ID
	 * @param trackCount 数量
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "提供给订单服务渲染购买商品（声音）列表-查询当前用户待购买声音列表")
	@GetMapping("/trackInfo/findPaidTrackInfoList/{trackId}/{trackCount}")
	public Result<List<TrackInfo>> findPaidTrackInfoList(@PathVariable Long trackId,@PathVariable Integer trackCount){
		Long userId = AuthContextHolder.getUserId();
		List<TrackInfo> trackInfoList = trackInfoService.findPaidTrackInfoList(userId, trackId, trackCount);
		return Result.ok(trackInfoList);
	}

}

