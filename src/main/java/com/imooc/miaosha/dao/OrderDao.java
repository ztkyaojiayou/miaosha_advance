package com.imooc.miaosha.dao;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;

import com.imooc.miaosha.bean.MiaoshaOrder;
import com.imooc.miaosha.bean.OrderInfo;

/**
 * 这是与订单相关的DAO
 */

@Mapper
public interface OrderDao {

	//1.根据用户ID和商品ID查询订单
	@Select("select * from miaosha_order where user_id=#{userId} and goods_id=#{goodsId}")
	public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(@Param("userId")long userId, @Param("goodsId")long goodsId);

	//2.把订单插入到订单详情表order_info中，生成订单
	@Insert("insert into order_info(user_id, goods_id, goods_name, goods_count, goods_price, order_channel, status, create_date)values("
			+ "#{userId}, #{goodsId}, #{goodsName}, #{goodsCount}, #{goodsPrice}, #{orderChannel},#{status},#{createDate} )")
	@SelectKey(keyColumn="id", keyProperty="id", resultType=long.class, before=false, statement="select last_insert_id()")
	public long insert(OrderInfo orderInfo);

	//3.再额外生成一个秒杀订单到miaosha_order表中，其中只包含商品ID、订单详情ID和用户ID
	//作用：利用商品ID（goodsId）、用户ID（userId）生成一个唯一索引，防止超卖
	@Insert("insert into miaosha_order (user_id, goods_id, order_id)values(#{userId}, #{goodsId}, #{orderId})")
	public int insertMiaoshaOrder(MiaoshaOrder miaoshaOrder);

	//4.根据订单ID查询订单OrderInfo
	@Select("select * from order_info where id = #{orderId}")
	public OrderInfo getOrderById(@Param("orderId")long orderId);

	//
	@Delete("delete from order_info")
	public void deleteOrders();

	//
	@Delete("delete from miaosha_order")
	public void deleteMiaoshaOrders();

	
}
