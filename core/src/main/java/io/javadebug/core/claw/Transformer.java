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


package io.javadebug.core.claw;

import io.javadebug.core.exception.ObjectTransformerException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Transformer {

    private static final Map<Class<?>, Function<String, Object>> OBJECT_TRANSFORMER_FUNCTION_MAP = new HashMap<>();
    static {

        OBJECT_TRANSFORMER_FUNCTION_MAP.put(Boolean.class, new Function<String, Object>() {

            /**
             * Applies this function to the given argument.
             *
             * @param s the function argument
             * @return the function result
             */
            @Override
            public Object apply(String s) {
                return Boolean.valueOf(s);
            }
        });

        OBJECT_TRANSFORMER_FUNCTION_MAP.put(Byte.class, new Function<String, Object>() {

            /**
             * Applies this function to the given argument.
             *
             * @param s the function argument
             * @return the function result
             */
            @Override
            public Object apply(String s) {
                return Byte.valueOf(s);
            }
        });

        OBJECT_TRANSFORMER_FUNCTION_MAP.put(Character.class, new Function<String, Object>() {
            /**
             * Applies this function to the given argument.
             *
             * @param s the function argument
             * @return the function result
             */
            @Override
            public Object apply(String s) {
                if (s == null) {
                    return null;
                }
                if (s.length() == 0) {
                    return ' ';
                }
                if (s.length() > 1) {
                    throw new IllegalArgumentException("expect char but get string:" + s);
                }
                return s.charAt(0);
            }
        });

        OBJECT_TRANSFORMER_FUNCTION_MAP.put(Integer.class, new Function<String, Object>() {

            /**
             * Applies this function to the given argument.
             *
             * @param s the function argument
             * @return the function result
             */
            @Override
            public Object apply(String s) {
                return Integer.valueOf(s);
            }
        });

        OBJECT_TRANSFORMER_FUNCTION_MAP.put(Long.class, new Function<String, Object>() {

            /**
             * Applies this function to the given argument.
             *
             * @param s the function argument
             * @return the function result
             */
            @Override
            public Object apply(String s) {
                return Long.valueOf(s);
            }
        });

        OBJECT_TRANSFORMER_FUNCTION_MAP.put(String.class, new Function<String, Object>() {

            /**
             * Applies this function to the given argument.
             *
             * @param s the function argument
             * @return the function result
             */
            @Override
            public Object apply(String s) {
                return s;
            }
        });

        OBJECT_TRANSFORMER_FUNCTION_MAP.put(Float.class, new Function<String, Object>() {

            /**
             * Applies this function to the given argument.
             *
             * @param s the function argument
             * @return the function result
             */
            @Override
            public Object apply(String s) {
                return Float.valueOf(s);
            }
        });

        OBJECT_TRANSFORMER_FUNCTION_MAP.put(Double.class, new Function<String, Object>() {

            /**
             * Applies this function to the given argument.
             *
             * @param s the function argument
             * @return the function result
             */
            @Override
            public Object apply(String s) {
                return Double.valueOf(s);
            }
        });

    }

    /**
     *  trans object.
     *
     * @param origin the origin string
     * @param targetCls the target type
     * @return  the target object
     * @throws ObjectTransformerException exception
     */
    public static Object trans(String origin, Class<?> targetCls) throws ObjectTransformerException {
        Function<String, Object> function = OBJECT_TRANSFORMER_FUNCTION_MAP.get(transType(targetCls));
        if (function == null) {
            throw new ObjectTransformerException("sorry, could not get the object trans function by target class:" + targetCls.getName());
        }
        return function.apply(origin);
    }

    private static Class<?> transType(Class<?> originClass) {
        switch (originClass.getName()) {
            case "int":
                return Integer.class;
            case "char":
                return Character.class;
            case "boolean":
                return Boolean.class;
            case "short":
                return Short.class;
            case "long":
                return Long.class;
            case "float":
                return Float.class;
            case "double":
                return Double.class;
            case "byte":
                return Byte.class;
        }
        return originClass;
    }

}
