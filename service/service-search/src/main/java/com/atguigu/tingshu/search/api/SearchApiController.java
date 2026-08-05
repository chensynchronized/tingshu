package com.atguigu.tingshu.search.api;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search")
@SuppressWarnings({"all"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;
    /**
     * 仅用于测试-将指定已存在专辑上架保存到索引库
     *
     * @param albumId
     * @return
     */
    @Operation(summary = "仅用于测试-将指定已存在专辑上架保存到索引库")
    @GetMapping("/albumInfo/upperAlbum/{albumId}")
    public Result upperAlbum(@PathVariable Long albumId){
        searchService.upperAlbum(albumId);
        return Result.ok();
    }
    /**
     * 下架专辑-删除文档
     * @param albumId
     * @return
     */
    @Operation(summary = "该接口仅用于测试-下架专辑-删除文档")
    @GetMapping("/albumInfo/lowerAlbum/{albumId}")
    public Result lowerAlbum(@PathVariable Long albumId){
        searchService.lowerAlbum(albumId);
        return Result.ok();
    }

    /**
     * 站内检索，支持关键字、分类、标签条件分页检索，结果高亮
     *
     * @param albumIndexQuery
     * @return
     */
    @Operation(summary = "站内检索，支持关键字、分类、标签条件分页检索，结果高亮")
    @PostMapping("/albumInfo")
    public Result<AlbumSearchResponseVo> search(@RequestBody AlbumIndexQuery albumIndexQuery){
        AlbumSearchResponseVo vo = searchService.search(albumIndexQuery);
        return Result.ok(vo);
    }
    @Operation(summary = "查询1级分类下置顶3级分类下包含分类热门专辑")
    @GetMapping("albumInfo/channel/{category1Id}")
    public Result<List<HashMap<String,Object>>> getTopCategory3HotAlbumList(@PathVariable Long category1Id){
        return Result.ok(searchService.getTopCategory3HotAlbumList(category1Id));
    }
    /**
     * 根据用户录入部分关键字进行自动补全
     * @param keyword
     * @return
     */
    @Operation(summary = "根据用户录入部分关键字进行自动补全")
    @GetMapping("/albumInfo/completeSuggest/{keyword}")
    public Result<List<String>> completeSuggest(@PathVariable String keyword){
        return Result.ok(searchService.completeSuggest(keyword));
    }

    /**
     * 为定时更新首页排行榜提供调用接口
     * @return
     */
    @Operation(summary = "为定时更新首页排行榜提供调用接口")
    @GetMapping("/albumInfo/updateLatelyAlbumRanking")
    public Result updateLatelyAlbumRanking(){
        searchService.updateLatelyAlbumRanking();
        return Result.ok();
    }

}

