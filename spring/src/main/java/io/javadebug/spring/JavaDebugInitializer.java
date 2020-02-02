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


package io.javadebug.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

public enum JavaDebugInitializer implements BeanFactoryPostProcessor, Ordered {
    JAVA_DEBUG_INITIALIZER
    ;

    // the git clone repository
    private static final String REMOTE_GIT_REPOSITORY = "";

    // the tcp server class
    private static final String AGENT_TCP_SERVER_CLASS_NAME = "io.javadebug.core.transport.NettyTransportServer";

    // weave spy class name
    private static final String AGENT_WEAVE_SPY_CLASS_NAME = "io.javadebug.agent.WeaveSpy";

    // the flag
    private static AtomicBoolean isInitialize = new AtomicBoolean(false);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }

    /**
     *  initial the java-debug agent
     *
     * @return the instance
     */
    public static JavaDebugInitializer initializer() {
        // already init
        if (isInitialize.get()) {
            return JAVA_DEBUG_INITIALIZER;
        }

        // another thread init
        if (!isInitialize.compareAndSet(false, true)) {
            return JAVA_DEBUG_INITIALIZER;
        }

        // install & attach from spring-jar
        try {
            return installFromJar();
        } catch (Throwable e) {
            System.err.println("[fallback install]error occ while install from jar:" + e);
            return fallbackInstall();
        }

    }

    /**
     *  install from jar
     *  1. find the bin dir
     *  2. attach to target jvm
     *
     * @return the instance
     */
    private static JavaDebugInitializer installFromJar() {

        String classPath = JavaDebugInitializer.class.getResource("/bin/").getPath();

        if (classPath == null || classPath.isEmpty()) {
            throw new IllegalStateException("could not locate resource path:/bin/");
        }

        int exitCode;

        // uncompress the jar
        if (classPath.contains("jar")) {
            // get the jar path
            classPath = classPath.split("/bin/")[0];
            if (classPath.endsWith("!")) {
                classPath = classPath.substring(0, classPath.length() - 1);
            }

            System.err.println("get the jar path:" + classPath);

            if (classPath.startsWith("file:")) {
                classPath = classPath.split("file:")[1];
            }
            if (classPath.startsWith("jar:")) {
                classPath = classPath.split("jar:")[1];
            }

            String workDir = new File(classPath).getParent();

            System.err.println("start to uncompress jar:[" + classPath + "]");

            exitCode = execShell("jar xvf " + classPath, workDir);

            if (exitCode != 0) {
                throw new IllegalStateException("could not uncompress jar:"  +classPath);
            }

            // +x
            //execShell("chmod +x bin/*.sh", workDir);

            System.err.println("get the work path:" + workDir);

            classPath = workDir;
        }

        if (classPath.endsWith("/")) {
            classPath += "bin/";
        } else {
            classPath += "/bin/";
        }

        System.err.println("get the resource path:"  + classPath);

        String attachAgentShellPath = classPath + "javadebug-agent-attach.sh";
        String debugAgentJarPath = classPath + "lib/debug-agent.jar";
        String debugCoreJarPath = classPath + "lib/debug-core.jar";

        System.err.println(String.format(
                "get the attach shell path:[%s]\nagent jar:[%s]\ncore jar:[%s]\n",
                attachAgentShellPath, debugAgentJarPath, debugCoreJarPath));

        // get the current jvm pid
        String pid = getProcessId();
        if (pid.isEmpty()) {
            throw new IllegalStateException("could not get the current jvm pid");
        }

        // attach target jvm
        System.err.println(String.format("get the current jvm pid:%s, start to attach to it.", pid));

        // do attach
        exitCode = execShell("sh javadebug-agent-attach.sh" + " " + pid, classPath);

        if (exitCode != 0) {
            System.err.println("try to using common shell to attach target jvm:" + pid);
            exitCode = execShell("sh javadebug-agent-launch.sh " + pid, classPath);
        }

        if (exitCode != 0) {
            System.err.println("could not attach to target jvm finally...");
        }

        System.err.println("success to attach target jvm:" + pid);

        return JAVA_DEBUG_INITIALIZER;
    }

    /**
     *  1. check the java-debug-tool repository
     *  2. run the ./javadebug-pack script to get the bin dir
     *  3. get the current jvm pid
     *  4. do attach job
     *
     * @return the instance
     */
    private static JavaDebugInitializer fallbackInstall() {

        // async to do the attach work
        new Thread(new InitAgentRunner(), "JavaDebugAgentAttachThread").start();

        return JAVA_DEBUG_INITIALIZER;
    }

    /**
     *  get the current jvm pid
     *
     * @return pid
     */
    private static String getProcessId() {
        final String runningVm = ManagementFactory.getRuntimeMXBean().getName();
        return runningVm.substring(0, runningVm.indexOf('@'));
    }

    /**
     *  run the shell script
     *
     * @param command the command to execute
     * @param workPath the target work path
     * @return the script shell exit code
     */
    private static int execShell(String command, String workPath) {
        Process process;
        try {
            process = Runtime.getRuntime().exec(command, new String[]{}, new File(workPath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        /// read result from process
        int exitCode = -1;

        // the output
        List<String> result = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
            /// wait for the process to terminal
            process.waitFor();
            exitCode = process.exitValue();
            return exitCode;
        } catch (Exception e) {
            return exitCode;
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // ignore.
            }
            // print the output
            for (String out : result) {
                System.out.println(out);
            }
        }
    }

    /**
     *  1. stop the tcpServer
     *
     */
    @SuppressWarnings("unchecked")
    public static void destroy() {
        try {
            // get the agent class
            ClassLoader agentClassLoader = null;
            Field allClassField = null;
            try {
                allClassField = ClassLoader.class.getDeclaredField("classes");
                allClassField.setAccessible(true);

                // get the all class by the system classloader loaded.
                Vector<Class<?>> classes = (Vector<Class<?>>) allClassField.get(ClassLoader.getSystemClassLoader());

                if (classes == null || classes.isEmpty()) {
                    throw new IllegalStateException("oh, what the.");
                }

                // get the Agent Class
                for (Class<?> cls : classes) {
                    if (cls.getName().endsWith(AGENT_WEAVE_SPY_CLASS_NAME)) {
                        agentClassLoader = (ClassLoader) cls.getField("AGENT_CLASS_LOADER").get(null);
                        break;
                    }
                }

                // check
                if (agentClassLoader == null) {
                    throw new IllegalStateException("could not get the agent classloader.");
                }

            } finally {
                if (allClassField != null) {
                    allClassField.setAccessible(false); // rollback
                }
            }

            System.err.println("get the agent classloader:" + agentClassLoader);

            // load the netty server class
            Class<?> serverCls = agentClassLoader.loadClass(AGENT_TCP_SERVER_CLASS_NAME);
            Object remoteNettyServerObject = serverCls.getMethod("getNettyTransportServer").invoke(null /*静态方法*/);
            if (remoteNettyServerObject == null) {
                System.err.println("无法获取到远程remoteNettyServerObject");
                return;
            }

            System.err.println("get the netty server class:" + serverCls
                                       + " with target object:" + remoteNettyServerObject + "\nstart to stop the server...\n\n");

            // stop the server
            serverCls.getMethod("stop").invoke(remoteNettyServerObject);

            System.err.println("success to stop the netty server.\n\n");
        } catch (Throwable e) {
            System.err.println("error occ:" + e);
        }
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }


    static class InitAgentRunner implements Runnable {

        /**
         * When an object implementing interface <code>Runnable</code> is used
         * to create a thread, starting the thread causes the object's
         * <code>run</code> method to be called in that separately executing
         * thread.
         * <p>
         * The general contract of the method <code>run</code> is that it may
         * take any action whatsoever.
         *
         * @see Thread#run()
         */
        @Override
        public void run() {
            // current thread init.

            String currentDir = System.getProperty("user.dir");

            System.err.println("get the current dir:" + currentDir);

            System.err.println("get the java-debug-tool work path:" + currentDir);

            String javaDebugPath = "java-debug-tool";
            if (currentDir.endsWith("/")) {
                javaDebugPath = currentDir + javaDebugPath;
            } else {
                javaDebugPath = currentDir + "/" + javaDebugPath;
            }

            // if exists
            File targetWorkFile = new File(javaDebugPath);
            int exitCode;
            if (targetWorkFile.exists()) {
                // checkout to develop branch
                execShell("git co develop ", javaDebugPath);

                // fetch new change
                execShell("git fetch origin develop", javaDebugPath);

                // merge HEAD
                execShell("git merge FETCH_HEAD", javaDebugPath);

            } else {
                System.err.println("start to git clone java-debug-tool repository to dir:" + currentDir);

                exitCode = execShell("git clone -b develop " + REMOTE_GIT_REPOSITORY, currentDir);

                if (exitCode != 0) {
                    throw new IllegalStateException("could not git clone java-debug-repository");
                }
            }

            // do the pack
            String scriptPath = javaDebugPath + "/script";
            if (!(new File(scriptPath)).exists()) {
                throw new IllegalStateException("the target 'java-debug-tool/script/' dir not exists");
            }

            exitCode = execShell("./javadebug-pack.sh", scriptPath);

            if (exitCode != 0) {
                throw new IllegalStateException("could not pack the java-debug-tool.");
            }

            // do attach jvm

            String binDir = javaDebugPath + "/bin";

            // check the bin dir
            if (!new File(binDir).exists()) {
                throw new IllegalStateException("the target bin dir not exists");
            }

            // get the current jvm pid
            String pid = getProcessId();

            // check pid
            if (pid.isEmpty()) {
                throw new IllegalStateException("could not get the target jvm pid");
            }

            // attach the agent to target jvm
            System.err.println("get the current jvm pid:" + pid);

            exitCode = execShell("./javadebug-agent-attach.sh " + pid, binDir);
            if (exitCode != 0) {
                throw new IllegalStateException("could not attach agent to target jvm:" + pid);
            }
        }
    }
}
