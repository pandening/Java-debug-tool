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

import io.javadebug.core.utils.ConsoleUtils;
import io.javadebug.core.log.PSLogger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.Map;

/**
 *  using webSocket to do transport between server & client, the default monitor
 *  will wrap the origin event as protocol like {@link io.javadebug.core.transport.RemoteCommand}
 *  then send to client, the client will receive the event, and handle it !
 *
 *
 *
 *   The show ui like this:
 *
 *   -------------------------------------------------------------------------
 *    [gc       area]       ...
 *    [class    area]       ...
 *    [cpu      area]       ...
 *    [load     area]       ...
 *    [mem      area]       ...
 *    [net      area]       ...
 *    [http     area]       ...
 *    [netty    area]       ...
 *    [jetty    area]       ...
 *
 *    -------------------------------------------------------------------------
 *    thread_id  thread_name status %cpu  %user_cpu %sys_cpu  stack_top_method
 *     101       test-thread  R     10%     30%       70%      package.method()
 *     ..           ...       ..    ..      ..        ..        ..
 *     ..           ...       ..    ..      ..        ..        ..
 *    -------------------------------------------------------------------------
 *
 *  Auth : pandening
 *  Time : 2020-01-03 23:00
 */
public class DefaultMonitorEventConsumer implements MonitorConsume<ChannelHandlerContext> {

    /**
     * consume the event produce by {@link MonitorCollector}
     *
     * @param eventMap the origin event {@link CounterEvent}
     */
    @Override
    public void consume(Map<String, CounterEvent> eventMap, ChannelHandlerContext ctx) {

        // trans
        TextWebSocketFrame webSocketFrame = transToWebSocketFrame(eventMap);

        // async send
        ctx.channel().writeAndFlush(webSocketFrame).addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                try {
                    future.get();
                } catch (Exception e) {
                    PSLogger.error("could not send webSocket frame to client : " + ctx.channel().remoteAddress(), e);
                }
            }
        });
    }

    // the type array
    private static final String[] SORTED_TYPE_ARRAY = new String[] {"gc", "class", "cpu", "load", "mem", "net", "http", "netty", "jetty", "thread", "sys", "runtime"};

    /**
     *  trans the event to target show text here
     *
     * @param eventMap the origin event map
     * @return  text ui
     */
    private TextWebSocketFrame transToWebSocketFrame(Map<String, CounterEvent> eventMap) {

        // check
        if (eventMap.isEmpty()) {
            return new TextWebSocketFrame("Nothing can be collected, check your monitor type!\n");
        }

        StringBuilder usb = new StringBuilder();

//        // thread, maybe use event.type is right
//        String header = newHeader("Thread Dynamic Statistic");
//        usb.append(header);
//        CounterEvent counterEvent = eventMap.get("thread");
//        if (counterEvent != null) {
//            usb.append(counterEvent.event()).append("\n");
//        }

        // others
        CounterEvent counterEvent;
        String header;
        //usb.append(ConsoleUtils.newLineFixConsoleWidth()).append("\n");
        for (String type : SORTED_TYPE_ARRAY) {
            counterEvent = eventMap.get(type);
            if (counterEvent == null) {
                continue;
            }

            // header
            header = newHeader(counterEvent.type());

            // event
            usb.append(header).append(counterEvent.event()).append("\n")
                    .append(ConsoleUtils.newLineFixConsoleWidth()).append("\n");
        }

        return new TextWebSocketFrame(usb.toString());
    }

    private String newHeader(String title) {
        StringBuilder usb = new StringBuilder();
        usb.append(ConsoleUtils.newLineFixConsoleWidth()).append("\n");

        int width = ConsoleUtils.getConsoleWidth();
        int startIndex = width / 2 - title.length();
        for (int i = 0; i < startIndex; i ++) {
            usb.append(" ");
        }
        usb.append(title).append("\n");

        usb.append(ConsoleUtils.newLineFixConsoleWidth()).append("\n");
        return usb.toString();
    }

    public static void main(String[] args) {
        String s = new DefaultMonitorEventConsumer().newHeader("test test");
        System.out.println(s);
    }

}
