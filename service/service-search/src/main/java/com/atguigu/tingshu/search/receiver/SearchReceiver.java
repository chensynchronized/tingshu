package com.atguigu.tingshu.search.receiver;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SearchReceiver {
    @Autowired
    private SearchService searchService;

    /**
     * 监听专辑上架消息，完成索引库导入
     * 考虑：1.要不要进行幂等性处理（不需要）  2.是否需要进行事务管理（不需要）
     *
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_UPPER)
    public void albumUpper(ConsumerRecord<String, String> record) {
        String value = record.value();
        if (StringUtils.isNotBlank(value)) {
            log.info("[搜索服务]监听到专辑上架消息：{}", value);
            searchService.upperAlbum(Long.valueOf(value));
        }
    }


    /**
     * 监听专辑下架消息，完成索引库删除
     * 考虑：1.要不要进行幂等性处理  2.是否需要进行事务管理（不需要）
     *
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_LOWER)
    public void albumLower(ConsumerRecord<String, String> record) {
        String value = record.value();
        if (StringUtils.isNotBlank(value)) {
            log.info("[搜索服务]监听到专辑下架消息：{}", value);
            searchService.lowerAlbum(Long.valueOf(value));
        }
    }
}
