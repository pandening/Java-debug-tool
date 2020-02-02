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


package io.javadebug.core.enhance;

import io.javadebug.core.utils.CDHelper;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.data.VariableModel;
import io.javadebug.core.transport.RemoteCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2019/5/10 14:40.
 *
 * @author <a href="H.J"> HuJian </a>
 */
@Deprecated
public abstract class AbstractMethodTraceCommandAdvice implements MethodAdvice, UniqueSource<String> {

    // the result
    private StringBuffer result;

    // which console
    private int contextId;

    // which class
    String className;

    // which method
    String methodName;

    // the method's desc
    String methodDesc;

    // the space count controller
    private int currentSpaceCnt = 2;

    // the line-local variable
    private StringBuffer localVarSb;

    // record the exit line
    private int exitLineNo = 0;

    // pre-line information, null means this is the first line invoked
    private String preLineDetail;

    // line variable
    private List<VariableModel> localVariableRecord;

    // this round is return
    boolean isReturn = false;

    // this round is throw
    boolean isThrow  = false;

    // the listener
    private MethodTraceListener methodTraceListener;

    // copy the args
    private Object[] copyArgs;

    // the ret val
    private Object retVal;

    // the throwable
    private Throwable throwable;

    // last line invoke time, -1 means this is the first invoke round
    private long lastInvokeMills = -1;

    // the method enter time mills, for total cost
    private long methodEnterTimeMills = -1;

    // whether simply the output
    private boolean simpleOutMode;

    // the method trace list (ArrayList)
    private List<MethodTraceFrame> methodTraceFrames;

    // the remote command cache.
    RemoteCommand remoteCommand;

    // the target class
    private String targetCls;

    // the target method
    private String targetMethod;

    // the target method desc
    private String targetDesc;

    // the target object
    private Object theTargetObject;

    // target method desc
    MethodDesc targetMethodDesc;

    AbstractMethodTraceCommandAdvice(int context, String className,
                                     String method, String desc,
                                     MethodTraceListener listener,
                                     boolean simpleOutMode,
                                     RemoteCommand remoteCommand) {
        this.contextId = context;
        this.className = className;
        this.methodName = method;
        this.methodDesc = desc;
        this.methodTraceListener = listener;
        if (this.methodTraceListener == null) {
            this.methodTraceListener = MethodTraceListener.NOP_LISTENER;
        }
        this.simpleOutMode = simpleOutMode;
        this.remoteCommand = remoteCommand;

        this.targetMethodDesc = new MethodDesc(className, methodName, methodDesc, null);

        // set the cd
        CDHelper.set(getCDKey(), 1);
    }

    /**
     *  所需要匹配的目标方法
     *
     * @return {@link MethodDesc}
     */
    public MethodDesc targetMethod() {
        return this.targetMethodDesc;
    }

    /**
     *  调用这个方法不代表会走后续的通知，只是告诉你一下当前方法调用可能会做
     *  接下来的通知，不要依赖这个调用而在后续等待
     *
     * @param loader 类加载器
     * @param className 类名
     * @param methodName 方法
     * @param methodDesc 描述
     * @param target 目标对象
     * @param args 参数列表信息
     */
    public void preMethodEnter(ClassLoader loader, String className, String methodName,
                        String methodDesc, Object target, Object[] args) {
        this.theTargetObject = target;
    }

    /**
     *  这个方法用来控制一次可返回状态的时候是否要结束观察，这个方法由各个子类控制即可，比如
     *  onReturn类型的Advice，只需要看是否CD结束了（默认就是这种行为），但是比如onThrow这种
     *  类型的Advice就要看本次Record是否是Throw的，如果不是，那么需要再等下一轮。
     *
     * @param cls 本次流量类
     * @param method 本次调用方法
     * @param desc 本次调用方法描述
     * @param args 有时候需要通过参数来辨别是否需要结束一次观察
     * @param retVal 如果方法正常结束，则方法返回值
     * @param throwable 如果方法抛出异常，则抛出的异常是什么
     * @return true/false
     */
    protected abstract boolean allowToEnd(Object[] args, Object retVal, Throwable throwable,
                                          String cls, String method, String desc);

    /**
     *  判断一下是否需要记录
     *
     * @param className 本次流量类名字
     * @param methodName 方法名字
     * @param methodDesc 方法描述
     * @return 是否需要记录
     */
    boolean checkOwn(String className, String methodName, String methodDesc) {

        //PSLogger.error("target=>" + className + "." + methodName + "@" + methodDesc);
        //PSLogger.error("myself=>" + this.className + "." + this.methodName + "@" + this.methodDesc);

        boolean ret = this.className.equals(className) && this.methodName.equals(methodName);

        ret = ret && (UTILS.isNullOrEmpty(methodDesc) || methodDesc.equals(this.methodDesc));

        return ret;
    }

    /**
     *  某些情况下，需要在开始记录frame之前做一些事情，那就可以重载这个方法
     *
     */
    protected void preStart() {

    }

    /**
     *  在某些情况下，你可能需要在等待结果前做一些事情，那么重写这个方法即可实现，这个方法
     *  将在你获取结果的时候执行；
     *
     */
    protected void preWaitResult(Object target) {

    }

    /**
     *  重新设置listener
     *
     * @param listener {@link MethodTraceListener}
     */
    void resetListener(MethodTraceListener listener) {
        if (listener == null) {
            PSLogger.error("could not reset the listener cause the new listener is null");
            return; // 忽略
        }
        this.methodTraceListener = listener;
    }

    private String getCDKey() {
        return contextId + "#mt#" + className + "." + methodName + "@" + methodDesc;
    }

    /**
     * 这个方法用于重置advice
     */
    @Override
    public void clearContext() {
        currentSpaceCnt = 2;
        exitLineNo = 0;
        isReturn = false;
        isThrow = false;
        lastInvokeMills = -1;
        methodEnterTimeMills = -1;
        localVariableRecord = new ArrayList<>();
        methodTraceFrames = new CopyOnWriteArrayList<>();
        CDHelper.set(getCDKey(), 1);
    }

    @Override
    public void init() {
        while (theTargetObject == null) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                PSLogger.error("InterruptedException:" + e);
            }
        }
        // execute the pre wait method
        preWaitResult(theTargetObject);
    }

    /**
     * 获取到结果
     *
     * @return "$back-data" for {@link RemoteCommand}
     */
    @Override
    public String result() {

//        while (theTargetObject == null) {
//            try {
//                TimeUnit.SECONDS.sleep(1);
//            } catch (InterruptedException e) {
//                PSLogger.error("error occ while wait the target object:" + e);
//
//                // set the result
//                remoteCommand.setResponseData("命令执行超时:" + UTILS.getErrorMsg(e) + "\n");
//
//                // 是被停止了吧，不要继续了，直接返回吧
//                return UTILS.getErrorMsg(e);
//            }
//        }

        // wait to end
        CDHelper.await(getCDKey());

        if (!allowToEnd(copyArgs, retVal, throwable, targetCls, targetMethod ,targetDesc)) {
            // another round to watch, so register the advice again is ok
            return "#another#"; // ..>..
        }

        String ret = "命令执行无结果或者超时";
        if (result != null && result.length() > 0) {
            ret = result.toString();
        }

        // return
        return ret;
    }

    /**
     *  某些情况下，你可能需要自己组装返回结果，那么调用这个方法可以获取到原始的
     *  方法链路信息，这样你就可以组装自己的返回结果
     *  {@link MethodTraceFrame}
     *
     * @return 方法调用链路信息
     */
    public List<MethodTraceFrame> traces() {
        if (methodTraceFrames == null) {
            return Collections.emptyList();
        }

        // remove the line no < 0
//        methodTraceFrames.removeIf(
//                methodTraceFrame -> methodTraceFrame == null || methodTraceFrame.getLineNo() < 0);

        return methodTraceFrames;
    }

    /**
     * 当某个方法被访问的时候，会首先通知该方法
     *
     * @param contextId  cid
     * @param loader     加载的类加载器
     * @param className  方法类名
     * @param methodName 方法名称
     * @param methodDesc 方法描述
     * @param target     所属对象
     * @param args       方法参数
     */
    @Override
    public void onMethodEnter(int contextId, ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args) {
        // pre-start
        preStart();

        // the target
        theTargetObject = target;

        result = new StringBuffer();
        // generate the msg header
        result.append("\n[").append(className).append(".")
                .append(methodName).append("@\"").append(methodDesc)
                .append("\" in object:").append(target).append(" with param:").append(Arrays.toString(args)).append("]\n");

        // the first invoke
        //lastInvokeMills = System.currentTimeMillis();

        // the method enter time mills
        methodEnterTimeMills = System.currentTimeMillis();

        if (methodTraceFrames == null) {
            methodTraceFrames = new ArrayList<>();
        } else if (!methodTraceFrames.isEmpty()) {
            methodTraceFrames.clear();
        }

        // the method trace
        MethodTraceFrame methodTraceFrame = MethodTraceFrame.newMethodEnterFrame(className, methodName, methodDesc, target, args, System.currentTimeMillis(), "");

        // record it
        methodTraceFrames.add(methodTraceFrame);

        // notice listener
        methodTraceListener.invokeLine(methodTraceFrame);

        // copy the args (just copy an object ref)
        copyArgs = args;

        // copy target info
        targetCls = className;

        targetMethod = methodName;

        targetDesc = methodDesc;
    }

    /**
     * 当方法正常退出的时候，会通知这个方法
     *
     * @param contextId cid
     * @param returnVal 方法执行结果
     */
    @Override
    public void onMethodExit(int contextId, Object returnVal,  String className, String methodName, String methodDesc) {
        // generate the msg tail (normal return)
        result.append("ReturnVal:[").append(returnVal).append("] at line:")
                .append(exitLineNo).append(" total cost:")
                .append(System.currentTimeMillis() - methodEnterTimeMills).append(" ms\n");

        // unregister the advice
        MethodAdvice methodAdvice = ClassMethodWeaver.unregisterMethodAdvice(contextId);

        // onReturn
        isReturn = true;

        // the retVal
        this.retVal = returnVal;

        // the method trace frame
        MethodTraceFrame methodTraceFrame = MethodTraceFrame.newMethodExitFrame(returnVal, System.currentTimeMillis(), null, null);

        // record it
        methodTraceFrames.add(methodTraceFrame);

        // notice listener
        methodTraceListener.invokeLine(methodTraceFrame);

        // cd, must last call
        CDHelper.cd(getCDKey());
    }

    /**
     * 当方法异常退出的时候，会通知这个方法
     *
     * @param contextId cid
     * @param throwable 发送的异常
     */
    @Override
    public void onMethodThrowing(int contextId, Throwable throwable,  String className, String methodName, String methodDesc) {
        // generate the msg tail (throw exception)
        String error = UTILS.getErrorMsg(throwable);
        result.append("ThrowExceptionWithMessage[").append(error).append("] at line:")
                .append(exitLineNo).append(" total cost:")
                .append(System.currentTimeMillis() - methodEnterTimeMills).append(" ms\n");

        // unregister the advice
        MethodAdvice methodAdvice = ClassMethodWeaver.unregisterMethodAdvice(contextId);

        // onThrow
        isThrow = true;

        // the throwable
        this.throwable = throwable;

        // the method trace frame
        MethodTraceFrame methodTraceFrame = MethodTraceFrame.newMethodThrowFrame(throwable, System.currentTimeMillis(), null, null);

        // record the frame
        methodTraceFrames.add(methodTraceFrame);

        // notice listener
        methodTraceListener.invokeLine(methodTraceFrame);

        // cd, must last call
        CDHelper.cd(getCDKey());
    }

    /**
     * 当访问方法的某一行的时候，会通知这个方法
     *
     * @param contextId    cid
     * @param lineNo       访问的行号
     * @param invokeThread 是哪个线程来访问的
     */
    @Override
    public void invokeLine(int contextId, int lineNo, Thread invokeThread) {
        // this is the actual the invoking line information.
        StringBuffer lineBuilder = new StringBuffer();
        // append the line
        for (StackTraceElement stackTraceElement : invokeThread.getStackTrace()) {
            if (stackTraceElement.getClassName().contains(className)) {
                // compute the time cost
                long cost = 0;
                if (lastInvokeMills != -1) {
                    cost = System.currentTimeMillis() - lastInvokeMills;
                }
                lastInvokeMills = System.currentTimeMillis();

                // this the actual one
                int ln = lineNo;
                if (stackTraceElement.getLineNumber() > 0) {
                    ln = stackTraceElement.getLineNumber(); // I don't know
                }

                if (simpleOutMode) {
                    lineBuilder.append("invoke line:").append(lineNo).append(" ")
                            .append(" [pre-cost").append(cost).append(" ms]\n");
                } else {
                    // add a line invoke
                    appendSpace(lineBuilder);

                    lineBuilder.append("+").append("[pre-line cost:").append(cost).append(" ms] ")
                            .append(stackTraceElement.toString()).append(" at line:").append(ln).append("\n");
                }
                if (preLineDetail == null) {
                    preLineDetail = lineBuilder.toString();
                } else {
                    // pre-line handle
                    result.append(preLineDetail);
                    preLineDetail = lineBuilder.toString();
                    // 看看是否有变量
                    if (localVarSb.length() > 0 && !localVarSb.toString().equals("[")) {

                        // fill space
                        for (int i = 0; i < currentSpaceCnt; i ++) {
                            result.append(" ");
                        }

                        result.append("\\-").append(localVarSb).append("]\n");
                    }
                }

                // the method trace frame
                MethodTraceFrame methodTraceFrame = MethodTraceFrame.newMethodInvokeLineFrame(
                        lineNo, stackTraceElement.toString(), localVariableRecord, System.currentTimeMillis());

                // record it
                methodTraceFrames.add(methodTraceFrame);

                // notice the listener
                methodTraceListener.invokeLine(methodTraceFrame);
                // for another line record
                localVariableRecord = new ArrayList<>();
                break;
            }
        }

        // clear the local buf
        localVarSb = new StringBuffer();
        localVarSb.append("[");

        // record the line number
        exitLineNo = lineNo;
    }

    /**
     *  每次调用都会增加一些空格，为了好看而已
     *
     */
    private void appendSpace(StringBuffer sb) {
        for (int i = 0; i < currentSpaceCnt; i ++) {
            sb.append(" ");
        }
        currentSpaceCnt ++;
    }

    /**
     * 当出现操作变量的指令的时候，会通知这个方法，比如出现存储、加载变量的指令的时候，这个
     * 方法就会拿到具体的通知
     *
     * @param contextId          cid
     * @param opcode             指令code
     * @param varIndex           操作的变量的index，可以去Local variable table里面取具体的lv
     * @param localVariableTable 局部变量表，用于获取到变量名称
     */
    @Override
    public void invokeVarInstruction(int contextId, int opcode, int varIndex,
                                     LocalVariableTable localVariableTable) {
        // old code..
    }

    /**
     * invokeVarInstruction 方法对通知实现要求较高，为了避免不必要的麻烦，转换一下
     * 直接提供可读的内容给通知
     *
     * @param opType   "STORE"/"LOAD"
     * @param localVar 变量名称，不要太依赖这个名字，应该主要看行号，对照代码进行分析，变量名不准确
     * @param varVal   变量值，代表执行到此的变量的值具体是什么
     */
    @Override
    public void invokeVarInstructionV2(int contextId, String opType, String localVar, Object varVal) {
        if (localVarSb == null) {
            localVarSb = new StringBuffer();
            localVarSb.append("[");
        }
        if (varVal == null) {
            return;
        }
        String kv = varVal.toString();
        if (!UTILS.isNullOrEmpty(localVar)) {
            // 可以拿到变量名
            kv = localVar + " = " + varVal;
        }
        localVarSb.append("(").append(opType).append(")").append(" ")
                .append(kv).append(" ");

        // record
        if (localVariableRecord == null) {
            localVariableRecord = new ArrayList<>();
        }
        localVariableRecord.add(new VariableModel(varVal, localVar));
    }

    /**
     *  当符合要求的时候，weave会将特殊的标志传输下来，观察者就可以拿到这个结果，这个方法
     *  不会始终被调用，这一点需要注意
     *
     * @param o 传输的对象
     */
    public void invokeSpecialTransformWithAttach(Object o) {
    }

    /**
     *  如果你需要传递什么数据到观察者，那么使用需要使得这个方法返回true，如果返回false，那么
     *  就不会将数据传输到观察者那边去
     *
     * @param loader 方法的类加载器
     * @param className 方法所属的类名
     * @param methodName 方法名字
     * @param methodDesc 方法描述
     * @param target 目标对象
     * @param args 方法参数
     * @return 是否需要传递
     */
    public boolean checkSpecialCondition(ClassLoader loader, String className, String methodName,
                                  String methodDesc, Object target, Object[] args) {
        return false;
    }

    /**
     *  拿到需要传输的数据，然后传输下去
     *
     * @return 需要传输的对象,可笑的是，你可以返回null
     */
    public Object specialDataTransGet() {
        return get();
    }

    /**
     *  在某些情况下，可能需要获取到一个区别各个client/advice的数据，比如uuid等，那
     *  这个方法就比较有用了
     *
     * @return 唯一数据
     */
    public String get() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
