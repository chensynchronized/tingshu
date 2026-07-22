package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AlbumInfoService extends IService<AlbumInfo> {

    /**
     * 保存专辑方法
     * @param userId 用户ID
     * @param albumInfoVo 专辑信息VO对象
     */
    void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId);

    void saveAlbumStat(Long albumId, String albumStatType, int statNum );

    Page<AlbumListVo> findUserAlbumPage(Page<AlbumListVo> pageParam, AlbumInfoQuery albumInfoQuery, Long userId);

    void removeAlbumInfo(Long id);

    AlbumInfo getAlbumInfo(Long id);

    void updateAlbumInfo(Long id, AlbumInfoVo albumInfoVo);
}
