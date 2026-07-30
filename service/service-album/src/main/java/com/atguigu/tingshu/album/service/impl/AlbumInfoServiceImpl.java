package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
	@Autowired
	private TrackInfoMapper trackInfoMapper;
	@Autowired
	private KafkaService kafkaService;

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
		albumInfo.setIsOpen("1");
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

		//5.审核通过后发送上架专辑消息到Kafka
		if ("1".equals(albumInfo.getIsOpen())) {
			kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_UPPER, albumInfo.getId().toString());
		}

	}

	@Override
	public void saveAlbumStat(Long albumId, String albumStatType, int statNum ) {
		AlbumStat albumStat = new AlbumStat();
		albumStat.setAlbumId(albumId);
		albumStat.setStatType(albumStatType);
		albumStat.setStatNum(statNum);
		albumStatMapper.insert(albumStat);
	}

	@Override
	public Page<AlbumListVo> findUserAlbumPage(Page<AlbumListVo> pageParam, AlbumInfoQuery albumInfoQuery, Long userId) {

		albumInfoQuery.setUserId(userId);
		return albumInfoMapper.findUserAlbumPage(pageParam,albumInfoQuery);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeAlbumInfo(Long id) {
		//1.判断专辑下是否有声音，如果有声音则无法删除
		LambdaQueryWrapper<TrackInfo> trackInfoLambdaQueryWrapper = Wrappers.lambdaQuery(TrackInfo.class).eq(TrackInfo::getAlbumId, id);
		long count = trackInfoMapper.selectCount(trackInfoLambdaQueryWrapper);
		if (count>0){
			throw new GuiguException(400,"专辑下有声音，无法删除");
		}
		//2.删除专辑
		albumInfoMapper.deleteById(id);
		//3.删除专辑关联的标签值
		LambdaQueryWrapper<AlbumAttributeValue> albumAttributeValueLambdaQueryWrapper = Wrappers.lambdaQuery(AlbumAttributeValue.class).eq(AlbumAttributeValue::getAlbumId, id);
		albumAttributeValueService.remove(albumAttributeValueLambdaQueryWrapper);
		//4.删除专辑关联的统计信息
		LambdaQueryWrapper<AlbumStat> albumStatLambdaQueryWrapper = Wrappers.lambdaQuery(AlbumStat.class).eq(AlbumStat::getAlbumId, id);
		albumStatMapper.delete(albumStatLambdaQueryWrapper);
		kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_LOWER, id.toString());
	}

	@Override
	public AlbumInfo getAlbumInfo(Long id) {
		AlbumInfo albumInfo = albumInfoMapper.selectById(id);
		if (ObjectUtil.isNotEmpty(albumInfo)){
			LambdaQueryWrapper<AlbumAttributeValue> wrapper = Wrappers.lambdaQuery(AlbumAttributeValue.class).eq(AlbumAttributeValue::getAlbumId, id);
			List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueService.getBaseMapper().selectList(wrapper);
			albumInfo.setAlbumAttributeValueVoList(albumAttributeValueList);
		}
		return albumInfo;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateAlbumInfo(Long id, AlbumInfoVo albumInfoVo) {
		AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
		albumInfo.setId(id);
		int row = albumInfoMapper.updateById(albumInfo);
		if (row <= 0){
			throw new GuiguException(400,"更新专辑信息失败");
		}
		if (CollUtil.isNotEmpty(albumInfo.getAlbumAttributeValueVoList())){
			albumAttributeValueService.remove(Wrappers.lambdaQuery(AlbumAttributeValue.class).eq(AlbumAttributeValue::getAlbumId, id));
			List<AlbumAttributeValue> albumAttributeValueList = albumInfo.getAlbumAttributeValueVoList().stream().map(albumAttributeValueVo -> {
				BeanUtil.copyProperties(albumAttributeValueVo, AlbumAttributeValue.class);
				albumAttributeValueVo.setAlbumId(id);
				return albumAttributeValueVo;
			}).collect(Collectors.toList());
			albumAttributeValueService.saveBatch(albumAttributeValueList);
		}
		String isOpen = albumInfo.getIsOpen();
		if ("1".equals(isOpen)){
			kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_UPPER, albumInfo.getId().toString());
		}else{
			kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_LOWER, albumInfo.getId().toString());
		}
	}

	@Override
	public List<AlbumInfo> findUserAllAlbumList(Long userId) {
		LambdaQueryWrapper<AlbumInfo> lambdaQueryWrapper = Wrappers.lambdaQuery(AlbumInfo.class)
				.eq(AlbumInfo::getUserId, userId)
				.select(AlbumInfo::getId, AlbumInfo::getAlbumTitle, AlbumInfo::getStatus)
				.last("limit 50")
				.orderByDesc(AlbumInfo::getId);
		return albumInfoMapper.selectList(lambdaQueryWrapper);

	}
}
