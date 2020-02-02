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


package io.javadebug.core.thirdparty;

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.thirdparty.exception.ThirtyPartyExecuteException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;

/**
 *  用于执行shell，比如第三方命令等等
 *
 *
 *  NOTICE: 这个类过于危险，建议不要对外暴露，否则后果不堪设想
 *
 */
public class ShellExecutor {

    private static final File CURRENT_PATH = new File("");
    static {
        PSLogger.error("获取到当前工作路径:" + CURRENT_PATH.getAbsolutePath());
    }

    /**
     *  获取到当前工作目录
     *
     * @return 工作目录
     * @throws ThirtyPartyExecuteException 无法找到
     */
    public static String getCurrentWorkPath() throws ThirtyPartyExecuteException {
        String curDir = CURRENT_PATH.getAbsolutePath();
        if (UTILS.isNullOrEmpty(curDir)) {
            throw new ThirtyPartyExecuteException("could not get the current work path.");
        }
        return curDir;
    }

    /**
     *  get the current jvm pid
     *
     * @return pid
     */
    public static String getProcessId() {
        final String runningVm = ManagementFactory.getRuntimeMXBean().getName();
        return runningVm.substring(0, runningVm.indexOf('@'));
    }

    /**
     *  在当前目录下执行shell
     *
     * @param command 命令 + 参数
     * @return exitCode
     * @throws ThirtyPartyExecuteException 命令执行出现错误，exitCode != 0
     */
    public static int execShell(String command, StringBuilder result) throws ThirtyPartyExecuteException {
        String curDir = CURRENT_PATH.getAbsolutePath();
        if (UTILS.isNullOrEmpty(curDir)) {
            throw new ThirtyPartyExecuteException("could not get the current work path.");
        }
        return execShell(command, curDir, result);
    }

    /**
     *  run the shell script
     *
     * @param command the command to execute
     * @param workPath the target work path
     * @return the script shell exit code
     */
    public static int execShell(String command, String workPath, StringBuilder result) throws ThirtyPartyExecuteException {
        Process process;
        try {
            process = Runtime.getRuntime().exec(command, new String[]{}, new File(workPath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        /// read result from process
        int exitCode;

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            /// wait for the process to terminal
            process.waitFor();
            exitCode = process.exitValue();
            return exitCode;
        } catch (Exception e) {
            throw new ThirtyPartyExecuteException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // ignore.
            }
            // print the output
            System.out.println(result.toString());
        }
    }

    public static void main(String[] args) throws ThirtyPartyExecuteException {
        StringBuilder sb = new StringBuilder();
        execShell("sh bin/hello.sh", sb);
        System.out.println(sb);
    }

}
