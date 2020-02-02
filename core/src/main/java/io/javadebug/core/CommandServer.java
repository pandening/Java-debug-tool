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

import io.javadebug.core.command.Command;

/**
 * Created on 2019/4/22 23:01.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public interface CommandServer {

    /**
     *  根据命令名字获取到命令类型
     *
     * @param cmd 命令名字
     * @return 获取到的命令类型，如果没有发现，那么就是null
     */
    Class<Command> queryCommand(String cmd);

    /**
     *  新建一个命令
     *
     * @param cmd 命令名字
     * @return {@link Command}
     */
    Command newCommand(String cmd);

}
