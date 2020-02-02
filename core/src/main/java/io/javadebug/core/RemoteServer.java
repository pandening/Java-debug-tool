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

/**
 * Created on 2019/4/17 16:51.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public interface RemoteServer {

    /**
     *  判断服务端是否已经就绪了
     *
     * @return true代表服务端已经就绪，可以accept
     */
    boolean isBind();

    /**
     *  执行服务端bind等逻辑，不同类型的服务端初始化过程可能不一样，但是执行完该方法之后，服务端
     *  必须有能力处理客户端连接；
     *
     * @param configure {@link Configure}
     */
    void start(Configure configure) throws Exception;

    /**
     *  客户端要求服务端关闭，这一般出现在最后一个debug的人发出了关闭服务端的命令，只要服务端还在
     *  为至少一个客户端服务，那么就不应该关闭服务端，所以服务端需要记录每一个连接，并有能力在需要
     *  的时候将服务端关闭；
     *
     * @param configure {@link Configure}
     */
    void stop(Configure configure) throws Exception;

    /**
     *  关闭服务端
     *
     * @throws Exception 处理异常
     */
    void stop() throws Exception;

}
