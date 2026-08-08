package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order")
@SuppressWarnings({"all"})
public class OrderInfoApiController {

	@Autowired
	private OrderInfoService orderInfoService;
	/**
	 * 订单结算页面数据汇总（VIP会员、专辑、声音）
	 *
	 * @param tradeVo
	 * @return 订单信息VO对象
	 */
	@GuiGuLogin
	@Operation(summary = "订单结算页面数据汇总（VIP会员、专辑、声音）")
	@PostMapping("/orderInfo/trade")
	public Result<OrderInfoVo> tradeOrderData(@RequestBody TradeVo tradeVo){
		Long userId = AuthContextHolder.getUserId();
		OrderInfoVo orderInfoVo = orderInfoService.tradeOrderData(userId,tradeVo);
		return Result.ok(orderInfoVo);
	}

	/**
	 * 提交订单，可能包含余额支付
	 *
	 * @param orderInfoVo
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "提交订单，可能包含余额支付")
	@PostMapping("/orderInfo/submitOrder")
	public Result<Map<String,String>> submitOrder(@RequestBody OrderInfoVo orderInfoVo){
		Long userId = AuthContextHolder.getUserId();
		Map<String,String> result = orderInfoService.submitOrder(orderInfoVo,userId);
		return Result.ok(result);
	}

	/**
	 * 查询当前用户指定订单信息
	 *
	 * @param orderNo
	 * @return
	 */
	@GuiGuLogin
	@GetMapping("/orderInfo/getOrderInfo/{orderNo}")
	public Result<OrderInfo> getOrderInfo(@PathVariable String orderNo){
		Long userId = AuthContextHolder.getUserId();
		OrderInfo orderInfo = orderInfoService.getOrderInfo(orderNo,userId);
		return Result.ok(orderInfo);
	}
	/**
	 * 分页获取当前用户订单列表
	 * @param page
	 * @param limit
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "分页获取当前用户订单列表")
	@GetMapping("/orderInfo/findUserPage/{page}/{limit}")
	public Result<Page<OrderInfo>> getUserOrderByPage(@PathVariable Long page,
													  @PathVariable Long limit){
		Page<OrderInfo> pageParam = new Page<>(page, limit);
		Long userId = AuthContextHolder.getUserId();
		pageParam = orderInfoService.getUserOrderByPage(pageParam, userId);
		return Result.ok(pageParam);
	}

}

