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


package io.javadebug.core.enhance;

public class ClassMethodWeaveConfig {
    private boolean args = true, ret = true, e = true, line = true,
            var = true, field = false, fieldDiff = false, sfield = false, sfd = false, stack = false;

    public boolean isArgs() {
        return args;
    }

    public void setArgs(boolean args) {
        this.args = args;
    }

    public boolean isRet() {
        return ret;
    }

    public void setRet(boolean ret) {
        this.ret = ret;
    }

    public boolean isE() {
        return e;
    }

    public void setE(boolean e) {
        this.e = e;
    }

    public boolean isLine() {
        return line;
    }

    public void setLine(boolean line) {
        this.line = line;
    }

    public boolean isVar() {
        return var;
    }

    public void setVar(boolean var) {
        this.var = var;
    }

    public ClassMethodWeaveConfig() {

    }

    ClassMethodWeaveConfig(boolean args, boolean ret, boolean e, boolean line, boolean var) {
        this.args = args;
        this.ret = ret;
        this.e = e;
        this.line = line;
        this.var = var;
    }

    public boolean isField() {
        return field;
    }

    public void setField(boolean field) {
        this.field = field;
    }

    public boolean isFieldDiff() {
        return fieldDiff;
    }

    public void setFieldDiff(boolean fieldDiff) {
        this.fieldDiff = fieldDiff;
    }

    @Override
    public String toString() {
        return "ClassMethodWeaveConfig{" +
                       "args=" + args +
                       ", ret=" + ret +
                       ", e=" + e +
                       ", line=" + line +
                       ", var=" + var +
                       ", field=" + field +
                       ", fieldDiff=" + fieldDiff +
                       '}';
    }

    public boolean isSfield() {
        return sfield;
    }

    public void setSfield(boolean sfield) {
        this.sfield = sfield;
    }

    public boolean isSfd() {
        return sfd;
    }

    public void setSfd(boolean sfd) {
        this.sfd = sfd;
    }

    public boolean isStack() {
        return stack;
    }

    public void setStack(boolean stack) {
        this.stack = stack;
    }
}
