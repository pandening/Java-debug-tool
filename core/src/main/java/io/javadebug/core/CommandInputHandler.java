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

import io.javadebug.core.transport.RemoteCommand;

/**
 * Created on 2019/4/22 23:13.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public interface CommandInputHandler {

    /**
     *  这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
     *  是命令在客户端输入之后的处理
     *
     * @param args 命令参数
     * @param origin 客户端持有的协议对象
     * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
     * @throws Exception 处理异常
     */
    RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception;

}
