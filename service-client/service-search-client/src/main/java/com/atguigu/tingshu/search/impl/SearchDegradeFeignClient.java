package com.atguigu.tingshu.search.impl;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.search.SearchFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SearchDegradeFeignClient implements SearchFeignClient {
    @Override
    public Result updateLatelyAlbumRanking() {
        return null;
    }
}
