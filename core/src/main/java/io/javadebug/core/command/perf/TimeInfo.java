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

public class TimeInfo {

    public double realSecs = -1.0, systemSecs, userSecs;
    public long nvcsw, nivcsw;
    //public double[] loadavg;

    @Override
    public String toString() {
        return String.format("[Times: user=%3.2f sys=%3.2f real=%3.2f secs Switch: nvcsw=%d nivcsw=%d]",
                userSecs, systemSecs, realSecs, nvcsw, nivcsw);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TimeInfo)) {
            return false;
        }
        if (Double.compare(((TimeInfo) obj).realSecs, realSecs) != 0) {
            return false;
        }
        if (Double.compare(((TimeInfo) obj).systemSecs, systemSecs) != 0) {
            return false;
        }
        if (Double.compare(((TimeInfo) obj).userSecs, userSecs) != 0) {
            return false;
        }
        return true;
    }
}
