package com.imooc.miaosha.redis;

//普通用户的key
public class UserKey extends BasePrefix{

	//普通登录用户的手机号ID和昵称key（根据这两个信息都可以找到该用户）
	private UserKey(String prefix) {
		super(prefix);
	}
	public static UserKey getById = new UserKey("id");
	public static UserKey getByName = new UserKey("name");
}
