package com.imooc.miaosha.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.imooc.miaosha.dao.GoodsDao;
import com.imooc.miaosha.bean.MiaoshaGoods;
import com.imooc.miaosha.vo.GoodsVo;

/**
 * 这是与秒杀商品列表相关的service（即相关业务的具体实现）
 */

@Service
public class GoodsService {
	
	@Autowired
	GoodsDao goodsDao;

	//1.获取秒杀商品列表信息
	public List<GoodsVo> listGoodsVo(){
		return goodsDao.listGoodsVo();
	}
    //2.根据商品ID获取此秒杀商品的详情页
	public GoodsVo getGoodsVoByGoodsId(long goodsId) {
		return goodsDao.getGoodsVoByGoodsId(goodsId);
	}

	//3.减库存
	//返回的是boolean类型，即判断是否减库存成功，因为若失败，则不需要再创建订单
	public boolean reduceStock(GoodsVo goods) {
		MiaoshaGoods g = new MiaoshaGoods();
		g.setGoodsId(goods.getId());
		//调用数据库dao层减库存（为什么不是调用缓存减库存呢？因为此时请求量已经很少了，数据库压力并不大，
		//而且最终本来就是要从数据库中减库存，Redis那儿只是预减库存而已，并不是真正的减库存）
		int ret = goodsDao.reduceStock(g);
		return ret > 0;
	}

	//4.重置库存
	public void resetStock(List<GoodsVo> goodsList) {
		for(GoodsVo goods : goodsList ) {
			MiaoshaGoods g = new MiaoshaGoods();
			g.setGoodsId(goods.getId());
			g.setStockCount(goods.getStockCount());
			goodsDao.resetStock(g);
		}
	}
	
	
	
}
