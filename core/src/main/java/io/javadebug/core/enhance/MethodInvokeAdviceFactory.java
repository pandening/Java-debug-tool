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
import io.javadebug.core.data.LRModel;
import io.javadebug.core.utils.ObjectUtils;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.ServerHook;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.claw.ObjectFieldInterpreter;
import io.javadebug.core.handler.StopAbleRunnable;
import io.javadebug.core.transport.RemoteCommand;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static io.javadebug.core.handler.CommandHandler.checkTimeoutMe;

public class MethodInvokeAdviceFactory {

    /**
     *  根据不同的type创建不同的advice
     *
     * @param type 仅能从 "return"\"throw"\"custom"\"watch"\"full" 中取，如果为null或者
     *             空，则默认和"full"的效果一样，如果输入的字符串不在上面的几个中，也将变为"full"
     *
     * @param remoteCommand  交互协议，持有这个对象可以做很多事情，从此上下文信息交互靠这个对象即可
     *
     * @param cls 需要匹配的目标类
     *
     * @param method 需要观察的目标方法名字
     *
     * @param desc 需要观察的目标方法描述
     *
     * @return {@link MethodInvokeAdvice}
     */
    @SuppressWarnings("unchecked")
    public static MethodInvokeAdvice createAdvice(String type, RemoteCommand remoteCommand,
                                                  String cls, String method, String desc, ServerHook serverHook,
                                                  Class<?> clazz, Instrumentation instrumentation) {
        if (UTILS.isNullOrEmpty(type)) {
            return new OnMethodInvokeAdvice(remoteCommand, cls, method, desc, clazz);
        }

        // the method desc
        MethodDesc methodDesc = new MethodDesc(cls, method, desc, clazz);

        // the te option
        String te = remoteCommand.getParam("$forward-common-te");

        switch (type) {
            case "return": {
                int tl = checkTargetLine(remoteCommand, methodDesc, serverHook, instrumentation);
                return new OnReturnAdvice(remoteCommand, cls, method, desc, clazz, tl, te);
            }
            case "throw": {
                int tl = checkTargetLine(remoteCommand, methodDesc, serverHook, instrumentation);
                String targetThrowClass = remoteCommand.getParam("$forward-trace-option-e");
                return new OnThrowAdvice(remoteCommand, cls, method, desc, targetThrowClass, clazz, tl, te);
            }
            case "record": {
                String optionU = remoteCommand.getParam("$forward-trace-option-u");
                int targetOrder = UTILS.safeParseInt(optionU, -1);
                if (!UTILS.isNullOrEmpty(optionU) && targetOrder != -1) {
                    List<MethodTraceFrame> methodTraceFrames = serverHook.queryTraceByOrderId(cls, method, desc, targetOrder);
                    if (methodTraceFrames != null && !methodTraceFrames.isEmpty()) {
                        remoteCommand.addCustomParam("$command-trace-result", methodTraceFrames);
                        return null;
                    }
                }

                String recordTimeStr = remoteCommand.getParam("$forward-trace-option-n");
                int recordCnt = UTILS.safeParseInt(recordTimeStr, 0);
                String timeoutStr = remoteCommand.getParam("$forward-trace-option-time");
                int timeout = UTILS.safeParseInt(timeoutStr, -1);

                return new RecordAdvice(remoteCommand, cls, method, desc, recordCnt, timeout, serverHook, clazz);
            }
            case "custom": {
                // 分两种情况，如果-u参数设置了，并且是合法的，那么优先从record中取值然后输入
                // 否则-i参数必须存在，否则抛出异常

                Object[] customParam = null;
                String iOptionVal;

                String optionU = remoteCommand.getParam("$forward-trace-option-u");
                int targetOrder = UTILS.safeParseInt(optionU, -1);
                if (targetOrder >= 0) {
                    // -u 参数起作用了
                    List<MethodTraceFrame> methodTraceFrames = serverHook.queryTraceByOrderId(cls, method, desc, targetOrder);
                    if (methodTraceFrames != null && !methodTraceFrames.isEmpty()) {
                        for (MethodTraceFrame traceFrame : methodTraceFrames) {
                            if (traceFrame.isMethodEnter()) {
                                customParam = traceFrame.getParams();
                                PSLogger.error("get the params:" + Arrays.toString(customParam));
                                break;
                            }
                        }
                    }
                }

                // claw script
                String clawScript = remoteCommand.getParam("$forward-trace-option-clawScript");
                if (!UTILS.isNullOrEmpty(clawScript)) {

                    // the target method
                    Method targetMethod = remoteCommand.getParam("$forward-trace-tmp-targetMethod");

                    // if -u not set
                    if (targetOrder < 0) {
                        targetOrder = 0;
                    }
                    List<MethodTraceFrame> methodTraceFrames = serverHook.queryTraceByOrderId(cls, method, desc, targetOrder);
                    if (methodTraceFrames != null && !methodTraceFrames.isEmpty()) {
                        for (MethodTraceFrame traceFrame : methodTraceFrames) {
                            if (traceFrame.isMethodEnter()) {
                                Object[] originParams = traceFrame.getParams();
                                if (originParams != null) {
                                    customParam = new Object[targetMethod.getParameterCount()];
                                    System.arraycopy(originParams, 0, customParam, 0, targetMethod.getParameterCount());
                                }
                                PSLogger.error("get the params:" + Arrays.toString(customParam));
                                break;
                            }
                        }
                    }

                    // execute the script
                    if (customParam != null) {
                        customParam = ObjectFieldInterpreter.interpreter(customParam, clawScript, targetMethod);
                    }
                }

                // 是否已经满足要求，不能对无参数的方法进行观察
                if (customParam == null) {
                    // -i 参数必填
                    iOptionVal = remoteCommand.getParam("$forward-trace-option-i");
                    if (UTILS.isNullOrEmpty(iOptionVal)) {
                        throw new IllegalStateException("please offer the params.");
                    }

                    // get the param from -i (transform to object[])
                    customParam = JacksonUtils.deserialize(iOptionVal, Object[].class);

                    PSLogger.error(String.format("deserialize the custom param:[%s] => [%s]",
                            iOptionVal, Arrays.toString(customParam)));
                } else {
                    iOptionVal = JacksonUtils.serialize(customParam);
                }

                PSLogger.error("jsonParams:" + iOptionVal);

                // 至此，如果还是null，那么直接抛出异常
                if (customParam == null) {
                    throw new IllegalStateException("请提供方法输入参数 -u / -i，如果是无参方法，请使用其他type进行观察");
                }

                // get the expression
                String eOptionVal = remoteCommand.getParam("$forward-trace-option-e");

                // 对于这种需要主动触发某种动作的advice，需要在register之后做一个触发动作
                return new OnCustomParamInMatchAdvice(remoteCommand, cls, method, desc, customParam, eOptionVal, clazz);
            }
            case "watch": {
                // target line number
                int tl = checkTargetLine(remoteCommand, methodDesc, serverHook, instrumentation);
                // get the expression
                String iOptionVal = remoteCommand.getParam("$forward-trace-option-i");
                if (UTILS.isNullOrEmpty(iOptionVal)) {
                    throw new IllegalArgumentException("you must offer a spring expression.");
                }

                // get the target method
                Method targetMethod = remoteCommand.getParam("$forward-trace-tmp-targetMethod");

                if (targetMethod == null) {
                    throw new IllegalArgumentException("you must attach the target method to command protocol.");
                }

                return new OnMatchParamAdvice(remoteCommand, cls, method, desc, iOptionVal, targetMethod, false, clazz, tl, te);
            }
            case "line": {
                String targetLineOption = remoteCommand.getParam("$forward-trace-option-i");
                //assert
                if (UTILS.isNullOrEmpty(targetLineOption)) {
                    throw new IllegalStateException("impossible empty target line number, please provide the target line no by 'i' option");
                }

                // simple check
                int targetLineNo = UTILS.safeParseInt(targetLineOption, -1);
                if (targetLineNo < 0) {
                    throw new IllegalStateException("impossible negative target line number : " + targetLineNo);
                }

                // invalid check
                checkTargetLine(targetLineNo, methodDesc, serverHook, instrumentation);

                // the advice
                return new OnTargetLineBeInvokedAdvice(remoteCommand, cls, method, desc, clazz, targetLineNo, te);
            }

        }

        // 如果都没有命中
        return new OnMethodInvokeAdvice(remoteCommand, cls, method, desc, clazz);
    }

    /**
     *  ref : {@link MethodInvokeAdviceFactory#checkTargetLine(int, MethodDesc, ServerHook, Instrumentation)}
     *
     *   the target line number extract from remote_command : "$forward-common-tl";
     *
     * @param remoteCommand {@link RemoteCommand}
     * @param methodDesc {@link MethodDesc}
     * @param serverHook {@link ServerHook}
     * @param instrumentation {@link Instrumentation}
     */
    private static int checkTargetLine(RemoteCommand remoteCommand, MethodDesc methodDesc,
                                        ServerHook serverHook, Instrumentation instrumentation) {
        String tlOption = remoteCommand.getParam("$forward-common-tl");
        if (UTILS.isNullOrEmpty(tlOption)) {
            return -1;
        }

        int targetLine = UTILS.safeParseInt(tlOption, -1);
        if (targetLine < 0) {
            return targetLine;
        }

        // real check
        checkTargetLine(targetLine, methodDesc, serverHook, instrumentation);

        // success
        return targetLine;
    }

    /**
     *  check the whether the line number is invalid
     *
     *  the target line number must in range [l, r]
     *
     *  the [l,r] range => {@link MethodLineRangeWeaver} {@link MethodLineRangeEnhancer}
     *
     * @param instrumentation the instrumentation
     * @param targetLineNo the set line number
     * @param methodDesc the target method desc
     * @param serverHook the server hook
     */
    private static void checkTargetLine(int targetLineNo, MethodDesc methodDesc,
                                        ServerHook serverHook, Instrumentation instrumentation) {
        // gen the cache key
        String key = "lr@" + methodDesc.getClassName() +"." + methodDesc.getName() + "#" + methodDesc.getDesc();
        LRModel lrModel = serverHook.getLRModel(key);

        // get the method range and cache it !
        if (lrModel == null) {

            byte[] preEnhanceBytes = serverHook.lastBytesForClass(methodDesc.getClassName());
            MethodLineRangeEnhancer methodLineRangeEnhancer = new MethodLineRangeEnhancer(methodDesc, preEnhanceBytes);

            try {
                instrumentation.addTransformer(methodLineRangeEnhancer, true);
                instrumentation.retransformClasses(methodDesc.getTargetClass());
            } catch (Exception e) {
                PSLogger.error("could not do retransformClasses for : " + methodDesc + "\n" + e);
            } finally {
                instrumentation.removeTransformer(methodLineRangeEnhancer);
            }

            if (methodLineRangeEnhancer.getLeftRange() >= 0 && methodLineRangeEnhancer.getRightRange() >= 0) {
                lrModel = new LRModel(methodLineRangeEnhancer.getLeftRange(),methodLineRangeEnhancer.getRightRange());

                // cache it !
                serverHook.cacheLRModel(key, lrModel);
            } else {
                PSLogger.error("invalid lr : [" + methodLineRangeEnhancer.getLeftRange() +"," + methodLineRangeEnhancer.getRightRange() + "]");
            }

        }

        if (lrModel == null) {
            throw new IllegalStateException("could not get the method range of method : " + methodDesc);
        }

        // check
        if (targetLineNo < lrModel.getL() || targetLineNo > lrModel.getR()) {
            throw new IllegalArgumentException("out of method line range, except " + lrModel + ", actual :" + targetLineNo);
        }

    }

    static class SatisfyPuppet {

        // the evaluation context
        private ThreadLocal<EvaluationContext> threadLocalEvaluationContext = new ThreadLocal<>();

        // the expression parser
        private ExpressionParser expressionParser = null;

        private Expression expressionResult = null;

        SatisfyPuppet(String expression) {
            if (!UTILS.isNullOrEmpty(expression)) {
                try {
                    this.expressionParser = new SpelExpressionParser();
                    this.expressionResult = expressionParser.parseExpression(expression);
                } catch (Exception e) {
                    PSLogger.error("invalid spring expression : " + expression, e);
                }
            }
        }

        private boolean isValid() {
            return expressionResult != null;
        }

        public boolean match(Object[] params) {
            if (!isValid()) {
                return true; // do not filter
            }

            initTheEvaluationContext(params);

            try {
                 return expressionResult.getValue(getContext(), boolean.class);
            } catch (Exception e) {
                PSLogger.error("error when getVal:" + ObjectUtils.printObjectToString(params), e);
                return true; // do not filter
            }
        }

        /**
         *  获取当前线程的context
         *
         * @return {@link StandardEvaluationContext}
         */
        private EvaluationContext getContext() {
            EvaluationContext evaluationContext = threadLocalEvaluationContext.get();
            if (evaluationContext == null) {
                evaluationContext = new StandardEvaluationContext();
                threadLocalEvaluationContext.set(evaluationContext);
            }
            return evaluationContext;
        }

        /**
         *  init the context
         *
         * @param args the args
         */
        private void initTheEvaluationContext(Object[] args) {
            EvaluationContext context = getContext();
            int pc = 0;
            for (Object o : args) {
                context.setVariable("p" + pc, o);
                pc ++;
            }
        }

    }


    static class OnMethodInvokeAdvice extends BaseMethodInvokeAdvice {

        OnMethodInvokeAdvice(RemoteCommand remoteCommand, String cls, String method, String desc, Class<?> clazz) {
            super(remoteCommand, cls, method, desc, clazz);
        }

        /**
         * advice的类型，这个很有用
         *
         * @return {@link MethodAdviceType}
         */
        @Override
        public MethodAdviceType type() {
            return MethodAdviceType.FULL_MATCH;
        }
    }

    static class OnReturnAdvice extends BaseMethodInvokeAdvice {

        // the target line the debug-er want to watch
        private int targetLineNo;

        // whether the target has been invoked
        private volatile boolean targetLineBeInvoked = false;

        private SatisfyPuppet satisfyPuppet = null;

        OnReturnAdvice(RemoteCommand remoteCommand, String cls, String method, String desc,
                       Class<?> clazz, int targetLineNo, String te) {
            super(remoteCommand, cls, method, desc, clazz);

            this.targetLineNo = targetLineNo;
            if (targetLineNo < 0) {
                targetLineBeInvoked = true;
            }

            if (!UTILS.isNullOrEmpty(te)) {
                this.satisfyPuppet = new SatisfyPuppet(te);
            }

        }

        /**
         * advice的类型，这个很有用
         *
         * @return {@link MethodAdviceType}
         */
        @Override
        public MethodAdviceType type() {
            return MethodAdviceType.ON_RETURN;
        }

        /**
         * 用于判断是否满足要求的方法，这个方法将在原始方法return或者throw之后调用，所以会携带的信息
         * 很多，你可以根据自己的需求判断是否可以结束本次advice（unregister），比如需要实现onReturn
         *  那么就只需要判断returnVal != null && throwable == null 即可，而实现onThrow则只需要判断throwable != null
         *  即可；
         * 本方法主要是给custom和watch用的，return/throw/record判断较为简单，并且每次调用该方法应该都会返回true，因为
         * weave内部就是在满足要求的时候才会来调用advice的该方法，比如对于throw类型{@code type}的advice，那么只会在
         * 方法抛出异常退出只会才会被回调，但是对于custom和watch来说，还是需要做一些计算的；
         *
         * @param loader    方法所属的类的类加载器
         * @param cls       方法所属的类
         * @param method    方法名称
         * @param desc      方法描述
         * @param targetObj 目标对象
         * @param args      本次请求参数信息
         * @param throwable 抛出的异常（如果正常结束则为null）
         * @param returnVal 方法返回结果，如果方法的签名为void，那么也为null，所以判断方法正常结束需要
         *                  结合throwable参数判断
         * @return 本次trace结果是否匹配你的需求，如果不满足，那么weave会继续为你监控，否则会将本次方法调用信息
         * 通知给你，然后顺便把该advice unregister调，避免再次被其他方法线程调用而造成链路结果混乱
         */
        @Override
        public boolean match(ClassLoader loader, String cls, String method, String desc, Object targetObj, Object[] args, Throwable throwable, Object returnVal) {
            // 是否超时
            return remoteCommand.needStop() || throwable == null;
        }

        @Override
        public void invoke(List<MethodTraceFrame> frames) {
            if (remoteCommand.needStop()) {
                return; // 命令超时了
            }
            this.frames = frames;

            // ignore
            if (targetLineBeInvoked) {
                return;
            }

            // check
            for (MethodTraceFrame methodTraceFrame : frames) {
                if (methodTraceFrame != null &&
                            methodTraceFrame.getLineNo() >= 0
                            && methodTraceFrame.getLineNo() == targetLineNo) {
                    targetLineBeInvoked = true;
                    // check [te] option
                    if (this.satisfyPuppet != null) {
                        Object[] objects = new Object[methodTraceFrame.getLocalVariable().size()];
                        for (int i = 0; i < methodTraceFrame.getLocalVariable().size(); i ++) {
                            objects[i] = methodTraceFrame.getLocalVariable().get(i).getVar();
                        }
                        // check
                        targetLineBeInvoked = this.satisfyPuppet.match(objects);
                    }
                }
            }

        }


        @Override
        public boolean needToUnReg(String cls, String method, String desc) {
            if (targetLineBeInvoked) {
                adviceDoneLatch.countDown();
                return false;
            }
            return true;
        }
    }

    static class OnThrowAdvice extends BaseMethodInvokeAdvice {

        // the target line the debug-er want to watch
        private int targetLineNo;

        // whether the target has been invoked
        private volatile boolean targetLineBeInvoked = false;

        // 目标异常类
        private Class<? extends Throwable> eClass;

        private SatisfyPuppet satisfyPuppet = null;

        @SuppressWarnings("unchecked")
        OnThrowAdvice(RemoteCommand remoteCommand, String cls, String method, String desc, String exception,
                      Class<?> clazz, int targetLineNo, String te) {
            super(remoteCommand, cls, method, desc, clazz);

            // 获取到目标异常类
            if (!UTILS.isNullOrEmpty(exception)) {
                // using the current classloader is ok.
                try {
                    eClass = (Class<? extends Throwable>) Thread.currentThread().getContextClassLoader().loadClass(exception);
                } catch (Throwable e) {
                    PSLogger.error("could not load target throwable class:" + exception + ":" + e);
                }
            }

            this.targetLineNo = targetLineNo;
            if (targetLineNo < 0) {
                this.targetLineBeInvoked = true;
            }

            if (!UTILS.isNullOrEmpty(te)) {
                this.satisfyPuppet = new SatisfyPuppet(te);
            }

        }

        @Override
        public void invoke(List<MethodTraceFrame> frames) {
            if (remoteCommand.needStop()) {
                return; // 命令超时了
            }
            this.frames = frames;

            // ignore
            if (targetLineBeInvoked) {
                return;
            }

            // check
            for (MethodTraceFrame methodTraceFrame : frames) {
                if (methodTraceFrame != null &&
                            methodTraceFrame.getLineNo() >= 0
                            && methodTraceFrame.getLineNo() == targetLineNo) {
                    targetLineBeInvoked = true;
                    // check [te] option
                    if (this.satisfyPuppet != null) {
                        Object[] objects = new Object[methodTraceFrame.getLocalVariable().size()];
                        for (int i = 0; i < methodTraceFrame.getLocalVariable().size(); i ++) {
                            objects[i] = methodTraceFrame.getLocalVariable().get(i).getVar();
                        }
                        // check
                        targetLineBeInvoked = this.satisfyPuppet.match(objects);
                    }
                }
            }
        }

        @Override
        public boolean needToUnReg(String cls, String method, String desc) {
            if (targetLineBeInvoked) {
                adviceDoneLatch.countDown();
                return false;
            }
            return true;
        }

        /**
         * advice的类型，这个很有用
         *
         * @return {@link MethodAdviceType}
         */
        @Override
        public MethodAdviceType type() {
            return MethodAdviceType.ON_THROW;
        }

        /**
         * 用于判断是否满足要求的方法，这个方法将在原始方法return或者throw之后调用，所以会携带的信息
         * 很多，你可以根据自己的需求判断是否可以结束本次advice（unregister），比如需要实现onReturn
         *  那么就只需要判断returnVal != null && throwable == null 即可，而实现onThrow则只需要判断throwable != null
         *  即可；
         * 本方法主要是给custom和watch用的，return/throw/record判断较为简单，并且每次调用该方法应该都会返回true，因为
         * weave内部就是在满足要求的时候才会来调用advice的该方法，比如对于throw类型{@code type}的advice，那么只会在
         * 方法抛出异常退出只会才会被回调，但是对于custom和watch来说，还是需要做一些计算的；
         *
         * @param loader    方法所属的类的类加载器
         * @param cls       方法所属的类
         * @param method    方法名称
         * @param desc      方法描述
         * @param targetObj 目标对象
         * @param args      本次请求参数信息
         * @param throwable 抛出的异常（如果正常结束则为null）
         * @param returnVal 方法返回结果，如果方法的签名为void，那么也为null，所以判断方法正常结束需要
         *                  结合throwable参数判断
         * @return 本次trace结果是否匹配你的需求，如果不满足，那么weave会继续为你监控，否则会将本次方法调用信息
         * 通知给你，然后顺便把该advice unregister调，避免再次被其他方法线程调用而造成链路结果混乱
         */
        @Override
        public boolean match(ClassLoader loader, String cls, String method, String desc, Object targetObj, Object[] args, Throwable throwable, Object returnVal) {
            // 是否超时
            return remoteCommand.needStop() || throwable != null && (eClass == null || eClass.getName().equals(throwable.getClass().getName()));
        }
    }

    static class OnCustomParamInMatchAdvice extends BaseMethodInvokeAdvice {

        // 目标对象，如果是静态方法则为null
        private volatile Object target;

        // 需要执行的参数列表
        private Object[] args;

        // 目标方法
        private Method invokeMethod;

        // 用于匹配目标方法参数的advice
        private MethodInvokeAdvice methodInvokeAdvice;

        // 用于匹配参数的表达式
        private String matchExpression;

        // 只发起一次
        private volatile boolean invoked = false;

        // target被赋值即可
        private CountDownLatch waitTargetLatch;

        // 是否结束的latch
        private CountDownLatch isDoneLatch;

        OnCustomParamInMatchAdvice(RemoteCommand remoteCommand, String cls, String method, String desc, Object[] args, String expression, Class<?> clazz) {
            super(remoteCommand, cls, method, desc, clazz);

            this.args = args;

            // get the method
            invokeMethod = remoteCommand.getParam("$forward-trace-tmp-targetMethod");

            this.waitTargetLatch = new CountDownLatch(1);

            this.isDoneLatch = new CountDownLatch(1);

            this.matchExpression = expression;

            if (!UTILS.isNullOrEmpty(matchExpression)) {
                methodInvokeAdvice = new OnMatchParamAdvice(remoteCommand, cls, method, desc, expression, invokeMethod, true, clazz, -1, null);
            }

        }

        /**
         * 用于判断是否满足要求的方法，这个方法将在原始方法return或者throw之后调用，所以会携带的信息
         * 很多，你可以根据自己的需求判断是否可以结束本次advice（unregister），比如需要实现onReturn
         *  那么就只需要判断returnVal != null && throwable == null 即可，而实现onThrow则只需要判断throwable != null
         *  即可；
         * 本方法主要是给custom和watch用的，return/throw/record判断较为简单，并且每次调用该方法应该都会返回true，因为
         * weave内部就是在满足要求的时候才会来调用advice的该方法，比如对于throw类型{@code type}的advice，那么只会在
         * 方法抛出异常退出只会才会被回调，但是对于custom和watch来说，还是需要做一些计算的；
         *
         * @param loader    方法所属的类的类加载器
         * @param cls       方法所属的类
         * @param method    方法名称
         * @param desc      方法描述
         * @param targetObj 目标对象
         * @param args      本次请求参数信息
         * @param throwable 抛出的异常（如果正常结束则为null）
         * @param returnVal 方法返回结果，如果方法的签名为void，那么也为null，所以判断方法正常结束需要
         *                  结合throwable参数判断
         * @return 本次trace结果是否匹配你的需求，如果不满足，那么weave会继续为你监控，否则会将本次方法调用信息
         * 通知给你，然后顺便把该advice unregister调，避免再次被其他方法线程调用而造成链路结果混乱
         */
        @Override
        public boolean match(ClassLoader loader, String cls, String method, String desc, Object targetObj, Object[] args, Throwable throwable, Object returnVal) {
            // 是否超时
            if (remoteCommand.needStop()) {
                return true;
            }

            // 执行到这里，target必须不为null，要么是目标方法是static的，否则告诉client请先获取到目标
            // 对象，比如使用mt命令别指定t参数

            if (targetObj != null) {

                if (this.target == null) {
                    this.target = targetObj;
                    waitTargetLatch.countDown();
                    return false; // 接着下一次
                }

            }

            return true;
        }

        @Override
        public boolean check(ClassLoader loader, String cls, String method, String desc, Object targetObj, Object[] args) {
            this.target = targetObj;
            waitTargetLatch.countDown();
            if (methodInvokeAdvice != null) {
                return methodInvokeAdvice.check(loader, cls, method, desc, targetObj, args);
            } else {
                return super.check(loader, cls, method, desc, targetObj, args);
            }
        }

        /**
         * advice的类型，这个很有用
         *
         * @return {@link MethodAdviceType}
         */
        @Override
        public MethodAdviceType type() {
            return MethodAdviceType.ON_MATCH_PARAM;
        }

        @Override
        public List<MethodTraceFrame> traces() {
            try {
                long checkTimeoutVal = checkTimeoutMe(remoteCommand);
                if (checkTimeoutVal <= 0) {
                    checkTimeoutVal = 10; // 100 seconds timeout
                }
                isDoneLatch.await(checkTimeoutVal, TimeUnit.SECONDS);
            } catch (Throwable e) {
                remoteCommand.setResponseData("等待执行命令出现错误，可能命令执行超时了\n");
            }
            if (methodInvokeAdvice == null) {
                return Collections.emptyList();
            }
            return methodInvokeAdvice.traces();
        }

        /**
         *  在注册了advice之后，需要做的一些task
         *
         * @return {@link StopAbleRunnable}
         */
        public List<StopAbleRunnable> tasks() {
            if (invoked) {
                return Collections.emptyList();
            }
            // 设置这个标志下次进来就拿不到任务了
            invoked = true;

            List<StopAbleRunnable> tasks = new ArrayList<>();

            tasks.add(new StopAbleRunnable(remoteCommand) {
                @Override
                public void execute() {

                    //在这里进行方法调用
                    try {
                        if (invokeMethod == null) {
                            throw new NullPointerException("the invoke method is null");
                        }

                        // 如果方法不是静态的，那么target不能为空
                        if (target == null) {
                            if (!Modifier.isStatic(invokeMethod.getModifiers())) {
                                // 等待target被赋值
                                try {
                                    long checkTimeoutVal = checkTimeoutMe(remoteCommand);
                                    if (checkTimeoutVal <= 0) {
                                        checkTimeoutVal = 10; // 100 seconds timeout
                                    }
                                    waitTargetLatch.await(checkTimeoutVal, TimeUnit.SECONDS);
                                } catch (Throwable e) {
                                    remoteCommand.setResponseData("命令等待target对象超时\n");
                                }
                                // 再次判断
                                if (target == null) {
                                    PSLogger.error("sorry, current-fail for could not get the target object.");
                                    remoteCommand.setResponseData("无法获取到目标对象\n");
                                    return;
                                }
                            }
                        }

                        // 这是用来观察本次输入参数的advice，如果没有提供spring expression，那么默认
                        // 只要将请求发出去了就完事了，否则会安装advice并且进行主动观察这次输入的执行路径
                        ClassMethodWeaver.regAdvice(remoteCommand.getContextId() * -1, methodInvokeAdvice);

                        // do invoke
                        Object o = invokeMethod.invoke(target, args);

                        if (methodInvokeAdvice == null) {
                            remoteCommand.setResponseData("成功发送请求,获取到返回结果:" + o + "\n");
                        }
                    } catch (Throwable e) {
                        // ignore the invoke exception
                        if (e instanceof InvocationTargetException ) {
                            if (methodInvokeAdvice == null) {
                                remoteCommand.setResponseData("调用抛出异常:" + ((InvocationTargetException) e).getTargetException());
                            }
                        } else {
                            remoteCommand.setResponseData("无法调用目标方法:" + e);
                        }
                    } finally {
                        isDoneLatch.countDown();
                    }
                }
            });

            return tasks;
        }

    }

    static class OnMatchParamAdvice extends BaseMethodInvokeAdvice {

        // the target line the debug-er want to watch
        private int targetLineNo;

        // whether the target has been invoked
        private volatile boolean targetLineBeInvoked = false;

        // the evaluation context
        private ThreadLocal<EvaluationContext> threadLocalEvaluationContext = new ThreadLocal<>();

        // the expression parser
        private ExpressionParser expressionParser;

        private Expression expressionResult;

        // the expression
        private String expression;

        // flag
        private boolean necessary = false;

        // don't check
        private boolean notCheckMode = false;

        private SatisfyPuppet satisfyPuppet = null;

        OnMatchParamAdvice(RemoteCommand remoteCommand, String cls, String method, String desc,
                           String expression, Method targetMethod, boolean fakeTag,
                           Class<?> clazz, int targetLineNo, String te) {
            super(remoteCommand, cls, method, desc, clazz);

            this.expression = expression;
            if (!UTILS.isNullOrEmpty(this.expression) && targetMethod != null) {
                necessary = true;
                expressionParser = new SpelExpressionParser();

                // get the var name
                Parameter[] parameters = targetMethod.getParameters();

                if (parameters == null || parameters.length == 0) {
                    necessary = false;
                }

                try {
                    expressionResult = expressionParser.parseExpression(expression);
                } catch (Exception e) {
                    necessary = false;
                }

            }
            String ncTag = remoteCommand.getParam("$forward-trace-option-s");
            if (!UTILS.isNullOrEmpty(ncTag) && "nc".equals(ncTag)) {
                notCheckMode = true;
            }

            this.targetLineNo = targetLineNo;
            if (targetLineNo < 0) {
                this.targetLineBeInvoked = true;
            }

            if (!UTILS.isNullOrEmpty(te)) {
                this.satisfyPuppet = new SatisfyPuppet(te);
            }

        }

        @Override
        public void invoke(List<MethodTraceFrame> frames) {
            if (remoteCommand.needStop()) {
                return; // 命令超时了
            }
            this.frames = frames;

            // ignore
            if (targetLineBeInvoked) {
                return;
            }

            // check
            for (MethodTraceFrame methodTraceFrame : frames) {
                if (methodTraceFrame != null &&
                            methodTraceFrame.getLineNo() >= 0
                            && methodTraceFrame.getLineNo() == targetLineNo) {
                    targetLineBeInvoked = true;
                    // check [te] option
                    if (this.satisfyPuppet != null) {
                        Object[] objects = new Object[methodTraceFrame.getLocalVariable().size()];
                        for (int i = 0; i < methodTraceFrame.getLocalVariable().size(); i ++) {
                            objects[i] = methodTraceFrame.getLocalVariable().get(i).getVar();
                        }
                        // check
                        targetLineBeInvoked = this.satisfyPuppet.match(objects);
                    }
                }
            }
        }

        @Override
        public boolean needToUnReg(String cls, String method, String desc) {
            if (targetLineBeInvoked) {
                adviceDoneLatch.countDown();
                return false;
            }
            return true;
        }

        /**
         * advice的类型，这个很有用
         *
         * @return {@link MethodAdviceType}
         */
        @Override
        public MethodAdviceType type() {
            return MethodAdviceType.ON_MATCH_PARAM;
        }

        /**
         *  获取当前线程的context
         *
         * @return {@link StandardEvaluationContext}
         */
        private EvaluationContext getContext() {
            EvaluationContext evaluationContext = threadLocalEvaluationContext.get();
            if (evaluationContext == null) {
                evaluationContext = new StandardEvaluationContext();
                threadLocalEvaluationContext.set(evaluationContext);
            }
            return evaluationContext;
        }

        /**
         * 用于判断是否满足要求的方法，这个方法将在原始方法return或者throw之后调用，所以会携带的信息
         * 很多，你可以根据自己的需求判断是否可以结束本次advice（unregister），比如需要实现onReturn
         *  那么就只需要判断returnVal != null && throwable == null 即可，而实现onThrow则只需要判断throwable != null
         *  即可；
         * 本方法主要是给custom和watch用的，return/throw/record判断较为简单，并且每次调用该方法应该都会返回true，因为
         * weave内部就是在满足要求的时候才会来调用advice的该方法，比如对于throw类型{@code type}的advice，那么只会在
         * 方法抛出异常退出只会才会被回调，但是对于custom和watch来说，还是需要做一些计算的；
         *
         * @param loader    方法所属的类的类加载器
         * @param cls       方法所属的类
         * @param method    方法名称
         * @param desc      方法描述
         * @param targetObj 目标对象
         * @param args      本次请求参数信息
         * @param throwable 抛出的异常（如果正常结束则为null）
         * @param returnVal 方法返回结果，如果方法的签名为void，那么也为null，所以判断方法正常结束需要
         *                  结合throwable参数判断
         * @return 本次trace结果是否匹配你的需求，如果不满足，那么weave会继续为你监控，否则会将本次方法调用信息
         * 通知给你，然后顺便把该advice unregister调，避免再次被其他方法线程调用而造成链路结果混乱
         */
        @Override
        public boolean match(ClassLoader loader, String cls, String method, String desc, Object targetObj, Object[] args, Throwable throwable, Object returnVal) {
            // 是否超时
            if (remoteCommand.needStop()) {
                return true;
            }

            if (!necessary) {
                remoteCommand.setResponseData("不合法的表达式输入，请重新输入\n");
                return true;
            }

            if (args == null || args.length == 0) {
                remoteCommand.setResponseData("是一个无参方法，无法匹配参数\n");
                return true;
            }

            // 获取到当前线程的上下文信息，如果不做ThreadLocal，那么多个表达式计算结果会互相影响
            initTheEvaluationContext(targetObj, args, returnVal, throwable, true);
            EvaluationContext evaluationContext = getContext();

            // check
            try {
                // evaluation the expression
                return expressionResult.getValue(evaluationContext, boolean.class);
            } catch (Throwable e) {
                necessary = false; // do not do again.
                PSLogger.error("error occ while evaluation spring expression:" + expression + ":" + e);
                remoteCommand.setResponseData("表达式编译运行错误:" + e + "\n");
                return true;
            }
        }

        private void initTheEvaluationContext(Object target, Object[] args, Object ret, Throwable throwable, boolean fillReturn) {
            EvaluationContext context = getContext();
            int pc = 0;
            for (Object o : args) {
                context.setVariable("p" + pc, o);
                pc ++;
            }
            if (!notCheckMode) {
                return;
            }

            // targetObj
            context.setVariable("obj", target);

            if (fillReturn) {
                // return obj
                context.setVariable("ret", ret);

                // throw exception
                context.setVariable("exp", throwable);
            }
        }

        /**
         * 如果每次方法调用的链路信息都记录下来，那么势必会造成大量的垃圾对象，因为理论上只有少量的请求是我们需要
         * 关心的，所以check方法的作用仅仅是告诉weave，本次请求我是不是感兴趣，但是具体是不是感兴趣，需要通过
         * match等方法进行精细化判断。
         *
         * @param loader    方法所属的类的类加载器
         * @param cls       方法所属的类
         * @param method    方法名称
         * @param desc      方法描述
         * @param targetObj 目标对象
         * @param args      本次请求参数信息
         * @return 如果本次方法调用你需要监听，那么你就返回true，但是最终是否满足你的需求你需要在
         * {@link MethodInvokeAdvice#match(ClassLoader, String, String, String, Object, Object[], Throwable, Object)}
         * 里面进行判断
         */
        @Override
        public boolean check(ClassLoader loader, String cls, String method, String desc, Object targetObj, Object[] args) {
            if (notCheckMode) {
                return true;
            }
            // 获取到当前线程的上下文信息，如果不做ThreadLocal，那么多个表达式计算结果会互相影响
            initTheEvaluationContext(targetObj, args, null, null, false);
            EvaluationContext evaluationContext = getContext();
            // check
            try {
                Expression expressionResult  = expressionParser.parseExpression(expression);
                // evaluation the expression
                return expressionResult.getValue(evaluationContext, boolean.class);
            } catch (Throwable e) {
                return true;
            }
        }

    }

    /**
     *  at least, the target line must be ensure at the range Method (start, end)
     *  or the command will always end with timeout;
     *
     *  so, you must get the target method's line range firstly, then check the target
     *  line attach with command, if the target line is invalid, the command should fast fail
     *  as soon as possible;
     *
     */
    static class OnTargetLineBeInvokedAdvice extends BaseMethodInvokeAdvice {

        // the target line the debug-er want to watch
        private int targetLineNo;

        // whether the target has been invoked
        private volatile boolean targetLineBeInvoked = false;

        private SatisfyPuppet satisfyPuppet = null;

        OnTargetLineBeInvokedAdvice(RemoteCommand remoteCommand, String cls,
                                    String method, String desc, Class<?> targetClass,
                                    int targetLineNo, String te) {
            super(remoteCommand, cls, method, desc, targetClass);

            this.targetLineNo = targetLineNo;

            if (!UTILS.isNullOrEmpty(te)) {
                this.satisfyPuppet = new SatisfyPuppet(te);
            }

        }

        /**
         * advice的类型，这个很有用
         *
         * @return {@link MethodAdviceType}
         */
        @Override
        public MethodAdviceType type() {
            return MethodAdviceType.FULL_MATCH;
        }

        /**
         * 这是2.0的通知实现，可以基于2.0去构造1.0的通知，1.0的通知参考 {@link MethodAdvice}
         * 1.0的实现较为复杂，实现起来不清晰，2.0将链路信息采集的过程控制上移，这样这个方法回调
         * 的时候被必然是有满足要求的链路出现了，1.0的时候这个判断是在advice里面做的，看起来
         * 既复杂又容易造成bug，所以2.0使用了更为简单的实现方式
         *
         * @param frames 方法调用链路信息
         */
        @Override
        public void invoke(List<MethodTraceFrame> frames) {
            if (remoteCommand.needStop()) {
                return; // 命令超时了
            }
            this.frames = frames;

            // check
            for (MethodTraceFrame methodTraceFrame : frames) {
                if (methodTraceFrame != null &&
                        methodTraceFrame.getLineNo() >= 0
                        && methodTraceFrame.getLineNo() == targetLineNo) {
                    targetLineBeInvoked = true;
                    // check [te] option
                    if (this.satisfyPuppet != null) {
                        Object[] objects = new Object[methodTraceFrame.getLocalVariable().size()];
                        for (int i = 0; i < methodTraceFrame.getLocalVariable().size(); i ++) {
                            objects[i] = methodTraceFrame.getLocalVariable().get(i).getVar();
                        }
                        // check
                        targetLineBeInvoked = this.satisfyPuppet.match(objects);
                    }
                }
            }
        }

        /**
         * 对于record来说，需要经过n次观察才能结束，这里控制一下，在match返回true之后还会来
         * 回调这个方法进行判断，如果还不想结束，那么返回false即可，除了record，其他都应该
         * 满足条件后立刻结束
         *
         * @return true代表可以结束
         */
        @Override
        public boolean needToUnReg(String cls, String method, String desc) {
            if (targetLineBeInvoked) {
                adviceDoneLatch.countDown();
                return false;
            }
            // another round
            return true;
        }

    }

    static class RecordAdvice extends BaseMethodInvokeAdvice {

        // 只能录制这么多时间（秒）
        private int recordTimeSecs;

        // 只能录制这么次数
        private int recordCnt;

        // 用于记录已经记录了多少次了
        private LongAdder recordAdder;

        // start mills
        private long startMills;

        // the server hook
        private ServerHook serverHook;

        RecordAdvice(RemoteCommand remoteCommand, String cls, String method,
                     String desc, int recordCnt, int recordTimeSecs, ServerHook serverHook, Class<?> clazz) {
            super(remoteCommand, cls, method, desc, clazz);

            if (recordCnt <= 0 || recordCnt > 10) {
                this.recordCnt = 10; // cnt limit
            } else {
                this.recordCnt = recordCnt;
            }

            if (recordTimeSecs <= 0 || recordTimeSecs >= 10) {
                this.recordTimeSecs = 10; // time limit
            } else {
                this.recordTimeSecs = recordTimeSecs;
            }

            this.recordAdder = new LongAdder();

            // get the server hook
            this.serverHook = serverHook;

            // this is very strange
            this.startMills = System.currentTimeMillis();
        }

        /**
         * advice的类型，这个很有用
         *
         * @return {@link MethodAdviceType}
         */
        @Override
        public MethodAdviceType type() {
            return MethodAdviceType.FULL_MATCH;
        }

        /**
         * 获取到符合要求的trace信息
         *
         * @return {@link MethodTraceFrame}
         */
        @Override
        public List<MethodTraceFrame> traces() {
            try {
                //CDHelper.await(getCDKey());
                long checkTimeoutVal = checkTimeoutMe(remoteCommand);
                if (checkTimeoutVal <= 0) {
                    checkTimeoutVal = 10; // 10 seconds timeout
                }
                adviceDoneLatch.await(checkTimeoutVal, TimeUnit.SECONDS);
            } catch (Throwable e) {
                remoteCommand.setResponseData("命令执行超时");
                return Collections.emptyList();
            }
            if (frames == null) {
                PSLogger.error("the traces result is null:" + remoteCommand);
                return Collections.emptyList();
            }

            // 这是录制，返回一个说明文案更合适
            remoteCommand.setResponseData(
                    "一共录制了:" + recordAdder.sum() + " 条请求，耗时:" + (System.currentTimeMillis() - startMills) + " ms\n");

            return frames;
        }

        /**
         * 对于record来说，需要经过n次观察才能结束，这里控制一下，在match返回true之后还会来
         * 回调这个方法进行判断，如果还不想结束，那么返回false即可，除了record，其他都应该
         * 满足条件后立刻结束
         *
         * @return true代表可以结束
         */
        @Override
        public boolean needToUnReg(String cls, String method, String desc) {
            // record this frame
            serverHook.recordMethodFlow(cls, method, desc, frames);

            // increment the cnt
            this.recordAdder.increment();

            // cnt check
            if (this.recordAdder.sum() >= recordCnt) {
                //CDHelper.cd(getCDKey());
                adviceDoneLatch.countDown();
                return false;
            }

            // time check
            if ((System.currentTimeMillis() - startMills) > recordTimeSecs * 1000) {
                //CDHelper.cd(getCDKey());
                adviceDoneLatch.countDown();
                return false;
            }

            // wait
            return true;
        }

    }

}
