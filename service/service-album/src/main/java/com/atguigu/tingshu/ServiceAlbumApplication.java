package com.atguigu.tingshu;

import com.atguigu.tingshu.common.constant.RedisConstant;
import jakarta.annotation.Resource;
import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ServiceAlbumApplication implements  CommandLineRunner{
    @Resource
    private RedissonClient redissonClient;

    public static void main(String[] args) {
        SpringApplication.run(ServiceAlbumApplication.class, args);
    }
    @Override
    public void run(String... args) throws Exception {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        bloomFilter.tryInit(500000L, 0.03);
    }

}
