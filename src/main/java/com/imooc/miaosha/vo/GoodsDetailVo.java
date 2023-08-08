package com.imooc.miaosha.vo;

import com.imooc.miaosha.bean.MiaoshaUser;

//GoodsDetailVo：即每一个具体的秒杀商品的详情对象（不仅包含了其基本的详情信息，还包含了秒杀状态和倒计时时间，因此使用了VO对象）
//用于：当用户点击进入到某一个秒杀商品时，把此秒杀商品的详细信息展示给用户
public class GoodsDetailVo {
    private int miaoshaStatus = 0;//秒杀状态
    private int remainSeconds = 0;//秒杀倒计时时间
    private GoodsVo goods;//秒杀商品的信息（也是一个VO对象）
    private MiaoshaUser user;//（秒杀的）用户信息

    public int getMiaoshaStatus() {
        return miaoshaStatus;
    }

    public void setMiaoshaStatus(int miaoshaStatus) {
        this.miaoshaStatus = miaoshaStatus;
    }

    public int getRemainSeconds() {
        return remainSeconds;
    }

    public void setRemainSeconds(int remainSeconds) {
        this.remainSeconds = remainSeconds;
    }

    public GoodsVo getGoods() {
        return goods;
    }

    public void setGoods(GoodsVo goods) {
        this.goods = goods;
    }

    public MiaoshaUser getUser() {
        return user;
    }

    public void setUser(MiaoshaUser user) {
        this.user = user;
    }
}
