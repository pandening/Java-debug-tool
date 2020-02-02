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

import io.javadebug.core.ServerHook;
import io.javadebug.core.transport.RemoteCommand;

import java.util.ArrayList;
import java.util.List;

public class RecordCountLimitMethodTraceAdviceImpl extends AbstractMethodTraceCommandAdvice {

    // the limit count
    private int limit;

    // timeout limit, minutes unit;
    private int timeout;

    // the start time
    private long startMills;

    // the server hook
    private ServerHook serverHook;

    public RecordCountLimitMethodTraceAdviceImpl(int context, String className,
                                                 String method, String desc,
                                                 MethodTraceListener listener,
                                                 int limit, int timeout,
                                                 ServerHook serverHook,
                                                 RemoteCommand remoteCommand) {
        super(context, className, method, desc, listener, true, remoteCommand);

        this.limit = limit;
        this.timeout = timeout;

        this.serverHook = serverHook;

        // check param
        checkParamAndAdjust();
    }

    /**
     *  需要确定一下，不要让客户端肆意妄为
     */
    private void checkParamAndAdjust() {
        if (limit >= 10) {
            limit = 10;
        }
        if (timeout <= 0) {
            timeout = 10;
        }
        if (timeout >= 60) {
            timeout = 60;
        }
    }

    @Override
    protected void preStart() {
        startMills = System.currentTimeMillis();

        // reset the listener
        resetListener(new MethodTraceListener() {

            private List<MethodTraceFrame> oneRoundTrace;

            @Override
            public void invokeLine(MethodTraceFrame methodTraceFrame) {
                if (oneRoundTrace == null) {
                    oneRoundTrace = new ArrayList<>();
                }
                oneRoundTrace.add(methodTraceFrame);
                if (methodTraceFrame.isEnd()) {
                    // record it
                    serverHook.recordMethodFlow(className, methodName, methodDesc, oneRoundTrace);

                    // another round
                    oneRoundTrace = new ArrayList<>();

                    // reduce 1 time
                    limit --;
                }
            }

            /**
             * 方法进入
             */
            @Override
            public void enter() {

            }

            /**
             * 方法正常退出
             */
            @Override
            public void exit() {

            }

            /**
             * 方法异常退出
             */
            @Override
            public void exception() {

            }
        });
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
        return remoteCommand.needStop()
                       || checkOwn(cls, method, desc) && (limit <= 0 || (System.currentTimeMillis() - startMills) >= (timeout * 1000L));

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
}
