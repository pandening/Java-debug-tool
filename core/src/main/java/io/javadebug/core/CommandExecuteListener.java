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


package io.javadebug.core;


import io.javadebug.core.transport.Connection;
import io.javadebug.core.transport.RemoteCommand;

/**
 * Created on 2019/4/28 14:08.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public interface CommandExecuteListener {

    /**
     *  当命令执行成功了，并且响应成功了，这个方法就会被回调
     *
     * @param remoteCommand 命令响应协议 {@link RemoteCommand}
     */
    void onResponse(RemoteCommand remoteCommand);

    /**
     *  当新的连接增加进来，这个方法就会被回调
     *
     * @param connection {@link Connection}
     */
    void onAddConnection(Connection connection);

    /**
     *  当一个连接被删除的时候，无论是被自动剔除，还是主动删除，这个方法
     *  都会被回调
     *
     * @param connection {@link Connection}
     */
    void onRemoveConnection(Connection connection);

    /**
     *  当选择了灰度模式后，被选中的连接会通知，这个方法会被回调
     *
     * @param connection {@link Connection}
     */
    void onGreyConnection(Connection connection);

    /**
     *  当灰度模式被关闭后，这个方法会被回调
     *
     */
    void onGreyEnd();

    /**
     *  客户端退出通知一下
     *
     */
    void onExit();

    /**
     *  将客户端运行时异常都通知出来，仅做通知
     *
     * @param e 异常信息
     */
    void onException(Throwable e);

    /**
     *  当命令执行失败的时候就会回调这个方法
     *
     * @param cmdLine 命令行输入
     * @param stage 命令执行阶段
     * @param origin 原始命令输入
     * @param errorMsg 错误信息
     */
    void onError(String cmdLine, RemoteCommand origin, String errorMsg, CommandExecuteStage stage);

    // 命令执行阶段
    enum CommandExecuteStage {
        ON_CONNECT_TO_REMOTE_JVM,
        ON_CLIENT_COMMAND,
        COMMAND_INPUT_HANDLE,
        SEND_COMMAND
    }

}
