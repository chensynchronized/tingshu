package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {
    /**
     * 获取当前用户收听声音播放进度
     * @param userId 用户ID
     * @param trackId 声音ID
     * @return
     */
    BigDecimal getTrackBreakSecond(Long userId, Long trackId);
    /**
     * 更新当前用户收听声音播放进度
     * @param userId 用户ID
     * @param userListenProcessVo 播放进度信息
     * @return
     */
    void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo);
    /**
     * 获取用户最近一次播放记录
     *
     * @param userId
     * @return
     */
    Map<String, Long> getLatelyTrack(Long userId);

}
