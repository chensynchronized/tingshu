package com.atguigu.tingshu.user.service.impl;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"all"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private RedisTemplate redisTemplate;
	@Autowired
	private KafkaService kafkaService;
	/**
	 * 获取当前用户收听声音播放进度
	 * @param userId 用户ID
	 * @param trackId 声音ID
	 * @return
	 */
	@Override
	public BigDecimal getTrackBreakSecond(Long userId, Long trackId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("trackId").is(trackId).and("userId").is(userId));
		UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class,
				MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
		if(ObjectUtil.isNotEmpty(userListenProcess)){
			return userListenProcess.getBreakSecond();
		}
		return new BigDecimal("0.0");
	}

	@Override
	public void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo) {
		//1.构建查询条件
		Query query = new Query();
		query.addCriteria(Criteria.where("userId").is(userId).and("trackId").is(userListenProcessVo.getTrackId()));
		query.limit(1);
		UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class,
				MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
		//2.如果记录不存在，则构建UserListenProcess
		if(ObjectUtil.isEmpty(userListenProcess)){
			userListenProcess = new UserListenProcess();
			userListenProcess.setUserId(userId);
			userListenProcess.setTrackId(userListenProcessVo.getTrackId());
			userListenProcess.setAlbumId(userListenProcessVo.getAlbumId());
			userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
			userListenProcess.setIsShow(1);
			userListenProcess.setCreateTime(new Date());
			userListenProcess.setUpdateTime(new Date());
		}else{
			//3.如果记录存在，则更新播放进度
			userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
			userListenProcess.setUpdateTime(new Date());
		}
		//4.保存到Mongodb中
		mongoTemplate.save(userListenProcess, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
		//5.构建播放进度统计标识
		String key = RedisConstant.USER_TRACK_REPEAT_STAT_PREFIX + userId + ":" + userListenProcessVo.getTrackId();
		long ttl = DateUtil.between(DateUtil.endOfDay(new Date()),new Date(), DateUnit.SECOND);
		Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl, TimeUnit.SECONDS);
		if (flag) {
			//5.如果是首次更新播放进度，发送消息到Kafka话题
			//5.1 构建更新声音播放进度MQVO对象
			TrackStatMqVo mqVo = new TrackStatMqVo();
			//生成业务唯一标识，消费者端（专辑服务、搜索服务）用来做幂等性处理，确保一个消息只能只被处理一次
			mqVo.setBusinessNo(IdUtil.fastSimpleUUID());
			mqVo.setAlbumId(userListenProcessVo.getAlbumId());
			mqVo.setTrackId(userListenProcessVo.getTrackId());
			mqVo.setStatType(SystemConstant.TRACK_STAT_PLAY);
			mqVo.setCount(1);
			//5.2 发送消息到更新声音统计话题中
			kafkaService.sendMessage(KafkaConstant.QUEUE_TRACK_STAT_UPDATE, JSON.toJSONString(mqVo));
		}
	}
}
