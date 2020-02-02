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

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

public class MethodLineRangeEnhancer implements ClassFileTransformer {

    private static final boolean _dump_class_to_debug_tag = false;

    private MethodDesc methodDesc;

    private MethodLineRangeWeaver methodLineRangeWeaver;

    private byte[] preEnhanceBytes;

    public MethodLineRangeEnhancer(MethodDesc methodDesc, byte[] preEnhanceBytes) {
        this.methodDesc = methodDesc;
        this.preEnhanceBytes = preEnhanceBytes;
    }

    /**
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
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        // check
        if (UTILS.isNullOrEmpty(this.methodDesc.getClassName()) || !this.methodDesc.getClassName().replace(".", "/").equals(className)) {
            //PSLogger.error("not match or null => [" + this.methodDesc.getClassName() + "]  VS [" + className + "]");
            return classfileBuffer;
        }

        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(cr, COMPUTE_FRAMES | COMPUTE_MAXS);

        methodLineRangeWeaver = new MethodLineRangeWeaver(Opcodes.ASM5, cw, methodDesc);
        dumpClass(cw, className + "_LR");

        // get the line range
        cr.accept(methodLineRangeWeaver, EXPAND_FRAMES);

        if (preEnhanceBytes != null && preEnhanceBytes.length > 0) {
            classfileBuffer = preEnhanceBytes;
        }

        return classfileBuffer;
    }

    /**
     *  在调试等场景下，需要将增强之后的类dump观察一下
     *
     * @param cw {@link ClassWriter}
     */
    private void dumpClass(ClassWriter cw, String cls) {
        if (_dump_class_to_debug_tag) {
            String path = "./debug-dump-dir/" + cls + ".class";
            File pathFile = new File(path);
            final File classPath = new File(pathFile.getParent());
            if (!classPath.mkdirs() && !classPath.exists()) {
                PSLogger.error("could not create path:" + classPath.getAbsolutePath());
                return;
            }
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(path);
                out.write(cw.toByteArray());

                PSLogger.error("success dump the new bytecode to:"  + path);
            } catch (Exception e) {
                PSLogger.error("error occ:" + e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        PSLogger.error("error occ:" + e);
                    }
                }
            }
        }
    }

    /**
     *  get the left range
     *
     * @return start line no
     */
    public int getLeftRange() {
        if (methodLineRangeWeaver == null) {
            return -1;
        }
        return methodLineRangeWeaver.getLineRangeL();
    }

    /**
     *  the right line no
     *
     * @return end line no
     */
    public int getRightRange() {
        if (methodLineRangeWeaver == null) {
            return -1;
        }
        return methodLineRangeWeaver.getLineRangeR();
    }

}
