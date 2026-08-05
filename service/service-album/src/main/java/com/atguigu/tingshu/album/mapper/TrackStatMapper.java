package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.TrackStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TrackStatMapper extends BaseMapper<TrackStat> {

    @Update("update track_stat set stat_num = stat_num + #{count} where track_id = #{trackId} and stat_type = #{statType}")
    void updateTrackStat(@Param("trackId") Long trackId, @Param("statType") String statType, @Param("count") Integer count);
}
