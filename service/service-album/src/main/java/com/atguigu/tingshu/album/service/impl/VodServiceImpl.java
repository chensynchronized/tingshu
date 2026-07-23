package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
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

    /**
     * 根据云点播平台文件唯一标识，获取音频文件详情信息
     *
     * @param mediaFileId 文件唯一标识
     * @return
     */
    @Override
    public TrackMediaInfoVo getTrackMediaInfo(String mediaFileId) {
        try {
            //1.实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
            //2.实例化要请求产品的client对象,clientProfile是可选的
            VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
            //3.实例化一个请求对象,每个接口都会对应一个request对象
            DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
            String[] fileIds1 = {mediaFileId};
            //3.1 封装获取详情对应文件唯一标识
            req.setFileIds(fileIds1);

            //4.调用获取文件详情接口。返回的resp是一个DescribeMediaInfosResponse的实例，与请求对象对应
            DescribeMediaInfosResponse describeMediaInfosResponse = client.DescribeMediaInfos(req);
            //5.解析结果对象获取音频文件时长，大小，类型
            if (describeMediaInfosResponse != null) {
                MediaInfo[] mediaInfoSet = describeMediaInfosResponse.getMediaInfoSet();
                if (mediaInfoSet != null && mediaInfoSet.length > 0) {
                    TrackMediaInfoVo vo = new TrackMediaInfoVo();
                    MediaInfo mediaInfo = mediaInfoSet[0];
                    //5.1 获取媒体文件基本信息对象
                    MediaBasicInfo basicInfo = mediaInfo.getBasicInfo();
                    vo.setType(basicInfo.getType());

                    //5.2 获取媒体文件元信息对象
                    MediaMetaData metaData = mediaInfo.getMetaData();
                    vo.setDuration(metaData.getAudioDuration());
                    vo.setSize(metaData.getSize());
                    return vo;
                }
            }
        } catch (Exception e) {
            log.error("[专辑服务]获取点播平台文件：{}，详情异常：{}", mediaFileId, e);
            throw new GuiguException(400, "音频文件详情获取异常！");
        }
        return null;
    }
}
