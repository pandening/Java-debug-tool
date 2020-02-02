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


import io.javadebug.core.monitor.gc.GarbageCollectMonitorCollector;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ManagementFactoryTest {

    public static void main(String[] args) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (1 == 1) {
                    byte[] bytes = new byte[1024 * 1024 * 10];
                    System.out.println("->" + bytes.length);
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    System.out.println(GarbageCollectMonitorCollector.GARBAGE_COLLECT_MONITOR_COLLECTOR.collect().event());

                    List<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
                    //System.out.println(garbageCollectorMXBeans);

                }
            }
        }).start();


//        OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
//        if (mxBean instanceof com.sun.management.OperatingSystemMXBean) {
//            com.sun.management.OperatingSystemMXBean mxBean1 = (com.sun.management.OperatingSystemMXBean) mxBean;
//            mxBean1.getTotalPhysicalMemorySize();
//        }
//        mxBean.getName();
//
//        List<MemoryPoolMXBean>  memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
//        System.out.println(memoryPoolMXBeans);

    }


}
