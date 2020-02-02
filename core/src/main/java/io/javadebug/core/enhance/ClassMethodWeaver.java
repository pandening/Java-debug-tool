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


import io.javadebug.core.data.ClassField;
import io.javadebug.core.utils.FieldValueExtract;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.data.VariableModel;
import io.javadebug.core.handler.StopAbleRunnable;
import io.javadebug.core.transport.CommandCodec;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.javadebug.core.Constant.DUMP_OBJECT_OPTIONS;

/**
 * Created on 2019/5/10 10:40.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class ClassMethodWeaver extends ClassVisitor implements Opcodes {

    // the debug mode
    public static volatile boolean debug = false;

    // the stack deep limit
    public static int callStackDeepLimit = 10;

    // console weave config
    private static final Map<String, ClassMethodWeaveConfig> CLIENT_WEAVE_CONFIG_MAP = new HashMap<>();

    /**
     *  获取到这个client的增强配置
     *
     * @param cls class name
     * @param method method name
     * @param desc method desc
     * @param cid console context id
     * @return weave config
     */
    @Deprecated
    public static ClassMethodWeaveConfig getWeaveConfig(String cls, String method, String desc, int cid) {
        String key = cid + "#mt#" + cls + "." + method + "@" + desc;
        ClassMethodWeaveConfig weaveConfig = CLIENT_WEAVE_CONFIG_MAP.get(key);
        if (weaveConfig == null) {
            weaveConfig = new ClassMethodWeaveConfig(WEAVE_ARGS, WEAVE_RETURN, WEAVE_THROW,
                    WEAVE_INVOKE_LINE, WEAVE_LOCAL_VAR_ASSIGN);
            CLIENT_WEAVE_CONFIG_MAP.put(key, weaveConfig);
        }
        return weaveConfig;
    }

    /**
     *  client设置了一个增强配置
     *
     * @param cls class name
     * @param method method name
     * @param desc method desc
     * @param cid console context id
     * @param weaveConfig the weave config
     */
    @Deprecated
    public static void setWeaveConfig(String cls, String method, String desc, int cid,
                                      ClassMethodWeaveConfig weaveConfig) {
        try {
            String key = cid + "#mt#" + cls + "." + method + "@" + desc;
            if (!UTILS.isNullOrEmpty(key) && weaveConfig != null) {
                CLIENT_WEAVE_CONFIG_MAP.put(key, weaveConfig);
            }
        } catch (Throwable e) {
            PSLogger.error("could not set weave config:" + e);
        }
    }

    // default weave config
    private static final boolean WEAVE_ARGS             = true; // 是否需要入参信息
    private static final boolean WEAVE_RETURN           = true; // 是否需要返回值信息
    private static final boolean WEAVE_THROW            = true; // 是否需要异常信息
    private static final boolean WEAVE_INVOKE_LINE      = true; // 是否需要代码执行行号链路信息
    private static final boolean WEAVE_LOCAL_VAR_ASSIGN = true; // 是否需要变量赋值信息


    // 用于管理client -> advice 的关系
    // 思考来思考去，还是觉得多client共享增强结果的功能应该放在Advice层去做，Weave层应该
    // 专心做增强工作即可，不应该过于关注CS相关或者command相关的内容
    @Deprecated
    private static final ConcurrentHashMap<Integer, MethodAdvice> CLIENT_METHOD_ADVICE_MAP = new ConcurrentHashMap<>();

    // 每个client都可以注册自己感兴趣的advice，weave会自己判断是否应该通知其中的advice
    private static final ConcurrentMap<Integer, MethodInvokeAdvice> METHOD_INVOKE_ADVICE_CONCURRENT_MAP = new ConcurrentHashMap<>();

    /**
     *  注册一个 {@link MethodInvokeAdvice} 到该weave上
     *
     * @param cid console id
     * @param methodInvokeAdvice method invoke advice
     */
    public static void regAdvice(int cid, MethodInvokeAdvice methodInvokeAdvice) {
        if (methodInvokeAdvice == null) {
            return;
        }
        METHOD_INVOKE_ADVICE_CONCURRENT_MAP.put(cid, methodInvokeAdvice);

        // 是否需要执行任务
        List<StopAbleRunnable> tasks = methodInvokeAdvice.tasks();
        if (!tasks.isEmpty()) {
            for (StopAbleRunnable runnable : tasks) {
                try {
                    TASK_EXECUTE.execute(runnable);
                } catch (Throwable e) {
                    PSLogger.error("执行任务出现错误:" + e);
                    try {
                        runnable.stop();
                    } catch (Throwable te) {
                        PSLogger.error("执行任务(stop方法)出现错误:" + te);
                    }
                }
            }
        }
    }

    /**
     *  register an advice with config
     *
     * @param cid the clinet id
     * @param methodInvokeAdvice the method invoke advice instance {@link MethodInvokeAdvice}
     * @param classMethodWeaveConfig  the weave config {@link ClassMethodWeaveConfig}
     */
    public static void regAdvice(int cid, MethodInvokeAdvice methodInvokeAdvice,
                                 ClassMethodWeaveConfig classMethodWeaveConfig) {
        regAdvice(cid, methodInvokeAdvice);

        // add one
        CLASS_WEAVE_CONFIG_MAP.put(cid, classMethodWeaveConfig);
    }

    /**
     *  一个client同一时间只能注册一个advice，完成观察之后需要来删除advice
     *
     * @param cid console id
     */
    public static MethodInvokeAdvice unRegAdvice(int cid) {

        // unReg config
        //CLASS_WEAVE_CONFIG_MAP.remove(cid);

        return METHOD_INVOKE_ADVICE_CONCURRENT_MAP.remove(cid);
    }

    // 任务执行队列
    private static final AtomicInteger inc = new AtomicInteger(1);
    private static final ExecutorService TASK_EXECUTE = new ThreadPoolExecutor(
            1,
            1,
            1,
            TimeUnit.MINUTES,
            new ArrayBlockingQueue<Runnable>(8),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "Weave-Task-Execute-Worker-" + inc.getAndIncrement());
                }
            });

    /**
     *  注册一个MTraceAdvice
     *
     * @param cid console id
     * @param methodAdvice {@link MethodAdvice}
     */
    public static void registerMethodAdvice(int cid, MethodAdvice methodAdvice) {
        if (methodAdvice == null || cid <= 0) {
            return;
        }
        CLIENT_METHOD_ADVICE_MAP.put(cid, methodAdvice);
        //PSLogger.error("console:" + cid + " register the method advice:" + methodAdvice + " withThread:" + Thread.currentThread());
    }

    /**
     *  卸载advice，这个客户端不想再收到通知了
     *
     * @param cid 客户端
     * @return 它的advice对象
     */
    public static MethodAdvice unregisterMethodAdvice(int cid) {
        if (cid <= 0) {
            return null;
        }
        MethodAdvice methodAdvice = CLIENT_METHOD_ADVICE_MAP.remove(cid);
        if (methodAdvice != null) {
            //PSLogger.error("console:" + cid + " unregister MethodAdvice:" + methodAdvice);
        }
        return methodAdvice;
    }

    // 用于竞争本次通知权，advice只能被一次方法调用通知，否则结果会混乱
    private volatile static AtomicInteger ADVICE_CONDITION_VAL = new AtomicInteger(1);

    /**
     *  用于竞争advice，如果没有竞争到，那么本次方法调用不需要通知;
     *  应该在_onMethodEnter的一开始进行advice选择，这个方法就当是放屁吧
     *
     * @return 需要通知的advice map
     */
    @Deprecated
    public static Map<Integer, MethodAdvice> adviceGet() {
        if (!ADVICE_CONDITION_VAL.compareAndSet(1, 0)) {
            return Collections.emptyMap(); // 没有竞争到
        }

        // 只可能有一个线程到这里
        Map<Integer, MethodAdvice> adviceMap = new HashMap<>(CLIENT_METHOD_ADVICE_MAP);

        // 清空advice
        // 这种情况对于watch、custom两种类型的trace带来了不可控性，可能这两种模式的advice
        // 会被register多次才能得到满足要求的结果
        CLIENT_METHOD_ADVICE_MAP.clear();

        try {
            return adviceMap;
        } finally {
            // 恢复竞争条件
            ADVICE_CONDITION_VAL.set(1);
        }
    }


    // thread local stack transport
    private static final ThreadLocal<Deque<Map<String, Object>>> THREAD_LOCAL_FRAME_STACK        = new ThreadLocal<Deque<Map<String, Object>>>(){
        @Override
        protected Deque<Map<String, Object>> initialValue() {
            return new LinkedList<>();
        }
    };

    // record tag stack
    private static final ThreadLocal<Deque<Boolean>> RECORD_TAG_STACK = new ThreadLocal<Deque<Boolean>>(){
        @Override
        protected Deque<Boolean> initialValue() {
            return new LinkedList<>();
        }
    };

    // 用于在线程内部传递信息，只考虑方法级别，并且是整个方法都由一个线程完成调用（感觉是废话）
    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL_CONTEXT_MAP               = new ThreadLocal<Map<String, Object>>() {
        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<>();
        }
    };

    /**
     *  从context map中获取到当前线程的上下文信息
     *
     * @param key key
     * @param <T> 值类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    private static <T> T getFromContext(String key) {
        if (UTILS.isNullOrEmpty(key)) {
            return null; // 考虑抛出异常是不是更合适
        }
        return (T) THREAD_LOCAL_CONTEXT_MAP.get().get(key);
    }

    /**
     *  将上下文信息保存在Local map中去
     *
     * @param key key
     * @param val 值
     * @param <T> 值类型
     */
    private static  <T> void setToContext(String key, T val) {
        if (UTILS.isNullOrEmpty(key) || val == null) {
            return; // 考虑抛出异常是不是更合适
        }
        THREAD_LOCAL_CONTEXT_MAP.get().put(key, val);
    }

    // 流控
    private static final Map<String, Boolean> RECORD_FLOW_CONTROLLER_MAP = new HashMap<>();

    /**
     *  如果想要开启另外一轮，只能通过调用这个方法来开启
     *
     * @param cid 客户端
     * @param cls 类
     * @param method 方法
     * @param desc 方法描述
     */
    private static void anotherTime(int cid, String cls, String method, String desc) {
        String key = cid + "#mt#" + cls + "." + method + "@" + desc;
        //PSLogger.error("another time for key:" + key);
        RECORD_FLOW_CONTROLLER_MAP.put(key, Boolean.TRUE);
    }

    /**
     *  判断一下是否允许记录信息
     *
     * @param cid 客户端
     * @param cls 类
     * @param method 方法
     * @param desc 方法描述
     * @return 是否允许
     */
    private static boolean allowToRecord(int cid, String cls, String method, String desc) {
        String key = cid + "#mt#" + cls + "." + method + "@" + desc;
        return RECORD_FLOW_CONTROLLER_MAP.get(key);
    }

    // 局部变量表，key是 class.method，value就是具体的方法的局部变量表
    private static final Map<String, LocalVariableTable> LOCAL_VARIABLE_TABLE_MAP = new HashMap<>();

    /**
     *  在这里设置一下var 到Local variable table
     *
     * @param index index
     * @param varName 变量名称
     */
    private static void setLocalVariable(String cls, String method, String desc, int index, String varName) {
        cls = cls.replaceAll("/", ".");
        String localVariableKey = cls + "." + method + "@" + desc;
        LocalVariableTable localVariableTable = LOCAL_VARIABLE_TABLE_MAP.get(localVariableKey);
        if (localVariableTable == null) {
            localVariableTable = new MapBaseLocalVariableTable();
            LOCAL_VARIABLE_TABLE_MAP.put(localVariableKey, localVariableTable);
        }
        try {
            // set the variable
            ((MapBaseLocalVariableTable)localVariableTable).setVar(index, varName);
        } catch (Throwable e) {
            e.printStackTrace();
            PSLogger.error("error status:" + e);
        }
    }

    /**
     *  获取到变量名字
     *
     * @param cls 类
     * @param method 方法
     * @param desc 方法描述
     * @param index 变量index
     * @return 变量名字，不准确，甚至可能为空
     */
    private static String getLocalVarName(String cls, String method, String desc, int index) {
        cls = cls.replaceAll("/", ".");
        String localVariableKey = cls + "." + method + "@" + desc;
        LocalVariableTable localVariableTable = LOCAL_VARIABLE_TABLE_MAP.get(localVariableKey);
        if (localVariableTable == null) {
            localVariableTable = new MapBaseLocalVariableTable();
            LOCAL_VARIABLE_TABLE_MAP.put(localVariableKey, localVariableTable);
        }

        //System.out.println("all local tables:" + localVariableTable);

        return Optional.ofNullable(localVariableTable.valueAt(index)).orElse("");
    }

    // black method set
    private static final Set<MethodDesc> BLACK_METHOD_SET = new HashSet<>();

    static {
        // toString
        BLACK_METHOD_SET.add(new MethodDesc("java.lang.Object", "toString", Method.getMethod("String toString()").getDescriptor(), Object.class));

        // hashCode
        BLACK_METHOD_SET.add(new MethodDesc("java.lang.Object", "hashCode", Method.getMethod("int hashCode()").getDescriptor(), Object.class));

    }

    private static Type WEAVE_SPY_TYPE;

    static {
        try {
            Class<?> WEAVE_SPY_CLASS = CommandCodec.agentClassLoader.loadClass("io.javadebug.agent.WeaveSpy");
            WEAVE_SPY_TYPE = Type.getType(WEAVE_SPY_CLASS);
        } catch (Throwable e) {
            // ignore
            PSLogger.error("class :" + ClassMethodWeaver.class.getName() + " init error:" + e);
        }
    }

    private static final Type CLASS_TYPE                            = Type.getType(Class.class);

    private static final Type OBJECT_TYPE                           = Type.getType(Object.class);

    private static final Type CLASS_FIELD_TYPE                      = Type.getType(ClassField.class);

    private static final Type THROWABLE_TYPE                        = Type.getType(Throwable.class);

    private static final Type METHOD_Type                           = Type.getType(java.lang.reflect.Method.class);

    private static final Method METHOD_INVOKE_METHOD                = Method.getMethod("Object invoke(Object,Object[])");


    /**
     *  get asm method {@link Method}
     *
     * @param clazz the class
     * @param methodName method name
     * @param parameterTypes params
     * @return the asm method
     */
    private static Method getAsmMethod(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes) {
        return Method.getMethod(getJavaMethodUnsafe(clazz, methodName, parameterTypes));
    }

    /**
     *  get java method
     *
     * @param clazz the class
     * @param methodName the method name
     * @param parameterTypes the params
     * @return the java method
     */
    private static java.lang.reflect.Method getJavaMethodUnsafe(final Class<?> clazz, final String methodName,
                                                                final Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *  判断一下spy是否需要工作
     *
     * @param cid console
     * @param cls cls
     * @param m method
     * @param desc method desc
     * @return 是否需要
     */
    private static boolean spyNeedToRecord(int cid, String cls, String m, String desc) {
        // 流控
        return allowToRecord(cid, cls, m, desc);
    }

    /**
     * 如果需要获取到类字段信息，则会增强相关字节码来获取，并在方法进入和出去之前获取到类的快照，作为对比最后会打印出来。
     *
     * @param fieldName 字段名称
     * @param fieldType 字段类型
     * @param fieldDesc 字段描述
     * @param fieldVal 字段值
     */
    @SuppressWarnings("unchecked")
    public static void onFieldNotice(int stage, String fieldName, String fieldType, String fieldDesc, Object fieldVal) {
        if (RECORD_TAG_STACK.get().isEmpty()) {
            return;
        }
        Boolean isRecordTag = RECORD_TAG_STACK.get().getFirst();
        if (isRecordTag == null || !isRecordTag) {
            return;
        }

        if (THREAD_LOCAL_FRAME_STACK.get().isEmpty()) {
            return;
        }

        ClassField classField = new ClassField();
        classField.setFieldName(fieldName);
        classField.setFieldType(fieldType);
        classField.setFieldValInObj(fieldVal, DUMP_OBJECT_OPTIONS);

        // get the frame stack
        Map<String, Object> threadLocalParamMap = THREAD_LOCAL_FRAME_STACK.get().getFirst();
        if (threadLocalParamMap == null) {
            PSLogger.error("error status, the frame stack must not be null here");
            throw new IllegalStateException("error status, the frame stack must not be null here");
        }

        List<ClassField> classFieldList;
        int fieldsCnt;
        if (stage == 1) {
            classFieldList = (List<ClassField>) threadLocalParamMap.get("1fields");
            if (classFieldList == null) {
                classFieldList = new ArrayList<>();
                threadLocalParamMap.put("1fields", classFieldList);
            }
        } else if (stage == 2) {
            classFieldList = (List<ClassField>) threadLocalParamMap.get("1fields");
            fieldsCnt = classFieldList.size();
            classFieldList = (List<ClassField>) threadLocalParamMap.get("2fields");
            if (classFieldList == null) {
                classFieldList = new ArrayList<>();
                threadLocalParamMap.put("2fields", classFieldList);
            }
            if (fieldsCnt == classFieldList.size()) {
                return;
            }
        } else {
            PSLogger.error("the stage value is invalid");
            return;
        }
        classFieldList.add(classField);
    }


    // static advice method area

    /**
     *  当某个方法被访问的时候，会首先通知该方法
     *
     * @param loader 加载的类加载器
     * @param className 方法类名
     * @param methodName 方法名称
     * @param methodDesc 方法描述
     * @param target 所属对象
     * @param args 方法参数
     */
    public static void _onMethodEnter(int contextId, ClassLoader loader, String className, String methodName,
                                      String methodDesc, Object target, Object[] args) {
        // 将入参记录下来，并且传递下去
        Map<String, Object> threadLocalParamMap = new HashMap<>();

        // 如果没有advice，那么就不需要录制
        if (METHOD_INVOKE_ADVICE_CONCURRENT_MAP.isEmpty()) {
            //setToContext("isRecord", false);
            RECORD_TAG_STACK.get().addFirst(false);
        } else {
            boolean isRecord = false;
            for (ConcurrentMap.Entry<Integer, MethodInvokeAdvice> entry : METHOD_INVOKE_ADVICE_CONCURRENT_MAP.entrySet()) {
                if (entry.getValue().check(loader, className, methodName, methodDesc, target, args)) {
                    isRecord = true;
                    break;
                }
            }

            // 这个标志传递下去，告诉下游要不要记录
            RECORD_TAG_STACK.get().addFirst(isRecord);

            // 如果不需要记录，那么一路上都不要传递
            if (isRecord) {
                // get a method enter frame
                MethodTraceFrame methodTraceFrame = MethodTraceFrame.newMethodEnterFrame(className, methodName, methodDesc, target, args, System.currentTimeMillis(), Thread.currentThread().toString());
                List<MethodTraceFrame> traces = new ArrayList<>();
                traces.add(methodTraceFrame);
                try {
                    threadLocalParamMap.put("traces", traces);
                    threadLocalParamMap.put("cid", contextId);
                    threadLocalParamMap.put("loader", loader);

                    // 替换一下
                    className = className.replaceAll("/", ".");

                    threadLocalParamMap.put("cls", className);
                    threadLocalParamMap.put("method", methodName);
                    threadLocalParamMap.put("mc", methodDesc);
                    threadLocalParamMap.put("target", target);
                    threadLocalParamMap.put("args", args);
                    // local table, 进入方法前应该已经存在了，如果不存在，那就真的不存在了
                    String localTableCacheKey = className + "." + methodName + "@" + methodDesc;
                    LocalVariableTable localVariableTable = LOCAL_VARIABLE_TABLE_MAP.get(localTableCacheKey);
                    if (localVariableTable == null) {
                        PSLogger.error("could not get the local variable table for cache key:"  + localTableCacheKey + " \n " + LOCAL_VARIABLE_TABLE_MAP + " \n");
                    }
                    threadLocalParamMap.put("localTable", localVariableTable);

                    // todo : check whether the caller need the call stack, if not, do not do this
                    // get the call trace
                    String callTraceStack = callTrace(className, methodName);
                    threadLocalParamMap.put("callTrace", callTraceStack);

                    // fields val
                    ClassMethodWeaveConfig classMethodWeaveConfig  = CLASS_WEAVE_CONFIG_MAP.get(contextId);
                    if (classMethodWeaveConfig.isSfield()) {
                        List<ClassField> classFields1 = FieldValueExtract.extractFields(target);
                        threadLocalParamMap.put("1fields", classFields1);
                    }

                } catch (Throwable e) {
                    PSLogger.error("error while save the context frame for context:" + contextId, e);
                } finally {
                    // 上下文信息传递
                    THREAD_LOCAL_FRAME_STACK.get().addFirst(threadLocalParamMap);
                }
            }
        }

    }

    /**
     *  如果你需要传递什么数据到观察者，那么使用需要使得这个方法返回true，如果返回false，那么
     *  就不会将数据传输到观察者那边去
     *
     * @param loader 方法的类加载器
     * @param className 方法所属的类名
     * @param methodName 方法名字
     * @param methodDesc 方法描述
     * @param target 目标对象
     * @param args 方法参数
     * @return 是否需要传递
     */
    public static boolean checkSpecialCondition(ClassLoader loader, String className, String methodName,
                                                String methodDesc, Object target, Object[] args) {
//        // 根据方法入参等进行判断
//        for (Map.Entry<Integer, MethodAdvice> entry : CLIENT_METHOD_ADVICE_MAP.entrySet()) {
//            MethodAdvice methodAdvice = entry.getValue();
//            int cid = entry.getKey();
//            if (methodAdvice == null || cid <= 0) {
//                continue;
//            }
//            // whether any advice need to advice
//            if (methodAdvice.checkSpecialCondition(
//                    loader, className, methodName, methodDesc, target, args)) {
//                return true;
//            }
//        }
        return false;
    }

    /**
     *  拿到需要传输的数据，然后传输下去
     *
     * @return 需要传输的对象,可笑的是，你可以返回null
     */
    public static Object specialDataTransGet() {
        return null;
//        for (Map.Entry<Integer, MethodAdvice> entry : CLIENT_METHOD_ADVICE_MAP.entrySet()) {
//            MethodAdvice methodAdvice = entry.getValue();
//            int cid = entry.getKey();
//            if (methodAdvice == null || cid <= 0) {
//                continue;
//            }
//            Object obj = methodAdvice.specialDataTransGet();
//
//            // 所以，每个advice请使用自己可识别的特殊内容作为传递内容，否则会影响别的advice，或者被别的
//            // advice影响，这样就会出现笑话
//            invokeSpecialDataTrans(obj);
//        }
//        return null; // ..>..
    }

    /**
     *  某些情况下，需要访问这个方法，用于传输一些特殊的数据到观察者那边去
     *  {@link MethodAdvice#invokeSpecialTransformWithAttach(Object)}
     *
     * @param param 数据内容
     */
    public static void invokeSpecialDataTrans(Object param) {
//        for (Map.Entry<Integer, MethodAdvice> entry : CLIENT_METHOD_ADVICE_MAP.entrySet()) {
//            MethodAdvice methodAdvice = entry.getValue();
//            int cid = entry.getKey();
//            if (methodAdvice == null || cid <= 0) {
//                continue;
//            }
//            // advice it.
//            methodAdvice.invokeSpecialTransformWithAttach(param);
//        }
    }

    /**
     *  判断方法入参是否是需要的入参
     *
     * @param cid context Id
     * @param loader 类加载器
     * @param className 类名
     * @param methodName 方法名
     * @param target 目标对象
     * @param args 方法入参
     * @return 是否匹配
     */
    private static boolean paramIsMatch(int cid, ClassLoader loader, String className,
                                        String methodName, Object target, Object[] args) {
        return true;
    }

    /**
     *  当方法正常退出的时候，会通知这个方法
     *
     * @param returnVal 方法执行结果
     */
    @SuppressWarnings("unchecked")
    public static void _onMethodExit(Object returnVal, String className, String methodName, String methodDesc) {
//        Boolean isRecordTag = getFromContext("isRecord");
//        if (isRecordTag == null || isRecordTag.equals(Boolean.FALSE)) {
//            return;
//        }

        // do advice
        doAdvice(returnVal, null);
    }

    /**
     *
     *  当方法结束的时候，调用这个方法来进行通知
     *
     * @param returnVal 方法返回结果
     * @param throwable 抛出的异常信息
     */
    @SuppressWarnings("unchecked")
    private static void doAdvice(Object returnVal, Throwable throwable) {

//        Boolean isRecordTag = getFromContext("isRecord");
//        if (isRecordTag == null || isRecordTag.equals(Boolean.FALSE)) {
//            // 恢复现场，继续录制
//            setToContext("isRecord", true);
//            return;
//        }
        if (RECORD_TAG_STACK.get().isEmpty()) {
            return;
        }
        Boolean isRecordTag = RECORD_TAG_STACK.get().removeFirst();
        if (isRecordTag == null || !isRecordTag) {
            return;
        }

        if (THREAD_LOCAL_FRAME_STACK.get().isEmpty()) {
            return; // empty trace
        }

        // get the frame stack
        Map<String, Object> threadLocalParamMap = THREAD_LOCAL_FRAME_STACK.get().removeFirst();
        if (threadLocalParamMap == null) {
            PSLogger.error("error status, the frame stack must not be null here");
            throw new IllegalStateException("error status, the frame stack must not be null here");
        }

        // check advice map and callback
        try {

            // get context info
            Integer contextId = (Integer) threadLocalParamMap.get("cid");
            String className = (String) threadLocalParamMap.get("cls");
            String methodName = (String) threadLocalParamMap.get("method");
            String methodDesc = (String) threadLocalParamMap.get("mc");
            ClassLoader loader = (ClassLoader) threadLocalParamMap.get("loader");
            Object target = threadLocalParamMap.get("target");
            Object[] args = (Object[]) threadLocalParamMap.get("args");
            List<MethodTraceFrame> traceFrames = (List<MethodTraceFrame>) threadLocalParamMap.get("traces");
            List<ClassField> fields1 = (List<ClassField>) threadLocalParamMap.get("1fields");
            List<ClassField> fields2 = (List<ClassField>) threadLocalParamMap.get("2fields");

            ClassMethodWeaveConfig classMethodWeaveConfig = getWeaveConfigure(contextId);
            if (classMethodWeaveConfig.isSfield() && classMethodWeaveConfig.isSfd()) {
                fields2 = FieldValueExtract.extractFields(target);
            }

            // append the return/throw frame
            MethodTraceFrame exitFrame;
            if (throwable != null) {
                // throw
                exitFrame = MethodTraceFrame.newMethodThrowFrame(throwable, System.currentTimeMillis(), fields1, fields2);
            } else {
                // return
                exitFrame = MethodTraceFrame.newMethodExitFrame(returnVal, System.currentTimeMillis(), fields1, fields2);
            }
            traceFrames.add(exitFrame);

            Set<Integer> adviceClientSets = new HashSet<>();

            for (ConcurrentMap.Entry<Integer, MethodInvokeAdvice> entry
                    : METHOD_INVOKE_ADVICE_CONCURRENT_MAP.entrySet()) {
                MethodInvokeAdvice methodInvokeAdvice = entry.getValue();

                // 基础类型判断
                MethodDesc targetMethod = methodInvokeAdvice.method();

                if (!targetMethod.isMatch(className, methodName, methodDesc)) {

                    continue; // 类型都不匹配，就不要继续了
                }

                if (methodInvokeAdvice.match(loader, className, methodName, methodDesc,
                        target, args, throwable, returnVal)) {
                    // 需要通知
                    adviceClientSets.add(entry.getKey());
                }

            }

            // 是否有需要通知的
            for (int cid : adviceClientSets) {
                MethodInvokeAdvice methodInvokeAdvice = unRegAdvice(cid);
                if (methodInvokeAdvice == null) {
                    continue; // 被其他的线程通知了，那就不需要通知了
                }

                // set the call trace
                String callTrace = (String) threadLocalParamMap.get("callTrace");
                traceFrames.get(traceFrames.size() - 1).setCallTrace(callTrace);

                // 通知
                methodInvokeAdvice.invoke(traceFrames);

                // 是否需要重新注册advice
                if (methodInvokeAdvice.needToUnReg(className, methodName, methodDesc)) {
                    regAdvice(cid, methodInvokeAdvice);
                }
            }

        } catch (Throwable e) {
            PSLogger.error("error while doAdvice");
        } finally {
            // 恢复现场，继续录制
            setToContext("isRecord", true);
            //System.err.println("stack size:" + THREAD_LOCAL_FRAME_STACK.get().size());
        }
    }

    /**
     *  get the call trace
     *
     * @return {@link Thread#currentThread()}
     */
    private static String callTrace(String tc, String tm) {
        StackTraceElement[] traceElements = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();

        int space = 1;

        boolean find = false;
        int deep = 0;
        for (StackTraceElement element : traceElements) {
            if (!find) {
                if (tc.equals(element.getClassName()) && tm.equals(element.getMethodName())) {
                    find = true;
                } else {
                    continue;
                }
            }
            for (int i = 0; i < space; i ++) {
                sb.append(" ");
            }
            space ++;
            sb.append("-").append(element.getClassName())
                    .append(".").append(element.getMethodName());
            if (element.getLineNumber() < 0) {
                sb.append("\n");
            } else {
                sb.append(" at line:").append(element.getLineNumber()).append("\n");
            }

            // check
            if (++ deep >= callStackDeepLimit) {
                break;
            }
        }

        return sb.toString();
    }

    /**
     *  当方法异常退出的时候，会通知这个方法
     *
     * @param throwable 发送的异常
     */
    @SuppressWarnings("unchecked")
    public static void _onMethodThrowing(Throwable throwable,  String className, String methodName, String methodDesc) {
//        Boolean isRecordTag = getFromContext("isRecord");
//        if (isRecordTag == null || isRecordTag.equals(Boolean.FALSE)) {
//            return;
//        }

        // do advice
        doAdvice(null, throwable);
    }

    /**
     *  当访问方法的某一行的时候，会通知这个方法
     *
     * @param lineNo 访问的行号
     */
    @SuppressWarnings("unchecked")
    public static void invokeLine(int lineNo) {
//        Boolean isRecordTag = getFromContext("isRecord");
//        if (isRecordTag == null || isRecordTag.equals(Boolean.FALSE)) {
//            return;
//        }
        if (RECORD_TAG_STACK.get().isEmpty()) {
            return;
        }
        Boolean isRecordTag = RECORD_TAG_STACK.get().getFirst();
        if (isRecordTag == null || !isRecordTag) {
            return;
        }

        if (THREAD_LOCAL_FRAME_STACK.get().isEmpty()) {
            return;
        }

        // get the frame stack
        Map<String, Object> threadLocalParamMap = THREAD_LOCAL_FRAME_STACK.get().getFirst();
        if (threadLocalParamMap == null) {
            PSLogger.error("error status, the frame stack must not be null here");
            throw new IllegalStateException("error status, the frame stack must not be null here");
        }

        // get context info
        String className = (String) threadLocalParamMap.get("cls");
        String methodName = (String) threadLocalParamMap.get("method");
        String methodDesc = (String) threadLocalParamMap.get("mc");
        ClassLoader loader = (ClassLoader) threadLocalParamMap.get("loader");
        Object target = threadLocalParamMap.get("target");
        Object[] args = (Object[]) threadLocalParamMap.get("args");
        List<MethodTraceFrame> traceFrames = (List<MethodTraceFrame>) threadLocalParamMap.get("traces");

        if (traceFrames == null) {
            PSLogger.error("the method traces must not null here, impossible");
            throw new IllegalStateException("the method traces must not null here, impossible");
        }

        // this line invoke info
        MethodTraceFrame methodTraceFrame =
                MethodTraceFrame.newMethodInvokeLineFrame(lineNo, "", null, System.currentTimeMillis());
        traceFrames.add(methodTraceFrame);

    }

    /**
     *  当访问Local variable 指令出现的时候，会通知这个方法
     *  该方法需要实现管理局部变量表的功能，需要建立好完善的局部变量表，然后attach到
     *  各个advice中去
     *
     * @param name 变量名称
     * @param desc 变量描述
     * @param signature 签名
     * @param index 很重要，这个是这个变量在Local variable table中的index，但是可能会被占用
     */
    private static void invokeLocalVariable(String cls, String method, String mDesc, String name, String desc, String signature, int index) {

        // set the variable to local table
        setLocalVariable(cls, method, mDesc, index, name);

    }

    public static void invokeIntVarInstruction(int varVal, int var, String op) {
        invokeVarInstructionV2(varVal, var, op);
    }

    public static void invokeFloatVarInstruction(float varVal, int var, String op) {
        invokeVarInstructionV2(varVal, var, op);
    }

    public static void invokeDoubleVarInstruction(double varVal, int var, String op) {
        invokeVarInstructionV2(varVal, var, op);
    }

    public static void invokeLongVarInstruction(long varVal, int var, String op) {
        invokeVarInstructionV2(varVal, var, op);
    }

    public static void invokeObjectVarInstruction(Object varVal, int var, String op) {
        invokeVarInstructionV2(varVal, var, op);
    }

    /**
     *  当出现操作变量的指令的时候会通知这个方法，比如变量加载或者存储，这个时候获取到的
     *  变量值是最新的，具有一定的参考价值
     *
     * @param op "STORE"/"LOAD"
     * @param var 变量名称
     * @param varVal 变量值
     */
    @SuppressWarnings("unchecked")
    public static void invokeVarInstructionV2(Object varVal, int var, String op) {
//        Boolean isRecordTag = getFromContext("isRecord");
//        if (isRecordTag == null || isRecordTag.equals(Boolean.FALSE)) {
//            return;
//        }

        if (RECORD_TAG_STACK.get().isEmpty()) {
            return;
        }
        Boolean isRecordTag = RECORD_TAG_STACK.get().getFirst();
        if (isRecordTag == null || !isRecordTag) {
            return;
        }

        if (THREAD_LOCAL_FRAME_STACK.get().isEmpty()) {
            return;
        }

//        // 如果变量值为null，则也不记录
//        if (varVal == null) {
//            return;
//        }

        // get the frame stack
        Map<String, Object> threadLocalParamMap = THREAD_LOCAL_FRAME_STACK.get().getFirst();
        if (threadLocalParamMap == null) {
            PSLogger.error("error status, the frame stack must not be null here");
            throw new IllegalStateException("error status, the frame stack must not be null here");
        }

        // context val
        String className = (String) threadLocalParamMap.get("cls");
        String methodName = (String) threadLocalParamMap.get("method");
        String methodDesc = (String) threadLocalParamMap.get("mc");

        // 取出当前行的frame trace，然后增加其变量信息
        List<MethodTraceFrame> traceFrames = (List<MethodTraceFrame>) threadLocalParamMap.get("traces");

        // 执行到这里，不可能为空
        if (traceFrames == null || traceFrames.isEmpty()) {
            PSLogger.error("the method trace must not be null or empty reach here.");
            throw new IllegalStateException("the method trace must not be null or empty reach here");
        }

        // 找到需要添加变量信息的trace item
        MethodTraceFrame nearestTrace = traceFrames.get(traceFrames.size() - 1);
        if (nearestTrace == null) {
            PSLogger.error("the method trace item must not be null or empty reach here.");
            throw new IllegalStateException("the method trace item must not be null or empty reach here");
        }

        // 变量信息
        List<VariableModel> localVariables = nearestTrace.getLocalVariable();

        if (localVariables == null) {
            localVariables = new ArrayList<>();
            nearestTrace.setLocalVariable(localVariables);
        }

        // 获取到变量名字
        String varName = getLocalVarName(className, methodName, methodDesc, var);

        // 新增一个变量
        localVariables.add(new VariableModel(varVal, varName));

    }

    /**
     *  当出现操作变量的指令的时候，会通知这个方法，比如出现存储、加载变量的指令的时候，这个
     *  方法就会拿到具体的通知
     *
     * @param opcode 指令code
     * @param varIndex 操作的变量的index，可以去Local variable table里面取具体的lv
     */
    @SuppressWarnings("unchecked")
    public static void invokeVarInstruction(int opcode, int varIndex) {
        Boolean isRecordTag = getFromContext("isRecord");
        if (isRecordTag == null || isRecordTag.equals(Boolean.FALSE)) {
            return;
        }
        // get the frame stack
        Map<String, Object> threadLocalParamMap = THREAD_LOCAL_FRAME_STACK.get().getFirst();
        if (threadLocalParamMap == null) {
            PSLogger.error("error status, the frame stack must not be null here");
            throw new IllegalStateException("error status, the frame stack must not be null here");
        }

        Map<Integer, MethodAdvice> matchedMethodAdviceMap = (Map<Integer, MethodAdvice>) threadLocalParamMap.get("advices");
//        if (advice == null) {
//            //throw new IllegalStateException("error status, the advice must not null here");
//            return; // I think it's ok
//        }

        // local table ref
        LocalVariableTable localVariableTable = (LocalVariableTable) threadLocalParamMap.get("localTable");

        try {
            for (Map.Entry<Integer, MethodAdvice> entry : matchedMethodAdviceMap.entrySet()) {
                MethodAdvice advice = entry.getValue();
                int cid = entry.getKey();
                if (advice == null || cid <= 0) {
                    continue;
                }
                advice.invokeVarInstruction(cid, opcode, varIndex, localVariableTable);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            PSLogger.error("error while advice invokeVarInstruction:" + e);
        }
    }

    // the target method desc
    private MethodDesc sMethodDesc;

    // 非常重要的client标志，用于唯一标志一个client
    private int contextId;

    // 增强配置，主要是一些增强点开关
    private ClassMethodWeaveConfig weaveConfig;

    private static Map<Integer, ClassMethodWeaveConfig> CLASS_WEAVE_CONFIG_MAP = new ConcurrentHashMap<>();

    private static ClassMethodWeaveConfig getWeaveConfigure(int cid) {
        ClassMethodWeaveConfig classMethodWeaveConfig = CLASS_WEAVE_CONFIG_MAP.get(cid);
        if (classMethodWeaveConfig == null) {
            classMethodWeaveConfig = new ClassMethodWeaveConfig();
            CLASS_WEAVE_CONFIG_MAP.put(cid, classMethodWeaveConfig);
        }
        return classMethodWeaveConfig;
    }

    /**
     * Constructs a new {@link ClassVisitor}.
     *
     * @param api the ASM API version implemented by this visitor. Must be one
     *            of {@link Opcodes#ASM4}, {@link Opcodes#ASM5} or {@link Opcodes#ASM6}.
     * @param cv  the class visitor to which this visitor must delegate method
     */
    ClassMethodWeaver(int api, ClassVisitor cv, MethodAdvice methodAdvice, MethodDesc methodDesc,
                      int contextId, ClassMethodWeaveConfig classMethodWeaveConfig) {
        super(api, cv);

        // 改为主动注册
        CLIENT_METHOD_ADVICE_MAP.put(contextId, methodAdvice);
        this.contextId = contextId;
        this.sMethodDesc = methodDesc;
        this.weaveConfig = classMethodWeaveConfig;
        CLASS_WEAVE_CONFIG_MAP.put(contextId, weaveConfig);

        // 仅允许执行一次记录通知
        anotherTime(contextId, sMethodDesc.getClassName(), sMethodDesc.getName(), sMethodDesc.getDesc());
    }

    /**
     * Visits information about an inner class. This inner class is not
     * necessarily a member of the class being visited.
     *
     * @param name
     *            the internal name of an inner class (see
     *            {@link Type#getInternalName() getInternalName}).
     * @param outerName
     *            the internal name of the class to which the inner class
     *            belongs (see {@link Type#getInternalName() getInternalName}).
     *            May be <tt>null</tt> for not member classes.
     * @param innerName
     *            the (simple) name of the inner class inside its enclosing
     *            class. May be <tt>null</tt> for anonymous inner classes.
     * @param access
     *            the access flags of the inner class as originally declared in
     *            the enclosing class.
     */
    public void visitInnerClass(String name, String outerName,
                                String innerName, int access) {
//        if (cv != null) {
//            cv.visitInnerClass(name, outerName, innerName, access);
//        }
    }

    /**
     * Visits a method of the class. This method <i>must</i> return a new
     * {@link MethodVisitor} instance (or <tt>null</tt>) each time it is called,
     * i.e., it should not return a previously returned visitor.
     *
     * @param access
     *            the method's access flags (see {@link Opcodes}). This
     *            parameter also indicates if the method is synthetic and/or
     *            deprecated.
     * @param name
     *            the method's name.
     * @param desc
     *            the method's descriptor (see {@link Type Type}).
     * @param signature
     *            the method's signature. May be <tt>null</tt> if the method
     *            parameters, return type and exceptions do not use generic
     *            types.
     * @param exceptions
     *            the internal names of the method's exception classes (see
     *            {@link Type#getInternalName() getInternalName}). May be
     *            <tt>null</tt>.
     * @return an object to visit the byte code of the method, or <tt>null</tt>
     *         if this class visitor is not interested in visiting the code of
     *         this method.
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        // 方法是否匹配
        if (mv == null || !isMatchMethod(access, name, desc)) {
            return mv;
        }

        // weave this method
        try {
            return new AdviceAdapter(ASM5, new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions), access, name, desc) {
                // label
                final Label try_label                          = new Label();
                final Label catch_label                        = new Label();

                /**
                 * Called at the beginning of the method or after super class call in
                 * the constructor. <br>
                 * <br>
                 *
                 * <i>Custom code can use or change all the local variables, but should not
                 * change state of the stack.</i>
                 */
                @Override
                protected void onMethodEnter() {
                    super.onMethodEnter();

                    mark(try_label);

                    // 处理方法进入时的通知事项
                    handleEnter();

                    // 字段
                    if (weaveConfig.isField()) {
                        handleDumpFields(1);
                    }

                    // try ... catch
                    //mark(try_label);
                }

                private void debug(String msg) {
                    if (debug && !UTILS.isNullOrEmpty(msg)) {
                        visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                        visitLdcInsn(msg);
                        visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
                    }
                }

                /**
                 *  需要完成通知方法进入的事件
                 *
                 */
                private void handleEnter() {
                    // pre start
                    //handlePreStart();

                    // print info
                    debug("method enter : " + sMethodDesc.getClassName() + "." + sMethodDesc.getName());

                    // load advice method
                    loadAdviceAsmMethod("starts");

                    // push the first method param for Method.invoke
                    push((Type) null);

                    // load the second method params for Method.invoke
                    loadMethodEnterParam();

                    // do method call: Method.invoke(...)
                    invokeVirtual(METHOD_Type, METHOD_INVOKE_METHOD);
                    pop(); // pop the result of executing Method.invoke(...)

                }

                /**
                 *  dump 类字段信息
                 *
                 * @param stage 1 -> enter 2-> exit
                 */
                private void handleDumpFields(int stage) {

                    Class<?> targetClass = sMethodDesc.getTargetClass();
                    if (targetClass == null) {
                        PSLogger.error("the target class is null, impossible !!!");
                        return;
                    }

                    // get all fields of this class
                    Set<ClassField> allFields = FieldValueExtract.getClassFields(targetClass);

                    if (allFields == null) {
                        PSLogger.error(String.format("the class [%s]'s fields not set up now, try next round", targetClass.getName()));
                        return;
                    }

                    handleClassField(stage, new ArrayList<>(allFields));
                }

                private void handleClassField(int stage, List<ClassField> allFields) {
                    // filter invalid field
                    filterInvalidField(allFields);

                    for (ClassField classField : allFields) {
                        String fieldName = classField.getFieldName();
                        String fieldType = classField.getFieldType();
                        String fieldDesc = classField.getDesc();

                        // load advice method
                        loadAdviceAsmMethod("field");

                        // push the first method param for Method.invoke
                        push((Type) null);

                        // new array
                        push(5);
                        newArray(OBJECT_TYPE);

                        // the stage value at index 0
                        dup();
                        push(0);
                        push(stage);
                        box(Type.getType(int.class));
                        arrayStore(Type.getType(Integer.class));

                        dup();
                        push(1);
                        push(fieldName);
                        arrayStore(Type.getType(String.class));

                        dup();
                        push(2);
                        push(fieldType);
                        arrayStore(Type.getType(String.class));

                        dup();
                        push(3);
                        push(fieldDesc);
                        arrayStore(Type.getType(String.class));

                        dup();
                        push(4);

                        String clsNameEx = sMethodDesc.getClassName();
                        clsNameEx = clsNameEx.replaceAll("\\.", "/");

                        if (classField.isStatic()) {
                            super.visitFieldInsn(Opcodes.GETSTATIC, clsNameEx, fieldName, fieldDesc);
                        } else {

                            // load this
                            super.visitVarInsn(Opcodes.ALOAD, 0);

                            // getField
                            super.visitFieldInsn(Opcodes.GETFIELD, clsNameEx, fieldName, fieldDesc);
                        }
                        boxIfNeeded(fieldDesc);

                        // store this class field val.
                        arrayStore(OBJECT_TYPE);

                        // do method call: Method.invoke(...)
                        invokeVirtual(METHOD_Type, METHOD_INVOKE_METHOD);
                        pop(); // pop the result of executing Method.invoke(...)
                    }
                }

                private void boxIfNeeded(final String desc) {
                    switch (Type.getType(desc).getSort()) {
                        case Type.CHAR:
                            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                            break;
                        case Type.BYTE:
                            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                            break;
                        case Type.BOOLEAN:
                            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                            break;
                        case Type.SHORT:
                            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                            break;
                        case Type.INT:
                            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                            break;
                        case Type.LONG:
                            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                            break;
                        case Type.FLOAT:
                            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                            break;
                        case Type.DOUBLE:
                            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                            break;
                    }
                }

                private void filterInvalidField(List<ClassField> allFields) {
                    Iterator<ClassField> itr = allFields.iterator();
                    while (itr.hasNext()) {
                        ClassField classField = itr.next();
                        if (classField == null || UTILS.isNullOrEmpty(classField.getFieldName())) {
                            itr.remove();
                            continue;
                        }

                        // if the field is "static", then using getStatic to get the field value, or
                        // using getField to get the field value.
                        if (!classField.isStatic()) {
                            // static call
                            // non-static call
                            if ((access & ACC_STATIC) != 0) {
                                itr.remove();
                            }

                        }
                    }

                }

                /**
                 *  完成方法进入的通知
                 *
                 */
                private void handlePreStart() {

                    // the if label
                    Label ifMatchSpecialConditionEndLabel = new Label();
                    Label gotoLabel = new Label();

                    // load check advice
                    loadAdviceAsmMethod("specialCondition");
                    push((Type) null);

                    // load the check param
                    loadPreEnterParam();

                    // check the condition
                    invokeVirtual(METHOD_Type, METHOD_INVOKE_METHOD);

                    mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");

                    int varIndex = newLocal(Type.getType(Boolean.class));
                    mv.visitVarInsn(ASTORE, varIndex);

                    mv.visitLabel(ifMatchSpecialConditionEndLabel);

                    mv.visitVarInsn(ALOAD, varIndex);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);

                    // skip the if statement if != true
                    mv.visitJumpInsn(IFEQ, gotoLabel);

                    // if () {

                    loadAdviceAsmMethod("specialAttach");
                    push((Type) null);
                    push((Type) null);

                    invokeVirtual(METHOD_Type, METHOD_INVOKE_METHOD);
                    pop(); // pop the ret

                    // }

                    mv.visitLabel(gotoLabel);
                }

                private void loadPreEnterParam() {
                    // new array with size 7 and type object
                    push(6);
                    newArray(OBJECT_TYPE);

                    // classloader at index 1
                    dup();
                    push(0);
                    loadClassLoader();
                    arrayStore(Type.getType(ClassLoader.class));

                    // class name at index 2
                    dup();
                    push(1);
                    push(tranClassName(sMethodDesc.getClassName()));
                    arrayStore(Type.getType(String.class));

                    // method name at index 3
                    dup();
                    push(2);
                    push(name);
                    arrayStore(Type.getType(String.class));

                    // method desc at index 4
                    dup();
                    push(3);
                    push(desc);
                    arrayStore(Type.getType(String.class));

                    // target object at index 5
                    dup();
                    push(4);
                    loadThisOrPushNullIfIsStatic();
                    arrayStore(OBJECT_TYPE);

                    // method params at index 6
                    dup();
                    push(5);
                    loadArgArray();
                    arrayStore(Type.getType(Object[].class));
                }

                /**
                 *  load the method enter params for advice
                 */
                private void loadMethodEnterParam() {
                    // new array with size 7 and type object
                    push(7);
                    newArray(OBJECT_TYPE);

                    // context id at index 0
                    dup();
                    push(0);
                    push(contextId);
                    box(Type.getType(int.class));
                    arrayStore(Type.getType(Integer.class));

                    // classloader at index 1
                    dup();
                    push(1);
                    loadClassLoader();
                    arrayStore(Type.getType(ClassLoader.class));

                    // class name at index 2
                    dup();
                    push(2);
                    push(tranClassName(sMethodDesc.getClassName()));
                    arrayStore(Type.getType(String.class));

                    // method name at index 3
                    dup();
                    push(3);
                    push(name);
                    arrayStore(Type.getType(String.class));

                    // method desc at index 4
                    dup();
                    push(4);
                    push(desc);
                    arrayStore(Type.getType(String.class));

                    // target object at index 5
                    dup();
                    push(5);
                    loadThisOrPushNullIfIsStatic();
                    arrayStore(OBJECT_TYPE);

                    // method params at index 6
                    dup();
                    push(6);
                    loadArgArray();
                    arrayStore(Type.getType(Object[].class));
                }

                /**
                 * load target object, this or null is ok
                 */
                private void loadThisOrPushNullIfIsStatic() {
                    if (isStaticMethod()) {
                        push((Type) null);
                    } else {
                        loadThis();
                    }
                }

                /**
                 *  用于加载通知方法
                 *
                 * @param adviceMethod 通知方法名称
                 */
                private void loadAdviceAsmMethod(String adviceMethod) {
                    if (UTILS.isNullOrEmpty(adviceMethod)) {
                        throw new IllegalStateException("error status: the adviceMethod must not null here");
                    }
                    switch (adviceMethod) {
                        case "starts":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_ENTER_CALL", METHOD_Type);
                            break;
                        case "end":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_EXIT_CALL", METHOD_Type);
                            break;
                        case "throw":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_THROW_EXCEPTION_CALL", METHOD_Type);
                            break;
                        case "line":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_INVOKE_LINE_CALL", METHOD_Type);
                            break;
                        case "var":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_INVOKE_LOCAL_VARIABLE_CALL", METHOD_Type);
                            break;
                        case "v":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_INVOKE_VAR_INS_CALL", METHOD_Type);
                            break;
                        case "v2":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_INVOKE_VAR_INS_V2_CALL", METHOD_Type);
                            break;
                        case "ii":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_INVOKE_VAR_INS_I_CALL", METHOD_Type);
                            break;
                        case "il":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_INVOKE_VAR_INS_L_CALL", METHOD_Type);
                            break;
                        case "if":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_INVOKE_VAR_INS_F_CALL", METHOD_Type);
                            break;
                        case "id":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_INVOKE_VAR_INS_D_CALL", METHOD_Type);
                            break;
                        case "ia":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_INVOKE_VAR_INS_A_CALL", METHOD_Type);
                            break;
                        case "specialCondition":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_IN_SPECIAL_CONDITION_JUDGE_CALL", METHOD_Type);
                            break;
                        case "specialAttach":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_IN_SPECIAL_CONDITION_TRANS_DATA_GET_CALL", METHOD_Type);
                            break;
                        case "specialTransfer":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_IN_SPECIAL_DATA_TRANS_CALL", METHOD_Type);
                            break;
                        case "field":
                            getStatic(WEAVE_SPY_TYPE, "ON_METHOD_FIELD_INVOKE_CALL", METHOD_Type);
                            break;
                        default:
                            throw new IllegalArgumentException("illegal advice key:" + adviceMethod);
                    }

                }

                /**
                 * 是否静态方法
                 * @return true:静态方法 / false:非静态方法
                 */
                private boolean isStaticMethod() {
                    return (methodAccess & ACC_STATIC) != 0;
                }

                /**
                 * 翻译类名称<br/>
                 * 将 java/lang/String 的名称翻译成 java.lang.String
                 *
                 * @param className 类名称 java/lang/String
                 * @return 翻译后名称 java.lang.String
                 */
                private String tranClassName(String className) {
                    return className.replace("/", ".");
                }

                /**
                 *  用于加载类的classLoader
                 *
                 */
                private void loadClassLoader() {

                    // todo: 如果有classLoader的需求，再将下面注释掉的代码放出来 !
                    push((Type) null);

//                    if (isStaticMethod()) {
//                        // 静态方法的classLoader加载使用Class.forName()来完,因为有可能当前这个静态方法在执行的时候
//                        // 当前类并没有完成实例化,会引起JVM对class文件的合法性校验失败
//                        String name = sMethodDesc.getClassName();
//                        name = name.replace("/", ".");
//                        visitLdcInsn(name);
//                        invokeStatic(CLASS_TYPE, Method.getMethod("Class forName(String)"));
//                        invokeVirtual(CLASS_TYPE, Method.getMethod("ClassLoader getClassLoader()"));
//                    } else {
//                        // 实例方法只需要访问Class.getClassLoader()即可
//                        loadThis();
//                        invokeVirtual(OBJECT_TYPE, Method.getMethod("Class getClass()"));
//                        invokeVirtual(CLASS_TYPE, Method.getMethod("ClassLoader getClassLoader()"));
//                    }
                }

                /**
                 * Called before explicit exit from the method using either return or throw.
                 * Top element on the stack contains the return value or exception instance.
                 * For example:
                 *
                 * <pre>
                 *   public void onMethodExit(int opcode) {
                 *     if(opcode==RETURN) {
                 *         visitInsn(ACONST_NULL);
                 *     } else if(opcode==ARETURN || opcode==ATHROW) {
                 *         dup();
                 *     } else {
                 *         if(opcode==LRETURN || opcode==DRETURN) {
                 *             dup2();
                 *         } else {
                 *             dup();
                 *         }
                 *         box(Type.getReturnType(this.methodDesc));
                 *     }
                 *     visitIntInsn(SIPUSH, opcode);
                 *     visitMethodInsn(INVOKESTATIC, owner, "onExit", "(Ljava/lang/Object;I)V");
                 *   }
                 *
                 *   // an actual call back method
                 *   public static void onExit(Object param, int opcode) {
                 *     ...
                 * </pre>
                 *
                 * <br>
                 * <br>
                 *
                 * <i>Custom code can use or change all the local variables, but should not
                 * change state of the stack.</i>
                 *
                 * @param opcode
                 *            one of the RETURN, IRETURN, FRETURN, ARETURN, LRETURN, DRETURN
                 *            or ATHROW
                 *
                 */
                @Override
                protected void onMethodExit(int opcode) {
                    super.onMethodExit(opcode);

                    // 字段
                    if (weaveConfig.isField() && weaveConfig.isFieldDiff()) {
                        handleDumpFields(2);
                    }

                    // return or throw exception
                    if (opcode != ATHROW) {
                        // advice the exit event
                        //handleExit(opcode);

                        handleExitV2(opcode);
                    }
                }

                /**
                 *  load the return / throw result as array & store at the top of current stack
                 *
                 */
                private void loadReturnArgs() {
                    dup2X1();
                    pop2();
                    push(1);
                    newArray(OBJECT_TYPE);
                    dup();
                    dup2X1();
                    pop2();
                    push(0);
                    swap();
                    arrayStore(OBJECT_TYPE);
                }

                /**
                 *  handle method exit
                 *
                 * @param opCode the opCode of exit method, like {@link Opcodes#ATHROW ...}
                 */
                private void handleExit(int opCode) {
                    // load the return array
                    loadReturn(opCode);

                    // load the advice method
                    loadAdviceAsmMethod("end");

                    // Method.invoke's params
                    push((Type) null);
                    loadReturnArgs();

                    // invoke the advice method
                    invokeVirtual(METHOD_Type, METHOD_INVOKE_METHOD);
                    pop(); // pop the result
                }

                private void handleExitV2(int opCode) {
                    // load return
                    int index = loadReturnV2(opCode);

                    // load advice method
                    loadAdviceAsmMethod("end");

                    // Method.invoke's params
                    push((Type) null);

                    // load the param array
                    push(4);
                    newArray(OBJECT_TYPE);

                    dup();
                    push(0);
                    if (index == -1) {
                        push((Type) null);
                        arrayStore(OBJECT_TYPE);
                    } else {
                        switch (opCode) {
                            case ARETURN: {
                                mv.visitVarInsn(ALOAD, index);
                                arrayStore(OBJECT_TYPE);
                                break;
                            }
                            case LRETURN: {
                                mv.visitVarInsn(LLOAD, index);
                                box(Type.getType(long.class));
                                arrayStore(Type.getType(Long.class));
                                break;
                            }
                            case DRETURN: {
                                mv.visitVarInsn(DLOAD, index);
                                box(Type.getType(double.class));
                                arrayStore(Type.getType(Double.class));
                                break;
                            }
                            case FRETURN: {
                                mv.visitVarInsn(FLOAD, index);
                                box(Type.getType(float.class));
                                arrayStore(Type.getType(Float.class));
                                break;
                            }
                            default: {
                                mv.visitVarInsn(ALOAD, index);
                                arrayStore(OBJECT_TYPE);
                                break;
                            }
                        }
                    }

                    // class name
                    dup();
                    push(1);
                    push(tranClassName(sMethodDesc.getClassName()));
                    arrayStore(Type.getType(String.class));

                    // method name
                    dup();
                    push(2);
                    push(sMethodDesc.getName());
                    arrayStore(Type.getType(String.class));

                    // method desc
                    dup();
                    push(3);
                    push(sMethodDesc.getDesc());
                    arrayStore(Type.getType(String.class));

                    // invoke the advice method
                    invokeVirtual(METHOD_Type, METHOD_INVOKE_METHOD);
                    pop(); // pop the result

                }

                /**
                 * 加载返回值
                 * @param opcode 操作吗
                 */
                private void loadReturn(int opcode) {
                    switch (opcode) {
                        case RETURN: {
                            push((Type) null);
                            break;
                        }
                        case ARETURN: {
                            dup();
                            break;
                        }
                        case LRETURN:
                        case DRETURN: {
                            dup2();
                            box(Type.getReturnType(methodDesc));
                            break;
                        }
                        default: {
                            dup();
                            box(Type.getReturnType(methodDesc));
                            break;
                        }
                    }
                }

                private int loadReturnV2(int opcode) {
                    switch (opcode) {
                        case RETURN: {
                           return -1;
                        }
                        case ARETURN: {
                            dup();
                            int local = newLocal(Type.getType(Object.class));
                            mv.visitVarInsn(ASTORE, local);
                            return local;
                        }
                        case LRETURN: {
                            dup2();
                            box(Type.getReturnType(methodDesc));
                            int local = newLocal(Type.getReturnType(methodDesc));
                            mv.visitVarInsn(LSTORE, local);
                            return local;
                        }
                        case DRETURN: {
                            dup2();
                            box(Type.getReturnType(methodDesc));
                            int local = newLocal(Type.getReturnType(methodDesc));
                            mv.visitVarInsn(DSTORE, local);
                            return local;
                        }
                        case FRETURN: {
                            dup();
                            box(Type.getReturnType(methodDesc));
                            int local = newLocal(Type.getReturnType(methodDesc));
                            mv.visitVarInsn(FSTORE, local);
                            return local;
                        }
                        default: {
                            dup();
                            box(Type.getReturnType(methodDesc));
                            int local = newLocal(Type.getReturnType(methodDesc));
                            mv.visitVarInsn(ASTORE, local);
                            return local;
                        }
                    }
                }

                /**
                 * Visits the maximum stack size and the maximum number of local variables
                 * of the method.
                 *
                 * @param maxStack
                 *            maximum stack size of the method.
                 * @param maxLocals
                 *            maximum number of local variables for the method.
                 */
                @Override
                public void visitMaxs(int maxStack, int maxLocals) {
                    mark(catch_label);
                    catchException(try_label, catch_label, THROWABLE_TYPE);

                    // 字段
                    if (weaveConfig.isField() && weaveConfig.isFieldDiff()) {
                        handleDumpFields(2);
                    }

                    // dup a throw
                    dup();
                    int local = newLocal(Type.getType(Throwable.class));
                    mv.visitVarInsn(ASTORE, local);

                    //todo: 类字段和对象字段在此加载传递下去

                    // load the method
                    loadAdviceAsmMethod("throw");

                    // method.invoke(null, ...)
                    push((Type) null);

                    // method.invoke(..., exception)
                    //loadReturnArgs();

                    push(4);
                    newArray(OBJECT_TYPE);

                    dup();
                    push(0);
                    mv.visitVarInsn(ALOAD, local);
                    arrayStore(Type.getType(Throwable.class));

                    // class name
                    dup();
                    push(1);
                    push(tranClassName(sMethodDesc.getClassName()));
                    arrayStore(Type.getType(String.class));

                    // method name
                    dup();
                    push(2);
                    push(sMethodDesc.getName());
                    arrayStore(Type.getType(String.class));

                    // method desc
                    dup();
                    push(3);
                    push(sMethodDesc.getDesc());
                    arrayStore(Type.getType(String.class));

                    // invoke advice method
                    invokeVirtual(METHOD_Type, METHOD_INVOKE_METHOD);
                    pop();

                    // throw this exception
                    throwException();

                    super.visitMaxs(maxStack, maxLocals);
                }

                /**
                 * Visits a line number declaration.
                 *
                 * @param line
                 *            a line number. This number refers to the source file from
                 *            which the class was compiled.
                 * @param start
                 *            the first instruction corresponding to this line number.
                 * @throws IllegalArgumentException
                 *             if <tt>start</tt> has not already been visited by this
                 *             visitor (by the {@link #visitLabel visitLabel} method).
                 */
                @Override
                public void visitLineNumber(int line, Label start) {

                    // load advice method
                    loadAdviceAsmMethod("line");

                    // method.invoke
                    push((Type) null);

                    // load line
                    wrapLine(line);

                    // advice here
                    invokeVirtual(METHOD_Type, METHOD_INVOKE_METHOD);
                    pop(); // pop ret (consume it!)

                    super.visitLineNumber(line, start);
                }

                private void wrapLine(int line) {
                    push(1);
                    newArray(OBJECT_TYPE);

                    // push the line number on the stack with wrap by array
                    dup();
                    push(0);
                    push(line);
                    box(Type.getType(int.class));
                    arrayStore(Type.getType(Integer.class));
                }

                /**
                 * Visits a local variable declaration.
                 *
                 * @param name
                 *            the name of a local variable.
                 * @param desc
                 *            the type descriptor of this local variable.
                 * @param signature
                 *            the type signature of this local variable. May be
                 *            <tt>null</tt> if the local variable type does not use generic
                 *            types.
                 * @param start
                 *            the first instruction corresponding to the scope of this local
                 *            variable (inclusive).
                 * @param end
                 *            the last instruction corresponding to the scope of this local
                 *            variable (exclusive).
                 * @param index
                 *            the local variable's index.
                 * @throws IllegalArgumentException
                 *             if one of the labels has not already been visited by this
                 *             visitor (by the {@link #visitLabel visitLabel} method).
                 */
                @Override
                public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {

                    handleVisitLocalVariable(name, desc, signature, index);

                    super.visitLocalVariable(name, desc, signature, start, end, index);
                }

                /**
                 *  局部变量的访问比较特殊，这在运行前就可以确定，所以不需要织入代码，直接访问advice即可
                 *
                 * @param name 变量名称
                 * @param desc 描述
                 * @param signature 泛型标签
                 * @param index 局部变量表中的index，可能会变，看编译器优化
                 */
                private void handleVisitLocalVariable(String name, String desc, String signature, int index) {
                    invokeLocalVariable(sMethodDesc.getClassName(), sMethodDesc.getName(),
                            sMethodDesc.getDesc(), name, desc, signature, index);
                }

                /**
                 * Visits a jump instruction. A jump instruction is an instruction that may
                 * jump to another instruction.
                 *
                 * @param opcode
                 *            the opcode of the type instruction to be visited. This opcode
                 *            is either IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ,
                 *            IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
                 *            IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
                 * @param label
                 *            the operand of the instruction to be visited. This operand is
                 *            a label that designates the instruction to which the jump
                 *            instruction may jump.
                 */
                @Override
                public void visitJumpInsn(int opcode, Label label) {
                    super.visitJumpInsn(opcode, label);
                }

                /**
                 * Visits a local variable instruction. A local variable instruction is an
                 * instruction that loads or stores the value of a local variable.
                 *
                 * @param opcode
                 *            the opcode of the local variable instruction to be visited.
                 *            This opcode is either ILOAD, LLOAD, FLOAD, DLOAD, ALOAD,
                 *            ISTORE, LSTORE, FSTORE, DSTORE, ASTORE or RET.
                 * @param var
                 *            the operand of the instruction to be visited. This operand is
                 *            the index of a local variable.
                 */
                @Override
                public void visitVarInsn(int opcode, int var) {

                    // v1
                    //handleVarInsV1(opcode, var);

                    // v2
                    handleVarInsV2(opcode, var);

                    super.visitVarInsn(opcode, var);
                }

                /**
                 *  V2版本变量变更通知处理
                 *
                 * @param opcode 操作符
                 * @param var 操作变量
                 */
                private void handleVarInsV2(int opcode, int var) {
                    String opType;
                    int localIndex;
                    switch (opcode) {
                        /////// STORE OP
                        case ASTORE:
                            dup();
                            localIndex = newLocal(Type.getType(Object.class));
                            mv.visitVarInsn(ASTORE, localIndex);
                            opType =  "ASTORE";
                            break;
                        case ISTORE:
                            dup();
                            localIndex = newLocal(Type.getType(Integer.class));
                            mv.visitVarInsn(ISTORE, localIndex);
                            opType =  "ISTORE";
                            break;
                        case DSTORE:
                            dup2();
                            localIndex = newLocal(Type.getType(Double.class));
                            mv.visitVarInsn(DSTORE, localIndex);
                            opType = "DSTORE";
                            break;
                        case LSTORE:
                            dup2();
                            localIndex = newLocal(Type.getType(Long.class));
                            mv.visitVarInsn(LSTORE, localIndex);
                            opType = "LSTORE";
                            break;
                        case FSTORE:
                            //dup2();
                            dup();
                            localIndex = newLocal(Type.getType(Float.class));
                            mv.visitVarInsn(FSTORE, localIndex);
                            opType = "FSTORE";
                            break;
                        default:
                            return;
                    }

                    switch (opType) {
                        case "ISTORE":
                            loadAdviceAsmMethod("ii");
                            push((Type) null);
                            break;
                        case "ASTORE":
                            loadAdviceAsmMethod("ia");
                            push((Type) null);
                            break;
                        case "DSTORE":
                            loadAdviceAsmMethod("id");
                            push((Type) null);
                            break;
                        case "LSTORE":
                            loadAdviceAsmMethod("il");
                            push((Type) null);
                            break;
                        case "FSTORE":
                            loadAdviceAsmMethod("if");
                            push((Type) null);
                            break;
                    }

                    push(3);
                    newArray(OBJECT_TYPE);

                    dup();
                    push(0);
                    switch (opType) {
                        case "ISTORE":
                            mv.visitVarInsn(ILOAD, localIndex);
                            box(Type.getType(int.class));
                            arrayStore(Type.getType(Integer.class));
                            break;
                        case "ASTORE":
                            mv.visitVarInsn(ALOAD, localIndex);
                            arrayStore(OBJECT_TYPE);
                            break;
                        case "DSTORE":
                            mv.visitVarInsn(DLOAD, localIndex);
                            box(Type.getType(double.class));
                            arrayStore(Type.getType(Double.class));
                            break;
                        case "LSTORE":
                            mv.visitVarInsn(LLOAD, localIndex);
                            box(Type.getType(long.class));
                            arrayStore(Type.getType(Long.class));
                            break;
                        case "FSTORE":
                            mv.visitVarInsn(FLOAD, localIndex);
                            box(Type.getType(float.class));
                            arrayStore(Type.getType(Float.class));
                            break;
                    }

                    // var index
                    dup();
                    push(1);
                    push(var);
                    box(Type.getType(int.class));
                    arrayStore(Type.getType(Integer.class));

                    // opType
                    dup();
                    push(2);
                    push(opType);
                    arrayStore(Type.getType(String.class));

                    // do method invoke here
                    invokeVirtual(METHOD_Type, METHOD_INVOKE_METHOD);
                    pop(); // pop result of Method.invoke

                }

                /**
                 *  V1版本变量通知处理
                 *
                 * @param opcode 操作符
                 * @param var 操作变量
                 */
                private void handleVarInsV1(int opcode, int var) {
                    // load advice method
                    loadAdviceAsmMethod("v");

                    // method.invoke
                    push((Type) null);

                    handleVisitVarIns(opcode, var);

                    // actual invoke the target advice method here
                    invokeVirtual(METHOD_Type, METHOD_INVOKE_METHOD);
                    pop(); // don't left it to stack
                }

                /**
                 *  load the advice params
                 *
                 * @param opCode opCode
                 * @param varIndex the invoking variable index
                 */
                private void handleVisitVarIns(int opCode, int varIndex) {

                    // new array with size = 2 and type = object
                    push(2);
                    newArray(OBJECT_TYPE);

                    // array[0] = opCode
                    dup();
                    push(0);
                    push(opCode);
                    box(Type.getType(int.class));
                    arrayStore(Type.getType(Integer.class));

                    // array[1] = varIndex
                    dup();
                    push(1);
                    push(varIndex);
                    box(Type.getType(int.class));
                    arrayStore(Type.getType(Integer.class));
                }

            };
        } catch (Throwable e) {
            PSLogger.error("could not weave method:" + sMethodDesc + ":" + e);
            return mv;
        }
    }

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

        // 是否是lambda相关方法调用
        // todo: 匿名内部类方法调用
//        if (name.contains("lambda$") /*&& MAGIC_TAG.get()*/) {
//            return true;
//        }

        if (!sMethodDesc.isMatch(name, desc)) {
            return false;
        }

        // black method list
        for (MethodDesc md : BLACK_METHOD_SET) {
            if (md.isMatch(name, desc)) {
                return false;
            }
        }

        // custom method matcher
        return sMethodDesc != null && sMethodDesc.isMatch(name, desc);
    }

    // 不准确的局部变量管理
    static class MapBaseLocalVariableTable implements LocalVariableTable {

        private Map<Integer, String> localVariableTable = new HashMap<>();

        /**
         *  设置一个局部变量到map中去
         *
         * @param index index
         * @param varName 名称
         */
        void setVar(int index, String varName) {
            localVariableTable.put(index, varName);
        }

        /**
         * 这个方法用于提供根据index查询局部变量名称的服务，本身就是方法级别的，所以
         * 不做区分
         *
         * @param index var index
         * @return 变量名称，可能不准确，不要强依赖
         */
        @Override
        public String valueAt(int index) {
            return localVariableTable.get(index);
        }

        @Override
        public String toString() {
            return "localTable:" + localVariableTable;
        }
    }

}
