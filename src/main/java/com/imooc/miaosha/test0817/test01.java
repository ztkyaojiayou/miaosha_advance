package com.imooc.miaosha.test0817;

/**
 * @Author: tk.zou
 * @Description:
 * @Date: 2021-08-17 17:22
 * @Version: 1.0.0
 */


public class test01 {

    public static void main(String[] args) {
        test01 test = new test01();
        test.t1();
        test01.t3();
    }

    public void t1() {
        t4();
        System.out.println("t1");
    }

    public void t2() {
//        test01 test01 = new test01();
//        test01.t1();
        //在类中调用该类中的其他方法时，不需要new了，直接使用即可
        t1();
        System.out.println("t2");
    }

    public static void t3() {
        System.out.println("t3");
    }

    public static void t4() {
        test01 test01 = new test01();
//        test01.t1();//对象调用static方法当然肯定可以
        //静态方法只能调用静态方法
//        t1();
        t3();//静态方法只能调用静态方法
        System.out.println("t4");
    }

}
