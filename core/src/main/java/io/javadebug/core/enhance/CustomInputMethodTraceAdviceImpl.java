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

import io.javadebug.core.utils.JacksonUtils;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.transport.RemoteCommand;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.util.Arrays;

@Deprecated
public class CustomInputMethodTraceAdviceImpl extends AbstractMethodTraceCommandAdvice {

    // 这是原始输入，可能是shell，需要解析
    private String inputData;

    // 如果输入的不是脚本，那么就是真正的参数内容
    private Object[] inputParams;

    // 是否已经结束了
    private boolean isVisited = false;

    // the target object
    private Object target = null;

    public CustomInputMethodTraceAdviceImpl(int context, String className, String method, String desc,
                                            MethodTraceListener listener, boolean simpleOutMode,
                                            String inputData, Object[] inputParams,
                                            RemoteCommand remoteCommand) {
        super(context, className, method, desc, listener, simpleOutMode, remoteCommand);

        this.inputData = inputData;
        this.inputParams = inputParams;

        // 设置自定义参数
        targetMethodDesc.setCustomParamIn(inputData);
    }

    /**
     * 这个方法用来控制一次可返回状态的时候是否要结束观察，这个方法由各个子类控制即可，比如
     * onReturn类型的Advice，只需要看是否CD结束了（默认就是这种行为），但是比如onThrow这种
     * 类型的Advice就要看本次Record是否是Throw的，如果不是，那么需要再等下一轮。
     *
     * @param args 有时候需要通过参数来辨别是否需要结束一次观察
     * @return true/false
     */
    @Override
    protected boolean allowToEnd(Object[] args, Object retVal, Throwable throwable, String cls, String method, String desc) {

        // 放心，不要担心设置了之后拿不到，会有一个CD专门负责等待的，只有当CD符合要求的时候才会来请求
        // 这个方法判断是否可以结束了
        return isVisited || remoteCommand.needStop();
    }

    /**
     *  在某些情况下，你可能需要在等待结果前做一些事情，那么重写这个方法即可实现，这个方法
     *  将在你获取结果的时候执行；
     *
     */
    protected void preWaitResult(Object theTarget) {

        // 反射发起一次方法调用

        try {

            // get the target
            this.target = theTarget;

            // do invoke by reflect mode
            MethodUtils.invokeMethod(target, methodName, inputParams);

            // log it
            PSLogger.error("invoked method:" + className + "." + methodName + " with params:" + Arrays.toString(inputParams));
        } catch (Throwable e) {
            PSLogger.error("error occ while preWaitResult for " + className + "." + methodName
                                   + " with exception:" + e + " the target:" + target);

            // the detail
            //e.printStackTrace();

            // set the final result
            //remoteCommand.setResponseData("error occ while preWaitResult:\n" + UTILS.getErrorMsg(e) + "\n");

            // 不要让客户端卡死了
            //isVisited = true;
        }

    }

    /**
     * 该advice的type
     *
     * @return {@link MethodAdviceType}
     */
    @Override
    public MethodAdviceType type() {
        return MethodAdviceType.ON_MATCH_PARAM;
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

//        if (this.target != target) {
//            PSLogger.error("the target is difference.[" + target + "] - [" + this.target + "]");
//            return false; // ugly
//        }

        // 每次方法调用都会来访问这个方法，所以请在这个方法内部做一些比较有意义的事情，并且不要
        // 执行耗时计算或者等待，否则后果不堪设想
        if (inputParams == null) {
            remoteCommand.setErrorStatusWithErrorMessage("error input params");
            return false;
        }
        try {
            String toJson = JacksonUtils.serialize(args);
            if (UTILS.isNullOrEmpty(toJson)) {
                remoteCommand.setErrorStatusWithErrorMessage("could not serialize args:" + Arrays.toString(args));
                return false; // 无参方法不允许这样
            }
            if (UTILS.isNullOrEmpty(inputData)) {
                remoteCommand.setErrorStatusWithErrorMessage("error input data");
                return false; // 你想干嘛
            }
            if (!inputData.equals(toJson)) {
                remoteCommand.setErrorStatusWithErrorMessage("params not match");
                return false; // 有待优化
            }
            PSLogger.error("the params match for :" + className + "." + methodName + " with params:" + toJson);

            // done.
            this.isVisited = true;

            return true;
        } catch (Throwable e) {
            PSLogger.error("error occ while serialize args:" + Arrays.toString(args));
            remoteCommand.setErrorStatusWithErrorMessage("error:" + e);
            return false;
        }
    }


    /**
     *  在某些情况下，可能需要获取到一个区别各个client/advice的数据，比如uuid等，那
     *  这个方法就比较有用了
     *
     * @return 唯一数据
     */
    public String get() {
        return inputData;
    }

}
