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
//  Auth : HJ


package io.javadebug.core.utils;

import io.javadebug.core.Constant;
import io.javadebug.core.log.PSLogger;
import org.objectweb.asm.Attribute;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created on 2019/4/17 15:34.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public final class UTILS {

    /**
     *  判断一个类是否需要重新定义一下
     *
     * @param clsBytes 类字节码数据
     * @return false代表不需要重新加载
     */
    public static boolean needToReload(byte[] clsBytes) {
        if (clsBytes == null || clsBytes.length == 0) {
            return false;
        }
        if (compareWithTargetJavaVersion(clsBytes) != 0) {
            /// 不能被加载
            PSLogger.error("需要重定义的类编译版本和目标JVM运行java版本不一致，拒绝执行");
            return false;
        }

        return true;
    }

    /**
     *  判断一个类的字节码是否可以更新
     *
     * @param className 类名称
     * @param newBytes 新的字节码
     * @return true则代表可以
     */
    public static boolean allowToReloadClass(String className, byte[] newBytes) {
        if (newBytes == null || newBytes.length == 0) {
            return false;
        }
        if (compareWithTargetJavaVersion(newBytes) != 0) {
            /// 不能被加载
            throw new IllegalStateException(String.format("类[%s]需要重定义的类编译版本和目标JVM运行java版本不一致，拒绝执行", className));
        }

        return true;
    }

    /**
     *  这个方法的职责是对输入的两个二进制数据进行比对，如果两份数据相同，则输出true，否则
     *  输出false，目前仅实现功能，后续优化时间
     *
     *  NOTICE：
     *          这个方法待定  ..>..
     *
     * @param b1 第一份数据
     * @param b2 第二份数据
     * @return 比对结果
     */
    public static boolean binDiff(byte[] b1, byte[] b2) {
        if (b1 == null) {
            return b2 == null;
        }
        if (b2 == null) {
            return false;
        }
        if (b1.length != b2.length) {
            return false; // 长度都不一样，肯定不是一份
        }
        int len = b1.length;
        /// he..he ..>..
        for (int i = 0; i < len; i ++) {
            if (Byte.compare(b1[i], b2[i]) != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     *  用于判断当前加载的字节码和目标JVM所使用的版本的关系，该方法用来校验加载的
     *  字节码是否可以在目标jvm执行，这个校验比较重要，比如低版本编译的字节码可能
     *  在高版本jvm中就无法执行
     *
     *  targetJvm - sourceCompileVersion
     *
     *  => = 0   : 版本匹配
     *  => > 0   : 目标JVM的版本更高
     *  => < 0   : 加载的字节码的编译版本过高
     *
     * @param source 加载的字节码数据
     * @return 比较结果 {@link Double#compare(double, double)}
     */
    public static int compareWithTargetJavaVersion(byte[] source) {
        String version = System.getProperty("java.class.version");
        double vv = safeParseDouble(version);
        return Double.compare(vv, readClassVersion(source));
    }

    /**
     *  用来读取Class文件的编译版本
     *
     * @param source {@link UTILS#loadFile(String, String)}
     * @return {@link Constant#V1_1 ...}
     */
    public static short readClassVersion(byte[] source) {
        if (isNullObject(source) || source.length == 0) {
            throw new NullPointerException("请提供合法的数据");
        }
        return readShort(source, 6);
    }

    /**
     * Reads a signed short value in {@code src}. <i>This method is intended
     * for {@link Attribute} sub classes, and is normally not needed by class
     * generators or adapters.</i>
     *
     * @param index
     *            the start index of the value to be read in {@code src}.
     * @return the read value.
     */
    private static short readShort(byte[] src, final int index) {
        return (short) (((src[index] & 0xFF) << 8) | (src[index + 1] & 0xFF));
    }

    public static byte[] loadFile(String filePath, String suffix) throws Exception {
        if (isNullOrEmpty(filePath) || isNullOrEmpty(suffix) || !filePath.endsWith(suffix)) {
            throw new NullPointerException("不合法的文件路径");
        }
        File file = new File(filePath);
        InputStream is = new FileInputStream(file);
        long length = file.length();
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead;
        while (offset <bytes.length &&
                (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "
                    + file.getName());
        }
        is.close();
        return bytes;
    }

    static <T> String toS(T origin) {
        return String.valueOf(origin);
    }

    public static String getErrorMsg(Throwable throwable) {
        if (isNullObject(throwable)) {
            return "call npe";
        }
        if (throwable.getCause() != null) {
            return getErrorMsg(throwable.getCause());
        }
        String msg = throwable.getMessage();
        if (isNullOrEmpty(msg)) {
            return "empty error msg";
        }
        return msg;
    }

    public static boolean isBlank(CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String format(String format, Object ... args) {
        if (isNullOrEmpty(format)) {
            return "";
        }
        if (isNullOrEmptyArray(args)) {
            return format;
        }
        return String.format(format, args);
    }

    public static String nullToEmpty(String msg) {
        return isNullOrEmpty(msg) ? "" : msg;
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static <T> boolean isNullObject(T obj) {
        return obj == null;
    }

    public static <T> boolean isNullOrEmptyArray(Object[] arr) {
        return arr == null || arr.length == 0;
    }

    public static int safeParseInt(String origin, int defaultVal) {
        return (int) safeParseLong(origin, defaultVal);
    }

    public static long safeParseLong(String origin, int defaultVal) {
        try {
            return Long.parseLong(origin);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    static double safeParseDouble(String origin) {
        try {
            return Double.parseDouble(origin);
        } catch (Exception e) {
            return 0d;
        }
    }

}
