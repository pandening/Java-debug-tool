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

public interface MethodTraceListener {

    /**
     *  当访问到方法的一行的时候，会调用这个方法，你可以基于这个方法做一些事情
     *
     * @param methodTraceFrame {@link MethodTraceFrame}
     */
    void invokeLine(MethodTraceFrame methodTraceFrame);

    /**
     *  方法进入
     *
     */
    void enter();

    /**
     *  方法正常退出
     *
     */
    void exit();

    /**
     *  方法异常退出
     *
     */
    void exception();

    MethodTraceListener NOP_LISTENER = new MethodTraceListener() {
        @Override
        public void invokeLine(MethodTraceFrame methodTraceFrame) {
            // do nothing
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
    };

}
