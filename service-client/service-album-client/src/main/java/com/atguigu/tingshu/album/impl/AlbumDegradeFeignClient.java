package com.atguigu.tingshu.album.impl;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AlbumDegradeFeignClient implements AlbumFeignClient {


    @Override
    public Result<AlbumInfo> getAlbumInfo(Long id) {
        log.error("[专辑模块]提供远程调用getAlbumInfo服务降级");
        return null;
    }

    @Override
    public Result<BaseCategoryView> getCategoryView(Long category3Id) {
        log.error("[专辑模块]提供远程调用getCategoryView服务降级");
        return null;
    }

    @Override
    public Result<JSONObject> getBaseCategoryListByCategory1Id(Long category1Id) {
        log.error("[专辑模块]getBaseCategoryListByCategory1Id");
        return null;
    }

    @Override
    public Result<List<BaseCategory3>> findTopBaseCategory3(Long category1Id) {
        log.error("[专辑模块]提供远程调用方法getTop7BaseCategory3服务降级");
        return null;
    }

    @Override
    public Result<AlbumStatVo> getAlbumStatVo(Long albumId) {
        log.error("[专辑模块]提供远程调用方法getAlbumStatVo服务降级");
        return null;
    }

}
