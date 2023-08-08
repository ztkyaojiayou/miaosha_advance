package com.imooc.miaosha.bean;

import java.util.Date;

//秒杀商品信息bean（只不过是从普通商品表中把用于秒杀的商品单独抽离出来了）
public class MiaoshaGoods {
    private Long id;//相当于主键，且同时也是普通商品表中的ID
    private Long goodsId;//秒杀商品表中的商品的对应ID
    private Integer stockCount;//库存
    private Date startDate;//秒杀开始时间
    private Date endDate;//秒杀结束时间

    //对应的get和set方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

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
}
