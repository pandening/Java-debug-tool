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


public class ParamModel {

    public ParamModel() {

    }

    public ParamModel(int intVal, double doubleVal) {
        this.intVal = intVal;
        this.doubleVal = doubleVal;
    }

    public int getIntVal() {
        return intVal;
    }

    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public double getDoubleVal() {
        return doubleVal;
    }

    public void setDoubleVal(double doubleVal) {
        this.doubleVal = doubleVal;
    }

    @Override
    public String toString() {
        return "ParamModel{" +
                       "intVal=" + intVal +
                       ", doubleVal='" + doubleVal + '\'' +
                       '}';
    }

    private int intVal;
    private double doubleVal;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ParamModel)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (((ParamModel) obj).getIntVal() != intVal) {
            return false;
        }
        if (doubleVal != ((ParamModel) obj).getDoubleVal()) {
            return false;
        }
        return true;
    }
}
