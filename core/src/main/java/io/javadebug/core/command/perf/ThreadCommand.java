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


package io.javadebug.core.command.perf;

import io.javadebug.core.CommandServer;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.ServerHook;
import io.javadebug.core.utils.ThreadUtils;
import io.javadebug.core.utils.Tuple2;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.annotation.CommandDescribe;
import io.javadebug.core.annotation.CommandType;
import io.javadebug.core.command.Command;
import io.javadebug.core.transport.RemoteCommand;

import java.lang.instrument.Instrumentation;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 *  @see java.lang.management.ThreadInfo
 */
@CommandDescribe(
        name = "thread",
        simpleName = "th",
        function = "about thread information in target jvm",
        usage = "thread \n" +
                        "       \t-top [topN] get the topN busy thread info\n" +
                        "       \t-status [R(runnable)|W(waiting)|TW(timed waiting)|B(blocking)] get the target status' thread info \n" +
                        "       \t-tid [thread id] get the thread info by thread id\n",
        cmdType = CommandType.COMPUTE
)
public class ThreadCommand implements Command {

    /**
     * 命令的前置检测，在执行真正的命令执行之前，会先执行这个方法，如果觉得当前的输入无法满
     * 命令执行的需求，则返回false，或者直接抛出异常即可
     *
     * @param remoteCommand 协议内容
     * @return true则表示继续执行命令，false则会停止命令的执行
     */
    @Override
    public boolean preExecute(RemoteCommand remoteCommand) throws Exception {
        return true;
    }

    /**
     * 一个命令需要实现该方法来执行具体的逻辑
     *
     * @param ins              增强器 {@link Instrumentation}
     * @param reqRemoteCommand 请求命令
     * @param commandServer    命令服务器
     * @param serverHook       {@link ServerHook} 服务钩子，用于便于从服务handler中获取数据
     * @return 响应命令
     */
    @Override
    public String execute(Instrumentation ins, RemoteCommand reqRemoteCommand, CommandServer commandServer, ServerHook serverHook) throws Exception {

        String topN = reqRemoteCommand.getParam("$forward-th-top");
        if (!UTILS.isNullOrEmpty(topN)) {
            return handleTopNBusyThreadReq(topN);
        }

        String statusOp = reqRemoteCommand.getParam("$forward-th-status");
        if (!UTILS.isNullOrEmpty(statusOp)) {
            return handleSpecialStatusThreadReq(statusOp);
        }

        String tidOp = reqRemoteCommand.getParam("$forward-th-tid");
        if (!UTILS.isNullOrEmpty(tidOp)) {
            return handleSpecialTidReq(tidOp);
        }

        // tell the usage
        return "please use \"h -cmd th\" to get the usage of this command\n";
    }

    /**
     *  get the target thread info by thread id
     *
     * @param tidOp the thread id
     * @return the result
     */
    private String handleSpecialTidReq(String tidOp) {

        int tid = UTILS.safeParseInt(tidOp, -1);
        if (tid < 0) {
            return "the thread id is invalid:" + tidOp + "\n";
        }
        // get the stack
        String tInfo = ThreadUtils.getThreadStackTrace(tid, null);
//        Map<Long,Long> allocMap = ThreadUtils.getThreadAllocatedBytes(new long[]{tid});
//        Long alloc = allocMap.get((long) tid);
//        if (alloc == null) {
//            PSLogger.error("could not get the allocBytes of thread : " + tid);
//        } else {
//            tInfo += "\nalloc bytes:" + alloc + "\n";
//        }

        if (UTILS.isNullOrEmpty(tInfo)) {
            return "could not find thread by tid:" + tInfo + "\n";
        }

        return tInfo;
    }

    /**
     *  get the topN busy thread info
     *
     * @param topNOption the topN
     * @return the result
     */
    private String handleTopNBusyThreadReq(String topNOption) {

        int topN = UTILS.safeParseInt(topNOption, -1);
        if (topN <= 0) {
            topN = 3;
            PSLogger.error("the topN options is null, fallback to " + topN);
        }
        Map<Long, Tuple2<Long, Long>> topNCpuBustThreadMap =
                ThreadUtils.getTopNCpuConsumeThreads(topN, false);

        // handle
        if (topNCpuBustThreadMap == null || topNCpuBustThreadMap.isEmpty()) {
            return "please use jstack to get the target jvm's thread stack firstly";
        }

        StringBuilder retSb = new StringBuilder();

        for (Map.Entry<Long, Tuple2<Long, Long>> entry : topNCpuBustThreadMap.entrySet()) {

            String each = ThreadUtils.getThreadStackTrace(entry.getKey(), entry.getValue());
            if (!UTILS.isNullOrEmpty(each)) {
                retSb.append(each).append("\n");
            }
        }

        return retSb.toString();
    }

    private static final Set<Thread.State> INCLUDE_ALL_KEEP_STATUS = new HashSet<>();
    static {
        INCLUDE_ALL_KEEP_STATUS.add(Thread.State.RUNNABLE);
        INCLUDE_ALL_KEEP_STATUS.add(Thread.State.WAITING);
        INCLUDE_ALL_KEEP_STATUS.add(Thread.State.TIMED_WAITING);
        INCLUDE_ALL_KEEP_STATUS.add(Thread.State.BLOCKED);
    }

    /**
     *  get the special status' thread
     *
     * @param status the status
     *               [R(runnable)|W(waiting)|TW(timed waiting)|B(blocking)]
     * @return result
     */
    private String handleSpecialStatusThreadReq(String status) {
        Set<Thread.State> includeStatus;
        if (UTILS.isNullOrEmpty(status)) {
            includeStatus = INCLUDE_ALL_KEEP_STATUS;
        } else {
            includeStatus = ThreadUtils.convertToStatus(status);
        }

        List<Thread> threadList = ThreadUtils.getAllThreads();
        long[] tidList = new long[threadList.size()];
        int i = 0;
        for (Thread thread : threadList) {
            tidList[i ++] = thread.getId();
        }

        ThreadInfo[] threadInfoList = ThreadUtils.getThreadInfo(tidList);

        if (threadInfoList == null || threadInfoList.length == 0) {
            return "empty thread info\n";
        }

        List<Long> includeTidList = new ArrayList<>();

        for (ThreadInfo threadInfo : threadInfoList) {
            if (includeStatus.contains(threadInfo.getThreadState())) {
                includeTidList.add(threadInfo.getThreadId());
            }
        }

        if (includeTidList.isEmpty()) {
            return "There is no thread with status : " + includeStatus + "\n";
        }

        StringBuilder sb = new StringBuilder();
        for (long tid : includeTidList) {
            String tInfo = ThreadUtils.getThreadStackTrace(tid, null);
            sb.append(tInfo).append("\n");
        }

        return sb.toString();
    }

    /**
     * 停止执行命令，任何一个命令的实现都需要感知到stop事件，这很重要，当命令中控器觉得命令
     * 执行的时间太久了，或者是检测到某种危险，或者觉得不再需要执行了，那么就会调用这个方法来
     * 打断命令的执行
     *
     * @param ins              {@link Instrumentation}
     * @param reqRemoteCommand {@link RemoteCommand}
     * @param commandServer    {@link CommandServer}
     * @param serverHook       {@link ServerHook}
     * @return 如果命令无法结束，请返回false，这样服务端虽然没办法，但是至少后续不会再同意这个
     * Client执行任何命令了，也就是在当前Context生命周期内，这个客户端被加入黑名单了
     */
    @Override
    public boolean stop(Instrumentation ins, RemoteCommand reqRemoteCommand, CommandServer commandServer, ServerHook serverHook) throws Exception {
        return false;
    }
}
