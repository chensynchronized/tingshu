package com.atguigu.tingshu.dispatch.job;

import com.atguigu.tingshu.search.SearchFeignClient;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DispatchHandler {
    @Autowired
    private SearchFeignClient searchFeignClient;

    @XxlJob("updateHotAlbumJob")
    public void updateHotAlbumJob(){
        log.info("定时执行热门专辑更新");
        searchFeignClient.updateLatelyAlbumRanking();
    }

    @Autowired
    private UserFeignClient userFeignClient;


    /**
     * 定时执行更新会员状态
     * @return
     */
    @XxlJob("updateUserVIPStatusJob")
    public ReturnT updateUserVIPStatusJob() {
        try {
            String jobParam = XxlJobHelper.getJobParam();
            log.info("定时执行会员状态:{}", jobParam);
            userFeignClient.updateVipExpireStatus();
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            return ReturnT.FAIL;
        }
    }


    @XxlJob("shardJob")
    public void shardJob(){
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.info("总分片数：{}", shardTotal);
        log.info("分片索引：{}", shardIndex);
    }

}