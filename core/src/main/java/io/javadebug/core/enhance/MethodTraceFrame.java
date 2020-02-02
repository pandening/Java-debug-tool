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

import io.javadebug.core.data.ClassField;
import io.javadebug.core.data.VariableModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MethodTraceFrame {

    // 下面的内容仅会在方法进入时携带
    private String className = "";
    private String methodName = "";
    private String methodDesc = "";
    private Object targetObject;
    private Object[] params;

    // 下面的内容仅在方法退出时携带
    private Object returnVal;
    private Throwable throwable;

    // 下面的内容仅方法访问某一行代码的时候携带（不包括进入退出）
    private int lineNo = -1;
    private String lineDesc = "";
    // varName=VarVal
    // 这里特别注意一下，这个变量表是和line有关系的，但是不意味着一个line对应着的变量表对应的就是
    // 该行的相关变量，是上一行（上一个lineNo）的，使用的时候记得处理一下，否则对不上
    private List<VariableModel> localVariable = new ArrayList<>();

    // 下面的代码用于说明方法是否结束
    private boolean isReturn = false;
    private boolean isThrow = false;

    // 标志是否是输入
    private boolean isMethodEnter = false;

    // 访问这一行代码的时候发生的时间，为了计算耗时
    private long invokeTimeMills;

    // 调用这个方法之前的执行路径，将这个结果附着在exit的frame上即可
    private String callTrace;

    // the caller
    private String caller;

    // 类字段快照
    private List<ClassField> onMethodEnterClassFields;
    private List<ClassField> onMethodExitClassFields;

    // 判断一下方法是否结束了
    public boolean isEnd() {
        return isReturn || isThrow;
    }

    /**
     *  新建一个方法进入的frame
     *
     * @param cls 是哪个类
     * @param method 是哪个方法
     * @param desc 如果方法重载了，那么可以根据这个字段确定到底是哪个方法
     * @param targetObject 目标对象实例
     * @param params 方法入参是什么
     * @return {@link MethodTraceFrame}
     */
    public static MethodTraceFrame newMethodEnterFrame(String cls, String method,
                                                       String desc, Object targetObject,
                                                       Object[] params, long time, String caller) {
        MethodTraceFrame methodTraceFrame = new MethodTraceFrame();

        methodTraceFrame.setClassName(cls);
        methodTraceFrame.setMethodName(method);
        methodTraceFrame.setMethodDesc(desc);
        methodTraceFrame.setTargetObject(targetObject);
        methodTraceFrame.setParams(params);
        methodTraceFrame.setInvokeTimeMills(time);
        methodTraceFrame.setMethodEnter(true);
        methodTraceFrame.setCaller(caller);

        return methodTraceFrame;
    }

    /**
     *  新建一个方法正常退出的frame
     *
     * @param returnVal 方法返回值
     * @return {@link MethodTraceFrame}
     */
    public static MethodTraceFrame newMethodExitFrame(Object returnVal, long time, List<ClassField> f1, List<ClassField> f2) {

        MethodTraceFrame methodTraceFrame = new MethodTraceFrame();

        methodTraceFrame.setReturnVal(returnVal);
        methodTraceFrame.setReturn(true);
        methodTraceFrame.setInvokeTimeMills(time);
        methodTraceFrame.setOnMethodEnterClassFields(f1);
        methodTraceFrame.setOnMethodExitClassFields(f2);

        return methodTraceFrame;
    }

    /**
     *  新建一个方法抛出异常退出的frame
     *
     * @param throwable 抛出的异常是什么
     * @return {@link MethodTraceFrame}
     */
    public static MethodTraceFrame newMethodThrowFrame(Throwable throwable, long time, List<ClassField> f1, List<ClassField> f2) {

        MethodTraceFrame methodTraceFrame = new MethodTraceFrame();

        methodTraceFrame.setThrowable(throwable);
        methodTraceFrame.setThrow(true);
        methodTraceFrame.setInvokeTimeMills(time);
        methodTraceFrame.setOnMethodEnterClassFields(f1);
        methodTraceFrame.setOnMethodExitClassFields(f2);

        return methodTraceFrame;
    }

    /**
     *  新建一个访问某一行的frame
     *
     * @param lineNo 执行行号
     * @param lineDesc 行的内容描述 {@link StackTraceElement#toString()}
     * @param localVariable 该行涉及到的变量赋值
     * @return {@link MethodTraceFrame}
     */
    public static MethodTraceFrame newMethodInvokeLineFrame(int lineNo, String lineDesc,
                                                            List<VariableModel> localVariable,
                                                            long time) {

        MethodTraceFrame methodTraceFrame = new MethodTraceFrame();

        methodTraceFrame.setLineNo(lineNo);
        methodTraceFrame.setLineDesc(lineDesc);

        // 变量信息赋值
        if (methodTraceFrame.localVariable != null) {
            if (localVariable != null) {
                methodTraceFrame.localVariable.addAll(localVariable);
            }
        } else if (localVariable != null) {
            methodTraceFrame.setLocalVariable(localVariable);
        }

        methodTraceFrame.setInvokeTimeMills(time);

        return methodTraceFrame;
    }

    public int getLineNo() {
        return lineNo;
    }

    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public void setMethodDesc(String methodDesc) {
        this.methodDesc = methodDesc;
    }

    public Object getTargetObject() {
        return targetObject;
    }

    public void setTargetObject(Object targetObject) {
        this.targetObject = targetObject;
    }

    public Object getReturnVal() {
        return returnVal;
    }

    public void setReturnVal(Object returnVal) {
        this.returnVal = returnVal;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public boolean isReturn() {
        return isReturn;
    }

    public void setReturn(boolean aReturn) {
        isReturn = aReturn;
    }

    public boolean isThrow() {
        return isThrow;
    }

    public void setThrow(boolean aThrow) {
        isThrow = aThrow;
    }


    public List<VariableModel> getLocalVariable() {
        return localVariable;
    }

    public void setLocalVariable(List<VariableModel> localVariable) {
        this.localVariable = localVariable;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    public String getLineDesc() {
        return lineDesc;
    }

    public void setLineDesc(String lineDesc) {
        this.lineDesc = lineDesc;
    }

    @Override
    public String toString() {
        return "MethodTraceFrame{" +
                       "lineNo=" + lineNo +
                       ", lineDesc='" + lineDesc + '\'' +
                       ", className='" + className + '\'' +
                       ", methodName='" + methodName + '\'' +
                       ", methodDesc='" + methodDesc + '\'' +
                       ", localVariable=" + localVariable +
                       ", isReturn=" + isReturn +
                       ", isThrow=" + isThrow +
                       ", params=" + Arrays.toString(params) +
                       '}';
    }

    public long getInvokeTimeMills() {
        return invokeTimeMills;
    }

    public void setInvokeTimeMills(long invokeTimeMills) {
        this.invokeTimeMills = invokeTimeMills;
    }

    public boolean isMethodEnter() {
        return isMethodEnter;
    }

    public void setMethodEnter(boolean methodEnter) {
        isMethodEnter = methodEnter;
    }

    public String getCallTrace() {
        return callTrace;
    }

    public void setCallTrace(String callTrace) {
        this.callTrace = callTrace;
    }

    public List<ClassField> getOnMethodEnterClassFields() {
        return onMethodEnterClassFields;
    }

    public void setOnMethodEnterClassFields(List<ClassField> onMethodEnterClassFields) {
        this.onMethodEnterClassFields = onMethodEnterClassFields;
    }

    public List<ClassField> getOnMethodExitClassFields() {
        return onMethodExitClassFields;
    }

    public void setOnMethodExitClassFields(List<ClassField> onMethodExitClassFields) {
        this.onMethodExitClassFields = onMethodExitClassFields;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }
}
