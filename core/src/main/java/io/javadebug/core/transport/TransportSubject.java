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


package io.javadebug.core.transport;

import io.netty.channel.Channel;

/**
 * Created on 2019/4/28 17:07.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public interface TransportSubject {

    /**
     *  有些命令发送需要进行灰度，否则容易造成事故，因此，在进行命令发送之前，请调用该方法进行
     *  灰度发布控制
     *
     */
    void greyController(int cnt);

    /**
     *  释放灰度发布开关
     *
     */
    void releaseGreyController(int cnt);

    /**
     *  开启新的一轮请求，每次开始发送命令之前都应该调用这个方法，否则
     *  会造成不同批次的请求互相干扰的情况
     *
     * @param connSize  链接数量
     *
     */
    void anotherRound(int connSize);

    /**
     *  某些情况下，连接已经变得不可用，这个时候如果不改变cd值，那么客户端会一直在
     *  等待，为了解决这个问题，需要动态控制cd值
     *
     * @param size 失效的客户端数量
     */
    void countdownConnect(int size);

    /**
     *  当服务端有响应的时候，这个方法就会被回调，客户端可以从这里拿到响应内容
     *
     * @param channel {@link Channel}
     * @param remoteCommand {@link RemoteCommand}
     */
    void onResponse(Channel channel, RemoteCommand remoteCommand);

    /**
     *  当客户端发生错误的时候，这个方法会被回调，并将出现错误的原因传递进来
     *
     * @param channel {@link Channel}
     * @param e 具体的错误原因，优先从cause中拿错误原因
     */
    void onError(Channel channel, Throwable e);

    /**
     *  判断一下当前Subject是否已经完成了，无论是通过什么方式完成的，都算是完成，比如
     *  {@code onResponse} 或者 {@code onError}
     *
     * @return true 代表完成
     */
    boolean isDone();

    /**
     *  无论处理结果是正确的还是错误的，都可以从这里拿到
     *
     *  注意，这个方法应该是阻塞的，需要将调用方阻塞起来，直到可以返回结果给调用方
     *
     * @return 结果，是多个服务端处理结果的综合，不是某一个连接的处理结果
     */
    RemoteCommand resp();

}
