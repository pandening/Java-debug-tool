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


import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import org.objectweb.asm.commons.Method;

import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BlockingThreadTest {

    public static void main(String[] args) throws NoSuchMethodException {

        String s = Method.getMethod(BlockingThreadTest.class.getDeclaredMethod("isInNonBlockingThread")).getDescriptor();
        System.out.println(s);
        call();

    }

    /**
     *  check the block call.
     *
     * @param nonBlockingThreadNamePattern match the non-blocking thread name
     */
    public static void checkBlock(String nonBlockingThreadNamePattern) {
        if (nonBlockingThreadNamePattern == null || nonBlockingThreadNamePattern.isEmpty()) {
            nonBlockingThreadNamePattern = "NonBlocking";
        }
        boolean blockCheckPass = true;

        // thread name check
        if (Thread.currentThread().getName().contains(nonBlockingThreadNamePattern)) {
            blockCheckPass = false;
        }

        // class type check
        if (blockCheckPass) {
            Class<?>[] cls = Thread.currentThread().getClass().getInterfaces();
            if (cls != null && cls.length != 0) {
                for (Class<?> ic : cls) {
                    // maybe : reactor.core.scheduler.NonBlocking
                    if (ic.getName().contains(nonBlockingThreadNamePattern)) {
                        blockCheckPass = false;
                    }
                }
            }
        }

        // thread stack check
        if (!blockCheckPass) {
            Map<String, String> IGNORE_CLASS_METHOD_MAP = new HashMap<>();
            IGNORE_CLASS_METHOD_MAP.put("a", "a");
            IGNORE_CLASS_METHOD_MAP.put("b", "b");

            // check the stack
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (int i = 0; i < 10 && i < stackTrace.length; i ++) {
                StackTraceElement ste = stackTrace[i];
                String ignore = IGNORE_CLASS_METHOD_MAP.get(ste.getClassName());
                if (!(ignore == null || ignore.isEmpty()) && ignore.contains(ste.getMethodName())) {
                    PSLogger.error("this is a ignore blocking method call : " + ste.getClassName() + "." + ste.getMethodName());
                    blockCheckPass = false;
                    break;
                }
            }
        }

        if (!blockCheckPass) {
            throw new IllegalStateException("thread are blocking, which is not supported in thread :"
                                                    + Thread.currentThread().getName());
        }
    }

    public static void sleep222(long var0, int var2) throws InterruptedException {
        if (var0 < 0L) {
            throw new IllegalArgumentException("timeout value is negative");
        } else if (var2 >= 0 && var2 <= 999999) {
            if (var2 >= 500000 || var2 != 0 && var0 == 0L) {
                ++var0;
            }

            sleep(var0);
        } else {
            throw new IllegalArgumentException("nanosecond timeout value out of range");
        }
    }

    public static void sleep111(long var0, int var2) throws Exception {
        Class var3 = Class.forName("io.javadebug.agent.WeaveSpy");
        var3.getDeclaredMethod("checkBlock", String.class).invoke((Object)null, "ok");
        if (var0 < 0L) {
            throw new IllegalArgumentException("timeout value is negative");
        } else if (var2 >= 0 && var2 <= 999999) {
            if (var2 >= 500000 || var2 != 0 && var0 == 0L) {
                ++var0;
            }

            sleep(var0);
        } else {
            throw new IllegalArgumentException("nanosecond timeout value out of range");
        }
    }

    private void init(ThreadGroup var1, Runnable var2, String var3, long var4) {
        //this.init(var1, var2, var3, var4, (AccessControlContext)null, true);
    }

    private void init111(ThreadGroup var1, Runnable var2, String var3, long var4) throws Exception {
        Thread.currentThread().getContextClassLoader().loadClass("");
        Class var6 = Class.forName("io.javadebug.agent.WeaveSpy");
        var6.getDeclaredMethod("checkBlock", String.class).invoke((Object)null, "ok");
        //this.init(var1, var2, var3, var4, (AccessControlContext)null, true);
    }


    private static void loadClass(long var0, int var2) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> weaveClazz = Class.forName("io.javadebug.agent.WeaveSpy");
        weaveClazz.getDeclaredMethod("checkBlock", String.class).invoke(null, "ok");

        if (var0 < 0L) {
            throw new IllegalArgumentException("timeout value is negative");
        } else if (var2 >= 0 && var2 <= 999999) {
            if (var2 >= 500000 || var2 != 0 && var0 == 0L) {
                ++var0;
            }

            sleep(var0);
        } else {
            throw new IllegalArgumentException("nanosecond timeout value out of range");
        }
    }

    private static void check(String threadTypePattern, String threadNamePattern) {
        boolean blockCheckPass = true;
        // thread name check
        if (java.lang.Thread.currentThread().getName().contains(threadNamePattern)) {
            blockCheckPass = false;
        }
        // class type check
        if (blockCheckPass) {
            for (Class ic : java.util.Optional.ofNullable(java.lang.Thread.currentThread().getClass().getInterfaces()).orElse(new Class[]{})) {
                // maybe : reactor.core.scheduler.NonBlocking
                if (ic.getName().contains(threadTypePattern)) {
                    blockCheckPass = false;
                }
            }
        }
        // thread stack check
        if (!blockCheckPass) {
            Map<String, String> ignoreMap = new HashMap<>();
            ignoreMap.put("a", "a");
            ignoreMap.put("b", "b");
            // check the stack
            java.lang.StackTraceElement[] stackTrace = java.lang.Thread.currentThread().getStackTrace();
            for (int i = 0; i < 10 && i < stackTrace.length; i ++) {
                java.lang.StackTraceElement ste = stackTrace[i];
                String ignore = ignoreMap.get(ste.getClassName());
                if (!(ignore == null || ignore.isEmpty()) && ignore.contains(ste.getMethodName())) {
                    PSLogger.error("this is a ignore blocking method call : " + ste.getClassName() + "." + ste.getMethodName());
                    blockCheckPass = false;
                    break;
                }
            }
        }
        if (!blockCheckPass) {
            throw new IllegalStateException("thread are blocking, which is not supported in thread :"
                                                    + Thread.currentThread().getName());
        }
    }

    private static boolean isInNonBlockingThread() {
        boolean blockCheckPass = true;
        Class<?>[] cls = Thread.currentThread().getClass().getInterfaces();
        if (cls != null && cls.length != 0) {
            for (Class<?> ic : cls) {
                // maybe : reactor.core.scheduler.NonBlocking
                if (ic.getName().contains("NonBlocking")) {
                    blockCheckPass = false;
                }
            }
        }
        if (!blockCheckPass) {
            throw new IllegalStateException("thread are blocking, which is not supported in thread :"
                                                    + Thread.currentThread().getName());
        }

        return true;
    }

    private void print() {
        System.out.println("hello world");
    }

    private static void call() {
        if (isInNonBlockingThread()) {
            throw new IllegalArgumentException("throw");
        }
    }

    public static void sleep1(long millis, int nanos)
            throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                    "nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        sleep(millis);
    }

    public static void sleep(long var0, int var2) throws InterruptedException {
        boolean var3 = true;
        Class[] var4 = Thread.currentThread().getClass().getInterfaces();
        if (var4 != null && var4.length != 0) {
            Class[] var5 = var4;
            int var6 = var4.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                Class var8 = var5[var7];
                if (var8.getName().contains("NonBlocking")) {
                    var3 = false;
                }
            }
        }

        if (!var3) {
            throw new IllegalStateException("thread are blocking, which is not supported in thread :" + Thread.currentThread().getName());
        } else if (var0 < 0L) {
            throw new IllegalArgumentException("timeout value is negative");
        } else if (var2 >= 0 && var2 <= 999999) {
            if (var2 >= 500000 || var2 != 0 && var0 == 0L) {
                ++var0;
            }

            sleep(var0);
        } else {
            throw new IllegalArgumentException("nanosecond timeout value out of range");
        }
    }

    public static void sleep3(long var0, int var2) throws InterruptedException {
        if (var0 < 0L) {
            throw new IllegalArgumentException("timeout value is negative");
        } else if (var2 >= 0 && var2 <= 999999) {
            if (var2 >= 500000 || var2 != 0 && var0 == 0L) {
                ++var0;
            }

            sleep(var0);
        } else {
            throw new IllegalArgumentException("nanosecond timeout value out of range");
        }
    }

    private static void sleep(long v) {

    }

}
