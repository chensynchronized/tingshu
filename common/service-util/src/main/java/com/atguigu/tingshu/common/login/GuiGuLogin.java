package com.atguigu.tingshu.common.login;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GuiGuLogin {

    /**
     * 是否必须登录，默认为：必须登录
     * @return
     */
    boolean required() default true;
}
