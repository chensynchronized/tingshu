package com.atguigu.tingshu.album.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AlbumReceiver {
    @Autowired
    private AlbumInfoService albumInfoService;


    /**
     * 监听到更新声音统计信息
     * 1.考虑消息幂等性 2.是否需要事务管理
     *
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_TRACK_STAT_UPDATE)
    public void updateTrackStat(ConsumerRecord<String, String> record) {
        String value = record.value();
        if (StringUtils.isNotBlank(value)) {
            log.info("[专辑服务]，监听到更新声音统计消息：{}", value);
            TrackStatMqVo mqVo = JSON.parseObject(value, TrackStatMqVo.class);
            albumInfoService.updateTrackStat(mqVo);
        }
    }
}
