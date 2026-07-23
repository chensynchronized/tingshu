package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VodService {
    /**
     * 将音频文件上传到腾讯云点播平台
     * @param file 音频文件
     * @return {mediaFileId:"文件唯一标识",mediaUrl:"播放地址"}
     */
    Map<String, String> uploadTrack(MultipartFile file);
    /**
     * 根据云点播平台文件唯一标识，获取音频文件详情信息
     * @param mediaFileId 文件唯一标识
     * @return
     */
    TrackMediaInfoVo getTrackMediaInfo(String mediaFileId);
    /**
     * 删除云点播平台文件
     * @param mediaFileId
     */
    void deleteTrackMedia(String mediaFileId);
}
