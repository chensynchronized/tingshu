package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

	@Autowired
	private BaseCategory1Mapper baseCategory1Mapper;

	@Autowired
	private BaseCategory2Mapper baseCategory2Mapper;

	@Autowired
	private BaseCategory3Mapper baseCategory3Mapper;

	@Autowired
	private BaseCategoryViewMapper baseCategoryViewMapper;

	@Autowired
	private BaseAttributeMapper baseAttributeMapper;


	@Override
	public List<JSONObject> getBaseCategoryList() {
		//1.创建目标对象集合
		List<JSONObject> allList = new ArrayList<>();
		//2.查询分类视图得到所有分类信息
		List<BaseCategoryView> allCategoryList = baseCategoryViewMapper.selectList(null);
		//3.处理一级分类
		if (ObjectUtil.isNotEmpty(allCategoryList)){
			Map<Long, List<BaseCategoryView>> baseCategory1Map = allCategoryList.stream().collect(Collectors.groupingBy(baseCategoryView -> baseCategoryView.getCategory1Id(), Collectors.toList()));
			for (Map.Entry<Long, List<BaseCategoryView>> entity1:baseCategory1Map.entrySet()){
				ArrayList<JSONObject> categoryChild1 = new ArrayList<>();
				JSONObject jsonObject1 = new JSONObject();
				jsonObject1.put("categoryId",entity1.getKey());
				List<BaseCategoryView> entity1Value = entity1.getValue();
				if (ObjectUtil.isNotEmpty(entity1Value)){
					jsonObject1.put("categoryName",entity1Value.get(0).getCategory1Name());
					Map<Long, List<BaseCategoryView>> baseCategory2Map = entity1Value.stream().collect(Collectors.groupingBy(baseCategoryView -> baseCategoryView.getCategory2Id(), Collectors.toList()));
					ArrayList<JSONObject> categoryChild2 = new ArrayList<>();
					for (Map.Entry<Long, List<BaseCategoryView>> entity2:baseCategory2Map.entrySet()){
						JSONObject jsonObject2 = new JSONObject();
						jsonObject2.put("categoryId",entity2.getKey());
						List<BaseCategoryView> entity2Value = entity2.getValue();
						if (ObjectUtil.isNotEmpty(entity2Value)){
							jsonObject2.put("categoryName",entity2Value.get(0).getCategory2Name());
							for (BaseCategoryView baseCategoryView:entity2Value){
								JSONObject jsonObject3 = new JSONObject();
								jsonObject3.put("categoryId",baseCategoryView.getCategory3Id());
								jsonObject3.put("categoryName",baseCategoryView.getCategory3Name());
								categoryChild2.add(jsonObject3);
							}
						}
						jsonObject2.put("categoryChild",categoryChild2);
						categoryChild1.add(jsonObject2);

					}
				}
				jsonObject1.put("categoryChild",categoryChild1);
				allList.add(jsonObject1);

			}
		}
		return allList;

	}

	@Override
	public List<BaseAttribute> getAttributesByCategory1Id(Long category1Id) {
		return baseAttributeMapper.getAttributesByCategory1Id(category1Id);


	}

	@Override
	public BaseCategoryView getCategoryView(Long category3Id) {
		return baseCategoryViewMapper.selectById(category3Id);
	}
}
