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

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.transport.CommandCodec;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;

import static org.apache.commons.io.IOUtils.toByteArray;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

/**
 * Created on 2019/5/10 10:40.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class MethodTraceEnhance implements CommonClassFileTransformer {

    // the method enhance
    private static MethodTraceEnhance methodTraceEnhance;

    // debug tag
    public static volatile boolean _dump_class_to_debug_tag = true;

    // the console
    private int contextId;

    // the target class to enhance
    private String className;

    private Class<?> targetClass;

    // the target method to enhance
    private String targetMethodName;

    // the target method's desc
    private String targetMethodDesc;

    // the method advice
    private MethodAdvice methodAdvice;

    // the bytecode array
    private byte[] enhancedBytes;

    // if the origin class has been enhanced, base it.
    private byte[] preEnhanceBytes;

    // the config weave configure
    private ClassMethodWeaveConfig classMethodWeaveConfig;

    public MethodTraceEnhance(int contextId, String className, String targetMethodName,
                              String targetMethodDesc,
                              byte[] preEnhanceBytes, Class<?> targetClass,
                              ClassMethodWeaveConfig classMethodWeaveConfig) {
        this.contextId = contextId;
        this.className = className;
        this.targetMethodName = targetMethodName;
        this.targetMethodDesc = targetMethodDesc;
        if (this.methodAdvice == null) {
            // just print the notice event
            PSLogger.error("the assign method advice is null, fallback.");
            this.methodAdvice = EmptyPrintMethodAdvice.EMPTY_PRINT_METHOD_ADVICE;
        }
        this.preEnhanceBytes = preEnhanceBytes;
        this.targetClass = targetClass;
        if (classMethodWeaveConfig == null) {
            classMethodWeaveConfig = new ClassMethodWeaveConfig();
            PSLogger.error("the class weave configure is null, using default configure:" + classMethodWeaveConfig);
        }
        this.classMethodWeaveConfig = classMethodWeaveConfig;
    }

//    /**
//     *  获取到enhance
//     *
//     * @return enhance
//     */
//    public static MethodTraceEnhance getMethodTraceEnhance(int contextId, String className, String targetMethodName,
//                                                           String targetMethodDesc,
//                                                           byte[] preEnhanceBytes) {
//        if (methodTraceEnhance == null) {
//            methodTraceEnhance = new MethodTraceEnhance(contextId, className, targetMethodName, targetMethodDesc, preEnhanceBytes);
//            return methodTraceEnhance;
//        } else {
//            methodTraceEnhance.contextId = contextId;
//            methodTraceEnhance.className = className;
//            methodTraceEnhance.targetMethodName = targetMethodName;
//            methodTraceEnhance.targetMethodDesc = targetMethodDesc;
//            methodTraceEnhance.preEnhanceBytes = preEnhanceBytes;
//
//            return methodTraceEnhance;
//        }
//    }


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
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {

        if (preEnhanceBytes != null && preEnhanceBytes.length > 0) {
            classfileBuffer = preEnhanceBytes;
        }

        // check
        if (UTILS.isNullOrEmpty(this.className) || !this.className.replace(".", "/").equals(className)) {
            PSLogger.error("not match or null => [" + this.className + "]  VS [" + className + "]");
            return classfileBuffer;
        }

        ClassReader cr = new ClassReader(classfileBuffer);

        ClassWriter cw  = new ClassWriter(cr, COMPUTE_FRAMES | COMPUTE_MAXS);

        MethodDesc methodDesc = new MethodDesc(this.className, targetMethodName, targetMethodDesc, targetClass);

        ClassMethodWeaver classMethodWeaver = new ClassMethodWeaver(Opcodes.ASM5, cw, methodAdvice, methodDesc, contextId, classMethodWeaveConfig);

        try {
            cr.accept(classMethodWeaver, EXPAND_FRAMES);

            // dump the weaved class
            dumpClass(cw, className);

            // load spy
            loadMethodWeave(loader);

            // get the enhance bytes
            enhancedBytes = cw.toByteArray();
            //PSLogger.error("NULL check : " + (enhancedBytes == null ? "null" : enhancedBytes.length));
        } catch (Exception e) {
            e.printStackTrace();
            PSLogger.info("Error Happen : " + UTILS.getErrorMsg(e) + " \n please append the target jar to classpath");
        }

        // he...he...
        return enhancedBytes;
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

    private static void loadMethodWeave(ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            ClassLoader agentClassLoader = CommandCodec.agentClassLoader;
            if (agentClassLoader == null) {
                throw new IllegalStateException("error status: the agent classloader must not null in here");
            }

            String weaveClass = "io.javadebug.agent.WeaveSpy";

            Class<?> agentSpyClass = agentClassLoader.loadClass(weaveClass);

            //check if the weave class already load by target classloader

            try {
                Class<?> checkClass = classLoader.loadClass(weaveClass);

                // check init status
                Field checkField = checkClass.getField("ON_METHOD_ENTER_CALL");
                if (checkField == null || checkField.get(null) == null) {
                    throw new IllegalStateException("the weaveSpy need to be init.");
                }

                PSLogger.error("method advice weave class already load by target classloader:" + checkClass.getName() + " from:" + classLoader);
            } catch (Throwable e) {
                PSLogger.error("start to define method advice weave class:" + e);

                Class<?> weaveSpyClass = null;

                try {
                    weaveSpyClass = defineClass(classLoader, weaveClass,
                            toByteArray(MethodTraceEnhance.class.getResourceAsStream("/" + weaveClass.replace(".", "/") + ".class")));
                    PSLogger.error("get the weave class by target classloader:" + weaveSpyClass.getName() + "@" + classLoader);
                } catch (Exception ee) {
                    PSLogger.error("could not define weave class to target classloader:" + ee);
                } finally {
                    if (weaveSpyClass != null) {
                        try {
                            // init the agent classloader
                            MethodUtils.invokeStaticMethod(weaveSpyClass, "initAgentClassLoader", agentClassLoader);

                            // init spy
                            MethodUtils.invokeStaticMethod(weaveSpyClass, "installAdviceMethod",
                                    agentSpyClass.getField("ON_METHOD_ENTER_CALL").get(null),
                                    agentSpyClass.getField("ON_METHOD_EXIT_CALL").get(null),
                                    agentSpyClass.getField("ON_METHOD_THROW_EXCEPTION_CALL").get(null),
                                    agentSpyClass.getField("ON_METHOD_INVOKE_LINE_CALL").get(null),
                                    agentSpyClass.getField("ON_METHOD_INVOKE_VAR_INS_CALL").get(null),
                                    agentSpyClass.getField("ON_METHOD_INVOKE_VAR_INS_V2_CALL").get(null),

                                    agentSpyClass.getField("ON_METHOD_INVOKE_VAR_INS_I_CALL").get(null),
                                    agentSpyClass.getField("ON_METHOD_INVOKE_VAR_INS_L_CALL").get(null),
                                    agentSpyClass.getField("ON_METHOD_INVOKE_VAR_INS_F_CALL").get(null),
                                    agentSpyClass.getField("ON_METHOD_INVOKE_VAR_INS_D_CALL").get(null),
                                    agentSpyClass.getField("ON_METHOD_INVOKE_VAR_INS_A_CALL").get(null),

                                    agentSpyClass.getField("ON_METHOD_IN_SPECIAL_DATA_TRANS_CALL").get(null),
                                    agentSpyClass.getField("ON_METHOD_IN_SPECIAL_CONDITION_JUDGE_CALL").get(null),
                                    agentSpyClass.getField("ON_METHOD_IN_SPECIAL_CONDITION_TRANS_DATA_GET_CALL").get(null),
                                    agentSpyClass.getField("ON_METHOD_FIELD_INVOKE_CALL").get(null)
                                    );
                        } catch (Throwable throwable) {
                            PSLogger.error("ignore the error:" + throwable);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            PSLogger.error("error occ:"  + e);
        }
    }

    /**
     * 定义类
     *
     * @param targetClassLoader 目标classLoader
     * @param className         类名称
     * @param classByteArray    类字节码数组
     * @return 定义的类
     * @throws Exception 处理异常
     */
    private synchronized static Class<?> defineClass(
            final ClassLoader targetClassLoader,
            final String className,
            final byte[] classByteArray) throws Exception {

        final java.lang.reflect.Method defineClassMethod = ClassLoader.class.getDeclaredMethod(
                "defineClass",
                String.class,
                byte[].class,
                int.class,
                int.class
        );
        final boolean acc = defineClassMethod.isAccessible();
        try {
            defineClassMethod.setAccessible(true);
            return (Class<?>) defineClassMethod.invoke(
                    targetClassLoader,
                    className,
                    classByteArray,
                    0,
                    classByteArray.length
            );
        } finally {
            defineClassMethod.setAccessible(acc);
        }
    }

    /**
     * 仅仅是获取到命令的结果，这里玩不出什么花样
     *
     * @return 返回给client的响应结果 {@see $back-data}
     */
    @Override
    public String getEnhanceResult() {
        return methodAdvice.result();
    }

    /**
     * 获取到增强过的字节码
     *
     * @return 增强过的字节码
     */
    @Override
    public byte[] getEnhanceBytes() {
        return enhancedBytes;
    }

}
