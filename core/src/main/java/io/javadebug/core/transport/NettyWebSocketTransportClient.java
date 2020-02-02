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
import io.javadebug.core.console.CommandSource;
import io.javadebug.core.console.MonitorCollectorCommandSource;
import io.javadebug.core.handler.WebSocketClientHandler;
import io.javadebug.core.monitor.MonitorEventHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;

import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public enum NettyWebSocketTransportClient {
    NETTY_WEB_SOCKET_TRANSPORT_CLIENT
    ;

    private static final ThreadPoolExecutor EXECUTOR_SERVICE = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>(), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "Java-Debug-Tool-WebSocket-Client-Holder");
        }
    });

    private EventLoopGroup EVENT_EXECUTORS = null;
    private Channel CHANNEL  = null;
    private volatile boolean started = false;
    private String url = "";
    private WebSocketClientHandler handler = null;
    private MonitorCollectorCommandSource commandSource = new MonitorCollectorCommandSource();
    private volatile MonitorEventHandler monitorEventHandler;

    /**
     *  setup the webSocket client
     *
     * @param host the target host
     * @param port the target port
     * @throws Exception any exception
     */
    public void setupWebSocketClient(String host, int port, OutputStream ps) throws Exception {
        if (UTILS.isNullOrEmpty(host) || (port <= 1000 || port >= 65535)) {
            throw new IllegalArgumentException("invalid params, could not setup the webSocket client");
        }

        if (started) {
            PSLogger.info("the webSocket : [" + url + "] already setup !");
          return;
        }

        // init the monitor event handler
        if (monitorEventHandler == null) {
            monitorEventHandler = new MonitorEventHandler(ps, commandSource);
        }

        CountDownLatch latch = new CountDownLatch(1);
        EXECUTOR_SERVICE.execute(() -> {
            // the event loop
            EVENT_EXECUTORS = new NioEventLoopGroup(1, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "Java-Debug-Tool-WebSocket-Client-Worker");
                }
            });
            try {
                url = "ws://" + host + ":" + port + "/websocket";
                URI uri = new URI(url);

                PSLogger.error("the webSocket connect url : " + url);

                // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
                // If you change it to V00, ping is not supported and remember to change
                // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
                 handler = new WebSocketClientHandler(
                                WebSocketClientHandshakerFactory
                                        .newHandshaker(uri, WebSocketVersion.V13, null,
                                                true, new DefaultHttpHeaders()), monitorEventHandler);

                Bootstrap b = new Bootstrap();
                b.group(EVENT_EXECUTORS)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline p = ch.pipeline();
                                p.addLast(
                                        new HttpClientCodec(),
                                        new HttpObjectAggregator(8192),
                                        WebSocketClientCompressionHandler.INSTANCE,
                                        handler);
                            }
                        });

                CHANNEL = b.connect(uri.getHost(), port).sync().channel();
                handler.handshakeFuture().sync();
                started = true;
                latch.countDown();
                // setup
                //MonitorEventHandler.monitorReq();

                // wait and send to remote webSocket server
                while (true) {
                    String msg = commandSource.source();
                    if (msg == null) {
                        break;
                    } else if ("bye".equals(msg.toLowerCase())) {
                        CHANNEL.writeAndFlush(new CloseWebSocketFrame());
                        CHANNEL.closeFuture().sync();
                        break;
                    } else if ("ping".equals(msg.toLowerCase())) {
                        WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[] { 8, 1, 8, 1 }));
                        CHANNEL.writeAndFlush(frame);
                    } else {
                        WebSocketFrame frame = new TextWebSocketFrame(msg);
                        CHANNEL.writeAndFlush(frame);
                    }
                }
            } catch (Exception e) {
                PSLogger.error("error : " + url, e);
            } finally {
                EVENT_EXECUTORS.shutdownGracefully();
                PSLogger.error("the webSocket client shutdown now : " + url);
            }
        });
       try {
           latch.await(10, TimeUnit.SECONDS);
       } catch (Exception e) {
           PSLogger.error("could not setup the webSocket client to : " + url, e);
       }
    }

    public CommandSource getCommandSource() {
        return commandSource;
    }

    public boolean isStarted() {
        return started;
    }

    public void shutdown() {
        try {
            EVENT_EXECUTORS.shutdownGracefully();
            started = false;
            PSLogger.error("success to shutdown the webSocket client : " + url);
        } catch (Exception e) {
            PSLogger.error("could not shutdown the webSocket client : " + url, e);
        }
    }

}
