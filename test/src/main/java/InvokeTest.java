//
//  ========================================================================
//  Copyright (c) 2018-2019 HuJian/Pandening soft collection.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the #{license} Public License #{version}
//  EG:
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  You should bear the consequences of using the software (named 'java-debug-tool')
//  and any modify must be create an new pull request and attach an text to describe
//  the change detail.
//  ========================================================================
//


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 *  测试Method.invoke和直接方法调用之间的性能差距
 *  设：n代表方法执次数 ， t1代表直接方法调用耗时， t2代表通过Method.invoke调用方法的耗时
 *
 *  -------------------------------------------+
 *  | n        |   t1    |   t2   |  diff      |
 *  +------------------------------------------+
 *  | 1000     |    1    |    3   |      2     |
 *  +------------------------------------------+
 *  | 10000    |         |        |            |
 *  +------------------------------------------+
 *  | 100000   |         |        |            |
 *  +------------------------------------------+
 *  | 1000000  |         |        |            |
 *  +------------------------------------------+
 *  | 10000000 |         |        |            |
 *  +------------------------------------------+
 *  | 100000000|         |        |            |
 *  +------------------------------------------+
 *
 *
 */
public class InvokeTest {

    private static Method me = null;

    static {
        try {
            me = InvokeTest.class.getDeclaredMethod("call");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    static void call() {

    }

    static void test1(long callTime) {
        long cost = 0;
        for (int k = 0; k < 10; k ++) {
            long startMills = System.currentTimeMillis();
            for (int i = 0; i < callTime; i ++) {
                call();
            }
            cost += System.currentTimeMillis() - startMills;
        }
        System.out.println("avg cost:" + (cost / 10.0));
    }

    static void test2(long callTime) {
        long cost = 0;
        for (int k = 0; k < 10; k ++) {
            long startMills = System.currentTimeMillis();
            for (int i = 0; i < callTime; i ++) {
                try {
                    me.invoke(null);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            cost += System.currentTimeMillis() - startMills;
        }
        System.out.println("avg cost:" + (cost / 10.0));
    }

    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException {
        long callTime = 100000000;
        test1(callTime);
        test2(callTime);

    }

}
