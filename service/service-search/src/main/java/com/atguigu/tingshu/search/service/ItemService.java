package com.atguigu.tingshu.search.service;

import java.util.Map;

public interface ItemService {
    /**
     * 根据专辑ID查询专辑详情相关数据
     *
     * @param albumId
     * @return
     */

    Map<String, Object> getItemInfo(Long albumId);

}
