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

import io.javadebug.core.utils.UTILS;
import io.javadebug.core.exception.ClawScriptScanException;

import java.lang.reflect.Field;

public enum  RealClawImpl implements ObjectClawDefine {
    REAL_CLAW;

    @Override
    public ObjectClawDefine set(Object originObject, Field classField, String targetVal) {
        boolean accessible = true;
        try {
            if (!classField.isAccessible()) {
                accessible = false;
                classField.setAccessible(true);
            }
            Object tVal;
            // double or float
            switch (classField.getType().getName()) {
                case "float":
                case "java.lang.Float":
                    tVal = Float.valueOf(targetVal);
                    break;
                case "double":
                case "java.lang.Double":
                    tVal = Double.valueOf(targetVal);
                    break;
                    default:
                        throw new ClawScriptScanException("[claw field set] could not parse the real number:" + targetVal);
            }
            classField.set(originObject, tVal);
        } catch (Exception e) {
            throw new ClassCastException(UTILS.getErrorMsg(e));
        } finally {
            if (!accessible) {
                classField.setAccessible(false);
            }
        }
        return this;
    }

    @Override
    public void set(ClawMeta clawMeta) {

    }
}
