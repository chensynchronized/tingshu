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
                redisTemplate.opsForValue().set(key,data,ttl, TimeUnit.SECONDS);
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
//@SneakyThrows
//@Around("@annotation(guiGuCache)")
//public Object guiGuCacheAdvice(ProceedingJoinPoint pjp, GuiGuCache guiGuCache) {
//    try {
//        //1.优先从redis缓存中获取业务数据
//        //1.1 构建业务数据key 形式：缓存注解中前缀+方法参数
//        //1.1.1 获取注解前缀
//        String prefix = guiGuCache.prefix();
//        //1.1.2 获取执行目标方法参数
//        String paramVal = "none";
//        Object[] args = pjp.getArgs();
//        if (args != null && args.length > 0) {
//            paramVal = Arrays.asList(args).stream()
//                    .map(arg -> arg.toString())
//                    .collect(Collectors.joining(":"));
//        }
//        String dataKey = prefix + paramVal;
//
//        //1.2 查询redis缓存中业务数据
//        Object resultObject = redisTemplate.opsForValue().get(dataKey);
//        if (resultObject != null) {
//            //1.3 命中缓存直接返回即可
//            return resultObject;
//        }
//
//        //2.获取分布式锁
//        //2.1 构建锁key
//        String lockKey = dataKey + RedisConstant.CACHE_LOCK_SUFFIX;
//        //2.2 创建锁对象
//        RLock lock = redissonClient.getLock(lockKey);
//        //2.3 获取分布式锁 阻塞线程直到获取锁成功为止
//        lock.lock();
//
//        //3.执行目标方法（查询数据库业务数据）将业务数据放入缓存
//        try {
//            //3.1 再次查询一次缓存:处于阻塞等待获取线程（终将获取锁成功）避免获取锁线程再次查库，这里再查一次缓存
//            resultObject = redisTemplate.opsForValue().get(dataKey);
//            if (resultObject != null) {
//                return resultObject;
//            }
//            //3.2 未命中缓存，执行查询数据库(目标方法)
//            resultObject = pjp.proceed();
//            //3.3 将查询数据库结果放入缓存
//            long ttl = resultObject == null ? RedisConstant.ALBUM_TEMPORARY_TIMEOUT : RedisConstant.ALBUM_TIMEOUT;
//            redisTemplate.opsForValue().set(dataKey, resultObject, ttl, TimeUnit.SECONDS);
//            return resultObject;
//        } finally {
//            //4.释放锁
//            lock.unlock();
//        }
//    } catch (Throwable e) {
//        log.info("自定义缓存切面异常：{}", e);
//        //5.兜底处理方案：如果redis服务不可用，则执行查询数据库方法
//        return pjp.proceed();
//    }
//}


}
