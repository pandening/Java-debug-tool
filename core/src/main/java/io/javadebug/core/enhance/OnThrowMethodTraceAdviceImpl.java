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

public class OnThrowMethodTraceAdviceImpl extends AbstractMethodTraceCommandAdvice {

    private Class<? extends Throwable> targetThrowClass;
    private String targetThrowClassName;

    public OnThrowMethodTraceAdviceImpl(int context, String className,
                                        String method, String desc,
                                        MethodTraceListener listener,
                                        /*Class<? extends Throwable> targetThrowClass*/
                                        String targetThrowClass,
                                        RemoteCommand remoteCommand) {
        super(context, className, method, desc, listener, true, remoteCommand);

        this.targetThrowClassName = targetThrowClass;

        // load the target exception
        loadTargetThrowableClass();
    }

    @SuppressWarnings("unchecked")
    private void loadTargetThrowableClass() {
        if (!UTILS.isNullOrEmpty(targetThrowClassName)) {
            // using the current classloader is ok.
            try {
                targetThrowClass = (Class<? extends Throwable>) Thread.currentThread().getContextClassLoader().loadClass(targetThrowClassName);
            } catch (Throwable e) {
                PSLogger.error("could not load target throwable class:" + targetThrowClassName + ":" + e);
            }
        }
    }

    /**
     * 这个方法用来控制一次可返回状态的时候是否要结束观察，这个方法由各个子类控制即可，比如
     * onReturn类型的Advice，只需要看是否CD结束了（默认就是这种行为），但是比如onThrow这种
     * 类型的Advice就要看本次Record是否是Throw的，如果不是，那么需要再等下一轮。
     *
     * @return true/false
     */
    @Override
    protected boolean allowToEnd(Object[] args, Object retVal, Throwable throwable, String cls, String method, String desc) {
        if (!checkOwn(cls, method, desc)) {
            return false;
        }
        if (targetThrowClass == null) {
            return isThrow;
        }
        return remoteCommand.needStop() || (isThrow && throwable.getClass().equals(targetThrowClass));
    }

    /**
     * 该advice的type
     *
     * @return {@link MethodAdviceType}
     */
    @Override
    public MethodAdviceType type() {
        return MethodAdviceType.ON_THROW;
    }
}
