package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.service.FileUploadService;
import com.atguigu.tingshu.common.execption.GuiguException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.atguigu.tingshu.album.service.FileUploadService;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private MinioConstantProperties minioConstantProperties;

    @Override
    public String imageUpload(MultipartFile file) {
        try{
            //1.校验文件是否为图片
            BufferedImage read = ImageIO.read(file.getInputStream());
            if (read == null){
                throw new GuiguException(400,"图片格式非法");
            }
            //2.调用文件上传接口
            String folderName = "/" + DateUtil.today() +"/";
            String fileName = IdUtil.randomUUID();
            String extName = FileUtil.extName(file.getOriginalFilename());
            String objectName = folderName + fileName + "." + extName;
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConstantProperties.getBucketName())
                            .object(objectName)
                            .stream(file.getInputStream(),file.getSize(),-1)
                            .contentType(file.getContentType())
                            .build()
            );
            return minioConstantProperties.getEndpointUrl() + "/" + minioConstantProperties.getBucketName() + "/" + objectName;
            //3.拼接文件上传成功后的在线地址
        }catch (Exception e){
            log.error("[专辑服务]上传图片文件异常：{}", e);
            throw new RuntimeException(e);
        }
    }
}
