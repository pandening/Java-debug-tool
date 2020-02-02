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


package io.javadebug.core.transport;

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.monitor.MonitorHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public enum NettyWebSocketTransportServer {
    NETTY_WEB_SOCKET_TRANSPORT_SERVER
    ;

    private static final String WEB_SOCKET_PATH = "/websocket";
    private static final int PORT = UTILS.safeParseInt(System.getProperty("java-debug-tool.ws.server.port", "20234"), 20234);
    private static final MonitorHandler MONITOR_HANDLER = new MonitorHandler();
    private static final EventLoopGroup EVENT_GROUP = new NioEventLoopGroup(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "Java-Debug-Tool-WebSocket-Server-Worker");
        }
    });
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "Java-Debug-Tool-WebSocket-Server-Holder");
        }
    });

    // check
    private volatile boolean started = false;

    // the actual port
    private static int actualPort = PORT;

    /**
     *  set up the webSocket server
     *
     */
    public void initWebSocketServer(int port) {
        if (started) {
            PSLogger.error("the webSocket server already started on port : " + PORT);
            if (port != actualPort) {
                throw new IllegalStateException("the webSocket server already started on :" + actualPort);
            }
            return;
        }
        if (port <= 1000 || port >= 65535) {
            PSLogger.error("the provide port is invalid : " + port + ", set port as :" + PORT);
            port = PORT;
            actualPort = port;
        }
        CountDownLatch latch = new CountDownLatch(1);
        EXECUTOR_SERVICE.execute(() -> {
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(EVENT_GROUP, EVENT_GROUP)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {

                            /**
                             * This method will be called once the {@link Channel} was registered. After the method returns this instance
                             * will be removed from the {@link ChannelPipeline} of the {@link Channel}.
                             *
                             * @param ch the {@link Channel} which was registered.
                             * @throws Exception is thrown if an error occurs. In that case it will be handled by
                             *                    which will by default close
                             *                   the {@link Channel}.
                             */
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new HttpServerCodec());
                                pipeline.addLast(new HttpObjectAggregator(65536));
                                pipeline.addLast(new WebSocketServerCompressionHandler());
                                pipeline.addLast(new WebSocketServerProtocolHandler(WEB_SOCKET_PATH, null, true));
                                pipeline.addLast(MONITOR_HANDLER);
                            }
                        });

                Channel ch = b.bind(actualPort).sync().channel();
                started = true;
                latch.countDown();

                PSLogger.error("webSocket server started at : 127.0.0.1:" + actualPort);

                ch.closeFuture().sync();
            } catch (Exception e){
                PSLogger.error("error at initWebSocketServer", e);
            } finally {
                latch.countDown();
                EVENT_GROUP.shutdownGracefully();
                PSLogger.info(" EVENT_GROUP : shutdownGracefully");
            }
        });
        try {
            latch.await(10, TimeUnit.SECONDS);
            PSLogger.error("the latch countdown to zero, the webSocket server started.");
        } catch (Exception e) {
            PSLogger.error("error when wait the latch", e);
        }
    }

    public void shutdown() {
        try {
            EVENT_GROUP.shutdownGracefully();
        } catch (Exception e) {
            PSLogger.error("could not shutdown webSocket server", e);
        } finally {
            started = false;
        }
    }

    public boolean isStarted() {
        return started;
    }

    public static void main(String[] args) {
        NETTY_WEB_SOCKET_TRANSPORT_SERVER.initWebSocketServer(20134);
    }

}
