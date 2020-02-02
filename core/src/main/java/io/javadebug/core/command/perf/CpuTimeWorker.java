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


package io.javadebug.core.command.perf;

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class CpuTimeWorker {

    private static final ScheduledThreadPoolExecutor POOL_EXECUTOR =
            new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "CpuTimeWorker");
                }
            });

    public static final int COLLECT_STEP_MILLS = 1000;

    static {
        POOL_EXECUTOR.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    doWork();
                } catch (Exception e) {
                    PSLogger.error("error occ:" + UTILS.getErrorMsg(e));
                }
            }
        }, 0, COLLECT_STEP_MILLS, TimeUnit.MILLISECONDS);
    }

    private static final int POINT_LIMIT = 30;
    private static volatile boolean workIns = false;
    private static CpuTimeReportStruct cpuTimeReportStruct;
    private static final long clock_tics_per_sec = GNULibC.GNU_LIB_C.getClockTicsPerSec();

    private static final List<Waiter> WAITERS = new CopyOnWriteArrayList<>();
    private static final AtomicInteger INC = new AtomicInteger(0);
    private static final double[] EMPTY_DOUBLE_ARRAY = {-1, -1, -1};

    public static double getClockTicsPerSec() {
        return clock_tics_per_sec;
    }

    public static void collect(long contextId, WaitListener waitListener) {
        // param check
        if (contextId <= 0 || waitListener == null) {
            PSLogger.error("invalid param when call collect");
            return;
        }

        // working
        if (!workIns) {
            init();
            workIns = true;
        }

        // init the wait
        Waiter waiter = new Waiter();
        waiter.latch = new CountDownLatch(1);
        WAITERS.add(waiter);

        // wait the latch
        try {
            waiter.latch.await(65, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            PSLogger.error("Interrupted !");
            waitListener.error(e);
            return;
        }

        // timeout
        if (cpuTimeReportStruct == null) {
            waitListener.error(new TimeoutException("wait result timeout!"));
            return;
        }

        // report result
        waitListener.done(cpuTimeReportStruct);
    }

    private static void doWork() {

        // empty work
        if (!workIns) {
            //PSLogger.error("do not work");
            return;
        }

        // check
        if (INC.getAndIncrement() > POINT_LIMIT) {
            reset();
            releaseWaiter();
            return;
        }

//        // measure
//        TimeInfo timeInfo = GNULibC.GNU_LIB_C.getTimesSecs();
//        if (timeInfo == null) {
//            reset();
//            releaseWaiter();
//            return;
//        }

        //
        // --- the 'getrusage' syscall will get the usr + sys time info of this process by the way
        // so , the 'times' syscall is unnecessary now;
        // BUT THE PARAM 'who' MUST EQUALS 0, OR THE RESULT IS THE CHILDREN OF CURRENT PROCESS;
        //
        RusageStruct rusageStruct = GNULibC.GNU_LIB_C.getCurrentProcessResourceUsage();
        if (rusageStruct == null) {
            reset();
            releaseWaiter();
            return;
        }
        TimeInfo timeInfo = new TimeInfo();
        timeInfo.userSecs = convertToSec(rusageStruct.ru_utime);
        timeInfo.systemSecs = convertToSec(rusageStruct.ru_stime);
        timeInfo.nvcsw = rusageStruct.ru_nvcsw.longValue();
        timeInfo.nivcsw = rusageStruct.ru_nivcsw.longValue();

//        // get the load avg
//        double[] loadavg = GNULibC.GNU_LIB_C.getLoadAvg();
//        if (loadavg != null) {
//            timeInfo.loadavg = loadavg;
//        } else {
//            timeInfo.loadavg = EMPTY_DOUBLE_ARRAY;
//        }

        // first time
        if (cpuTimeReportStruct.baseTimeLine == null) {

            // the start mills
            cpuTimeReportStruct.startMills = System.currentTimeMillis();

            cpuTimeReportStruct.baseTimeLine = timeInfo;
            PSLogger.error("get the cpu measure time base line:" + timeInfo);
            return;
        }

        // normal cpu time usage
        List<TimeInfo> timeInfoList = cpuTimeReportStruct.timeInfoList;
        if (timeInfoList == null) {
            timeInfoList = new ArrayList<>();
            cpuTimeReportStruct.timeInfoList = timeInfoList;
        }
        timeInfoList.add(timeInfo);
    }

    /**
     *  convert to sec
     *
     * @param timeval {@link TimeValStruct}
     * @return the sec val
     */
    private static double convertToSec(TimeValStruct timeval) {

        if (timeval == null) {
            return 0.0;
        }

        double sec = timeval.tv_sec.longValue() * 1.0;

        // microseconds -> millisecond
        sec += timeval.tv_usec / (1000000.0);

        return sec;
    }

    private static void init() {
        // init the structure
        cpuTimeReportStruct = new CpuTimeReportStruct();

        // the start time mills
        cpuTimeReportStruct.startMills = System.currentTimeMillis();
    }

    private static void reset() {
        // stop mills
        cpuTimeReportStruct.stopMills = System.currentTimeMillis();

        // work tag
        workIns = false;

        // reset the inc
        INC.set(0);
    }

    private static void releaseWaiter() {
        for (Waiter waiter : WAITERS) {
            waiter.latch.countDown();
        }
        // clear the waiter
        WAITERS.clear();
    }

    public static void main(String[] args) {
        collect(1, new WaitListener() {
            @Override
            public void done(CpuTimeReportStruct cpuTimeReportStruct) {
                System.out.println("done:\n" + cpuTimeReportStruct);
            }

            @Override
            public void error(Throwable e) {
                System.out.println("error:\n" + e);
            }
        });
    }

    /**
     *
     *  @see Waiter
     */
    interface WaitListener {

        /**
         * called when {@link CpuTimeReportStruct} has been collected;
         *
         * @param cpuTimeReportStruct the {@link CpuTimeReportStruct}
         */
        void done(CpuTimeReportStruct cpuTimeReportStruct);

        /**
         *  throw exception, the caller will receive the {@link Throwable}
         *
         * @param e the exception
         */
        void error(Throwable e);

    }

    /**
     *  the waiter is used to wait something, the caller will wait on the latch, the
     *  waiter release component will countdown the latch;
     *
     *  @see WaitListener
     */
    static class Waiter {
        CountDownLatch latch;
    }

}
