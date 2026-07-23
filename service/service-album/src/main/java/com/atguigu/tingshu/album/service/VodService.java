package com.atguigu.tingshu.album.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VodService {
    /**
     * 将音频文件上传到腾讯云点播平台
     * @param file 音频文件
     * @return {mediaFileId:"文件唯一标识",mediaUrl:"播放地址"}
     */
    Map<String, String> uploadTrack(MultipartFile file);
}
