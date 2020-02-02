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


import java.util.Date;

public class TryCatchFinallyTest {

    static class IntWrap {
        int intVal;

        @Override
        public String toString() {
            return "[" + intVal + "]";
        }
    }

    static IntWrap intVal() {
        IntWrap intWrap = new IntWrap();
        try {
            intWrap.intVal = 10;
            String a = null;
            a.length();
            return intWrap;
        } catch (NullPointerException e) {
            intWrap.intVal = 20;
            System.out.println("npe");
            return intWrap;
        } finally {
            intWrap.intVal = 30;
            throw new IllegalStateException("do not reach here");
        }
    }


    public static void main(String[] args) {

        System.out.println(intVal());

    }

}
