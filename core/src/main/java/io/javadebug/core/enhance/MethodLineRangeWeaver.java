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
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;

public class MethodLineRangeWeaver extends ClassVisitor implements Opcodes {

    // the target method desc
    private MethodDesc sMethodDesc;

    /**
     * Constructs a new {@link ClassVisitor}.
     *
     * @param api the ASM API version implemented by this visitor. Must be one
     *            of {@link Opcodes#ASM4}, {@link Opcodes#ASM5} or {@link Opcodes#ASM6}.
     * @param cv  the class visitor to which this visitor must delegate method
     */
    public MethodLineRangeWeaver(int api, ClassVisitor cv, MethodDesc sMethodDesc) {
        super(api, cv);

        this.sMethodDesc = sMethodDesc;
    }

    // ------- the method range [lineRangeL, lineRangeR] -------

    public int getLineRangeL() {
        return lineRangeL;
    }

    public int getLineRangeR() {
        return lineRangeR;
    }

    // the left range
    private int lineRangeL = -1;

    // the right range
    private int lineRangeR = -1;



    /**
     *  看看方法是否匹配，当然需要把一些特殊的方法去掉，不让你观察之
     *
     * @param access {@link Opcodes#ACC_ABSTRACT ...}
     * @param name 方法名称
     * @param desc 方法描述
     * @return 是否匹配
     */
    private boolean isMatchMethod(int access, String name, String desc) {
        // abstract method do not allow to enhance
        if ((ACC_ABSTRACT & access) == ACC_ABSTRACT) {
            return false;
        }

        // init, cinit
        if (name.equals("<clinit>") || name.equals("<init>")) {
            return false;
        }

        if (!sMethodDesc.isMatch(name, desc)) {
            return false;
        }

        // custom method matcher
        return sMethodDesc != null && sMethodDesc.isMatch(name, desc);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        // 方法是否匹配
        if (mv == null || !isMatchMethod(access, name, desc)) {
            return mv;
        }

        try {
            return new AdviceAdapter(api, new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions), access, name, desc) {
                /**
                 * Visits a line number declaration.
                 *
                 * @param line  a line number. This number refers to the source file from
                 *              which the class was compiled.
                 * @param start the first instruction corresponding to this line number.
                 * @throws IllegalArgumentException if <tt>start</tt> has not already been visited by this
                 *                                  visitor (by the {@link #visitLabel visitLabel} method).
                 */
                @Override
                public void visitLineNumber(int line, Label start) {
                    super.visitLineNumber(line, start);
                    if (lineRangeL < 0) {
                        lineRangeL = line;
                        PSLogger.error("get the left range of method : " + sMethodDesc);
                    }
                    lineRangeR = line;
                }
            };
        } catch (Exception e) {
            PSLogger.error("error occ when get the line scope of method : " + sMethodDesc);
        }

        return mv;
    }


}
