package com.atguigu.tingshu.handler;

import com.atguigu.tingshu.model.CDCEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;
@Slf4j
@Component
@CanalTable("album_info") ////监听变更表
public class AlbumInfoCdcHandler implements EntryHandler<CDCEntity> {
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 监听到数据修改
     *
     * @param before 变更前数据
     * @param after  变更后数据
     */
    @Override
    public void update(CDCEntity before, CDCEntity after) {
        log.info("监听到数据修改,ID:{}", after.getId());
        String key = "album:info:" + after.getId();
        redisTemplate.delete(key);

    }

    /**
     * 监听到删除操作
     *
     * @param cdcEntity 删除前数据
     */
    @Override
    public void delete(CDCEntity cdcEntity) {
        log.info("监听到数据删除,ID:{}", cdcEntity.getId());
        String key = "album:info:" + cdcEntity.getId();
        redisTemplate.delete(key);
    }
}
