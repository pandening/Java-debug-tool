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


package io.javadebug.core.monitor.gc;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import io.javadebug.core.data.JVMMemoryInfo;
import io.javadebug.core.monitor.CounterEvent;
import io.javadebug.core.monitor.MonitorCollector;
import io.javadebug.core.monitor.MonitorConsume;
import io.javadebug.core.monitor.MonitorHandler;
import io.javadebug.core.utils.MemoryUtils;

import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum GarbageCollectMonitorCollector implements MonitorCollector {
    GARBAGE_COLLECT_MONITOR_COLLECTOR {
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
                    return "Dynamic GC Statistic";
                }

                @Override
                public String event() {
                    return doCollect(false);
                }
            };
        }
    },
    SIMPLE_GARBAGE_COLLECT_MONITOR_COLLECTOR {
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
                    return "(Simple) Dynamic GC Statistic";
                }

                @Override
                public String event() {
                    return doCollect(true);
                }
            };
        }
    };

    private static final Set<String> YoungGCAlgorithmSet = new HashSet<String>() {
        {
            add("Copy");
            add("ParNew");
            add("PS Scavenge");
            add("G1 Young Generation");
        }
    };

    private static final Set<String> OldGCAlgorithmSet = new HashSet<String>() {
        {
            add("MarkSweepCompact");
            add("PS MarkSweep");
            add("ConcurrentMarkSweep");
            add("G1 Old Generation");
        }
    };

    private static volatile long lastGcCount = 0;

    private static volatile long lastGcTime = 0;

    private static volatile long lastFullGcTime = 0;

    private static volatile long lastFullGcCount = 0;

    private static volatile long lastYoungGcTime = 0;

    private static volatile long lastYoungGcCount = 0;

//    private static String gcName;
//    private static String gcCause;
//    private static String gcAction;
//
//    static  {
//        for (final GarbageCollectorMXBean garbageCollector : ManagementFactory.getGarbageCollectorMXBeans()) {
//            if (garbageCollector instanceof NotificationEmitter) {
//                NotificationListener notificationListener = (notification, ref) -> {
//                    if (!notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
//                        return;
//                    }
//                    CompositeData cd = (CompositeData) notification.getUserData();
//                    GarbageCollectionNotificationInfo notificationInfo = GarbageCollectionNotificationInfo.from(cd);
//
//                    gcName = notificationInfo.getGcName();
//                    gcCause = notificationInfo.getGcCause();
//                    gcAction = notificationInfo.getGcAction();
//                };
//                NotificationEmitter notificationEmitter = (NotificationEmitter) garbageCollector;
//                notificationEmitter.addNotificationListener(notificationListener, null, null);
//            }
//        }
//    }

    private static String doCollect(boolean simpleMode) {
        long gcCount = 0, gcTime = 0, fullGCount = 0, fullGcTime = 0, youngGcCount = 0, youngGcTime = 0;
        String algorithm;
        boolean hasFullGc = false;
        StringBuilder lastGcBuilder = new StringBuilder();
        // do statistic
        Map<String, String> statisticMap = new HashMap<>();

        for (final GarbageCollectorMXBean garbageCollector : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcTime += garbageCollector.getCollectionTime();
            gcCount += garbageCollector.getCollectionCount();
            algorithm = garbageCollector.getName();
            if (YoungGCAlgorithmSet.contains(algorithm)) {
                youngGcTime += garbageCollector.getCollectionTime();
                youngGcCount += garbageCollector.getCollectionCount();
            } else if (OldGCAlgorithmSet.contains(algorithm)) {
                fullGcTime += garbageCollector.getCollectionTime();
                fullGCount += garbageCollector.getCollectionCount();
            }

            // last gc info
            if (!simpleMode && (garbageCollector instanceof com.sun.management.GarbageCollectorMXBean)) {
                lastGcCollect((com.sun.management.GarbageCollectorMXBean) garbageCollector, lastGcBuilder);
            }

        }

        // gc count
        statisticMap.put("jvm.gc.count", "" + (gcCount - lastGcCount));

        // gc time
        statisticMap.put("jvm.gc.time", "" + (gcTime - lastGcTime));

        // young gc count
        statisticMap.put("jvm.gc.young.count", "" + (youngGcCount - lastYoungGcCount));

        // young time
        statisticMap.put("jvm.gc.young.time", "" + (youngGcTime - lastYoungGcTime));

        // young mean time
        if (youngGcCount > lastYoungGcCount) {
            statisticMap.put("jvm.gc.young.meantime",
                    "" + (youngGcTime - lastYoungGcTime) / (1.0 * (youngGcCount - lastYoungGcCount)));
        }

        // full gc count
        if (fullGCount - lastFullGcCount > 0) {
             hasFullGc = true;
        }
        statisticMap.put("jvm.gc.full.count", "" + (fullGCount - lastFullGcCount));

        // full gc count
        statisticMap.put("jvm.gc.full.time", "" + (fullGcTime - lastFullGcTime));

        // full gc mean time
        if (fullGCount > lastFullGcCount) {
            statisticMap.put("jvm.gc.full.meantime",
                    "" + (fullGcTime - lastFullGcTime) / (1.0 * (fullGCount - lastFullGcCount)));
        }

        // check the old gen space size after full gc
        if (hasFullGc) {
            JVMMemoryInfo jvmMemoryInfo = MemoryUtils.getMemory();

            // compute old gen usage pct
            long usedOldGen = jvmMemoryInfo.getUsedOldGenMem();
            long maxOldGen = jvmMemoryInfo.getMaxOldGenMem();

            if (usedOldGen > 0 && maxOldGen > 0) {
                double pct = (usedOldGen / (maxOldGen * 1.0)) * 100d;
                String pctInfo = String.format("%.2f", pct);
                statisticMap.put("jvm.memory.oldgen.used.percent.after.fullgc", pctInfo + "%");
            }

        }

        // update the count
        lastGcCount = gcCount;
        lastGcTime = gcTime;
        lastYoungGcCount = youngGcCount;
        lastYoungGcTime = youngGcTime;
        lastFullGcCount = fullGCount;
        lastFullGcTime = fullGcTime;

        // build the result
        return build(statisticMap, lastGcBuilder);
    }

    /**
     *  simple build the result by key  : value
     *
     * @param map the statistic map
     * @return the result
     */
    private static String build(Map<String, String> map, StringBuilder lsb) {
        if (map.isEmpty()) {
            return "empty result";
        }
        String format = "%-50.50s " + "%10.10s";
        StringBuilder rsb = new StringBuilder();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            rsb.append(String.format(format, entry.getKey(), entry.getValue())).append("\n");
        }

        if (lsb.length() > 0) {
            rsb.append(lsb);
        }

        return rsb.toString();
    }

    /**
     *  {@link com.sun.management.GarbageCollectorMXBean#getLastGcInfo()}
     *
     * @param garbageCollectorMXBean the bean
     * @param lsb last gc info builder
     */
    private static void lastGcCollect(com.sun.management.GarbageCollectorMXBean garbageCollectorMXBean, StringBuilder lsb) {

        GcInfo gcInfo = garbageCollectorMXBean.getLastGcInfo();
        if (gcInfo == null) {
            return;
        }

        String name = garbageCollectorMXBean.getName();
        lsb.append("\n").append(" --- Last GC Action Summary :(").append(name).append(") ---").append("\n");
        lsb.append("Total cost : ").append(gcInfo.getDuration()).append(" ms").append("\n");

        // space compare
        String format = "%-40.40s " + "%-10.10s " + "%-10.10s " + "%-12.12s " + "%12.12s " + "%12.12s " + "%10.10s";
        String hh = "-------------";
        String h = String.format(format, hh, hh, hh, hh, hh, hh, hh);
        lsb.append(h).append("\n")
                .append(String.format(format, "Space", "Total", "Init", "Committed", "BeforeUsage", "AfterUsage", "Delta"))
                .append("\n").append(h).append("\n");

        // before gc
        Map<String, MemoryUsage> beforeGcMemoryUsageMap = gcInfo.getMemoryUsageBeforeGc();
        Map<String, MemoryUsage> afterGcMemoryUsageMap = gcInfo.getMemoryUsageAfterGc();
        for (Map.Entry<String, MemoryUsage> entry : beforeGcMemoryUsageMap.entrySet()) {
            String space = entry.getKey();
            MemoryUsage before = entry.getValue();
            MemoryUsage after = afterGcMemoryUsageMap.get(space);
            if (before == null || after == null) {
                continue;
            }
            lsb.append(String.format(format, space, "" + before.getMax(), "" + after.getInit(),
                    "" + after.getCommitted(), "" + before.getUsed(), "" + after.getUsed(),
                    "" + (before.getUsed() - after.getUsed()))).append("\n");
        }
    }

    public static void main(String[] args) {

        String pctInfo = String.format("%.2f", 1.234567);
        System.out.println(pctInfo);

    }

}
