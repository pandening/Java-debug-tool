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

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;

/**
 * Created on 2019/4/25 16:07.
 *
 * @author <a href="H.J"> HuJian </a>
 */
@CommandDescribe(name = "rollback", simpleName = "back", function = "恢复字节码为原始字节码，如果没有增强过字节码，该命令不会执行",
        usage = "rollback|back -c [className1,className2]", cmdType = CommandType.ENHANCE
)
public class RollbackClassCommand implements Command {

    /**
     * 命令的前置检测，在执行真正的命令执行之前，会先执行这个方法，如果觉得当前的输入无法满
     * 命令执行的需求，则返回false，或者直接抛出异常即可
     *
     * @param remoteCommand 协议内容
     * @return true则表示继续执行命令，false则会停止命令的执行
     */
    @Override
    public boolean preExecute(RemoteCommand remoteCommand) throws Exception {
        String clsList = remoteCommand.getParam("$forward-back-class");
        if (UTILS.isNullOrEmpty(clsList)) {
            PSLogger.error("没有获取到需要回滚的类信息:" + remoteCommand);
            return false; // 展示一下用法
        }
        return true;
    }

    /**
     * 一个命令需要实现该方法来执行具体的逻辑
     *
     * @param ins              增强器 {@link Instrumentation}
     * @param reqRemoteCommand 请求命令
     * @param commandServer    命令服务器
     * @param serverHook
     * @return 响应命令
     */
    @Override
    public String execute(Instrumentation ins, RemoteCommand reqRemoteCommand, CommandServer commandServer, ServerHook serverHook) throws Exception {
        String clsList = reqRemoteCommand.getParam("$forward-back-class");
        String[] classes = clsList.split(",");
        StringBuilder sb = new StringBuilder();
        long stwCost = 0;
        for (String cls : classes) {
            byte[] originClassByte = serverHook.getBackupClassByte(cls);
            if (originClassByte == null || originClassByte.length == 0) {
                sb.append("类:").append(cls).append(" 没有被增强过，不需要回滚").append("\n");
                continue;
            }
            Class<?> theClass = findClassByName(cls, ins);
            if (theClass == null) {
                sb.append("类:").append(cls).append(" 暂时没有在目标JVM加载").append("\n");
                continue;
            }
            long stwStart = System.currentTimeMillis();
            ins.redefineClasses(new ClassDefinition(theClass, originClassByte));
            long cost = (System.currentTimeMillis() - stwStart);
            stwCost += cost;
            sb.append("类:").append(cls).append(" 回滚成功，耗时:").append(cost).append(" ms").append("\n");

            // remove advice
            serverHook.removeAdvice(reqRemoteCommand.getContextId());

            // remove weave status
            serverHook.clearClassWeaveByteCode(cls);

            // clear all backup bytecode
            serverHook.clearBackupBytes(cls);

            // set the origin bytecode
            serverHook.backupClassByte(cls, originClassByte);


//            // remove top bytecode
//            serverHook.removeTopBytes(cls);
        }

        // stw cost
        reqRemoteCommand.setStwCost((int) stwCost);

        return sb.toString();
    }

    /**
     *  {@link Instrumentation#getAllLoadedClasses()}
     *
     * @param name 类名字，全限定名
     * @return 类
     */
    private Class<?> findClassByName(String name, Instrumentation ins) {
        if (UTILS.isNullOrEmpty(name)) {
            return null;
        }
        for (Class<?> cls : ins.getAllLoadedClasses()) {
            if (cls.getName().equals(name)) {
                return cls;
            }
        }
        return null;
    }

    /**
     * 停止执行命令，任何一个命令的实现都需要感知到stop事件，这很重要，当命令中控器觉得命令
     * 执行的时间太久了，或者是检测到某种危险，或者觉得不再需要执行了，那么就会调用这个方法来
     * 打断命令的执行
     *
     * @param ins
     * @param reqRemoteCommand
     * @param commandServer
     * @param serverHook
     * @return 如果命令无法结束，请返回false，这样服务端虽然没办法，但是至少后续不会再同意这个
     * Client执行任何命令了，也就是在当前Context生命周期内，这个客户端被加入黑名单了
     */
    @Override
    public boolean stop(Instrumentation ins, RemoteCommand reqRemoteCommand, CommandServer commandServer, ServerHook serverHook) throws Exception {
        return true;
    }

}
