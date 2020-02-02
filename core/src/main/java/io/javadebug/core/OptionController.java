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


package io.javadebug.core;

import io.javadebug.core.enhance.MethodTraceEnhance;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.monitor.thread.ThreadMonitorCollector;
import io.javadebug.core.utils.ObjectUtils;

/**
 * Created on 2019/4/18 20:52.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class OptionController {

    public static void offInfoLog() {
        PSLogger.configureInfo(false);
    }

    public static void onInfoLog() {
        PSLogger.configureInfo(true);
    }

    public static void offErrorLog() {
        PSLogger.configureError(false);
    }

    public static void onErrorLog() {
        PSLogger.configureError(true);
    }

    public static void onForcePrintObjectToJson() {
        ObjectUtils.forceToJson = true;
    }

    public static void offForcePrintObjectToJson() {
        ObjectUtils.forceToJson = false;
    }

    public static void onPrintObjectToJson() {
        ObjectUtils.objectToJson = true;
    }

    public static void offPrintObjectToJson() {
        ObjectUtils.objectToJson = false;
    }

    public static void onDumpClassAfterEnhance() {
        MethodTraceEnhance._dump_class_to_debug_tag = true;
    }

    public static void offDumpClassAfterEnhance() {
        MethodTraceEnhance._dump_class_to_debug_tag = false;
    }

    public static void onShowTopCallStackForThreadMonitor() {
        ThreadMonitorCollector.setNeedCallStack(true);
    }

    public static void offShowTopCallStackForThreadMonitor() {
        ThreadMonitorCollector.setNeedCallStack(false);
    }

    public static int setTopNForThreadCollector(int topN) {
        return ThreadMonitorCollector.setShowTopNThreadCnt(topN);
    }

}
