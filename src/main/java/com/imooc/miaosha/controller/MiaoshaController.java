package com.imooc.miaosha.controller;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.imooc.miaosha.access.AccessLimit;
import com.imooc.miaosha.bean.MiaoshaOrder;
import com.imooc.miaosha.bean.MiaoshaUser;
import com.imooc.miaosha.rabbitmq.MQSender;
import com.imooc.miaosha.rabbitmq.MiaoshaMessage;
import com.imooc.miaosha.redis.GoodsKey;
import com.imooc.miaosha.redis.MiaoshaKey;
import com.imooc.miaosha.redis.OrderKey;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.result.Result;
import com.imooc.miaosha.service.GoodsService;
import com.imooc.miaosha.service.MiaoshaService;
import com.imooc.miaosha.service.LoginService;
import com.imooc.miaosha.service.OrderService;
import com.imooc.miaosha.vo.GoodsVo;

/**
 * 这是与秒杀核心功能相关的controller
 */

@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean {

	@Autowired
	LoginService userService;
	
	@Autowired
	RedisService redisService;
	
	@Autowired
	GoodsService goodsService;
	
	@Autowired
	OrderService orderService;
	
	@Autowired
	MiaoshaService miaoshaService;
	
	@Autowired
	MQSender sender;

	/**
	 * 0.系统初始化：把所有的秒杀商品信息加载到缓存Redis中
	 * */

	//使用HashMap，用于对缓存中的商品进行标记（即所谓的内存标记）
	private HashMap<Long, Boolean> localOverMap =  new HashMap<Long, Boolean>();
//0.1把商品信息先存入Redis中（用于预减库存）
	public void afterPropertiesSet() throws Exception {
		List<GoodsVo> goodsList = goodsService.listGoodsVo();
		if(goodsList == null) {
			return;
		}
		for(GoodsVo goods : goodsList) {//遍历所有商品
			//0.1.11把其都存入Redis中
			redisService.set(GoodsKey.getMiaoshaGoodsStock, ""+goods.getId(), goods.getStockCount());
			//0.1.2对存入缓存中的每一个商品都先标记为false
			localOverMap.put(goods.getId(), false);
		}
	}

	//0.2重置（按钮），相当于后台管理系统，一键还原到最初状态（非重点）
	@RequestMapping(value="/reset", method=RequestMethod.GET)
    @ResponseBody
    public Result<Boolean> reset(Model model) {
		List<GoodsVo> goodsList = goodsService.listGoodsVo();
		for(GoodsVo goods : goodsList) {
			goods.setStockCount(10);
			redisService.set(GoodsKey.getMiaoshaGoodsStock, ""+goods.getId(), 10);
			localOverMap.put(goods.getId(), false);
		}
		redisService.delete(OrderKey.getMiaoshaOrderByUidGid);
		redisService.delete(MiaoshaKey.isGoodsOver);
		miaoshaService.reset(goodsList);
		return Result.success(true);
	}

	//1.（用户点击秒杀按钮后）先要获取秒杀地址url（为了安全考虑，秒杀地址设置成了动态的，因此要临时获取）
	//先要通过验证码的校验才能获取
	@AccessLimit(seconds=5, maxCount=5, needLogin=true)//自定义的“访问限制”注解，5秒钟内只能请求5次
	@RequestMapping(value="/path", method=RequestMethod.GET)
	@ResponseBody
	public Result<String> getMiaoshaPath(HttpServletRequest request, MiaoshaUser user,
										 @RequestParam("goodsId")long goodsId,
										 @RequestParam(value="verifyCode", defaultValue="0")int verifyCode
	) {
		if(user == null) {//用户为空（细节不能丢）
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		//1.1先要校验验证码
		boolean check = miaoshaService.checkVerifyCode(user, goodsId, verifyCode);
		if(!check) {
			return Result.error(CodeMsg.REQUEST_ILLEGAL);
		}
		//1.2再获取秒杀url地址
		String path  =miaoshaService.createMiaoshaPath(user, goodsId);
		return Result.success(path);
	}

	//2.（当用户点击获取验证码按钮时）获取验证码
	@RequestMapping(value="/verifyCode", method=RequestMethod.GET)
	@ResponseBody
	public Result<String> getMiaoshaVerifyCod(HttpServletResponse response,MiaoshaUser user,
											  @RequestParam("goodsId")long goodsId) {
		if(user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		try {
			BufferedImage image  = miaoshaService.createVerifyCode(user, goodsId);
			OutputStream out = response.getOutputStream();
			ImageIO.write(image, "JPEG", out);
			out.flush();
			out.close();
			return null;
		}catch(Exception e) {
			e.printStackTrace();
			return Result.error(CodeMsg.MIAOSHA_FAIL);
		}
	}
	
	/**
	 * 3.开始秒杀
	 * 秒杀接口优化之前：QPS:1306
	 * 5000 * 10
	 * 优化之后：QPS: 2114（理论上效果是非常明显的，与服务器性能也有关）
	 * */
    @RequestMapping(value="/{path}/do_miaosha", method=RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model,MiaoshaUser user,
    		@RequestParam("goodsId")long goodsId,
    		@PathVariable("path") String path) {
    	model.addAttribute("user", user);
    	if(user == null) {//用户不存在
    		return Result.error(CodeMsg.SESSION_ERROR);
    	}
    	//3.1先验证秒杀地址url
    	boolean check = miaoshaService.checkPath(user, goodsId, path);
    	if(!check){
    		return Result.error(CodeMsg.REQUEST_ILLEGAL);
    	}
    	//3.2（重点）内存标记，减少redis访问
    	boolean over = localOverMap.get(goodsId);
    	if(over) {
    		return Result.error(CodeMsg.MIAO_SHA_OVER);
    	}
    	//3.3（重点）预减库存（在Redis中进行，因为在系统初始化的时候已经加载进来了）
		//decr：即减1操作
    	long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, ""+goodsId);//10
    	if(stock < 0) {
    		 localOverMap.put(goodsId, true);//若Redis库存为0，则之后的请求则直接拒绝，返回“秒杀结束”，减少redis访问
    		return Result.error(CodeMsg.MIAO_SHA_OVER);
    	}
    	//3.4先判断该用户之前是否已经秒杀过（即是否重复秒杀）
    	MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
    	if(order != null) {//若该用户有订单，则说明重复秒杀了，禁止之后的操作
    		return Result.error(CodeMsg.REPEATE_MIAOSHA);
    	}
    	//3.5若用户是第一次秒杀，则入队，进行异步处理（重点），此时先给用户返回：排队中（但秒杀是否成功尚未可知）
    	MiaoshaMessage mm = new MiaoshaMessage();//先创建“秒杀信息”的消息对象
		//再设置对应的值
    	mm.setUser(user);
    	mm.setGoodsId(goodsId);
    	sender.sendMiaoshaMessage(mm);
    	return Result.success(0);//先对用户先显示“排队中”，之后用户可以根据结果查询按钮查看秒杀结果
    	/*
    	//这是优化之前的版本
    	//判断库存
    	GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);//10个商品，req1 req2
    	int stock = goods.getStockCount();
    	if(stock <= 0) {
    		return Result.error(CodeMsg.MIAO_SHA_OVER);
    	}
    	//判断是否已经秒杀到了
    	MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
    	if(order != null) {
    		return Result.error(CodeMsg.REPEATE_MIAOSHA);
    	}
    	//减库存 下订单 写入秒杀订单表（这三步必须是同时成功，或者同时失败，因此需要在service层开启事务）
    	OrderInfo orderInfo = miaoshaService.miaosha(user, goods);
        return Result.success(orderInfo);
        */
    }
    
    /**
	 * 4.（用户）获取秒杀结果（因为之前返回的是“排队中”，还不知道具体结果）
	 * 有三种：
     * orderId：成功，返回订单编号
     * -1：秒杀失败
     * 0： （依然）排队中
     * */
    @RequestMapping(value="/result", method=RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model,MiaoshaUser user,
    		@RequestParam("goodsId")long goodsId) {
    	model.addAttribute("user", user);
    	if(user == null) {
    		return Result.error(CodeMsg.SESSION_ERROR);
    	}
    	long result = miaoshaService.getMiaoshaResult(user.getId(), goodsId);//获取秒杀结果
    	return Result.success(result);//返回秒杀结果
    }


}
