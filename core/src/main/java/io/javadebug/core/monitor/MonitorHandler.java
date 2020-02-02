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

import io.javadebug.core.monitor.runtime.RuntimeMonitorCollector;
import io.javadebug.core.monitor.sys.SystemMonitorCollector;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.handler.WebSocketFrameHandler;
import io.javadebug.core.monitor.clazz.ClassMonitorCollector;
import io.javadebug.core.monitor.cpu.CPUCollector;
import io.javadebug.core.monitor.gc.GarbageCollectMonitorCollector;
import io.javadebug.core.monitor.http.HttpMonitorCollector;
import io.javadebug.core.monitor.jetty.JettyMonitorCollector;
import io.javadebug.core.monitor.load.LoadAverageMonitorCollector;
import io.javadebug.core.monitor.mem.MemoryMonitorCollector;
import io.javadebug.core.monitor.net.NetworkMonitorCollector;
import io.javadebug.core.monitor.netty.NettyMonitorCollector;
import io.javadebug.core.monitor.thread.ThreadMonitorCollector;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *  the monitor handler, as Controller of the monitor, the {@link MonitorHandler}
 *  will producer an event after calling the method {@link MonitorCollector#collect()}
 *  the {@link MonitorConsume#consume(Map, Object)} should be called after this;
 *
 *  the controller also need to check the monitor's status, and route the client's
 *  request by some tag.
 *
 *  @see io.javadebug.core.handler.CommandHandler
 *  @see io.javadebug.core.transport.RemoteCommand
 */
@ChannelHandler.Sharable
public class MonitorHandler extends WebSocketFrameHandler {

    // the monitor map
    private static final Map<String, MonitorCollector> MONITOR_COLLECTOR_MAP = new HashMap<>();
    static {
        MONITOR_COLLECTOR_MAP.put("class", ClassMonitorCollector.CLASS_MONITOR_COLLECTOR);
        MONITOR_COLLECTOR_MAP.put("cpu", CPUCollector.CPU_COLLECTOR);
        MONITOR_COLLECTOR_MAP.put("gc", GarbageCollectMonitorCollector.GARBAGE_COLLECT_MONITOR_COLLECTOR);
        MONITOR_COLLECTOR_MAP.put("http", HttpMonitorCollector.HTTP_MONITOR_COLLECTOR);
        MONITOR_COLLECTOR_MAP.put("jetty", JettyMonitorCollector.JETTY_MONITOR_COLLECTOR);
        MONITOR_COLLECTOR_MAP.put("load", LoadAverageMonitorCollector.LOAD_AVERAGE_MONITOR_COLLECTOR);
        MONITOR_COLLECTOR_MAP.put("sys", SystemMonitorCollector.SYSTEM_MONITOR_COLLECTOR);
        MONITOR_COLLECTOR_MAP.put("mem", MemoryMonitorCollector.MEMORY_MONITOR_COLLECTOR);
        MONITOR_COLLECTOR_MAP.put("net", NetworkMonitorCollector.NETWORK_MONITOR_COLLECTOR);
        MONITOR_COLLECTOR_MAP.put("netty", NettyMonitorCollector.NETTY_MONITOR_COLLECTOR);
        MONITOR_COLLECTOR_MAP.put("thread", ThreadMonitorCollector.THREAD_MONITOR_COLLECTOR);
        MONITOR_COLLECTOR_MAP.put("runtime", RuntimeMonitorCollector.RUNTIME_MONITOR_COLLECTOR);
    }

    // the monitor consume
    private static final MonitorConsume<ChannelHandlerContext> MONITOR_CONSUME = new DefaultMonitorEventConsumer();

    /**
     *  get a {@link MonitorCollector} by the req type
     *
     * @param type  the req type
     * @return the monitor collector
     */
    private MonitorCollector dispatch(String type) {
        if (UTILS.isNullOrEmpty(type)) {
            return null;
        }
        return MONITOR_COLLECTOR_MAP.get(type);
    }

    /**
     *  the client start to request the collect
     *
     * @param req the collectors , split by ','
     */
    private void request(String req, ChannelHandlerContext ctx) {
        Map<String, CounterEvent> collectMap = new LinkedHashMap<>();

        // notice client the request is invalid
        if (UTILS.isNullOrEmpty(req)) {
            MONITOR_CONSUME.consume(collectMap, ctx);
            return;
        }

        String[] collectors = req.split(",");

        // foreach collect the monitor, then send to client
        for (String collector : collectors) {
            MonitorCollector monitorCollector = dispatch(collector);
            if (monitorCollector != null) {
                CounterEvent counterEvent = monitorCollector.collect();
                if (counterEvent != null) {
                    collectMap.put(collector, counterEvent);
                }
            }
        }

        // notice the client
        MONITOR_CONSUME.consume(collectMap, ctx);
    }

    @Override
    protected void handleWebSocketFrame(String request, ChannelHandlerContext ctx) {
        request(request, ctx);
    }
}
