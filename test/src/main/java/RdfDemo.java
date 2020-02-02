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

/**
 *
 *  rdf -p RdfDemo:/Users/hujian06/github/java-debug-tool/test/target/classes/RdfDemo.class
 *  back -c RdfDemo
 *
 */
public class RdfDemo {

    public int call(int in) {
        //throw new UnsupportedOperationException("unSupport method call");
        return in;
    }

    public static void main(String[] args) {

        Random random = new Random(47);
        RdfDemo rdfDemo = new RdfDemo();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        TimeUnit.SECONDS.sleep(2);
                        int ret =rdfDemo.call(random.nextInt(10));
                        System.out.println(ret);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }).start();
    }

}
