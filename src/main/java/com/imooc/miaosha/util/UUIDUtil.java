package com.imooc.miaosha.util;

import java.util.UUID;

//生成全局ID的工具包（重点）
public class UUIDUtil {
	public static String uuid() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
