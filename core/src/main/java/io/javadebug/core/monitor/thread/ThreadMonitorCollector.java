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


package io.javadebug.core.monitor.thread;

import io.javadebug.core.monitor.load.LoadAverageMonitorCollector;
import io.javadebug.core.utils.Tuple2;
import io.javadebug.core.command.perf.ThreadRichnessInfo;
import io.javadebug.core.utils.ThreadUtils;
import io.javadebug.core.monitor.CounterEvent;
import io.javadebug.core.monitor.MonitorCollector;
import io.javadebug.core.monitor.MonitorConsume;
import io.javadebug.core.monitor.MonitorHandler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public enum ThreadMonitorCollector implements MonitorCollector {
    THREAD_MONITOR_COLLECTOR
    ;

    // the top n thread will be show to user
    private static volatile int SHOW_TOP_N_THREAD_CNT = 10;

    // this is heavy action
    private static volatile boolean needCallStack = false;

    /**
     *  update the topN cnt value
     *
     * @param showTopNThreadCnt the top 'N'
     */
    public static int setShowTopNThreadCnt(int showTopNThreadCnt) {
        int old = ThreadMonitorCollector.SHOW_TOP_N_THREAD_CNT;
        ThreadMonitorCollector.SHOW_TOP_N_THREAD_CNT = showTopNThreadCnt;
        return old;
    }

    /**
     *  control the 'call-stack'
     *
     * @param needCallStack true means the list will show the call stack
     */
    public static void setNeedCallStack(boolean needCallStack) {
        ThreadMonitorCollector.needCallStack = needCallStack;
    }

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
        // the result
        StringBuilder rsb = new StringBuilder();

        // get the topN threads info
        // key -> thread id
        // value -> thread cpu consume
        //          key     -> total cpu consume pct (%)
        //          value   -> user cpu consume  pct (%)
        Map<Long, ThreadRichnessInfo> topNThreadInfoMap =
                ThreadUtils.getTopNCpuConsumeThreadsAdvance(SHOW_TOP_N_THREAD_CNT, false);

//        // get the thread info here
//        Set<Long> tidList = topNThreadInfoMap.keySet();
//        long[] tids = new long[tidList.size()];
//        int i = 0;
//        for (long id : tidList) {
//            tids[i ++] = id;
//        }
////        Map<Long, ThreadInfo> threadInfoMap = Collections.emptyMap();
////        // maybe, we needn't call stack
////        if (needCallStack) {
////            threadInfoMap = new HashMap<>();
////            ThreadInfo[] threadInfos = ThreadUtils.getThreadInfo(tids);
////            for (ThreadInfo threadInfo : threadInfos) {
////                threadInfoMap.put(threadInfo.getThreadId(), threadInfo);
////            }
////        }

        // get the allocated
        // BUT THIS SEEM USELESS ..., SO DO NOT GET THIS INFO, LET CPU DO SOME OTHER USEFUL THINGS
        //Map<Long, Long> allocatedMap = Collections.emptyMap();//ThreadUtils.getThreadAllocatedBytes(tids);

        // thread_id  thread_name status %cpu  %user_cpu %sys_cpu  stack_top_method
        String format = "%-10.10s " + "%-50.50s " + "%-10.10s " + "%-6.6s " + "%-100.100s";

        // header
        String ttt = "-----";
        String pHeader;
        String header;
        if (needCallStack) {
            header = String.format(format, "tid", "name", "  s", "cpu%", "call");
            pHeader = String.format(format, ttt, ttt, ttt, ttt, ttt);
        } else {
            header = String.format(format, "tid", "name", "  s", "cpu%", " ");
            pHeader = String.format(format, ttt, ttt, ttt, ttt, "");
        }
        rsb.append(pHeader).append("\n");
        rsb.append(header).append("\n");
        rsb.append(pHeader).append("\n");

        List<String> cols = new ArrayList<>();

        ThreadRichnessInfo threadRichnessRef = null;

        // for each and get the result
        for (Map.Entry<Long, ThreadRichnessInfo> entry : topNThreadInfoMap.entrySet()) {
            cols.clear();

            ThreadRichnessInfo threadRichnessInfo = entry.getValue();
            if (threadRichnessInfo == null) {
                continue;
            }

            // for thread count
            if (threadRichnessRef == null) {
                threadRichnessRef = threadRichnessInfo;
            }

            // the cpu consume
            Tuple2<Long, Long> tuple2 = threadRichnessInfo.getCpuTuple();
            if (tuple2 == null) {
                continue;
            }

            // maybe it is un-necessary to show
            if (tuple2.getT1() <= 0) {
                break;
            }

            // tid
            cols.add((entry.getKey() + ""));

            // the thread ref
            Thread thread = threadRichnessInfo.getT();

            // name
            String name = ThreadUtils.trimThreadName(thread.getName(), 50);
            cols.add(name);

            // status
            cols.add("  " + ThreadUtils.getShowStatusOfThread(thread.getState()));

//            // alloc
//            Long alloc = allocatedMap.get(entry.getKey());
//            if (alloc == null || alloc <= 0) {
//                cols.add("nil");
//            } else {
//                cols.add(handleMemAlloc(alloc));
//            }

            // %cpu
            cols.add(tuple2.getT1() + "%");

//            // usr%
//            long user = entry.getValue().getT2();
//            if (user <= 0) {
//                cols.add("NaN");
//            } else {
//                cols.add(user + "%");
//            }
//
//            // sys%
//            long sys = (100 - (entry.getValue().getT2() == 0 ? 100 : entry.getValue().getT2()));
//            if (sys <= 0) {
//                cols.add("NaN");
//            } else {
//                cols.add(sys + "%");
//            }

            // top method
            String topCall = "in native";
            String secondTopCall = null;
            if (needCallStack) {
                StackTraceElement[] traceElements = thread.getStackTrace();
                if (traceElements != null && traceElements.length > 0) {
                    StackTraceElement stackTraceElement = traceElements[0];
                    topCall = stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName();

                    // line number
                    if (stackTraceElement.getLineNumber() >= 0) {
                        topCall += " at " + stackTraceElement.getLineNumber();
                    }
                    if (traceElements.length > 1) {
                        stackTraceElement = traceElements[1];
                        secondTopCall = "\\-" + stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName();

                        // line number
                        if (stackTraceElement.getLineNumber() >= 0) {
                            secondTopCall += " at " + stackTraceElement.getLineNumber();
                        }
                        secondTopCall = String.format(format, "", "", "", "", " " + secondTopCall);
                    }
                }
                cols.add(topCall);
            } else {
                cols.add("");
            }
            // each cols
            String line = String.format(format, cols.toArray());
            rsb.append(line).append("\n");
            if (secondTopCall != null) {
                rsb.append(secondTopCall).append("\n");
            }
        }

        // thread count show line
        if (threadRichnessRef != null) {
            String countLine = String.format("Total Thread : %d, New : %d, Runnable : %d, Blocked : %d," +
                                                     " Waiting : %d, Timed Waiting : %d, Terminated : %d",
                    threadRichnessRef.getThreadCnt(),
                    threadRichnessRef.getnCount(), threadRichnessRef.getrCount(), threadRichnessRef.getbCount(),
                    threadRichnessRef.getwCount(), threadRichnessRef.getTwCount(), threadRichnessRef.gettCount()
            );
            rsb.append("\n").append(countLine);
        }

        // system info
        rsb.append("\n").append("--- System Load ---").append("\n")
                .append(LoadAverageMonitorCollector.LOAD_AVERAGE_MONITOR_COLLECTOR.collect().event())
                .append("\n");

        return new CounterEvent() {
            @Override
            public String type() {
                return "Thread Dynamic Statistic";
            }

            @Override
            public String event() {
                return rsb.toString();
            }
        };
    }

    /**
     *  [0, 1024bytes] -> bytes
     *  [1kb, 1024kb]  -> kb
     *  [1M, 1024M]    -> mb
     *  [1G, 1024G]    -> gb
     *
     * @param bytes the bytes
     * @return with fix unit
     */
    private String handleMemAlloc(long bytes) {
        if (bytes <= 1024) { // b
            return bytes + " b";
        } else if (bytes <= (1024.0 * 1024.0)) {
            return  trimDouble((bytes) / (1024.0 * 1024.0), 1) + "kb";
        } else if (bytes <= (1024.0 * 1024.0 * 1024)) {
            return trimDouble((bytes / (1024.0 * 1024.0)), 1) + "mb";
        } else if (bytes <= (1024.0 * 1024.0 * 1024.0 * 1024)) {
            return trimDouble((bytes / (1024.0 * 1024.0 * 1024.0)), 2) + "gb";
        } else if (bytes <= (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024)) {
            return trimDouble((bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0)), 2) + "tb";
        } else {
            return trimDouble((bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0)), 3) + "pb";
        }
    }

    private double trimDouble(double d, int s) {
        BigDecimal bigDecimal = new BigDecimal(d);
        bigDecimal = bigDecimal.setScale(s, BigDecimal.ROUND_HALF_UP);
        return bigDecimal.doubleValue();
    }

    private String space(int len) {
        StringBuilder rsb = new StringBuilder();
        for (int i = 0; i < len; i ++) {
            rsb.append(' ');
        }
        return rsb.toString();
    }

    public static void main(String[] args) {

        String s = ThreadMonitorCollector.THREAD_MONITOR_COLLECTOR.collect().event();
        System.out.println(s);

    }

}
