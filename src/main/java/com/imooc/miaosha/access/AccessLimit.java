package com.imooc.miaosha.access;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

//自定义的接口防刷的注解
@Retention(RUNTIME)
@Target(METHOD)
public @interface AccessLimit {
    int seconds();//每几秒

    int maxCount();//只能请求几次

    boolean needLogin() default true;//默认需要登录
}
