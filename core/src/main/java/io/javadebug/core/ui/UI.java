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


package io.javadebug.core.ui;

import io.javadebug.core.transport.RemoteCommand;

/**
 * Created on 2019/4/21 00:00.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public interface UI {

    /**
     *  每一个命令返回结果都是一种通用模型，所以可以做一个通用UI，如果需要实现
     *  个性化展示样式，可以实现该方法
     * 
     * @param remoteCommand {@link RemoteCommand}
     * @return 展示字符串
     */
    String toUI(RemoteCommand remoteCommand);
    
}
