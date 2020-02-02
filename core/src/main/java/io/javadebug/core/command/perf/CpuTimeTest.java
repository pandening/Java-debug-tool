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
import com.sun.jna.Native;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.DoubleByReference;

import java.util.concurrent.TimeUnit;

public class CpuTimeTest {

//    private static ICpuTime INS;
//    static {
//        //System.setProperty("jna.debug_load", "true");
//        //INS = Native.load("cputime", ICpuTime.class);
//    }
//
//    public interface ICpuTime extends Library {
//        boolean getTimesSecs(PointerType process_real_time, PointerType process_user_time, PointerType process_system_time);
//    }
//
//    public boolean getTimesSecs(DoubleByReference process_real_time, DoubleByReference process_user_time,
//                                DoubleByReference process_system_time) {
//        return INS.getTimesSecs(process_real_time, process_user_time, process_system_time);
//    }

    public static void main(String[] args) throws InterruptedException {
//        CpuTimeTest time = new CpuTimeTest();
//
//        DoubleByReference rt = new DoubleByReference(), ut = new DoubleByReference(), st = new DoubleByReference();
//
//        boolean ret = time.getTimesSecs(rt, ut, st);
//        if (ret ) {
//            System.out.println(String.format("rt:%f ut:%f st:%f", rt.getValue(), ut.getValue(), st.getValue()));
//        } else {
//            System.out.println("bad result");
//        }


        long clock_tics_per_sec = GNULibC.GNU_LIB_C.getClockTicsPerSec();
        System.out.println("result:" + clock_tics_per_sec);

        int sum = 0;
        for (int i = 0; i < 1000000000; i ++) {
            sum += i;
            if (sum > 10000) {
                sum %= 10000;
            }
            TimeUnit.MILLISECONDS.sleep(50);

            System.out.println("cur:" + System.currentTimeMillis());

//            TimeInfo timeInfo = NativeCpuTime.NATIVE_CPU_TIME.getTimesSecs();
//            if (timeInfo != null) {
//                System.out.println("1===> " + timeInfo);
//            } else {
//                System.out.println("null pointer");
//            }

            TimeInfo tm = GNULibC.GNU_LIB_C.getTimesSecs();
            if (tm != null) {
                System.out.println("2===> " + tm);
            } else {
                System.out.println("null pointer");
            }

//            if (timeInfo != null && tm != null && !timeInfo.equals(tm)) {
//                System.err.println("fixme!\n" + timeInfo + "\n" + tm);
//                return;
//            }


//            ret = time.getTimesSecs(rt, ut, st);
//            if (ret ) {
//                System.out.println(String.format("rt:%f ut:%f st:%f", rt.getValue(), ut.getValue(), st.getValue()));
//            } else {
//                System.out.println("bad result");
//            }

        }

    }

}
