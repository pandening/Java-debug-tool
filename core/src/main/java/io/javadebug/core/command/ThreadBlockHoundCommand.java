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


package io.javadebug.core.command;

import io.javadebug.core.CommandServer;
import io.javadebug.core.ServerHook;
import io.javadebug.core.enhance.MethodDesc;
import io.javadebug.core.enhance.ThreadBlockHoundTransformer;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.transport.RemoteCommand;
import io.javadebug.core.utils.UTILS;
import org.objectweb.asm.commons.Method;
import sun.misc.Unsafe;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.lang.instrument.Instrumentation;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ThreadBlockHoundCommand implements Command {

    private static String getMethodDesc(java.lang.reflect.Method origin) {
        return Method.getMethod(origin).getDescriptor();
    }

    private static final List<Class<?>> HOOK_CLASS_LIST = new ArrayList<>();
    private static final Map<String, Set<MethodDesc>> BLOCK_CHECK_HOOK_METHOD = new HashMap<String, Set<MethodDesc>>(){{

        // java.lang.Thread
        // sleep
        // yield
        put(Thread.class.getName(), new HashSet<MethodDesc>(){{
            //add(new MethodDesc(Thread.class.getName(), "sleep", "(J)V", Thread.class));
            try {
                add(new MethodDesc(Thread.class.getName(), Thread.class.getDeclaredMethod("sleep", long.class, int.class).getName(),
                        getMethodDesc(Thread.class.getDeclaredMethod("sleep", long.class, int.class)), Thread.class));
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            //add(new MethodDesc(Thread.class.getName(), "yield", "()V", Thread.class));
        }});
        HOOK_CLASS_LIST.add(Thread.class);

        // Forbid: System.out.println("");
        put(PrintStream.class.getName(), new HashSet<MethodDesc>(){{
            try {
                add(new MethodDesc(PrintStream.class.getName(), PrintStream.class.getDeclaredMethod("write", int.class).getName(),
                        getMethodDesc(PrintStream.class.getDeclaredMethod("write", int.class)), PrintStream.class));
                add(new MethodDesc(PrintStream.class.getName(), PrintStream.class.getDeclaredMethod("write", char[].class).getName(),
                        getMethodDesc(PrintStream.class.getDeclaredMethod("write", char[].class)), PrintStream.class));
                add(new MethodDesc(PrintStream.class.getName(), PrintStream.class.getDeclaredMethod("write", String.class).getName(),
                        getMethodDesc(PrintStream.class.getDeclaredMethod("write", String.class)), PrintStream.class));
                add(new MethodDesc(PrintStream.class.getName(), PrintStream.class.getDeclaredMethod("write", byte[].class, int.class, int.class).getName(),
                        getMethodDesc(PrintStream.class.getDeclaredMethod("write", byte[].class, int.class, int.class)), PrintStream.class));
                add(new MethodDesc(PrintStream.class.getName(), PrintStream.class.getDeclaredMethod("newLine").getName(),
                        getMethodDesc(PrintStream.class.getDeclaredMethod("newLine")), PrintStream.class));
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }});
        HOOK_CLASS_LIST.add(PrintStream.class);

//        // java.lang.Object
//        // wait
//        put(Object.class.getName(), new HashSet<MethodDesc>(){{
//            add(new MethodDesc(Object.class.getName(), "wait", "(J)V", Object.class));
//        }});
//        HOOK_CLASS_LIST.add(Object.class);

        // java.io.RandomAccessFile
        put(RandomAccessFile.class.getName(), new HashSet<MethodDesc>(){{
            add(new MethodDesc(RandomAccessFile.class.getName(), "read", "(J)V", RandomAccessFile.class));
            try {
                // open

                add(new MethodDesc(RandomAccessFile.class.getName(), RandomAccessFile.class.getDeclaredMethod("open", String.class, int.class).getName(),
                        getMethodDesc(RandomAccessFile.class.getDeclaredMethod("open", String.class, int.class)), RandomAccessFile.class));

                // read

                add(new MethodDesc(RandomAccessFile.class.getName(), RandomAccessFile.class.getDeclaredMethod("read").getName(),
                        getMethodDesc(RandomAccessFile.class.getDeclaredMethod("read")), RandomAccessFile.class));

                add(new MethodDesc(RandomAccessFile.class.getName(), RandomAccessFile.class.getDeclaredMethod("read", byte[].class).getName(),
                        getMethodDesc(RandomAccessFile.class.getDeclaredMethod("read", byte[].class)), RandomAccessFile.class));

                add(new MethodDesc(RandomAccessFile.class.getName(), RandomAccessFile.class.getDeclaredMethod("read", byte[].class, int.class, int.class).getName(),
                        getMethodDesc(RandomAccessFile.class.getDeclaredMethod("read", byte[].class, int.class, int.class)), RandomAccessFile.class));

                // write

                add(new MethodDesc(RandomAccessFile.class.getName(), RandomAccessFile.class.getDeclaredMethod("write", int.class).getName(),
                        getMethodDesc(RandomAccessFile.class.getDeclaredMethod("write", int.class)), RandomAccessFile.class));

                add(new MethodDesc(RandomAccessFile.class.getName(), RandomAccessFile.class.getDeclaredMethod("write", byte[].class).getName(),
                        getMethodDesc(RandomAccessFile.class.getDeclaredMethod("write", byte[].class)), RandomAccessFile.class));

                add(new MethodDesc(RandomAccessFile.class.getName(), RandomAccessFile.class.getDeclaredMethod("write", byte[].class, int.class, int.class).getName(),
                        getMethodDesc(RandomAccessFile.class.getDeclaredMethod("write", byte[].class, int.class, int.class)), RandomAccessFile.class));

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }});
        HOOK_CLASS_LIST.add(RandomAccessFile.class);

        // java.net.Socket
        put(Socket.class.getName(), new HashSet<MethodDesc>(){{
            try {
                // connect

                add(new MethodDesc(Socket.class.getName(), Socket.class.getDeclaredMethod("connect", SocketAddress.class).getName(),
                        getMethodDesc(Socket.class.getDeclaredMethod("connect", SocketAddress.class)), Socket.class));

                add(new MethodDesc(Socket.class.getName(), Socket.class.getDeclaredMethod("connect", SocketAddress.class, int.class).getName(),
                        getMethodDesc(Socket.class.getDeclaredMethod("connect", SocketAddress.class, int.class)), Socket.class));

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }});
        HOOK_CLASS_LIST.add(Socket.class);

        // java.net.DatagramSocket
        put(DatagramSocket.class.getName(), new HashSet<MethodDesc>(){{
            try {
                // connect

                add(new MethodDesc(DatagramSocket.class.getName(), DatagramSocket.class.getDeclaredMethod("connect", SocketAddress.class).getName(),
                        getMethodDesc(DatagramSocket.class.getDeclaredMethod("connect", SocketAddress.class)), DatagramSocket.class));

                add(new MethodDesc(DatagramSocket.class.getName(), DatagramSocket.class.getDeclaredMethod("connect", InetAddress.class, int.class).getName(),
                        getMethodDesc(DatagramSocket.class.getDeclaredMethod("connect", InetAddress.class, int.class)), DatagramSocket.class));

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }});
        HOOK_CLASS_LIST.add(DatagramSocket.class);

        // java.io.FileInputStream
        put(FileInputStream.class.getName(), new HashSet<MethodDesc>(){{
            try {
                // read

                add(new MethodDesc(FileInputStream.class.getName(), FileInputStream.class.getDeclaredMethod("read").getName(),
                        getMethodDesc(FileInputStream.class.getDeclaredMethod("read")), FileInputStream.class));

                add(new MethodDesc(FileInputStream.class.getName(), FileInputStream.class.getDeclaredMethod("read", byte[].class).getName(),
                        getMethodDesc(FileInputStream.class.getDeclaredMethod("read", byte[].class)), FileInputStream.class));

                add(new MethodDesc(FileInputStream.class.getName(), FileInputStream.class.getDeclaredMethod("read", byte[].class, int.class, int.class).getName(),
                        getMethodDesc(FileInputStream.class.getDeclaredMethod("read", byte[].class, int.class, int.class)), FileInputStream.class));

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }});
        HOOK_CLASS_LIST.add(FileInputStream.class);

        // java.io.FileOutputStream
        put(FileOutputStream.class.getName(), new HashSet<MethodDesc>(){{
            try {
                // write

                add(new MethodDesc(FileOutputStream.class.getName(), FileOutputStream.class.getDeclaredMethod("write", int.class).getName(),
                        getMethodDesc(FileOutputStream.class.getDeclaredMethod("write", int.class)), FileOutputStream.class));

                add(new MethodDesc(FileOutputStream.class.getName(), FileOutputStream.class.getDeclaredMethod("write", byte[].class).getName(),
                        getMethodDesc(FileOutputStream.class.getDeclaredMethod("write", byte[].class)), FileOutputStream.class));

                add(new MethodDesc(FileOutputStream.class.getName(), FileOutputStream.class.getDeclaredMethod("write", int.class, boolean.class).getName(),
                        getMethodDesc(FileOutputStream.class.getDeclaredMethod("write", int.class, boolean.class)), FileOutputStream.class));

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }});
        HOOK_CLASS_LIST.add(FileOutputStream.class);

//        // sun.misc.Unsafe
//        put(Unsafe.class.getName(), new HashSet<MethodDesc>(){{
//            try {
//                // park
//
//                add(new MethodDesc(Unsafe.class.getName(), Unsafe.class.getDeclaredMethod("park", boolean.class, long.class).getName(),
//                        getMethodDesc(Unsafe.class.getDeclaredMethod("park", boolean.class, long.class)), Unsafe.class));
//
//            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
//            }
//        }});
//        HOOK_CLASS_LIST.add(Unsafe.class);

        // java.util.concurrent.locks.LockSupport
        put(LockSupport.class.getName(), new HashSet<MethodDesc>(){{
            try {
                // park

                add(new MethodDesc(LockSupport.class.getName(), LockSupport.class.getDeclaredMethod("park").getName(),
                        getMethodDesc(LockSupport.class.getDeclaredMethod("park")), LockSupport.class));

                add(new MethodDesc(LockSupport.class.getName(), LockSupport.class.getDeclaredMethod("park", Object.class).getName(),
                        getMethodDesc(LockSupport.class.getDeclaredMethod("park", Object.class)), LockSupport.class));

                add(new MethodDesc(LockSupport.class.getName(), LockSupport.class.getDeclaredMethod("parkNanos", long.class).getName(),
                        getMethodDesc(LockSupport.class.getDeclaredMethod("parkNanos", long.class)), LockSupport.class));

                add(new MethodDesc(LockSupport.class.getName(), LockSupport.class.getDeclaredMethod("parkNanos", Object.class, long.class).getName(),
                        getMethodDesc(LockSupport.class.getDeclaredMethod("parkNanos", Object.class, long.class)), LockSupport.class));

                add(new MethodDesc(LockSupport.class.getName(), LockSupport.class.getDeclaredMethod("parkUntil", long.class).getName(),
                        getMethodDesc(LockSupport.class.getDeclaredMethod("parkUntil", long.class)), LockSupport.class));

                add(new MethodDesc(LockSupport.class.getName(), LockSupport.class.getDeclaredMethod("parkUntil", Object.class, long.class).getName(),
                        getMethodDesc(LockSupport.class.getDeclaredMethod("parkUntil", Object.class, long.class)), LockSupport.class));

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }});
        HOOK_CLASS_LIST.add(LockSupport.class);

        // java.util.CompletableFuture
        put(CompletableFuture.class.getName(), new HashSet<MethodDesc>(){{
            try {
                // get

                add(new MethodDesc(CompletableFuture.class.getName(), CompletableFuture.class.getDeclaredMethod("get").getName(),
                        getMethodDesc(CompletableFuture.class.getDeclaredMethod("get")), CompletableFuture.class));

                add(new MethodDesc(CompletableFuture.class.getName(), CompletableFuture.class.getDeclaredMethod("get", long.class, TimeUnit.class).getName(),
                        getMethodDesc(CompletableFuture.class.getDeclaredMethod("get", long.class, TimeUnit.class)), CompletableFuture.class));

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }});
        HOOK_CLASS_LIST.add(CompletableFuture.class);

    }};

    /**
     * 命令的前置检测，在执行真正的命令执行之前，会先执行这个方法，如果觉得当前的输入无法满
     * 命令执行的需求，则返回false，或者直接抛出异常即可
     *
     * @param remoteCommand 协议内容
     * @return true则表示继续执行命令，false则会停止命令的执行
     */
    @Override
    public boolean preExecute(RemoteCommand remoteCommand) throws Exception {
        return true;
    }

    /**
     * 一个命令需要实现该方法来执行具体的逻辑
     *
     * @param ins              增强器 {@link Instrumentation}
     * @param reqRemoteCommand 请求命令
     * @param commandServer    命令服务器
     * @param serverHook       {@link ServerHook} 服务钩子，用于便于从服务handler中获取数据
     * @return 响应命令
     */
    @Override
    public String execute(Instrumentation ins, RemoteCommand reqRemoteCommand, CommandServer commandServer, ServerHook serverHook) throws Exception {

        // the thread name pattern
        String threadNamePattern = reqRemoteCommand.getParam("$forward-tb-name");

        ThreadBlockHoundTransformer threadBlockHoundTransformer = new ThreadBlockHoundTransformer(BLOCK_CHECK_HOOK_METHOD, "", threadNamePattern);
        ins.addTransformer(threadBlockHoundTransformer, true);

        try {
            ins.retransformClasses(HOOK_CLASS_LIST.toArray(new Class[HOOK_CLASS_LIST.size()]));
        } catch (Throwable e) {
            PSLogger.error("error", e);
            return "error:" + UTILS.getErrorMsg(e);
        } finally {
            ins.removeTransformer(threadBlockHoundTransformer);
        }

        return "ok";
    }

    /**
     * 停止执行命令，任何一个命令的实现都需要感知到stop事件，这很重要，当命令中控器觉得命令
     * 执行的时间太久了，或者是检测到某种危险，或者觉得不再需要执行了，那么就会调用这个方法来
     * 打断命令的执行
     *
     * @param ins              {@link Instrumentation}
     * @param reqRemoteCommand {@link RemoteCommand}
     * @param commandServer    {@link CommandServer}
     * @param serverHook       {@link ServerHook}
     * @return 如果命令无法结束，请返回false，这样服务端虽然没办法，但是至少后续不会再同意这个
     * Client执行任何命令了，也就是在当前Context生命周期内，这个客户端被加入黑名单了
     */
    @Override
    public boolean stop(Instrumentation ins, RemoteCommand reqRemoteCommand, CommandServer commandServer, ServerHook serverHook) throws Exception {
        return true;
    }
}
