package com.imooc.miaosha.redis;

public interface KeyPrefix {//key的前缀
		
	public int expireSeconds();//过期时间
	
	public String getPrefix();//获取前缀
	
}
