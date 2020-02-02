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


package io.javadebug.core.monitor.mem;

import io.javadebug.core.data.JVMMemoryInfo;
import io.javadebug.core.monitor.CounterEvent;
import io.javadebug.core.monitor.MonitorCollector;
import io.javadebug.core.monitor.MonitorConsume;
import io.javadebug.core.monitor.MonitorHandler;
import io.javadebug.core.monitor.clazz.ClassMonitorCollector;
import io.javadebug.core.monitor.gc.GarbageCollectMonitorCollector;
import io.javadebug.core.utils.MemoryUtils;
import io.javadebug.core.utils.UTILS;

public enum MemoryMonitorCollector implements MonitorCollector {
    MEMORY_MONITOR_COLLECTOR
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
        return new CounterEvent() {
            @Override
            public String type() {
                return "Dynamic JVM Memory Monitor";
            }

            @Override
            public String event() {

                StringBuilder esb = new StringBuilder();

                // base mem area
                JVMMemoryInfo jvmMemoryInfo = MemoryUtils.getMemory();
                esb.append(jvmMemoryInfo.toString());

                // class area
                CounterEvent classCE = ClassMonitorCollector.CLASS_MONITOR_COLLECTOR.collect();
                if (!UTILS.isNullOrEmpty(classCE.event())) {
                    esb.append("\n").append(classCE.event());
                }

                // gc area
                CounterEvent gcCE = GarbageCollectMonitorCollector.SIMPLE_GARBAGE_COLLECT_MONITOR_COLLECTOR.collect();
                if (!UTILS.isNullOrEmpty(gcCE.event())) {
                    esb.append("\n").append(gcCE.event());
                }

                return esb.toString();
            }
        };
    }

    public static void main(String[] args) {

        System.out.println(MEMORY_MONITOR_COLLECTOR.collect().event());

    }

}
