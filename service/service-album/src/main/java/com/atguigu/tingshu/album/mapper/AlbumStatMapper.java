package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AlbumStatMapper extends BaseMapper<AlbumStat> {

    @Update("update album_stat set stat_num = stat_num + #{count} where album_id = #{albumId} and stat_type = #{albumStatPlay}")
    void updateAlbumStat(@Param("albumId") Long albumId, @Param("albumStatPlay") String albumStatPlay, @Param("count") Integer count);
}
