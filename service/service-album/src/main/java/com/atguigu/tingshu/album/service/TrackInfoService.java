package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TrackInfoService extends IService<TrackInfo> {
    /**
     * 保存专辑下声音
     * @param userId 用户ID
     * @param trackInfoVo 声音信息VO对象
     * @return
     */
    void saveTrackInfo(Long userId, TrackInfoVo trackInfoVo);
    /**
     * 新增专辑统计信息
     * @param trackId 声音ID
     * @param statType 统计类型
     * @param statNum 统计数值
     */
    void saveTrackStat(Long trackId, String statType, int statNum);
    /**
     * 获取当前登录声音分页列表
     * @param pageInfo MP分页对象
     * @param trackInfoQuery 查询声音条件对象
     * @return
     */
    Page<TrackListVo> findUserTrackPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery);
    /**
     * TODO 该接口登录才可访问
     * 修改声音信息
     * @param id
     * @param trackInfoVo
     * @return
     */
    void updateTrackInfo(Long id, TrackInfoVo trackInfoVo);
    /**
     * 根据ID删除声音
     * @param id 声音ID
     * @return
     */
    void removeTrackInfo(Long id);
    /**
     * 用于小程序端专辑页面展示分页声音列表，动态根据用户展示声音付费标识
     *
     * @param pageParam MP分页对象
     * @param albumId 专辑ID
     * @param userId 用户ID
     * @return
     */
    Page<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> pageParam, Long albumId, Long userId);
    /**
     * 根据声音ID，获取声音统计信息
     * @param trackId
     * @return
     */
    TrackStatVo getTrackStatVo(Long trackId);

}
