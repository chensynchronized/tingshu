package com.atguigu.tingshu.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaService.class);

    @Autowired
    private KafkaTemplate kafkaTemplate;

    public void sendMessage(String topic, String data) {
        this.sendMessage(topic, null, data);
    }

    private void sendMessage(String topic, String key, String data) {
        CompletableFuture completableFuture = kafkaTemplate.send(topic, key, data);
        completableFuture.completeAsync(()->{
            logger.info("[生产者]发送消息成功,话题：{}，key：{}，消息：{}", topic, key, data);
            return "success";
        }).exceptionallyAsync(e -> {
            logger.error("[生产者]发送消息失败,话题：{}，key：{}，消息：{}，异常原因：{}", topic, key, data, e);
            return "failure";
        });

    }
}
