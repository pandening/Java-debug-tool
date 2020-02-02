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
import io.javadebug.core.exception.CouldNotFindClassByteException;
import io.javadebug.core.exception.ForbidExecuteException;
import io.javadebug.core.transport.RemoteCommand;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static io.javadebug.core.utils.UTILS.format;

/**
 * Created on 2019/4/23 21:14.
 *
 * @author <a href="H.J"> HuJian </a>
 */
@CommandDescribe(name = "redefine", simpleName = "rdf", function = "用于动态替换类字节码",
        usage = "redefine|rdf -p [className1:class1Path className1:class2Path]", cmdType = CommandType.ENHANCE
)
public class RedefineClassCommand implements Command {

    /**
     * 命令的前置检测，在执行真正的命令执行之前，会先执行这个方法，如果觉得当前的输入无法满
     * 命令执行的需求，则返回false，或者直接抛出异常即可
     *
     * @param remoteCommand 协议内容
     * @return true则表示继续执行命令，false则会停止命令的执行
     */
    @Override
    public boolean preExecute(RemoteCommand remoteCommand) throws Exception {
        Object redefineClassMap = remoteCommand.getParam("$forward-rdf-kv");
        if (!(redefineClassMap instanceof HashMap)) {
            throw new IllegalArgumentException("不合法的参数类型");
        }
        if (((HashMap) redefineClassMap).isEmpty()) {
            PSLogger.error("不存在有效参数，但不属于错误");
            return false; // 告诉你使用方法
        }
        return true;
    }

    /**
     * 一个命令需要实现该方法来执行具体的逻辑
     *
     * @param ins              增强器 {@link Instrumentation}
     * @param reqRemoteCommand 请求命令
     * @param commandServer    命令服务器
     * @return 响应命令
     */
    @Override
    public String execute(Instrumentation ins, RemoteCommand reqRemoteCommand, CommandServer commandServer, ServerHook serverHook) throws Exception {
        Map<String, byte[]> redefineClassMap = reqRemoteCommand.getParam("$forward-rdf-kv");

        StringBuilder sb = new StringBuilder();
        // for each mode
        for (Map.Entry<String, byte[]> entry : redefineClassMap.entrySet()) {
            PSLogger.error("start to redefine class:" + entry.getKey());

            // fail fast，对于已经成功替换掉字节码的类，如果想要回滚，则执行相应的命令即可

            sb.append(redefineEachClass(entry.getKey(), entry.getValue(), reqRemoteCommand, serverHook, ins)).append("\n");

            PSLogger.error("end to redefine class:" + entry.getKey());
        }

        return sb.toString();
    }

    /**
     *  重新定义一个类，通过动态替换其字节码实现
     *
     * @param className 类名
     * @param bytes 需要替换的目标字节码
     * @param remoteCommand 协议
     * @param serverHook {@link ServerHook}
     * @return 结果
     * @throws Exception 执行异常
     */
    private String redefineEachClass(String className, byte[] bytes, RemoteCommand remoteCommand, ServerHook serverHook, Instrumentation ins) throws Exception {
        /// 是否已经被其他client锁定，或者是否被是自己重入
        if (!serverHook.lockClassByte(className, remoteCommand.getContextId())) {
            return "抱歉，类:[" + className + "] 已被其他客户端锁定";
        } else {
            PSLogger.error("客户端:" + remoteCommand.getContextId() + " 锁定了类:[" + className + "]");
        }

        // 找到类
        Class<?> cls = findClassByName(className, ins);
        if (cls == null) {
            throw new ForbidExecuteException("类:" + className + " 暂未在目标JVM加载，或者类不存在");
        }

        /// 获取原始类的字节码
        final byte[][] originBytes = {serverHook.getBackupClassByte(className)};
        if (originBytes[0] == null) {
            //originBytes[0] = getClassByteByName(cls);
            ClassFileTransformer classFileTransformer = new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                    if (cls.getName().replaceAll("\\.", "/").equals(className)) {
                        originBytes[0] = classfileBuffer;
                    }
                    return classfileBuffer;
                }
            };
            ins.addTransformer(classFileTransformer, true);
            try {
                ins.retransformClasses(cls);
            } catch (Throwable e) {
                PSLogger.error("error while retransformClasses:" + cls + ":" + e);
            } finally {
                ins.removeTransformer(classFileTransformer);
            }
        }
        if (originBytes[0] == null) {
            throw new CouldNotFindClassByteException("无法找到类的字节码:" + className);
        }

        boolean allowToRedefine = UTILS.allowToReloadClass(className, bytes);
        if (!allowToRedefine) {
            throw new ForbidExecuteException("不允许执行命令");
        }

        // 记录下原始字节码
        serverHook.backupClassByte(className, originBytes[0]);

        long stageSTWCost = System.currentTimeMillis();

        ins.redefineClasses(new ClassDefinition(cls, bytes));

        stageSTWCost = (System.currentTimeMillis() - stageSTWCost) + remoteCommand.getStwCost();

        remoteCommand.setStwCost((int) stageSTWCost);

        // record the new bytes
        serverHook.recordEnhanceByteForClass(className, bytes);

        // remove the weave advice
        serverHook.clearClassWeaveByteCode(cls.getName());

        return format("类：[%s] 重定义成功", className);
    }

    /**
     *  获取类的原始字节码
     *
     * @param cls 类
     * @return 字节码
     */
    @Deprecated
    private byte[] getClassByteByName(Class<?> cls) throws Exception {
        if (cls == null) {
            return null;
        }
        String jarPath = "";
        try {
            jarPath = cls.getProtectionDomain().getCodeSource().getLocation().getPath();
            PSLogger.error("jarPath:" + jarPath);
        } catch (Exception e) {
            String msg = "找不到类的资源:" + cls.getName() + ":"  + e;
            PSLogger.error(msg);
            return null;
        }
        JarFile JarFile = new JarFile(jarPath);
        Enumeration<JarEntry> jarEntryEnumeration = JarFile.entries();
        byte[] data = null;
        while (jarEntryEnumeration.hasMoreElements()) {
            JarEntry jarEntry = jarEntryEnumeration.nextElement();
            String jarEntryName = jarEntry.getName();
            jarEntryName = jarEntryName.replace("/", ".");
            String className = jarEntryName.replace(".class", "");
            if (!className.equals(cls.getName())) {
                continue;
            }
            InputStream is = JarFile.getInputStream(jarEntry);
            if (is == null) {
                break;
            }
            data = new byte[is.available()];
            int offset = 0;
            int readCnt;
            while (offset < data.length &&
                    (readCnt = is.read(data, offset, data.length - offset)) >= 0) {
                offset += readCnt;
            }
            if (offset < data.length) {
                throw new IOException("Could not read file completely:" + is.toString());
            }
            break;
        }
        return data;
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
     * @return 如果命令无法结束，请返回false，这样服务端虽然没办法，但是至少后续不会再同意这个
     * Client执行任何命令了，也就是在当前Context生命周期内，这个客户端被加入黑名单了
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean stop(Instrumentation ins, RemoteCommand reqRemoteCommand, CommandServer commandServer, ServerHook serverHook) throws Exception {
        if (!UTILS.isNullOrEmpty(reqRemoteCommand.getParam("$evil-stop-tag"))) {
            PSLogger.error("已经执行过stop操作");
            return true;
        }
        Object isRedefinedMap = reqRemoteCommand.getParam("$back-rd-redefine-status");
        if (isRedefinedMap == null || !(isRedefinedMap instanceof HashMap)) {
            PSLogger.error("rd命令执行，但是未发现执行过rd，直接退出");
            return true;
        }
        Map<String, Class<?>> realIsRedefinedMap = (HashMap<String, Class<?>>) isRedefinedMap;
        if (realIsRedefinedMap.isEmpty()) {
            return true;
        }
        Class<?>[] cls = new Class[realIsRedefinedMap.size()];
        realIsRedefinedMap.values().toArray(cls);

        /// 记录STW时间（粗略）
        long stwStart = System.currentTimeMillis();

        RollbackClass rollbackClass = new RollbackClass(serverHook, reqRemoteCommand);

        // batch reTransform
        ins.addTransformer(rollbackClass, true);

        ins.retransformClasses(cls);

        long stwMills = System.currentTimeMillis() - stwStart;

        int totalStwCost = (int) (reqRemoteCommand.getStwCost() + stwMills);

        reqRemoteCommand.setStwCost(totalStwCost);

        ins.removeTransformer(rollbackClass);

        reqRemoteCommand.addParam("$evil-stop-tag", "true");

        return true;
    }

    static class RollbackClass implements ClassFileTransformer {

        private ServerHook serverHook;
        private RemoteCommand remoteCommand;

        RollbackClass(ServerHook serverHook, RemoteCommand remoteCommand) {
            this.serverHook  = serverHook;
            this.remoteCommand = remoteCommand;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) throws IllegalClassFormatException {
            byte[] origin = serverHook.getBackupClassByte(className);
            if (origin == null) {
                PSLogger.error("类：" + className + " 字节码暂未被修改过，不需要回滚");
                return classfileBuffer;
            }
            PSLogger.error("回滚类:" + className);

            // 解锁
            serverHook.unlockClassByte(className, remoteCommand.getContextId());

            return origin;
        }
    }

}
