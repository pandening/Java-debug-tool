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


/**
 *   Copyright © 2019-XXX HJ All Rights Reserved
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
//  Auth : HJ


package io.javadebug.core.transport;

import io.javadebug.core.Configure;
import io.javadebug.core.Constant;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.RemoteServer;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.exception.ConfigureErrorException;
import io.javadebug.core.handler.CommandHandler;
import io.javadebug.core.handler.ServerIdleHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 2019/4/20 12:44.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public enum NettyTransportServer implements RemoteServer {
    NETTY_TRANSPORT_SERVER;

    /**
     *  获取到Netty服务器
     *
     * @return {@code NETTY_TRANSPORT_SERVER}
     */
    public static RemoteServer getNettyTransportServer(Instrumentation instrumentation) {
        ins = instrumentation;
        return NETTY_TRANSPORT_SERVER;
    }

    /**
     *  有时候你只是想获取到这个对象
     *
     * @return 这个对象
     */
    public static RemoteServer getNettyTransportServer() {
        return NETTY_TRANSPORT_SERVER;
    }

    private ChannelFuture channelFuture;

    static Instrumentation ins;

    private volatile boolean setup = false;

    //---------------------------------------------
    /// 是否允许控制探测器，如果安装了，那么当一个连接空闲时间
    /// 超过阈值之后，这个连接就会被关闭
    //---------------------------------------------
    private volatile boolean allowIdleHandler = true;

    private CommandHandler commandHandler;

    private static AtomicInteger threadNoCnt = new AtomicInteger(0);

    private static EventLoopGroup bossGroup = null;

    private static EventLoopGroup workGroup = null;

    public RemoteServer setAllowIdleHandler(boolean allowIdleHandler) {
        if (isBind()) {
            throw new IllegalStateException("服务端已经启动，无法设置该参数");
        }
        this.allowIdleHandler = allowIdleHandler;
        return this;
    }

    /**
     * 判断服务端是否已经就绪了
     *
     * @return true代表服务端已经就绪，可以accept
     */
    @Override
    public boolean isBind() {
        return this.setup;
    }

    /**
     * 执行服务端bind等逻辑，不同类型的服务端初始化过程可能不一样，但是执行完该方法之后，服务端
     * 必须有能力处理客户端连接；
     *
     * @param configure {@link Configure}
     */
    @Override
    public void start(Configure configure) throws Exception {
        if (configure == null) {
            throw new ConfigureErrorException("配置信息不完整");
        }

        // init server logger
        PSLogger.initServerLogger();

        bossGroup = new NioEventLoopGroup(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("JavaDebug-NettyServer-Boss-Worker-" + threadNoCnt.incrementAndGet());
                return thread;
            }
        });

        workGroup = new NioEventLoopGroup(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("JavaDebug-NettyServer-EventLoop-Worker-" + threadNoCnt.incrementAndGet());
                return thread;
            }
        });

        ((NioEventLoopGroup)workGroup).setIoRatio(70);

        ServerBootstrap serverBootstrap = new ServerBootstrap().group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 4)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                /// 高低水位，用于保护客户端
                /// 当输出缓冲区中缓存数据大于高水位后，Channel变得不可写，当不可写之后，缓存区的数据大小
                /// 低于低水位之后，Channel再次变得可写,每次写的时候请判断Channel是否可以
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                        new WriteBufferWaterMark(4 * 1024, 8 * 1024))
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        /// 空转handler
        ServerIdleHandler serverIdleHandler = new ServerIdleHandler();

        // command handler
        commandHandler = new CommandHandler(ins, this);

        // set up handle
        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            /**
             * This method will be called once the {@link Channel} was registered. After the method returns this instance
             * will be removed from the {@link ChannelPipeline} of the {@link Channel}.
             *
             * @param channel the {@link Channel} which was registered.
             * @throws Exception is thrown if an error occurs. In that case it will be handled by
             *                   {@link #exceptionCaught(ChannelHandlerContext, Throwable)} which will by default close
             *                   the {@link Channel}.
             */
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {

                if (!setup) {
                    PSLogger.error("服务端暂未启动或者不再接受新连接");
                    channel.writeAndFlush("服务端暂未启动或者不再接受新连接").addListener(new GenericFutureListener<Future<? super Void>>() {
                        @Override
                        public void operationComplete(Future<? super Void> future) throws Exception {
                            if (!future.isSuccess()) {
                                /// 拿到异常
                                future.get();
                            }
                        }
                    });
                    return;
                }

                ChannelPipeline pipeline = channel.pipeline();

                // codec
                pipeline.addLast("commandDecoder", CommandCodec.getDecodeHandler());
                pipeline.addLast("commandEncoder", CommandCodec.getEncodeHandler());

                // idle handler
                /// 空闲时间超过阈值服务器主动关闭链接~
                if (allowIdleHandler) {
                    pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, 10, TimeUnit.MINUTES));
                    pipeline.addLast("serverIdleStateHandler", serverIdleHandler);
                }

                // command handler
                pipeline.addLast("javaDebugCommandHandler", commandHandler);
            }
        });

        /// start to bind
        PSLogger.error(UTILS.format("start to bind on:%s:%d", configure.getTargetIp(), configure.getTargetPort()));

        this.channelFuture = serverBootstrap.bind(getAddress(configure.getTargetIp(), configure.getTargetPort()));

        this.channelFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                if (future.isSuccess()) {
                    setup = true;
                    PSLogger.error(UTILS.format("server started on %s:%d", configure.getTargetIp(), configure.getTargetPort()));
                } else {
                    try {
                        future.get();
                    } catch (Exception e) {
                        PSLogger.error(UTILS.format("could not started on %s:%d %s", configure.getTargetIp(), configure.getTargetPort(), e));
                    }
                }
            }
        });
    }

    /**
     *  获取到绑定的地址信息
     *
     * @param ip 绑定的ip地址
     * @param port 绑定的端口号，默认为 {@link Constant#DEFAULT_SERVER_PORT}
     *
     * @return {@link InetSocketAddress}
     */
    private InetSocketAddress getAddress(String ip, int port) {
        if (UTILS.isNullOrEmpty(ip)) {
            ip = Constant.DEFAULT_SERVER_IP;
        }
        if (port <= 0 || port > 65535) {
            PSLogger.error("error port range:" + port);
            port = Constant.DEFAULT_SERVER_PORT;
        }

        PSLogger.info("remote ip:port =>" + ip + ":" + port);

        if (Constant.DEFAULT_SERVER_IP.equals(ip)) {
            // local mode
            return new InetSocketAddress(port);
        } else {
            // remote mode
            return new InetSocketAddress(ip, port);
        }
    }

    /**
     * 客户端要求服务端关闭，这一般出现在最后一个debug的人发出了关闭服务端的命令，只要服务端还在
     * 为至少一个客户端服务，那么就不应该关闭服务端，所以服务端需要记录每一个连接，并有能力在需要
     * 的时候将服务端关闭；
     *
     * @param configure {@link Configure}
     */
    @Override
    public void stop(Configure configure) throws Exception {
        if (this.channelFuture != null) {
            this.channelFuture.channel().close();
        }
        /// stop boss ..
        bossGroup.shutdownGracefully();

        /// stop work..
        workGroup.shutdownGracefully();

        // stop command handler
        if (commandHandler != null) {
            commandHandler.close();
        }

        // shutdown status
        this.setup = false;
    }

    /**
     * 关闭服务端
     *
     * @throws Exception 处理异常
     */
    @Override
    public void stop() throws Exception {
        stop(null);
    }

    public static void main(String[] args) throws Exception {

        Configure configure = new Configure();

        configure.setTargetIp(Constant.DEFAULT_SERVER_IP);
        configure.setTargetPort(Constant.DEFAULT_SERVER_PORT);

        NETTY_TRANSPORT_SERVER.start(configure);

    }

}
