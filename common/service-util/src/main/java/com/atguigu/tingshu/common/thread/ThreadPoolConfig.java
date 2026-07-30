package com.atguigu.tingshu.common.thread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
@Slf4j
public class ThreadPoolConfig {
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        //1.动态得到线程数
        int processors = Runtime.getRuntime().availableProcessors();
        int coreCount = processors * 2;
        //2.创建线程池对象
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                coreCount,
                coreCount,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1000),
                Executors.defaultThreadFactory(),
                (r, e) -> {
                    //r:被拒绝任务  e:线程池对象
                    //自定义拒绝策略：重试-将任务再次提交给线程执行
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                    e.submit(r);
                }

        );

        //3.线程池核心线程第一个任务提交才创建
        threadPoolExecutor.prestartCoreThread();
        return threadPoolExecutor;
    }
}
