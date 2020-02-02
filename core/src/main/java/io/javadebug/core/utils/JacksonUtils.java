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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.javadebug.core.exception.SerializationException;

import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL;

@SuppressWarnings("all")
public enum JacksonUtils {
    JACKSON_UTILS;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectMapper originMapper = new ObjectMapper();

    public static final String CLASS_KEY = "@class";
    public static final String BASE_VALUE_KEY = "\"value\"";
    public static final String BASE_VALUE_NODE_KEY = "value";

    static {
        SimpleModule module = new SimpleModule();
        mapper.enableDefaultTypingAsProperty(NON_FINAL, CLASS_KEY);
        module.setKeyDeserializers(new MapKeyDeserializers());
        module.addKeySerializer(Object.class, new MapKeySerializer());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.registerModule(module);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    public static String serialize(Object obj) throws SerializationException {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Throwable t) {
            throw new SerializationException(t);
        }
    }

    public static <T> T deserialize(String jsonString, Class<T> clazz) throws
            SerializationException {
        try {
            return mapper.readValue(jsonString, clazz);
        } catch (Throwable t) {
            throw new SerializationException(t);
        }
    }

    public static JsonNode readNode(String jsonString) throws SerializationException {
        try {
            return mapper.readTree(jsonString);
        } catch (Throwable t) {
            throw new SerializationException(t);
        }
    }

    public ObjectMapper getOriginMapper() {
        return originMapper;
    }
}
