package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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

	@Override
	public Map<String, String> wxLogin(String code) {
		try{
			//1.调用微信接口服务查询openid
			WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
			String openid = sessionInfo.getOpenid();
			//2.根据openid查询用户信息
			LambdaQueryWrapper<UserInfo> queryWrapper = Wrappers.lambdaQuery(UserInfo.class).eq(UserInfo::getWxOpenId, openid);
			UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
			if (ObjectUtil.isEmpty(userInfo)){
				//2.1如果用户信息为null，表示用户为首次登录，构建userInfo对象
				userInfo = new UserInfo();
				userInfo.setWxOpenId(openid);
				userInfo.setNickname("听友" + IdUtil.getSnowflakeNextId());
				userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
				userInfo.setIsVip(0);
				userInfoMapper.insert(userInfo);
				//2.2.发送kafka消息，初始化用户账户信息
				kafkaService.sendMessage(KafkaConstant.QUEUE_USER_REGISTER, userInfo.getId().toString());
			}
			//3.基于用户信息生成token
			String token = IdUtil.fastSimpleUUID();
			String key = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
			UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
			redisTemplate.opsForValue().set(key, userInfoVo,RedisConstant.USER_LOGIN_REFRESH_KEY_TIMEOUT);
			//4.将用户token封装结果返回
			HashMap<String, String> resultMap = new HashMap<>();
			resultMap.put("token",token);
			return resultMap;
		}catch (Exception e){
			log.error("[用户服务]微信登录异常：{}", e);
			throw new RuntimeException(e);
		}
	}
}
