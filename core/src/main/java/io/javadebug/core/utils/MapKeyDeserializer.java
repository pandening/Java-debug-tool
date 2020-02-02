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

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.KeyDeserializer;
import io.javadebug.core.utils.JacksonUtils;

import java.io.IOException;
import java.util.Map;

public class MapKeyDeserializer extends KeyDeserializer {

    /**
     * Method called to deserialize a {@link Map} key from JSON property name.
     *
     */
    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        JsonNode jsonNode = JacksonUtils.readNode(key);
        JsonNode classNode = jsonNode.get(JacksonUtils.CLASS_KEY);
        Class<?> clz;
        try {
            clz = Class.forName(classNode.asText());
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        JsonNode valueNode = jsonNode.get(JacksonUtils.BASE_VALUE_NODE_KEY);
        if (clz.equals(Integer.class)) {
            return Integer.parseInt(valueNode.asText());
        } else if (clz.equals(Boolean.class)) {
            return Boolean.parseBoolean(valueNode.asText());
        } else if (clz.equals(Byte.class)) {
            return Byte.parseByte(valueNode.asText());
        } else if (clz.equals(Short.class)) {
            return Short.parseShort(valueNode.asText());
        } else if (clz.equals(Long.class)) {
            return Long.parseLong(valueNode.asText());
        } else if (clz.equals(Float.class)) {
            return Float.parseFloat(valueNode.asText());
        } else if (clz.equals(Double.class)) {
            return Double.parseDouble(valueNode.asText());
        } else if (clz.equals(String.class)) {
            return valueNode.asText();
        } else if (clz.isEnum()) {
            return JacksonUtils.deserialize(valueNode.toString(), clz);
        } else if (clz.isArray()) {
            return JacksonUtils.deserialize(valueNode.toString(), clz);
        }
        return JacksonUtils.deserialize(key, clz);
    }
}
