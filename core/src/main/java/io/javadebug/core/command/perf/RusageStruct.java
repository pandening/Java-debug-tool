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

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;



//              struct rusage {
//                      struct timeval ru_utime; /* user CPU time used */
//                      struct timeval ru_stime; /* system CPU time used */
//                       long   ru_maxrss;        /* maximum resident set size */
//                       long   ru_ixrss;         /* integral shared memory size */
//                       long   ru_idrss;         /* integral unshared data size */
//                       long   ru_isrss;         /* integral unshared stack size */
//                       long   ru_minflt;        /* page reclaims (soft page faults) */
//                       long   ru_majflt;        /* page faults (hard page faults) */
//                       long   ru_nswap;         /* swaps */
//                       long   ru_inblock;       /* block input operations */
//                       long   ru_oublock;       /* block output operations */
//                       long   ru_msgsnd;        /* IPC messages sent */
//                       long   ru_msgrcv;        /* IPC messages received */
//                       long   ru_nsignals;      /* signals received */
//                       long   ru_nvcsw;         /* voluntary context switches */
//                       long   ru_nivcsw;        /* involuntary context switches */
//                       };

public class RusageStruct extends Structure {

    public TimeValStruct ru_utime = new TimeValStruct(); /* user CPU time used */
    public TimeValStruct ru_stime = new TimeValStruct(); /* system CPU time used */
    public NativeLong ru_maxrss;        /* maximum resident set size */
    public NativeLong   ru_ixrss;         /* integral shared memory size */
    public NativeLong   ru_idrss;         /* integral unshared data size */
    public NativeLong   ru_isrss;         /* integral unshared stack size */
    public NativeLong   ru_minflt;        /* page reclaims (soft page faults) */
    public NativeLong   ru_majflt;        /* page faults (hard page faults) */
    public NativeLong   ru_nswap;         /* swaps */
    public NativeLong   ru_inblock;       /* block input operations */
    public NativeLong   ru_oublock;       /* block output operations */
    public NativeLong   ru_msgsnd;        /* IPC messages sent */
    public NativeLong   ru_msgrcv;        /* IPC messages received */
    public NativeLong   ru_nsignals;      /* signals received */
    public NativeLong   ru_nvcsw;         /* voluntary context switches */
    public NativeLong   ru_nivcsw;        /* involuntary context switches */

    public static class ByReference extends RusageStruct implements Structure.ByReference {}
    public static class ByValue extends RusageStruct implements Structure.ByValue {}

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("ru_utime", "ru_stime", "ru_maxrss", "ru_ixrss", "ru_idrss", "ru_isrss",
                "ru_minflt", "ru_majflt", "ru_nswap", "ru_inblock", "ru_oublock", "ru_msgsnd",
                "ru_msgrcv", "ru_nsignals", "ru_nvcsw", "ru_nivcsw");
    }

    @Override
    public String toString() {
        return "RusageStruct{" +
                       "ru_utime=" + ru_utime +
                       ", ru_stime=" + ru_stime +
                       ", ru_maxrss=" + ru_maxrss.longValue() +
                       ", ru_ixrss=" + ru_ixrss.longValue() +
                       ", ru_idrss=" + ru_idrss.longValue() +
                       ", ru_isrss=" + ru_isrss.longValue() +
                       ", ru_minflt=" + ru_minflt.longValue() +
                       ", ru_majflt=" + ru_majflt.longValue() +
                       ", ru_nswap=" + ru_nswap.longValue() +
                       ", ru_inblock=" + ru_inblock.longValue() +
                       ", ru_oublock=" + ru_oublock.longValue() +
                       ", ru_msgsnd=" + ru_msgsnd.longValue() +
                       ", ru_msgrcv=" + ru_msgrcv.longValue() +
                       ", ru_nsignals=" + ru_nsignals.longValue() +
                       ", ru_nvcsw=" + ru_nvcsw.longValue() +
                       ", ru_nivcsw=" + ru_nivcsw.longValue() +
                       '}';
    }

}
