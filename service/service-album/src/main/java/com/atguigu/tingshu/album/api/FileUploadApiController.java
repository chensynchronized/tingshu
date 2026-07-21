package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.FileUploadService;
import com.atguigu.tingshu.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "上传管理接口")
@RestController
@RequestMapping("api/album")
public class FileUploadApiController {
    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 将文件上传到分布式存储服务MinIO中
     *
     * @param file
     * @return
     */
    @Operation(summary = "将文件上传到minio中")
    @PostMapping("fileUpload")
    public Result fileUpload(@RequestPart MultipartFile file){
        String fileUrl = fileUploadService.imageUpload(file);
        return Result.ok(fileUrl);
    }



}
