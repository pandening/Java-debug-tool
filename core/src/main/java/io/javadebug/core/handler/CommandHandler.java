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


package io.javadebug.core.handler;

import io.javadebug.core.CommandServer;
import io.javadebug.core.Constant;
import io.javadebug.core.RemoteServer;
import io.javadebug.core.ServerHook;
import io.javadebug.core.annotation.CommandDescribeUtil;
import io.javadebug.core.command.AgentInfoCommand;
import io.javadebug.core.command.AliveCommand;
import io.javadebug.core.command.BTraceCommand;
import io.javadebug.core.command.Command;
import io.javadebug.core.command.ExitCommand;
import io.javadebug.core.command.FindJavaSourceCommand;
import io.javadebug.core.command.HelpCommand;
import io.javadebug.core.command.ListCommand;
import io.javadebug.core.command.LockClassCommand;
import io.javadebug.core.command.MethodTraceCommand;
import io.javadebug.core.command.MonitorCollectorCommand;
import io.javadebug.core.command.OptionCommand;
import io.javadebug.core.command.RedefineClassCommand;
import io.javadebug.core.command.RollbackClassCommand;
import io.javadebug.core.command.perf.CpuTimeUsageCommand;
import io.javadebug.core.command.perf.ThreadCommand;
import io.javadebug.core.data.LRModel;
import io.javadebug.core.enhance.ClassMethodWeaver;
import io.javadebug.core.enhance.CustomInputMethodTraceAdviceImpl;
import io.javadebug.core.enhance.FullMatchMethodTraceAdviceImpl;
import io.javadebug.core.enhance.MethodAdvice;
import io.javadebug.core.enhance.MethodTraceFrame;
import io.javadebug.core.enhance.OnParamMatchMethodTraceAdviceIml;
import io.javadebug.core.enhance.OnReturnMethodTraceAdviceImpl;
import io.javadebug.core.enhance.OnThrowMethodTraceAdviceImpl;
import io.javadebug.core.enhance.RecordCountLimitMethodTraceAdviceImpl;
import io.javadebug.core.exception.ClientAuthFailException;
import io.javadebug.core.exception.CommandNotFindException;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.thirdparty.ThirdPartyAbility;
import io.javadebug.core.thirdparty.ThirdPartyRouter;
import io.javadebug.core.thirdparty.exception.ThirtPartyAbilityNotFindException;
import io.javadebug.core.transport.RemoteCommand;
import io.javadebug.core.utils.JacksonUtils;
import io.javadebug.core.utils.UTILS;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.Closeable;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import static io.javadebug.core.transport.CommandProtocol.*;

/**
 * Created on 2019/4/20 13:29.
 *
 * @author <a href="H.J"> HuJian </a>
 */
@ChannelHandler.Sharable
public class CommandHandler extends SimpleChannelInboundHandler<RemoteCommand> implements CommandServer, Closeable, ServerHook {

    // --------------------------------------------------------
    /// 字节码增强
    // --------------------------------------------------------
    private Instrumentation instrumentation;

    // --------------------------------------------------------
    /// 服务端，持有这个服务端引用，在关键时刻可以关闭服务端不再接受新连接等操作
    // --------------------------------------------------------
    private static RemoteServer remoteServer;

    public CommandHandler(Instrumentation instrumentation, RemoteServer server) {
        this.instrumentation = instrumentation;
        remoteServer = server;

        PSLogger.error("Start to set up command handler");

        initCommandHandler();
    }

    /// 正常响应/错误响应的计数
    private static LongAdder successServiceCnt = new LongAdder();
    private static LongAdder errorServiceCnt = new LongAdder();

    private static final AtomicInteger threadCnt = new AtomicInteger(0);

    //------------------------------------------------------------------------
    /// 如果服务端关闭了，记得关闭CommandHandler，这一点很重要，因为CommandHandler中也需要
    /// 关闭一些资源，比如线程池；
    /// 该属性用来标记handler的在线状态
    //------------------------------------------------------------------------
    private static volatile boolean serverCloseStatus = false;

    //------------------------------------------------------------------
    /// 当服务端检测到没有任何客户端连接之后，是否需要关闭服务端，一般情况下不建议
    /// 关闭，因为关闭之后需要重新加载Agent，这个代价比一直让这个小型Server运行着
    /// 的代价要大很多，当然，如果是一次性或者断断续续使用该工具的场景来说，打开这个
    /// 这个开关就没必要担心服务端没有被关闭的风险了
    //------------------------------------------------------------------
    private static volatile boolean closeServerOnNoClientConnect = false;

    //------------------------------------------------------------------------
    /// 每个客户端建连之后都需要申请一个context id，并且在每个客户端的生命周期中都得使用
    /// 这个context id
    //------------------------------------------------------------------------
    private static final AtomicInteger CONTEXT_SEQ = new AtomicInteger(10000);

    //------------------------------------------------------------------------
    /// 用于存储master client的context id
    //------------------------------------------------------------------------
    private static AtomicReference<Integer> masterClient;

    //------------------------------------------------------------------------
    /// 用于记录当前某个类的字节码是否已经被锁定，如果被锁定，那么其他客户端无法增强该字节码
    /// 除非该字节码锁被持有锁的客户端持有
    //------------------------------------------------------------------------
    private static ConcurrentMap<String, Integer> BYTE_LOCK_MAP;

    //------------------------------------------------------------------------
    /// 如果一个类字节码被修改过，那么将原始字节码存储在这里，为了快速恢复，这很重要，有时候
    /// 会想要快速验证一次修复是否能达到预期，但是当发现修复之后问题更加验证，而这个时候又
    /// 没有能快速修复问题，或者修复问题的时间不可预期，那么只能快速回滚字节码
    //------------------------------------------------------------------------
    private static ConcurrentMap<String, byte[]> BACK_UP_CLASS_BYTE_MAP;

    //-----------------------------------------------------------------------
    /// 用于执行命令的线程池，最多仅允许两条线程来做命令执行，并且任务队列是一个有界队列，这样
    /// 可以保护被疯狂连接，在保护服务端这一点上，现有的客户端实现将不太允许客户端连续发送命令
    /// 到服务端，只有当发送到服务端的命令执行完成了，响应成功之后才允许再次发送接下来的命令
    //-----------------------------------------------------------------------
    private static ExecutorService NETTY_SERVER_BUSINESS_EXEC_POOL;

    //------------------------------------------------------------------------
    /// 服务端用来执行一些周期性任务的线程池，所有需要周期性执行的任务都提交到这里来
    //------------------------------------------------------------------------
    private static ScheduledExecutorService NETTY_SERVER_SCHEDULER;

    //------------------------------------------------------------------------
    /// 命令字典
    //------------------------------------------------------------------------
    private static Map<String, Class<?>> COMMAND_MAP;

    //------------------------------------------------------------------------
    /// 命令列表
    //------------------------------------------------------------------------
    private static String COMMAND_LIST = "";

    //------------------------------------------------------------------------
    /// 当前有多少客户端在连接着，当然还要做心跳检测，传输一种特殊的协议，客户端在拿到之后需要
    /// 响应，否则服务端会认为你已经死了，直接关闭连接
    /// 待定：：：：：：：：：：：：：：：：：：：：：：：：：：：：：：：：：：：：：：：
    //------------------------------------------------------------------------
    private static ConcurrentMap<Integer, ChannelHandlerContext> CONNECTED_CLIENT_MAP;

    //------------------------------------------------------------------------
    /// 将当前正在处理的协议记录起来，用于超时检测等功能的实现
    //------------------------------------------------------------------------
    private static Map<Integer, RemoteCommand> ON_ALIVE_CONTEXT_Map;

    //------------------------------------------------------------------------
    /// 当前正在进行处理的命令列表，会进行定时查看是否超时等事件，并进行及时关闭命令操作
    //------------------------------------------------------------------------
    private static Map<Integer, Command> ON_PROGRESS_COMMAND_PROTOCOL_Map;

    //------------------------------------------------------------------------
    /// 简单记录一下当前活动的连接数量，用于控制是否需要关闭服务端
    //------------------------------------------------------------------------
    private static AtomicInteger CURRENT_ACTIVE_CONNECTION;

    //------------------------------------------------------------------------
    /// 任何命令执行时间不能超过这个时间，超过这个时间则会直接打断命令的执行，并
    /// 向客户端响应执行超时(被动，给command机会)
    //------------------------------------------------------------------------
    private static final Long SAFE_COMMAND_EXECUTE_TIME_MILLS_1 = 5 * 1000L;

    //------------------------------------------------------------------------
    /// 任何命令执行时间不能超过这个时间，超过这个时间则会直接打断命令的执行，并
    /// 向客户端响应执行超时（主动，直接打断）
    //------------------------------------------------------------------------
    private static final Long SAFE_COMMAND_EXECUTE_TIME_MILLS_2 = 10 * 1000L;

    //------------------------------------------------------------------------
    /// 为了控制客户端执行命令，每个客户端对一个类的一个方法仅能增强一次，之后的访问将不再做增强
    /// 操作，直接获取增强结果即可（重新将Advice注册到Weave中即可获取到一次增强的结果，神奇吧）
    //------------------------------------------------------------------------
    private static final Map<String, MethodAdvice> METHOD_ADVICE_MAP = new ConcurrentHashMap<>();

    //------------------------------------------------------------------------
    /// 一个方法仅需要被一个客户端增强一次，之后的客户端就可以共享这个增强之后的字节码，然后不同的
    /// 客户端根据command选择不同的Advice即可实现差异化的观察
    //------------------------------------------------------------------------
    private static final Map<String, Integer> METHOD_TRACE_ENHANCE_STATUS = new ConcurrentHashMap<>();

    //------------------------------------------------------------------------
    /// 可能某个类被增强/变更过，如果不记录起来，可能会导致对一个类多次在增强都是基于原始
    /// 字节码，因此将增强过的字节码记录起来，达到增强累计的效果；
    /// 当然，如果需要，还可以基于这个记录提供类回滚的功能
    //------------------------------------------------------------------------
    private static final Map<String, List<byte[]>> CLASS_BYTE_CODE_RECORD_MAP = new LinkedHashMap<>();

    //------------------------------------------------------------------------
    /// 缓存类的内部类信息可以就是类的全限定名称，value就是一串内部类字符串
    //------------------------------------------------------------------------
    private static final ConcurrentMap<String, String> CLASS_INNER_CLASS_MAP = new ConcurrentHashMap<>();

    //------------------------------------------------------------------------
    /// cache the lr model
    //------------------------------------------------------------------------
    private static final ConcurrentMap<String, LRModel> LR_MODEL_CACHE_MAP = new ConcurrentHashMap<>();

    //------------------------------------------------------------------------
    /// 记录方法的流量，用于后续进行回放
    //------------------------------------------------------------------------
    private static final Map<String, List<List<MethodTraceFrame>>> METHOD_TRACE_FLOW_RECORD_MAP = new ConcurrentHashMap<>();

    //------------------------------------------------------------------------
    /// 用于任务超时停止命令
    //------------------------------------------------------------------------
    private static final ConcurrentMap<Integer, StopAbleRunnable> RUNNABLE_COMMAND_TASK_QUEUE = new ConcurrentHashMap<>();

    /**
     *  是否在服务端连接数为0的时候主动关闭服务端呢，不建议主动关闭，主动关闭意味着需要
     *  重新开启
     *
     * @param set true 代表允许，false代表不允许
     */
    public static void setCloseServerOnNoClientConnect(boolean set) {
        closeServerOnNoClientConnect = set;
        if (closeServerOnNoClientConnect && CURRENT_ACTIVE_CONNECTION.get() == 0) {
            PSLogger.error("当前客户端连接数量为0，关闭服务器");
            try {
                remoteServer.stop(null);
            } catch (Exception e) {
                PSLogger.error("error while stop server:" + e);
            }
        }
    }

    /**
     *  初始化命令处理器，主要是初始化处理线程池
     *
     */
    private void initCommandHandler() {

        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                PSLogger.error("UncaughtExceptionHandler : " + UTILS.getErrorMsg(e) + " Thread : " + t.getName());
            }
        };

        // 不排队，否则在core线程用完queue没满之前客户端输入命令会出现假死现象，这样体验不太好
        // 直接拒绝提交的任务，让客户端认识到当前还有其他命令在处理，所以无法再进行做其他命令的处理了
        NETTY_SERVER_BUSINESS_EXEC_POOL = new ThreadPoolExecutor(1, 2, 60,
                TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t =  new Thread(r);
                t.setName(Constant.JAVA_DEBUG_WORKER_NAME_PREFIX + threadCnt.incrementAndGet());
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(uncaughtExceptionHandler);
                return t;
            }
        });

        NETTY_SERVER_SCHEDULER = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "java-debug-scheduler-execute-thread");
            }
        });

        successServiceCnt = new LongAdder();
        errorServiceCnt = new LongAdder();

        masterClient = new AtomicReference<>(-1);

        BYTE_LOCK_MAP = new ConcurrentHashMap<>();

        BACK_UP_CLASS_BYTE_MAP = new ConcurrentHashMap<>();

        // 初始化命令字典
        COMMAND_MAP = new HashMap<>();

        COMMAND_MAP.put("findClass", FindJavaSourceCommand.class);
        COMMAND_MAP.put("fc", FindJavaSourceCommand.class);
        COMMAND_LIST += "fc\n";

        COMMAND_MAP.put("help", HelpCommand.class);
        COMMAND_MAP.put("h", HelpCommand.class);
        COMMAND_LIST += "help\n";

        COMMAND_MAP.put("list", ListCommand.class);
        COMMAND_MAP.put("all", ListCommand.class);
        COMMAND_LIST += "list\n";

        COMMAND_MAP.put("exit", ExitCommand.class);
        COMMAND_LIST += "exit\n";

        COMMAND_MAP.put("alive", AliveCommand.class);
        COMMAND_LIST += "alive\n";

        COMMAND_MAP.put("redefine", RedefineClassCommand.class);
        COMMAND_MAP.put("rdf", RedefineClassCommand.class);
        COMMAND_LIST += "redefine\n";

        COMMAND_MAP.put("rollback", RollbackClassCommand.class);
        COMMAND_MAP.put("back", RollbackClassCommand.class);
        COMMAND_LIST += "rollback\n";

        COMMAND_MAP.put("lockClass", LockClassCommand.class);
        COMMAND_MAP.put("lock", LockClassCommand.class);
        COMMAND_LIST += "lock\n";

        COMMAND_MAP.put("option", OptionCommand.class);
        COMMAND_MAP.put("set", OptionCommand.class);
        COMMAND_LIST += "set\n";

        COMMAND_MAP.put("info", AgentInfoCommand.class);
        COMMAND_MAP.put("if", AgentInfoCommand.class);
        COMMAND_LIST += "info\n";

        COMMAND_MAP.put("trace", MethodTraceCommand.class);
        COMMAND_MAP.put("mt", MethodTraceCommand.class);
        COMMAND_LIST += "trace\n";
        COMMAND_LIST += "mt\n";

        COMMAND_MAP.put("cputime", CpuTimeUsageCommand.class);
        COMMAND_MAP.put("ct", CpuTimeUsageCommand.class);
        COMMAND_LIST += "ct\n";

        COMMAND_MAP.put("thread", ThreadCommand.class);
        COMMAND_MAP.put("th", ThreadCommand.class);
        COMMAND_LIST += "thread\n";

        COMMAND_MAP.put("monitor", MonitorCollectorCommand.class);
        COMMAND_MAP.put("collect", MonitorCollectorCommand.class);
        COMMAND_LIST += "monitor\n";

        COMMAND_MAP.put("btrace", BTraceCommand.class);
        COMMAND_MAP.put("bt", BTraceCommand.class);
        COMMAND_LIST += "btrace\n";

        /// 初始化周期性任务
        NETTY_SERVER_SCHEDULER.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    doScheduleWork();
                } catch (Exception e) {
                    PSLogger.error(UTILS.format("周期性任务执行失败:[%s]", UTILS.getErrorMsg(e)));
                }
            }
        }, 0, 10, TimeUnit.SECONDS);

        CONNECTED_CLIENT_MAP = new ConcurrentHashMap<>();

        ON_ALIVE_CONTEXT_Map = new ConcurrentHashMap<>();

        ON_PROGRESS_COMMAND_PROTOCOL_Map = new ConcurrentHashMap<>();

        CURRENT_ACTIVE_CONNECTION = new AtomicInteger();

        serverCloseStatus = false;

        PSLogger.error("command handler set up");
    }

    /**
     *  客户端可以自己指定一个超时时间，如果发现指定了，那么就会以这个时间为超时时间，否则还是
     *  以系统自定义的超时时间来检测
     *
     * @param remoteCommand {@link RemoteCommand}
     * @return 超时时间 （秒）
     */
    public static long checkTimeoutMe(RemoteCommand remoteCommand) {
        String timeoutCheckTag = remoteCommand.getParam("$forward-timeout-check-tag");
        if (UTILS.isNullOrEmpty(timeoutCheckTag)) {
            return -1; // 可以check
        }
        return UTILS.safeParseLong(timeoutCheckTag, -1) * 1000;
    }

    /**
     *  这个方法事情比较杂，代理服务内部的所有周期性的事情都需要管，比如master client管理（避免无法正常关闭服务端）
     *  以及连接管理、命令执行管理等;
     *
     *
     */
    private void doScheduleWork() throws Exception {

        //// 超时检测（被动）
        for (Map.Entry<Integer, RemoteCommand> entry : ON_ALIVE_CONTEXT_Map.entrySet()) {
            RemoteCommand remoteCommand = entry.getValue();
            long standLevel1TimeoutVal = SAFE_COMMAND_EXECUTE_TIME_MILLS_1;
            long checkTimeoutVal = checkTimeoutMe(remoteCommand);
            if (checkTimeoutVal > 0) {
                standLevel1TimeoutVal = checkTimeoutVal;
            }

            long cost = System.currentTimeMillis() - remoteCommand.getExecuteStartTime();
            if (cost >= standLevel1TimeoutVal) {
                PSLogger.error("命令超时，已经执行了:" + cost + ":" + remoteCommand);
                Command command = ON_PROGRESS_COMMAND_PROTOCOL_Map.get(remoteCommand.getContextId());
                if (command != null) {
                    remoteCommand.setStopTag("true");
                    command.stop(instrumentation, remoteCommand, this, this);
                }
            }
        }

        /// 超时检测（主动关闭）
        Set<Integer> removeKeys = new HashSet<>();
        for (Map.Entry<Integer, StopAbleRunnable> entry : RUNNABLE_COMMAND_TASK_QUEUE.entrySet()) {
            StopAbleRunnable stopAbleRunnable = entry.getValue();
            long standLevel2TimeoutVal = SAFE_COMMAND_EXECUTE_TIME_MILLS_2;
            long checkTimeoutVal = checkTimeoutMe(stopAbleRunnable.getCommand());
            if (checkTimeoutVal > 0) {
                standLevel2TimeoutVal = checkTimeoutVal; // 时间改变一下
            }
            long cost = System.currentTimeMillis() - stopAbleRunnable.getStartMills();
            if (cost >= standLevel2TimeoutVal) {
                PSLogger.error("命令执行超时，停止任务执行:" + stopAbleRunnable);
                removeKeys.add(entry.getKey());
                try {
                    stopAbleRunnable.stop();
                } catch (Throwable throwable) {
                    PSLogger.error("终止命令任务执行失败：" + throwable);
                }
                // remove the advice
                ClassMethodWeaver.unRegAdvice(entry.getKey());
                PSLogger.error("unReg the advice for console:" + entry.getKey());
            }
        }

        if (!removeKeys.isEmpty()) {
            for (Integer contextId : removeKeys) {
                RUNNABLE_COMMAND_TASK_QUEUE.remove(contextId);
            }
        }

    }

    /**
     *  权限控制是非常有必要的，因为工具允许客户端进行远程代码执行（待定），或者进行一些敏感的
     *  命令操作，所以每个命令都会存在权限等级，如果没有获取到服务端授权，命令校验不会通过，则
     *  命令终止执行，并返回"没有权限执行命令"的错误信息；
     *
     * @param remoteCommand {@link RemoteCommand} 协议
     */
    private void checkClientAuth(RemoteCommand remoteCommand) throws Exception{
        if (remoteCommand.getContextId() < 0) {
            throw new ClientAuthFailException("暂未获得命令执行授权");
        }
    }

    /**
     *  通用的协议内容校验，比如是否携带了context id，是否是合法的请求等
     *
     * @param remoteCommand {@link RemoteCommand}
     * @return true则代表可以接着处理，否则不再后续处理
     */
    private static boolean commonProtocolCheck(RemoteCommand remoteCommand, ChannelHandlerContext context) {
        if (remoteCommand.getVersion() != CURRENT_SERVER_VERSION) {
            createErrorResponse(context, remoteCommand, "不支持的版本，请保持最新的服务端版本");
            return false;
        }
        if (remoteCommand.getCallSeq() <= 0) {
            createErrorResponse(context, remoteCommand, "不合法的round值，请重新连接");
            context.channel().close();
            return false;
        }
        if (remoteCommand.getContextId() <= 0) {
            createErrorResponse(context, remoteCommand, "不合法的context id，请先申请context id");
            context.channel().close();
            return false;
        }
        if (UTILS.isNullOrEmpty(remoteCommand.getCommandName())) {
            createErrorResponse(context, remoteCommand, "请指定命令");
            return false;
        }
        if (remoteCommand.getTimestamp() <= 0) {
            remoteCommand.setTimeStamp(System.currentTimeMillis());
        }

        return true;
    }


    /**
     * Calls {@link ChannelHandlerContext#fireChannelActive()} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
        CURRENT_ACTIVE_CONNECTION.incrementAndGet();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelInactive()} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
        int now = CURRENT_ACTIVE_CONNECTION.decrementAndGet();
        if (now == 0 && closeServerOnNoClientConnect) {
            PSLogger.error("当前客户端连接数量为0，关闭服务器");
            remoteServer.stop(null);
        }
        int cid = -1;
        for (Map.Entry<Integer, ChannelHandlerContext> entry : CONNECTED_CLIENT_MAP.entrySet()) {
            // 只要是同一条连接就可以
            if (ctx.channel().equals(entry.getValue().channel())) {
                cid = entry.getKey();
                break;
            }
        }
        if (cid > 0) {
            PSLogger.error("start to detect bytecode lock cause the channel is inactive now, find cid:" + cid);

            // byte code lock
            checkBytecodeLockWhenConnectExit(cid);

            // method trace advice
            removeAdvice(cid);

            // 是否还有在执行的命令
            StopAbleRunnable runnable = RUNNABLE_COMMAND_TASK_QUEUE.remove(cid);
            if (runnable != null) {
                PSLogger.error("the console run command " + runnable.getCommand() + " stop it!");
                try {
                    runnable.stop();
                } catch (Throwable throwable) {
                    PSLogger.error("could not stop the runnable:" + runnable);
                }
            } else {
                PSLogger.error("running command:\n" + RUNNABLE_COMMAND_TASK_QUEUE + "\n");
            }

        }
    }

    /**
     *  检测一下客户端退出的时候是否还有锁定的字节码，如果有的话，自动释放
     *
     * @param contextId 客户端id
     */
    private void checkBytecodeLockWhenConnectExit(int contextId) {
        if (BYTE_LOCK_MAP == null) {
            return;
        }
        PSLogger.info("start to scan byte-lock-map to find whether the console hold any bytecode lock: " + contextId);

        for (Map.Entry<String, Integer> entry : BYTE_LOCK_MAP.entrySet()) {
            if (contextId == entry.getValue()) {
                unlockClassByte(entry.getKey(), contextId);
                PSLogger.error("unlock bytecode on exit for console:" + contextId + " key:" + entry.getKey());
            }
        }
    }

    /**
     * Calls {@link ChannelHandlerContext#fireExceptionCaught(Throwable)} to forward
     * to the next {@link ChannelHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        createErrorResponse(ctx, null,  getStackTraceFromException(cause));
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelRead(Object)} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, RemoteCommand msg) {
//        if (!(msg instanceof RemoteCommand)) {
//            createErrorResponse(ctx, null, "解码错误");
//            return;
//        }

        // 新建一个task
        RemoteCommand remoteCommand = (RemoteCommand) msg;
        StopAbleRunnable stopAbleRunnable = new StopAbleRunnable(remoteCommand) {
            @Override
            public void execute() {
                RemoteCommand remoteCommand = (RemoteCommand) msg;
                /// 前置处理
                remoteCommand = preHandleCommand(remoteCommand);

                try {
                    handleCommand(ctx, remoteCommand, this);
                } catch (Exception e) {
                    String trace = getStackTraceFromException(e);
                    PSLogger.error(trace);
                    // response
                    createErrorResponse(ctx, (RemoteCommand) msg, trace);
                } finally {
                    // 删除任务
                    StopAbleRunnable runnable = RUNNABLE_COMMAND_TASK_QUEUE.remove(remoteCommand.getContextId());
                    PSLogger.info("the command execute to end:" + runnable);
                }
            }
        };

        // 执行任务
        NETTY_SERVER_BUSINESS_EXEC_POOL.execute(stopAbleRunnable);
    }

    /**
     *  命令执行错误，这里统一回复一下
     *
     * @param ctx {@link ChannelHandlerContext}
     * @param errorMsg 错误消息
     */
    private static void createErrorResponse(ChannelHandlerContext ctx, RemoteCommand originRemoteCommand, String errorMsg) {
        RemoteCommand remoteCommand = new RemoteCommand();

        if (originRemoteCommand != null) {
            remoteCommand  = originRemoteCommand.simpleCopy(remoteCommand);
        } else {
            remoteCommand.setTag(CONNECTION_TAG_ONE);
            remoteCommand.setVersion(CURRENT_SERVER_VERSION);

            /// context, this is very important
            remoteCommand.setContextId(CONTEXT_SEQ.getAndIncrement());

//            /// seq
//            /// 如果走到这里，说明这个客户端还没有获取到contextId，所以round值设置为1是合理的
//            remoteCommand.setCallSeq(1);

            /// command name
            /// 还没有执行任何命令，所以直接设置空值即可
            remoteCommand.setCommandName("");

//            // increment round
//            // 每次请求服务端都需要执行加1操作，客户端不要随意变动这个字段
//            int callSeq = remoteCommand.getCallSeq();
//            remoteCommand.setCallSeq(callSeq + 1);
        }

        /// protocol type
        /// 这是响应类型的协议内容
        remoteCommand.setProtocolType(COMMAND_RES_PROTOCOL_TYPE);

        /// params
        /// 设置服务响应结果
        remoteCommand.addParam("$back-errorCode", "-1");
        remoteCommand.addParam("$back-errorMsg", errorMsg);

//        /// round + 1
//        int seqCnt = remoteCommand.getCallSeq();
//        remoteCommand.setCallSeq(seqCnt + 1);

        // 写到远程
        writeAndFlush(ctx, remoteCommand);
    }

    /**
     *  创建一个成功的响应
     *
     * @param ctx {@link ChannelHandlerContext}
     * @param originRemoteCommand {@link RemoteCommand}
     * @param resp 命令执行结果，会安排在key为"$back-data"的字段里面
     */
    private void createSuccessResponse(ChannelHandlerContext ctx, RemoteCommand originRemoteCommand, String resp) {
        RemoteCommand remoteCommand = new RemoteCommand();
        originRemoteCommand.simpleCopy(remoteCommand);
        // 校验一下Context id
        int cid = remoteCommand.getContextId();
        if (cid <= 0) {
            createErrorResponse(ctx, remoteCommand, "不合法的Context ID，你是怎么混进来的");
            return;
        }

//        // increment round
//        // 每次请求服务端都需要执行加1操作，客户端不要随意变动这个字段
//        int callSeq = originRemoteCommand.getCallSeq();
//        originRemoteCommand.setCallSeq(callSeq + 1);

        // protocolType
        // 协议内容类型，这个一定要变一下，否则会出现可怕的后果
        remoteCommand.setProtocolType(COMMAND_RES_PROTOCOL_TYPE);

        /// cost
        /// 计算一下执行耗时，这里的耗时是从客户端发送请求到服务端即将回写结果这一段时间，如果需要
        /// 计算其他类型的时间，在协议中增加相关内容再进行
        /// fix：  这个时间还是客户端自己计算吧，客户端在发送请求的时候将时间戳加到协议中，这样拿到结果
        /// 之后就可以计算整个命令执行的耗时了，这样也比较符合业务场景

        /// data 填充
        if (!remoteCommand.hasSetErrorStatus()) {
            remoteCommand.addParam("$back-errorCode", "0");
        }
        if (!remoteCommand.hasResult()) {
            remoteCommand.addParam("$back-data", resp);
        }

        int retry = 0;

        if (!ctx.channel().isWritable()) {
            while (!ctx.channel().isWritable() && retry < 3) {
                PSLogger.error("当前时间Channel不可写，稍等...");
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    retry ++;
                }
            }
        }

        /// 写到远程
        writeAndFlush(ctx, remoteCommand);
    }

    /**
     *  用来统一发送消息
     *
     * @param ctx {@link ChannelHandlerContext}
     * @param msg {@link RemoteCommand}
     */
    private static void writeAndFlush(ChannelHandlerContext ctx, Object msg) {
        if (!ctx.channel().isActive() || !ctx.channel().isWritable()) {
            PSLogger.error("the target channel is inactive or not writable");
            ctx.channel().close();
            return;
        }
        ctx.writeAndFlush(msg).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                String remoteInfo = parseRemoteAddress(ctx.channel());
                if (future.isSuccess()) {
                    //PSLogger.info(UTILS.format("成功发送消息:[%s][%s]", remoteInfo, msg));
                } else {
                    try {
                        // get the exception
                        future.get();
                    } catch (Exception e) {
                        PSLogger.error(UTILS.format("无法发送消息:[%s],[%s],[%s]", remoteInfo, msg, e));
                        // 删除连接
                        RemoteCommand remoteCommand = (RemoteCommand) msg;
                        if (remoteCommand != null) {
                            ChannelHandlerContext ctx = CONNECTED_CLIENT_MAP.get(remoteCommand.getContextId());
                            try {
                                ctx.channel().close();
                                ctx.close();
                            } catch (Exception ee) {
                                PSLogger.error("无法关闭连接：" + ee);
                            } finally {
                                CONNECTED_CLIENT_MAP.remove(remoteCommand.getContextId());
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     *  进行一些前置处理
     *
     * @param remoteCommand {@link RemoteCommand}
     */
    private static RemoteCommand preHandleCommand(RemoteCommand remoteCommand) {
        /// round + 1
        int roundCnt = remoteCommand.getCallSeq();
        remoteCommand.setCallSeq(roundCnt + 1);

        remoteCommand.removeParam("$back-errorCode");
        remoteCommand.removeParam("$back-errorMsg");
        remoteCommand.removeParam("$back-data");

        return remoteCommand;
    }

    /**
     *  如果是第三方命令，那么返回一个第三方能力
     *
     * @param remoteCommand {@link RemoteCommand}
     * @return {@link ThirdPartyAbility}
     */
    private ThirdPartyAbility isThirdPartyCommand(RemoteCommand remoteCommand)
            throws ThirtPartyAbilityNotFindException {
        try {
            return ThirdPartyRouter.findThirdParty(remoteCommand);
        } catch (Exception e) {
            return null; // ignore it!
        }
    }

    /**
     *  完成命令交互，需要处理初始化的过程，比如contextId分配，客户端首次连接通过发送tag=0来申请新的context；
     *  一个连接的生命周期内不要重复申请，下次连接请携带获取到的ContextId，否则服务端会认为你还没有获取到ContextId
     *  从而一直拒绝执行命令；
     *
     * @param runnable 因为首次连接的客户端的clientId为0，所以需要特殊处理一下
     * @param ctx {@link ChannelHandlerContext}
     * @param remoteCommand {@link RemoteCommand}
     */
    private void handleCommand(ChannelHandlerContext ctx, RemoteCommand remoteCommand,
                               StopAbleRunnable runnable) {

        /// 是否申请ContextId
        byte contextTag = remoteCommand.getTag();
        if (CONNECTION_TAG_ZERO == contextTag) {
            if (handleAskContextReq(ctx, remoteCommand)) {
                return;
            }
        }

        // set the context id for task
        runnable.setContextId(remoteCommand.getContextId());

        // 加到任务队列中去(对于一个client来说任意时刻只能有一个命令在执行)
        RUNNABLE_COMMAND_TASK_QUEUE.put(remoteCommand.getContextId(), runnable);

        Throwable throwable = null;
        // 命令处理一下
        try {

            /// 命令权限校验
            checkClientAuth(remoteCommand);

            /// 协议校验
            if (!commonProtocolCheck(remoteCommand, ctx)) {
                return;
            }

            // 命令输出响应
            String resp;

            // 是否是第三方命令
            ThirdPartyAbility thirdPartyAbility = isThirdPartyCommand(remoteCommand);
            if (thirdPartyAbility != null) {
                // 执行命令
                resp = ThirdPartyRouter.routeAndExecute(thirdPartyAbility, remoteCommand);
            } else {
                /// 找到命令
                Command command = dispatchToCommand(remoteCommand);
                /// 是否不支持的命令
                if (command == null) {
                    throw new CommandNotFindException(UTILS.format("unSupport command:[%s]", remoteCommand.getCommandName()));
                }

                /// 命令前置处理
                preExecutingCommand(remoteCommand, ctx, command);

                /// 命令前置校验
                if (!command.preExecute(remoteCommand)) {
                    // 命令使用姿势发一下
                    String usage = "Usage:\n" + CommandDescribeUtil.collectFromCommand(command);
                    createErrorResponse(ctx, remoteCommand, usage);
                    return;
                }

                /// 执行命令
                resp = command.execute(instrumentation, remoteCommand, this, this);
            }

            // write response
            if (resp == null) {
                createErrorResponse(ctx, remoteCommand, "empty result");
            }

            /// 响应
            createSuccessResponse(ctx, remoteCommand, resp);
        } catch (Throwable e) {
            throwable = e;
            createErrorResponse(ctx, remoteCommand, getStackTraceFromException(e));
        } finally {
            if (throwable != null) {
                errorServiceCnt.increment();
            } else {
                successServiceCnt.increment();
            }
            /// 后处理
            afterExecutingCommand(remoteCommand, ctx);
        }
    }

    /**
     *  命令前置处理
     *
     * @param remoteCommand 命令协议
     * @param command 执行的命令
     */
    private void preExecutingCommand(RemoteCommand remoteCommand, ChannelHandlerContext ctx, Command command) {
        int cid = remoteCommand.getContextId();
        ON_ALIVE_CONTEXT_Map.put(cid, remoteCommand);
        ON_PROGRESS_COMMAND_PROTOCOL_Map.put(cid, command);
        CONNECTED_CLIENT_MAP.putIfAbsent(cid, ctx);
        remoteCommand.setExecuteStartTime(System.currentTimeMillis());
    }

    /**
     *  命令后处理
     *
     * @param remoteCommand 命令协议
     * @param ctx {@link ChannelHandlerContext}
     */
    private void afterExecutingCommand(RemoteCommand remoteCommand, ChannelHandlerContext ctx) {
        /// 转态清理
        int cid = remoteCommand.getContextId();
        ON_ALIVE_CONTEXT_Map.remove(cid);
        ON_PROGRESS_COMMAND_PROTOCOL_Map.remove(cid);

        /// 是否需要关闭连接
        String needToCloseClient = remoteCommand.getParam("$back-close-tag");
        if ("true".equals(needToCloseClient)) {
            ctx.channel().close().addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if (future.isSuccess()) {
                        PSLogger.error("success close channel:" + ctx.channel().remoteAddress());
                    } else {
                        try {
                            future.get();
                        } catch (Exception e) {
                            PSLogger.error("could not close channel :" + ctx.channel().remoteAddress() + ":" + e);
                        }
                    }

                    /// 释放字节码
                    checkBytecodeLockWhenConnectExit(cid);

                    // method advice
                    removeAdvice(cid);
                }
            });
        }

    }

    /**
     *  获取到可识别的异常信息
     *
     * @param e 原始异常对象
     * @return 可打印的异常信息
     */
    private static String getStackTraceFromException(Throwable e) {
        if (e == null) {
            return "empty error msg";
        }
        StackTraceElement[] stackTraceElements =e.getStackTrace();
        if (stackTraceElements != null && stackTraceElements.length > 0) {
            StringBuilder sb = new StringBuilder();

            sb.append("-").append(e.toString()).append("\n");

            int spaceCnt = 1;
            for (StackTraceElement element : stackTraceElements) {
                for (int i = 0; i < spaceCnt; i ++) {
                    sb.append(" ");
                }
                spaceCnt ++;
                sb.append("+");
                sb.append(element.toString()).append("\n");
            }
            return sb.toString();
        }
        if (!UTILS.isNullOrEmpty(e.getMessage())) {
            return e.getMessage();
        }
        if (e.getCause() != null) {
            return getStackTraceFromException(e.getCause());
        }
        return "could not get error msg";
    }

    /**
     *  定位到具体的命令
     *
     * @param remoteCommand {@link RemoteCommand}
     * @return {@link Command}
     */
    private Command dispatchToCommand(RemoteCommand remoteCommand) throws Exception {
        String commandName = remoteCommand.getCommandName();
        if (UTILS.isNullOrEmpty(commandName)) {
            throw new CommandNotFindException("invalid protocol : empty command");
        }
        Class<?> cmdCls = COMMAND_MAP.get(commandName);
        if (cmdCls == null) {
            //throw new CommandNotFindException(UTILS.format("无法找到命令:%s", commandName));
            return null;
        }
        return (Command) cmdCls.getDeclaredConstructor().newInstance();
    }

    /**
     *  处理ContextId申请的请求
     *
     * @param ctx  {@link ChannelHandlerContext}
     * @param remoteCommand {@link RemoteCommand}
     */
    private boolean handleAskContextReq(ChannelHandlerContext ctx, RemoteCommand remoteCommand) {
        RemoteCommand resp = new RemoteCommand();
        resp = remoteCommand.simpleCopy(resp);

        /// 获取一个ContextId
        int newContextId = CONTEXT_SEQ.getAndIncrement();
        resp.setContextId(newContextId);

        // 你是master client不
        // 这个master client的标记是用来做一些很弱的，但是能力极大的权限控制的，比如关闭服务端，对于server来说
        // 只有master client才能关闭服务端，normal client如果试图关闭服务端，那么会对其进行警告，但不会做任何
        // 处理，因为normal client确实无法去主动关闭服务端
        // 有一种情况是normal client将自己的这个tag变更成了master console，这个时候服务端就会比较严格，如果发现
        // 这是一个假的 "master console"， 那么后果就是直接将其连接关闭，所以不要试图改变自己的client tag
        if (masterClient.compareAndSet(-1, newContextId)) {
            resp.setClientTag(CONNECTION_TYPE_MASTER);
        } else {
            resp.setClientTag(CONNECTION_TYPE_NORMAL);
        }

        /// 假设分配了之后就成了，这里设置一下标记
        resp.setTag(CONNECTION_TAG_ONE);

        /// 优化，判断是否携带命令，如果携带命令，则接着执行命令，否则直接返回
        String commandName = remoteCommand.getCommandName();
        if (UTILS.isNullOrEmpty(commandName)) {
            writeAndFlush(ctx, resp);
            return true;
        } else {
            remoteCommand.setContextId(resp.getContextId());
            remoteCommand.setTag(resp.getTag());
            remoteCommand.setClientTag(resp.getClientTag());

            PSLogger.error(UTILS.format("client [%s] get ContextId，find with command:[%s]，continue run ！",
                    ctx.channel().remoteAddress(), commandName));
            return false;
        }
    }

    private static String parseRemoteAddress(final Channel channel) {
        if (null == channel) {
            return "unknown";
        }
        final SocketAddress remote = channel.remoteAddress();
        return doParse(remote != null ? remote.toString().trim() : "unknown");
    }

    private static String doParse(String addr) {
        if (UTILS.isBlank(addr)) {
            return "unknown";
        }
        if (addr.charAt(0) == '/') {
            return addr.substring(1);
        } else {
            int len = addr.length();
            for (int i = 1; i < len; ++i) {
                if (addr.charAt(i) == '/') {
                    return addr.substring(i + 1);
                }
            }
            return addr;
        }
    }

    /**
     * 根据命令名字获取到命令类型
     *
     * @param cmd 命令名字
     * @return 获取到的命令类型，如果没有发现，那么就是null
     */
    @SuppressWarnings("unchecked")
    @Override
    public Class<Command> queryCommand(String cmd) {
        if (UTILS.isNullOrEmpty(cmd)) {
            return null;
        }
        return (Class<Command>) COMMAND_MAP.get(cmd);
    }

    /**
     * 新建一个命令
     *
     * @param cmd 命令名字
     * @return {@link Command}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Command newCommand(String cmd) {
        Class<Command> cls = (Class<Command>) queryCommand(cmd);
        if (cls == null) {
            return null;
        }
        try {
            return cls.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            PSLogger.error(String.format("could not create instance [%s]", cls.getClass().getName()), e);
            return null;
        }
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     * <p>
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (serverCloseStatus) {
            PSLogger.error("Command Handler Already shutdown");
        }
        synchronized (this) {
            if (!serverCloseStatus) {
                PSLogger.error("start to close handler.");
                try {
                    NETTY_SERVER_BUSINESS_EXEC_POOL.shutdownNow();
                    NETTY_SERVER_SCHEDULER.shutdownNow();
                    serverCloseStatus = true;
                    PSLogger.error("success to close handler.");
                } catch (Exception e) {
                    PSLogger.error("could not close handler:" + e);
                }
            }
        }
    }

    /**
     * 如果一个类的字节码被动态修改过，那么就会将原始字节码记录下来，主要
     * 是为了恢复
     *
     * @param className 类名称
     * @return 如果没有被增强过，那么就是null，这一点需要注意
     */
    @Override
    public byte[] getBackupClassByte(String className) {
        if (UTILS.isNullOrEmpty(className)) {
            PSLogger.error("class name is null, skip it!");
            return null;
        }
        byte[] bytes = BACK_UP_CLASS_BYTE_MAP.get(className);

        //PSLogger.info("获取到字节码:" + className + ":\n" + Arrays.toString(bytes));

        return bytes;
    }

    /**
     * 某些命令会修改字节码，这个时候需要原类的原始字节码存储起来，该方法仅支持
     * 将类的原始字节码存储起来的功能，如果想要实现递进式类字节码存储，需要重新
     * 实现:参考 {@code backupClassByteWithLastVersion}
     *
     * @param className 类名字，全限定
     * @param bytes     字节码
     */
    @Override
    public void backupClassByte(String className, byte[] bytes) {
        if (UTILS.isNullOrEmpty(className)) {
            PSLogger.error("class name is null, skip it!");
            return;
        }
        if (bytes == null || bytes.length == 0) {
            PSLogger.error("bytecode array is null or empty, skip it!");
            return;
        }
        if (getBackupClassByte(className) != null) {
            PSLogger.error("class " + className + " is stored!");
            return;
        }
        //PSLogger.info("存储字节码:" + className + ":\n" + Arrays.toString(bytes));
        BACK_UP_CLASS_BYTE_MAP.put(className, bytes);
    }

    /**
     * {@code backupClassByte}方法的另外一种版本，每次调用都会将该字节码存储起来作为
     * 回滚版本，所以，调用该方法之前，请确保你当前的字节码是稳定、正确的，因为该方法不会
     * 对类进行任何校验，如果字节码不合法，或者不符合规范，那么就会造成不可回滚类的严重后果
     *
     * @param className 类名称
     * @param bytes     类字节码数组
     */
    @Override
    public void backupClassByteWithLastVersion(String className, byte[] bytes) {
        if (UTILS.isNullOrEmpty(className)) {
            PSLogger.error("class name is null, skip it!");
            return;
        }
        if (bytes == null || bytes.length == 0) {
            PSLogger.error("bytecode array is null or empty, skip it!");
            return;
        }
        BACK_UP_CLASS_BYTE_MAP.put(className, bytes);
    }

    /**
     * 获取当前服务端记录的字节码锁定信息
     *
     * @return {@link CommandHandler#BYTE_LOCK_MAP}
     */
    @Override
    public ConcurrentMap<String, Integer> classLockInfoMap() {
        return BYTE_LOCK_MAP;
    }

    /**
     * 获取持有类字节码锁的客户端信息
     *
     * @param className 类
     * @return contextId
     */
    @Override
    public int bytecodeOwner(String className) {
        return Optional.ofNullable(BYTE_LOCK_MAP.get(className)).orElse(-1);
    }

    /**
     * 锁定字节码，不允许被别人变更
     *
     * @param className 类名
     * @param contextId 客户端id
     * @return 是否成功
     */
    @Override
    public boolean lockClassByte(String className, int contextId) {
        if (UTILS.isNullOrEmpty(className)) {
            return false;
        }
        if (contextId <= 0) {
            return false;
        }
        int holder = bytecodeOwner(className);
        // 自己持有
        if (holder > 0 && holder == contextId) {
            return true;
        }
        // 别人持有
        if (holder > 0) {
            return false;
        }

        Integer oldVal = BYTE_LOCK_MAP.putIfAbsent(className, contextId);

        if (oldVal != null) {
            PSLogger.error("类:" + className + " 已经被:" + oldVal + " 锁定， " + contextId + " 无法获得锁");
            return false;
        }

        return true;
    }

    /**
     * 解锁一个类，之后就允许其他客户端修改字节码了
     *
     * @param className 类
     * @param contextId id
     * @return 是否成功，较为严格的校验，如果该字节码并不是该客户端持有，也会认为解锁失败
     */
    @Override
    public boolean unlockClassByte(String className, int contextId) {
        if (UTILS.isNullOrEmpty(className)) {
            return false;
        }
        if (contextId <= 0) {
            return false;
        }
        int lockHolder = bytecodeOwner(className);
        if (lockHolder == -1) {
            PSLogger.error("暂未锁定类:" + className);
            return true; // 认为解锁成功吧
        }
        if (lockHolder != contextId) {
            PSLogger.error("没有权限解锁:" + className + " by:" + contextId + " holder:" + lockHolder);
            return false;
        }

        BYTE_LOCK_MAP.remove(className);

        return true;
    }

    /**
     * 这个方法用来获取到当前客户端的 {@link MethodAdvice}，特别的是，如果发现该
     * 客户端还没有创建过给定key的advice的话，那么会创建一个；最后，无论是以前创建
     * 的还是本次创建的，都将这个advice返回
     *
     * @param contextId 客户端标志
     * @param cls       类
     * @param method    方法
     * @param desc      方法描述
     * @param remoteCommand 一个参数一个参数的取，累死了，直接丢进去，想要什么就取什么吧，别删数据就行
     * @return {@link MethodAdvice}
     */
    @Deprecated
    @Override
    public MethodAdvice createNewMethodAdviceIfNeed(int contextId, String cls, String method, String desc, String mode,
                                                    RemoteCommand remoteCommand) {
        //String key = contextId + "#trace#" + cls + "." + method + "@" + desc;
        MethodAdvice methodAdvice;
        if (UTILS.isNullOrEmpty(mode)) {
            methodAdvice = new FullMatchMethodTraceAdviceImpl(contextId, cls, method, desc, null, remoteCommand);
        } else {
            switch (mode) {
                case "return": { // on return
                    methodAdvice = new OnReturnMethodTraceAdviceImpl(contextId, cls, method, desc, null, remoteCommand);
                    break;
                }
                case "throw": { // on throw
                    String targetThrowClass = remoteCommand.getParam("$forward-trace-option-e");
                    methodAdvice = new OnThrowMethodTraceAdviceImpl(
                            contextId, cls, method, desc, null, targetThrowClass, remoteCommand);
                    break;
                }
                case "record": { // history flow touch
                    String optionU = remoteCommand.getParam("$forward-trace-option-u");
                    int targetOrder = UTILS.safeParseInt(optionU, -1);
                    if (!UTILS.isNullOrEmpty(optionU) && targetOrder != -1) {
                        List<MethodTraceFrame> methodTraceFrames = queryTraceByOrderId(cls, method, desc, targetOrder);
                        if (methodTraceFrames != null && !methodTraceFrames.isEmpty()) {
                            remoteCommand.addParam("$command-trace-result", methodTraceFrames);
                            return null;
                        }
                    }

                    String recordTimeStr = remoteCommand.getParam("$forward-trace-option-n");
                    int recordCnt = UTILS.safeParseInt(recordTimeStr, 0);
                    String timeoutStr = remoteCommand.getParam("$forward-trace-option-time");
                    int timeout = UTILS.safeParseInt(timeoutStr, -1);
                    methodAdvice = new RecordCountLimitMethodTraceAdviceImpl(contextId, cls, method, desc,
                            null, recordCnt, timeout, this, remoteCommand);
                    break;
                }
                case "custom": { // custom input

                    // 分两种情况，如果-u参数设置了，并且是合法的，那么优先从record中取值然后输入
                    // 否则-i参数必须存在，否则抛出异常

                    Object[] customParam = null;
                    String iOptionVal;

                    String optionU = remoteCommand.getParam("$forward-trace-option-u");
                    int targetOrder = UTILS.safeParseInt(optionU, -1);
                    if (!UTILS.isNullOrEmpty(optionU) && targetOrder >= 0) {
                        // -u 参数起作用了
                        List<MethodTraceFrame> methodTraceFrames = queryTraceByOrderId(cls, method, desc, targetOrder);
                        if (methodTraceFrames != null && !methodTraceFrames.isEmpty()) {
                            for (MethodTraceFrame traceFrame : methodTraceFrames) {
                                if (traceFrame.isMethodEnter()) {
                                    customParam = traceFrame.getParams();
                                    //PSLogger.error("get the params:" + Arrays.toString(customParam));
                                    break;
                                }
                            }
                        }
                    }

                    // 是否已经满足要求，不能对无参数的方法进行观察
                    if (customParam == null) {
                        // -i 参数必填
                        iOptionVal = remoteCommand.getParam("$forward-trace-option-i");
                        if (UTILS.isNullOrEmpty(iOptionVal)) {
                            throw new IllegalStateException("please offer the params.");
                        }

                        // get the param from -i (transform to object[])
                        customParam = JacksonUtils.deserialize(iOptionVal, Object[].class);

                        PSLogger.error(String.format("deserialize the custom param:[%s] => [%s]",
                                iOptionVal, Arrays.toString(customParam)));
                    } else {
                        iOptionVal = JacksonUtils.serialize(customParam);
                    }

                    // 至此，如果还是null，那么直接抛出异常
                    if (customParam == null) {
                        throw new IllegalStateException("请提供方法输入参数 -u / -i，如果是无参方法，请使用其他type进行观察");
                    }

                    // 开始进行Advice配置
                    methodAdvice = new CustomInputMethodTraceAdviceImpl(
                            contextId, cls, method, desc, null, true, iOptionVal, customParam, remoteCommand);
                    break;
                }
                case "watch": { // watch mode
                    // get the expression
                    String iOptionVal = remoteCommand.getParam("$forward-trace-option-i");
                    if (UTILS.isNullOrEmpty(iOptionVal)) {
                        throw new IllegalArgumentException("you must offer a spring expression.");
                    }

                    // get the target method
                    Method targetMethod = remoteCommand.getParam("$forward-trace-tmp-targetMethod");

                    if (targetMethod == null) {
                        throw new IllegalArgumentException("you must attach the target method to command protocol.");
                    }

                    // get the advice
                    methodAdvice = new OnParamMatchMethodTraceAdviceIml(
                            contextId, cls, method, desc, null, true, iOptionVal, targetMethod, remoteCommand);
                    break;
                }
                default: { // default mode
                    methodAdvice = new FullMatchMethodTraceAdviceImpl(contextId, cls, method, desc, null, remoteCommand);
                }
            }
        }
        //METHOD_ADVICE_MAP.put(key, methodAdvice);
        return methodAdvice;
    }

    /**
     * 这个方法用来删除一个 {@link MethodAdvice}，这个方法一般在类被unlock的时候
     * 检测删除，当然如果被删除掉了，那么就没必要再删除了
     *
     * @param contextId 客户端标志
     * @param cls       类
     * @param method    方法
     * @param desc      方法描述
     * @return {@link MethodAdvice}
     */
    @Override
    public MethodAdvice removeMethodAdviceIfNeed(int contextId, String cls, String method, String desc) {
        String key = contextId + "#trace#" + cls + "." + method + "@" + desc;
        MethodAdvice methodAdvice = METHOD_ADVICE_MAP.get(key);
        if (method == null) {
            return null; // unnecessary
        }
        return METHOD_ADVICE_MAP.remove(key);
    }

    /**
     * 当某个愚蠢的人在使用了mt命令之后就退出了，或者执行了lock命令，那么别人就无法再来增强
     * 这个类的任何方法了，所以有必要清理一下这些"垃圾" {@link MethodAdvice}
     *
     * @param contextId console id
     * @param cls       类
     * @return 所有匹配到的类
     */
    @Override
    public Set<MethodAdvice> removeAdvice(int contextId, String cls) {
        Set<MethodAdvice> methodAdvices = new HashSet<>();
        Set<String> keys = new HashSet<>();

        for (Map.Entry<String, MethodAdvice> entry : METHOD_ADVICE_MAP.entrySet()) {
            String key = entry.getKey();
            // contextId#mit#cls.method@desc
            String[] split = key.split("#");
            if (split.length != 3) {
                continue;
            }
            split = split[2].split("\\.");
            // check the cls
            if (cls.equals(split[0])) {
                methodAdvices.add(entry.getValue());
                keys.add(key);
            }
        }

        // do remove
        if (!keys.isEmpty()) {
            for ( String key : keys) {
                METHOD_ADVICE_MAP.remove(key);
            }
        }

        return methodAdvices;
    }

    /**
     * 根据客户端来删除资源
     *
     * @param contextId 客户端id
     * @return 被删除的advice
     */
    @Override
    public Set<MethodAdvice> removeAdvice(int contextId) {
        Set<MethodAdvice> methodAdvices = new HashSet<>();
        Set<String> keys = new HashSet<>();

        for (Map.Entry<String, MethodAdvice> entry : METHOD_ADVICE_MAP.entrySet()) {
            String key = entry.getKey();
            // contextId#mit#cls.method@desc
            String[] split = key.split("#");
            if (split.length != 3) {
                continue;
            }
            int targetCid;
            try {
                targetCid = Integer.parseInt(split[0]);
            } catch (Throwable e) {
                PSLogger.error("could not parse string to int:" + split[0]);
                continue;
            }
            // check the cls
            if (contextId == targetCid) {
                methodAdvices.add(entry.getValue());
                keys.add(key);
            }
        }

        // do remove
        if (!keys.isEmpty()) {
            for ( String key : keys) {
                METHOD_ADVICE_MAP.remove(key);
            }
        }

        return methodAdvices;
    }

    /**
     * 这个方法的职责比较简单，看看客户端是否已经增强过某个具体的方法
     *
     * @param contextId 客户端标志
     * @param cls       类
     * @param method    方法
     * @param desc      方法描述
     * @return true     true代表已经被增强过了，不要再增强了
     */
    @Override
    public boolean isMethodClassWeaveDone(int contextId, String cls, String method, String desc) {
        String key = cls + "#" + method + "@" + desc;
        Integer own = METHOD_TRACE_ENHANCE_STATUS.get(key);

        //PSLogger.error("来判断了:" + METHOD_TRACE_ENHANCE_STATUS + " with key:" + key + " res:" + own);

        return own != null && own > 0;
    }

    /**
     * 记录一个方法的流量，需要使用相应的Advice才能做到
     * {@link RecordCountLimitMethodTraceAdviceImpl}
     *
     * @param cls    目标类
     * @param method 目标方法
     * @param desc   方法描述
     * @param traces 一次方法调用的信息堆栈记录
     */
    @Override
    public void recordMethodFlow(String cls, String method, String desc, List<MethodTraceFrame> traces) {
        String key = cls + "#" + method + "@" + desc;
        List<List<MethodTraceFrame>> mTraces =
                METHOD_TRACE_FLOW_RECORD_MAP.computeIfAbsent(key, k -> new ArrayList<>());
        if (mTraces.size() >= 10) {
            PSLogger.error("the method flow record limit 10, full : [" + key + "]");
            return;
        }

        PSLogger.info("Record for key:" + key + " with frame traces:" + traces);

        // record it.
        mTraces.add(traces);
    }

    /**
     * 需要回放流量来观察的时候，可以调用这个方法实现流量回放
     *
     * @param cls    目标类
     * @param method 目标方法
     * @param desc   方法描述
     * @param order  流量id，从0开始，值越小代表越早记录
     * @return 一次方法调用的历史堆栈，回放历史仅需要input即可
     */
    @Override
    public List<MethodTraceFrame> queryTraceByOrderId(String cls, String method, String desc, int order) {
        String key = cls + "#" + method + "@" + desc;
        List<List<MethodTraceFrame>> mTraces =
                METHOD_TRACE_FLOW_RECORD_MAP.computeIfAbsent(key, k -> new ArrayList<>());
        if (mTraces.isEmpty()) {
            PSLogger.error("the method trace record is empty: [" + key + "]");
            return null; // handle it.
        }
        if (order > mTraces.size()) {
            order = order % mTraces.size();
        }
        // get the target trace
        return mTraces.get(order);
    }

    /**
     * 一个client将字节码增强过之后，标记一下，之后就会被其他客户端共享
     *
     * @param context 客户端id
     * @param cls     增强的类
     * @param method  增强的方法
     * @param dsc     方法描述
     */
    @Override
    public void setMethodTraceEnhanceStatus(int context, String cls, String method, String dsc) {
        String key = cls + "#" + method + "@" + dsc;
        METHOD_TRACE_ENHANCE_STATUS.put(key, context);
    }

    /**
     * 将所有该类的增强相关状态清空，用于在redefine一个类之后的后置动作，下次
     * 就需要重新增强了
     *
     * @param cls 需要清楚的类
     */
    @Override
    public void clearClassWeaveByteCode(String cls) {
        if (UTILS.isNullOrEmpty(cls)) {
            return;
        }
        Set<String> clearKeys = new HashSet<>();

        for (Map.Entry<String, Integer> entry : METHOD_TRACE_ENHANCE_STATUS.entrySet()) {
            String key = entry.getKey();
            if (key.split("#").length != 2) {
                continue;
            }
            key = key.split("#")[0];
            if (UTILS.isNullOrEmpty(key)) {
                continue;
            }
            if (key.equals(cls)) {
                clearKeys.add(entry.getKey());
            }
        }

        if (!clearKeys.isEmpty()) {
            PSLogger.error("start to clear weave status for:" + clearKeys);

            for (String c : clearKeys) {
                METHOD_TRACE_ENHANCE_STATUS.remove(c);
            }
        }
    }

    /**
     * 如果一个类被增强/改变过，那么记录起来，调用这个方法可以获取到最新的类的字节码，如果类
     * 的字节码没有被变更过，那么调用这个方法直接返回null
     *
     * @param className 类名
     * @return 最新的字节码
     */
    @Override
    public byte[] lastBytesForClass(String className) {
        if (UTILS.isNullOrEmpty(className)) {
            return null;
        }
        List<byte[]> clsByteArray = CLASS_BYTE_CODE_RECORD_MAP.get(className);
        if (clsByteArray == null || clsByteArray.isEmpty()) {
            return null;
        }
        // return the last record
        return clsByteArray.get(clsByteArray.size() - 1);
    }

    /**
     * 因为目前rollback会将类回退到原始版本，所以需要将目前的类字节码删除
     *
     * @param className 类名
     */
    @Override
    public void clearBackupBytes(String className) {
        if (UTILS.isNullOrEmpty(className)) {
            return;
        }

        List<byte[]> objects = CLASS_BYTE_CODE_RECORD_MAP.remove(className);

        if (objects != null && !objects.isEmpty()) {
            PSLogger.error("remove all backup bytecode for class:" + className + " =>"  + objects.size());
        }
    }

    /**
     * 如果执行了rollback了，那么就回退类
     *
     * @param className 类名
     */
    @Override
    public void removeTopBytes(String className) {
        if (UTILS.isNullOrEmpty(className)) {
            return;
        }
        List<byte[]> clsByteArray = CLASS_BYTE_CODE_RECORD_MAP.get(className);
        if (clsByteArray == null || clsByteArray.isEmpty()) {
            PSLogger.error("the cls:" + className + " has not any bytecode cache");
            return;
        }

        PSLogger.error("rollback class:" + className);

        // remove last
        clsByteArray.remove(clsByteArray.size() - 1);

        // re-int
        CLASS_BYTE_CODE_RECORD_MAP.put(className, clsByteArray);
    }

    /**
     * 每次增强一个类，都调用一下这个方法，就可以将最新的字节码记录下来，下次增强就是基于这次变更的最新
     * 字节码来增强了
     *
     * @param className 类
     * @param bytes     本次需要记录的字节码
     */
    @Override
    public void recordEnhanceByteForClass(String className, byte[] bytes) {
        if (UTILS.isNullOrEmpty(className) || bytes == null || bytes.length <= 0) {
            PSLogger.error("the class name is null or the bytes is empty at method:recordEnhanceByteForClass");
            return;
        }
        List<byte[]> clsByteArray = CLASS_BYTE_CODE_RECORD_MAP.computeIfAbsent(className, k -> new ArrayList<>());
        clsByteArray.add(bytes);

        PSLogger.error("the class :" + className + " record new bytes, total record:" + clsByteArray.size());
    }

    /**
     * 用于将类的内部类缓存起来，因为如果每次都去增强一下原始类，听操蛋的
     *
     * @param cls        原始类
     * @param innerClass 内部类，然后不存在内部类，只要调用了该方法，则缓存一个空结果
     */
    @Override
    public void storeClassInnerClass(String cls, String innerClass) {
        String val = CLASS_INNER_CLASS_MAP.get(cls);
        if (val != null) {
            return;
        }
        innerClass = UTILS.nullToEmpty(innerClass);
        CLASS_INNER_CLASS_MAP.putIfAbsent(cls, innerClass);
    }

    /**
     * 获取一个类的内部类信息
     *
     * @param cls 需要获取的类
     * @return 内部类信息，可能为空
     */
    @Override
    public String getClassInnerClass(String cls) {
        if (UTILS.isNullOrEmpty(cls)) {
            return "";
        }
        return CLASS_INNER_CLASS_MAP.get(cls);
    }

    /**
     * get the cached method's line range [l,r]
     *
     * @param cacheKey the cache key
     * @return the lr-model
     */
    @Override
    public LRModel getLRModel(String cacheKey) {

        if (UTILS.isNullOrEmpty(cacheKey)) {
            return null;
        }

        // get the cached lr model, maybe null
        return LR_MODEL_CACHE_MAP.get(cacheKey);
    }

    /**
     * cache the lr model
     *
     * @param cacheKey key
     * @param lrModel  value
     */
    @Override
    public void cacheLRModel(String cacheKey, LRModel lrModel) {
        if (UTILS.isNullOrEmpty(cacheKey)) {
            return;
        }

        // overwrite the old cache
        LR_MODEL_CACHE_MAP.put(cacheKey, lrModel);
    }

    /**
     * list commands
     *
     * @return the commands
     */
    @Override
    public String listCommands() {
        return COMMAND_LIST;
    }

}
