package com.imooc.miaosha.vo;

import java.util.Date;

import com.imooc.miaosha.bean.Goods;

/**
 * GoodsVo：用于把goods表和miaosha_goods表中同一秒杀商品的信息聚合起来
 * 这是当用户登录之后，所看到的秒杀商品的列表的信息
 * （回顾VO对象：简单的讲，就是一个对象中还包含另一个对象的信息）
 */
public class GoodsVo extends Goods {
    private Double miaoshaPrice;
    private Integer stockCount;
    private Date startDate;
    private Date endDate;

    public Integer getStockCount() {
        return stockCount;
    }

    public void setStockCount(Integer stockCount) {
        this.stockCount = stockCount;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Double getMiaoshaPrice() {
        return miaoshaPrice;
    }

    public void setMiaoshaPrice(Double miaoshaPrice) {
        this.miaoshaPrice = miaoshaPrice;
    }
}
