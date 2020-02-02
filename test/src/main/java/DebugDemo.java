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


import java.util.concurrent.TimeUnit;

public class DebugDemo {

//    private int solve(int in) {
//        if (in < 0) {
//            throw new IllegalArgumentException("invalid params");
//        } else if (in == 0) {
//            return 0;
//        } else {
//            return in * 100;
//        }
//    }

    private int solve(int in) {
        try {
            report("param", in);
            if (in < 0) {
                report("invoke", 17);
                throw new IllegalArgumentException("invalid params");
            } else if (in == 0) {
                report("invoke", 19);
                return 0;
            } else {
                report("invoke", 21);
                report("return", in * 100);
                return in * 100;
            }
        } catch (Exception e) {
            report("exception", e);
            throw e;
        }
    }

    static void report(Object ... p) {

    }

    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (1 == 1) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

}
