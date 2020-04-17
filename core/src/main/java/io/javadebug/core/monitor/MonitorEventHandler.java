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


package io.javadebug.core.monitor;

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.console.MonitorCollectorCommandSource;
import io.javadebug.core.transport.NettyTransportClient;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class MonitorEventHandler {

    // the monitor request types
    private static String MONITOR_REQUEST_TYPES = "thread";

    // the monitor delay, DO NOT SET TOO SMALL, SMALL DELAY WILL LEAD TO HEAVY LOAD IN RUNTIME
    private static volatile int delay = 5;

    // the work flag
    private static volatile boolean ready = false;

    private static OutputStream printStream;

    private static MonitorCollectorCommandSource commandSource;

    // the delay map
    private static final ConcurrentMap<String, Integer> DELAY_MAP = new ConcurrentHashMap<>();
    static {
        DELAY_MAP.put("thread", 5); // 5s
        DELAY_MAP.put("mem", 1); // 1s
        DELAY_MAP.put("gc", 1); // 1s
    }

    /**
     *  determine the delay by req, default is {@link MonitorEventHandler#delay}
     *
     * @param key the command key
     * @return the real delay (unit with seconds)
     */
    private static int getDelay(String key) {
        Integer targetDelay = DELAY_MAP.get(key);
        return targetDelay == null ? delay : targetDelay;
    }

    /**
     *  set delay
     *
     * @param key the command key
     * @param interval the delay
     */
    private static void setReady(String key, int interval) {
        if (interval <= 0) {
            PSLogger.error("invalid interval for key :" + key + " -> " + interval);
            return;
        }
        DELAY_MAP.put(key, interval);
        PSLogger.error("set interval for key :" + key + " with " + interval + " second(s)");
    }

    // the hashWheel timer
    private static HashedWheelTimer hashedWheelTimer;

    static {
        try {
            hashedWheelTimer = new HashedWheelTimer(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "Java-Debut-Tool-HashWheelTimer");
                }
            }, 1000, TimeUnit.MILLISECONDS);

            // start the hashWheel timer
            hashedWheelTimer.start();

            // register the repeat job
            hashedWheelTimer.newTimeout(new RepeatTimer(new Runnable() {
                @Override
                public void run() {
                    if (ready && commandSource != null) {
                        monitorReq();
                    }
                }
            }), getDelay(MONITOR_REQUEST_TYPES), TimeUnit.SECONDS);

        } catch (Exception e) {
            PSLogger.error("could not init the hashWheel timer", e);
        }
    }

    ///---------------- public method start ---------------------

    /**
     *  register an timer to the hashWheel
     *
     * @param job the job
     * @param delayMills the timeout mills
     * @return the timeout object
     */
    public static Timeout registerTimer(Runnable job, long delayMills) {
        return hashedWheelTimer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                job.run();
            }
        }, delayMills, TimeUnit.MILLISECONDS);
    }

    ///---------------- public method  end  ---------------------

    public static void setMonitorRequestTypes(String monitorRequestTypes) {
        MonitorEventHandler.MONITOR_REQUEST_TYPES = monitorRequestTypes;
    }

    public static void monitorReq() {
        commandSource.offer(MONITOR_REQUEST_TYPES);
    }

    public MonitorEventHandler(OutputStream ps, MonitorCollectorCommandSource source) {
        MonitorEventHandler.printStream = ps;
        MonitorEventHandler.commandSource = source;
    }

    public static void start() {
        MonitorEventHandler.ready = true;
        NettyTransportClient.setMonitorMode(true);
        //emit once
        monitorReq();
    }

    public static void start(String req, int interval) {
        setMonitorRequestTypes(req);

        // whether to check the req ? maybe contains multi key like "mem,gc,thread"
        setReady(req, interval);
        start();
    }

    public static void stop() {
        MonitorEventHandler.ready = false;
        NettyTransportClient.setMonitorMode(false);
    }

    /**
     *  control the delay
     *
     * @param delay unit is sec
     */
    public static void setDelay(int delay) {
        if (delay < 1) {
            PSLogger.error("the delay must >= 1 sec, fallback to 1 sec, your option is " + delay + " sec");
            delay = 1;
        }
        MonitorEventHandler.delay = delay;
    }

    /**
     *  the origin event, just show it !
     *
     * @param event the event
     */
    public void handle(String event) throws IOException {
        if (printStream == null) {
            throw new IllegalStateException("the output stream is null");
        }

        if (UTILS.isNullOrEmpty(event)) {
            PSLogger.error("the monitor event handler receive empty event !");
            return;
        }

        // clear the screen
        printStream.write("\033[H\033[2J".getBytes());
        printStream.flush();

        // show the event
        printStream.write(event.getBytes());

    }

    static class RepeatTimer implements TimerTask {

        private Runnable job;

        RepeatTimer(Runnable r) {
            this.job = r;
        }

        /**
         * Executed after the delay specified
         *
         * @param timeout a handle which is associated with this task
         */
        @Override
        public void run(Timeout timeout) throws Exception {
            try {
                // step 1 : cancel old timeout
                if (timeout != null && !timeout.isCancelled()) {
                    timeout.cancel();
                }

                // step 2 : run the job
                if (job != null) {
                    job.run();
                }

                // step 3 : register an new timeout
                hashedWheelTimer.newTimeout(this, getDelay(MONITOR_REQUEST_TYPES), TimeUnit.SECONDS);

            } catch (Exception e) {
                PSLogger.error("could not consume the job with exception", e);
            }
        }
    }

    public static void main(String[] args) {

        hashedWheelTimer.newTimeout(new RepeatTimer(new Runnable() {
            @Override
            public void run() {
                System.out.println("timeout printer");
            }
        }), delay, TimeUnit.SECONDS);

    }

}
