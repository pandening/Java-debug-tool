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

public class TmStructure extends Structure {
    public NativeLong tms_utime;	/* [XSI] User CPU time */
    public NativeLong	 tms_stime;	/* [XSI] System CPU time */
    public NativeLong	 tms_cutime;	/* [XSI] Terminated children user CPU time */
    public NativeLong	 tms_cstime;	/* [XSI] Terminated children System CPU time */

    public static class ByReference extends TmStructure implements Structure.ByReference {}
    public static class ByValue extends TmStructure implements Structure.ByValue {}

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("tms_utime", "tms_stime", "tms_cutime", "tms_cstime");
    }

    @Override
    public String toString() {
        return String.format("[Times: user=%3.2f sys=%3.2f secs]",
                tms_utime.longValue() / 100.0, tms_stime.longValue() / 100.0);
    }
}
