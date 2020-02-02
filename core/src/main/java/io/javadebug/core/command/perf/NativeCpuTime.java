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


package io.javadebug.core.command.perf;

import com.sun.jna.Library;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.DoubleByReference;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;

@Deprecated
public enum  NativeCpuTime {
    NATIVE_CPU_TIME;

    private static ICpuTime CPU_TIME;
    static {

        //
        // DO NOT REMOVE THE COMMENT !!!
        //

        //System.setProperty("jna.debug_load", "true");
        //CPU_TIME = Native.load("cputime", ICpuTime.class);
    }

    public interface ICpuTime extends Library {

        /**
         *  the mirror of native function;
         *
         * @param process_real_time walk clock time
         * @param process_user_time user time for all threads
         * @param process_system_time system time
         * @return valid tag
         */
        boolean getTimesSecs(PointerType process_real_time,
                             PointerType process_user_time,
                             PointerType process_system_time);

    }

    /**
     *
     *  get the current process' cpu time usage
     *
     * @return {@link TimeInfo}
     */
    @Deprecated
    public TimeInfo getTimesSecs() {

        DoubleByReference processRealTime = new DoubleByReference(),
                processUserTime = new DoubleByReference(),
                processSystemTime = new DoubleByReference() ;

        try {
            boolean valid = CPU_TIME.getTimesSecs(processRealTime, processUserTime, processSystemTime);
            if (valid) {
                TimeInfo timeInfo = new TimeInfo();

                timeInfo.realSecs = processRealTime.getValue();
                timeInfo.userSecs = processUserTime.getValue();
                timeInfo.systemSecs = processSystemTime.getValue();

                return timeInfo;
            }

            PSLogger.error("oh ! the native tell me the function call result is invalid !");

            return null;
        } catch (Throwable e) {

            PSLogger.error("error occ:" + UTILS.getErrorMsg(e));

            return null;
        }

    }

}
