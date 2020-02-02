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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.javadebug.core.utils.JacksonUtils;

import java.io.IOException;
import java.lang.reflect.Modifier;

public class MapKeySerializer extends JsonSerializer {

    /**
     * Method that can be called to ask implementation to serialize
     * values of type this serializer handles.
     *
     * @param value       Value to serialize; can <b>not</b> be null.
     * @param gen         Generator used to output resulting Json content
     * @param serializers Provider that can be used to get serializers for
     */
    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String keyString = JacksonUtils.serialize(value);
        boolean isFinal = Modifier.isFinal(value.getClass().getModifiers());

        StringBuilder key = new StringBuilder();
        if (isFinal) {

            key.append("{\"").append(JacksonUtils.CLASS_KEY).append("\":\"").append(value.getClass().getName()).append("\",");

            if (value instanceof Integer || value instanceof Boolean
                        || value instanceof Byte || value instanceof Short
                        || value instanceof Long || value instanceof Float
                        || value instanceof Double || value instanceof String) {
                key.append(JacksonUtils.BASE_VALUE_KEY).append(":").append(keyString);
            } else if (value instanceof Enum) {
                key.append(JacksonUtils.BASE_VALUE_KEY).append(":").append(keyString);
            } else if(value.getClass().isArray()){
                key.append(JacksonUtils.BASE_VALUE_KEY).append(":").append(keyString);
            } else if (keyString.startsWith("{")) {
                key.append(keyString.substring(1, keyString.length() - 1));
            } else {
                key.append(keyString);
            }

            key.append("}");
            keyString = key.toString();
        }

        gen.writeFieldName(keyString);
    }
}
