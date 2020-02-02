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

import io.javadebug.core.thirdparty.exception.ThirtPartyAbilityNotFindException;
import io.javadebug.core.thirdparty.exception.ThirtyPartyExecuteException;
import io.javadebug.core.transport.RemoteCommand;

/**
 *  第三方类库的能力抽象定义，区分不同类库的性质来决定怎么实现能力抽象，参考
 *  {@link ThirdPartyAttribute}
 *
 * @author pandening
 *
 * @since 2.0
 */
public interface ThirdPartyAbility {

    /**
     *  第三方类库执行命令，获取响应的入口
     *
     * @param remoteCommand Java-debug-tool协议结构体
     * @return 命令执行结果
     */
    String exec(RemoteCommand remoteCommand) throws ThirtPartyAbilityNotFindException, ThirtyPartyExecuteException;

    /**
     *  某些情况下，执行某个第三方命令需要安装一个安全保障组件，这样不至于在用户输入错误或者不符合命令
     *  规范的情况下对系统造成影响
     *
     * @param remoteCommand 命令协议
     * @return {@link ThirdPartySafetyDetective}
     */
    ThirdPartySafetyDetective detective(RemoteCommand remoteCommand);

}
