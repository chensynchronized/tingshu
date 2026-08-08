package com.atguigu.tingshu.common.delay;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DelayMsgService {
    @Resource
    private RedissonClient redissonClient;
    /**
     * 基于Redisson（Redis）实现延迟消息
     *
     * @param data      数据
     * @param queueName 延迟队列名称
     * @param ttl       延迟时间：单位s
     */
    public void sendDelayMessage(String queueName, String data, int ttl){
        try{
            RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(queueName);
            RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
            delayedQueue.offer(data, ttl, java.util.concurrent.TimeUnit.SECONDS);
        }catch (Exception e) {
            log.error("[延迟消息]发送异常：{}", data);
            throw new RuntimeException(e);
        }
    }
}
