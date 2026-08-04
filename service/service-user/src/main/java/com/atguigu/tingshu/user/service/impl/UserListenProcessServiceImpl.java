package com.atguigu.tingshu.user.service.impl;

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
}
