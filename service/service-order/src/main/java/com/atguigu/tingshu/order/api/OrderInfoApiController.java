package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}

