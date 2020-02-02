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


package io.javadebug.core.command;

import io.javadebug.core.CommandServer;
import io.javadebug.core.OptionController;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.ServerHook;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.annotation.CommandDescribe;
import io.javadebug.core.annotation.CommandType;
import io.javadebug.core.handler.CommandHandler;
import io.javadebug.core.transport.RemoteCommand;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created on 2019/4/27 18:47.
 *
 * @author <a href="H.J"> HuJian </a>
 */
@CommandDescribe(
        name = "option",
        simpleName = "set",
        function = "dynamic set for agent options",
        usage = "option|set -p [option name] -o [option operator]\n" +
                        "       \t dump                 \t: control whether dump class file after enhancing class [on | off]\n" +
                        "       \t ojson                \t: control the object print, toJson when the object is non-toString [on | off]\n" +
                        "       \t fjson                \t: control force print object to json [on | off] \n" +
                        "       \t log-info             \t: control the info log [on | off]\n" +
                        "       \t log-error            \t: control the error log [on | off]\n" +
                        "       \t close-while-no-con   \t: control the server shut down while no connections [on | off]\n" +
                        "       \t call                 \t: control whether show the call for thread collector [on | off]\n" +
                        "       \t top                  \t: control the topN number for thread collector  [an number]\n",
        cmdType = CommandType.COMPUTE
)
public class OptionCommand implements Command {

    // ------------------------------------------------
    /// 当前支持的所有设置项以及执行function
    // ------------------------------------------------
    private static final Map<String, Function<String, Boolean>> OPTION_MAP = new HashMap<>();

    static {

        // 开启或者关闭服务端info日志
        OPTION_MAP.put("log-info", new Function<String, Boolean>() {
            @Override
            public Boolean apply(String s) {
                if ("on".equals(s)) {
                    OptionController.onInfoLog();
                } else if ("off".equals(s)) {
                    OptionController.offInfoLog();
                } else {
                    return false;
                }
                return true;
            }
        });

        // 开启或者关闭服务端error日志
        OPTION_MAP.put("log-error", new Function<String, Boolean>() {
            @Override
            public Boolean apply(String s) {
                if ("on".equals(s)) {
                    OptionController.onErrorLog();
                } else if ("off".equals(s)) {
                    OptionController.offErrorLog();
                } else {
                    return false;
                }
                return true;
            }
        });

        // 服务端连接数为0的时候是否需要关闭服务端
        OPTION_MAP.put("close-while-no-con", new Function<String, Boolean>() {
            @Override
            public Boolean apply(String s) {
                if ("on".equals(s)) {
                    CommandHandler.setCloseServerOnNoClientConnect(true);
                } else if ("off".equals(s)) {
                    CommandHandler.setCloseServerOnNoClientConnect(false);
                } else {
                    return false;
                }
                return true;
            }
        });

        OPTION_MAP.put("ojson", new Function<String, Boolean>() {
            @Override
            public Boolean apply(String s) {
                if ("on".equals(s)) {
                    OptionController.onPrintObjectToJson();
                } else if ("off".equals(s)) {
                    OptionController.offPrintObjectToJson();
                } else {
                    return false;
                }
                return true;
            }
        });

        OPTION_MAP.put("fjson", new Function<String, Boolean>() {
            @Override
            public Boolean apply(String s) {
                if ("on".equals(s)) {
                    OptionController.onForcePrintObjectToJson();
                } else if ("off".equals(s)) {
                    OptionController.offForcePrintObjectToJson();
                } else {
                    return false;
                }
                return true;
            }
        });

        OPTION_MAP.put("dump", new Function<String, Boolean>() {
            @Override
            public Boolean apply(String s) {
                if ("on".equals(s)) {
                    OptionController.onDumpClassAfterEnhance();
                } else if ("off".equals(s)) {
                    OptionController.offDumpClassAfterEnhance();
                } else {
                    return false;
                }
                return true;
            }
        });

        OPTION_MAP.put("call", new Function<String, Boolean>() {
            @Override
            public Boolean apply(String s) {
                if ("on".equals(s)) {
                    OptionController.onShowTopCallStackForThreadMonitor();
                } else if ("off".equals(s)) {
                    OptionController.offShowTopCallStackForThreadMonitor();
                } else {
                    return false;
                }
                return true;
            }
        });

        OPTION_MAP.put("top", new Function<String, Boolean>() {
            @Override
            public Boolean apply(String s) {

                int topN = UTILS.safeParseInt(s, -1);
                if (topN <= 0) {
                    return false;
                }

                // set
                int old = OptionController.setTopNForThreadCollector(topN);

                PSLogger.error("the topN for thread collector set  [" + old + " -> " + topN + "]");
                return true;
            }
        });

    }

    /**
     * 命令的前置检测，在执行真正的命令执行之前，会先执行这个方法，如果觉得当前的输入无法满
     * 命令执行的需求，则返回false，或者直接抛出异常即可
     *
     * @param remoteCommand 协议内容
     * @return true则表示继续执行命令，false则会停止命令的执行
     */
    @Override
    public boolean preExecute(RemoteCommand remoteCommand) throws Exception {
        String optionName = remoteCommand.getParam("$forward-set-name");
        String optionOp= remoteCommand.getParam("$forward-set-op");
        if (UTILS.isNullOrEmpty(optionName) || UTILS.isNullOrEmpty(optionOp)) {
            PSLogger.error("不合法的命令调用:" + remoteCommand);
            return false;
        }
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
        String optionName = reqRemoteCommand.getParam("$forward-set-name");
        String optionOp= reqRemoteCommand.getParam("$forward-set-op");

        Function<String, Boolean> function = OPTION_MAP.get(optionName);

        if (function == null) {
            return "不支持的配置命令:" + optionName + "\n";
        }

        boolean result = function.apply(optionOp);

        if (!result) {
            return String.format("设置配置失败:[%s],[%s]\n", optionName, optionOp);
        }

        return String.format("设置配置成功:[%s],[%s]\n", optionName, optionOp);
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
        return true;
    }
}
