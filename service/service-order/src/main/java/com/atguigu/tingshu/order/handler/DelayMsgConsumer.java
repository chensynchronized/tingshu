package com.atguigu.tingshu.order.handler;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
@Slf4j
@Component
public class DelayMsgConsumer {
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderInfoService orderInfoService;

    /**
     * 项目启动后开启线程监听阻塞队列中消息
     */
    @PostConstruct
    public void orderCancal(){
        RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(KafkaConstant.QUEUE_ORDER_CANCEL);
        Executors.newSingleThreadExecutor().submit(()->{
            while (true) {
                String take = null;
                try {
                    take = blockingQueue.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (StringUtils.isNotBlank(take)) {
                    log.info("监听到延迟关单消息：{}", take);
                    //查询订单状态，关闭订单
                    orderInfoService.orderCanncal(Long.valueOf(take));
                }
            }
        });
    }
}
