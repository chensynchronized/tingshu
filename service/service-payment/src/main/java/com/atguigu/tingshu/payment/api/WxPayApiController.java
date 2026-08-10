package com.atguigu.tingshu.payment.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.payment.service.WxPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment")
@Slf4j
public class WxPayApiController {

    @Autowired
    private WxPayService wxPayService;
    /**
     * 获取微信小程序拉起本地微信支付所需要参数
     *
     * @param paymentType 支付类型：支付类型：1301-订单 1302-充值
     * @param orderNo     订单编号
     * @return 小程序拉调用wx.requestPayment(Object object)需要参数
     */
    @GuiGuLogin
    @Operation(summary = "获取微信小程序拉起本地微信支付所需要参数")
    @PostMapping("/wxPay/createJsapi/{paymentType}/{orderNo}")
    public  Result<Map<String,Object>> getWxPrePayParams(@PathVariable String paymentType, @PathVariable String orderNo){
        Map<String,Object> result = wxPayService.getWxPrePayParams(paymentType,orderNo);
        return Result.ok(result);
    }

    /**
     * 小程序轮询查询支付结果-根据商户订单编号查询交易状态
     *
     * @param orderNo
     * @return
     */
    @Operation(summary = "小程序轮询查询支付结果-根据商户订单编号查询交易状态")
    @GetMapping("/wxPay/queryPayStatus/{orderNo}")
    public Result<Boolean> queryPayStatus(@PathVariable String orderNo){
        Boolean result = wxPayService.queryPayStatus(orderNo);
        return Result.ok(result);
    }
    /**
     * 提供给微信支付进行异步回调接口
     *
     * @param request
     * @return
     */
    @Operation(summary = "提供给微信支付进行异步回调接口")
    @PostMapping("/wxPay/notify")
    public Map<String, String> paySuccessNotify(HttpServletRequest request) {
        Map<String, String> mapResult = wxPayService.paySuccessNotify(request);
        return mapResult;
    }
}
