package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

	@Autowired
	private AlbumInfoService albumInfoService;

	/**
	 * TODO 该接口登录才能访问，目前并未实现登录无法获取用户ID
	 * 提供给内容创作者/运营人员保存专辑
	 * @param albumInfoVo
	 * @return
	 */
	@Operation(summary = "保存专辑")
	@PostMapping("/albumInfo/saveAlbumInfo")
	public Result saveAlbumInfo(@RequestBody @Validated AlbumInfoVo albumInfoVo){
		Long userId = AuthContextHolder.getUserId();
		albumInfoService.saveAlbumInfo(albumInfoVo, userId);
		return Result.ok();
	}
	/**
	 * TODO 该接口登录才能访问
	 * 查询当前用户专辑分页列表
	 * @param page 页码
	 * @param limit 页大小
	 * @param albumInfoQuery 查询条件对象
	 * @return
	 */
	@Operation(summary = "查询当前用户专辑分页列表")
	@PostMapping("/albumInfo/findUserAlbumPage/{page}/{limit}")
	public Result<Page<AlbumListVo>> findUserAlbumPage(@PathVariable Long page,
													   @PathVariable Long limit,
													   @RequestBody AlbumInfoQuery albumInfoQuery){
		Long userId = AuthContextHolder.getUserId();
		Page<AlbumListVo> pageParam = new Page<>(page, limit);
		pageParam = albumInfoService.findUserAlbumPage(pageParam, albumInfoQuery, userId);
		return Result.ok(pageParam);

	}
	/**
	 * TODO 该接口登录才能访问
	 * 根据专辑ID删除专辑
	 * @param id
	 * @return
	 */
	@Operation(summary = "根据专辑ID删除专辑")
	@DeleteMapping("/albumInfo/removeAlbumInfo/{id}")
	public Result removeAlbumInfo(@PathVariable Long id){
		albumInfoService.removeAlbumInfo(id);
		return Result.ok();
	}
	/**
	 * 根据专辑ID查询专辑信息（包含专辑标签及值）
	 * @param id
	 * @return
	 */
	@GetMapping("/albumInfo/getAlbumInfo/{id}")
	@Operation(summary = "根据专辑ID查询专辑信息（包含专辑标签及值）")
	public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id){
		AlbumInfo albumInfo = albumInfoService.getAlbumInfo(id);
		return Result.ok(albumInfo);
	}

}

