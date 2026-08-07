package com.atguigu.tingshu.handler;

import com.atguigu.tingshu.model.CDCEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

import javax.annotation.Resource;

@Slf4j
@Component
@CanalTable("user_info") ////监听变更表
public class UserCdcHandler implements EntryHandler<CDCEntity> {
    @Resource
    private RedisTemplate redisTemplate;
    @Override
    public void update(CDCEntity before, CDCEntity after) {
        log.info("监听到数据修改,ID:{}", after.getId());
        String key = "user:info:" + after.getId();
        redisTemplate.delete(key);
    }
    @Override
    public void delete(CDCEntity cdcEntity) {
        log.info("监听到数据删除,ID:{}", cdcEntity.getId());
        String key = "user:info:" + cdcEntity.getId();
        redisTemplate.delete(key);
    }
}
