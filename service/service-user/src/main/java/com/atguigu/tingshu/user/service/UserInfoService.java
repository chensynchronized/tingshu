package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {
    /**
     * 小程序端提交临时凭据code，登录（调用微信接口获取微信账号唯一标识：openId）
     * @param code 临时凭据
     * @return 对象，登录登录成功后：token
     */
    Map<String, String> wxLogin(String code);
    /**
     * 获取用户信息
     * @param userId
     * @return
     */
    UserInfoVo getUserInfo(Long userId);
    /**
     * 修改用户基本信息
     * @param userId
     * @param userInfoVo
     */
    void updateUser(Long userId, UserInfoVo userInfoVo);
    /**
     * 根据用户ID获取用户（主播）基本信息
     *
     * @param userId
     * @return
     */
    UserInfoVo getUserInfoVoById(Long userId);
    /**
     * 判断当前用户某一页中声音列表购买情况
     *
     * @param userId               用户ID
     * @param albumId              专辑ID
     * @param needCheckTrackIdList 待检查购买情况声音列表
     * @return data:{声音ID：购买结果}   结果：1（已购）0（未购买）
     */
    Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckTrackIdList);
    /**
     * 验证当前用户是否购买过专辑
     * @param albumId
     * @return
     */
    Boolean isPaidAlbum(Long userId, Long albumId);
    /**
     * 提供给专辑服务调用，获取当前用户已购声音集合
     *
     * @param albumId
     * @return
     */
    List<Long> findUserPaidTrackList(Long userId, Long albumId);
}
