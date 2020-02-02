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
//  Auth : HJ


package io.javadebug.core.log;

import java.io.PrintStream;
import java.util.Date;

/**
 * Created on 2019/4/17 17:42.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class PSLogger {

    private static final PrintStream eps = System.err;
    private static final PrintStream ops = System.out;

    private static volatile boolean allowInfo = false;
    private static volatile boolean allowErr = true;

    public static void configureInfo(boolean f) {
        allowInfo = f;
    }

    public static void configureError(boolean f) {
        allowErr = f;
    }

    // get the internal logger
    private static InternalLogger LOGGER = new StdImplLogger();

    public static void initClientLogger() {
        System.err.println("init client logger ...");
        LOGGER = InternalLoggerFactory.getLogger("c");
    }

    public static void initServerLogger() {
        System.err.println("init server logger ...");
        LOGGER = InternalLoggerFactory.getLogger("s");
    }

    public static void t(String msg) {
        //
    }

    public static void d(String msg) {
        //
    }

    public static void i(String msg) {
        if (allowInfo) {
            ops.println(String.format("[info] [%s] [%s] %s", new Date(), Thread.currentThread().getName(), msg));
        }
    }

    public static void w(String msg) {
        if (allowErr) {
            eps.println(String.format("[error] [%s] [%s] %s", new Date(), Thread.currentThread().getName(), msg));
        }
    }

    public static void e(String msg) {
        if (allowErr) {
            eps.println(String.format("[error] [%s] [%s] %s", new Date(), Thread.currentThread().getName(), msg));
        }
    }



    public static void info(String msg) {
        LOGGER.info(msg);
    }

    public static void error(String msg) {
        LOGGER.error(msg);
    }

    public static void error(String msg, Throwable e) {
        LOGGER.error(msg, e);
    }

}
