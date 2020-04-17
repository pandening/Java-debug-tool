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

import io.javadebug.core.handler.StopAbleRunnable;

import java.util.List;

/**
 *  weave invoke order:
 *
 *  {@link MethodInvokeAdvice#method()} => check the class type
 *  {@link MethodInvokeAdvice#match(ClassLoader, String, String, String, Object, Object[], Throwable, Object)} => check the biz
 *  {@link MethodInvokeAdvice#invoke(List)} => callback
 *  {@link MethodInvokeAdvice#needToUnReg(String, String, String)} => unregister the advice
 *
 *
 */
public interface MethodInvokeAdvice {

    /**
     *  这是2.0的通知实现，可以基于2.0去构造1.0的通知，1.0的通知参考 {@link MethodAdvice}
     *  1.0的实现较为复杂，实现起来不清晰，2.0将链路信息采集的过程控制上移，这样这个方法回调
     *  的时候被必然是有满足要求的链路出现了，1.0的时候这个判断是在advice里面做的，看起来
     *  既复杂又容易造成bug，所以2.0使用了更为简单的实现方式
     *
     * @param frames 方法调用链路信息
     */
    void invoke(List<MethodTraceFrame> frames);

    /**
     *  advice的类型，这个很有用
     *
     * @return {@link MethodAdviceType}
     */
    MethodAdviceType type();

    /**
     *  在注册了advice之后，需要做的一些task
     *
     * @return {@link StopAbleRunnable}
     */
    List<StopAbleRunnable> tasks();

    /**
     *  获取到符合要求的trace信息
     *
     * @return {@link MethodTraceFrame}
     */
    List<MethodTraceFrame> traces();

    /**
     *  get the result from this method, but this method must call after traces, the common
     *  code maybe like this:
     *
     *  String result;
     *  List<MethodTraceFrame> traces = advice.traces()
     *  if (traces == null) {
     *      result = advice.print();
     *  } else {
     *      result = traceHandle(traces)
     *  }
     *
     * @return the result
     */
    String print();

    /**
     *  weave需要做一些基础的判断，否则其他方法的trace也会通知过来
     *
     * @return {@link MethodDesc}
     */
    MethodDesc method();

    /**
     *  用于判断是否满足要求的方法，这个方法将在原始方法return或者throw之后调用，所以会携带的信息
     *  很多，你可以根据自己的需求判断是否可以结束本次advice（unregister），比如需要实现onReturn
     *  那么就只需要判断returnVal != null && throwable == null 即可，而实现onThrow则只需要判断throwable != null
     *  即可；
     *  本方法主要是给custom和watch用的，return/throw/record判断较为简单，并且每次调用该方法应该都会返回true，因为
     *  weave内部就是在满足要求的时候才会来调用advice的该方法，比如对于throw类型{@code type}的advice，那么只会在
     *  方法抛出异常退出只会才会被回调，但是对于custom和watch来说，还是需要做一些计算的；
     *
     * @param loader 方法所属的类的类加载器
     * @param cls 方法所属的类
     * @param method 方法名称
     * @param desc 方法描述
     * @param targetObj 目标对象
     * @param args 本次请求参数信息
     * @param throwable 抛出的异常（如果正常结束则为null）
     * @param returnVal  方法返回结果，如果方法的签名为void，那么也为null，所以判断方法正常结束需要
     *                   结合throwable参数判断
     *
     * @return 本次trace结果是否匹配你的需求，如果不满足，那么weave会继续为你监控，否则会将本次方法调用信息
     *         通知给你，然后顺便把该advice unregister调，避免再次被其他方法线程调用而造成链路结果混乱
     */
    boolean match(ClassLoader loader, String cls, String method,
                  String desc, Object targetObj, Object[] args, Throwable throwable, Object returnVal);

    /**
     *
     *  如果每次方法调用的链路信息都记录下来，那么势必会造成大量的垃圾对象，因为理论上只有少量的请求是我们需要
     *  关心的，所以check方法的作用仅仅是告诉weave，本次请求我是不是感兴趣，但是具体是不是感兴趣，需要通过
     *  match等方法进行精细化判断。
     *
     * @param loader 方法所属的类的类加载器
     * @param cls 方法所属的类
     * @param method 方法名称
     * @param desc 方法描述
     * @param targetObj 目标对象
     * @param args 本次请求参数信息
     *
     * @return 如果本次方法调用你需要监听，那么你就返回true，但是最终是否满足你的需求你需要在
     *  {@link MethodInvokeAdvice#match(ClassLoader, String, String, String, Object, Object[], Throwable, Object)}
     *  里面进行判断
     */
    boolean check(ClassLoader loader, String cls, String method, String desc, Object targetObj, Object[] args);

    /**
     *  对于record来说，需要经过n次观察才能结束，这里控制一下，在match返回true之后还会来
     *  回调这个方法进行判断，如果还不想结束，那么返回false即可，除了record，其他都应该
     *  满足条件后立刻结束
     *
     * @return true代表可以结束
     */
    boolean needToUnReg(String cls, String method, String desc);

}
