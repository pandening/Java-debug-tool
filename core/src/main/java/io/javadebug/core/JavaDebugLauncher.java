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


package io.javadebug.core;

//import com.sun.tools.attach.VirtualMachine;
//import com.sun.tools.attach.VirtualMachineDescriptor;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.util.List;

import static io.javadebug.core.log.PSLogger.error;
import static io.javadebug.core.utils.UTILS.getErrorMsg;

/**
 * Created on 2019/4/17 17:17.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class JavaDebugLauncher {

    private Configure configure;

    /**
     *  参数解析工作在这个方法里面进行
     *
     *  -agent  : 代理jar包路径
     *  -core   : 核心处理代码jar包路径
     *  -pid    : 目标JVM
     *  -ip     ：目标JVM所在的机器
     *  -port   ：目标JVM启动监听端口
     *
     * @param args 传递参数
     */
    private JavaDebugLauncher(String[] args) throws Exception {

        /// 参数解析

        configure = parseConfig(args);

        /// 挂载JVM
        attachAgentToTargetJvm();

    }

    /**
     *  负责参数解析
     * 
     * @param args {@link JavaDebugLauncher#main(String[])}
     * @return {@link Configure}
     */
    private Configure parseConfig(String[] args) {
        final OptionParser parser = new OptionParser();
        parser.accepts("core").withRequiredArg().ofType(String.class).required();
        parser.accepts("agent").withRequiredArg().ofType(String.class).required();
        parser.accepts("pid").withRequiredArg().ofType(String.class).required();
        parser.accepts("ip").withOptionalArg().ofType(String.class);
        parser.accepts("port").withOptionalArg().ofType(int.class);

        final OptionSet optionSet = parser.parse(args);
        final Configure configure = new Configure();
        
        configure.setCoreJar((String) optionSet.valueOf("core"));
        configure.setAgentJar((String) optionSet.valueOf("agent"));
        configure.setPid((String) optionSet.valueOf("pid"));
        configure.setCoreJar((String) optionSet.valueOf("core"));
        int port = Constant.DEFAULT_SERVER_PORT;
        if (optionSet.has("port")) {
            port = (int) optionSet.valueOf("port");
        }
        configure.setTargetPort(port);
        String ip = Constant.DEFAULT_SERVER_IP; // localhost
        if (optionSet.has("ip")) {
            ip = (String) optionSet.valueOf("ip");
        }
        configure.setTargetIp(ip);

        return configure;
    }

//    private void attachAgentToTargetJVM() throws Exception {
//        List<VirtualMachineDescriptor> virtualMachineDescriptors = VirtualMachine.list();
//        VirtualMachineDescriptor targetVM = null;
//        for (VirtualMachineDescriptor descriptor : virtualMachineDescriptors) {
//            if (descriptor.id().equals(configure.getPid())) {
//                targetVM = descriptor;
//                break;
//            }
//        }
//        if (targetVM == null) {
//            throw new IllegalArgumentException("could not find the target jvm by process id:" + configure.getPid());
//        }
//        VirtualMachine virtualMachine = null;
//        try {
//            virtualMachine = VirtualMachine.attach(targetVM);
//            virtualMachine.loadAgent("{agent}", "{params}");
//        } catch (Exception e) {
//            if (virtualMachine != null) {
//                virtualMachine.detach();
//            }
//        }
//    }

    /**
     *  该方法需要实现的功能包括：
     *  1）找到目标JVM，确认pid没有错误
     *  2）加载Agent包，触发Agent逻辑
     *
     * @throws Exception 处理异常
     */
    private void attachAgentToTargetJvm() throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> vmdClass = loader.loadClass("com.sun.tools.attach.VirtualMachineDescriptor");
        Class<?> vmClass = loader.loadClass("com.sun.tools.attach.VirtualMachine");

        Object attachVmdObj = null;
        for (Object obj : (List<?>) vmClass.getMethod("list", (Class<?>[]) null).invoke(null, (Object[]) null)) {
            if ((vmdClass.getMethod("id", (Class<?>[]) null).invoke(obj, (Object[]) null)).equals(configure.getPid())) {
                attachVmdObj = obj;
            }
        }

        Object vmObj = null;
        try {
            if (null == attachVmdObj) {
                vmObj = vmClass.getMethod("attach", String.class).invoke(null, "" + configure.getPid());
            } else {
                vmObj = vmClass.getMethod("attach", vmdClass).invoke(null, attachVmdObj);
            }
            vmClass.getMethod("loadAgent", String.class, String.class)
                    .invoke(vmObj, configure.getAgentJar(), configure.getCoreJar() + ";" + configure.toString());
        } finally {
            if (null != vmObj) {
                vmClass.getMethod("detach", (Class<?>[]) null).invoke(vmObj, (Object[]) null);
            }
        }
    }

    /**
     *  用来实现挂载启动流程，之后，Agent会Attach到目标JVM上，并在目标JVM上启动一个
     *  小型的TcpServer，之后就可以和这个目标JVM Server进行通信了
     *
     * @param args 输入参数
     */
    public static void main(String[] args) {
        try {
            new JavaDebugLauncher(args);
//            for (;;) {
//                TimeUnit.SECONDS.sleep(10);
//            }
        } catch (Exception e) {
            error(getErrorMsg(e));
        }
    }

}
