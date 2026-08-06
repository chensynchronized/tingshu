package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信授权登录接口")
@RestController
@RequestMapping("/api/user/wxLogin")
@Slf4j
public class WxLoginApiController {

    @Autowired
    private UserInfoService userInfoService;


    /**
     * 小程序端提交临时凭据code，登录（调用微信接口获取微信账号唯一标识：openId）
     * @param code 临时凭据
     * @return 对象，登录登录成功后：token
     */
    @Operation(summary = "微信小程序端登录")
    @GetMapping("/wxLogin/{code}")
    public Result<Map<String, String>> wxLogin(@PathVariable("code") String code){
        Map<String, String> mapResult = userInfoService.wxLogin(code);
        return Result.ok(mapResult);
    }
    /**
     * 该接口必须才能访问
     * 获取当前登录用户信息
     *
     * @return
     */
    @GuiGuLogin
    @GetMapping("/getUserInfo")
    public Result<UserInfoVo> getUserInfo(){
        Long userId = AuthContextHolder.getUserId();
        UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
        return Result.ok(userInfoVo);
    }
    /**
     * 修改当前登录用户基本信息
     * @param userInfoVo
     * @return
     */
    @Operation(summary = "修改当前登录用户基本信息")
    @GuiGuLogin
    @PostMapping("/updateUser")
    public Result updateUser(@RequestBody UserInfoVo userInfoVo) {
        Long userId = AuthContextHolder.getUserId();
        userInfoService.updateUser(userId,userInfoVo);
        return Result.ok();
    }


}
