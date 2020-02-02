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


package io.javadebug.agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created on 2019/4/14 12:02.
 *
 * @author HuJian
 */
public class Agent {

    ///  --------------------------------------------
    /// 这个属性很重要，因为重新加载agent需要STW JVM，目前测试发现重新加载Agent
    /// 大概需要150 ms，相当于目标JVM执行一次FullGC；如果Agent类变多，这个时间
    /// 还会继续恶化，所以，使用这个标记来控制是否允许重新加载Agent，理论上，不要
    /// 打开，除非发现Agent存在重大Bug的时候，不得不重新加载Agent的时候可以打开
    /// 这个开关，并再次编译，在目标JVM上执行即可
    ///  --------------------------------------------
    private static final boolean allow_to_reload_agent = false;

    ///  --------------------------------------------
    /// Agent类加载器，使用自定义的、打破双亲委派的类加载器来加载Agent，这样Agent
    /// 就不会对目标JVM造成了类污染，甚至造成不可逆转的错误，当然，jdk类库还是从系统
    /// 类加载器中获取，避免目标JVM中出现两份jdk类
    /// java.*
    ///  --------------------------------------------
    private static volatile ClassLoader cachedAgentClassLoadHolder = null;

    private static final PrintStream ps = System.err;

    /**
     *  拿到Agent的类加载器
     *
     * @return {@link AgentClassLoader}
     */
    public static ClassLoader getAgentClassLoader() {
        if (cachedAgentClassLoadHolder == null) {
            throw new RuntimeException("AgentClassloader暂时不可用");
        }
        return cachedAgentClassLoadHolder;
    }

    /**
     *  获取agent需要的隔离的类加载器
     *
     * @param jarPath 系统打好的jar包
     * @return {@link AgentClassLoader}
     * @throws Throwable 无法获取到系统需要的类加载器
     */
    private static ClassLoader getAgentClassLoadHolder(String jarPath) throws Throwable {
        final ClassLoader classLoader;
        if (null != cachedAgentClassLoadHolder) {
             classLoader = cachedAgentClassLoadHolder;
        } else {
            classLoader = new AgentClassLoader(jarPath);
            cachedAgentClassLoadHolder = classLoader;
        }

        // init the agent class loader
        WeaveSpy.initAgentClassLoader(cachedAgentClassLoadHolder);

        System.err.println("开始初始化weave");

        Class<?> weaveClass = cachedAgentClassLoadHolder.loadClass("io.javadebug.core.enhance.ClassMethodWeaver");
        if (weaveClass == null) {
            throw new IllegalStateException("impossible null!");
        }

        WeaveSpy.installAdviceMethod(
                weaveClass.getMethod("_onMethodEnter", int.class, ClassLoader.class, String.class, String.class, String.class, Object.class, Object[].class),
                weaveClass.getMethod("_onMethodExit", Object.class, String.class, String.class, String.class),
                weaveClass.getMethod("_onMethodThrowing", Throwable.class, String.class, String.class, String.class),
                weaveClass.getMethod("invokeLine", int.class),
                weaveClass.getMethod("invokeVarInstruction", int.class, int.class),
                weaveClass.getMethod("invokeVarInstructionV2", Object.class, int.class, String.class),
                weaveClass.getMethod("invokeIntVarInstruction", int.class, int.class, String.class),
                weaveClass.getMethod("invokeLongVarInstruction", long.class, int.class, String.class),
                weaveClass.getMethod("invokeFloatVarInstruction", float.class, int.class, String.class),
                weaveClass.getMethod("invokeDoubleVarInstruction", double.class, int.class, String.class),
                weaveClass.getMethod("invokeObjectVarInstruction", Object.class, int.class, String.class),
                weaveClass.getMethod("invokeSpecialDataTrans", Object.class),
                weaveClass.getMethod("checkSpecialCondition", ClassLoader.class, String.class, String.class, String.class, Object.class, Object[].class),
                weaveClass.getMethod("specialDataTransGet"),
                weaveClass.getMethod("onFieldNotice", int.class, String.class, String.class, String.class, Object.class)
                );

        System.err.println("成功加载weave spy");

        return classLoader;
    }


    /// static attach
    public static void premain(String args, Instrumentation inst) throws Exception {
        doAttachJvm(args, inst);
    }

    /// dynamic attach by VirtualMachine.attach
    public static void agentmain(String args, Instrumentation inst) throws Exception {
        doAttachJvm(args, inst);
    }

    /**
     *  读取一个java字节码文件
     *
     * @param filePath 文件路径
     * @return 字节码数组
     * @throws Exception 读取异常
     */
    private static byte[] loadFile(String filePath) throws Exception {
        if (isNullOrEmpty(filePath)) {
            throw new NullPointerException("不合法的文件路径");
        }
        File file = new File(filePath);
        InputStream is = new FileInputStream(file);
        long length = file.length();
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead;
        while (offset <bytes.length &&
                (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not read file completely:" + file.getName());
        }
        is.close();
        return bytes;
    }

    /**
     *  从输入流中读取到数据
     *
     * @param is {@link InputStream}
     * @return 读取到的内容
     * @throws Exception 读取异常
     */
    private static byte[] readFromIs(InputStream is) throws Exception {
        if (is == null) {
            return new byte[]{};
        }
        byte[] data = new byte[is.available()];
        int offset = 0;
        int readCnt;
        while (offset < data.length &&
                (readCnt = is.read(data, offset, data.length - offset)) >= 0) {
            offset += readCnt;
        }
        if (offset < data.length) {
            throw new IOException("Could not read file completely:" + is.toString());
        }
        return data;
    }

    /**
     *  执行一段脚本
     *
     * @param cmd 命令内容
     * @param args 参数
     * @return 命令执行结果
     */
    private static String executeShell(String cmd, String args) {
        if (isNullOrEmpty(cmd)) {
            return "";
        }
        BufferedReader input = null;
        try {
            if (isNullOrEmpty(args)) {
                cmd += " " + args;
            }
            Process process = Runtime.getRuntime().exec(cmd);
            input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = input.readLine()) != null) {
                sb.append(line).append("\n");
            }
            int code = process.waitFor();
            if (code != 0) {
                throw new RuntimeException("执行shell返回非0结果");
            }
            return sb.toString();
        } catch (Exception e) {
            ps.println("无法执行脚本：" + e);
            return "";
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *  为了避免更新jar包不能表现在目标JVM上，当发现重新Attach的时候，重新加载core
     *
     * @param instrumentation {@link Instrumentation} 增强
     * @throws Exception 操作失败
     */
    private static void reloadAgent(Instrumentation instrumentation, String coreJarPath) throws Exception {
        ClassLoader agentClassLoader = cachedAgentClassLoadHolder;
        if (agentClassLoader == null) {
            ps.println("无法重新加载代理，因为无法获取到代理类加载器");
            return;
        }
        if (isNullOrEmpty(coreJarPath)) {
            ps.println("无效的代码jar包路径");
            return;
        }

        // 不要随意变更package
        String baseAgentPkg = "io.javadebug.core";
        String formatPkg = "io/javadebug/core";

        URL url = agentClassLoader.getResource(formatPkg);
        if (url == null) {
            ps.println("could not get the core package url");
            return;
        }
        // jar protocol
        if (!"jar".equals(url.getProtocol())) {
            ps.println("wrong url protocol find, expect jar: but find:" + url.getProtocol());
            return;
        }

        Class<?> utilCls = agentClassLoader.loadClass("io.javadebug.core.UTILS");
        if (utilCls == null) {
            ps.println("无法拿到UTILS类，是不是变更类名了..");
            return;
        }

        // 是否需要重新加载Agent判断方法
        Method needToReloadMethod = utilCls.getMethod("needToReload", byte[].class);

        if (needToReloadMethod == null) {
            ps.println("无法拿到方法:[UTILS.needToReloadMethod]");
            return;
        }

        JarFile coreJarFile = new JarFile(coreJarPath);
        Enumeration<JarEntry> jarEntryEnumeration = coreJarFile.entries();
        while (jarEntryEnumeration.hasMoreElements()) {
            JarEntry jarEntry = jarEntryEnumeration.nextElement();
            String jarEntryName = jarEntry.getName();
            jarEntryName = jarEntryName.replace("/", ".");
            if (jarEntryName.startsWith(baseAgentPkg) && jarEntryName.endsWith(".class")) {
                // Instrumentation is useful here
                try {
                    String className = jarEntryName.replace(".class", "");

                    // get the class
                    Class<?> theClass = agentClassLoader.loadClass(className);

                    // read to bytes
                    InputStream is = coreJarFile.getInputStream(jarEntry);
                    byte[] classFileBytes = readFromIs(is);
                    if (classFileBytes == null || classFileBytes.length == 0) {
                        continue;
                    }

                    // 类校验，是否需要重新加载，是否允许重新加载，通过校验才能进行reload，否则会很难受
                    boolean ret = (boolean) needToReloadMethod.invoke(null, (Object) classFileBytes);
                    if (!ret) {
                        ps.println(className + " 不能被重定义");
                        continue;
                    }

                    // redefine the file
                    ps.println("redefine class:" + className);
                    instrumentation.redefineClasses(new ClassDefinition(theClass, classFileBytes));

                } catch (Exception e) {
                    // tell me what happen.
                    e.printStackTrace();
                    ps.println("could not reload core class:" + jarEntryName);
                }
            }
        }
    }

    /**
     *  实现静态/动态挂载到运行时的JVM上去
     *
     * @param args 参数，需要解析一下，中间通过逗号分隔","
     * @param inst {@link Instrumentation}
     */
    private static synchronized void doAttachJvm(String args, Instrumentation inst) {

        ps.println("I Am run in Thread:" + Thread.currentThread().getName());

        // 没有提供参数，只能就此结束
        if (isNullOrEmpty(args)) {
            ps.println("需要提供挂在JVM所需要的参数，[coreJar;configure]");
            return;
        }

        String[] argsArr = args.split(";");

        if (isNullOrEmptyArray(argsArr)) {
            ps.println("需要提供挂在JVM所需要的参数，[coreJar;configure]");
            return;
        }

        String coreJar = argsArr[0];
        String config = argsArr[1];

        if (coreJar.startsWith("//")) {
            coreJar = coreJar.substring(1);
        }

        ////// 开始尝试挂载JVM

        try {
            /// 将Agent添加到Bootstrap ClassLoader中去
            //  When the system class loader for delegation (see
            //  {@link java.lang.ClassLoader#getSystemClassLoader getSystemClassLoader()})
            //  unsuccessfully searches for a class, the entries in the
            // {@link java.util.jar.JarFile JarFile} will be searched as well.
            /// 需要在目标JVM上获取到Agent相关信息，所以这里添加到BootstrapClassLoader的类搜索路径中
            /// 需要注意的是，该Agent指的是debug-agent.jar，debug-core.jar是被Agent 加载进来的，不要
            /// 吧debug-core.jar添加到BootstrapClassLoader的类搜索路径中，否则会对目标JVM造成类污染，后果不堪设想
            inst.appendToBootstrapClassLoaderSearch(
                    new JarFile(WeaveSpy.class.getProtectionDomain().getCodeSource().getLocation().getFile())
            );

            /// agent classLoader，不要对目标JVM产生影响
            final ClassLoader agentClassLoader = getAgentClassLoadHolder(coreJar);

            if (agentClassLoader == null) {
                ps.println("无法获取到类加载器");
                return;
            }

            /// 加载Configure类，解析一下参数看看
            Class<?> configureCls = agentClassLoader.loadClass("io.javadebug.core.Configure");

            if (configureCls == null) {
                ps.println("无法加载io.javadebug.Configure类");
                return;
            }

            Object configureObj =
                    configureCls.getMethod("toConfigure", String.class).invoke(null/*静态方法*/, config);

            ps.println("解析出参数：" + configureObj.toString());

            /// 加载server并启动
            Class<?> remoteServerCls = agentClassLoader.loadClass("io.javadebug.core.transport.NettyTransportServer");

            if (remoteServerCls == null) {
                ps.println("无法加载远程server类：[io.javadebug.core.transport.NettyTransportServer]");
                return;
            }
            ps.println("[io.javadebug.core.transport.NettyTransportServer] ： " + remoteServerCls);

            String pid = (String) configureCls.getMethod("getPid").invoke(configureObj);

            if (isNullOrEmpty(pid)) {
                ps.println("请提供需要挂载的java进程号，可通过 jps命令获取");
                return;
            }
            ps.println("get pid: " + pid);

            Object remoteNettyServerObject = remoteServerCls
                    .getMethod("getNettyTransportServer", Instrumentation.class)
                    .invoke(null /*静态方法*/, inst);

            if (remoteNettyServerObject == null) {
                ps.println("无法获取到远程remoteNettyServerObject");
                return;
            }

            ps.println("get remoteNettyServerObject:" + remoteNettyServerObject);

            /// 检测一下是否已经启动过了
            boolean isBindCheck = (boolean) remoteServerCls.getMethod("isBind").invoke(remoteNettyServerObject);
            if (isBindCheck) {
                ps.println("服务端已经启动，不需要重复启动");
                if (allow_to_reload_agent) {
                    long start = System.currentTimeMillis();

                    // re-load the agent cause the agent is modified
                    reloadAgent(inst, coreJar);

                    ps.println("重新加载Agent完成, 耗时:" + (System.currentTimeMillis() - start) + " ms");
                }
            } else {
                /// 启动服务端
                try {
                    remoteServerCls.getMethod("start", configureCls).invoke(remoteNettyServerObject, configureObj);
                    ps.println("服务端启动：" + configureCls.getMethod("toString").invoke(configureObj));
                } catch (Exception e) {
                    // 启动失败
                    remoteServerCls.getMethod("stop", configureCls).invoke(remoteNettyServerObject, configureObj);
                    ps.println("服务端启动失败:" + e);
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
            ps.println(e);
        }

    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static <T> boolean isNullOrEmptyArray(Object[] arr) {
        return arr == null || arr.length == 0;
    }

}
