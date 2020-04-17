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


import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Demo {

    private void say(String word, boolean tag, int rdm) {
        if (word == null) {
            word = "test say";
        }
        int length = word.length();
        if (tag) {
            length += 1;
        } else {
            length -= 1;
        }
        word += "@" + length;
        System.out.println(word);
        if (rdm > 5) {
            throw new IllegalStateException("test exception");
        }
    }

    private static final String[] list = {"a", "ab", "abc", "abcd"};

    public static void main(String[] args) {
        Demo demo = new Demo();
        Random random = new Random(47);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(;;) {
                    try {
                        demo.say(list[random.nextInt(4)], random.nextBoolean(), random.nextInt(10));
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "demo-thread").start();
    }


}
