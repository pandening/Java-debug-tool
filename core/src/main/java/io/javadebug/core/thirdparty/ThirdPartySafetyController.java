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

import io.javadebug.core.transport.RemoteCommand;
import io.netty.util.HashedWheelTimer;

/**
 *  因为第三方类库的特点，可能存在一些会让调试者遗留的工作，所以，需要有一个角色来为这种
 *  不愉快的操作进行收场，一个好的方式就是在进行一次第三方命令执行的时候，注册一个安全监测
 *  手段，这个安全操作手段有能力在用户出现操作失误、不完善的操作的情况下进行系统保护
 *
 * @author pandening
 *
 * @since 2.0
 */
public class ThirdPartySafetyController {

    /**
     *
     * 为命令注册一个检测器
     *
     * @param detective {@link ThirdPartySafetyDetective}
     * @param remoteCommand {@link RemoteCommand}
     */
    public static void registerSafetyDetective(ThirdPartySafetyDetective detective,
                                        RemoteCommand remoteCommand) {
        HashedWheelTimer hashedWheelTimer;
    }

}
