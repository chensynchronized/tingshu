package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    int checkAndDeduct(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
    @Update("update user_account set recharge_amount = recharge_amount + #{rechargeAmount} where user_id = #{userId}")
    int updateUserAccount(@Param("userId") Long userId, @Param("rechargeAmount") BigDecimal rechargeAmount);


}
