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


package io.javadebug.core.console;

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.transport.RemoteCommand;
import io.javadebug.core.ui.SimplePSUI;
import io.javadebug.core.ui.UI;

import java.io.OutputStream;

/**
 * Created on 2019/4/29 22:43.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class ConsoleCommandSink implements CommandSink {

    private static OutputStream os = System.out;
    private static final UI ui = new SimplePSUI();

    /**
     * 命令执行结果出来，比如可以直接打印出来，或者写到某个队列中去
     *
     * @param remoteCommand {@link RemoteCommand} 命令执行结果
     * @return 是否处理成功
     */
    @Override
    public boolean sink(RemoteCommand remoteCommand) {
        try {
            print(ui.toUI(remoteCommand), os);
        } catch (Exception e) {
            PSLogger.error("无法处理响应结果:" + remoteCommand + " ：" + e);
        }
        return true;
    }

    private static void print(String msg, OutputStream os) throws Exception {
        os.write(("\n" + msg + "\n").getBytes());
    }
}
