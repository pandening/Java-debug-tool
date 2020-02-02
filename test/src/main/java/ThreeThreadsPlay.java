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


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreeThreadsPlay {

    private static AtomicInteger count = new AtomicInteger(0);
    private static final int limit = 100;
    private static volatile boolean sf = false;

    private static CountDownLatch t1c = new CountDownLatch(0);
    private static CountDownLatch t2c = new CountDownLatch(1);
    private static CountDownLatch t3c = new CountDownLatch(1);

    private static Map<Integer, CountDownLatch> CDMap = new HashMap<>();
    static {
        CDMap.put(0, t1c);
        CDMap.put(1, t2c);
        CDMap.put(2, t3c);
        CDMap.put(3, t1c);
    }

    public static void main(String[] args) {

        for (int i = 0; i < 3; i ++) {
            new Thread(new CountRunner(i)).start();
        }

    }

    static class CountRunner implements Runnable {
        private int id;
        CountRunner(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            while (!sf) {
                // wait
                if (waitDC()) {
                    break;
                }

                // work
                work();

                // check & reset
                checkAndReset();

                // notify
                notifyDC();
            }
        }

        private boolean waitDC() {
            CountDownLatch cd = CDMap.get(id);
            try {
                cd.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (limit == count.get()) {
                // notify
                cd = CDMap.get(id + 1);
                cd.countDown();
                return true;
            }
            return false;
        }

        private void checkAndReset() {
            CountDownLatch cd;
            if (limit == count.get()) {
                sf = true;
                cd = new CountDownLatch(0);
            } else {
                cd = new CountDownLatch(1);
            }
            CDMap.put(id, cd);
            if (id == 0) {
                CDMap.put(3, cd);
            }
        }

        private void work() {
            System.out.println(Thread.currentThread().getName() + ":" + count.incrementAndGet());
        }

        private void notifyDC() {
            CountDownLatch cd = CDMap.get(id + 1);
            cd.countDown();
        }

    }

}
