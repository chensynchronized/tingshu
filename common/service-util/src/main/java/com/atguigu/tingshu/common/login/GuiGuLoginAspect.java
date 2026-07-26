package com.atguigu.tingshu.common.login;

import cn.hutool.core.util.ObjectUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
@Component
@Aspect
@Slf4j
public class GuiGuLoginAspect {
    @Autowired
    private RedisTemplate redisTemplate;
    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(guiGuLogin)")
    public Object loginAspect(ProceedingJoinPoint joinPoint, GuiGuLogin guiGuLogin){
        Object resultObject = new Object();
        log.info("前置通知...");
        //1.获取小程序端提交的token令牌
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes)requestAttributes;
        HttpServletRequest request = sra.getRequest();
        String token = request.getHeader("token");
        //2.查询存放在redis中的用户信息
        String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
        UserInfoVo userInfoVo = (UserInfoVo) redisTemplate.opsForValue().get(loginKey);
        //3.判断注解是否要求必须登录
        if(guiGuLogin.required() && ObjectUtil.isEmpty(userInfoVo)){

            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);

        }
        //4.将用户信息放入TreadLocal中
        if(ObjectUtil.isNotEmpty(userInfoVo)){
            AuthContextHolder.setUserId(userInfoVo.getId());
            AuthContextHolder.setUsername(userInfoVo.getNickname());
        }
        //5.执行业务方法
        try {
            resultObject = joinPoint.proceed();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        //6.删除ThreadLocal中的用户信息
        AuthContextHolder.removeUserId();
        AuthContextHolder.removeUsername();
        log.info("后置通知...");
        return resultObject;

    }
}
