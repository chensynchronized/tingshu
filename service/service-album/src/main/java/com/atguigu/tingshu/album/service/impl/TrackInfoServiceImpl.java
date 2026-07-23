package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
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
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
}
