package com.atguigu.tingshu.account.receiver;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccountReceiver {

    @Autowired
    private UserAccountService userAccountService;

    /**
     * 监听用户注册消息；初始化账户（余额）记录
     * 考虑问题：1、是否需要幂等性处理  2、业务中是否需要事务管理
     * 幂等性：同一个消息被多次投递（生产者重发，服务器重发-网络原因），业务执行一次跟多次结果一样
     *
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_USER_REGISTER)
    public void userRegister(ConsumerRecord<String, String> record) {
        String value = record.value();
        if (StringUtils.isNotBlank(value)) {
            log.info("[账户服务]监听用户注册成功消息：{}", value);
            Long userId = Long.valueOf(value);
            userAccountService.saveUserAccount(userId);
        }
    }
}
