package com.atguigu.tingshu.user.service;

import java.math.BigDecimal;

public interface UserListenProcessService {
    /**
     * 获取当前用户收听声音播放进度
     * @param userId 用户ID
     * @param trackId 声音ID
     * @return
     */
    BigDecimal getTrackBreakSecond(Long userId, Long trackId);
}
