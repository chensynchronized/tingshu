package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
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
}
