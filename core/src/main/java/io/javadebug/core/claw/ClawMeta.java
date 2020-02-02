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

import io.javadebug.core.log.PSLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ClawMeta {

    // the object claw
    private ObjectClawDefine clawDefine;

    // the target object
    private Object object;

    // the target class Field
    private Field classField;

    // the val in string
    private String val;

    // the target params
    private Object[] params;

    // the target method
    private Method targetMethod;

    // the params order
    private int paramOrder = -1;

    void doSet() {
        if (clawDefine == null) {
            PSLogger.error("the object claw is null");
            return;
        }

        if (object == null) {
            PSLogger.error("the target object is null");
            return;
        }

        if (classField == null) {
            PSLogger.error("the class field is null");
            return;
        }

        // do file set here
        clawDefine.set(object, classField, val);

        // set again
        clawDefine.set(this);
    }

    public void setClawDefine(ObjectClawDefine clawDefine) {
        this.clawDefine = clawDefine;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public Object getObject() {
        return object;
    }

    public void setClassField(Field classField) {
        this.classField = classField;
    }

    public Field getClassField() {
        return this.classField;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    public int getParamOrder() {
        return paramOrder;
    }

    public void setParamOrder(int paramOrder) {
        this.paramOrder = paramOrder;
    }

    public Method getTargetMethod() {
        return targetMethod;
    }

    public void setTargetMethod(Method targetMethod) {
        this.targetMethod = targetMethod;
    }
}
