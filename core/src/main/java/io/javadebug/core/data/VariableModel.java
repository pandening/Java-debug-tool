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


package io.javadebug.core.data;

import io.javadebug.core.utils.ObjectUtils;
import io.javadebug.core.utils.UTILS;

public class VariableModel {

    public VariableModel(Object var, String varName) {
        this.var = var;
        this.varName = varName;
    }

    public Object getVar() {
        return var;
    }

    public void setVar(Object var) {
        this.var = var;
    }

    public String getVarName() {
        return varName;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    @Override
    public String toString() {
        String varStringVal = ObjectUtils.printObjectToString(var);
        if (UTILS.isNullOrEmpty(varName)) {
            if (var == null) {
                return varStringVal;
            } else {
                return var.getClass().getName() + "@" + varStringVal;
            }
        } else {
            return varName + " = " + varStringVal;
        }
    }

    private Object var;
    private String varName;

}
