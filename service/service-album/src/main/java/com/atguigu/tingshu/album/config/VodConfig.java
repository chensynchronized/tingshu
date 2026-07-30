package com.atguigu.tingshu.album.config;

import com.qcloud.vod.VodUploadClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VodConfig {
    @Bean
    public VodUploadClient vodUploadClient(VodConstantProperties vodConstantProperties) {
        return new VodUploadClient(vodConstantProperties.getSecretId(),vodConstantProperties.getSecretKey() );
    }
}
