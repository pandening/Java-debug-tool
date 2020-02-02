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
//  Auth : HJ


package io.javadebug.core.data;

/**
 * Created on 2019/4/19 15:00.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class ClassModel {

    private int classCompileVersion;
    private String className;
    private byte[] classBytes;

    public ClassModel(int classCompileVersion, String className, byte[] classBytes) {
        this.classCompileVersion = classCompileVersion;
        this.className = className;
        this.classBytes = classBytes;
    }

    public int getClassCompileVersion() {
        return classCompileVersion;
    }

    public void setClassCompileVersion(int classCompileVersion) {
        this.classCompileVersion = classCompileVersion;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public byte[] getClassBytes() {
        return classBytes;
    }

    public void setClassBytes(byte[] classBytes) {
        this.classBytes = classBytes;
    }

}
