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
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.ServerHook;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.annotation.CommandDescribe;
import io.javadebug.core.annotation.CommandType;
import io.javadebug.core.transport.RemoteCommand;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created on 2019/4/25 16:59.
 *
 * @author <a href="H.J"> HuJian </a>
 */
@CommandDescribe(name = "lockClass", simpleName = "lock", function = "用于锁定/解锁字节码，锁定字节码之后，其他客户端无法修改该字节码，除非解锁",
        usage = "lockClass|lock -c <className1,className2> -op [<lock>|<unlock>]", cmdType = CommandType.COMPUTE
)
public class LockClassCommand implements Command {

    /**
     * 命令的前置检测，在执行真正的命令执行之前，会先执行这个方法，如果觉得当前的输入无法满
     * 命令执行的需求，则返回false，或者直接抛出异常即可
     *
     * @param remoteCommand 协议内容
     * @return true则表示继续执行命令，false则会停止命令的执行
     */
    @Override
    public boolean preExecute(RemoteCommand remoteCommand) throws Exception {
        String clsList = remoteCommand.getParam("$forward-lock-class");
        String op = remoteCommand.getParam("$forward-lock-op");
        if (UTILS.isNullOrEmpty(clsList)) {
            PSLogger.error("没有获取到需要锁定的类信息:" + remoteCommand);
            return false; // 展示一下用法
        }
        if (UTILS.isNullOrEmpty(op)) {
            PSLogger.error("没有提供加锁/解锁操作类型:" + remoteCommand);
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
        String clsList = reqRemoteCommand.getParam("$forward-lock-class");
        String op = reqRemoteCommand.getParam("$forward-lock-op");
        String[] classes = clsList.split(",");
        StringBuilder sb = new StringBuilder();
        for (String cls : classes) {
            List<Class<?>> theClasses = findClassByName(cls, ins);
            if (theClasses.isEmpty()) {
                sb.append("类：").append(cls).append(" 暂未在目标JVM加载").append("\n");
                continue;
            }
            if (theClasses.size() > 1) {
                sb.append("类：").append(cls).append("发现多个同名类，暂不支持锁定").append(theClasses).append("\n");
                continue;
            }
            Class<?> theCls = theClasses.get(0);
            if ("lock".equals(op)) {
                boolean lockRet = serverHook.lockClassByte(cls, reqRemoteCommand.getContextId());
                sb.append("类：").append(theCls.getName()).append(" 锁定结果：").append(lockRet).append("\n");
            } else if ("unlock".equals(op)) {
                boolean lockRet = serverHook.unlockClassByte(cls, reqRemoteCommand.getContextId());
                sb.append("类：").append(theCls.getName()).append(" 解锁结果：").append(lockRet).append("\n");

                // 释放该类相关资源
                serverHook.removeAdvice(reqRemoteCommand.getContextId(), cls);
            } else if ("ask".equals(op)) {
               int byteCodeOwner = serverHook.bytecodeOwner(theCls.getName());
               if (byteCodeOwner == -1) {
                   byteCodeOwner = serverHook.bytecodeOwner(cls);
               }
               sb.append("类：").append(theCls.getName()).append(" 正在被客户端：").append(byteCodeOwner).append("持有").append("\n");
            } else {
                sb.append("不支持的操作类型：").append(op).append("\n");
                break;
            }
        }
        return sb.toString();
    }

    /**
     *  {@link Instrumentation#getAllLoadedClasses()}
     *
     * @param name 类名字，全限定名
     * @return 类
     */
    private List<Class<?>> findClassByName(String name, Instrumentation ins) {
        if (UTILS.isNullOrEmpty(name)) {
            return Collections.emptyList();
        }
        /// 如果发现多个类，那么抛出异常
        List<Class<?>> classes = new ArrayList<>();
        for (Class<?> cls : ins.getAllLoadedClasses()) {
            if (cls.getName().equals(name)) {
                classes.add(cls);
            }
        }
        return classes;
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
