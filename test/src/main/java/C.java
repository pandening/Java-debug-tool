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


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class C {

    private int a;
    private static String hello = "hello";

    private void update(int aa, String ab) {
        a = aa;
        hello = ab;
    }

    private static void showFields(Object obj) throws Exception {
        for (Field field : obj.getClass().getDeclaredFields()) {

            if (!Modifier.isStatic(field.getModifiers())) {
                System.out.println(field.getName() + "=" + field.get(obj));
            } else {
                System.out.println(field.getName() + "=" + field.get(obj));
            }
        }
    }

    private static void testGetFields(C c) throws Exception {

        long cnt = 100000000L;
        int ret = 0;
        long start = System.currentTimeMillis();

        for (int i = 0; i < cnt; i ++) {
            ret = c.getA();
        }

        long cost = System.currentTimeMillis() - start;
        System.out.println("c =" + ret + " cost:" + (cost) + " avg:" + cost / (1.0 * cnt));

        Field field = c.getClass().getDeclaredField("a");
        start = System.currentTimeMillis();

        for (int i = 0; i < cnt; i ++) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
                ret = (int) field.get(c);
                field.setAccessible(false);
            } else {
                ret = (int) field.get(c);
            }
        }

        cost = System.currentTimeMillis() - start;
        System.out.println("c =" + ret + " cost:" + (cost) + " avg:" + cost / (1.0 * cnt));
    }

    public static void main(String[] args) throws Exception {

        C c = new C();

        showFields(c);

        c.update(10, "haha");

        showFields(c);

        testGetFields(c);


    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }
}
