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


import org.objectweb.asm.Type;

public class B {
    private int a;
    private String b;
    private static String c = "abc";

    public void readField() {
        receiveField(a);
        receiveField(b);
        receiveField(c);
    }

    public void call(Object p) {
        System.out.println(p);
    }

    public void receiveField(Object o) {
        System.out.println(o);
    }

    public static void main(String[] args) throws NoSuchFieldException {
        new B().call(1234);

        String desc = Type.getDescriptor(B.class.getDeclaredField("c").getType());


        System.out.println(desc);

    }

}
