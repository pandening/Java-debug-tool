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

import java.util.List;

/**
 * Created on 2019/5/10 10:40.
 *
 * @author <a href="H.J"> HuJian </a>
 */
@Deprecated
public interface MethodAdvice {

    /**
     *  做一些初始化的事情
     *
     */
    void init();

    /**
     *  该advice的type
     *
     * @return {@link MethodAdviceType}
     */
    MethodAdviceType type();

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
    void preMethodEnter(ClassLoader loader, String className, String methodName,
                        String methodDesc, Object target, Object[] args);

    /**
     *  所需要匹配的目标方法
     *
     * @return {@link MethodDesc}
     */
    MethodDesc targetMethod();

    /**
     *  这个方法用于重置advice
     *
     */
    void clearContext();

    /**
     *  获取到结果
     *
     * @return "$back-data" for {@link io.javadebug.core.transport.RemoteCommand}
     */
    String result();

    /**
     *  某些情况下，你可能需要自己组装返回结果，那么调用这个方法可以获取到原始的
     *  方法链路信息，这样你就可以组装自己的返回结果
     *  {@link MethodTraceFrame}
     *
     * @return 方法调用链路信息
     */
    List<MethodTraceFrame> traces();

    /**
     *  当某个方法被访问的时候，会首先通知该方法
     *
     * @param contextId cid
     * @param loader 加载的类加载器
     * @param className 方法类名
     * @param methodName 方法名称
     * @param methodDesc 方法描述
     * @param target 所属对象
     * @param args 方法参数
     */
    void onMethodEnter(int contextId, ClassLoader loader, String className, String methodName,
                       String methodDesc, Object target, Object[] args);

    /**
     *  当方法正常退出的时候，会通知这个方法
     *
     * @param methodDesc 方法描述
     * @param methodName 方法名称
     * @param className 类名称
     * @param contextId cid
     * @param returnVal 方法执行结果
     */
    void onMethodExit(int contextId, Object returnVal,  String className, String methodName, String methodDesc);

    /**
     *  当方法异常退出的时候，会通知这个方法
     *
     * @param methodDesc 方法描述
     * @param methodName 方法名称
     * @param className 类名称
     * @param contextId cid
     * @param throwable 发送的异常
     */
    void onMethodThrowing(int contextId, Throwable throwable,  String className, String methodName, String methodDesc);

    /**
     *  当访问方法的某一行的时候，会通知这个方法
     *
     * @param contextId cid
     * @param invokeThread 是哪个线程来访问的
     * @param lineNo 访问的行号
     */
    void invokeLine(int contextId, int lineNo, Thread invokeThread);

    /**
     *  当出现操作变量的指令的时候，会通知这个方法，比如出现存储、加载变量的指令的时候，这个
     *  方法就会拿到具体的通知
     *
     * @param contextId cid
     * @param localVariableTable 局部变量表，用于获取到变量名称
     * @param opcode 指令code
     * @param varIndex 操作的变量的index，可以去Local variable table里面取具体的lv
     */
    @Deprecated
    void invokeVarInstruction(int contextId, int opcode, int varIndex, LocalVariableTable localVariableTable);

    /**
     *  invokeVarInstruction 方法对通知实现要求较高，为了避免不必要的麻烦，转换一下
     *  直接提供可读的内容给通知
     *
     * @param opType "STORE"/"LOAD"
     * @param localVar 变量名称，不要太依赖这个名字，应该主要看行号，对照代码进行分析，变量名不准确
     * @param varVal 变量值，代表执行到此的变量的值具体是什么
     */
    void invokeVarInstructionV2(int contextId, String opType, String localVar, Object varVal);

    /**
     *  当符合要求的时候，weave会将特殊的标志传输下来，观察者就可以拿到这个结果，这个方法
     *  不会始终被调用，这一点需要注意
     *
     * @param o 传输的对象
     */
    @Deprecated
    void invokeSpecialTransformWithAttach(Object o);

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
    boolean checkSpecialCondition(ClassLoader loader, String className, String methodName,
                                  String methodDesc, Object target, Object[] args);

    /**
     *  拿到需要传输的数据，然后传输下去
     *
     * @return 需要传输的对象,可笑的是，你可以返回null
     */
    Object specialDataTransGet();

}
