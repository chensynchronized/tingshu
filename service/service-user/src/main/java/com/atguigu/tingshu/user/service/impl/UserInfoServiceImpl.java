package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

	@Autowired
	private UserInfoMapper userInfoMapper;
	@Autowired
	private RedisTemplate redisTemplate;
	@Autowired
	private WxMaService wxMaService;
	@Autowired
	private KafkaService kafkaService;
	@Resource
	private UserPaidAlbumMapper userPaidAlbumMapper;
	@Resource
	private UserPaidTrackMapper userPaidTrackMapper;

	@Override
	public Map<String, String> wxLogin(String code) {
		try {
			//1.根据入参提交临时票据，调用微信获取微信账户唯一标识接口，得到微信账户openId
			WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
			if (sessionInfo != null) {
				//2.根据微信唯一标识查询用户记录
				String wxOpenId = sessionInfo.getOpenid();

				//2.1 如果查询为空-将微信OpenId跟听书项目中用户关联（新增用户记录中存储微信账户唯一标识）
				LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
				queryWrapper.eq(UserInfo::getWxOpenId, wxOpenId);
				UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
				if (userInfo == null) {
					//2.2 为首次登录用户构建用户对象，保存用户记录
					userInfo = new UserInfo();
					userInfo.setWxOpenId(wxOpenId);
					userInfo.setNickname("听友" + IdUtil.getSnowflakeNextId());
					userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
					userInfo.setIsVip(0);
					userInfoMapper.insert(userInfo);
					//2.3 发送Kafka异步消息，通知账户微服务新增账户记录
					kafkaService.sendMessage(KafkaConstant.QUEUE_USER_REGISTER, userInfo.getId().toString());
				}

				//3.基于用户记录生成Token 将用户令牌存入Redis Key:前缀+token  Value:用户信息UserInfoVo
				String token = IdUtil.fastUUID();
				String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
				//排除掉用户隐私数据
				UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
				redisTemplate.opsForValue().set(loginKey, userInfoVo, RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);

				//4.将用户token封装结果返回
				Map<String, String> mapResult = new HashMap<>();
				mapResult.put("token", token);
				return mapResult;
			}
			return null;
		} catch (Exception e) {
			log.error("[用户服务]微信登录异常：{}", e);
			throw new RuntimeException(e);
		}
	}
	/**
	 * 获取用户信息
	 * @param userId
	 * @return
	 */
	@Override
	public UserInfoVo getUserInfo(Long userId) {
		UserInfo userInfo = userInfoMapper.selectById(userId);
		UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
		return userInfoVo;
	}
	/**
	 * 修改用户基本信息（限定只能修改账户昵称、头像）
	 *
	 * @param userId
	 * @param userInfoVo
	 */
	@Override
	public void updateUser(Long userId, UserInfoVo userInfoVo) {
		UserInfo userInfo = new UserInfo();
		userInfo.setId(userId);
		userInfo.setNickname(userInfoVo.getNickname());
		userInfo.setAvatarUrl(userInfoVo.getAvatarUrl());
		userInfoMapper.updateById(userInfo);

	}

	@Override
	public UserInfoVo getUserInfoVoById(Long userId) {
		UserInfo userInfo = userInfoMapper.selectById(userId);
		UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
		return userInfoVo;

	}
	/**
	 * 判断当前用户某一页中声音列表购买情况
	 *
	 * @param userId               用户ID
	 * @param albumId              专辑ID
	 * @param needChackTrackIdList 待检查购买情况声音列表
	 * @return data:{声音ID：购买结果}   结果：1（已购）0（未购买）
	 */
	@Override
	public Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckTrackIdList) {
		//1.根据用户id查询已购买专辑信息
		LambdaQueryWrapper<UserPaidAlbum> paidAlbumLambdaQueryWrapper = Wrappers.lambdaQuery(UserPaidAlbum.class).eq(UserPaidAlbum::getUserId, userId)
				.eq(UserPaidAlbum::getAlbumId, albumId);
		Long count = userPaidAlbumMapper.selectCount(paidAlbumLambdaQueryWrapper);
		if (count>0){
			Map<Long, Integer> resultMap = needCheckTrackIdList.stream().collect(Collectors.toMap(id -> id, id -> 1));
			return resultMap;
		}
		//2.如果未购买专辑，则查询已购声音信息
		LambdaQueryWrapper<UserPaidTrack> paidTrackLambdaQueryWrapper = Wrappers.lambdaQuery(UserPaidTrack.class).eq(UserPaidTrack::getUserId, userId)
				.eq(UserPaidTrack::getAlbumId,albumId)
				.in(UserPaidTrack::getTrackId,needCheckTrackIdList);
		List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(paidTrackLambdaQueryWrapper);
		if (CollUtil.isEmpty(userPaidTrackList)){
			return needCheckTrackIdList.stream().collect(Collectors.toMap(id -> id, id -> 0));
		}
		List<Long> trackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
		HashMap<Long, Integer> resultMap = new HashMap<>();
		for (Long trackId : needCheckTrackIdList){
			if (trackIdList.contains(trackId)){
				resultMap.put(trackId,1);
			}else {
				resultMap.put(trackId,0);
			}
		}
		return resultMap;

	}

	@Override
	public Boolean isPaidAlbum(Long userId, Long albumId) {
		LambdaQueryWrapper<UserPaidAlbum> lambdaQueryWrapper = Wrappers.lambdaQuery(UserPaidAlbum.class).eq(UserPaidAlbum::getUserId, userId)
				.eq(UserPaidAlbum::getAlbumId, albumId);
		Long count = userPaidAlbumMapper.selectCount(lambdaQueryWrapper);
		return count > 0;
	}

}
