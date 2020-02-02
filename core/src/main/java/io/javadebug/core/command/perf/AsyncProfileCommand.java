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
import io.javadebug.core.ServerHook;
import io.javadebug.core.annotation.CommandDescribe;
import io.javadebug.core.annotation.CommandType;
import io.javadebug.core.command.Command;
import io.javadebug.core.transport.RemoteCommand;

import java.lang.instrument.Instrumentation;

@CommandDescribe(
        name = "profile",
        simpleName = "profile",
        function = "Sampling CPU and HEAP profiler for Java featuring AsyncGetCallTrace + perf_events",
        usage = "Usage: ./profiler.sh [action] [options] <pid>\n" +
                        "Actions:\n" +
                        "  start             start profiling and return immediately\n" +
                        "  resume            resume profiling without resetting collected data\n" +
                        "  stop              stop profiling\n" +
                        "  status            print profiling status\n" +
                        "  list              list profiling events supported by the target JVM\n" +
                        "  collect           collect profile for the specified period of time\n" +
                        "                    and then stop (default action)\n" +
                        "Options:\n" +
                        "  -e event          profiling event: cpu|alloc|lock|cache-misses etc.\n" +
                        "  -d duration       run profiling for <duration> seconds\n" +
                        "  -f filename       dump output to <filename>\n" +
                        "  -i interval       sampling interval in nanoseconds\n" +
                        "  -j jstackdepth    maximum Java stack depth\n" +
                        "  -b bufsize        frame buffer size\n" +
                        "  -t                profile different threads separately\n" +
                        "  -s                simple class names instead of FQN\n" +
                        "  -g                print method signatures\n" +
                        "  -a                annotate Java method names\n" +
                        "  -o fmt            output format: summary|traces|flat|collapsed|svg|tree|jfr\n" +
                        "  -v, --version     display version string\n" +
                        "\n" +
                        "  --title string    SVG title\n" +
                        "  --width px        SVG width\n" +
                        "  --height px       SVG frame height\n" +
                        "  --minwidth px     skip frames smaller than px\n" +
                        "  --reverse         generate stack-reversed FlameGraph / Call tree\n" +
                        "\n" +
                        "  --all-kernel      only include kernel-mode events\n" +
                        "  --all-user        only include user-mode events\n" +
                        "  --sync-walk       use synchronous JVMTI stack walker (dangerous!)\n" +
                        "\n" +
                        "<pid> is a numeric process ID of the target JVM\n" +
                        "      or 'jps' keyword to find running JVM automatically\n" +
                        "\n" +
                        "Example: ./profiler.sh -d 30 -f profile.svg 3456\n" +
                        "         ./profiler.sh start -i 999000 jps\n" +
                        "         ./profiler.sh stop -o summary,flat jps",
        cmdType = CommandType.COMPUTE
)
public class AsyncProfileCommand implements Command {


    /**
     * 命令的前置检测，在执行真正的命令执行之前，会先执行这个方法，如果觉得当前的输入无法满
     * 命令执行的需求，则返回false，或者直接抛出异常即可
     *
     * @param remoteCommand 协议内容
     * @return true则表示继续执行命令，false则会停止命令的执行
     */
    @Override
    public boolean preExecute(RemoteCommand remoteCommand) throws Exception {
        return false;
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
        return null;
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
