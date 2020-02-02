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


package io.javadebug.core.handler;

import io.javadebug.core.CommandRequestHandler;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.transport.RemoteCommand;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static io.javadebug.core.transport.CommandProtocol.COMMAND_REQ_PROTOCOL_TYPE;
import static io.javadebug.core.transport.CommandProtocol.CURRENT_SERVER_VERSION;

/**
 * Created on 2019/4/20 16:40.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public abstract class BaseCommandRequestHandler implements CommandRequestHandler {

    private static BlockingQueue<RemoteCommand> MQ = new LinkedBlockingQueue<>();

    /**
     * 这个方法用来产生远程请求
     *
     * @return {@link RemoteCommand}
     */
    @Override
    public RemoteCommand get() {
        try {
            return MQ.take();
        } catch (InterruptedException e) {
            return null;
        }
    }


    /**
     * 用于监听命令请求创建的消息
     *
     * @param cmd 原始命令
     */
    @Override
    public RemoteCommand onCreateRequest(String cmd, RemoteCommand remoteCommand) {
        if (UTILS.isNullOrEmpty(cmd)) {
            return null;
        }
        remoteCommand = toCommand(cmd, remoteCommand);
        if (remoteCommand == null) {
            return null;
        }

        /// common filed
        remoteCommand.setProtocolType(COMMAND_REQ_PROTOCOL_TYPE);
        remoteCommand.setTimeStamp(System.currentTimeMillis());
        remoteCommand.setVersion(CURRENT_SERVER_VERSION);

        try {
            MQ.put(remoteCommand);
        } catch (InterruptedException e) {
            // ignore
            return null;
        }
        return remoteCommand;
    }

    /**
     *  这个方法用于实现将输入变成命令
     *
     * @param cmd 这是原始命令
     * @return {@link RemoteCommand} 转换成一个命令
     */
    public abstract RemoteCommand toCommand(String cmd, RemoteCommand remoteCommand);

}
