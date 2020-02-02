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


package io.javadebug.core.utils;

import io.javadebug.core.command.perf.ThreadRichnessInfo;
import io.javadebug.core.log.PSLogger;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {

    /**
     *
     *  get all threads in this jvm in call-time, this is a not clear-way to
     *  reach target, see {@link Thread#getAllStackTraces()}
     *
     *  Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
     *
     *  While much cleaner than the other alternative proposed, this has the
     *  downside of incurring the cost of getting stack traces for all threads.
     *  If you will be using those stack traces anyway, this is clearly superior.
     *  If not, then this may be significantly slower for no gain other than clean
     *  code
     *
     *  @see ThreadGroup
     *  @see Thread#getAllStackTraces()
     *  @see Thread#getThreads()
     *  :ref: https://stackoverflow.com/questions/1323408/get-a-list-of-all-threads-currently-running-in-java
     *
     * @return all threads
     */
    public static List<Thread> getAllThreads() {

        // get the root thread group
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parentGroup;
        while ((parentGroup = rootGroup.getParent()) != null) {
            rootGroup = parentGroup;
        }

        //Now, call the enumerate() function on the root group repeatedly.
        // The second argument lets you get all threads, recursively

        Thread[] threads = new Thread[rootGroup.activeCount()];
        while (rootGroup.enumerate(threads, true ) == threads.length) {
            threads = new Thread[threads.length * 2];
        }
        List<Thread> threadSet = new ArrayList<>();
        for (Thread thread : threads) {
            if (thread != null && thread.isAlive()) {
                threadSet.add(thread);
            }
        }
        return threadSet;
    }

    private static Method GET_THREADS_METHOD_OF_THREAD_CLASS;
    static {
        try {
            GET_THREADS_METHOD_OF_THREAD_CLASS = Thread.class.getDeclaredMethod("getThreads");

            // this code is not safe
            GET_THREADS_METHOD_OF_THREAD_CLASS.setAccessible(true);
        } catch (NoSuchMethodException e) {
            PSLogger.error("error when init the method : Thread.class.getDeclaredMethod(\"getThreads\")");
        }
    }

    /**
     *  {@link ThreadUtils#getAllThreads()}
     *  {@link Thread#getThreads()}
     *
     * @return all threads in this jvm
     */
    @SuppressWarnings("all")
    public static List<Thread> getAllThreadsByReflect() {
        if (GET_THREADS_METHOD_OF_THREAD_CLASS == null) {
            return getAllThreads();
        }
        try {
            Thread[] threads = (Thread[]) GET_THREADS_METHOD_OF_THREAD_CLASS.invoke(null, null);
            List<Thread> threadSet = new ArrayList<>();
            for (Thread thread : threads) {
                if (thread != null && thread.isAlive()) {
                    threadSet.add(thread);
                }
            }
            return threadSet;
        } catch (Exception e) {
            PSLogger.error("error : " + UTILS.getErrorMsg(e));
            return Collections.emptyList();
        }
    }

    // sample time mills
    private static final long TIME_TO_SAMPLE_MILLS = 100;

    // the thread mx bean
    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

    // cache the thread info
    private static final ConcurrentMap<Long, ThreadInfo> THREAD_INFO_CONCURRENT_MAP = new ConcurrentHashMap<>();

    /**
     *  {@link ThreadMXBean#getThreadInfo(long[], boolean, boolean)}
     *
     * @param tid the thread id list
     * @return {@link ThreadInfo}
     */
    public static ThreadInfo[] getThreadInfo(long[] tid) {
        return THREAD_MX_BEAN.getThreadInfo(tid, true, true);
    }

    /**
     *  {@link com.sun.management.ThreadMXBean#getThreadAllocatedBytes(long)}
     *
     * @param tid the tid array
     * @return key -> tid val -> allocated bytes
     */
    public static Map<Long, Long> getThreadAllocatedBytes(long[] tid) {
        if (THREAD_MX_BEAN instanceof com.sun.management.ThreadMXBean
                && ((com.sun.management.ThreadMXBean) THREAD_MX_BEAN).isThreadAllocatedMemorySupported()) {
            long[] allocatedBytes = ((com.sun.management.ThreadMXBean) THREAD_MX_BEAN).getThreadAllocatedBytes(tid);
            Map<Long, Long> allocatedMap = new HashMap<>();
            for (int i = 0; i < tid.length; i ++) {
                allocatedMap.put(tid[i], allocatedBytes[i]);
            }
            return allocatedMap;
        }
        return Collections.emptyMap();
    }

    /**
     *  {@link ThreadMXBean#getThreadInfo(long[], boolean, boolean)}
     *
     *  FBI WARN:
     *      THIS METHOD IS CONSUME MANY CPU TIME, PLEASE USE
     *      {@link ThreadUtils#getThreadInfo(long[])}
     *      IF YOU WANT TO GET MANY THREAD'S INFO
     *
     * @param tid the thread id list
     * @return {@link ThreadInfo}
     */
    public static ThreadInfo getThreadInfo(long tid) {
        ThreadInfo[] threadInfos = getThreadInfo(new long[]{tid});
        if (threadInfos == null || threadInfos.length == 0) {
            return null;
        } else {
            return threadInfos[0];
        }
    }

    /**
     *  substring , there are some threads' name is to long
     *
     * @param originName the origin name
     * @param len the target len
     * @return thread name
     */
    public static String trimThreadName(String originName, int len) {
        if (UTILS.isNullOrEmpty(originName)) {
            return "oops";
        }
        if (originName.length() <= len) {
            return originName;
        }
        int start = originName.length() - len;
        return originName.substring(start);
    }

    /**
     *  trans the origin thread status to simple show style
     *
     * @param state {@link Thread.State}
     * @return simple style
     */
    public static String getShowStatusOfThread(Thread.State state) {
        if (state == null) {
            return "Nil";
        }
        String s = "U";
        switch (state) {
            case NEW:
                s = "N";
                break;
            case RUNNABLE:
                s = "R";
                break;
            case BLOCKED:
                s = "B";
                break;
            case WAITING:
                s = "W";
                break;
            case TIMED_WAITING:
                s = "TW";
                break;
            case TERMINATED:
                s = "T";
                break;
        }
        return s;
    }

    /**
     *  {@link com.sun.management.ThreadMXBean#getThreadCpuTime(long[])}
     *
     * @param tids tid array
     * @return key -> tid, value -> cpu time (user + sys)
     *          return null if not support
     */
    private static Map<Long, Long> getThreadCpuTimes(long[] tids) {
        if (THREAD_MX_BEAN instanceof com.sun.management.ThreadMXBean
                && THREAD_MX_BEAN.isThreadCpuTimeSupported()
                && THREAD_MX_BEAN.isThreadCpuTimeEnabled()) {
            long[] cpuTimes = ((com.sun.management.ThreadMXBean) THREAD_MX_BEAN).getThreadCpuTime(tids);
            Map<Long, Long> cpuTimesMap = new HashMap<>();
            for (int i = 0; i < tids.length; i ++) {
                cpuTimesMap.put(tids[i], cpuTimes[i]);
            }
            return cpuTimesMap;
        }
        return null;
    }

    /**
     *  {@link com.sun.management.ThreadMXBean#getThreadUserTime(long[])}
     *
     * @param tids tid array
     * @return key -> tid, value -> cpu time (user)
     *          return null if not support
     */
    private static Map<Long, Long> getThreadUserCpuTimes(long[] tids) {
        if (THREAD_MX_BEAN instanceof com.sun.management.ThreadMXBean
                    && THREAD_MX_BEAN.isThreadCpuTimeSupported()
                    && THREAD_MX_BEAN.isThreadCpuTimeEnabled()) {
            long[] cpuTimes = ((com.sun.management.ThreadMXBean) THREAD_MX_BEAN).getThreadUserTime(tids);
            Map<Long, Long> cpuTimesMap = new HashMap<>();
            for (int i = 0; i < tids.length; i ++) {
                cpuTimesMap.put(tids[i], cpuTimes[i]);
            }
            return cpuTimesMap;
        }
        return null;
    }

    /**
     *  the origin implement
     *
     * @param topN top n
     * @return {@link ThreadRichnessInfo}
     */
    public static Map<Long, ThreadRichnessInfo> getTopNCpuConsumeThreadsAdvance(int topN, boolean userSysMode) {

        // the result
        Map<Long, ThreadRichnessInfo> topNCpuMap = new LinkedHashMap<>();

        // get all alive threads
        List<Thread> threadSet = getAllThreads();

        // the thread count
        int thCnt = threadSet.size();

        int rc = 0, wc = 0, twc = 0, bc = 0, nc = 0, tc = 0;
        for (Thread t : threadSet) {
            switch (t.getState()) {
                case NEW:
                    nc ++;
                    break;
                case RUNNABLE:
                    rc ++;
                    break;
                case BLOCKED:
                    bc ++;
                    break;
                case WAITING:
                    wc ++;
                    break;
                case TIMED_WAITING:
                    twc ++;
                    break;
                case TERMINATED:
                    tc ++;
                    break;
            }
        }

        // the thread id -> cpu time map
        Map<Long, Tuple3<Long, Long, Long>> cpuConsumeBAMap = new LinkedHashMap<>();

        // user cpu consume
        Map<Long, Tuple3<Long, Long, Long>> userCpuConsumeBAMap = new LinkedHashMap<>();

        // get the thread ids
        long[] tids = new long[threadSet.size()];
        for (int i = 0; i < tids.length; i ++) {
            tids[i] = threadSet.get(i).getId();
        }

        // sample cpu time here
        Map<Long, Long> cpuTimesMap1 = getThreadCpuTimes(tids);
        Map<Long, Long> cpuUserTimesMap1 = Collections.emptyMap();
        if (userSysMode) {
            cpuUserTimesMap1 = getThreadUserCpuTimes(tids);
        }

        if (cpuTimesMap1 == null || cpuUserTimesMap1 == null) {
            PSLogger.error("this jvm do not support cpu profiler");
            return Collections.emptyMap();
        }

        // sample cpu time
        for (long tid : tids) {
            long cpuTime = cpuTimesMap1.get(tid);
            Long userCpuTime = cpuUserTimesMap1.get(tid);
            if (userCpuTime == null) {
                userCpuTime = 0L;
            }
            Tuple3<Long, Long, Long> tuple = Tuple3.of(cpuTime, 0L, 0L);
            cpuConsumeBAMap.put(tid, tuple);
            Tuple3<Long, Long, Long> tuple1 = Tuple3.of(userCpuTime, 0L, 0L);
            userCpuConsumeBAMap.put(tid, tuple1);
        }


        // sleep
        try {
            TimeUnit.MILLISECONDS.sleep(TIME_TO_SAMPLE_MILLS);
        } catch (InterruptedException e) {
            PSLogger.error("InterruptedException on sample sleep ..." + UTILS.getErrorMsg(e));
            return Collections.emptyMap();
        }

        // re-sample
        Map<Long, Long> cpuTimesMap2 = getThreadCpuTimes(tids);
        Map<Long, Long> cpuUserTimesMap2 = Collections.emptyMap();
        if (userSysMode) {
            cpuUserTimesMap2 = getThreadUserCpuTimes(tids);
        }
        if (cpuTimesMap2 == null || cpuUserTimesMap2 == null) {
            PSLogger.error("impossible null in step 2");
            return Collections.emptyMap();
        }

        for (long tid : tids) {
            long cpuTime = cpuTimesMap2.get(tid);
            Long userCpuTime = cpuUserTimesMap2.get(tid);
            Tuple3<Long, Long, Long> tuple = cpuConsumeBAMap.get(tid);

            // the tuple must not null here
            tuple.setT2(cpuTime);

            // user cpu time
            Tuple3<Long, Long, Long> tuple1 = userCpuConsumeBAMap.get(tid);
            tuple1.setT2(userCpuTime == null ? 0L  : userCpuTime);
        }

        // handle the cpu consume
        long totalCpuConsume = 0;
        for (Map.Entry<Long, Tuple3<Long, Long, Long>> entry : cpuConsumeBAMap.entrySet()) {

            Tuple3<Long, Long, Long> tuple3 = entry.getValue();
            long ct1 = tuple3.getT1();
            long ct2 = tuple3.getT2();

            if (ct1 < 0 || ct2 <= 0) {
                entry.getValue().setT3(0L);
            } else if (ct2 <= ct1) {
                entry.getValue().setT3(0L);
            } else {
                entry.getValue().setT3(ct2 - ct1);
                totalCpuConsume += (ct2 - ct1);
            }
        }

        // user time
        for (Map.Entry<Long, Tuple3<Long, Long, Long>> entry : userCpuConsumeBAMap.entrySet()) {

            Tuple3<Long, Long, Long> tuple3 = entry.getValue();
            long ct1 = tuple3.getT1();
            long ct2 = tuple3.getT2();

            long cpuConsume;
            if (ct1 < 0 || ct2 <= 0) {
                cpuConsume = 0;
            } else if (ct2 <= ct1) {
                PSLogger.error("the user time is invalid with ct2 < ct1:[" + ct1 + "," + ct2 + "]");
                cpuConsume = 0;
            } else {
                cpuConsume = ct2 - ct1;
            }
            // the sys + usr cpu time
            long total = cpuConsumeBAMap.get(entry.getKey()).getT3();
            if (total > 0) {
                double cpuPct = (cpuConsume) / (total * 1.0);
                entry.getValue().setT3(Math.round(cpuPct * 100));
            } else {
                entry.getValue().setT3(0L);
            }

        }

        if (totalCpuConsume == 0) {
            PSLogger.error("the total cpu consume is 0, check the JVM with jstack ...");
            return Collections.emptyMap();
        }

        // cpu%
        for (Map.Entry<Long, Tuple3<Long, Long, Long>> entry : cpuConsumeBAMap.entrySet()) {
            Tuple3<Long, Long, Long> tuple3 = entry.getValue();
            long cpuConsume = tuple3.getT3();
            double cpuPct = (cpuConsume) / (totalCpuConsume * 1.0);
            entry.getValue().setT3(Math.round(cpuPct * 100));
        }

        //sort
        threadSet.sort(new Comparator<Thread>() {
            @Override
            public int compare(Thread o1, Thread o2) {
                long t1t = cpuConsumeBAMap.get(o1.getId()).getT3();
                long t2t = cpuConsumeBAMap.get(o2.getId()).getT3();

                // high -> low
                return Long.compare(t2t, t1t);
            }
        });

        // sub
        if (threadSet.size() > topN) {
            threadSet = threadSet.subList(0, topN);
        }

        // gen the result
        for (Thread thread : threadSet) {
            Tuple2<Long, Long> tuple2 = Tuple2.of(cpuConsumeBAMap.get(thread.getId()).getT3(),
                    userCpuConsumeBAMap.get(thread.getId()).getT3());

            // fill result
            ThreadRichnessInfo threadRichnessInfo = new ThreadRichnessInfo();
            threadRichnessInfo.setThread(thread);
            threadRichnessInfo.setCpuTuple(tuple2);
            threadRichnessInfo.setThreadCnt(thCnt);
            threadRichnessInfo.setnCount(nc);
            threadRichnessInfo.setrCount(rc);
            threadRichnessInfo.setbCount(bc);
            threadRichnessInfo.setTwCount(twc);
            threadRichnessInfo.settCount(tc);
            threadRichnessInfo.setwCount(wc);

            topNCpuMap.put(thread.getId(), threadRichnessInfo);
        }

        return topNCpuMap;
    }


    /**
     *  this method will get top N busy thread in target jvm,
     *
     * @param topN top n busy thread
     * @return the top n busy thread's id (tid)
     */
    @Deprecated
    public static Map<Long, Tuple2<Long, Long>> getTopNCpuConsumeThreads(int topN, boolean userSysMode) {
        // read
        Map<Long, ThreadRichnessInfo> threadRichnessInfoMap = getTopNCpuConsumeThreadsAdvance(topN, userSysMode);
        if (threadRichnessInfoMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // fill
        Map<Long, Tuple2<Long, Long>> topNCpuMap = new HashMap<>();
        threadRichnessInfoMap.forEach((id, info) -> topNCpuMap.put(id, info.getCpuTuple()));
        return topNCpuMap;
    }

    //private static final int MAX_FRAMES = 8;

    /**
     * get the thread's stacktrace with cpu usage
     *
     * Returns a string representation of this thread info.
     * The format of this string depends on the implementation.
     * The returned string will typically include
     *
     * @param tid the thread id
     * @param cpuUsage the cpu usage
     * @return the ret
     */
    public static String getThreadStackTrace(long tid, Tuple2<Long, Long> cpuUsage) {
        ThreadInfo threadInfo;
        try {
            threadInfo  = THREAD_MX_BEAN.getThreadInfo(tid, Integer.MAX_VALUE);
            if (threadInfo == null) {
                return "invalid thread id " + tid + "\n";
            }
        } catch (Exception e) {
            return "error when get the thread by tid : " + tid + " =>" + UTILS.getErrorMsg(e);
        }

        StringBuilder sb = new StringBuilder("\"" + threadInfo.getThreadName() + "\"" +
                                                     " Id=" + threadInfo.getThreadId() + " " +
                                                     threadInfo.getThreadState());
        // cpu usage
        if (cpuUsage != null) {

            // total
            sb.append(" cpu = ").append(cpuUsage.getT1()).append("%");

            // user
            if (cpuUsage.getT2() > 0) {
                sb.append(" user = ").append(cpuUsage.getT2()).append("%");
            }

            // sys
            if (cpuUsage.getT2() > 0) {
                sb.append(" sys = ").append(100  - (cpuUsage.getT2() == 0 ? 100 : cpuUsage.getT2())).append("%");
            }

        }

        // status
        sb.append(" Status = ").append(threadInfo.getThreadState());

        if (threadInfo.getLockName() != null) {
            sb.append(" on ").append(threadInfo.getLockName());
        }
        if (threadInfo.getLockOwnerName() != null) {
            sb.append(" owned by \"").append(threadInfo.getLockOwnerName())
                    .append("\" Id=").append(threadInfo.getLockOwnerId());
        }
        if (threadInfo.isSuspended()) {
            sb.append(" (suspended)");
        }
        if (threadInfo.isInNative()) {
            sb.append(" (in native)");
        }
        sb.append('\n');
        int i = 0;
        for (; i < threadInfo.getStackTrace().length /*&& i < MAX_FRAMES*/; i++) {
            StackTraceElement ste = threadInfo.getStackTrace()[i];
            sb.append("\tat ").append(ste.toString());
            sb.append('\n');
            if (i == 0 && threadInfo.getLockInfo() != null) {
                Thread.State ts = threadInfo.getThreadState();
                switch (ts) {
                    case BLOCKED:
                        sb.append("\t-  blocked on ").append(threadInfo.getLockInfo());
                        sb.append('\n');
                        break;
                    case WAITING:
                        sb.append("\t-  waiting on ").append(threadInfo.getLockInfo());
                        sb.append('\n');
                        break;
                    case TIMED_WAITING:
                        sb.append("\t-  waiting on ").append(threadInfo.getLockInfo());
                        sb.append('\n');
                        break;
                    default:
                }
            }

            for (MonitorInfo mi : threadInfo.getLockedMonitors()) {
                if (mi.getLockedStackDepth() == i) {
                    sb.append("\t-  locked ").append(mi);
                    sb.append('\n');
                }
            }
        }
        if (i < threadInfo.getStackTrace().length) {
            sb.append("\t...");
            sb.append('\n');
        }

        LockInfo[] locks = threadInfo.getLockedSynchronizers();
        if (locks.length > 0) {
            sb.append("\n\tNumber of locked synchronizers = ").append(locks.length);
            sb.append('\n');
            for (LockInfo li : locks) {
                sb.append("\t- ").append(li);
                sb.append('\n');
            }
        }
        sb.append('\n');
        return sb.toString();
    }

    /**
     *  convert the command option's status to thread.status
     *  {@link Thread.State}
     *
     *  R   => {@link Thread.State#RUNNABLE}
     *  W   => {@link Thread.State#WAITING}
     *  TW  => {@link Thread.State#TIMED_WAITING}
     *  B   => {@link Thread.State#BLOCKED}
     *
     * @param status the origin status
     * @return {@link Thread.State}
     */
    public static Set<Thread.State> convertToStatus(String status) {
        Set<Thread.State> includeStatus = new HashSet<>();
        String[] splitStatus = status.split(",");
        for (String s : splitStatus) {
            switch (s) {
                case "R": {
                    includeStatus.add(Thread.State.RUNNABLE);
                    break;
                }
                case "W": {
                    includeStatus.add(Thread.State.WAITING);
                    break;
                }
                case "TW": {
                    includeStatus.add(Thread.State.TIMED_WAITING);
                    break;
                }
                case  "B": {
                    includeStatus.add(Thread.State.BLOCKED);
                    break;
                }
            }
        }
        return includeStatus;
    }

    /**
     *  get the thread's cpu consume by tid
     *
     * @param tid the thread id
     * @return cpu consume
     */
    private static long getThreadCpuConsume(long tid) {
        return THREAD_MX_BEAN.getThreadCpuTime(tid);
    }

    /**
     *  get the thread's user cpu consume by tid
     *
     * @param tid the thread id
     * @return cpu consume
     */
    private static long getThreadUserCpuConsume(long tid) {
        return THREAD_MX_BEAN.getThreadUserTime(tid);
    }

}
