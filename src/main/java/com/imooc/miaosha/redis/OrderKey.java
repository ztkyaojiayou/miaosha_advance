package com.imooc.miaosha.redis;

public class OrderKey extends BasePrefix {
//订单key
	public OrderKey(String prefix) {
		super(prefix);
	}
	public static OrderKey getMiaoshaOrderByUidGid = new OrderKey("moug");
}
