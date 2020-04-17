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
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.springframework.cglib.core.Local;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.javadebug.core.enhance.ClassMethodWeaver.METHOD_INVOKE_METHOD;
import static io.javadebug.core.enhance.ClassMethodWeaver.METHOD_Type;
import static io.javadebug.core.enhance.ClassMethodWeaver.WEAVE_SPY_TYPE;

public class ThreadBlockHoundWeaver extends ClassVisitor implements Opcodes {

    private Class<?> currentEnhanceClass;
    private Set<MethodDesc> containsMethodSet;
    private String nonBlockingThreadNamePattern;

    /**
     * Constructs a new {@link ClassVisitor}.
     *
     * @param api the ASM API version implemented by this visitor. Must be one
     *            of {@link Opcodes#ASM4}, {@link Opcodes#ASM5} or {@link Opcodes#ASM6}.
     */
    public ThreadBlockHoundWeaver(int api, Set<MethodDesc> methodSet, Class<?> cls, String threadNamePattern) {
        super(api);

        this.containsMethodSet = methodSet;
        this.currentEnhanceClass = cls;
        this.nonBlockingThreadNamePattern = threadNamePattern;
    }

    /**
     * Constructs a new {@link ClassVisitor}.
     *
     * @param api the ASM API version implemented by this visitor. Must be one
     *            of {@link Opcodes#ASM4}, {@link Opcodes#ASM5} or {@link Opcodes#ASM6}.
     * @param cv  the class visitor to which this visitor must delegate method
     */
    public ThreadBlockHoundWeaver(int api, ClassVisitor cv, Set<MethodDesc> methodSet, Class<?> cls, String threadNamePattern) {
        super(api, cv);

        this.containsMethodSet = methodSet;
        this.currentEnhanceClass = cls;
        this.nonBlockingThreadNamePattern = threadNamePattern;
        if (UTILS.isNullOrEmpty(nonBlockingThreadNamePattern)) {
            this.nonBlockingThreadNamePattern = "";
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        // check is the method is need to enhance
        if (!needToEnhance(access, name, desc)) {
            return mv;
        }

        // the thread name pattern
        final String threadNamePattern = nonBlockingThreadNamePattern;

        // enhance the method
        try {
            return new AdviceAdapter(ASM5, new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions), access, name, desc) {

                @Override
                protected void onMethodEnter() {
                    super.onMethodEnter();

                    mv.visitLdcInsn(threadNamePattern);
                    mv.visitMethodInsn(INVOKESTATIC, "io/javadebug/agent/WeaveSpy", "checkBlock", "(Ljava/lang/String;)V", false);

//                    Label l0 = new Label();
//                    mv.visitLabel(l0);
//                    mv.visitLdcInsn("io.javadebug.agent.WeaveSpy");
//                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
//                    int i0 = newLocal(Type.getType(int.class));
//                    mv.visitVarInsn(ASTORE, i0);
//                    Label l1 = new Label();
//                    mv.visitLabel(l1);
//                    mv.visitVarInsn(ALOAD, i0);
//                    mv.visitLdcInsn("checkBlock");
//                    mv.visitInsn(ICONST_1);
//                    mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
//                    mv.visitInsn(DUP);
//                    mv.visitInsn(ICONST_0);
//                    mv.visitLdcInsn(Type.getType("Ljava/lang/String;"));
//                    mv.visitInsn(AASTORE);
//                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
//                    mv.visitInsn(ACONST_NULL);
//                    mv.visitInsn(ICONST_1);
//                    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
//                    mv.visitInsn(DUP);
//                    mv.visitInsn(ICONST_0);
//                    mv.visitLdcInsn("ok");
//                    mv.visitInsn(AASTORE);
//                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
//                    mv.visitInsn(POP);

//                    /**
//                     *  check the block call.
//                     *
//                     * @param nonBlockingThreadNamePattern match the non-blocking thread name
//                     */
//                    public static void checkBlock(String nonBlockingThreadNamePattern) {
//                        if (UTILS.isNullOrEmpty(nonBlockingThreadNamePattern)) {
//                            nonBlockingThreadNamePattern = "NonBlocking";
//                        }
//                        boolean blockCheckPass = true;
//
//                        // thread name check
//                        if (Thread.currentThread().getName().contains(nonBlockingThreadNamePattern)) {
//                            blockCheckPass = false;
//                        }
//
//                        // class type check
//                        if (blockCheckPass) {
//                            Class<?>[] cls = Thread.currentThread().getClass().getInterfaces();
//                            if (cls != null && cls.length != 0) {
//                                for (Class<?> ic : cls) {
//                                    // maybe : reactor.core.scheduler.NonBlocking
//                                    if (ic.getName().contains(nonBlockingThreadNamePattern)) {
//                                        blockCheckPass = false;
//                                    }
//                                }
//                            }
//                        }
//
//                        // thread stack check
//                        if (!blockCheckPass) {
//                            Map<String, String> IGNORE_CLASS_METHOD_MAP = new HashMap<>();
//                            IGNORE_CLASS_METHOD_MAP.put("a", "a");
//                            IGNORE_CLASS_METHOD_MAP.put("b", "b");
//
//                            // check the stack
//                            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
//                            for (int i = 0; i < 10 && i < stackTrace.length; i ++) {
//                                StackTraceElement ste = stackTrace[i];
//                                String ignore = IGNORE_CLASS_METHOD_MAP.get(ste.getClassName());
//                                if (!UTILS.isNullOrEmpty(ignore) && ignore.contains(ste.getMethodName())) {
//                                    PSLogger.error("this is a ignore blocking method call : " + ste.getClassName() + "." + ste.getMethodName());
//                                    return;
//                                }
//                            }
//                            throw new IllegalStateException("thread are blocking, which is not supported in thread :"
//                                                                    + Thread.currentThread().getName());
//                        }
//                    }


//                    // load advice method
//                    getStatic(WEAVE_SPY_TYPE, "IS_IN_NON_BLOCKING_THREAD_METHOD", METHOD_Type);
//
//                    // push the first method param for Method.invoke
//                    push((Type) null);
//
//                    // load the second method params for Method.invoke
//                    push(1);
//                    newArray(Type.getType(Object.class));
//
//                    dup();
//                    push(0);
//                    push(nonBlockingThreadNamePattern);
//                    arrayStore(Type.getType(String.class));
//
//                    // do method call: Method.invoke(...)
//                    invokeVirtual(METHOD_Type, METHOD_INVOKE_METHOD);
//                    pop(); // pop the result of executing Method.invoke(...)


                    //// ===========================================================///
                    ////  Enhance the follow code in the begin of this method       ///
                    //// ===========================================================///

//                    boolean blockCheckPass = true;
//                    Class<?>[] cls = Thread.currentThread().getClass().getInterfaces();
//                    if (cls != null && cls.length != 0) {
//                        for (Class<?> ic : cls) {
//                            // maybe : reactor.core.scheduler.NonBlocking
//                            if (ic.getName().contains("NonBlocking")) {
//                                blockCheckPass = false;
//                            }
//                        }
//                    }
//                    if (!blockCheckPass) {
//                        throw new IllegalStateException("thread are blocking, which is not supported in thread :"
//                                                                + Thread.currentThread().getName());
//                    }

//                    Label l0 = new Label();
//                    mv.visitLabel(l0);
//                    mv.visitInsn(ICONST_1);
//                    int blockCheckPass = newLocal(Type.getType(int.class));
//                    mv.visitVarInsn(ISTORE, blockCheckPass);
//                    Label l1 = new Label();
//                    mv.visitLabel(l1);
//                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
//                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
//                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getInterfaces", "()[Ljava/lang/Class;", false);
//                    int cls = newLocal(Type.getType(int.class));
//                    mv.visitVarInsn(ASTORE, cls);
//                    Label l2 = new Label();
//                    mv.visitLabel(l2);
//                    mv.visitVarInsn(ALOAD, cls);
//                    Label l3 = new Label();
//                    mv.visitJumpInsn(IFNULL, l3);
//                    mv.visitVarInsn(ALOAD, cls);
//                    mv.visitInsn(ARRAYLENGTH);
//                    mv.visitJumpInsn(IFEQ, l3);
//                    Label l4 = new Label();
//                    mv.visitLabel(l4);
//                    mv.visitVarInsn(ALOAD, cls);
//                    int i2 = newLocal(Type.getType(int.class));
//                    mv.visitVarInsn(ASTORE, i2);
//                    mv.visitVarInsn(ALOAD, i2);
//                    mv.visitInsn(ARRAYLENGTH);
//                    int i3 = newLocal(Type.getType(int.class));
//                    mv.visitVarInsn(ISTORE, i3);
//                    mv.visitInsn(ICONST_0);
//                    int i4 = newLocal(Type.getType(int.class));
//                    mv.visitVarInsn(ISTORE, i4);
//                    Label l5 = new Label();
//                    mv.visitLabel(l5);
//                    mv.visitFrame(Opcodes.F_FULL, 5, new Object[]{Opcodes.INTEGER, "[Ljava/lang/Class;", "[Ljava/lang/Class;", Opcodes.INTEGER, Opcodes.INTEGER}, 0, new Object[]{});
//                    mv.visitVarInsn(ILOAD, i4);
//                    mv.visitVarInsn(ILOAD, i3);
//                    mv.visitJumpInsn(IF_ICMPGE, l3);
//                    mv.visitVarInsn(ALOAD, i2);
//                    mv.visitVarInsn(ILOAD, i4);
//                    mv.visitInsn(AALOAD);
//                    int i5 = newLocal(Type.getType(int.class));
//                    mv.visitVarInsn(ASTORE, i5);
//                    Label l6 = new Label();
//                    mv.visitLabel(l6);
//                    mv.visitVarInsn(ALOAD, i5);
//                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
//                    mv.visitLdcInsn("NonBlocking");
//                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
//                    Label l7 = new Label();
//                    mv.visitJumpInsn(IFEQ, l7);
//                    Label l8 = new Label();
//                    mv.visitLabel(l8);
//                    mv.visitInsn(ICONST_0);
//                    mv.visitVarInsn(ISTORE, blockCheckPass);
//                    mv.visitLabel(l7);
//                    //mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
//                    mv.visitIincInsn(i4, 1);
//                    mv.visitJumpInsn(GOTO, l5);
//                    mv.visitLabel(l3);
//                    //mv.visitFrame(Opcodes.F_CHOP, 3, null, 0, null);
//                    mv.visitVarInsn(ILOAD, blockCheckPass);
//                    Label l9 = new Label();
//                    mv.visitJumpInsn(IFNE, l9);
//                    Label l10 = new Label();
//                    mv.visitLabel(l10);
//                    mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
//                    mv.visitInsn(DUP);
//                    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
//                    mv.visitInsn(DUP);
//                    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
//                    mv.visitLdcInsn("thread are blocking, which is not supported in thread :");
//                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
//                    Label l11 = new Label();
//                    mv.visitLabel(l11);
//                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
//                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;", false);
//                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
//                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
//                    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V", false);
//                    mv.visitInsn(ATHROW);
//                    mv.visitLabel(l9);


                }
            };
        } catch (Throwable e) {
            // any throwable will catch, log it;
            PSLogger.error("could not enhance the method, " +
                                   currentEnhanceClass.getName() + "." + name + " " + desc, e);
        }

        return mv;
    }

    /**
     *  check whether we need to enhance the method
     *
     * @param access the access of this method
     * @param name the method name
     * @param desc the method desc
     * @return  check it, the name and desc are both necessary
     */
    private boolean needToEnhance(int access, String name, String desc) {
        // abstract method do not allow to enhance
        if ((ACC_ABSTRACT & access) == ACC_ABSTRACT) {
            return false;
        }

        // init, cinit
        if (name.equals("<clinit>") || name.equals("<init>")) {
            return false;
        }

        // the method matcher work here ~
        for (MethodDesc md : containsMethodSet) {
            if (md.isMatch(name, desc)) {
                return true;
            }
        }
        // do not enhance this method
        return false;
    }

}
