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


package io.javadebug.core.utils;

import io.javadebug.core.log.PSLogger;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class ObjectUtils {

    // whether print object as json
    public static volatile boolean objectToJson = true;

    // whether force print object as json
    public static volatile boolean forceToJson = true;

    /**
     *
     * @see     java.lang.Boolean#TYPE      | bool
     * @see     java.lang.Character#TYPE    | char
     * @see     java.lang.Byte#TYPE         | byte
     * @see     java.lang.Short#TYPE        | short
     * @see     java.lang.Integer#TYPE      | int
     * @see     java.lang.Long#TYPE         | long
     * @see     java.lang.Float#TYPE        | float
     * @see     java.lang.Double#TYPE       | double
     * @see     java.lang.Void#TYPE         | void
     *
     */
    private static final Set<String> primitiveTypeSet = new HashSet<>();
    static {
        primitiveTypeSet.addAll(Arrays.asList(Boolean.class.getName(), Character.class.getName(), Byte.class.getName(),
                Short.class.getName(), Integer.class.getName(), Long.class.getName(), Float.class.getName(),
                Double.class.getName(), Void.class.getName()));
    }

    /**
     *  将对象打印出来（单个field）
     *
     * @param obj 对象本身
     * @return
     */
    public static String printObjectToString(Object obj) {

        // check force tag
        if (forceToJson) {
            try {
                return JacksonUtils.serialize(obj);
            } catch (Exception e) {
                // ignore, fallback
                PSLogger.error("error while call JacksonUtils.serialize with error: " + UTILS.getErrorMsg(e));
                return obj.toString();
            }
        }


        // 如果是null，则直接返回'NULL'
        if (obj == null) {
            return "NULL";
        }

        // 是否是String类型
        if (obj instanceof String) {
            return (String) obj;
        }

        // 是否是枚举类型
        if (obj.getClass().isEnum()) {
            return obj.toString();
        }

        // 是否实现了toString方法
        if (isInheritedToStringMethod(obj.getClass())) {
            return obj.toString();
        }

        // 基本数据类型
        if (isPrimitive(obj.getClass())) {
            return String.valueOf(obj);
        }

        // 将对象打印成json格式
        StringBuilder result = new StringBuilder();
        printObjectToJson(obj, result);

        // 返回结果
        return result.toString();
    }

    /**
     *  递归的打印对象信息
     *
     * @param obj
     */
    private static void printObjectToJson(Object obj, StringBuilder result) {
        if (!objectToJson && !forceToJson) {
            result.append(obj.toString());
            return;
        }
        if (/*(obj instanceof Serializable) ||*/ obj.getClass().getName().startsWith("java.")
                || obj.getClass().getName().startsWith("sun.")) {
            result.append(obj.toString());
            return;
        }
        try {
            String ret = JacksonUtils.serialize(obj);
            if (UTILS.isNullOrEmpty(ret)) {
                ret = "!-!";
            }
            result.append(ret);
        } catch (Throwable e) {
            String error = "无法将对象转换成json，遇到无法解决的异常:" + UTILS.getErrorMsg(e);
            PSLogger.error(error);
            result.append(obj.toString());
        }
    }

    /**
     *  是不是原始类型
     *

     *
     * @param cls
     * @return
     */
    private static boolean isPrimitive(Class<?> cls) {
        return cls != null && primitiveTypeSet.contains(cls.getName());
    }

    /**
     *  该类是否自己实现了 {@link Object#toString()} 方法
     *
     * @param cls 类
     * @return true则代表这个类已经实现了toString方法，直接用即可
     */
    private static boolean isInheritedToStringMethod(Class<?> cls) {
        return Arrays.stream(cls.getDeclaredMethods()).anyMatch(new Predicate<Method>() {
            @Override
            public boolean test(java.lang.reflect.Method method) {
                return "toString".equals(method.getName());
            }
        });
    }

}
