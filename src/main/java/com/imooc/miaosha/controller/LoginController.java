package com.imooc.miaosha.controller;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.Result;
import com.imooc.miaosha.service.LoginService;
import com.imooc.miaosha.vo.LoginVo;

/**
 * 这是与用户登录相关的controller
 */

/**
 * 1.先进入登录页面
 * （注意：我们这个系统的登录界面是由我们自己输入地址进入的）
 */
@Controller
@RequestMapping("/login")
public class LoginController {

    private static Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    LoginService userService;

    @Autowired
    RedisService redisService;
    //1.先跳转到登录界面
    @RequestMapping("/to_login")
    public String toLogin() {
        return "login";//用户输入登录页面的地址后，就跳转到“登录”页面（即login.html页面）
    }

    //2.开始登录
    @RequestMapping("/do_login")
    @ResponseBody
    //用户登录时就对其输入的账号和密码进行jsr303参数校验（在方法参数里面加上@Valid LoginVo loginVo即可）
    public Result<String> doLogin(HttpServletResponse response, @Valid LoginVo loginVo) {
        log.info(loginVo.toString());
        //登录，成功之后会返回一个token
        String token = userService.login(response, loginVo);
        return Result.success(token);
    }
}
