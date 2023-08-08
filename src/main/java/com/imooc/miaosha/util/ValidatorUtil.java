package com.imooc.miaosha.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 3.再编写校验手机号是否符合规范的具体代码
 * 这是利用jsr303框架进行用户登录时对其输入的手机号和密码进行参数校验时，验证是否是符合规范的手机号的具体代码
 * 只是把它专门封装了一个工具包而已
 */
public class ValidatorUtil {
	//使用的是正则表达式
	private static final Pattern mobile_pattern = Pattern.compile("1\\d{10}");

	//返回的是一个boolean值
	public static boolean isMobile(String src) {
		if(StringUtils.isEmpty(src)) {
			return false;
		}
		Matcher m = mobile_pattern.matcher(src);//；利用正则表达式进行判断
		return m.matches();
	}
	
//	public static void main(String[] args) {
//			System.out.println(isMobile("18912341234"));
//			System.out.println(isMobile("1891234123"));
//	}
}
