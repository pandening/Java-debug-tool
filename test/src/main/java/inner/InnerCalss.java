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


/**
 *   Copyright © 2019-XXX HJ All Rights Reserved
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
//  Author : HJ


package inner;

import java.util.function.Function;

public class InnerCalss {


    class InnerA {

        private int in(int c) {
            return c;
        }

    }

    static class InnerB {
        private int in(int c) {
            return c;
        }
    }

    public static void main(String[] args) {

        InnerCalss innerCalss = new InnerCalss();

        InnerA innerA = innerCalss.new InnerA();

        InnerB innerB = new InnerB();

        System.out.println(innerA.in(1) + innerB.in(1));

        for (int i = 0; i < 10; i ++) {
            final int  j = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    InnerB innerB1 = new InnerB();

                    InnerA innerA1 = innerCalss.new InnerA();

                    System.out.println(innerA1.in(10 + j) + innerB1.in(100 + j));
                }
            }).start();
        }

        Function<String, Integer> intConverter = s -> {
            if (s == null || s.isEmpty()) {
                return -1;
            }
            for (char c : s.toCharArray()) {
                if (c < '0' || c > '9') {
                    return -1;
                }
            }
            return Integer.parseInt(s);
        };

        System.out.println(intConverter.apply("1234"));

    }

}
