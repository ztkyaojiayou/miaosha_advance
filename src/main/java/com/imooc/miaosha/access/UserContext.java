package com.imooc.miaosha.access;

import com.imooc.miaosha.bean.MiaoshaUser;

public class UserContext {
    /**
     * （1）ThreadLocal是JDK包提供的，是多线程情况下，保证线程安全的一种方式,它提供线程本地变量，
     * 如果创建一个ThreadLocal对象，那么每个访问这个变量的线程都会有这个变量的一个副本，
     * 在实际多线程操作的时候，操作的是自己本地内存中的变量，
     * 从而该对象中的数据/对象既可以同时被多个请求/线程/用户处理又规避了线程安全问题，贼爽
     *
     * （2）这里的目的就是使得多个线程安全地获取MiaoshaUser对象
     */
    //1.先创建一个ThreadLocal对象
    private static ThreadLocal<MiaoshaUser> userHolder = new ThreadLocal<MiaoshaUser>();

    //2.再往这个对象里面塞值
    public static void setUser(MiaoshaUser user) {
        userHolder.set(user);
    }

    //3.然后就可以供多个线程安全地获取此对象啦，完美解决问题
    public static MiaoshaUser getUser() {
        return userHolder.get();
    }

}
