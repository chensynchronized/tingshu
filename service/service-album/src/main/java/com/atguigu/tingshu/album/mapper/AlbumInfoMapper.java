package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AlbumInfoMapper extends BaseMapper<AlbumInfo> {

    Page<AlbumListVo> findUserAlbumPage(Page<AlbumListVo> pageParam, @Param("albumInfoQuery") AlbumInfoQuery albumInfoQuery);
    /**
     * 根据专辑ID查询专辑统计信息
     *
     * @param albumId
     * @return
     */
    AlbumStatVo getAlbumStatVo(@Param("param") Long albumId);

}
