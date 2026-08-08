package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.*;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

	@Autowired
	private TrackInfoMapper trackInfoMapper;
	@Autowired
	private AlbumInfoService albumInfoService;
	@Autowired
	private TrackStatMapper trackStatMapper;
	@Autowired
	private VodService vodService;
	@Autowired
	private AlbumInfoMapper albumInfoMapper;
	@Resource
	private UserFeignClient userFeignClient;
	/**
	 * 保存专辑下声音
	 * @param userId 用户ID
	 * @param trackInfoVo 声音信息VO对象
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveTrackInfo(Long userId, TrackInfoVo trackInfoVo) {
		//1.保存声音信息
		TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);
		trackInfo.setUserId(userId);
		AlbumInfo albumInfo = albumInfoService.getById(trackInfo.getAlbumId());
		trackInfo.setOrderNum(albumInfo.getIncludeTrackCount() + 1);
		trackInfo.setStatus(SystemConstant.TRACK_STATUS_PASS);
		trackInfo.setSource("1");
		TrackMediaInfoVo trackMediaInfo = vodService.getTrackMediaInfo(trackInfoVo.getMediaFileId());
		if (ObjectUtil.isNotEmpty(trackMediaInfo)){
			trackInfo.setMediaSize(trackMediaInfo.getSize());
			trackInfo.setMediaUrl(trackMediaInfo.getMediakUrl());
			trackInfo.setMediaDuration(new BigDecimal(trackMediaInfo.getDuration()));
			trackInfo.setMediaType(trackMediaInfo.getType());
		}
		int insert = trackInfoMapper.insert(trackInfo);
		if (insert <= 0){
			throw new GuiguException(400,"新增声音失败，请稍后再试");
		}
		//2.新增声音统计信息
		Long trackId = trackInfo.getId();
		this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_PLAY, 0);
		this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_COLLECT, 0);
		this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_PRAISE, 0);
		this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_COMMENT, 0);
		//3.更新专辑包含声音数量
		LambdaUpdateWrapper<AlbumInfo> updateWrapper = Wrappers.lambdaUpdate(AlbumInfo.class)
				.set(AlbumInfo::getIncludeTrackCount, albumInfo.getIncludeTrackCount() + 1)
				.eq(AlbumInfo::getId, trackInfo.getAlbumId());
		boolean update = albumInfoService.update(updateWrapper);
		if (!update){
			throw new GuiguException(400,"更新专辑包含声音数量失败，请稍后再试");
		}
	}

	@Override
	public void saveTrackStat(Long trackId, String statType, int statNum) {
		TrackStat trackStat = new TrackStat();
		trackStat.setTrackId(trackId);
		trackStat.setStatType(statType);
		trackStat.setStatNum(statNum);
		int insert = trackStatMapper.insert(trackStat);
		if (insert <= 0){
			throw new GuiguException(400,"新增声音统计信息失败，请稍后再试");
		}
	}
	/**
	 * 获取当前登录声音分页列表
	 * @param pageInfo MP分页对象
	 * @param trackInfoQuery 查询声音条件对象
	 * @return
	 */
	@Override
	public Page<TrackListVo> findUserTrackPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery) {
		return trackInfoMapper.findUserTrackPage(pageInfo, trackInfoQuery);
	}

	@Override
	public void updateTrackInfo(Long id, TrackInfoVo trackInfoVo) {
		//1.拷贝属性
		TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);
		//2.判断音频文件是否变更
		TrackInfo trackInfoOld = trackInfoMapper.selectById(id);
		if (ObjectUtil.notEqual(trackInfoOld.getMediaFileId(), trackInfoVo.getMediaFileId())){
			TrackMediaInfoVo trackMediaInfo = vodService.getTrackMediaInfo(trackInfoVo.getMediaFileId());
			if (ObjectUtil.isNotEmpty(trackMediaInfo)){
				trackInfo.setMediaSize(trackMediaInfo.getSize());
				trackInfo.setMediaUrl(trackMediaInfo.getMediakUrl());
				trackInfo.setMediaDuration(new BigDecimal(trackMediaInfo.getDuration()));
				trackInfo.setMediaType(trackMediaInfo.getType());
				vodService.deleteTrackMedia(trackInfoVo.getMediaFileId());
			}

		}
		//3.更新声音信息
		int update = trackInfoMapper.updateById(trackInfo);
		if (update <= 0){
			throw new GuiguException(400,"更新声音信息失败，请稍后再试");
		}

	}
	/**
	 * 根据ID删除声音
	 * @param id 声音ID
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeTrackInfo(Long id) {
		//1.根据声音id查询声音信息
		TrackInfo trackInfo = trackInfoMapper.selectById(id);
		//2.将比删除声音序号大的声音序号进行递减
		trackInfoMapper.updateTrackNum(trackInfo.getAlbumId(), trackInfo.getOrderNum());
		//3.删除声音
		trackInfoMapper.deleteById(id);
		//4.修改专辑包含的声音数量
		AlbumInfo albumInfo = albumInfoService.getById(trackInfo.getAlbumId());
		LambdaUpdateWrapper<AlbumInfo> updateWrapper = Wrappers.lambdaUpdate(AlbumInfo.class)
				.set(AlbumInfo::getIncludeTrackCount, albumInfo.getIncludeTrackCount() - 1)
				.eq(AlbumInfo::getId, trackInfo.getAlbumId());
		boolean update = albumInfoService.update(updateWrapper);
		if (!update){
			throw new GuiguException(400,"更新专辑包含声音数量失败，请稍后再试");
		}
		//5.删除声音统计信息

		int delete = trackStatMapper.delete(Wrappers.lambdaQuery(TrackStat.class).eq(TrackStat::getTrackId, id));
		if (delete < 4){
			throw new GuiguException(400,"删除声音统计信息失败，请稍后再试");
		}
		//6.删除声音媒体文件
		vodService.deleteTrackMedia(trackInfo.getMediaFileId());
	}
	/**
	 * 分页获取专辑下声音列表，动态根据用户情况展示声音付费标识
	 *
	 * @param userId   用户ID
	 * @param albumId  专辑ID
	 * @param pageInfo 分页对象
	 * @return
	 */
	@Override
	public Page<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> pageParam, Long albumId, Long userId) {
		//1.查询当前页专辑下声音列表，包含统计信息
		pageParam = albumInfoMapper.getAlbumTrackPage(pageParam, albumId);
		//2.根据专辑id查询专辑信息
		AlbumInfo albumInfo = albumInfoService.getById(albumId);
		Assert.notNull(albumInfo, "专辑不存在");
		String payType = albumInfo.getPayType();
		Integer tracksForFree = albumInfo.getTracksForFree();
		//3.如果用户未登录，则不显示付费标识
		if(ObjectUtil.isEmpty(userId)){
			//3.1当前页专辑下声音序号大于免费试听集数的声音，则显示付费标识
			if (SystemConstant.ALBUM_PAY_TYPE_VIPFREE.equals(payType) || SystemConstant.ALBUM_PAY_TYPE_REQUIRE.equals(payType)){
				pageParam.getRecords().stream().filter(albumTrackListVo -> {
					return albumTrackListVo.getOrderNum() > tracksForFree;
				}).forEach(albumTrackListVo -> {
					albumTrackListVo.setIsShowPaidMark(true);
				});
			}

		}else{
			//4.如果用户已经登录
			//4.1.远程调用用户服务，查询用户信息
			UserInfoVo userInfoVo = userFeignClient.getUserInfoVoById(userId).getData();
			Assert.notNull(userInfoVo, "用户不存在");
			Integer isVip = userInfoVo.getIsVip();
			Date vipExpireTime = userInfoVo.getVipExpireTime();
			Boolean isNeedCheckPayStatus = false;
			//4.2.如果专辑是vip免费
			if(SystemConstant.ALBUM_PAY_TYPE_VIPFREE.equals(payType)){
				//4.3.如果是普通用户或vip用户，但vip已过期，则需要检查付费状态
				if(isVip == 0 || (isVip == 1 && vipExpireTime.getTime() < System.currentTimeMillis())){
					isNeedCheckPayStatus = true;
				}
			}
			//4.3.如果专辑是付费
			if(SystemConstant.ALBUM_PAY_TYPE_REQUIRE.equals(payType)){
				isNeedCheckPayStatus = true;
			}
			//4.4.如果需要检查付费状态，则获取需要检查付费状态的声音列表
			if(isNeedCheckPayStatus){
				List<Long> trackIdList = pageParam.getRecords().stream().filter(albumTrackListVo -> {
					return albumTrackListVo.getOrderNum() > tracksForFree;
				}).map(AlbumTrackListVo::getTrackId).collect(Collectors.toList());
				//4.5.远程调用用户服务，查询用户付费声音列表
				Map<Long, Integer> map = userFeignClient.userIsPaidTrack(userId, albumId, trackIdList).getData();
				//4.6.根据付费状态设置付费标识
				pageParam.getRecords().forEach(albumTrackListVo -> {
					if (trackIdList.contains(albumTrackListVo.getTrackId())){
						Boolean isShowPaidMark = map.get(albumTrackListVo.getTrackId()) == 0;
						albumTrackListVo.setIsShowPaidMark(isShowPaidMark);
					}

				});

			}
		}
		return pageParam;
	}
	/**
	 * 根据声音ID，获取声音统计信息
	 * @param trackId
	 * @return
	 */
	@Override
	public TrackStatVo getTrackStatVo(Long trackId) {
		TrackStatVo trackStatVo = trackStatMapper.getTrackStatVo(trackId);
		return trackStatVo;

	}

	/**
	 * map.put("name","本集"); // 显示文本
	 * map.put("price",albumInfo.getPrice()); // 专辑声音对应的价格
	 * map.put("trackCount",1); // 记录购买集数
	 * @param userId
	 * @param trackId 声音ID
	 * @return
	 */
	@Override
	public List<Map<String, Object>> findUserTrackPaidList(Long userId, Long trackId) {
		//1.根据声音id查询专辑信息
		TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
		Assert.notNull(trackInfo, "声音不存在");
		//2.根据声音id查询可购买声音列表
		LambdaQueryWrapper<TrackInfo> queryWrapper = Wrappers.lambdaQuery(TrackInfo.class)
				.eq(TrackInfo::getAlbumId, trackInfo.getAlbumId())
				.ge(TrackInfo::getOrderNum, trackInfo.getOrderNum());
		List<TrackInfo> waitBuyTrackList = trackInfoMapper.selectList(queryWrapper);
		if (CollUtil.isEmpty(waitBuyTrackList)){
			throw new GuiguException(400,"专辑下没有符合条件的声音");
		}
		//3.根据专辑id和用户id远程调用用户服务，查询用户已购买声音id
		List<Long> userPaidTrackIdList = userFeignClient.findUserPaidTrackList(trackInfo.getAlbumId()).getData();
		if(CollUtil.isNotEmpty(userPaidTrackIdList)){
			//4.获取未购买声音id列表
			waitBuyTrackList = waitBuyTrackList.stream().filter(ti -> {
				return !userPaidTrackIdList.contains(ti.getId());
			}).collect(Collectors.toList());
		}
		//5.动态构建分级购买对象
		if (CollUtil.isEmpty(waitBuyTrackList)){
			throw new GuiguException(400,"专辑下没有未购买的声音");
		}
		ArrayList<Map<String, Object>> mapList = new ArrayList<>();
		int size = waitBuyTrackList.size();
		AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
		Assert.notNull(albumInfo, "专辑不存在");
		BigDecimal price = albumInfo.getPrice();
		Map<String, Object> currMap = new HashMap<>();
		currMap.put("name","本集");
		currMap.put("price",price );
		currMap.put("trackCount",1);
		mapList.add(currMap);

		for(int i = 10;i <= 40 ;i+=10){
			if (i < size){
				Map<String, Object> map = new HashMap<>();
				map.put("name", "后" + i + "集");
				map.put("price", price.multiply(new BigDecimal(i)));
				map.put("trackCount", i);
				mapList.add(map);
			}else {
				//反之全集（动态构建后count集合）
				Map<String, Object> map = new HashMap<>();
				map.put("name", "后" + size + "集");
				map.put("price", price.multiply(new BigDecimal(size)));
				map.put("trackCount", size);
				mapList.add(map);
				break;
			}
		}
		return mapList;

	}

	@Override
	public List<TrackInfo> findPaidTrackInfoList(Long userId, Long trackId, Integer trackCount) {
		//1.根据声音id查询声音信息
		TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
		Assert.notNull(trackInfo, "声音不存在");
		Long albumId = trackInfo.getAlbumId();
		//2.根据用户id+专辑id查询用户已购买声音列表
		List<Long> buyTrackIdList = userFeignClient.findUserPaidTrackList(albumId).getData();
		//3.查询用户待购买声音列表
		LambdaQueryWrapper<TrackInfo> queryWrapper = Wrappers.lambdaQuery(TrackInfo.class)
				.eq(TrackInfo::getAlbumId, albumId)
				.notIn(TrackInfo::getId, buyTrackIdList,CollUtil.isNotEmpty(buyTrackIdList))
				.select(TrackInfo::getId, TrackInfo::getTrackTitle, TrackInfo::getCoverUrl, TrackInfo::getAlbumId)
				.orderByAsc(TrackInfo::getOrderNum)
				.last("limit" + trackCount);
		List<TrackInfo> trackInfoList = trackInfoMapper.selectList(queryWrapper);
		if(CollUtil.isEmpty(trackInfoList)){
			throw new GuiguException(400,"没有找到符合条件的声音");
		}
		return trackInfoList;


	}
}
