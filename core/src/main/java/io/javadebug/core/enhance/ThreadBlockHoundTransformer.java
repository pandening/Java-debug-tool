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
import io.javadebug.core.transport.CommandCodec;
import io.javadebug.core.utils.UTILS;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.io.IOUtils.toByteArray;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

public class ThreadBlockHoundTransformer implements ClassFileTransformer {

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private Map<String, Set<MethodDesc>> methodDescSetMap;
    private String threadTypePattern = "nonBlocking";
    private String threadNamePattern = "";

    public ThreadBlockHoundTransformer(Map<String, Set<MethodDesc>> md, String typePattern, String namePattern) {
        this.methodDescSetMap = md;
        if (!UTILS.isNullOrEmpty(typePattern)) {
            this.threadTypePattern = typePattern;
        }
        if (!UTILS.isNullOrEmpty(namePattern)) {
            this.threadNamePattern = namePattern;
        }
    }

    /**
     * The implementation of this method may transform the supplied class file and
     * return a new replacement class file.
     * <p>
     * <p>
     * There are two kinds of transformers, determined by the <code>canRetransform</code>
     * parameter of
     * {@link Instrumentation#addTransformer(ClassFileTransformer, boolean)}:
     * <ul>
     * <li><i>retransformation capable</i> transformers that were added with
     * <code>canRetransform</code> as true
     * </li>
     * <li><i>retransformation incapable</i> transformers that were added with
     * <code>canRetransform</code> as false or where added with
     * {@link Instrumentation#addTransformer(ClassFileTransformer)}
     * </li>
     * </ul>
     * <p>
     * <p>
     * Once a transformer has been registered with
     * {@link Instrumentation#addTransformer(ClassFileTransformer, boolean)
     * addTransformer},
     * the transformer will be called for every new class definition and every class redefinition.
     * Retransformation capable transformers will also be called on every class retransformation.
     * The request for a new class definition is made with
     * {@link ClassLoader#defineClass ClassLoader.defineClass}
     * or its native equivalents.
     * The request for a class redefinition is made with
     * {@link Instrumentation#redefineClasses Instrumentation.redefineClasses}
     * or its native equivalents.
     * The request for a class retransformation is made with
     * {@link Instrumentation#retransformClasses Instrumentation.retransformClasses}
     * or its native equivalents.
     * The transformer is called during the processing of the request, before the class file bytes
     * have been verified or applied.
     * When there are multiple transformers, transformations are composed by chaining the
     * <code>transform</code> calls.
     * That is, the byte array returned by one call to <code>transform</code> becomes the input
     * (via the <code>classfileBuffer</code> parameter) to the next call.
     * <p>
     * <p>
     * Transformations are applied in the following order:
     * <ul>
     * <li>Retransformation incapable transformers
     * </li>
     * <li>Retransformation incapable native transformers
     * </li>
     * <li>Retransformation capable transformers
     * </li>
     * <li>Retransformation capable native transformers
     * </li>
     * </ul>
     * <p>
     * <p>
     * For retransformations, the retransformation incapable transformers are not
     * called, instead the result of the previous transformation is reused.
     * In all other cases, this method is called.
     * Within each of these groupings, transformers are called in the order registered.
     * Native transformers are provided by the <code>ClassFileLoadHook</code> event
     * in the Java Virtual Machine Tool Interface).
     * <p>
     * <p>
     * The input (via the <code>classfileBuffer</code> parameter) to the first
     * transformer is:
     * <ul>
     * <li>for new class definition,
     * the bytes passed to <code>ClassLoader.defineClass</code>
     * </li>
     * <li>for class redefinition,
     * <code>definitions.getDefinitionClassFile()</code> where
     * <code>definitions</code> is the parameter to
     * {@link Instrumentation#redefineClasses
     * Instrumentation.redefineClasses}
     * </li>
     * <li>for class retransformation,
     * the bytes passed to the new class definition or, if redefined,
     * the last redefinition, with all transformations made by retransformation
     * incapable transformers reapplied automatically and unaltered;
     * for details see
     * {@link Instrumentation#retransformClasses
     * Instrumentation.retransformClasses}
     * </li>
     * </ul>
     * <p>
     * <p>
     * If the implementing method determines that no transformations are needed,
     * it should return <code>null</code>.
     * Otherwise, it should create a new <code>byte[]</code> array,
     * copy the input <code>classfileBuffer</code> into it,
     * along with all desired transformations, and return the new array.
     * The input <code>classfileBuffer</code> must not be modified.
     * <p>
     * <p>
     * In the retransform and redefine cases,
     * the transformer must support the redefinition semantics:
     * if a class that the transformer changed during initial definition is later
     * retransformed or redefined, the
     * transformer must insure that the second class output class file is a legal
     * redefinition of the first output class file.
     * <p>
     * <p>
     * If the transformer throws an exception (which it doesn't catch),
     * subsequent transformers will still be called and the load, redefine
     * or retransform will still be attempted.
     * Thus, throwing an exception has the same effect as returning <code>null</code>.
     * To prevent unexpected behavior when unchecked exceptions are generated
     * in transformer code, a transformer can catch <code>Throwable</code>.
     * If the transformer believes the <code>classFileBuffer</code> does not
     * represent a validly formatted class file, it should throw
     * an <code>IllegalClassFormatException</code>;
     * while this has the same effect as returning null. it facilitates the
     * logging or debugging of format corruptions.
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
        Set<MethodDesc> methodDescSet = needToEnhance(className);
        if (methodDescSet == null || methodDescSet.isEmpty()) {
            return EMPTY_BYTE_ARRAY;
        }

        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw  = new ClassWriter(cr, COMPUTE_FRAMES | COMPUTE_MAXS);
        ThreadBlockHoundWeaver threadBlockHoundWeaver = new ThreadBlockHoundWeaver(Opcodes.ASM5, cw, methodDescSet, classBeingRedefined, threadNamePattern);

        try {
            // enhance
            cr.accept(threadBlockHoundWeaver, EXPAND_FRAMES);
            byte[] newBytes = cw.toByteArray(); //enhance(getCtClass(classfileBuffer, loader), methodDescSet);

            // dump the weaved class
            dumpClass(newBytes, className);

            // load weave
            //loadMethodWeave(loader);

            // using the new bytecode
            return newBytes;
        } catch (Throwable e) {
            PSLogger.error("could not enhance class:" + className, e);
        }

        return EMPTY_BYTE_ARRAY;
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
                                    agentSpyClass.getField("ON_METHOD_FIELD_INVOKE_CALL").get(null),
                                    agentSpyClass.getField("IS_IN_NON_BLOCKING_THREAD_METHOD").get(null)
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

    private byte[] enhance(CtClass ctClass, Set<MethodDesc> methodDescSet) {
        CtMethod[] ctMethods = ctClass.getDeclaredMethods();
        for (CtMethod ctMethod : ctMethods) {
            String mname    = ctMethod.getName();
            String desc     = ctMethod.getMethodInfo().getDescriptor();
            for (MethodDesc md : methodDescSet) {
                if (md.isMatch(mname, desc)) {
                    PSLogger.error("start to enhance method :" + ctClass.getName()
                                           + "." + ctMethod.getName() + "#" + ctMethod.getMethodInfo().getDescriptor());
                    enhanceOneMethod(ctMethod);
                }
            }
        }
        try {
            return ctClass.toBytecode();
        } catch (Exception e) {
            PSLogger.error("error", e);
            return EMPTY_BYTE_ARRAY;
        }
    }

    private String enhanceMethodBody() {
        String code = "boolean blockCheckPass = true;\n" +
                              "        if (java.lang.Thread.currentThread().getName().contains(\"threadNamePattern\")) {\n" +
                              "            blockCheckPass = false;\n" +
                              "        }\n" +
                              "        // class type check\n" +
                              "        if (blockCheckPass) {\n" +
                              "            for (Class ic : java.util.Optional.ofNullable(java.lang.Thread.currentThread().getClass().getInterfaces()).orElse(new Class[]{})) {\n" +
                              "                // maybe : reactor.core.scheduler.NonBlocking\n" +
                              "                if (ic.getName().contains(\"threadTypePattern\")) {\n" +
                              "                    blockCheckPass = false;\n" +
                              "                }\n" +
                              "            }\n" +
                              "        }\n" +
                              "        if (!blockCheckPass) {\n" +
                              "            java.util.Map IGNORE_CLASS_METHOD_MAP = new java.util.HashMap();\n" +
                              "            \\IGNORE_CLASS_METHOD_MAP.put(\"a\", \"a\");\n" +
                              "            \\IGNORE_CLASS_METHOD_MAP.put(\"b\", \"b\");\n" +
                              "            java.lang.StackTraceElement[] stackTrace = java.lang.Thread.currentThread().getStackTrace();\n" +
                              "            for (int i = 0; i < 10 && i < stackTrace.length; i ++) {\n" +
                              "                java.lang.StackTraceElement ste = stackTrace[i];\n" +
                              "                String ignore = IGNORE_CLASS_METHOD_MAP.get(ste.getClassName());\n" +
                              "                if (!(ignore == null || ignore.isEmpty()) && ignore.contains(ste.getMethodName())) {\n" +
                              "                    blockCheckPass = false;\n" +
                              "                    break;\n" +
                              "                }\n" +
                              "            }\n" +
                              "        }\n" +
                              "        if (!blockCheckPass) {\n" +
                              "            throw new IllegalStateException(\"thread are blocking, which is not supported in thread :\"\n" +
                              "                                                    + Thread.currentThread().getName());\n" +
                              "        }";
        code = code.replace("threadNamePattern", threadNamePattern);
        code = code.replace("threadTypePattern", threadTypePattern);
        return code;
    }

    private void enhanceOneMethod(CtMethod ctMethod) {
        try {
            ctMethod.insertBefore(enhanceMethodBody());
        } catch (Exception e) {
            PSLogger.error("could not insert bytecode to method : " + ctMethod.getName(), e);
        }
    }

    private CtClass getCtClass(byte[] classFileBuffer, ClassLoader classLoader) throws IOException {
        ClassPool classPool = new ClassPool(true);
        if(null != classLoader) {
            classPool.appendClassPath(new LoaderClassPath(classLoader));
        }
        CtClass clazz = classPool.makeClass(new ByteArrayInputStream(classFileBuffer), false);
        clazz.defrost();
        return clazz;
    }

    /**
     *
     *  check if we need to enhance the class
     *
     * @param className
     * @return
     */
    private Set<MethodDesc> needToEnhance(String className) {
        className = className.replaceAll("/", ".");
        return methodDescSetMap.get(className);
    }

    /**
     *  dump the new class to check
     *
     * @param bytes
     * @param cls
     */
    private void dumpClass(byte[] bytes, String cls) {
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
            out.write(bytes);

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
