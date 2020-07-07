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


package io.javadebug.agent;

import java.lang.reflect.Method;

/**
 * Created on 2019/5/11 11:02.
 *
 * @author HuJian
 */
public class WeaveSpy {

    // 目标jvm通过获取到这个classLoader来寻找agent内部的类信息
    public static ClassLoader AGENT_CLASS_LOADER;

    //------------------------------------------------------------
    //|   下面的Field定义了潜伏在目标JVM内部的Spy
    //------------------------------------------------------------

    public static Method ON_METHOD_ENTER_CALL;
    public static Method ON_METHOD_EXIT_CALL;
    public static Method ON_METHOD_THROW_EXCEPTION_CALL;
    public static Method ON_METHOD_INVOKE_LINE_CALL;
    public static Method ON_METHOD_INVOKE_VAR_INS_CALL;
    public static Method ON_METHOD_INVOKE_VAR_INS_V2_CALL;

    // ins invoke
    public static Method ON_METHOD_INVOKE_VAR_INS_I_CALL;
    public static Method ON_METHOD_INVOKE_VAR_INS_L_CALL;
    public static Method ON_METHOD_INVOKE_VAR_INS_F_CALL;
    public static Method ON_METHOD_INVOKE_VAR_INS_D_CALL;
    public static Method ON_METHOD_INVOKE_VAR_INS_A_CALL;

    // aux method area
    public static Method ON_METHOD_IN_SPECIAL_DATA_TRANS_CALL;
    public static Method ON_METHOD_IN_SPECIAL_CONDITION_JUDGE_CALL;
    public static Method ON_METHOD_IN_SPECIAL_CONDITION_TRANS_DATA_GET_CALL;

    // field notice
    public static Method ON_METHOD_FIELD_INVOKE_CALL;

    // thread block hound check method
    public static Method IS_IN_NON_BLOCKING_THREAD_METHOD;

    //------------------------------------------------------------

    /**
     *  初始化agent classLoader
     *
     * @param agentLoader {@link Agent#getAgentClassLoader()}
     */
    public static void initAgentClassLoader(ClassLoader agentLoader) {
        AGENT_CLASS_LOADER = agentLoader;
    }

    /**
     *  初始化Spy方法，应该将正确的目标Method安装起来
     *
     * @param ON_METHOD_ENTER 方法进入
     * @param ON_METHOD_EXIT 方法出口
     * @param ON_METHOD_THROW_EXCEPTION 抛出异常
     * @param ON_METHOD_INVOKE_LINE 访问某一行
     * @param ON_METHOD_INVOKE_VAR_INS 访问变量，主要关注存储、加载命令，这样可以获取到变量的最新值
     */
    public static void installAdviceMethod(Method ON_METHOD_ENTER,
                                           Method ON_METHOD_EXIT,
                                           Method ON_METHOD_THROW_EXCEPTION,
                                           Method ON_METHOD_INVOKE_LINE,
                                           Method ON_METHOD_INVOKE_VAR_INS,
                                           Method ON_METHOD_INVOKE_VAR_INS_V2,
                                           Method ON_METHOD_INVOKE_VAR_INS_I,
                                           Method ON_METHOD_INVOKE_VAR_INS_L,
                                           Method ON_METHOD_INVOKE_VAR_INS_F,
                                           Method ON_METHOD_INVOKE_VAR_INS_D,
                                           Method ON_METHOD_INVOKE_VAR_INS_A,
                                           Method ON_METHOD_IN_SPECIAL_DATA_TRANS,
                                           Method ON_METHOD_IN_SPECIAL_CONDITION_JUDGE,
                                           Method ON_METHOD_IN_SPECIAL_CONDITION_TRANS_DATA_GET,
                                           Method ON_METHOD_FIELD_INVOKE,
                                           Method IS_IN_NON_BLOCKING_THREAD_METHOD_INVOKE
                                           ) {

        ON_METHOD_ENTER_CALL = ON_METHOD_ENTER;
        ON_METHOD_EXIT_CALL = ON_METHOD_EXIT;
        ON_METHOD_THROW_EXCEPTION_CALL = ON_METHOD_THROW_EXCEPTION;
        ON_METHOD_INVOKE_LINE_CALL = ON_METHOD_INVOKE_LINE;
        ON_METHOD_INVOKE_VAR_INS_CALL = ON_METHOD_INVOKE_VAR_INS;
        ON_METHOD_INVOKE_VAR_INS_V2_CALL = ON_METHOD_INVOKE_VAR_INS_V2;

        ON_METHOD_INVOKE_VAR_INS_I_CALL = ON_METHOD_INVOKE_VAR_INS_I;
        ON_METHOD_INVOKE_VAR_INS_L_CALL = ON_METHOD_INVOKE_VAR_INS_L;
        ON_METHOD_INVOKE_VAR_INS_F_CALL = ON_METHOD_INVOKE_VAR_INS_F;
        ON_METHOD_INVOKE_VAR_INS_D_CALL = ON_METHOD_INVOKE_VAR_INS_D;
        ON_METHOD_INVOKE_VAR_INS_A_CALL = ON_METHOD_INVOKE_VAR_INS_A;

        ON_METHOD_IN_SPECIAL_DATA_TRANS_CALL = ON_METHOD_IN_SPECIAL_DATA_TRANS;
        ON_METHOD_IN_SPECIAL_CONDITION_JUDGE_CALL = ON_METHOD_IN_SPECIAL_CONDITION_JUDGE;
        ON_METHOD_IN_SPECIAL_CONDITION_TRANS_DATA_GET_CALL = ON_METHOD_IN_SPECIAL_CONDITION_TRANS_DATA_GET;

        ON_METHOD_FIELD_INVOKE_CALL = ON_METHOD_FIELD_INVOKE;

        IS_IN_NON_BLOCKING_THREAD_METHOD = IS_IN_NON_BLOCKING_THREAD_METHOD_INVOKE;
    }

    /**
     *  check the block call.
     *
     * @param nonBlockingThreadNamePattern match the non-blocking thread name
     */
    public static void checkBlock(String nonBlockingThreadNamePattern) {
        if (nonBlockingThreadNamePattern == null || nonBlockingThreadNamePattern.isEmpty()) {
            return; // ignore check
        }
        boolean blockCheckPass = true;
        String[] threadNames = nonBlockingThreadNamePattern.split(",");
        for (String tn : threadNames) {
            // thread name check
            if (Thread.currentThread().getName().contains(tn)) {
                blockCheckPass = false;
                break;
            }
        }

        String blockingCall = "";

        // thread stack check
        if (!blockCheckPass) {
            // check the stack
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace.length > 2) {
                blockingCall = stackTrace[2].toString();
            }
            for (int i = 0; i < 20 && i < stackTrace.length; i ++) {
                StackTraceElement ste = stackTrace[i];
                String ignore = ",";
                switch (ste.getClassName()) {
                    case "java.util.concurrent.ScheduledThreadPoolExecutor":
                    {
                        ignore = "scheduleAtFixedRate,submit,";
                        break;
                    }
                    case "java.lang.ClassLoader":
                    {
                        ignore = "loadClass";
                        break;
                    }
                    case "java.net.URLClassLoader":
                    {
                        ignore = "findClass,defineClass,";
                        break;
                    }
                    case "java.util.concurrent.ThreadPoolExecutor":
                    {
                        ignore = "runWorker,";
                        break;
                    }
                    case "java.util.UUID":
                    {
                        ignore = "randomUUID,";
                        break;
                    }
                    case "java.util.concurrent.AbstractExecutorService":
                    {
                        ignore = "submit,";
                        break;
                    }
                    case "java.util.concurrent.ArrayBlockingQueue":
                    case "java.util.concurrent.LinkedBlockingDeque":
                    case "java.util.concurrent.DelayQueue":
                    case "java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue":
                    {
                        ignore = "offer,add,put,take,poll,remove";
                        break;
                    }
                }

                // check
                if (ignore.contains(ste.getMethodName())) {
                    return; // ignore this blocking call
                }
            }
        }

        if (!blockCheckPass) {
//            throw new IllegalStateException("[BlockHound] thread are blocking, which is not supported in thread :"
//                                                    + Thread.currentThread().getName());

            // just print the warn message
            System.err.println("[BlockHound] thread are blocking, which is not supported in thread :"
                                       + Thread.currentThread().getName() + "\n         The blocking call is:[" + blockingCall + "]");
            // print the stack
            new RuntimeException().printStackTrace();
        }
    }

}
