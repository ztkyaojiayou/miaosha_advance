package com.imooc.miaosha.vo;

import com.imooc.miaosha.bean.OrderInfo;

/**
 * 同理，订单详情VO，其中包含商品信息GoodsVo、订单详情信息OrderInfo
 * 注意：此订单详情vo是一个关于订单的组合对象，其既不是单纯的订单详情OrderInfo，也不是秒杀订单MiaoshaOrder
 * 而是一个包含商品信息GoodsVo、订单详情信息OrderInfo的订单
 */
public class OrderDetailVo {
	private GoodsVo goods;//商品信息
	private OrderInfo order;//订单详情
	public GoodsVo getGoods() {
		return goods;
	}
	public void setGoods(GoodsVo goods) {
		this.goods = goods;
	}
	public OrderInfo getOrder() {
		return order;
	}
	public void setOrder(OrderInfo order) {
		this.order = order;
	}
}
