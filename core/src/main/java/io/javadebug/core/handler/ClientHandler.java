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

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.transport.NettyTransportClient;
import io.javadebug.core.transport.RemoteCommand;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Created on 2019/4/20 16:36.
 *
 * @author <a href="H.J"> HuJian </a>
 */
@ChannelHandler.Sharable
public class ClientHandler extends BaseWrapClientHandler {

    public ClientHandler(NettyTransportClient nettyTransportClient) {
        super(nettyTransportClient);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        PSLogger.error("出现严重错误:" + cause + "\n");
        nettyTransportClientRef.receiveResp(null);
    }

    /**
     * <strong>Please keep in mind that this method will be renamed to
     * {@code messageReceived(ChannelHandlerContext, I)} in 5.0.</strong>
     * <p>
     * Is called for each message of type {@code I}.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
     *            belongs to
     * @param msg the message to handle
     * @throws Exception is thrown if an error occurred
     */
    @Override
    protected void handleChannelReadEvent(ChannelHandlerContext ctx, RemoteCommand msg) throws Exception {

        /// 简单处理，如果想要复杂处理可以想想办法，但是理论上NettyHandler仅做简单的事情

        nettyTransportClientRef.receiveResp(msg);
    }

}
