package com.imooc.miaosha.vo;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import com.imooc.miaosha.validator.IsMobile;

/**
 * 利用jsr303框架进行参数校验，这是目前常用的一种方案
 * 目的：用于校验用户登录时的账号和密码是否符合规范
 * 本来是直接写在登录页面的（即LoginController），但考虑到很多地方都会用到，因此把其专门提取出来
 * 如何使用：只需在某一个需要校验的方法的方法参数里面加上@Valid LoginVo loginVo即可
 */
public class LoginVo {

	//1.校验手机号：不能为空
	@NotNull //不能为空
	@IsMobile//是否为手机号码，这个校验的具体代码是框架没有的，我们需要/可以自己定义
	private String mobile;

	//2.校验密码：
	@NotNull //不能为空
	@Length(min=32) //长度至少为32位
	private String password;
	
	public String getMobile() {
		return mobile;
	}
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	@Override
	public String toString() {
		return "LoginVo [mobile=" + mobile + ", password=" + password + "]";
	}
}
