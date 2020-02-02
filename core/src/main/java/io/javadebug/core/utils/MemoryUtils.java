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


package io.javadebug.core.utils;

import io.javadebug.core.data.JVMMemoryInfo;

public class MemoryUtils {

    /**
     *  simple create an new instance of Type: {@link JVMMemoryInfo}
     *
     * @return the instance of jvm memory info
     */
    public static JVMMemoryInfo getMemory() {
        return new JVMMemoryInfo();
    }

    public static void main(String[] args) {
        System.out.println(getMemory());
    }

}
