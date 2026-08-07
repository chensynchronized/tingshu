package com.atguigu.tingshu.common.cache;


import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface GuiGuCache {
    String prefix() default "data:";
}
