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

public class TimeValStruct extends Structure {

    public NativeLong tv_sec; /* seconds */
    public int tv_usec;  /* and microseconds */

    public static class ByReference extends TimeValStruct implements Structure.ByReference {}
    public static class ByValue extends TimeValStruct implements Structure.ByValue {}

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("tv_sec", "tv_usec");
    }

    @Override
    public String toString() {
        return "TimeValStruct{" +
                       "tv_sec=" + tv_sec +
                       ", tv_usec=" + tv_usec +
                       '}';
    }

}
