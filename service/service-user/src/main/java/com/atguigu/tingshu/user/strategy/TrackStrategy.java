package com.atguigu.tingshu.user.strategy;

import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.service.UserPaidTrackService;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("1002")
@Slf4j
public class TrackStrategy implements ItemTypeStrategy {
    @Resource
    private UserPaidTrackMapper userPaidTrackMapper;
    @Resource
    private AlbumFeignClient albumFeignClient;
    @Resource
    private UserPaidTrackService userPaidTrackService;
    @Override
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        //2.处理声音
        //2.1.判断声音是否已经购买
        LambdaQueryWrapper<UserPaidTrack> lambdaQueryWrapper = Wrappers.lambdaQuery(UserPaidTrack.class)
                .eq(UserPaidTrack::getUserId, userPaidRecordVo.getUserId())
                .in(UserPaidTrack::getTrackId, userPaidRecordVo.getItemIdList());
        Long count = userPaidTrackMapper.selectCount(lambdaQueryWrapper);
        if (count > 0){
            throw new GuiguException(400,"所选声音已经购买，请重新选择");
        }
        //2.2.根据声音id查询声音信息
        TrackInfo trackInfo = albumFeignClient.getTrackInfo(userPaidRecordVo.getItemIdList().get(0)).getData();
        Assert.notNull(trackInfo, "声音不存在");
        Long albumId = trackInfo.getAlbumId();
        //2.3.未购买则保存声音购买记录
        List<UserPaidTrack> userPaidTrackList = userPaidRecordVo.getItemIdList().stream().map(id -> {
            UserPaidTrack userPaidTrack = new UserPaidTrack();
            userPaidTrack.setTrackId(id);
            userPaidTrack.setAlbumId(albumId);
            userPaidTrack.setUserId(userPaidRecordVo.getUserId());
            userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
            return userPaidTrack;
        }).collect(Collectors.toList());
        userPaidTrackService.saveBatch(userPaidTrackList);
    }
}
