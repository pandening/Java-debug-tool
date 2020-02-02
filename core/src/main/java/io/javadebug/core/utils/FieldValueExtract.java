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

package io.javadebug.core.utils;

import io.javadebug.core.data.ClassField;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.security.ThreadSafeKeeper;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.javadebug.core.Constant.DUMP_OBJECT_OPTIONS;

/**
 *  in this way, the console will get dirty field val.
 *
 *  you must weave bytecode to get the class 'snapshot'.
 */
public class FieldValueExtract {

    // cache the class's field map
    private static final ConcurrentMap<Class<?>, Set<ClassField>> classFieldsCacheMap = new ConcurrentHashMap<>();

    /**
     *  first query from cache, then get the cache in another thread.
     *  do not block the target jvm's thread.
     *
     * @param cls the target class
     * @return the result
     */
    public static Set<ClassField> getClassFields(Class<?> cls) {
        Set<ClassField> retSet = classFieldsCacheMap.get(cls);
        if (retSet == null) {
            PSLogger.error(String.format("the class:[%s] field has not been parse, check next time.", cls.getName()));
            Set<ClassField> classFields = new HashSet<>();
            for (Field field : cls.getDeclaredFields()) {
                ClassField classField = new ClassField();
                boolean isStatic = false;
                if (Modifier.isStatic(field.getModifiers())) {
                    isStatic = true;
                }
                classField.setField(field);
                classField.setStatic(isStatic);
                classField.setFieldName(field.getName());
                classField.setDesc(Type.getDescriptor(field.getType()));
                classField.setFieldType(field.getType().getName());

                classFields.add(classField);
            }
            classFieldsCacheMap.putIfAbsent(cls, classFields);
            PSLogger.error("cached the field of class:" + cls.getName());
        }
        // anyway, query again to check whether the job is complete, if not, this round
        // will could not get the field information of the class.
        retSet = classFieldsCacheMap.get(cls);
        return retSet;
    }

    /**
     *  get the class Fields directly
     *
     * @param o the target object
     * @return the class fields set
     */
    public static List<ClassField> extractFields(Object o) {
        Set<ClassField> classFields = getClassFields(o.getClass());
        if (classFields == null || classFields.isEmpty()) {
            return Collections.emptyList();
        }
        List<ClassField> result = new ArrayList<>();
        for (ClassField classField : classFields) {
            boolean restore = false;
            try {
                if (!classField.getField().isAccessible()) {
                    restore = true;
                    classField.getField().setAccessible(true);
                }
                classField.setFieldValInObj(classField.getField().get(o), DUMP_OBJECT_OPTIONS);
                result.add(classField.copy());
            } catch (Exception e) {
                PSLogger.error("ignore the fields cause occ error:" + UTILS.getErrorMsg(e));
            } finally {
                if (restore) {
                    classField.getField().setAccessible(false);
                }
            }
        }
        return result;
    }

    /**
     *  extract all fields' value declared in the class {#obj.getClass}
     *
     *  NOTICE:
     *     THIS METHOD MUST BE CALLED IN JAVA-DEBUG-TOOL'S THREAD, DO NOT
     *     LET THIS METHOD RUN AT TARGET JVM.
     *
     * @param obj the object
     * @return map
     * @throws Exception any exception occ.
     */
    @Deprecated
    public static List<ClassField> extractFields(Object obj, boolean printProtected, boolean printPrivate,
                                                 boolean printStatic) throws Exception {
        boolean isSafe = ThreadSafeKeeper.isSafe();
        if (!isSafe) {
            return Collections.emptyList();
        }

        if (obj == null) {
            PSLogger.error("the param obj is null");
            return Collections.emptyList();
        }

        Class<?> cls = obj.getClass();

        List<ClassField> classFieldList = new ArrayList<>();

        for (Field field : cls.getDeclaredFields()) {

            if (!printPrivate) {
                if (Modifier.isPrivate(field.getModifiers())) {
                    continue;
                }
            }
            if (!printProtected) {
                if (Modifier.isProtected(field.getModifiers())) {
                    continue;
                }
            }
            if (!printStatic) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
            }

            ClassField classField = buildOneField(obj, field);

            if (classField != null) {
                classFieldList.add(classField);
            }
        }

        return classFieldList;
    }

    private static ClassField buildOneField(Object obj, Field field) {
        try {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            ClassField classField = new ClassField();
            classField.setAccess(printFieldModifier(field.getModifiers()));
            classField.setFieldName(field.getName());
            classField.setFieldType(field.getType().getName());
            // val
            Object val;
            if (Modifier.isStatic(field.getModifiers())) {
                val = field.get(null);
            } else {
                val = field.get(obj);
            }
            if (val == null) {
                classField.setFieldVal("NULL");
            } else {
                classField.setFieldVal(ObjectUtils.printObjectToString(val));
            }
            return classField;
        } catch (Exception e) {
            PSLogger.error("error occ when extract field:" + field + " of:" + obj);
            return null;
        } finally {
            if (!field.isAccessible()) {
                field.setAccessible(false);
            }
        }
    }

    // the field's modifier
    private static final int[] modifierArray = {Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE,
            Modifier.STATIC, Modifier.FINAL, Modifier.TRANSIENT,
            Modifier.VOLATILE};

    private static String printFieldModifier(int mod) {
        StringBuilder retSb = new StringBuilder();

        if (Modifier.isPrivate(mod)) {
            retSb.append("private").append(" ");
        }

        if (Modifier.isProtected(mod)) {
            retSb.append("protected").append(" ");
        }

        if (Modifier.isPublic(mod)) {
            retSb.append("public").append(" ");
        }

        if (Modifier.isStatic(mod)) {
            retSb.append("static").append(" ");
        }

        if (Modifier.isFinal(mod)) {
            retSb.append("final").append(" ");
        }

        if (Modifier.isTransient(mod)) {
            retSb.append("transient").append(" ");
        }

        return retSb.toString();
    }

}
