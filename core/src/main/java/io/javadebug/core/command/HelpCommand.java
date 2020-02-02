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
import io.javadebug.core.ServerHook;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.annotation.CommandDescribe;
import io.javadebug.core.annotation.CommandDescribeUtil;
import io.javadebug.core.annotation.CommandType;
import io.javadebug.core.transport.RemoteCommand;

import java.lang.instrument.Instrumentation;

/**
 * Created on 2019/4/22 22:52.
 *
 * @author <a href="H.J"> HuJian </a>
 */
@CommandDescribe(name = "help", simpleName = "h", function = "用于查看一个命令应该怎么使用",
        usage = "help|h -cmd [command]", cmdType = CommandType.COMPUTE
)
public class HelpCommand implements Command {

    /**
     * 命令的前置检测，在执行真正的命令执行之前，会先执行这个方法，如果觉得当前的输入无法满
     * 命令执行的需求，则返回false，或者直接抛出异常即可
     *
     * @param remoteCommand 协议内容
     * @return true则表示继续执行命令，false则会停止命令的执行
     */
    @Override
    public boolean preExecute(RemoteCommand remoteCommand) throws Exception {

        String helpCmdName = remoteCommand.getParam("$forward-help-cmd");
        if (UTILS.isNullOrEmpty(helpCmdName)) {
            return false; // 这样就会告诉客户端这个命令应该怎么用
        }

        /// 只要提供了所需要help的命令，就可以继续执行
        return true;
    }

    /**
     * 一个命令需要实现该方法来执行具体的逻辑
     *
     * @param ins              增强器 {@link Instrumentation}
     * @param reqRemoteCommand 请求命令
     * @return 响应命令
     */
    @Override
    public String execute(Instrumentation ins, RemoteCommand reqRemoteCommand, CommandServer commandServer, ServerHook serverHook) throws Exception {

        String helpCommand = reqRemoteCommand.getParam("$forward-help-cmd");
        if (UTILS.isNullOrEmpty(helpCommand)) {
            return CommandDescribeUtil.collectFromCommand(this);
        }
        Class<Command> commandClass = commandServer.queryCommand(helpCommand);
        if (commandClass == null) {
            return "不存在的命令[" + helpCommand + "]\n";
        }
        return CommandDescribeUtil.collectFromCommand(commandClass);
    }

    /**
     * 停止执行命令，任何一个命令的实现都需要感知到stop事件，这很重要，当命令中控器觉得命令
     * 执行的时间太久了，或者是检测到某种危险，或者觉得不再需要执行了，那么就会调用这个方法来
     * 打断命令的执行
     *
     * @return 如果命令无法结束，请返回false，这样服务端虽然没办法，但是至少后续不会再同意这个
     * Client执行任何命令了，也就是在当前Context生命周期内，这个客户端被加入黑名单了
     */
    @Override
    public boolean stop(Instrumentation ins, RemoteCommand reqRemoteCommand, CommandServer commandServer, ServerHook serverHook) throws Exception {
        return true;
    }
}
