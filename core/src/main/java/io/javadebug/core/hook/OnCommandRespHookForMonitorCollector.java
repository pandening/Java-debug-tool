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


package io.javadebug.core.hook;

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.monitor.MonitorEventHandler;
import io.javadebug.core.transport.NettyWebSocketTransportClient;
import io.javadebug.core.transport.RemoteCommand;
import io.javadebug.core.utils.UTILS;

import java.io.OutputStream;

public class OnCommandRespHookForMonitorCollector implements DefaultHookImpl<RemoteCommand, Void> {

    private static final String COMMON_REMOTE_RESP_KEY  =       "$back-data";

    private OutputStream os;
    private String host;
    private int port;

    public OnCommandRespHookForMonitorCollector(OutputStream ps, String remote, int port) {
        this.os = ps;
        this.host = remote;
        this.port = port;
    }

    /**
     * check the data before you consume it!
     *
     * @param stage the stage of current
     * @return true means you will consume the data,
     * the {@link HookOperator#apply(Object)} method will call with param {@see data}
     */
    @Override
    public boolean isInterest(RuntimeStage stage) {
        return stage.equals(RuntimeStage.DEBUG_COMMAND_RESP);
    }

    /**
     * Applies this function to the given argument.
     *
     * @param remoteCommand the function argument
     * @return the function result
     */
    @Override
    public Void apply(RemoteCommand remoteCommand) {

        // start to monitor
        if (remoteCommand.getCommandName().equals("monitor")
                || remoteCommand.getCommandName().equals("collect")) {

            String resp = remoteCommand.getParam(COMMON_REMOTE_RESP_KEY);
            String monitorType = remoteCommand.getParam("$forward-monitor-type");
            String intervalOp    = remoteCommand.getParam("$forward-monitor-interval");
            int interval  = UTILS.safeParseInt(intervalOp, -1);

            if (UTILS.isNullOrEmpty(monitorType)) {
                PSLogger.error("command not set monitor type, set default type : 'thread'");
                monitorType = "thread";
            }

            if ("ok".equals(resp)) {
                boolean started = NettyWebSocketTransportClient.NETTY_WEB_SOCKET_TRANSPORT_CLIENT.isStarted();
                if (started) {
                    MonitorEventHandler.start(monitorType, interval);
                    PSLogger.error("another round monitor command, the client already setup ...");
                    return null;
                }

                // start to setup the client
                try {
                    NettyWebSocketTransportClient.NETTY_WEB_SOCKET_TRANSPORT_CLIENT
                            .setupWebSocketClient(host, port, os);
                } catch (Exception e) {
                    PSLogger.error("could not setup the webSocket client", e);
                }

                // check
                started = NettyWebSocketTransportClient.NETTY_WEB_SOCKET_TRANSPORT_CLIENT.isStarted();
                if (started) {
                    PSLogger.error("start to setup the MonitorEventHandler ...");
                    MonitorEventHandler.start(monitorType, interval);
                } else {
                    PSLogger.error("could not start the webSocket");
                }

            }

        }

        return null;
    }
}
