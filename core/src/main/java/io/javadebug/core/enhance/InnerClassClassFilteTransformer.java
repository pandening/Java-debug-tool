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


package io.javadebug.core.enhance;

import io.javadebug.core.utils.UTILS;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.List;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;

public class InnerClassClassFilteTransformer implements ClassFileTransformer {

    private String targetClassName;
    private List<String> resultList;
    private byte[] preEnhanceBytes;

    public InnerClassClassFilteTransformer(String targetClassName, List<String> resultList,
                                           byte[] preEnhanceBytes) {
        this.targetClassName = targetClassName;
        this.resultList = resultList;
        this.preEnhanceBytes = preEnhanceBytes;
    }

    /**
     * The implementation of this method may transform the supplied class file and
     * return a new replacement class file.
     *
     * @param loader              the defining loader of the class to be transformed,
     *                            may be <code>null</code> if the bootstrap loader
     * @param className           the name of the class in the internal form of fully
     *                            qualified class and interface names as defined in
     *                            <i>The Java Virtual Machine Specification</i>.
     *                            For example, <code>"java/util/List"</code>.
     * @param classBeingRedefined if this is triggered by a redefine or retransform,
     *                            the class being redefined or retransformed;
     *                            if this is a class load, <code>null</code>
     * @param protectionDomain    the protection domain of the class being defined or redefined
     * @param classfileBuffer     the input byte buffer in class file format - must not be modified
     * @return a well-formed class file buffer (the result of the transform),
     * or <code>null</code> if no transform is performed.
     * @throws IllegalClassFormatException if the input does not represent a well-formed class file
     * @see Instrumentation#redefineClasses
     */
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {

        // check
        if (UTILS.isNullOrEmpty(this.targetClassName) || !this.targetClassName.replace(".", "/").equals(className)) {
            return classfileBuffer;
        }

        ClassReader cr = new ClassReader(classfileBuffer);

        cr.accept(new InnerClassWeaver(Opcodes.ASM5, resultList), EXPAND_FRAMES);

        if (preEnhanceBytes != null && preEnhanceBytes.length > 0) {
            classfileBuffer = preEnhanceBytes;
        }

        return classfileBuffer;
    }
}
