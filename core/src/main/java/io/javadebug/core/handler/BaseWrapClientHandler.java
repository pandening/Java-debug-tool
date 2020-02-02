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


package io.javadebug.core.handler;

import io.javadebug.core.transport.NettyTransportClient;
import io.javadebug.core.transport.RemoteCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Created on 2019/4/20 23:27.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public abstract class BaseWrapClientHandler extends SimpleChannelInboundHandler<RemoteCommand> {

    /// for context id
    protected NettyTransportClient nettyTransportClientRef;

    public BaseWrapClientHandler(NettyTransportClient nettyTransportClient) {
        this.nettyTransportClientRef = nettyTransportClient;
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
    protected void channelRead0(ChannelHandlerContext ctx, RemoteCommand msg) throws Exception {
        if (msg != null) {
            nettyTransportClientRef.setRemoteCommand(msg);
        }

        /// call the biz handler

        handleChannelReadEvent(ctx, msg);
        ctx.fireChannelRead(msg);
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
    protected abstract void handleChannelReadEvent(ChannelHandlerContext ctx, RemoteCommand msg) throws Exception;

}
