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


import io.javadebug.core.utils.JacksonUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class R {

//    private static final Logger LOGGER = LoggerFactory.getLogger(R.class);

    private int input;
    private static final String hello = "world";

    private void hehe() {

        String hehe = "hehe";
        System.out.println(hehe);

    }

    public static class MMM {
//        @Override
//        public String toString() {
//            return "MMM{" +
//                           "data='" + data + '\'' +
//                           '}';
//        }

        private String data = "default";

        public MMM(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    public int call(int i, int j, C c, int[] dd, List<Long> e, long[] ff, Long[] g, List<MMM> mmmList) {

//        LOGGER.error("i am log4j, test print log here ~");

        List<MMM> mmmList1 = mmmList;
        System.out.println(mmmList1);
        if (c.getA() < 0) {
            throw new NullPointerException("test claw");
        }

        if (dd != null) {
            System.out.println("array:" + Arrays.asList(dd));
        }

        int el = -1;
        if (e != null) {
            el = e.size();
            System.out.println("list:" + e);
        }

        if (ff != null) {
            el = ff.length;
        }

        if (g != null) {
            el = g.length;
        }

        hehe();

        Integer sa = 1;
        input = i + j;
        int ii = 0;
        int ij = ii;
        Integer jk = sa;
        float f = 1.0f;
        double d = 0.123d;
        String name = "hello" + i + "," + j;
        List<Integer> list = new ArrayList<>();
        if (i > 5) {
            list.add(i);
            list.add(i + 1);
        }
        if (i > 8) {
            list.add(8);
        }
        if ( i <= 5) {
            list.add(5);
        }
        if (i + j > 10) {
            throw new IllegalArgumentException(" i + j <= 10:" + i + j);
        }

        return list.size();
    }

    public static void main(String[] args) {
        int[] array = new int[]{1,2,3};
        System.out.println(Arrays.asList(array));

        int a = 10, b = 100;
        C c = new C();

        Object[] objects = new Object[]{a, b, c, null, null, null, null};
        String serialize = JacksonUtils.serialize(objects);
        System.out.println(serialize);

        R r = new R();
        Random random = new Random(47);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    // 1kb
                    byte[] bytes = new byte[1024];
                    for (int i = 0; i < b; i ++) {
                        bytes[i] = (byte) i;
                    }
                    //System.out.println(bytes.length);
                    try {
                        List<MMM> mmms = new ArrayList<>();
                        mmms.add(new MMM("test"));
                        System.out.println(r.call(random.nextInt(10), random.nextInt(10), new C(), null, null, null, null, mmms));
                    } catch (Exception e) {
                        //System.out.println(e.getMessage());
                    } finally {
                        try {
                            TimeUnit.MILLISECONDS.sleep(2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, "test-test-R-worker").start();

    }

}
