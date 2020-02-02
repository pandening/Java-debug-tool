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


package io.javadebug.core.monitor.runtime;

import io.javadebug.core.monitor.CounterEvent;
import io.javadebug.core.monitor.MonitorCollector;
import io.javadebug.core.monitor.MonitorConsume;
import io.javadebug.core.monitor.MonitorHandler;
import io.javadebug.core.utils.SystemUtils;

public enum RuntimeMonitorCollector implements MonitorCollector {
    RUNTIME_MONITOR_COLLECTOR
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
                return "Runtime Information";
            }

            @Override
            public String event() {
                String format = "%-20.20s " + "%-50.50s";

                StringBuilder rsb = new StringBuilder();

                // jvm name
                String jvmName = String.format(format, "jvm name", SystemUtils.getName());
                rsb.append(jvmName).append("\n");

                // vm name
                String vmName = String.format(format, "vm name", SystemUtils.getVmName());
                rsb.append(vmName).append("\n");

                // vm version
                String vmVersion = String.format(format, "vm version", SystemUtils.getVmVersion());
                rsb.append(vmVersion).append("\n");

                // vm vendor
                String vmVendor = String.format(format, "vm vendor", SystemUtils.getVmVendor());
                rsb.append(vmVendor).append("\n");

                // spec name
                String specName = String.format(format, "spec name", SystemUtils.getSpecName());
                rsb.append(specName).append("\n");

                // spec vendor
                String specVendor = String.format(format, "spec vendor", SystemUtils.getSpecVendor());
                rsb.append(specVendor).append("\n");

                // spec version
                String specVersion = String.format(format, "spec version", SystemUtils.getSpecVersion());
                rsb.append(specVersion).append("\n");

//                // class path
//                String classPath = String.format(format, "class path", SystemUtils.getClassPath());
//                rsb.append(classPath).append("\n");

//                // boot class path
//                String bootClassPath = String.format(format, "boot class path", SystemUtils.getBootClassPath());
//                rsb.append(bootClassPath).append("\n");

                // start time
                String start = String.format(format, "startTime", SystemUtils.getStartTime());
                rsb.append(start).append("\n");

                // uptime
                String uptime = String.format(format, "uptime", SystemUtils.getUptime() + "");
                rsb.append(uptime).append("\n");

                // input args
                String args = String.format(format, "args", SystemUtils.getInputArguments());
                rsb.append(args).append("\n");

                return rsb.toString();
            }
        };
    }


    public static void main(String[] args) {

        System.out.println(RUNTIME_MONITOR_COLLECTOR.collect().event());
    }
}
