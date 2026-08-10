package com.atguigu.tingshu.payment.service.impl;

import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.order.client.OrderFeignClient;
import com.atguigu.tingshu.payment.config.WxPayV3Config;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.atguigu.tingshu.payment.util.PayUtil;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

	@Autowired
	private PaymentInfoService paymentInfoService;
	@Autowired
	private RSAAutoCertificateConfig rsaAutoCertificateConfig;
	@Autowired
	private WxPayV3Config wxPayV3Config;
	@Autowired
	private UserFeignClient userFeignClient;
	@Autowired
	private OrderFeignClient orderFeignClient;
	@Override
	public Map<String, Object> getWxPrePayParams(String paymentType, String orderNo) {

		//1.保存本地交易单消息
		Long userId = AuthContextHolder.getUserId();
		PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(paymentType, orderNo, userId);
		//2.构建微信预支付对象
		JsapiServiceExtension jsapiServiceExtension = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();
		PrepayRequest prepayRequest = new PrepayRequest();
		Amount amount = new Amount();
		amount.setTotal(1);
		prepayRequest.setAmount(amount);
		prepayRequest.setAppid(wxPayV3Config.getAppid());
		prepayRequest.setMchid(wxPayV3Config.getMerchantId());
		prepayRequest.setDescription(paymentInfo.getContent());
		prepayRequest.setNotifyUrl(wxPayV3Config.getNotifyUrl());
		Payer payer = new Payer();
		payer.setOpenid("oSHQ-642JRpQSgTH7fbDMGIuJj94");
		prepayRequest.setPayer(payer);
		prepayRequest.setOutTradeNo(paymentInfo.getOrderNo());
		PrepayWithRequestPaymentResponse response = jsapiServiceExtension.prepayWithRequestPayment(prepayRequest);
		if (response != null) {
			Map<String, Object> mapResult = new HashMap<>();
			mapResult.put("timeStamp", response.getTimeStamp());
			mapResult.put("package", response.getPackageVal());
			mapResult.put("paySign", response.getPaySign());
			mapResult.put("signType", response.getSignType());
			mapResult.put("nonceStr", response.getNonceStr());
			return mapResult;
		}
		return null;

	}

	@Override
	public Boolean queryPayStatus(String orderNo) {

		QueryOrderByOutTradeNoRequest queryOrderByOutTradeNoRequest = new QueryOrderByOutTradeNoRequest();
		queryOrderByOutTradeNoRequest.setMchid(wxPayV3Config.merchantId);
		queryOrderByOutTradeNoRequest.setOutTradeNo(orderNo);
		JsapiServiceExtension jsapiServiceExtension = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();
		Transaction transaction = jsapiServiceExtension.queryOrderByOutTradeNo(queryOrderByOutTradeNoRequest);
		if (transaction != null){
			Transaction.TradeStateEnum tradeState = transaction.getTradeState();
			if (tradeState == Transaction.TradeStateEnum.SUCCESS){
				//2.发货
				OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderNo).getData();
				Assert.notNull(orderInfo, "订单信息不存在");
				UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
				userPaidRecordVo.setUserId(orderInfo.getUserId());
				userPaidRecordVo.setOrderNo(orderInfo.getOrderNo());
				userPaidRecordVo.setItemType(orderInfo.getItemType());
				List<Long> itemIdList = orderInfo.getOrderDetailList().stream().map(OrderDetail::getItemId).collect(Collectors.toList());
				userPaidRecordVo.setItemIdList(itemIdList);
				Result savePaidRecordResult = userFeignClient.savePaidRecord(userPaidRecordVo);
				if (savePaidRecordResult.getCode() != 200){
					throw new GuiguException(500, "新增购买记录异常");
				}
				return true;
			}
		}
		return false;
	}
	/**
	 * 用户付款成功后，处理微信支付异步回调
	 *
	 * @param request
	 * @return
	 */
	@Override
//	@GlobalTransactional(rollbackFor = Exception.class)
	public Map<String, String> paySuccessNotify(HttpServletRequest request) {
		//1.从请求头中获取微信提交参数
		String wechatPaySerial = request.getHeader("Wechatpay-Serial");  //签名
		String nonce = request.getHeader("Wechatpay-Nonce");  //签名中的随机数
		String timestamp = request.getHeader("Wechatpay-Timestamp"); //时间戳
		String signature = request.getHeader("Wechatpay-Signature"); //签名类型

		//HTTP 请求体 body。切记使用原始报文，不要用 JSON 对象序列化后的字符串，避免验签的 body 和原文不一致。
		String requestBody = PayUtil.readData(request);
		//2.构建RequestParam请求参数对象
		RequestParam requestParam = new RequestParam.Builder()
				.serialNumber(wechatPaySerial)
				.nonce(nonce)
				.signature(signature)
				.timestamp(timestamp)
				.body(requestBody)
				.build();
		//3.// 初始化 NotificationParser 解析器对象
		NotificationParser parser = new NotificationParser(rsaAutoCertificateConfig);
		//4. 调用解析器对象解析方法 验签、解密 并转换成 Transaction
		Transaction transaction = parser.parse(requestParam, Transaction.class);
		if (transaction != null) {
			//4.1 业务验证，验证付款状态以及用户实际付款金额跟商户侧金额是否一致
			if (Transaction.TradeStateEnum.SUCCESS == transaction.getTradeState()) {
				Integer payerTotal = transaction.getAmount().getPayerTotal();
				//todo 调试阶段支付金额为1分，后续改为动态从本地交易记录中获取实际金额
				if (payerTotal.intValue() == 1) {
					//4.2 更新本地交易记录状态
					paymentInfoService.updatePaymentInfoSuccess(transaction);
					Map<String, String> map = new HashMap<>();
					map.put("code", "SUCCESS");
					map.put("message", "SUCCESS");
					return map;
				}
			}
		}
		return null;


	}
}
