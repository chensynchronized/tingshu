package com.atguigu.tingshu.user.strategy;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserVipService;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserVipServiceMapper;
import com.atguigu.tingshu.user.mapper.VipServiceConfigMapper;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component("1003")
public class VipStrategy implements ItemTypeStrategy {
    @Resource
    private UserInfoMapper userInfoMapper;
    @Resource
    private VipServiceConfigMapper vipServiceConfigMapper;
    @Resource
    private UserVipServiceMapper userVipServiceMapper;

    @Override
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        log.info("处理购买项目类型为：VIP会员");
        //3.处理vip
        //3.1.查询vip套餐信息
        VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(userPaidRecordVo.getItemIdList().get(0));
        Assert.notNull(vipServiceConfig, "vip套餐不存在");
        Integer serviceMonth = vipServiceConfig.getServiceMonth();
        //3.2.根据用户id查询用户信息
        UserInfo userInfo = userInfoMapper.selectById(userPaidRecordVo.getUserId());
        //3.3.当前用户已经是vip
        UserVipService userVipService = new UserVipService();

        if (userInfo.getIsVip() == 1 && userInfo.getVipExpireTime().after(new Date())){
            userVipService.setStartTime(userInfo.getVipExpireTime());
            userVipService.setExpireTime(DateUtil.offsetMonth(userInfo.getVipExpireTime(), serviceMonth));
            //3.4.当前用户是普通用户
        }else {
            userVipService.setStartTime(new Date());
            userVipService.setExpireTime(DateUtil.offsetMonth(new Date(), serviceMonth));
        }
        //3.5.封装数据
        userVipService.setUserId(userPaidRecordVo.getUserId());
        userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
        userVipServiceMapper.insert(userVipService);
        //3.6.修改用户信息
        userInfo.setIsVip(1);
        userInfo.setVipExpireTime(userVipService.getExpireTime());
        userInfoMapper.updateById(userInfo);
    }
}
