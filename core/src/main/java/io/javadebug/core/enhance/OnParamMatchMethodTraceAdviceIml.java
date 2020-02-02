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
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.transport.RemoteCommand;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class OnParamMatchMethodTraceAdviceIml extends AbstractMethodTraceCommandAdvice {

    // the evaluation context
    private EvaluationContext evaluationContext;

    // the expression parser
    private ExpressionParser expressionParser;

    // the expression
    private String expression;

    // whether to end
    private boolean isVisited = false;

    // flag
    private boolean necessary = false;

    // the params var name.
    private List<String> paramsNameList;

    public OnParamMatchMethodTraceAdviceIml(int context, String className,
                                            String method, String desc,
                                            MethodTraceListener listener,
                                            boolean simpleOutMode,
                                            String expression, Method targetMethod,
                                            RemoteCommand remoteCommand) {
        super(context, className, method, desc, listener, simpleOutMode, remoteCommand);

        this.expression = expression;
        if (!UTILS.isNullOrEmpty(this.expression) && targetMethod != null) {
            necessary = true;
            evaluationContext = new StandardEvaluationContext();
            expressionParser = new SpelExpressionParser();

            // get the var name
            Parameter[] parameters = targetMethod.getParameters();

            if (parameters == null || parameters.length == 0) {
                necessary = false;
            } else {
                paramsNameList = new ArrayList<>();
                for (Parameter parameter : parameters) {
                    paramsNameList.add(parameter.getName());
                }
                //report it.
                PSLogger.error("method:" + targetMethod.getName() + "'s params =>" + paramsNameList);
            }
        }

    }

    /**
     * 这个方法用来控制一次可返回状态的时候是否要结束观察，这个方法由各个子类控制即可，比如
     * onReturn类型的Advice，只需要看是否CD结束了（默认就是这种行为），但是比如onThrow这种
     * 类型的Advice就要看本次Record是否是Throw的，如果不是，那么需要再等下一轮。
     *
     * @param args      有时候需要通过参数来辨别是否需要结束一次观察
     * @param retVal    如果方法正常结束，则方法返回值
     * @param throwable 如果方法抛出异常，则抛出的异常是什么
     * @return true/false
     */
    @Override
    protected boolean allowToEnd(Object[] args, Object retVal, Throwable throwable, String cls, String method, String desc) {
        return (!necessary || isVisited || remoteCommand.needStop());
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
        // check own
        if (!checkOwn(className, methodName, methodDesc)) {
            return false;
        }

        // if necessary
        if (!necessary) {
            return false;
        }

        // set the context
        if (args == null || args.length == 0) {
            necessary = false;
            return false;
        }

        int pc = 0;
        for (Object o : args) {
            evaluationContext.setVariable("p" + pc, o);
            evaluationContext.setVariable(paramsNameList.get(pc), o);
            pc ++;
        }

        // check
        try {
            Expression expressionResult  = expressionParser.parseExpression(expression);

            // evaluation the expression
            isVisited =  expressionResult.getValue(evaluationContext, boolean.class);

            return isVisited;
        } catch (Throwable e) {
            necessary = false; // do not do again.
            PSLogger.error("error occ while evaluation spring expression:" + expression + ":" + e);
            remoteCommand.setErrorStatusWithErrorMessage("error occ:" + e);
        }

        return false;
    }

}
