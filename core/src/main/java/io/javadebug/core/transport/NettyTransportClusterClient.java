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
//  Author : HJ


package io.javadebug.core.transport;

import io.javadebug.core.utils.CDHelper;
import io.javadebug.core.CommandExecuteListener;
import io.javadebug.core.CommandRequestHandler;
import io.javadebug.core.Constant;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.console.CommandSink;
import io.javadebug.core.console.CommandSource;
import io.javadebug.core.console.ConsoleCommandSink;
import io.javadebug.core.console.ConsoleCommandSource;
import io.javadebug.core.console.JLineConsole;
import io.javadebug.core.handler.ClientCommandRequestHandler;
import io.javadebug.core.handler.MultiRemoteClientHandler;
import io.javadebug.core.handler.ServerIdleHandler;
import io.javadebug.core.ui.SimplePSUI;
import io.javadebug.core.ui.UI;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2019/4/28 14:08.
 *
 *   用于同时向多个服务端发起连接，连接单个服务端可以使用 {@link NettyTransportClient}
 *   本类的实现和{@link NettyTransportClient} 不一样，它需要和多个Server发起连接，并且
 *   和多个服务端进行命令传输与响应解析，情况要复杂得多
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class NettyTransportClusterClient {

    //----------------------------------------------
    /// 当前服务端集群的连接参数配置，简单包括ip + port
    /// 当根据这个每个 {@code Connection}生成一条和服务端
    /// 的连接的时候，需要将该连接赋值给Connection,通过方法
    //  {@code Connection.assignConnect}即可完成
    //----------------------------------------------
    private Set<Connection> connectionSet;

    //----------------------------------------------
    /// 这是在周期性探测过程中发现的不可用的连接
    //----------------------------------------------
    private Set<Connection> unavailableConnectSet;

    //-------------------------------------------------
    /// 用于处理客户端命令输入、转换成可传输的协议的handler
    /// 使用这个handler需要协同处理get和set，该handler提供了两个
    /// 接口，onCreateRequest和get；前者用于解析好了客户端的输入的
    /// 时候需要触发的接口，而后者将接收到前者传递进来的准备好的传输体
    /// 类似于一个简单的Observable和Observer的关系
    //-------------------------------------------------
    private CommandRequestHandler commandRequestHandler;

    //-------------------------------------------------
    /// 用于将错误信息、服务端响应结果打印出来的组件，这个组件非常独立，和
    /// 系统主流程没有任何关联关系，除了RemoteCommand结构；
    //-------------------------------------------------
    private UI ui = new SimplePSUI();

    //-------------------------------------------------------
    /// 命令响应处理组件，分批次
    //-------------------------------------------------------
    private TransportSubject transportSubject;

    //----------------------------------------------
    /// 用于做一些周期性任务，比如剔除不可用的连接
    //----------------------------------------------
    private ScheduledExecutorService scheduledExecutorService;

    //-----------------------------------------------
    /// 是否已经连接过了，如果连接过了，无法再次连接
    //-----------------------------------------------
    private volatile boolean isConnected = false;

    //---------------------------------------------------
    /// 客户端处理器
    //---------------------------------------------------
    private MultiRemoteClientHandler multiRemoteClientHandler;

    //--------------------------------
    /// 事件循环
    //--------------------------------
    private EventLoopGroup eventLoopGroup;

    //-----------------
    /// 命令行提示符
    //-----------------
    private static final String PROMPT = /*Constant.ANSI_GREEN +*/  "javadebug:_>";

    //--------------------------------
    /// 判断一下是否是在灰度期间，如果是的话，则
    /// 将灰度连接赋值给这个属性，当开启全量的时候
    /// 再设置为null
    //--------------------------------
    private Connection greyConnection = null;

    //---------------------------------
    // bootstrap
    //---------------------------------
    private Bootstrap bootstrap;

    //-----------------------------------
    /// 用于生成命令输入
    //-----------------------------------
    private static CommandSource commandSource;

    //-----------------------------------
    /// 用于处理命令响应
    //-----------------------------------
    private static Set<CommandSink> commandSinkSet;

    // -----------------------------------
    /// 命令执行监听，每个安装的Listener都会被回调，不保证回调顺序
    // -----------------------------------
    private static Set<CommandExecuteListener> executeListeners;

    /**
     *  需要提供所有需要连接的ip:port字符串，可以有两种模式
     *  case 1:
     *      address = "ip:port,ip:port"
     *  case 2:
     *      address = "ip:port,ip,port"
     *
     * @param address 逗号分隔地址，仅提供port则连接"127.0.0.1:port"，仅提供ip则默认连接
     *                "ip:11234",默认ip port可以参考 {@link io.javadebug.core.Constant}
     */
    public NettyTransportClusterClient(String address) {
        PSLogger.error("start to parse address:" + address);
        // 地址解析
        connectionSet = new HashSet<>();
        doParseAddress(address, connectionSet);
        PSLogger.error("start to connect remote server:" + connectionSet);

        PSLogger.error("start to init request command input handler");
        commandRequestHandler = new ClientCommandRequestHandler();

        PSLogger.error("start to init transportSubject");
        transportSubject = new CDLatchTransportSubject(this.getAvailableConnectionSet());

        PSLogger.error("start to init default command source");
        try {
            commandSource = new JLineConsole();
        } catch (IOException e) {
            commandSource = new ConsoleCommandSource();
        }

        PSLogger.error("start to init default command sink");
        commandSinkSet = new HashSet<>();
        commandSinkSet.add(new ConsoleCommandSink());

        PSLogger.error("start to init the listener set");
        executeListeners = new HashSet<>();

    }

    /**
     * 目前仅支持一个source，后续可能支持从多个source读取命令
     *
     * 小技巧:设置为null可以停止生产ml输入
     *
     * @param source {@link CommandSource}
     */
    public static void setCommandSource(CommandSource source) {
        if (source == null) {
            CDHelper.set("commandSource", 1);
        } else {
            CDHelper.cd("commandSource");
        }
        commandSource = source;
    }

    /**
     *  某些情况下，可能需要获取到source，然后进行命令输入
     *
     * @return {@link CommandSource}
     */
    public static CommandSource getCommandSource() {
        return commandSource;
    }

    /**
     *  当前支持多个sink，在命令执行完成之后会将结果依次写到sink中去
     *
     * @param commandSink sink
     */
    public static void addCommandSink(CommandSink commandSink) {
        if (commandSink == null) {
            return;
        }
        commandSinkSet.add(commandSink);
    }

    /**
     *  安装一个命令执行过程监听器
     *
     * @param listener {@link CommandExecuteListener}
     */
    public static void addCommandExecuteListener(CommandExecuteListener listener) {
        if (listener != null) {
            executeListeners.add(listener);
        }
    }

    /**
     *  卸载一个命令监听器
     *
     * @param listener 监听器 {@link CommandExecuteListener}
     * @return 是否卸载成功
     */
    public static boolean removeExecuteListener(CommandExecuteListener listener) {
        return listener != null && executeListeners.remove(listener);
    }

    /**
     * 在某些情况下，不再需要监听命令执行结果，那么可以将sink移除
     *
     * @param sink 需要移除的sink
     * @return 是否移除成功
     */
    public static boolean removeSink(CommandSink sink) {
        return sink != null && commandSinkSet.remove(sink);
    }

    /**
     *  获取到可用的连接
     *
     * @return set
     */
    public Set<Connection> getAvailableConnectionSet() {
        if (this.connectionSet == null) {
            this.connectionSet = new HashSet<>();
        }
        return this.connectionSet;
    }

    /**
     *  获取到不可用的连接
     *
     * @return set
     */
    public Set<Connection> getUnavailableConnectionSet() {
        if (this.unavailableConnectSet == null) {
            this.unavailableConnectSet = new HashSet<>();
        }
        return this.unavailableConnectSet;
    }

    /**
     *  这是入口
     *
     */
    public void start() throws Exception {
        // 连接
        doConnect();

        PSLogger.error("start to do schedule work.");

        // 开启周期性任务
        doScheduleWork();

        PSLogger.error("start to do cs-loop");

        // 开始进行命令处理
        doCSLoop();
    }

    private void doScheduleWork() {
        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "java-debug-scheduler-execute-thread");
                }
            });
        }

        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                // 连接剔除
                PSLogger.info("start to detect unavailable connect job.");

                Set<Connection> tmpUnavailableConnectSet = new HashSet<>();
                for (Connection connection : connectionSet) {
                    if (!connection.isConnected()) {
                        tmpUnavailableConnectSet.add(connection);
                    }
                }
                PSLogger.info("start to delete unavailable connect:" + tmpUnavailableConnectSet);

                for (Connection connection : tmpUnavailableConnectSet) {
                    connectionSet.remove(connection);
                    noticeOnRemoveConnectionListener(connection);
                }

                if (unavailableConnectSet == null) {
                    unavailableConnectSet = new HashSet<>();
                }
                unavailableConnectSet.addAll(tmpUnavailableConnectSet);

                // 是否存在失效的连接
                transportSubject.countdownConnect(tmpUnavailableConnectSet.size());

                PSLogger.info("end to detect unavailable connect job.");
            }
        }, 1, 1, TimeUnit.MINUTES);

    }

    /**
     *  解析地址，之后才能开始连接服务端
     *
     * @param address 地址字符串
     */
    private void doParseAddress(String address, Set<Connection> connections) {
        if (UTILS.isNullOrEmpty(address)) {
            String msg = "需要提供远程服务端地址列表";
            PSLogger.error(msg);
            throw new IllegalArgumentException(msg);
        }

        String[] multiAddressStr = address.split(",");
        for (String pp : multiAddressStr) {
            String[] ipPort = pp.split(":");
            if (ipPort.length == 1) {
                String val = ipPort[0];
                if (val.contains(".")) {
                    // ip address, using default port
                    connections.add(new Connection(val, Constant.DEFAULT_SERVER_PORT));
                } else {
                    // port, using 127.0.0.1
                    int port = Constant.DEFAULT_SERVER_PORT;
                    try {
                        port = Integer.parseInt(val);
                    } catch (Exception e) {
                        noticeOnExceptionListener(e);
                        PSLogger.error(String.format("错误的端口信息[%s]，使用默认端口:[%d]", val, port));
                    }
                    connections.add(new Connection(Constant.DEFAULT_SERVER_IP, port));
                }
            } else if (ipPort.length == 2) {
                String ip = ipPort[0];
                String portStr = ipPort[1];
                int port = Constant.DEFAULT_SERVER_PORT;
                try {
                    port = Integer.parseInt(portStr);
                } catch (Exception e) {
                    noticeOnExceptionListener(e);
                    PSLogger.error(String.format("错误的端口信息[%s]，使用默认端口:[%d]", portStr, port));
                }
                connections.add(new Connection(ip, port));
            } else {
                PSLogger.error("错误的地址信息:" + pp);
            }
        }

        PSLogger.info("获取到所有的连接配置：" + connections);
    }

    /**
     *  连接到所有的服务端
     *
     */
    private void doConnect() {
        if (this.connectionSet == null || this.connectionSet.isEmpty()) {
            String error = "there is no remote server to connect right now, please configure it";
            PSLogger.error(error);
            return;
        }

        if (isConnected) {
            PSLogger.error("already connected");
            return;
        }

        eventLoopGroup = new NioEventLoopGroup(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "JavaDebug-Netty-Client");
            }
        });

        /// 空转handler
        ServerIdleHandler serverIdleHandler = new ServerIdleHandler();

        /// console handler
        multiRemoteClientHandler = new MultiRemoteClientHandler(transportSubject);

        bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioSocketChannel.class)
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
                        /// 空闲时间超过阈值服务器主动关闭~
                        pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, 5, TimeUnit.MINUTES));
                        pipeline.addLast("serverIdleStateHandler", serverIdleHandler);

                        // print handler
                        pipeline.addLast("printHandler", new SimpleChannelInboundHandler<RemoteCommand>() {
                            /**
                             * <strong>Please keep in mind that this method will be renamed to
                             * {@code messageReceived(ChannelHandlerContext, I)} in 5.0.</strong>
                             * <p>
                             * Is called for each message of type {@code i}.
                             *
                             * @param ctx the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
                             *            belongs to
                             * @param msg the message to handle
                             * @throws Exception is thrown if an error occurred
                             */
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, RemoteCommand msg) throws Exception {
                                //PSLogger.info(ctx.channel() + " => Receive msg:" + msg);
                                ctx.fireChannelRead(msg);
                            }
                        });

                        // console handler
                        pipeline.addLast("clientHandler", multiRemoteClientHandler);
                    }
                });

        // connect to servers
        int sucConCnt = 0;
        for (Connection connection : this.connectionSet) {
            try {
                Channel channel = bootstrap.connect(connection.toAddress()).sync().channel();
                connection.assignConnect(channel);
                sucConCnt ++;

                PSLogger.error("连接到:" + connection);
            } catch (Exception e) {
                noticeOnExceptionListener(e);
                String msg = "could not connect to remote connect:" + connection + " " + e;
                noticeOnErrorListener("", null, msg, CommandExecuteListener.CommandExecuteStage.ON_CONNECT_TO_REMOTE_JVM);
                PSLogger.error(msg);
            }
        }

        /// 是否没有任何可用的连接
        if (sucConCnt == 0) {
            PSLogger.error("no available connect, stop console.");
            System.exit(1);
        }

        isConnected = true;

        PSLogger.info("connect done ~");
    }

    /**
     *
     * 成功的时候通知一下
     *
     * @param remoteCommand {@link RemoteCommand} 响应结果
     */
    private void noticeOnSuccessListener(RemoteCommand remoteCommand) {
        for (CommandExecuteListener listener : executeListeners) {
            listener.onResponse(remoteCommand);
        }
    }

    /**
     *  客户端推出去了，通知一下吧
     *
     */
    private void noticeClientExitListener() {
        for (CommandExecuteListener listener : executeListeners) {
            listener.onExit();
        }
    }

    /**
     *  命令处理失败的时候通知一下
     *
     * @param cmdLine 命令行输入
     * @param origin 原始协议内容
     * @param errorMsg 错误信息
     * @param stage 阶段
     */
    private void noticeOnErrorListener(String cmdLine, RemoteCommand origin, String errorMsg, CommandExecuteListener.CommandExecuteStage stage) {
        for (CommandExecuteListener listener : executeListeners) {
            listener.onError(cmdLine, origin, errorMsg, stage);
        }
    }

    /**
     *  新增连接，通知一下
     *
     * @param connection 新增的连接信息
     */
    private void noticeOnAddConnectionListener(Connection connection) {
        if (connection == null) {
            return;
        }
        for (CommandExecuteListener listener : executeListeners) {
            listener.onAddConnection(connection);
        }
    }

    /**
     *  移除连接的时候通知
     *
     * @param connection 被移除的连接信息
     */
    private void noticeOnRemoveConnectionListener(Connection connection) {
        if (connection == null) {
            return;
        }
        for (CommandExecuteListener listener : executeListeners) {
            listener.onRemoveConnection(connection);
        }
    }

    /**
     *  异常通知
     *
     * @param e 发送的异常
     */
    private void noticeOnExceptionListener(Throwable e) {
        if (e == null) {
            return;
        }
        for (CommandExecuteListener listener : executeListeners) {
            listener.onException(e);
        }
    }

    /**
     *  灰度模式开始后，将被选中用来进行灰度测试的连接通知
     *
     * @param connection 灰度连接
     */
    private void noticeOnGreyListener(Connection connection) {
        if (connection == null) {
            return;
        }
        for (CommandExecuteListener listener : executeListeners) {
            listener.onGreyConnection(connection);
        }
    }

    /**
     *  灰度模式结束了，通知一下
     *
     */
    private void noticeOnGreyEndListener() {
        for (CommandExecuteListener listener : executeListeners) {
            listener.onGreyEnd();
        }
    }

    /**
     *  仅在客户端执行的命令，不需要发送到服务端
     *
     * @param line 命令行
     * @return 输出
     */
    private String clientCommandExec(String line) {
        final OptionParser parser = new OptionParser();
        parser.accepts("p").withOptionalArg().ofType(String.class);
        parser.accepts("address").withOptionalArg().ofType(String.class);

        final OptionSet optionSet = parser.parse(line.split(" "));

        if (!optionSet.has("p")) {
            return "\n请指定-p参数\n";
        }
        String p = (String) optionSet.valueOf("p");
        if (UTILS.isNullOrEmpty(p)) {
            return "\n请指定-p参数\n";
        }
        StringBuilder resp = new StringBuilder();
        switch (p) {
            case "ac": // 可用连接
                resp.append("\n").append("可用连接").append("\n");
                for (Connection connection : getAvailableConnectionSet()) {
                    resp.append(connection).append("\n");
                }
                break;
            case "uc": // 被剔除的连接
                resp.append("\n").append("不可用连接").append("\n");
                for (Connection connection : getUnavailableConnectionSet()) {
                    resp.append(connection).append("\n");
                }
                break;
            case "grey": // 灰度控制，选择一台机器进行发布验证
                PSLogger.error("开始进行灰度连接选择");
                boolean choose = false;
                for (Connection connection : getAvailableConnectionSet()) {
                    if (!choose) {
                        PSLogger.error("选择灰度连接:" + connection);
                        connection.choose(true);
                        choose = true;
                        greyConnection = connection;
                        noticeOnGreyListener(greyConnection);
                        continue;
                    }
                    connection.choose(false);
                }
                transportSubject.greyController(1);
                break;
            case "all": // 全量发布
                greyConnection = null;
                PSLogger.error("打开全量发布开关");
                for (Connection connection : getAvailableConnectionSet()) {
                    connection.choose(true);
                }
                noticeOnGreyEndListener();
                transportSubject.releaseGreyController(getAvailableConnectionSet().size());
                break;
            case "add": // 手动增加连接
            case "del": // 手动删除连接
                if (greyConnection != null) {
                    resp.append("灰度发布期间，不允许增减连接\n");
                    break;
                }
                if (!optionSet.has("address")) {
                    resp.append("无效的客户端命令，请使用-address参数提供追加连接的ip、port信息\n");
                    break;
                }
                String address = (String) optionSet.valueOf("address");
                if (UTILS.isNullOrEmpty(address)) {
                    resp.append("无效的客户端命令，请使用-address参数提供追加连接的ip、port信息\n");
                    break;
                }
                Set<Connection> cons = new HashSet<>();
                doParseAddress(address, cons);

                if (cons.isEmpty()) {
                    resp.append("无效的客户端命令，请使用-address参数提供追加连接的ip、port信息\n");
                    break;
                }

                // 执行操作
                if ("add".equals(p)) {
                    for (Connection connection : cons) {
                        try {
                            Channel channel = bootstrap.connect(connection.toAddress()).sync().channel();
                            connection.assignConnect(channel);
                            connectionSet.add(connection);
                            resp.append("增加:").append(connection).append(" 成功\n");
                            noticeOnAddConnectionListener(connection);
                        } catch (Exception e) {
                            String msg = "增加连接:" + connection + " 失败：" + e;
                            noticeOnErrorListener(line, null, msg, CommandExecuteListener.CommandExecuteStage.ON_CLIENT_COMMAND);
                            PSLogger.error(msg);
                        }
                    }
                } else {
                    Set<Connection> delSet = new HashSet<>();
                    for (Connection connection : cons) {
                        if (connectionSet.contains(connection)) {
                            delSet.add(connection);
                        }
                    }
                    if (!delSet.isEmpty()) {
                        connectionSet.removeAll(delSet);
                        resp.append("移除:").append(delSet).append(" 成功\n");
                        for (Connection c : delSet) {
                            noticeOnRemoveConnectionListener(c);
                        }
                    }

                    // 是否没有连接了
                    if (connectionSet.isEmpty()) {
                        noticeClientExitListener();
                        exitClient();
                        PSLogger.error("bye from console command...");
                    }
                }

                // another round
                transportSubject.anotherRound(connectionSet.size());
                break;
            case "choose": // 选择连接，只有被选择的连接才能进行命令发送
                resp.append("暂不支持\n");
                break;
            default:
                resp.append("\n不合法的参数 -p:").append(p).append("\n");
        }
        return resp.toString();
    }

    /**
     *  为了让用户更好的看到灰度的连接，这里处理一下，如果是在灰度期间，那么
     *  提示符变为灰度的连接信息
     *
     * @return "ip:port>"
     */
    private String getGreyAddress() {
       if (greyConnection != null) {
           return greyConnection.getIp() + ":" + greyConnection.getPort() + ">";
       }
       return "";
    }

    /**
     *  所谓的SCLoop，就是严格的客户端发送命令，等待服务端响应，然后再进行新一轮的
     *  发送命令，等待响应，因为需要和多个服务端交互，问题比较复杂，可能其中某些服务端
     *  响应特别慢，或者干脆不响应了，为了防止客户端卡死，应该做一些超时控制，并及时将
     *  不可用的服务端剔除掉;
     *
     *  简单来说，这个方法需要完成命令输入-命令传输-等待响应的过程
     *  【ctrl + c】停止
     */
    private void doCSLoop() throws Exception {
        OutputStream os = System.out;
        for (;;) {
            // 绘制提示符
            printPrompt(os, getGreyAddress());

            // 输入命令
            if (commandSource == null) {
                // await to get the source
                CDHelper.await("commandSource");
            }
            String line = commandSource.source();
            if (UTILS.isNullOrEmpty(line)) {
                continue;
            }

            /// 是否特殊命令，不需要发送到服务端执行
            if (line.startsWith("console")) {
                try {
                    String resp = clientCommandExec(line);
                    if (!UTILS.isNullOrEmpty(resp)) {
                        print(resp, os);
                    }
                    if (getAvailableConnectionSet().isEmpty()) {
                        noticeClientExitListener();
                        exitClient();
                        PSLogger.error("bye after console command：" + line);
                        System.exit(0);
                    }
                } catch (Exception e) {
                    noticeOnExceptionListener(e);
                    String msg = "clientExecute error：" + e;
                    noticeOnErrorListener(line, null, msg, CommandExecuteListener.CommandExecuteStage.ON_CLIENT_COMMAND);
                    PSLogger.error(msg);
                }
                continue;
            }

            /// 处理输入
            boolean isOk = true;
            for (Connection connection : connectionSet) {
                RemoteCommand remoteCommand;
                try {
                    remoteCommand = commandRequestHandler.onCreateRequest(line, connection.remoteCommand());
                } catch (Exception e) {
                    noticeOnExceptionListener(e);
                    String msg = "输入处理异常：" + e;
                    noticeOnErrorListener(line, null, msg, CommandExecuteListener.CommandExecuteStage.COMMAND_INPUT_HANDLE);
                    print(msg, os);
                    isOk = false;
                    break;
                }
                if (remoteCommand != null) {
                    // 非常滑稽 ..>..
                    connection.assignRemoteCommand(remoteCommand);
                } else {
                    isOk = false;
                    break;
                }
            }

            if (!isOk) {
                PSLogger.error("输入处理异常，请重新输入");
                continue;
            }

            /// 获取处理结果，并且发送出去
            int receiveCnt = 0;

            /// 如果拿到的协议和处理的协议不一致，一直等，按理来说应该立刻能拿到
            while (receiveCnt != connectionSet.size()) {
                commandRequestHandler.get();
                receiveCnt ++;
            }

            Set<Connection> removeSet = new HashSet<>();

            for (Connection connection : connectionSet) {
                try {
                    connection.writeAndFlush();
                } catch (Exception e) {
                    noticeOnExceptionListener(e);
                    PSLogger.error("writeAndFlush error:" + e);
                    noticeOnErrorListener(line, connection.remoteCommand(), e.toString(), CommandExecuteListener.CommandExecuteStage.SEND_COMMAND);
                    removeSet.add(connection);
                    if (unavailableConnectSet == null) {
                        unavailableConnectSet = new HashSet<>();
                    }
                    unavailableConnectSet.add(connection);
                    this.transportSubject.countdownConnect(1);
                }
            }
            if (!removeSet.isEmpty()) {
                for (Connection connection : removeSet) {
                    connectionSet.remove(connection);
                }
            }

            /// 拿到结果
            RemoteCommand resp = transportSubject.resp();

            /// 命令结果
            for (CommandSink sink : commandSinkSet) {
                sink.sink(resp);
            }

            /// 成功通知一下
            noticeOnSuccessListener(resp);

            if ("exit".equals(resp.getCommandName())) {
                noticeClientExitListener();
                PSLogger.error("ready to exit ...");
                exitClient();
                break;
            }

            /// 开启另外的一轮CS
            transportSubject.anotherRound(this.getAvailableConnectionSet().size());
        }
        PSLogger.error("bye from console ~");
    }

    /**
     *  退出客户端，关闭和服务端的链接
     *
     */
    public void exitClient() {
        // 关闭调度线程池
        scheduledExecutorService.shutdownNow();

        // 关闭EventLoop
        eventLoopGroup.shutdownGracefully();
    }

    private static void printPrompt(OutputStream os, String address) throws Exception {
        if (UTILS.isNullOrEmpty(address) || address.split(":").length != 2) {
            os.write(PROMPT.getBytes());
        } else {
            os.write((address).getBytes());
        }
        os.flush();
    }

    private static void print(String msg, OutputStream os) throws Exception {
        os.write(("\n" + msg + "\n").getBytes());
    }

    public static void main(String[] args) throws Exception {
        String address = "11234,11235";

        NettyTransportClusterClient nettyTransportClusterClient = new NettyTransportClusterClient(address);

        nettyTransportClusterClient.start();

    }

}
