package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

	@Autowired
	private AlbumInfoMapper albumInfoMapper;
	@Autowired
	private AlbumAttributeValueService albumAttributeValueService;
	@Autowired
	private AlbumStatMapper albumStatMapper;
	/**
	 * 保存专辑方法
	 * 1.将提交专辑VO转为PO对象，新增专辑
	 * 2.将提交专辑标签封装为专辑标签关系集合对象，进行批量保存
	 * 3.为新增专辑标签，批量保存专辑统计信息
	 *
	 * @param userId      用户ID
	 * @param albumInfoVo 专辑信息VO对象
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId) {

		//1.将AlbumInfoVo转为AlbumInfo对象
		AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
		albumInfo.setUserId(userId);
		if ("0103".equals(albumInfo.getPayType())){
			albumInfo.setTracksForFree(5);
		}
		albumInfo.setIncludeTrackCount(0);
		albumInfo.setIsFinished("0");
		albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
		albumInfoMapper.insert(albumInfo);

		//2.保存专辑标签值信息
		List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
		if (CollUtil.isNotEmpty(albumAttributeValueVoList)){
			List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueVoList.stream().map(albumAttributeValueVo -> {
						BeanUtil.copyProperties(albumAttributeValueVo, AlbumAttributeValue.class);
						albumAttributeValueVo.setAlbumId(albumInfo.getId());
						return albumAttributeValueVo;
					})
					.collect(Collectors.toList());
			albumAttributeValueService.saveBatch(albumAttributeValueList);
		}
		//3.保存专辑统计信息
		this.saveAlbumStat(albumInfo.getId(),SystemConstant.ALBUM_STAT_PLAY,0);
		this.saveAlbumStat(albumInfo.getId(),SystemConstant.ALBUM_STAT_SUBSCRIBE,0);
		this.saveAlbumStat(albumInfo.getId(),SystemConstant.ALBUM_STAT_BUY,0);
		this.saveAlbumStat(albumInfo.getId(),SystemConstant.ALBUM_STAT_COMMENT,0);

	}

	@Override
	public void saveAlbumStat(Long albumId, String albumStatType, int statNum ) {
		AlbumStat albumStat = new AlbumStat();
		albumStat.setAlbumId(albumId);
		albumStat.setStatType(albumStatType);
		albumStat.setStatNum(statNum);
		albumStatMapper.insert(albumStat);
	}
}
