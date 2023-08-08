package com.imooc.miaosha.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.imooc.miaosha.bean.MiaoshaGoods;
import com.imooc.miaosha.vo.GoodsVo;
/**
 * 这是与秒杀商品列表相关的DAO（即与数据库相关的操作）
 */

@Mapper
public interface GoodsDao {

	//1.先查询/获取秒杀商品列表信息，使用了联合查询
	//（由于秒杀商品表中只有几个关键信息，与此商品相关的具体信息在goods表中，因此用到GoodsVo，把这两个表的信息关联起来）
	@Select("select g.*,mg.stock_count, mg.start_date, mg.end_date,mg.miaosha_price from miaosha_goods mg left join goods g on mg.goods_id = g.id")
	public List<GoodsVo> listGoodsVo();

    //2.再根据goodsId获取此秒杀商品的具体信息，也使用了联合查询
	@Select("select g.*,mg.stock_count, mg.start_date, mg.end_date,mg.miaosha_price from miaosha_goods mg left join goods g on mg.goods_id = g.id where g.id = #{goodsId}")
	public GoodsVo getGoodsVoByGoodsId(@Param("goodsId")long goodsId);

	//3.扣减/更新库存（当用户秒杀成功之后，更新库存，即 库存-1）
	@Update("update miaosha_goods set stock_count = stock_count - 1 " +
			"where goods_id = #{goodsId} and stock_count > 0")
	public int reduceStock(MiaoshaGoods g);

	//4.
	@Update("update miaosha_goods set stock_count = #{stockCount} where goods_id = #{goodsId}")
	public int resetStock(MiaoshaGoods g);
	
}
