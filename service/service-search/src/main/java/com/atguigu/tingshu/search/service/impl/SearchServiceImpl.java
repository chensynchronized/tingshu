package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.util.PinYinUtils;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.repository.SuggestIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private ElasticsearchClient elasticsearchClient;
    @Autowired
    private SuggestIndexRepository suggestIndexRepository;
    @Autowired
    private RedisTemplate redisTemplate;



    private static final String INDEX_NAME = "albuminfo";
    //建议词词库
    private static final String SUCCEST_INDEX_NAME = "suggestinfo";
    /**
     * 新增提词记录到提词索引库
     *
     * @param albumInfoIndex
     */
    @Override
    public void saveSuggestIndex(AlbumInfoIndex albumInfoIndex){
        SuggestIndex suggestIndex = new SuggestIndex();
        suggestIndex.setId(albumInfoIndex.getId().toString());
        suggestIndex.setTitle(albumInfoIndex.getAlbumTitle());
        suggestIndex.setKeyword(new Completion(new String[]{suggestIndex.getTitle()}));
        suggestIndex.setKeywordPinyin(new Completion(new String[]{PinyinUtil.getPinyin(albumInfoIndex.getAlbumTitle(),"")}));
        suggestIndex.setKeywordSequence(new Completion(new String[]{PinyinUtil.getFirstLetter(albumInfoIndex.getAlbumTitle(),"")}));
        suggestIndexRepository.save(suggestIndex);
    }
    @Override
    public void upperAlbum(Long albumId) {
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //1.根据专辑id查询专辑信息（远程调用专辑服务）
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑不存在，专辑ID{}", albumId);
            BeanUtil.copyProperties(albumInfo, albumInfoIndex);
            //2.将albumAttributeValue转为AttributeValueIndex
            List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
            if (CollUtil.isNotEmpty(albumAttributeValueVoList)) {
                List<AttributeValueIndex> attributeValueIndexList = albumAttributeValueVoList.stream().map(albumAttributeValue -> {
                    AttributeValueIndex attributeValueIndex = BeanUtil.copyProperties(albumAttributeValue, AttributeValueIndex.class);
                    return attributeValueIndex;
                }).collect(Collectors.toList());
                albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
            }
            return albumInfo;

        },threadPoolExecutor);
        CompletableFuture<Void> userCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //3.根据用户id查询用户信息（远程调用用户服务）
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVoById(albumInfo.getUserId()).getData();
            Assert.notNull(userInfoVo, "用户不存在，用户ID{}", albumInfo.getUserId());
            albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());
        },threadPoolExecutor);
        CompletableFuture<Void> categoryCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //4.根据三级分类id查询一级分类，二级分类信息（远程调用专辑服务）
            BaseCategoryView baseCategoryView = albumFeignClient.getCategoryView(albumInfo.getCategory3Id()).getData();
            Assert.notNull(baseCategoryView, "分类不存在，分类ID{}", albumInfo.getCategory3Id());
            albumInfoIndex.setCategory1Id(baseCategoryView.getCategory1Id());
            albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());
        },threadPoolExecutor);
        CompletableFuture<Void> scoreCompletableFuture = CompletableFuture.runAsync(() -> {
            //5.5.TODO 封装统计信息，采用产生随机值 以及专辑热度
            //5.1 随机为专辑产生播放量，订阅量，购买量，评论量 、
            int num1 = RandomUtil.randomInt(1000, 2000);
            int num2 = RandomUtil.randomInt(500, 1000);
            int num3 = RandomUtil.randomInt(200, 400);
            int num4 = RandomUtil.randomInt(100, 200);
            albumInfoIndex.setPlayStatNum(num1);
            albumInfoIndex.setSubscribeStatNum(num2);
            albumInfoIndex.setBuyStatNum(num3);
            albumInfoIndex.setCommentStatNum(num4);

            //5.2 基于统计值计算出专辑得分 为不同统计类型设置不同权重
            BigDecimal bigDecimal1 = new BigDecimal(num4).multiply(new BigDecimal("0.4"));
            BigDecimal bigDecimal2 = new BigDecimal(num3).multiply(new BigDecimal("0.3"));
            BigDecimal bigDecimal3 = new BigDecimal(num2).multiply(new BigDecimal("0.2"));
            BigDecimal bigDecimal4 = new BigDecimal(num1).multiply(new BigDecimal("0.1"));
            BigDecimal hotScore = bigDecimal1.add(bigDecimal2).add(bigDecimal3).add(bigDecimal4);
            albumInfoIndex.setHotScore(hotScore.doubleValue());
        },threadPoolExecutor);

        CompletableFuture.allOf(albumInfoCompletableFuture,categoryCompletableFuture, userCompletableFuture, scoreCompletableFuture).join();
        //6.写入索引库
        albumInfoIndexRepository.save(albumInfoIndex);
        this.saveSuggestIndex(albumInfoIndex);
    }

    @Override
    public void lowerAlbum(Long albumId) {
        albumInfoIndexRepository.deleteById(albumId);
    }

    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {
        try{
            //1.构建请求对象
            SearchRequest searchRequest = this.buildDSL(albumIndexQuery);
            //2.调用es客户端查询
            SearchResponse<AlbumInfoIndex> search = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);
            //3.解析结果
            return this.parseResult(search, albumIndexQuery);
        }catch (Exception e){
            log.error("[搜索服务]查询条件：{}，站内检索异常：{}", albumIndexQuery, e);
            throw new RuntimeException(e);
        }


    }

    @Override
    public SearchRequest buildDSL(AlbumIndexQuery albumIndexQuery) {
        //1.创建检索请求构建器对象-封装检索索引库 及 所有检索DSL语句
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder();
        searchBuilder.index(INDEX_NAME);
        //2.设置请求体参数"query",处理查询条件（关键字、分类、标签）
        //2.1 创建最外层bool组合条件对象
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        //2.2 处理关键字查询条件 采用must必须满足，包含bool组合三个子条件，三个子条件或者关系
        String keyword = albumIndexQuery.getKeyword();
        if(ObjectUtil.isNotEmpty(keyword)){
            BoolQuery.Builder keywordBoolQuery = new BoolQuery.Builder();
            keywordBoolQuery.should(s->s.match(m->m.field("albumTitle").query(keyword)));
            keywordBoolQuery.should(s->s.match(m->m.field("albumAuthor").query(keyword)));
            keywordBoolQuery.should(s->s.term(t->t.field("announcerName").value(keyword)));
            boolQuery.must(keywordBoolQuery.build()._toQuery());
        }
        //2.3 处理分类ID查询条件
        if (ObjectUtil.isNotEmpty(albumIndexQuery.getCategory1Id())){
            boolQuery.filter(f->f.term(t->t.field("category1Id").value(albumIndexQuery.getCategory1Id())));
        }
        if (ObjectUtil.isNotEmpty(albumIndexQuery.getCategory2Id())){
            boolQuery.filter(f->f.term(t->t.field("category2Id").value(albumIndexQuery.getCategory2Id())));
        }
        if (ObjectUtil.isNotEmpty(albumIndexQuery.getCategory3Id())){
            boolQuery.filter(f->f.term(t->t.field("category3Id").value(albumIndexQuery.getCategory3Id())));
        }
        //2.4 处理标签查询条件(可能有多个)
        if (CollUtil.isNotEmpty(albumIndexQuery.getAttributeList())){
            for(String attributeIdAndValueId : albumIndexQuery.getAttributeList()){
                String[] split = attributeIdAndValueId.split(":");
                if (split != null && split.length == 2){
                    boolQuery.filter(f->f.nested(n->n.path("attributeValueIndexList")
                            .query(q->q.bool(b->b.must(m->m.term(t->t.field("attributeId").value(split[0])))
                                    .must(m->m.term(t->t.field("attributeValueId").value(split[1])))))));
                }
            }
        }
        searchBuilder.query(boolQuery.build()._toQuery());
        //3.设置请求体参数"from","size" 处理分页
        int from = (albumIndexQuery.getPageNo() - 1) * albumIndexQuery.getPageSize();
        searchBuilder.from(from);
        searchBuilder.size(albumIndexQuery.getPageSize());
        //4.设置请求体参数"sort" 处理排序（动态 综合、播放量、发布时间）
        String order = albumIndexQuery.getOrder();
        if (ObjectUtil.isNotEmpty(order)){
            String[] split = order.split(":");
            if (split != null && split.length == 2){
                String orderField = "";
                switch (split[0]){
                    case "1":
                        orderField = "hotScore";
                        break;
                    case "2":
                        orderField = "playStatNum";
                        break;
                    case "3":
                        orderField = "createTime";
                        break;
                }
                String finalOrderField = orderField;
                searchBuilder.sort(s->s.field(f->f.field(finalOrderField).order("asc".equals(split[1])?SortOrder.Asc:SortOrder.Desc)));
            }

        }
        //5.设置请求体参数"highlight" 处理高亮，前提：用户录入关键字
        if (ObjectUtil.isNotEmpty(keyword)){
            searchBuilder.highlight(h->h.fields("albumTitle", hl->hl.preTags("<font color='red'>").postTags("</font>")));
        }
        //6.设置请求体参数"_source" 处理字段指定
        searchBuilder.source(s -> s.filter(f -> f.excludes("category1Id",
                "category2Id",
                "category3Id",
                "attributeValueIndexList.attributeId",
                "attributeValueIndexList.valueId")));

        //7.调用构建器builder返回检索请求对象
        return searchBuilder.build();
    }

    @Override
    public AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> searchResponse, AlbumIndexQuery queryVo) {
        //1.创建结果对象，设置页码和页大小
        AlbumSearchResponseVo vo = new AlbumSearchResponseVo();
        vo.setPageNo(queryVo.getPageNo());
        vo.setPageSize(queryVo.getPageSize());
        //2.从响应结果中获取总记录数，计算总页数
        long total = searchResponse.hits().total().value();
        vo.setTotal(total);
        long totalPages = total % queryVo.getPageSize() == 0 ? total / queryVo.getPageSize() : total / queryVo.getPageSize() + 1;
        vo.setTotalPages(totalPages);
        //3.处理文档
        List<Hit<AlbumInfoIndex>> hitList = searchResponse.hits().hits();
        if(CollUtil.isNotEmpty(hitList)){
            List<AlbumInfoIndexVo> albumInfoIndexVoList = hitList.stream().map(hit -> {
                AlbumInfoIndexVo albumInfoIndexVo = BeanUtil.copyProperties(hit.source(), AlbumInfoIndexVo.class);
                Map<String, List<String>> highlightMap = hit.highlight();
                if (CollectionUtil.isNotEmpty(highlightMap) && highlightMap.containsKey("albumTitle")) {
                    String highlightAlbumTitle = highlightMap.get("albumTitle").get(0);
                    albumInfoIndexVo.setAlbumTitle(highlightAlbumTitle);
                }
                return albumInfoIndexVo;
            }).collect(Collectors.toList());
            //4.返回结果对象
            vo.setList(albumInfoIndexVoList);
        }

        return vo;
    }
    /**
     * 查询1级分类下置顶3级分类下包含分类热门专辑
     *
     * @param category1Id
     * @return
     */
    @Override
    public List<HashMap<String, Object>> getTopCategory3HotAlbumList(Long category1Id) {
        try{
            //1.根据1级分类ID远程调用专辑服务获取置顶前7个三级分类集合
            List<BaseCategory3> baseCategory3List = albumFeignClient.findTopBaseCategory3(category1Id).getData();
            if (CollUtil.isNotEmpty(baseCategory3List)){
                List<Long> category3Ids = baseCategory3List.stream().map(BaseCategory3::getId).collect(Collectors.toList());
                Map<Long, BaseCategory3> category3Map = baseCategory3List.stream().collect(Collectors.toMap(BaseCategory3::getId, c -> c));
                List<FieldValue> fieldValueList = category3Ids.stream().map(id -> FieldValue.of(id)).collect(Collectors.toList());
                //2.检索ES获取置顶三级分类（7个）不同置顶三级分类下热度前6个的专辑列表
                SearchResponse<AlbumInfoIndex> searchResponse = elasticsearchClient.search(s -> s.index(INDEX_NAME)
                        .size(10)
                        .query(q->q.terms(t->t.field("category3Id").terms(th->th.value(fieldValueList))))
                        .aggregations("category3Agg",a->a.terms(t->t.field("category3Id").size(10)).aggregations("top6Agg",al->al.topHits(th->th.size(6).sort(sort->sort.field(f->f.field("hotScore").order(SortOrder.Desc))))))
                        , AlbumInfoIndex.class);
                //3.解析ES响应聚合
                Aggregate category3Agg = searchResponse.aggregations().get("category3Agg");
                Buckets<LongTermsBucket> buckets = category3Agg.lterms().buckets();
                List<LongTermsBucket> bucketList = buckets.array();
                if(CollUtil.isNotEmpty(bucketList)){
                    List<HashMap<String, Object>> result = bucketList.stream().map(bucket -> {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("baseCategory3", category3Map.get(bucket.key()));
                        Aggregate top6Agg = bucket.aggregations().get("top6Agg");
                        List<AlbumInfoIndex> albumInfoIndexList = top6Agg.topHits().hits().hits().stream().map(hit -> {
                            JsonData source = hit.source();
                            return JSON.parseObject(source.toString(), AlbumInfoIndex.class);
                        }).collect(Collectors.toList());
                        hashMap.put("list", albumInfoIndexList);
                        return hashMap;
                    }).collect(Collectors.toList());
                    return result;
                }
            }

        }catch (Exception e){
            log.error("[检索服务]首页热门专辑异常：{}", e);
            throw new RuntimeException(e);
        }
        return null;

    }

    @Override
    public List<String> completeSuggest(String keyword) {

        try{
            //1.查询题词索引库
            SearchResponse<SuggestIndex> searchResponse = elasticsearchClient.search(s->s.index(SUCCEST_INDEX_NAME)
                            .suggest(su->su.suggesters("mySuggestKeyword",s1->s1.prefix(keyword).completion(c->c.field("keyword").size(10).skipDuplicates(true)))
                                    .suggesters("mySuggestPinyin",s1->s1.prefix(keyword).completion(c->c.field("keywordPinyin").size(10).skipDuplicates(true)))
                                    .suggesters("mySuggestSequence",s1->s1.prefix(keyword).completion(c->c.field("keywordSequence").size(10).skipDuplicates(true))))
                    ,SuggestIndex.class);
            //2.解析建议词响应结果，将结果进行去重
            Set<String> hashSet = new HashSet<>();
            hashSet.addAll(this.parseSuggestResult("mySuggestKeyword", searchResponse));
            hashSet.addAll(this.parseSuggestResult("mySuggestPinyin", searchResponse));
            hashSet.addAll(this.parseSuggestResult("mySuggestSequence", searchResponse));
            if (hashSet.size() >= 10) {
                return new ArrayList<>(hashSet).subList(0, 10);
            }
            //3.如果建议词记录数小于10，采用全文查询专辑索引库尝试补全
            SearchResponse<AlbumInfoIndex> infoIndexSearchResponse = elasticsearchClient.search(s -> s.index(INDEX_NAME)
                            .query(q -> q.match(m -> m.field("title").query(keyword)))
                    , AlbumInfoIndex.class);
            //4.解析检索结果，将结果放入HashSet
            List<Hit<AlbumInfoIndex>> hits = infoIndexSearchResponse.hits().hits();
            if (CollUtil.isNotEmpty(hits)) {
                hits.forEach(hit -> hashSet.add(hit.source().getAlbumTitle()));
            }
            return new ArrayList<>(hashSet);
        }catch (Exception e){
            log.error("[搜索服务]建议词自动补全异常：{}", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<String> parseSuggestResult(String suggestName, SearchResponse<SuggestIndex> searchResponse) {
        //1.获取指定自定义建议词名称获取建议结果
        List<Suggestion<SuggestIndex>> suggestionList = searchResponse.suggest().get(suggestName);
        //2.获取建议自动补全对象
        List<String> list = new ArrayList<>();
        suggestionList.forEach(suggestIndexSuggestion -> {
            //3.获取options中自动补全结果
            for (CompletionSuggestOption<SuggestIndex> suggestOption : suggestIndexSuggestion.completion().options()) {
                SuggestIndex suggestIndex = suggestOption.source();
                list.add(suggestIndex.getTitle());
            }
        });
        return list;
    }
    /**
     * 获取不同分类下不同排序方式榜单专辑列表
     */
    @Override
    public void updateLatelyAlbumRanking() {
        try{
            //1.远程调用专辑服务，获取所有一级分类
            List<BaseCategory1> baseCategory1List = albumFeignClient.findAllCategory1().getData();
            Assert.isNull(baseCategory1List, "一级分类为空");
            //2.循环遍历一级分类
            for (BaseCategory1 baseCategory1 : baseCategory1List) {
                Long baseCategory1Id = baseCategory1.getId();
                //3.处理当前一级分类，五种排序方式榜单专辑
                String[] rankingDimensionArray =
                        new String[]{"hotScore", "playStatNum", "subscribeStatNum", "buyStatNum", "commentStatNum"};
                for (String rankingDimension : rankingDimensionArray) {
                    //3.1查询es
                    SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(s -> s.index(INDEX_NAME)
                                    .query(q->q.match(m->m.field("category1Id").query(baseCategory1Id)))
                                    .sort(sort->sort.field(f->f.field(rankingDimension).order(SortOrder.Desc)))
                                    .size(10)
                            , AlbumInfoIndex.class);
                    List<Hit<AlbumInfoIndex>> hits = response.hits().hits();
                    if (CollUtil.isNotEmpty(hits)){
                        List<AlbumInfoIndex> albumInfoIndexList = hits.stream().map(hit -> {
                            AlbumInfoIndex source = hit.source();
                            return source;

                        }).collect(Collectors.toList());
                        //4.将专辑榜单存入redis中
                        String key = RedisConstant.RANKING_KEY_PREFIX + baseCategory1Id;
                        redisTemplate.opsForHash().put(key,rankingDimension, albumInfoIndexList);
                    }
                }
            }

        }catch (Exception e){
            log.error("[搜索服务]更新排行榜异常：{}", e);
            throw new RuntimeException(e);
        }
    }
}
