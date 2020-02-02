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
import io.javadebug.core.enhance.ClassMethodWeaveConfig;
import io.javadebug.core.enhance.ClassMethodWeaver;
import io.javadebug.core.enhance.CommonClassFileTransformer;
import io.javadebug.core.enhance.MethodInvokeAdvice;
import io.javadebug.core.enhance.MethodInvokeAdviceFactory;
import io.javadebug.core.enhance.MethodTraceConverter;
import io.javadebug.core.enhance.MethodTraceEnhance;
import io.javadebug.core.enhance.MethodTraceFrame;
import io.javadebug.core.exception.CouldNotFindClassByteException;
import io.javadebug.core.transport.RemoteCommand;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created on 2019/4/30 17:57.
 *
 * @author <a href="H.J"> HuJian </a>
 */
@CommandDescribe(
        name = "trace",
        simpleName = "mt",
        function = "set breakpoint and trace it, get the view of method execute in anytime",
        usage = "       \n\ntrace|mt -c <class> -m <method> -d [method desc] -t [watch|return|throw|record|custom|line] \n\n" +
                        "Options:\n" +
                        "[-t]       \t: the mode choose:\n" +
                        "                   \t'return' \t=> onReturn \n" +
                        "                   \t'throw'  \t=> onThrow\n" +
                        "                   \t'record' \t=> using stored request to touch\n" +
                        "                   \t'custom' \t=> you should input the request by -i param\n" +
                        "                   \t'watch'  \t=> wait the condition to end.\n" +
                        "                   \t'line'   \t=> get advice if the target line has been invoked.\n" +
                        "[-n]       \t: the record count\n" +
                        "[-time]    \t: the time to record\n" +
                        "[-timeout] \t: the timeout of the current round command execute(unit:seconds).\n" +
                        "[-s]       \t: special output: \n" +
                        "                   \t'trace'     \t=> print the call trace. \n" +
                        "                   \t'nc'        \t=> do not check the condition before method exiting.\n" +
                        "[-e]        \t: the target exception class.\n" +
                        "[-l]        \t: the target line number, for watching the local variable about the 'line'.\n" +
                        "[-u]        \t: set the index, then the program will input this record, throw exception if any error\n" +
                        "[-i]        \t: the custom input.\n" +
                        "[-c]        \t: the target class name, require param, the full path\n" +
                        "[-m]        \t: the target method name, require param, simple name\n" +
                        "[-tl]       \t: stop when the target line has been invoked\n" +
                        "[-te]       \t: stop when the target line's expression is true, the [tl] must be set before using this option\n" +
                        "[-d]        \t: the method desc, ex: int a(int) => (I)I, optional param, if just 1 method is matched, the param is unused\n" ,
        cmdType = CommandType.ENHANCE
)
public class MethodTraceCommand implements Command {

    /**
     * 命令的前置检测，在执行真正的命令执行之前，会先执行这个方法，如果觉得当前的输入无法满
     * 命令执行的需求，则返回false，或者直接抛出异常即可
     *
     * @param remoteCommand 协议内容
     * @return true则表示继续执行命令，false则会停止命令的执行
     */
    @Override
    public boolean preExecute(RemoteCommand remoteCommand) throws Exception {
        String cls = remoteCommand.getParam("$forward-trace-cls");
        String method = remoteCommand.getParam("$forward-trace-method");

        // check
        if (UTILS.isNullOrEmpty(cls) || UTILS.isNullOrEmpty(method)) {
            PSLogger.error("console " + remoteCommand.getContextId() +
                    " is foolish with protocol data:" + remoteCommand);
            return false; // print the usage of this command
        }

        // check forbid class
        // for fuck reason that cause :
        //   *** java.lang.instrument ASSERTION FAILED ***: "!errorOutstanding" with message transform method call failed at JPLISAgent.c line: 844
        // if ( !errorOutstanding ) {
        // 793             jplis_assert(agent->mInstrumentationImpl != NULL);
        // 794             jplis_assert(agent->mTransform != NULL);
        // 795             transformedBufferObject = (*jnienv)->CallObjectMethod(
        // 796                                                 jnienv,
        // 797                                                 agent->mInstrumentationImpl,
        // 798                                                 agent->mTransform,
        // 799                                                 loaderObject,
        // 800                                                 classNameStringObject,
        // 801                                                 classBeingRedefined,
        // 802                                                 protectionDomain,
        // 803                                                 classFileBufferObject,
        // 804                                                 is_retransformer);
        // 805             errorOutstanding = checkForAndClearThrowable(jnienv);
        // 806             jplis_assert_msg(!errorOutstanding, "transform method call failed");
        // 807         }
        // I don't know what's going on, but I just forbid enhance java.lang.* & sun.* class now;
        // maybe someday I will remove the following code
        //      ---- who can figure out the reason, contact with pandening ---
        //
        if (cls.startsWith("java.lang") || cls.startsWith("sun.")) {
            throw new UnsupportedOperationException("'java.lang.*' && 'sun.*' are not supported");
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
        String cls = reqRemoteCommand.getParam("$forward-trace-cls");
        String method = reqRemoteCommand.getParam("$forward-trace-method");
        String desc = reqRemoteCommand.getParam("$forward-trace-desc");

        // option val
        String lOptionVal = reqRemoteCommand.getParam("$forward-trace-option-l");
        String mode = reqRemoteCommand.getParam("$forward-trace-option-t");
        String sOptionVal = reqRemoteCommand.getParam("$forward-trace-option-s");
        //String fieldOptionsVal = (String) Optional.ofNullable(reqRemoteCommand.getParam("$forward-trace-option-field")).orElse("");

        // check whether the target class is exist
        Class<?> targetClass = null;
        try {
            boolean findMethod = false;
            Method targetMethod = null;
            //targetClass = Thread.currentThread().getContextClassLoader().loadClass(cls);

            for (Class<?> theCls : ins.getAllLoadedClasses()) {
                if (theCls.getName().equals(cls)) {
                    targetClass = theCls;
                    break;
                }
            }

            if (targetClass == null) {
                throw new IllegalStateException("类:" + cls + " 暂未被加载");
            }

            // io.javadebug.XXX不允许被观察
            if (targetClass.getName().startsWith("io.javadebug")) {
                String msg = "the io.javadebug package's class are not allow to enhance.";
                reqRemoteCommand.setResponseData(msg);
                throw new UnsupportedOperationException(msg);
            }

            int mc = 0;
            for (Method m : targetClass.getDeclaredMethods()) {
                if (m.getName().equals(method)) {
                    findMethod = true;
                    targetMethod = m;
                    mc ++;
                }
            }
            // this is ugly check!
            if (!findMethod) {
                throw new IllegalArgumentException(
                        String.format("could not get the target method:[%s] by scan method of class:[%s]",
                                method, targetClass.getName()));
            }

            // this method may be not the target method
            String thisMethodDesc = org.objectweb.asm.commons.Method.getMethod(targetMethod).getDescriptor();

            // get the desc
            if ( mc == 1) {
                desc = thisMethodDesc;
            } else {
                if (UTILS.isNullOrEmpty(desc)) {
                    throw new IllegalArgumentException(
                            String.format("目标类:[%s]中存在多个名字为:[%s]的方法，请提供目标方法描述来确定具体的方法", cls, method));
                }
                if (!desc.equals(thisMethodDesc)) {
                    // find the target method here
                    targetMethod = null;
                    for (Method m : targetClass.getDeclaredMethods()) {
                        if (m.getName().equals(method) &&
                                    org.objectweb.asm.commons.Method.getMethod(m).getDescriptor().equals(desc)) {
                            targetMethod = m;
                        }
                    }
                    // could not find the target method
                    if (targetMethod == null) {
                        throw new IllegalArgumentException(String.format("无法找到目标方法:[%s] [%s.%s]", desc, cls, method));
                    }
                }
            }

            // attach the target method to remote command protocol
            reqRemoteCommand.addCustomParam("$forward-trace-tmp-targetMethod", targetMethod);
        } catch (Throwable e) {
            PSLogger.error("could not get the target class or target method with exception:" + e);
            return "foolish eggs! ..>>..";
        }

        // check weave status
        String result = "foolish result.";

        // remove all advice
        ClassMethodWeaver.unregisterMethodAdvice(reqRemoteCommand.getContextId());


        // classname处理一下
        int splitIndex = targetClass.getName().lastIndexOf("/");
        cls = targetClass.getName();
        if (splitIndex > 0) {
            cls = cls.substring(0, splitIndex);
        }

        // choose the method advice and register it to weave.
        //MethodAdvice methodAdvice = serverHook.createNewMethodAdviceIfNeed(reqRemoteCommand.getContextId(), cls, method, desc, mode, reqRemoteCommand);

        MethodInvokeAdvice methodInvokeAdvice = MethodInvokeAdviceFactory.createAdvice(mode, reqRemoteCommand, cls, method, desc, serverHook, targetClass, ins);

        if (methodInvokeAdvice == null) {
            Object ret = reqRemoteCommand.getParam("$command-trace-result");
            if (ret != null) {
                if (List.class.isAssignableFrom(ret.getClass())) {
                    List<MethodTraceFrame> methodTraceFrames = reqRemoteCommand.getParam("$command-trace-result");
                    //return methodTraceFrames.toString();
                    return MethodTraceConverter.toLineWithVarCostTrace(methodTraceFrames, null, true, false);
                }
                // check
                if (reqRemoteCommand.hasResult()) {
                    return "-.-";
                }
            }

            throw new IllegalStateException("impossible null after serverHook.createNewMethodAdviceIfNeed");
        }

        //////////////// enhance area

        // 如果没有增强过，那么增强代码，否则，就选择合适的Advice进行register即可
        if (!serverHook.isMethodClassWeaveDone(reqRemoteCommand.getContextId(), cls, method, desc)) {

            // check the class own & lock the class
            if (!serverHook.lockClassByte(cls, reqRemoteCommand.getContextId())) {
                return "抱歉，类:" + cls + " 已经被其他客户端锁死，请稍后再试!";
            }

            /// 获取原始类的字节码
            final String finClsName = cls;
            final byte[][] originBytes = {serverHook.getBackupClassByte(cls)};
            if (originBytes[0] == null) {
                ClassFileTransformer classFileTransformer = new ClassFileTransformer() {
                    @Override
                    public byte[] transform(ClassLoader loader, String className,
                                            Class<?> classBeingRedefined,
                                            ProtectionDomain protectionDomain,
                                            byte[] classfileBuffer) throws IllegalClassFormatException {
                        if (finClsName.replaceAll("\\.", "/").equals(className)) {
                            originBytes[0] = classfileBuffer;
                        }
                        return classfileBuffer;
                    }
                };
                ins.addTransformer(classFileTransformer, true);
                try {
                    ins.retransformClasses(targetClass);
                } catch (Throwable e) {
                    PSLogger.error("error while retransformClasses:" + cls + ":" + e);
                } finally {
                    ins.removeTransformer(classFileTransformer);
                }
            }
            if (originBytes[0] == null) {
                throw new CouldNotFindClassByteException("无法找到类的字节码:" + cls);
            }

            // 记录下原始字节码
            serverHook.backupClassByte(cls, originBytes[0]);

            byte[] preEnhanceBytes = serverHook.lastBytesForClass(cls);

            // weave config
            ClassMethodWeaveConfig classMethodWeaveConfig = new ClassMethodWeaveConfig();
            if (!UTILS.isNullOrEmpty(sOptionVal) && "field".equals(sOptionVal)) {
                classMethodWeaveConfig.setField(true);
            }
            if (!UTILS.isNullOrEmpty(sOptionVal) && "fd".equals(sOptionVal)) {
                classMethodWeaveConfig.setFieldDiff(true);
                classMethodWeaveConfig.setField(true);
            }
            if (!UTILS.isNullOrEmpty(sOptionVal) && "sfield".equals(sOptionVal)) {
                classMethodWeaveConfig.setSfield(true);
            }
            if (!UTILS.isNullOrEmpty(sOptionVal) && "sfd".equals(sOptionVal)) {
                classMethodWeaveConfig.setSfd(true);
                classMethodWeaveConfig.setSfield(true);
            }

            // the classFileTransformer
            CommonClassFileTransformer classFileTransformer =
                    new MethodTraceEnhance(reqRemoteCommand.getContextId(), cls, method, desc, preEnhanceBytes, targetClass, classMethodWeaveConfig);

            // do it
            ins.addTransformer(classFileTransformer, true);

            try {
                long startTime = System.currentTimeMillis();

                ins.retransformClasses(targetClass);

                // record the stw time
                reqRemoteCommand.setStwCost((int) (System.currentTimeMillis() - startTime));

                // do redefine class work
                byte[] enhancedBytes = classFileTransformer.getEnhanceBytes();
                if (enhancedBytes == null) {
                    return "没有获取到增强过的字节码，请check日志\n";
                }

                // real work.
                //ins.redefineClasses(new ClassDefinition(targetClass, classFileTransformer.getEnhanceBytes()));

                // record the enhance result
                serverHook.recordEnhanceByteForClass(cls, enhancedBytes);

                // tag it
                serverHook.setMethodTraceEnhanceStatus(reqRemoteCommand.getContextId(), cls, method, desc);
            } catch (Throwable e) {
                PSLogger.error("could not do retransformClasses for class:" + cls + " with protocol:" + reqRemoteCommand);
                result += " err:" + e;
                return  result;
            } finally {
                ins.removeTransformer(classFileTransformer);
            }
        }

        ///////////////// watch work area

        // 设置advice
        ClassMethodWeaver.regAdvice(reqRemoteCommand.getContextId(), methodInvokeAdvice);

        // 已经设置了结果了
        if (reqRemoteCommand.hasResult()) {
            return "<result:1>";
        }

        //////---- filter work area

        List<MethodTraceFrame> methodTraceCommandList = methodInvokeAdvice.traces();

        // 已经设置了结果了
        if (reqRemoteCommand.hasResult()) {
            return "<result:2>";
        }

        // -l
        if (!UTILS.isNullOrEmpty(lOptionVal)) {
            Set<Integer> targetLineSet = new HashSet<>();
            String[] lines = lOptionVal.split(",");
            for (String lineNumber : lines) {
                int lv = UTILS.safeParseInt(lineNumber, -1);
                if (lv >= 0) {
                    targetLineSet.add(lv);
                }
            }
            return MethodTraceConverter.toLineWithVarCostTrace(methodTraceCommandList, targetLineSet, true, false);
        }

        boolean isField = false;
        if (!UTILS.isNullOrEmpty(sOptionVal) && "sf".equals(sOptionVal)) {
            isField = true;
        }

        // -s option
        if (!UTILS.isNullOrEmpty(sOptionVal)) {
            switch (sOptionVal) {
                case "line": {
                    // just print the line number trace
                    return MethodTraceConverter.toLineTrace(methodTraceCommandList, null, false, isField);
                }
                case "cost": {
                    // just print the line cost
                    return MethodTraceConverter.toLineWithCostTrace(methodTraceCommandList, null, false, isField);
                }
                case "trace": {
                    // just print the line cost
                    return MethodTraceConverter.toLineWithCostTrace(methodTraceCommandList, null, true, isField);
                }
            }
        }

        // the new result.
        return MethodTraceConverter.toLineWithVarCostTrace(methodTraceCommandList, null, true, isField);
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
