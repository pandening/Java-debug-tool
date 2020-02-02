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


import com.google.common.base.Strings;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

public final class S {

    public S(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public S(byte[] bytes, int o, int len) {
        checkBounds(bytes, o, len);
    }

    private static void checkBounds(byte[] bytes, int offset, int length) {
        if (length < 0)
            throw new StringIndexOutOfBoundsException(length);
        if (offset < 0)
            throw new StringIndexOutOfBoundsException(offset);
        if (offset > bytes.length - length)
            throw new StringIndexOutOfBoundsException(offset + length);
    }

    private static void checkBoundsaa(byte[] bytes, int offset, int length) throws InvocationTargetException, IllegalAccessException {
        try {
            WeaveSpy.ON_METHOD_ENTER_CALL.invoke((Object)null, new Integer(10000), null, "S", "checkBounds", "([BII)V", null, new Object[]{bytes, new Integer(offset), new Integer(length)});
            WeaveSpy.ON_METHOD_INVOKE_LINE_CALL.invoke((Object)null, new Integer(15));
            if (length < 0) {
                WeaveSpy.ON_METHOD_INVOKE_LINE_CALL.invoke((Object)null, new Integer(16));
                throw new StringIndexOutOfBoundsException(length);
            } else {
                WeaveSpy.ON_METHOD_INVOKE_LINE_CALL.invoke((Object)null, new Integer(17));
                if (offset < 0) {
                    WeaveSpy.ON_METHOD_INVOKE_LINE_CALL.invoke((Object)null, new Integer(18));
                    throw new StringIndexOutOfBoundsException(offset);
                } else {
                    WeaveSpy.ON_METHOD_INVOKE_LINE_CALL.invoke((Object)null, new Integer(19));
                    if (offset > bytes.length - length) {
                        WeaveSpy.ON_METHOD_INVOKE_LINE_CALL.invoke((Object)null, new Integer(20));
                        throw new StringIndexOutOfBoundsException(offset + length);
                    } else {
                        WeaveSpy.ON_METHOD_INVOKE_LINE_CALL.invoke((Object)null, new Integer(21));
                        WeaveSpy.ON_METHOD_EXIT_CALL.invoke((Object)null, null, "S", "checkBounds", "([BII)V");
                    }
                }
            }
        } catch (Throwable var4) {
            WeaveSpy.ON_METHOD_THROW_EXCEPTION_CALL.invoke((Object)null, var4, "S", "checkBounds", "([BII)V");
            throw var4;
        }
    }

    public static Integer valueOf(String s) throws NumberFormatException {
        return null;
    }

    public static void main(String[] args) {
        String origin = "test";
        byte[] bytes = origin.getBytes();
//        try {
//            checkBoundsaa(bytes, 0, bytes.length);
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {

                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    String s = "";

                    try {
                        //s = new String(bytes);

                        if (Strings.isNullOrEmpty(s)) {
                            System.out.println("hehe");
                        }

                        String v = "111";
                        int vv = Integer.valueOf(v);
                        System.out.println(vv);
                        checkBounds(bytes, 0, bytes.length);
                        System.out.println("ret : " + s);
                        S ss = new S(bytes);
                        System.out.println(ss);
                    }catch (Exception e) {
                        e.printStackTrace();
                        try {
                            TimeUnit.SECONDS.sleep(3);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        }, "AAAA");
        t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
            }
        });
        t.start();

    }


}
