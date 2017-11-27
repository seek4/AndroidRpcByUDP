package me.yangtong.udprpc.util;

/**
 * Created by yangtong on 2017/11/7.
 */

public abstract class Runnable1<A> implements Runnable{

    public A mP1;
    public Runnable1(A a){
        this.mP1 = a;
    }

}
