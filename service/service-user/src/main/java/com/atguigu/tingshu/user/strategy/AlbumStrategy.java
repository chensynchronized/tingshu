package com.atguigu.tingshu.user.strategy;

import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.stereotype.Component;

@Component("1001")
@Slf4j
public class AlbumStrategy implements ItemTypeStrategy {
    @Resource
    private UserPaidAlbumMapper userPaidAlbumMapper;
    @Override
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {

        log.info("处理购买项目类型为：专辑");
        //1.1.判断专辑是否已经购买
        LambdaQueryWrapper<UserPaidAlbum> albumLambdaQueryWrapper = Wrappers.lambdaQuery(UserPaidAlbum.class)
                .eq(UserPaidAlbum::getAlbumId, userPaidRecordVo.getItemIdList().get(0))
                .eq(UserPaidAlbum::getUserId, userPaidRecordVo.getUserId());
        Long count = userPaidAlbumMapper.selectCount(albumLambdaQueryWrapper);
        if (count > 0){
            throw new RuntimeException("专辑已购买");
        }
        //1.2.未购买则保存专辑购买记录
        UserPaidAlbum userPaidAlbum = new UserPaidAlbum();
        userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
        userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
        userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
        userPaidAlbumMapper.insert(userPaidAlbum);
    }
}
