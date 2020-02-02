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

import io.javadebug.core.CommandRequestHandler;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.console.CommandSink;
import io.javadebug.core.console.CommandSource;
import io.javadebug.core.console.ConsoleCommandSource;
import io.javadebug.core.console.JLineConsole;
import io.javadebug.core.exception.CommandExecuteWithStageException;
import io.javadebug.core.handler.ClientCommandRequestHandler;
import io.javadebug.core.handler.ClientHandler;
import io.javadebug.core.handler.ServerIdleHandler;
import io.javadebug.core.hook.JavaDebugToolRuntimeHook;
import io.javadebug.core.hook.OnCommandInputHookImpl;
import io.javadebug.core.hook.OnCommandRespHookForMonitorCollector;
import io.javadebug.core.hook.OnCommandToUIHookImpl;
import io.javadebug.core.hook.OnErrorHookImpl;
import io.javadebug.core.hook.RuntimeStage;
import io.javadebug.core.monitor.MonitorEventHandler;
import io.javadebug.core.ui.SimplePSUI;
import io.javadebug.core.ui.UI;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2019/4/20 12:44.
 *
 *   特此说明：本类仅作为附属类随从java-debug发布，仅作为一个命令行客户端提供命令处理
 *   展示界面，并且仅能连接到一台jvm上，无法同时向多个局面发送命令，如果有需要，
 *   你可以使用 {@link NettyTransportClusterClient} 来作为你的客户端
 *   实现，这个客户端提供了可扩展的接口，包括Source和Sink等，方便外部系统来对接
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class NettyTransportClient {

    private static final OutputStream os = System.out;
    private static final String COMMON_REMOTE_RESP_KEY  =       "$back-data";

    // default config

    private String targetIp = "127.0.0.1";
    private int targetPort = 11234;
    private CommandRequestHandler commandRequestHandler = new ClientCommandRequestHandler();

    /// 用于展示响应信息的类
    private UI ui = new SimplePSUI();

    private Channel channel;

    private volatile boolean shutdown = false;

    private volatile  boolean silenceMode = false;

    private static volatile boolean monitorMode = false;

    private BlockingQueue<RemoteCommand> RECEIVE_MQ = new LinkedBlockingQueue<>();

    /// 这是这个客户端使用的命令对象，不要随意变更
    private RemoteCommand remoteCommand;

    private static final RemoteCommand ERROR_REMOTE_COMMAND =
            new RemoteCommand().addParam("$back-errorCode", "-1").addParam("$back-errorMsg", "错误发生");

    private int contextId = -1;

    private Bootstrap bootstrap = null;

    //-----------------------------------
    /// 用于生成命令输入
    //-----------------------------------
    private volatile static CommandSource commandSource;

    static {
        try {
            commandSource = new JLineConsole();
            PSLogger.info("use jLine console as command source.");
        } catch (IOException e) {
            PSLogger.error("could not get the jLine condole, fallback to console command source", e);
            commandSource = new ConsoleCommandSource();
        }
    }

    //-----------------------------------
    /// 用于处理命令响应
    //-----------------------------------
    private static Set<CommandSink> commandSinkSet = new HashSet<>();

    public NettyTransportClient setCommandSource(CommandSource source) {
        if (source == null) {
            return this;
        }
        commandSource = source;

        // show the command source info, maybe a method like "toString" is needed
        // by command source / command sink
        PSLogger.error("using new command source :" + source.getClass().getName() );

        return this;
    }

    public static void setMonitorMode(boolean monitorMode) {
        NettyTransportClient.monitorMode = monitorMode;
    }

    public NettyTransportClient addCommandSink(CommandSink sink) {
        if (sink == null) {
            return this;
        }
        commandSinkSet.add(sink);
        return this;
    }

    public NettyTransportClient setIP(String ip) {
        if (UTILS.isNullOrEmpty(ip)) {
            return this;
        }
        this.targetIp = ip;
        return this;
    }

    public NettyTransportClient setPort(int port) {
        if (port < 1000 || port > 65535) {
            return this;
        }
        this.targetPort = port;
        return this;
    }

    public NettyTransportClient silence(boolean set) {
        this.silenceMode = set;
        if (set) {
            PSLogger.error("using silence mode .");
        }
        return this;
    }

    private boolean isSilenceMode() {
        return this.silenceMode;
    }

    public NettyTransportClient() {
        // using default config
    }

    public NettyTransportClient(String targetIp, int targetPort, CommandRequestHandler commandRequestHandler) {
        this.targetIp = targetIp;
        this.targetPort = targetPort;
        this.commandRequestHandler = commandRequestHandler;
    }

    public void shutdown() {
        PSLogger.error("shutdown console");
        this.shutdown = true;
    }

    /**
     *  收到消息写到这里，打印机会从这里拉取消息打印，客户端严格实现成了
     *  C -> S -> C -> S ...
     *  的模式，所以论上队列里面最多就一条消息，但是也不排除意外，打印机
     *  在一定时间之后会允许重新输入命令，这样之后MQ中就可能存在多条命令的
     *  响应结果
     *
     * @param remoteCommand {@link RemoteCommand}
     */
    public void receiveResp(RemoteCommand remoteCommand) {
        if (remoteCommand == null) {
            RECEIVE_MQ.offer(ERROR_REMOTE_COMMAND);
            return;
        }

        /// 这里使用offer是没有问题的

        try {
            RECEIVE_MQ.offer(remoteCommand);
        } catch (Exception e) {
            PSLogger.error("无法将相应结果写到MQ中:" + remoteCommand);
        }
    }

    /**
     *  当你想重置命令源的时候可以调用该方法
     *
     * @param commandRequestHandler {@link CommandRequestHandler} 新的命令源
     */
    public void resetCommandRequestHandle(CommandRequestHandler commandRequestHandler) {
        this.commandRequestHandler = commandRequestHandler;
    }

    public RemoteCommand getRemoteCommand() {
        if (this.remoteCommand == null) {
            return new RemoteCommand(); // 为了获取Context Id
        }
        return this.remoteCommand;
    }

    public void setRemoteCommand(RemoteCommand remoteCommand) {
        this.remoteCommand = remoteCommand;
        if (this.contextId < 0) {
            this.contextId = remoteCommand.getContextId();
            PSLogger.error("获取到Context ID:" + this.contextId);
        }
    }

    public int getContextId() {
        return this.contextId;
    }

    public void setContextId(int contextId) {
        this.contextId = contextId;
    }

    public void asyncInit() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                init();
            }
        }, "NettyTransportClient-Async-Init-Worker").start();
    }

    /**
     *  初始化客户端，需要连接到远程服务器
     *
     */
    public void init() {

//        // set client logger
//        PSLogger.setClientLogger();

        // register the default hook here [command input]
        JavaDebugToolRuntimeHook.Register.onCommandInput(new OnCommandInputHookImpl());

        // register the default hook here [command to ui]
        JavaDebugToolRuntimeHook.Register.onCommandToUI(new OnCommandToUIHookImpl());

        // register the default hook here [error]
        JavaDebugToolRuntimeHook.Register.onExecuteError(new OnErrorHookImpl());

        // get the ws port, the default is 20234
        String webSocketPort = System.getProperty("java-debug-tool.ws.server.port", "20234");
        int wsPort = UTILS.safeParseInt(webSocketPort, 20234);

        // register the default hook here [resp]
        JavaDebugToolRuntimeHook.Register.onCommendResp(new OnCommandRespHookForMonitorCollector(os, targetIp, wsPort));

        EventLoopGroup group = new NioEventLoopGroup(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "JavaDebug-Netty-Client");
            }
        });

        // console handle
        ClientHandler clientHandler = new ClientHandler(this);

        /// 空转handler
        ServerIdleHandler serverIdleHandler = new ServerIdleHandler();

        bootstrap = new Bootstrap()
                            .group(group).channel(NioSocketChannel.class)
                            .option(ChannelOption.WRITE_BUFFER_WATER_MARK,
                                    new WriteBufferWaterMark(4 * 1024, 8 * 1024))
                            .handler(new ChannelInitializer<SocketChannel>() {
                                /**
                                 * This method will be called once the {@link Channel} was registered. After the method returns this instance
                                 * will be removed from the {@link ChannelPipeline} of the {@link Channel}.
                                 *
                                 * @param ch the {@link Channel} which was registered.
                                 * @throws Exception is thrown if an error occurs. In that case it will be handled by
                                 *                   {@link #exceptionCaught(ChannelHandlerContext, Throwable)} which will by default close
                                 *                   the {@link Channel}.
                                 */
                                @Override
                                protected void initChannel(SocketChannel ch) throws Exception {

                                    ChannelPipeline pipeline = ch.pipeline();

                                    // codec
                                    pipeline.addLast("commandDecoder", CommandCodec.getDecodeHandler());
                                    pipeline.addLast("commandEncoder", CommandCodec.getEncodeHandler());

                                    // idle handler
                                    /// 空闲时间超过阈值主动关闭连接~
                                    pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, 5, TimeUnit.MINUTES));
                                    pipeline.addLast("serverIdleStateHandler", serverIdleHandler);

                                    // handle
                                    pipeline.addLast("clientHandler", clientHandler);
                                }
                            });

        PSLogger.error(UTILS.format("start to connect [%s:%d]", targetIp, targetPort));

        try {
            channel = bootstrap.connect(targetIp, targetPort).sync().channel();
        } catch (InterruptedException e) {
            PSLogger.error(UTILS.format("fail to connect [%s:%d], e=%s", targetIp, targetPort, UTILS.getErrorMsg(e)));
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!shutdown) {

                    RemoteCommand remoteCommand = commandRequestHandler.get();
                    if (remoteCommand == null) {
                        continue;
                    }

                    // write to remote

                    CountDownLatch countDownLatch = new CountDownLatch(1);

                    int waitSec = 0;
                    /// 判断一下
                    if (!channel.isWritable()) {

                        PSLogger.error("try another connection ...");
                        newConnection();

                        // 尝试等待一分钟
                        while (!channel.isWritable() && waitSec < 60) {
                            PSLogger.error("当前Channel不可写");
                            try {
                                TimeUnit.SECONDS.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } finally {
                                waitSec ++;
                            }
                        }
                    }

                    /// 关闭该链接
                    if (!channel.isWritable()) {
                        /// console channel

                        CountDownLatch closeClientCD = new CountDownLatch(1);

                        channel.close().addListener(new GenericFutureListener<Future<? super Void>>() {
                            @Override
                            public void operationComplete(Future<? super Void> future) throws Exception {
                                if (future.isSuccess()) {
                                    PSLogger.error("链接关闭成功");
                                    shutdown();
                                } else {
                                    PSLogger.error("链接无法关闭");
                                }
                                closeClientCD.countDown();
                            }
                        });

                        /// wait to close the channel
                        try {
                            closeClientCD.await();
                        } catch (InterruptedException e) {
                            PSLogger.error(e.getMessage());
                        }

                        // stop consumer thread
                        continue;
                    }

                    channel.writeAndFlush(remoteCommand).addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {

                            // notice
                            JavaDebugToolRuntimeHook.Notice.onCommandSend(remoteCommand, RuntimeStage.DEBUG_COMMAND_SENT);

                            if (future.isSuccess()) {
                                //PSLogger.info("成功发送一个命令到服务端");
                            } else {
                                try {
                                    future.get();
                                } catch (Exception e) {
                                    PSLogger.error("命令发送失败:" + e);
                                    JavaDebugToolRuntimeHook.Notice.onExecuteError(new CommandExecuteWithStageException(e, RuntimeStage.DEBUG_COMMAND_SENT), RuntimeStage.ERROR);
                                }
                            }
                            countDownLatch.countDown();
                        }
                    });
                    try {
                        countDownLatch.await();
                    } catch (InterruptedException e) {
                        PSLogger.error("await server response error:" + UTILS.getErrorMsg(e));
                    }
                }
            }
        }).start();

        try {
            handleLoopEvent(targetIp + ":" + targetPort);
        } catch (IOException e) {
            PSLogger.error("循环处理失败:" + UTILS.getErrorMsg(e));
            System.exit(1);
        }

    }

    private void newConnection() {
        try {

            PSLogger.error("start to close the idle channel : " + channel);

            channel.close().sync();
        } catch (InterruptedException e) {
            PSLogger.error("could not close the channel : " + channel);
            return;
        }

        // new connection
        try {
            channel = bootstrap.connect(targetIp, targetPort).sync().channel();
        } catch (InterruptedException e) {
            PSLogger.error("could not do connect to " + targetIp + ":" + targetPort);
        }

        PSLogger.error("using new connection : " + channel);
    }

    private void noticeSink(String msg) {
        if (remoteCommand != null) {
            remoteCommand.addParam("$$-notice", msg);
            for (CommandSink sink : commandSinkSet) {
                sink.sink(remoteCommand);
            }
        } else {
            for (CommandSink sink : commandSinkSet) {
                sink.sink(null);
            }
        }
    }

    private static final String PROMPT = /*Constant.ANSI_GREEN +*/  "javadebug:_>";

    private void handleLoopEvent(String address) throws IOException {

        // loop to active c/s model
        while (!shutdown) {

            /// show the prompt
            printPrompt(os, address);

            //String line = br.readLine();

            // using the command source to produce the command
            String line = commandSource.source();
            if (UTILS.isNullOrEmpty(line)) {
                continue;
            }

            // in monitor mode, the input will be fuck.
            if (monitorMode) {

                switch (line) {
                    case "q": {
                        // exit the monitor mode
                        PSLogger.error("stop the monitor action");
                        MonitorEventHandler.stop();
                    }
                }

                continue;
            }

            // NOTICE
            JavaDebugToolRuntimeHook.Notice.onCommandInput(line, RuntimeStage.COMMAND_INPUT);

            RemoteCommand remoteCommand = getRemoteCommand();

            remoteCommand.clearShit();

            try {
                remoteCommand = commandRequestHandler.onCreateRequest(line, remoteCommand);

                // NOTICE
                JavaDebugToolRuntimeHook.Notice.onCommandCreate(remoteCommand, RuntimeStage.GENERATE_COMMAND_PROTOCOL);

            } catch (Exception e) {
                print("Invalid command：" + e, os);
                noticeSink("输入处理异常：" + e);
                JavaDebugToolRuntimeHook.Notice.onExecuteError(new CommandExecuteWithStageException("input handler error", e, RuntimeStage.GENERATE_COMMAND_PROTOCOL), RuntimeStage.ERROR);
                continue;
            }
            if (remoteCommand == null) {
                print("Invalid command", os);
                noticeSink("输入处理异常，请重新输入");
                continue; // 避免客户端假死
            }

            // 拉取消息
            try {

                // block to get the response from server
                RemoteCommand resp = RECEIVE_MQ.take();

                // notice
                JavaDebugToolRuntimeHook.Notice.onCommendResp(resp, RuntimeStage.DEBUG_COMMAND_RESP);

                // show string
                String show = ui.toUI(resp);

                // notice
                JavaDebugToolRuntimeHook.Notice.onCommandToUI(show, RuntimeStage.DEBUG_COMMAND_SHOW);

                /// 对结果进行处理
                if ((resp.getCommandName().equals("monitor")
                        || resp.getCommandName().equals("collect"))
                        && "ok".equals(resp.getParam(COMMON_REMOTE_RESP_KEY))) {
                    // just ignore the resp
                    PSLogger.error("start to monitor ...");
                    //MonitorEventHandler.start();
                } else {
                    print(show, os);
                }

                // sink
                for (CommandSink sink : commandSinkSet) {
                    sink.sink(resp);
                }

                /// another message
                while (!RECEIVE_MQ.isEmpty()) {
                    resp = RECEIVE_MQ.take();

                    /// 对结果进行处理
                    print(ui.toUI(resp), os);
                }

            } catch (InterruptedException e) {
                PSLogger.error("Fail to poll response from resp-mq, continue shell.", e);
            }

            // 是否退出
            if ("exit".equals(remoteCommand.getCommandName())) {
                channel.close().addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        if (future.isSuccess()) {
                            PSLogger.error("客户端关闭成功：" + channel.remoteAddress());
                        } else {
                            try {
                                future.get();
                            } catch (Exception e) {
                                PSLogger.error("关闭客户端失败:" + e);
                            }
                        }
                    }
                });
                break;
            }
        }
        PSLogger.error("Bye~");
        System.exit(0);
    }

    private void printPrompt(OutputStream os, String address) throws IOException {
        if (isSilenceMode()) {
            return;
        }
        if (UTILS.isNullOrEmpty(address) || address.split(":").length != 2) {
            os.write(PROMPT.getBytes());
            os.flush();
        } else {
            os.write((address + ">").getBytes());
            os.flush();
        }
    }

    private void print(String msg, OutputStream os) throws IOException {
        if (silenceMode) {
            return;
        }
        os.write(("\n" + msg + "\n").getBytes());
    }

}