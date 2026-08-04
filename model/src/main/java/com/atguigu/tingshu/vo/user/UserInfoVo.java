package com.atguigu.tingshu.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@Schema(description = "UserInfoVo")
public class UserInfoVo implements Serializable {

	@Schema(description = "用户id")
	private Long id;

	@Schema(description = "微信openId")
	private String wxOpenId;

	@Schema(description = "nickname")
	private String nickname;

	@Schema(description = "主播用户头像图片")
	private String avatarUrl;

	@Schema(description = "用户是否为VIP会员 0:普通用户  1:VIP会员")
	private Integer isVip;

	@Schema(description = "当前VIP到期时间，即失效时间")
	@DateTimeFormat(
			pattern = "yyyy-MM-dd"
	)
	@JsonFormat(
			shape = JsonFormat.Shape.STRING,
			pattern = "yyyy-MM-dd",
			timezone = "GMT+8"
	)
	@JsonDeserialize
	private Date vipExpireTime;

}
