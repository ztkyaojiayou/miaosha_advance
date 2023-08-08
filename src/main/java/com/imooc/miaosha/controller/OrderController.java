package com.imooc.miaosha.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.imooc.miaosha.bean.MiaoshaUser;
import com.imooc.miaosha.bean.OrderInfo;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.result.Result;
import com.imooc.miaosha.service.GoodsService;
import com.imooc.miaosha.service.LoginService;
import com.imooc.miaosha.service.OrderService;
import com.imooc.miaosha.vo.GoodsVo;
import com.imooc.miaosha.vo.OrderDetailVo;

/**
 * 这是与订单相关的controller
 */
@Controller
@RequestMapping("/order")
public class OrderController {

	@Autowired
	LoginService userService;
	
	@Autowired
	RedisService redisService;
	
	@Autowired
	OrderService orderService;
	
	@Autowired
	GoodsService goodsService;
	
    @RequestMapping("/detail")
    @ResponseBody
	//根据订单ID获取订单详情（并不是秒杀订单）
    public Result<OrderDetailVo> info(Model model,MiaoshaUser user,
    		@RequestParam("orderId") long orderId) {
    	if(user == null) {//若用户为空
    		return Result.error(CodeMsg.SESSION_ERROR);
    	}
    	OrderInfo order = orderService.getOrderById(orderId);
    	if(order == null) {//若订单为空
    		return Result.error(CodeMsg.ORDER_NOT_EXIST);
    	}
    	long goodsId = order.getGoodsId();
    	//把该订单对应的商品详细信息查询出来
    	GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
    	OrderDetailVo vo = new OrderDetailVo();
    	//再把订单订单信息和商品信息一起塞到OrderDetailVo中
    	vo.setOrder(order);
    	vo.setGoods(goods);
    	//最后返回OrderDetailVo对象vo给客户端，用于填充html页面中相应动态字段，再展示给用户
    	//（这里用的也都是页面静态化技术，即前端页面都是直接从浏览器获取/跳转的，不需要通过后端跳转，后端只需要返给一些动态数据（对象）给前端即可）
		return Result.success(vo);
    }
    
}
