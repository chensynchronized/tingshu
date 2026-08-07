package com.atguigu.tingshu.common.cache;

import cn.hutool.core.util.ObjectUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.atguigu.tingshu.common.constant.RedisConstant.CACHE_INFO_PREFIX;

@Slf4j
@Aspect
@Component
public class GuiGuCacheAspect {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(guiGuCache)")
    public Object guiGuCacheAdvice(ProceedingJoinPoint joinPoint, GuiGuCache guiGuCache)throws Throwable {
        try{
            //1.构建redis key
            //1.1获取注解参数
            String prefix = guiGuCache.prefix();
            //1.2获取业务方法参数
            String paramVal = "none";
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0){
                paramVal = Arrays.asList(args).stream().map(arg -> arg.toString()).collect(Collectors.joining(":"));
            }
            String key = prefix + paramVal;
            //2.先查询缓存
            Object data = redisTemplate.opsForValue().get(key);
            if (ObjectUtil.isNotEmpty(data)){
                return data;
            }
            //3.查询不到获取分布式锁
            RLock lock = redissonClient.getLock(CACHE_INFO_PREFIX + paramVal);
            try{
                //4.双检加锁
                lock.lock();
                data = redisTemplate.opsForValue().get(key);
                if (ObjectUtil.isNotEmpty(data)){
                    return data;
                }
                //5.查询数据库，存入缓存，数据库也没有则将空对象也存入缓存
                data = joinPoint.proceed();
                Long ttl = ObjectUtil.isNotEmpty(data) ? RedisConstant.ALBUM_TIMEOUT : RedisConstant.ALBUM_TEMPORARY_TIMEOUT;
                redisTemplate.opsForValue().set(key,data,ttl);
                return data;
            }finally {
                lock.unlock();
            }
        }catch (Exception e){
            log.error("自定义缓存切面异常：{}", e);
            //兜底处理方案，如果自定义切面类出现异常，直接查询数据库
            return joinPoint.proceed();
        }
    }


}
