package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class ItemServiceImpl implements ItemService {

    @Resource
    private AlbumFeignClient albumFeignClient;
    @Resource
    private UserFeignClient userFeignClient;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private RedissonClient redissonClient;
    @Override
    public Map<String, Object> getItemInfo(Long albumId) {
//        //0.查询布隆过滤器
//        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
//        boolean flag = bloomFilter.contains(albumId);
//        if (!flag) {
//            throw new GuiguException(404, "访问专辑不存在");
//        }
        //1.创建concurrentHashMap储存结果
        ConcurrentHashMap<String, Object> resultMap = new ConcurrentHashMap<>();
        //2.远程调用专辑服务，查询专辑信息
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑不存在{}", albumId);
            resultMap.put("albumInfo", albumInfo);
            return albumInfo;
        }, threadPoolExecutor);
        //3.远程调用用户服务，查询用户信息
        CompletableFuture<Void> announcerCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            Long userId = albumInfo.getUserId();
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVoById(userId).getData();
            Assert.notNull(userInfoVo, "用户不存在{}", userId);
            resultMap.put("announcer", userInfoVo);
        }, threadPoolExecutor);
        //4.远程调用专辑服务，查询专辑分类信息

        CompletableFuture<Void> baseCategoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            Long category3Id = albumInfo.getCategory3Id();
            BaseCategoryView baseCategoryView = albumFeignClient.getCategoryView(category3Id).getData();
            Assert.notNull(baseCategoryView, "分类不存在{}", category3Id);
            resultMap.put("baseCategoryView", baseCategoryView);
        }, threadPoolExecutor);
        //5.远程调用专辑服务，查询专辑统计信息
        CompletableFuture.runAsync(()->{
            AlbumStatVo albumStatVo = albumFeignClient.getAlbumStatVo(albumId).getData();
            Assert.notNull(albumStatVo, "专辑统计信息不存在{}", albumId);
            resultMap.put("albumStatVo", albumStatVo);
        }, threadPoolExecutor);
        CompletableFuture.allOf(announcerCompletableFuture, baseCategoryViewCompletableFuture).join();
        return resultMap;
    }
}
