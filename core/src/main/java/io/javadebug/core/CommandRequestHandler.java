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

import io.javadebug.core.transport.RemoteCommand;

/**
 * Created on 2019/4/20 16:21.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public interface CommandRequestHandler {


    /**
     *  这个方法用来产生远程请求
     *
     * @return {@link RemoteCommand}
     */
    RemoteCommand get();

    /**
     *  用于监听命令请求创建的消息
     *
     * @param remoteCommand 这是一个客户端持有的命令对象，一个连接使用一个命令对象即可
     * @param cmd 原始输入命令
     * @return 如果返回false，则重新输入
     */
    RemoteCommand onCreateRequest(String cmd, RemoteCommand remoteCommand);

}
