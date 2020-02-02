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


package io.javadebug.core.monitor.clazz;

import io.javadebug.core.monitor.CounterEvent;
import io.javadebug.core.monitor.MonitorCollector;
import io.javadebug.core.monitor.MonitorConsume;
import io.javadebug.core.monitor.MonitorHandler;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;

public enum ClassMonitorCollector implements MonitorCollector {
    CLASS_MONITOR_COLLECTOR
    ;

    /**
     * collect the event by your way, and the implement need to response real-time, do
     * not do scheduler or cache something in your implement, this function will be called
     * by {@link MonitorHandler}, as Producer, the {@link MonitorConsume} will consume
     * the event produced by this method;
     *
     * @return the origin event
     */
    @Override
    public CounterEvent collect() {
        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        long totalLoadedClassCount = classLoadingMXBean.getTotalLoadedClassCount();
        long loadedClassCount = classLoadingMXBean.getLoadedClassCount();
        long unloadedClassCount = classLoadingMXBean.getUnloadedClassCount();

        return new CounterEvent() {
            @Override
            public String type() {
                return "Dynamic monitor for Class Info";
            }

            @Override
            public String event() {
                StringBuilder sb = new StringBuilder();
                String format = "%-50.50s " + "%10.10s";

                // total loaded class count
                sb.append(String.format(format, "jvm.classloading.totalloaded.count", "" + totalLoadedClassCount)).append("\n");
                //sb.append("jvm.classloading.totalloaded.count   :").append(totalLoadedClassCount).append("\n");

                // current loaded class
                sb.append(String.format(format, "jvm.classloading.loaded.count", "" + loadedClassCount)).append("\n");
                //sb.append("jvm.classloading.loaded.count        :").append(loadedClassCount).append("\n");

                // unloaded class
                sb.append(String.format(format, "jvm.classloading.unloaded.count", "" +unloadedClassCount)).append("\n");
                //sb.append("jvm.classloading.unloaded.count      :").append(unloadedClassCount).append("\n");

                return sb.toString();
            }
        };
    }

    public static void main(String[] args) {

        System.out.println(CLASS_MONITOR_COLLECTOR.collect().event());

    }
}
