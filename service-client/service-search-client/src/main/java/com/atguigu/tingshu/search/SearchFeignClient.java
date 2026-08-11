package com.atguigu.tingshu.search;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.search.impl.SearchDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
@FeignClient(value = "search-service", fallback = SearchDegradeFeignClient.class)
public interface SearchFeignClient {

    /**
     * 更新排行榜
     * @return
     */
    @GetMapping("api/search/albumInfo/updateLatelyAlbumRanking")
    Result updateLatelyAlbumRanking();
}
