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
import io.javadebug.core.utils.Strings;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.annotation.CommandDescribe;
import io.javadebug.core.annotation.CommandType;
import io.javadebug.core.enhance.InnerClassClassFilteTransformer;
import io.javadebug.core.transport.RemoteCommand;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static io.javadebug.core.utils.UTILS.format;

/**
 * Created on 2019/4/21 12:57.
 *
 * @author <a href="H.J"> HuJian </a>
 */
@CommandDescribe(name = "findClass", simpleName = "fc", function = "用于查找一个类是从哪个jar包加载的",
        usage = "findClass|fc <-class|-r> <className> -l [count]", cmdType = CommandType.COMPUTE
)
public class FindJavaSourceCommand implements Command {

    /**
     * 命令的前置检测，在执行真正的命令执行之前，会先执行这个方法，如果觉得当前的输入无法满
     * 命令执行的需求，则返回false，或者直接抛出异常即可
     *
     * @param remoteCommand 协议内容
     * @return true则表示继续执行命令，false则会停止命令的执行
     */
    @Override
    public boolean preExecute(RemoteCommand remoteCommand) throws Exception {
        String cls = remoteCommand.getParam("$forward-fc-class");
        String regex = remoteCommand.getParam("$forward-fc-regex");
        if (UTILS.isNullOrEmpty(cls) && UTILS.isNullOrEmpty(regex)) {
            PSLogger.error("携带命令参数不完整，拒绝执行命令");
            return false;
        }
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
        String clsName = Strings.nullToEmpty(reqRemoteCommand.getParam("$forward-fc-class"));
        String regex = Strings.nullToEmpty(reqRemoteCommand.getParam("$forward-fc-regex"));
        int limit = reqRemoteCommand.getParam("$forward-fc-limit");
        Pattern pattern = null;
        if (!UTILS.isNullOrEmpty(regex)) {
            PSLogger.error("使用正则匹配:" + regex);
            pattern = Pattern.compile(regex);
        }
        Class[] classes = ins.getAllLoadedClasses();
        StringBuilder retSb = new StringBuilder();
        String report = clsName;
        if (UTILS.isNullOrEmpty(report)) {
            report = regex;
        }
        int cnt = 1;
        for (Class<?> cls : classes) {
            try {
                if (pattern != null) {
                    if (pattern.matcher(cls.getName()).matches()) {
                        appendNewResp(retSb, cls, cnt ++, serverHook, ins, reqRemoteCommand);
                    }
                } else if (cls.getName().equals(clsName) || cls.getSimpleName().equals(clsName)) {
                    appendNewResp(retSb, cls, cnt ++, serverHook, ins, reqRemoteCommand);
                }
                if (cnt > limit) {
                    break;
                }
            } catch (Throwable e) {
                PSLogger.error("error while match cls:" + e);
            }
        }
        if (retSb.length() == 0) {
            return "[" + report + "]暂未在目标JVM中加载\n";
        }

        PSLogger.info("匹配到的内容:" + retSb.toString());

        return retSb.toString();
    }

    /**
     *  在结果终止追加一条响应
     *
     * @param sb 结果
     * @param cls 匹配到的类
     */
    private void appendNewResp(StringBuilder sb, Class<?> cls, int cnt, ServerHook serverHook,
                               Instrumentation ins, RemoteCommand remoteCommand) {
        String ret = "ClassName\t:" + cls.getName() + "\n";
        if (cls.getName().startsWith("java.")) {
            ret += "Resource\t:rt.jar\nClassLoader\t:BootstrapClassloader\n";
        } else {
            try {
                ret += ("Resource\t:" + cls.getProtectionDomain().getCodeSource().getLocation().getFile() + "\n");
                ClassLoader classLoader = cls.getClassLoader();
                ret += "ClassLoader\t:" + classLoader + "\n";
                String space = "         ";
                String methods = getMethodsOfClass(cls, space);
                if (!UTILS.isNullOrEmpty(methods)) {
                    ret += "Methods:\n" + methods;
                }
                String innerClass = getInnerClassOfClassV2(cls, serverHook, ins, remoteCommand, space);
                if (!UTILS.isNullOrEmpty(innerClass)) {
                    ret += "InnerClass:\n" + innerClass;
                }
            } catch (Exception e) {
                // ignore
                PSLogger.error(format("无法获取类:[%s]信息：%s", cls, e));
            }
        }
        if (!UTILS.isNullOrEmpty(ret)) {
            sb.append("[").append(cnt).append("]\n").append(ret);
        }
    }

    /**
     *  将类的所有方法路排列出来，对于调试lambda等需要获取方法名的场景特别有用
     *
     * @param clazz 需要罗列方法的类
     * @return 方法列表
     *           Methods:
     *             -> method 1
     *             -> method 2
     *             -> method 3
     */
    private String getMethodsOfClass(Class<?> clazz, String space) {
        Method[] methods = clazz.getDeclaredMethods();
        StringBuilder stringBuilder = new StringBuilder();
        for (Method method : methods) {
            stringBuilder
                    .append(space)
                    .append("->")
                    .append(method.toGenericString())
                    .append("\n")
                    .append(space).append(" \\- ")
                    .append(org.objectweb.asm.commons.Method.getMethod(method).getDescriptor())
                    .append("\n");
        }
        return stringBuilder.toString();
    }

    /**
     * V2 =>
     *  将类的内部类都罗列出来，这样就可以深度调试了
     *
     * @param clazz 需要罗列内部类的类
     * @return 内部类
     *         InnerClass:
     *           -> innerClass 1
     *           -> innerClass 2
     *           -> innerClass 3
     *
     */
    private String getInnerClassOfClassV2(Class<?> clazz, ServerHook serverHook, Instrumentation ins,
                                          RemoteCommand remoteCommand, String space) {
        String clsName = clazz.getName();
        String val = serverHook.getClassInnerClass(clsName);
        if (val != null) {
            return val;
        }

        // 开始获取内部类信息
        List<String> resultList = new ArrayList<>();
        byte[] preEnhanceBytes = serverHook.lastBytesForClass(clazz.getName());
        InnerClassClassFilteTransformer innerClassClassFilteTransformer =
                new InnerClassClassFilteTransformer(clazz.getName(), resultList, preEnhanceBytes);

        try {
            ins.addTransformer(innerClassClassFilteTransformer, true);

            long startTime = System.currentTimeMillis();

            ins.retransformClasses(clazz);

            // record the stw time
            remoteCommand.setStwCost((int) (System.currentTimeMillis() - startTime));

        } catch (Exception e) {
            PSLogger.error("could not do retransformClasses for class:" + clsName
                                   + " with protocol:" +remoteCommand + "\n" + e);
        } finally {
            ins.removeTransformer(innerClassClassFilteTransformer);
        }

        if (resultList.isEmpty()) {
            serverHook.storeClassInnerClass(clsName, "");
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (String s : resultList) {
            if (s.equals(clsName)) {
                continue; // 不要包括自己
            }
            sb.append(space).append(s).append("\n");
        }

        // store it!
        serverHook.storeClassInnerClass(clsName, sb.toString());

        return sb.toString();
    }

    /**
     * V1 =>
     *  将类的内部类都罗列出来，这样就可以深度调试了
     *
     * @param clazz 需要罗列内部类的类
     * @return 内部类
     *         InnerClass:
     *           -> innerClass 1
     *           -> innerClass 2
     *           -> innerClass 3
     *
     */
    private String getInnerClassOfClass(Class<?> clazz, ServerHook serverHook, Instrumentation ins,
                                        RemoteCommand remoteCommand, String space) {

        StringBuilder sb = new StringBuilder();
        for (Class<?> cls : clazz.getDeclaredClasses()) {
            sb.append(space).append(cls.getName()).append("\n");
        }
        return sb.toString();

//        String clsName = clazz.getName();
//        String val = serverHook.getClassInnerClass(clsName);
//        if (val != null) {
//            return val;
//        }
//
//        // 开始获取内部类信息
//        List<String> resultList = new ArrayList<>();
//        InnerClassClassFilteTransformer innerClassClassFilteTransformer =
//                new InnerClassClassFilteTransformer(clazz.getName(), resultList);
//
//        try {
//            ins.addTransformer(innerClassClassFilteTransformer, true);
//
//            long startTime = System.currentTimeMillis();
//
//            ins.retransformClasses(clazz);
//
//            // record the stw time
//            remoteCommand.setStwCost((int) (System.currentTimeMillis() - startTime));
//
//        } catch (Exception e) {
//            PSLogger.error("could not do retransformClasses for class:" + clsName
//                                   + " with protocol:" +remoteCommand + "\n" + e);
//        } finally {
//            ins.removeTransformer(innerClassClassFilteTransformer);
//        }
//
//        if (resultList.isEmpty()) {
//            serverHook.storeClassInnerClass(clsName, "");
//            return "";
//        }
//
//        StringBuilder sb = new StringBuilder();
//
//        for (String s : resultList) {
//            if (s.equals(clsName)) {
//                continue; // 不要包括自己
//            }
//            sb.append(space).append(s).append("\n");
//        }
//
//        // store it!
//        serverHook.storeClassInnerClass(clsName, sb.toString());
//
//        return sb.toString();
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
