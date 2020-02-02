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

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.transport.RemoteCommand;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created on 2019/5/10 14:40.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public enum EmptyPrintMethodAdvice implements MethodAdvice {
    EMPTY_PRINT_METHOD_ADVICE {
        /**
         * 做一些初始化的事情
         */
        @Override
        public void init() {

        }

        /**
         * 该advice的type
         *
         * @return {@link MethodAdviceType}
         */
        @Override
        public MethodAdviceType type() {
            return MethodAdviceType.FULL_MATCH;
        }

        /**
         * 调用这个方法不代表会走后续的通知，只是告诉你一下当前方法调用可能会做
         * 接下来的通知，不要依赖这个调用而在后续等待
         *
         * @param loader     类加载器
         * @param className  类名
         * @param methodName 方法
         * @param methodDesc 描述
         * @param target     目标对象
         * @param args       参数列表信息
         */
        @Override
        public void preMethodEnter(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args) {

        }

        /**
         * 所需要匹配的目标方法
         *
         * @return {@link MethodDesc}
         */
        @Override
        public MethodDesc targetMethod() {
            return null;
        }

        /**
         * 这个方法用于重置advice
         */
        @Override
        public void clearContext() {

        }

        /**
         * 获取到结果
         *
         * @return "$back-data" for {@link RemoteCommand}
         */
        @Override
        public String result() {
            return "<done>";
        }

        /**
         * 某些情况下，你可能需要自己组装返回结果，那么调用这个方法可以获取到原始的
         * 方法链路信息，这样你就可以组装自己的返回结果
         * {@link MethodTraceFrame}
         *
         * @return 方法调用链路信息
         */
        @Override
        public List<MethodTraceFrame> traces() {
            return Collections.emptyList();
        }

        /**
         * 当某个方法被访问的时候，会首先通知该方法
         *
         * @param loader     加载的类加载器
         * @param className  方法类名
         * @param methodName 方法名称
         * @param methodDesc 方法描述
         * @param target     所属对象
         * @param args       方法参数
         */
        @Override
        public void onMethodEnter(int contextId, ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args) {
            PSLogger.error("方法进入:" + loader + "," + className + "," + methodName + "," + methodDesc + "," + target + "," + Arrays.toString(args) + "@CID:" + contextId);

        }

        /**
         * 当方法正常退出的时候，会通知这个方法
         *
         * @param returnVal 方法执行结果
         */
        @Override
        public void onMethodExit(int contextId, Object returnVal,  String className, String methodName, String methodDesc) {
            PSLogger.error("方法退出:" + returnVal + "@CID:" + contextId);
        }

        /**
         * 当方法异常退出的时候，会通知这个方法
         *
         * @param throwable 发送的异常
         */
        @Override
        public void onMethodThrowing(int contextId, Throwable throwable,  String className, String methodName, String methodDesc) {
            PSLogger.error("方法抛出异常:" + throwable + "@CID:" + contextId);
        }

        /**
         * 当访问方法的某一行的时候，会通知这个方法
         *
         * @param lineNo 访问的行号
         */
        @Override
        public void invokeLine(int contextId, int lineNo, Thread invokeThread) {
            PSLogger.error("访问行:" + lineNo + "@CID:" + contextId);
        }

        /**
         * 当出现操作变量的指令的时候，会通知这个方法，比如出现存储、加载变量的指令的时候，这个
         * 方法就会拿到具体的通知
         *
         * @param opcode   指令code
         * @param varIndex 操作的变量的index，可以去Local variable table里面取具体的lv
         */
        @Override
        public void invokeVarInstruction(int contextId, int opcode, int varIndex, LocalVariableTable localVariableTable) {
            PSLogger.error("变量指令:" + opcode + "," + varIndex + "@CID:" + contextId + " with name:" + localVariableTable.valueAt(varIndex));
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
            PSLogger.error("变量访问:" + opType + ":" + localVar + "=" + varVal);
        }

        /**
         * 当符合要求的时候，weave会将特殊的标志传输下来，观察者就可以拿到这个结果，这个方法
         * 不会始终被调用，这一点需要注意
         *
         * @param o 传输的对象
         */
        @Override
        public void invokeSpecialTransformWithAttach(Object o) {
            PSLogger.error("特殊对象传输:" + o);
        }

        /**
         * 如果你需要传递什么数据到观察者，那么使用需要使得这个方法返回true，如果返回false，那么
         * 就不会将数据传输到观察者那边去
         *
         * @param loader     方法的类加载器
         * @param className  方法所属的类名
         * @param methodName 方法名字
         * @param methodDesc 方法描述
         * @param target     目标对象
         * @param args       方法参数
         * @return 是否需要传递
         */
        @Override
        public boolean checkSpecialCondition(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args) {
            return false;
        }

        /**
         * 拿到需要传输的数据，然后传输下去
         *
         * @return 需要传输的对象, 可笑的是，你可以返回null
         */
        @Override
        public Object specialDataTransGet() {
            return null;
        }

    };

}
