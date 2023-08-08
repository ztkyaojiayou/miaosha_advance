package com.imooc.miaosha.redis;

public class GoodsKey extends BasePrefix{

	//与商品信息相关的key
	private GoodsKey(int expireSeconds, String prefix) {
		super(expireSeconds, prefix);
	}
	//商品列表、商品详情、秒杀商品库存key
	public static GoodsKey getGoodsList = new GoodsKey(60, "gl");
	public static GoodsKey getGoodsDetail = new GoodsKey(60, "gd");
	public static GoodsKey getMiaoshaGoodsStock= new GoodsKey(0, "gs");
}
