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


package io.javadebug.core.thirdparty.asyncpprofiler;

import io.javadebug.core.utils.UTILS;
import io.javadebug.core.thirdparty.ShellExecutor;
import io.javadebug.core.thirdparty.ThirdPartyAbility;
import io.javadebug.core.thirdparty.ThirdPartySafetyDetective;
import io.javadebug.core.thirdparty.exception.ThirtPartyAbilityNotFindException;
import io.javadebug.core.thirdparty.exception.ThirtyPartyExecuteException;
import io.javadebug.core.transport.RemoteCommand;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 *  async-profiler是一个用于做运行时性能分析数据收集的工具，可以收集到准确的运行时性能数据，并
 *  可以多维度的进行性能问题分析
 *  {@see https://github.com/jvm-profiling-tools/async-profiler}
 *
 *  @author pandening
 *
 *  @since  2.0
 */
public class AsyncProfilerAbility implements ThirdPartyAbility {

    // -------------------------------------
    // 用于做环境校验，不需要每次都校验，第一次调用的时候校验即可
    // -------------------------------------
    private static volatile AtomicReference<Boolean> CHECK_ASYNC_PROFILER_ENVIRONMENT = null;

    // -------------------------------------
    // 用于生成命令对应的shell命令的map
    // -------------------------------------
    private static final Map<String, Function<RemoteCommand, String>> ASYNC_PROFILER_SHELL_MAP = new HashMap<>();

    // -------------------------------------
    // 用于生成命令对应的detective map
    // -------------------------------------
    private static final Map<String, Function<RemoteCommand,ThirdPartySafetyDetective>> ASYNC_PROFILER_SAFE_DETECTIVE_MAP = new HashMap<>();

    static {

        /// shell generate function map
        ASYNC_PROFILER_SHELL_MAP.put("", remoteCommand -> "");

        /// safety detective function map
        ASYNC_PROFILER_SAFE_DETECTIVE_MAP.put("", remoteCommand -> null);

    }

    /**
     *  用于判断当前环境是否可以执行async-profiler的命令的工作，主要还是check一下是否在bin目录下
     *  找到async-profiler相关配置
     *
     * @throws ThirtPartyAbilityNotFindException 如果环境不允许，则认为该第三方能力无法满足
     */
    private void checkAsyncProfilerEnvironment() throws ThirtPartyAbilityNotFindException, ThirtyPartyExecuteException {
        if (CHECK_ASYNC_PROFILER_ENVIRONMENT != null) {
            if (!CHECK_ASYNC_PROFILER_ENVIRONMENT.get()) {
                throw new ThirtPartyAbilityNotFindException("could not find third-party 'async-profiler' in current environment, please init it!");
            }
        }

        String workPath = ShellExecutor.getCurrentWorkPath();
        if (!workPath.endsWith("/")) {
            workPath += "/";
        }

        // the async path
        String asyncProfilerPath = workPath + "bin/third-party/async-profiler/build";
        File file = new File(asyncProfilerPath);
        if (!file.exists()) {
            CHECK_ASYNC_PROFILER_ENVIRONMENT = new AtomicReference<>(false);
            throw new ThirtPartyAbilityNotFindException("could not find third-party 'async-profiler' in current environment, please init it!");
        } else {
            CHECK_ASYNC_PROFILER_ENVIRONMENT = new AtomicReference<>(true);
        }

    }

    /**
     * 第三方类库执行命令，获取响应的入口
     *
     * @param remoteCommand Java-debug-tool协议结构体
     * @return 命令执行结果
     */
    @Override
    public String exec(RemoteCommand remoteCommand) throws ThirtPartyAbilityNotFindException, ThirtyPartyExecuteException {

        // check async-profiler ability
        checkAsyncProfilerEnvironment();

        // find the shell
        Function<RemoteCommand, String> shellFunction = ASYNC_PROFILER_SHELL_MAP.get(remoteCommand.getCommandName());
        if (shellFunction == null) {
            throw new ThirtPartyAbilityNotFindException("thirty-party 'async-profiler' not support command:"
                                                                + remoteCommand.getCommandName());
        }

        String shell = shellFunction.apply(remoteCommand);
        if (UTILS.isNullOrEmpty(shell)) {
            throw new ThirtyPartyExecuteException("third-party 'async-profiler' could not generate the shell for command:" +
                                                          remoteCommand.getCommandName());
        }

        // execute the shell
        StringBuilder shellResult = new StringBuilder();
        int exitCode = ShellExecutor.execShell(shell, shellResult);

        // error exit code
        if (exitCode != 0) {
            throw new ThirtyPartyExecuteException("could not execute third-party 'async-profiler' command "
                                                          + remoteCommand.getCommandName() + " with exitCode:"
                                                          + exitCode + ":" + shellResult.toString());
        }

        // check the result
        if (shellResult.length() == 0) {
            return "empty result from 'async-profiler' module\n";
        }

        // return the result.
        return shellResult.toString();
    }

    /**
     * 某些情况下，执行某个第三方命令需要安装一个安全保障组件，这样不至于在用户输入错误或者不符合命令
     * 规范的情况下对系统造成影响
     *
     * @param remoteCommand 命令协议
     * @return {@link ThirdPartySafetyDetective}
     */
    @Override
    public ThirdPartySafetyDetective detective(RemoteCommand remoteCommand) {

        // get the map function
        Function<RemoteCommand, ThirdPartySafetyDetective> fc =
                ASYNC_PROFILER_SAFE_DETECTIVE_MAP.get(remoteCommand.getCommandName());

        // check the map function
        if (fc == null) {
            return null;
        }

        // get it!
        return fc.apply(remoteCommand);
    }

    static class AsyncProfileDetective extends ThirdPartySafetyDetective {

        @Override
        protected void doDetective() {

        }
    }

}
