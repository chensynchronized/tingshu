package com.atguigu.tingshu.search.service;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

public interface SearchService {


    void upperAlbum(Long albumId);

    void lowerAlbum(Long albumId);
    /**
     * 站内检索，支持关键字、分类、标签条件分页检索，结果高亮
     * @param albumIndexQuery 查询条件对象：包含关键字、分类id、标签列表、排序、分页信息
     * @return
     */
    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);
    /**
     * 封装检索请求对象
     * @param albumIndexQuery 查询条件
     * @return 检索请求对象
     */
    SearchRequest buildDSL(AlbumIndexQuery albumIndexQuery);

    /**
     * 解析ES检索响应结果
     *
     * @param searchResponse ES检索结果对象
     * @param queryVo
     * @return 自定义VO
     */
    AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> searchResponse, AlbumIndexQuery queryVo);
}
