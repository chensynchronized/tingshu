package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.vod.v20180717.VodClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;
    @Autowired
    private VodUploadClient vodUploadClient;
    /**
     * 将音频文件上传到腾讯云点播平台
     *
     * @param file 音频文件
     * @return {mediaFileId:"文件唯一标识",mediaUrl:"播放地址"}
     */
    @Override
    public Map<String, String> uploadTrack(MultipartFile file) {
        try{
            //1.将上传文件保存到临时上传目录下
            String uploadTempPath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);
            //2.构建上传对象
            VodUploadRequest vodUploadRequest = new VodUploadRequest();
            vodUploadRequest.setMediaFilePath(uploadTempPath);
            //3.上传文件
            VodUploadResponse response = vodUploadClient.upload(vodConstantProperties.getRegion(), vodUploadRequest);
            if (ObjectUtil.isNotEmpty(response)){
                HashMap<String, String> result = new HashMap<>();
                result.put("mediaFileId", response.getFileId());
                result.put("mediaUrl", response.getMediaUrl());
                return result;
            }
        }catch (Exception e){
            log.error("[专辑服务]上传音频文件到点播平台异常：文件：{}，错误信息：{}", file, e);
            throw new RuntimeException(e);
        }
        return null;
    }
}
