package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.*;
import com.atguigu.tingshu.user.mapper.*;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.user.service.UserPaidTrackService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Date;
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
	@Resource
	private UserPaidTrackService userPaidTrackService;
	@Resource
	private AlbumFeignClient albumFeignClient;
	@Resource
	private VipServiceConfigMapper vipServiceConfigMapper;
	@Resource
	private UserVipServiceMapper userVipServiceMapper;

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
	/**
	 * 根据用户ID+专辑ID查询已购声音集合
	 *
	 * @param userId  用户ID
	 * @param albumId 专辑ID
	 * @return
	 */
	@Override
	public List<Long> findUserPaidTrackList(Long userId, Long albumId) {
		LambdaQueryWrapper<UserPaidTrack> lambdaQueryWrapper = Wrappers.lambdaQuery(UserPaidTrack.class)
				.eq(UserPaidTrack::getAlbumId, albumId)
				.eq(UserPaidTrack::getUserId, userId);
		List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(lambdaQueryWrapper);
		if (CollUtil.isNotEmpty(userPaidTrackList)){
			List<Long> trackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
			return trackIdList;
		}
		return null;

	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
		String itemType = userPaidRecordVo.getItemType();
		//1.判断购买项目类型-处理专辑
		if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(itemType)){
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
		}else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(itemType)){
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
		}else if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(itemType)) {
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

}
