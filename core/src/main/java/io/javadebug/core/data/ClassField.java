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


package io.javadebug.core.data;

import io.javadebug.core.utils.ObjectUtils;

import java.lang.reflect.Field;

public class ClassField {

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getFieldVal() {
        return fieldVal;
    }

    public void setFieldVal(String fieldVal) {
        this.fieldVal = fieldVal;
    }

    @Override
    public String toString() {
        return "ClassField:\n"
                +"     |_ " + fieldName + "[" + fieldVal + "]" + "@" + fieldType;
    }

    private String fieldName;
    private String access;
    private String fieldType;
    private String fieldVal;

    // raw data
    private Field field;
    private boolean isStatic;
    private String desc;
    private Object fieldValInObj;

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Object getFieldValInObj() {
        return fieldValInObj;
    }

    /**
     *  NOTICE:
     *  this method will run in user code's thread.
     *
     * @param fieldValInObj the origin object.
     */
    public void setFieldValInObj(Object fieldValInObj, boolean dumpObj) {
        this.fieldValInObj = fieldValInObj;
        try {
            if (fieldValInObj == null) {
                this.fieldVal = "NULL";
                return;
            }
            if (!dumpObj) {
                this.fieldVal = fieldValInObj.toString();
                return;
            }

            // this is ugly ..., do the object dump on another thread may could not get the 'snapshot' of the class.
            // This is the result of compromise.
            this.fieldVal = ObjectUtils.printObjectToString(fieldValInObj);
        } catch (Exception e) {
            this.fieldVal = fieldValInObj.toString();
        }
    }

    public ClassField copy() {
        ClassField classField = new ClassField();

        classField.setFieldName(fieldName);
        classField.setFieldType(fieldType);
        classField.setFieldVal(fieldVal);

        return classField;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }
}
