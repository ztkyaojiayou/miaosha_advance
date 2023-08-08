package com.imooc.miaosha.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.imooc.miaosha.access.UserContext;
import com.imooc.miaosha.bean.MiaoshaUser;
import com.imooc.miaosha.service.LoginService;

//自定义解析器
//需要实现HandlerMethodArgumentResolver接口
@Service
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

	@Autowired
	LoginService userService;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> clazz = parameter.getParameterType();
		return clazz==MiaoshaUser.class;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		return UserContext.getUser();//获取用户信息
	}

}
