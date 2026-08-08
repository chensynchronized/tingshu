package com.atguigu.tingshu.vo.account;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountDeductVo {

    private Long userId;

    private String orderNo;

    private BigDecimal amount;

    private String content;

}
