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
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;

import java.math.BigDecimal;

public enum GNULibC {
    GNU_LIB_C;

    public static GLibC LIBC;
    private static long clock_tics_per_sec = -1;

    /*
    *
    *  mac:     /usr/include/sys/resource.h
    *  linux:   /usr/include/bits/resource.h
    *
    *   for getrusage:
    *      0  => Current process information
    *      -1 => Current process's children information
    * */
    private static final int RUSAGE_SELF = 0;

    static {
        try {
            System.setProperty("jna.debug_load", "true");
            LIBC = Native.load("c", GLibC.class);
        } catch (Throwable e) {
            e.printStackTrace();
            PSLogger.error("could not load 'c' library in this os", e);
        }

    }

    public interface GLibC extends Library {

        /**
         *  do the syscall from java
         *
         * @param sysNo the syscall no, ref sys/syscall.h
         * @param args the call args
         * @return the ret, -1 means call error, the error stores in errno;
         */
        int syscall(int sysNo, Object... args);

        /**
         *  get the system config
         *
         * @param cid the config id
         * @return the config value
         */
        NativeLong sysconf(int cid);

        /**
         *  do the syscall "times" from java
         *
         * @param tmStructure {@link TmStructure}
         * @return the real-time (walk clock time)
         */
        NativeLong times(TmStructure tmStructure);

        /**
         *
         * @param who
        RUSAGE_SELF
        Return resource usage statistics for the calling process,
        which is the sum of resources used by all threads in the
        process.

        RUSAGE_CHILDREN
        Return resource usage statistics for all children of the
        calling process that have terminated and been waited for.
        These statistics will include the resources used by
        grandchildren, and further removed descendants, if all of the
        intervening descendants waited on their terminated children.

        RUSAGE_THREAD (since Linux 2.6.26)
        Return resource usage statistics for the calling thread.  The
        _GNU_SOURCE feature test macro must be defined (before
        including any header file) in order to obtain the definition
        of this constant from <sys/resource.h>.

         * @param usage {@link RusageStruct}
         *
         * @return get resource usage
         */
        int getrusage(int who, RusageStruct usage);

        /**
         * The getloadavg() function returns the number of processes in the system
         * run queue averaged over various periods of time.  Up to nelem samples are
         * retrieved and assigned to successive elements of loadavg[].  The system
         * imposes a maximum of 3 samples, representing averages over the last 1, 5,
         * and 15 minutes, respectively.
         * @param loadavg An array of doubles which will be filled with the results
         * @param nelem Number of samples to return
         * @return If the load average was unobtainable, -1 is returned; otherwise,
         * the number of samples actually retrieved is returned.
         * @see <A HREF="https://www.freebsd.org/cgi/man.cgi?query=getloadavg&sektion=3">getloadavg(3)</A>
         */
        int getloadavg(Pointer loadavg, int nelem);

    }

    /**
     *  get the system's load avg
     *
     * @return load avg 1 / 5 / 15 min
     */
    public double[] getLoadAvg() {
        try {
            int nelem = 3;
            int doubleNativeSize = Native.getNativeSize(Double.TYPE);
            Pointer loadavg = new Memory(nelem * doubleNativeSize);
            int actnelem = GNULibC.LIBC.getloadavg(loadavg, nelem);
            if (actnelem == -1) {
                PSLogger.error("could not get the load avg with -1 nelem");
                return null;
            }
            double[] ret = new double[nelem];
            for (int i = 0; i < nelem; i ++) {
                ret[i] = loadavg.getDouble(i * doubleNativeSize);
                BigDecimal bigDecimal = new BigDecimal(ret[i]);
                ret[i] = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            }
            return ret;
        } catch (Exception e) {
            PSLogger.error("fail to call getloadavg with error:" + UTILS.getErrorMsg(e));
            return null;
        }
    }

    /**
     *  get the current process' cpu time usage
     *
     * @return {@link TimeInfo}
     */
    public TimeInfo getTimesSecs() {
        try {
            TmStructure tmStructure = new TmStructure();
            NativeLong real = GNULibC.LIBC.times(tmStructure);

            if (real == null || real.longValue() == -1) {
                PSLogger.error("could not get the times info by syscall \"times\"");
                return null;
            }

            TimeInfo timeInfo = new TimeInfo();

            timeInfo.systemSecs = tmStructure.tms_stime.longValue() / (1.0 * clock_tics_per_sec);
            timeInfo.userSecs = tmStructure.tms_utime.longValue() / (1.0 * clock_tics_per_sec);
            timeInfo.realSecs = real.longValue() / (1.0 * clock_tics_per_sec);

            return  timeInfo;
        } catch (Throwable e) {
            PSLogger.error("fail to call syscall times:" + UTILS.getErrorMsg(e));
            return null;
        }
    }

    public RusageStruct getCurrentProcessResourceUsage() {
        try {
            RusageStruct rusageStruct = new RusageStruct();
            int errno = GNULibC.LIBC.getrusage(RUSAGE_SELF, rusageStruct);

            //On success, zero is returned.  On error, -1 is returned, and errno is
            //set appropriately.

            if (errno != 0) {
                PSLogger.error("fail to call syscall 'rusage' , error maybe : " +
                                       "\n[1]  usage points outside the accessible address space;" +
                                       "\n[2]  who is invalid.");
                return null;
            }

            // get the result
            return rusageStruct;
        } catch (Throwable e) {
            PSLogger.error("fail to call syscall getrusage:" + UTILS.getErrorMsg(e));
            return null;
        }
    }

    /**
     *  get the _SC_CLK_TCK define value
     *
     *  linux: 2
     *  mac:   3
     *
     * @return the config
     */
    private int get_SC_CLK_TCK() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return 3; // /usr/include/unistd.h
        } else if (os.contains("linux")) {
            return 2; // /usr/include/bits/time.h
        } else {
            throw new UnsupportedOperationException("un-support os:" + os);
        }
    }

    /**
     *  get clock_tics_per_sec of this system
     *
     * @return the clock_tics_per_sec val, default 100
     */
    public long getClockTicsPerSec() {

        if (clock_tics_per_sec > 0) {
            return clock_tics_per_sec;
        }

        try {
            NativeLong ret =  GNULibC.LIBC.sysconf(get_SC_CLK_TCK());
            if (ret == null) {
                PSLogger.error("null pointer");
                clock_tics_per_sec = 100;
                return clock_tics_per_sec;
            }

            // cache this value
            clock_tics_per_sec = ret.longValue();
            PSLogger.error("get the clock_tics_per_sec and cache it : " + clock_tics_per_sec);

            return clock_tics_per_sec;
        } catch (Throwable e) {
            PSLogger.error("could not get the clock_tics_per_sec, default = 100");
            clock_tics_per_sec = 100;
            return clock_tics_per_sec;
        }
    }

}
