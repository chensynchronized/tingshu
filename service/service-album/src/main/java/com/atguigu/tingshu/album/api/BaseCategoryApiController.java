package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value="/api/album")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {

	@Autowired
	private BaseCategoryService baseCategoryService;
	@Operation(summary = " 查询所有分类（1、2、3级分类）")
	@GetMapping("/category/getBaseCategoryList")
	public Result<List<JSONObject>> getBaseCategoryList(){
		List<JSONObject> list = baseCategoryService.getBaseCategoryList();
		return Result.ok(list);

	}
	/**
	 * 根据一级分类Id获取分类（标签名包含标签值） 列表
	 * @param category1Id 1级分类ID
	 * @return
	 */
	@Operation(summary = "根据一级分类Id获取分类（标签名包含标签值） 列表")
	@GetMapping("/category/findAttribute/{category1Id}")
	public Result<List<BaseAttribute>> getAttributesByCategory1Id(@PathVariable Long category1Id){
		List<BaseAttribute> list = baseCategoryService.getAttributesByCategory1Id(category1Id);
		return Result.ok(list);
	}

	/**
	 * 根据三级分类ID(视图主键)查询分类视图对象
	 * @param category3Id
	 * @return
	 */
	@Operation(summary = "根据三级分类ID查询分类视图对象")
	@GetMapping("/category/getCategoryView/{category3Id}")
	public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id){
		BaseCategoryView categoryView = baseCategoryService.getCategoryView(category3Id);
		return Result.ok(categoryView);
	}
	/**
	 * 查询指定1级分类下置顶前7个三级分类集
	 * @param category1Id
	 * @return
	 */
	@Operation(summary = "查询指定1级分类下置顶前7个三级分类集合")
	@GetMapping("/category/findTopBaseCategory3/{category1Id}")
	public Result<List<BaseCategory3>> findTopBaseCategory3(@PathVariable Long category1Id){
		List<BaseCategory3> list = baseCategoryService.findTopBaseCategory3(category1Id);
		return Result.ok(list);
	}

	/**
	 * 根据1级分类对象查询包含二级分类（包含三级分类）
	 * @param category1Id
	 * @return
	 */
	@Operation(summary = "根据1级分类对象查询包含二级分类（包含三级分类）")
	@GetMapping("/category/getBaseCategoryList/{category1Id}")
	public Result<JSONObject> getBaseCategoryListByCategory1Id(@PathVariable Long category1Id){
		JSONObject jsonObject = baseCategoryService.getBaseCategoryListByCategory1Id(category1Id);
		return Result.ok(jsonObject);
	}
	/**
	 * 查询所有一级分类列表
	 * @return
	 */
	@Operation(summary = "查询所有一级分类列表")
	@GetMapping("/category/findAllCategory1")
	public Result<List<BaseCategory1>> findAllCategory1(){
		List<BaseCategory1> list = baseCategoryService.list();
		return Result.ok(list);
	}




}

