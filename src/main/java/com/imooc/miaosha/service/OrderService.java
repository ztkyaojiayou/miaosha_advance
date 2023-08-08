package com.imooc.miaosha.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imooc.miaosha.dao.OrderDao;
import com.imooc.miaosha.bean.MiaoshaOrder;
import com.imooc.miaosha.bean.MiaoshaUser;
import com.imooc.miaosha.bean.OrderInfo;
import com.imooc.miaosha.redis.OrderKey;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.vo.GoodsVo;

/**
 * 这是与订单相关的service
 * 即生成订单的具体实现
 */
@Service
public class OrderService {
	
	@Autowired
	OrderDao orderDao;
	
	@Autowired
	RedisService redisService;

	/**
	 * 注意：这里创建了两个订单：
	 * （1）普通订单orderInfo，用于存放订单的详细信息
	 * （2）秒杀订单miaoshaOrder，只包含了商品ID、订单ID和用户ID
	 * @param user
	 * @param goods
	 * @return
	 */
	//1.创建订单（有两个）
	@Transactional//之前讲过，下订单、写入秒杀订单表都需要开启事务
	public OrderInfo createOrder(MiaoshaUser user, GoodsVo goods) {
		//1.1先写入订单相关信息
		OrderInfo orderInfo = new OrderInfo();
		orderInfo.setCreateDate(new Date());
		orderInfo.setDeliveryAddrId(0L);
		orderInfo.setGoodsCount(1);
		orderInfo.setGoodsId(goods.getId());
		orderInfo.setGoodsName(goods.getGoodsName());
		orderInfo.setGoodsPrice(goods.getMiaoshaPrice());
		orderInfo.setOrderChannel(1);
		orderInfo.setStatus(0);
		orderInfo.setUserId(user.getId());

		//1.2再生成订单（即把订单信息写入到orderInfo表中）
		orderDao.insert(orderInfo);

		//1.3最后，再额外生成一个秒杀订单，其只包含商品ID、用户ID和订单ID
		//作用：利用商品ID、用户ID生成一个唯一索引，防止超卖
		MiaoshaOrder miaoshaOrder = new MiaoshaOrder();
		miaoshaOrder.setGoodsId(goods.getId());
		miaoshaOrder.setOrderId(orderInfo.getId());
		miaoshaOrder.setUserId(user.getId());
		orderDao.insertMiaoshaOrder(miaoshaOrder);
		//1.4再把秒杀订单信息存入Redis，以后就可以绕过数据库而直接从缓存里取啦（即又使用了对象缓存技术）
		redisService.set(OrderKey.getMiaoshaOrderByUidGid, ""+user.getId()+"_"+goods.getId(), miaoshaOrder);
		//1.5返回订单
		return orderInfo;
	}

	//2.删除秒杀订单
	public void deleteOrders() {
		orderDao.deleteOrders();
		orderDao.deleteMiaoshaOrders();
	}

	//3.根据订单ID获取订单OrderInfo（后面的OrderDetailVo有用到）
	public OrderInfo getOrderById(long orderId) {
		return orderDao.getOrderById(orderId);
	}

	//4.根据用户ID和商品ID获取秒杀订单MiaoshaOrder
	public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(long userId, long goodsId) {
		//return orderDao.getMiaoshaOrderByUserIdGoodsId(userId, goodsId);//原始方案，直接从数据库中取
		//从缓存Redis中取（因为在创建订单的时候同时把它存入了缓存Redis中哒）
		return redisService.get(OrderKey.getMiaoshaOrderByUidGid, ""+userId+"_"+goodsId, MiaoshaOrder.class);
	}

}
