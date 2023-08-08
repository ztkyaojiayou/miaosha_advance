package com.imooc.miaosha.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imooc.miaosha.bean.MiaoshaOrder;
import com.imooc.miaosha.bean.MiaoshaUser;
import com.imooc.miaosha.bean.OrderInfo;
import com.imooc.miaosha.redis.MiaoshaKey;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.util.MD5Util;
import com.imooc.miaosha.util.UUIDUtil;
import com.imooc.miaosha.vo.GoodsVo;

/**
 * 这是与秒杀核心功能相关的controller
 */


/**
 * 秒杀核心方法（掌握）
 * 注意：秒杀项目的重点并不是说是很多用户抢同一个商品，谁抢到了或者谁先抢到；
 * 我们关注的是当大量用户并发时，我们的服务器是否承受得住，是否会出错，并不关心到底是谁抢到，只要是被抢到了且能正常下单且服务器没有崩溃即可
 * 因此，我们从三个角度入手：
 * 角度1：从前端入手，各种加缓存
 * 角度2：从后端入手，利用消息队列（MQ）进行异步下单（代码级）
 * 角度3：从硬件入手，采用Nginx实现反向代理和负载均衡，每个Nginx再接若干个Tomcat服务器集群，通过这样的横向拓展，一般可以承载八万级别的并发场景，若为千万级的并发场景，则可以使用LVS实现（硬件级）
 */
@SuppressWarnings("restriction")
@Service
public class MiaoshaService {
	
	@Autowired
	GoodsService goodsService;
	
	@Autowired
	OrderService orderService;
	
	@Autowired
	RedisService redisService;


	//1.（验证码校验通过后）再创建动态秒杀url地址
	public String createMiaoshaPath(MiaoshaUser user, long goodsId) {
		if(user == null || goodsId <=0) {
			return null;
		}
		//使用uuid创建，再利用md5加密一次，存入Redis中（用于后面检验，过期时间是60s）
		String str = MD5Util.md5(UUIDUtil.uuid()+"123456");
		redisService.set(MiaoshaKey.getMiaoshaPath, ""+user.getId() + "_"+ goodsId, str);
		return str;
	}
	//2.（先）创建验证码（是一个简单的四则运算式子，具体实现不重要）
	public BufferedImage createVerifyCode(MiaoshaUser user, long goodsId) {
		if(user == null || goodsId <=0) {
			return null;
		}
		int width = 80;
		int height = 32;
		//create the image
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		// set the background color
		g.setColor(new Color(0xDCDCDC));
		g.fillRect(0, 0, width, height);
		// draw the border
		g.setColor(Color.black);
		g.drawRect(0, 0, width - 1, height - 1);
		// create a random instance to generate the codes
		Random rdm = new Random();
		// make some confusion
		for (int i = 0; i < 50; i++) {
			int x = rdm.nextInt(width);
			int y = rdm.nextInt(height);
			g.drawOval(x, y, 0, 0);
		}
		// generate a random code
		String verifyCode = generateVerifyCode(rdm);
		g.setColor(new Color(0, 100, 0));
		g.setFont(new Font("Candara", Font.BOLD, 24));
		g.drawString(verifyCode, 8, 24);
		g.dispose();
		//把验证码的值存到redis中，用于后面校验
		int rnd = calc(verifyCode);
		redisService.set(MiaoshaKey.getMiaoshaVerifyCode, user.getId()+","+goodsId, rnd);
		//返回：验证码图片
		return image;
	}


	//3.检查秒杀url是否有效
	public boolean checkPath(MiaoshaUser user, long goodsId, String path) {
		if(user == null || path == null) {
			return false;
		}
		//比较传入的path和缓存在Redis中的path是否相等即可
		String pathOld = redisService.get(MiaoshaKey.getMiaoshaPath, ""+user.getId() + "_"+ goodsId, String.class);
		return path.equals(pathOld);//若相等，说明url有效
	}

	//4.检查用户输入的验证码是否正确
	public boolean checkVerifyCode(MiaoshaUser user, long goodsId, int verifyCode) {
		if(user == null || goodsId <=0) {
			return false;
		}
		//同样的，比较用户填的值和缓存中的值是否相等即可
		Integer codeOld = redisService.get(MiaoshaKey.getMiaoshaVerifyCode, user.getId()+","+goodsId, Integer.class);
		if(codeOld == null || codeOld - verifyCode != 0 ) {
			return false;
		}
		//用完就删除这个值，用于下次复用
		redisService.delete(MiaoshaKey.getMiaoshaVerifyCode, user.getId()+","+goodsId);
		return true;
	}


	//5.开始秒杀，即减库存、下订单、写入秒杀订单表
	//（由于这三步必须是同时成功，或者同时失败，因此都需要使用事务）
	@Transactional //此注解即表示开启事务
	public OrderInfo miaosha(MiaoshaUser user, GoodsVo goods) {
		//5.1扣减库存（返回：是否成功）
		boolean success = goodsService.reduceStock(goods);
		//5.2再根据库存是否扣减成功，决定是否创建订单
		if(success) {
			//5.2.1若扣减成功，则开始创建订单
			return orderService.createOrder(user, goods);
		}else {//5.2.2否则，说明已经被别人秒杀了，把该商品存入Redis中，做一个“已被其他用户秒杀完毕了”的标记
			setGoodsOver(goods.getId());
			return null;//此时，没有订单可以返回，于是返回null
		}
	}

	//6.（用户）获取秒杀结果方法的具体实现
	public long getMiaoshaResult(Long userId, long goodsId) {
		MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(userId, goodsId);
		//6.1若订单存在，则表明秒杀成功，返回订单编号
		if(order != null) {
			return order.getOrderId();
		}else {//6.2否则，要先判断该商品是否已经售罄
			boolean isOver = getGoodsOver(goodsId);
			if(isOver) {
				return -1;//6.2.1若售罄，则秒杀失败，返回-1
			}else {
				return 0;//6.2.2否则，还有机会，继续（依然）排队中，返回0
			}
		}
	}
	//6.2判断该商品是否已经被其他用户秒杀完毕了的具体实现
    //先把已经抢到的商品存入Redis（作“判断该商品是否已经售罄”的中介）
	private void setGoodsOver(Long goodsId) {
		redisService.set(MiaoshaKey.isGoodsOver, ""+goodsId, true);
	}
	//则：若该商品在缓存中，则说明已经被其他用户秒杀完毕了，返回给用户秒杀失败
	private boolean getGoodsOver(long goodsId) {
		return redisService.exists(MiaoshaKey.isGoodsOver, ""+goodsId);
	}


	public void reset(List<GoodsVo> goodsList) {
		goodsService.resetStock(goodsList);
		orderService.deleteOrders();
	}

	private static int calc(String exp) {
		try {
			ScriptEngineManager manager = new ScriptEngineManager();
			ScriptEngine engine = manager.getEngineByName("JavaScript");
			return (Integer)engine.eval(exp);
		}catch(Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * + - *
	 * 创建验证码的具体实现
	 * */
	private static char[] ops = new char[] {'+', '-', '*'};
	private String generateVerifyCode(Random rdm) {
		int num1 = rdm.nextInt(10);
		int num2 = rdm.nextInt(10);
		int num3 = rdm.nextInt(10);
		char op1 = ops[rdm.nextInt(3)];
		char op2 = ops[rdm.nextInt(3)];
		String exp = ""+ num1 + op1 + num2 + op2 + num3;
		return exp;
	}

}
