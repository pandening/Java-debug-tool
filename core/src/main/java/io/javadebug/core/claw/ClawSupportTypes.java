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

import io.javadebug.core.data.ClassField;
import io.javadebug.core.log.PSLogger;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClawSupportTypes {

    /**
     *  dispatch to a claw impl according to {@link ClassField}, a {@link ObjectClawDefine}
     *  will be return if the fields is primitive type:
     *  {@link Byte}
     *  {@link Boolean}
     *  {@link Character}
     *  {@link Short}
     *  {@link Integer}
     *  {@link Float}
     *  {@link Double}
     *  {@link Long}
     *  {@link String}
     *
     * @param field the class filed {@link Field}
     * @return {@link ObjectClawDefine} or null if the field's type is not primitive.
     */
    public static ObjectClawDefine dispatch(Field field) {
        if (field == null) {
            PSLogger.error("the class filed must not be null here ~");
            return null;
        }

        switch (field.getType().getName()) {
            case "java.lang.String": // string
                return StringClawImpl.STRING_CLAW;
            case "java.lang.Integer": // int
            case "int":
            case "java.lang.Short": // short
            case "short":
                return IntClawImpl.INT_CLAW;
            case "java.lang.Character": // char
            case "char":
                return CharClawImpl.CHAR_CLAW;
            case "java.lang.Double": // double
            case "double":
            case "java.lang.Float": // float
            case "float":
                return RealClawImpl.REAL_CLAW;
            case "java.lang.Byte": // byte
            case "byte":
                return ByteClawImpl.BYTE_CLAW;
            case "java.lang.Boolean":// boolean
            case "boolean":
                return BooleanClawImpl.BOOLEAN_CLAW;
            case "java.lang.Long":
            case "long":
                return LongClawImpl.LONG_CLAW;
        }

        // array
        if (field.getType().isArray()) {
            return ArrayClawImpl.ARRAY_CLAW;
        }

        // list
        if (List.class.isAssignableFrom(field.getType())) {
            return ListClawImpl.LIST_CLAW;
        }

        // set
        if (Set.class.isAssignableFrom(field.getType())) {
            return SetClawImpl.SET_CLAW;
        }

        // map
        if (Map.class.isAssignableFrom(field.getType())) {
            return MapClawImpl.MAP_CLAW;
        }

        PSLogger.error("could not get the claw impl for:" + field);
        return null;
    }

    public static ObjectClawDefine dispatch(Class<?> cls) {
        // array
        if (cls.isArray()) {
            return ArrayClawImpl.ARRAY_CLAW;
        }

        // list
        if (List.class.isAssignableFrom(cls)) {
            return ListClawImpl.LIST_CLAW;
        }

        // set
        if (Set.class.isAssignableFrom(cls)) {
            return SetClawImpl.SET_CLAW;
        }

        // map
        if (Map.class.isAssignableFrom(cls)) {
            return MapClawImpl.MAP_CLAW;
        }

        return null;
    }

}
