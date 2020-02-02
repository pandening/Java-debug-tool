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

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.transport.RemoteCommand;
import io.javadebug.core.transport.TransportSubject;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Created on 2019/4/28 19:39.
 *
 * @author <a href="H.J"> HuJian </a>
 */
@ChannelHandler.Sharable
public class MultiRemoteClientHandler extends SimpleChannelInboundHandler<RemoteCommand> {

    private TransportSubject transportSubject;

    public MultiRemoteClientHandler(TransportSubject subject) {
        this.transportSubject = subject;
    }

    /**
     * Calls {@link ChannelHandlerContext#fireExceptionCaught(Throwable)} to forward
     * to the next {@link ChannelHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        PSLogger.error(String.format("receive error response from:[%s] with cause:[%s]", ctx.channel().remoteAddress(), cause));
        transportSubject.onError(ctx.channel(), cause);
    }

    /**
     * <strong>Please keep in mind that this method will be renamed to
     * {@code messageReceived(ChannelHandlerContext, I)} in 5.0.</strong>
     * <p>
     * Is called for each message of type {@code msg}.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
     *            belongs to
     * @param msg the message to handle
     * @throws Exception is thrown if an error occurred
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemoteCommand msg) throws Exception {
        if (msg == null) {
            return;
        }
        //PSLogger.info(String.format("receive message from:[%s] with msg:[%s]", ctx.channel().remoteAddress(), msg));

        transportSubject.onResponse(ctx.channel(), msg);
    }
}
