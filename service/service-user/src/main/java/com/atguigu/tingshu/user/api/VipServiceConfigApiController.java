package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.service.VipServiceConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "VIP服务配置管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class VipServiceConfigApiController {

	@Autowired
	private VipServiceConfigService vipServiceConfigService;

	/**
	 * 获取全部VIP会员服务配置信息
	 * api/user/vipServiceConfig/findAll
	 * @return
	 */
	@GetMapping("/vipServiceConfig/findAll")
	public Result findAll(){
		return Result.ok(vipServiceConfigService.list());
	}
	/**
	 * 根据套餐ID查询套餐信息
	 *
	 * @param id
	 * @return
	 */
	@Operation(summary = "根据套餐ID查询套餐信息")
	@GetMapping("/vipServiceConfig/getVipServiceConfig/{id}")
	public Result<VipServiceConfig> getVipServiceConfig(@PathVariable Long id){
		VipServiceConfig vipServiceConfig = vipServiceConfigService.getById(id);
		return Result.ok(vipServiceConfig);
	}

}

