package com.atguigu.tingshu.user.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class UserDegradeFeignClient implements UserFeignClient {


    @Override
    public Result<UserInfoVo> getUserInfoVoById(Long userId) {
        log.error("远程调用[用户服务]getUserInfoVoByUserId方法服务降级");
        return null;
    }
}
