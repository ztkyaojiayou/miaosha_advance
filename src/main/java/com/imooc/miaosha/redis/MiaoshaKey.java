package com.imooc.miaosha.redis;

//与秒杀有关的key
public class MiaoshaKey extends BasePrefix{

	private MiaoshaKey( int expireSeconds, String prefix) {
		super(expireSeconds, prefix);
	}
	//商品是否售罄
	public static MiaoshaKey isGoodsOver = new MiaoshaKey(0, "go");
	//秒杀url，过期时间为60s
	public static MiaoshaKey getMiaoshaPath = new MiaoshaKey(60, "mp");
	//秒杀验证码，过期时间为300s
	public static MiaoshaKey getMiaoshaVerifyCode = new MiaoshaKey(300, "vc");
}
