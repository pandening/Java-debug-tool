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
//  Author : HJ


package io.javadebug.test;

import io.javadebug.test.a.Aa;

public class TestClass {

    Aa aa = new Aa();

    public int test(int in) {

        if (in == 5) {
            return 100;
        }
        String tag = "the in:" + in;
        if (in < 5) {
            in += 2;
        } else {
            in -= 1;
        }

        if (in > 5) {
            throw new IllegalArgumentException("must <= 5");
        }
        if (in <= 3) {
            throw new NullPointerException("must >= 3");
        }

        return in * 100;
    }

}
