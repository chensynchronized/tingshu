package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

}

