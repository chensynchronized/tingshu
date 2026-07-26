package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {
    /**
     * 小程序端提交临时凭据code，登录（调用微信接口获取微信账号唯一标识：openId）
     * @param code 临时凭据
     * @return 对象，登录登录成功后：token
     */
    Map<String, String> wxLogin(String code);
}
