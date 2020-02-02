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


package io.javadebug.core.monitor.load;

import io.javadebug.core.monitor.CounterEvent;
import io.javadebug.core.monitor.MonitorCollector;
import io.javadebug.core.monitor.MonitorConsume;
import io.javadebug.core.monitor.MonitorHandler;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public enum LoadAverageMonitorCollector implements MonitorCollector {
    LOAD_AVERAGE_MONITOR_COLLECTOR
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
                return "RealTime AvgLoad Monitor";
            }

            @Override
            public String event() {
                OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
                double systemAvgLoad = operatingSystem.getSystemLoadAverage();
                double jvmProcessAvgLoad = -1.0;
                int process = operatingSystem.getAvailableProcessors();
                if (operatingSystem instanceof com.sun.management.OperatingSystemMXBean) {
                    jvmProcessAvgLoad = ((com.sun.management.OperatingSystemMXBean) operatingSystem).getProcessCpuLoad();
                }
                String format = "%-30.30s " + "%10.10s";

                StringBuilder rsb = new StringBuilder();

                // available process
                rsb.append(String.format(format, "AvailableProcessors", process + "")).append("\n");

                // system avg load (load1)
                rsb.append(String.format(format, "SystemLoadAverage", systemAvgLoad + "")).append("\n");

                // jvm process load
                rsb.append(String.format(format, "ProcessCpuLoad", "" + jvmProcessAvgLoad)).append("\n");

                return rsb.toString();
            }
        };
    }

    public static void main(String[] args) {

        System.out.println(LOAD_AVERAGE_MONITOR_COLLECTOR.collect().event());

    }

}
