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
import io.javadebug.core.utils.UTILS;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * Created on 2019/4/20 13:03.
 *
 * @author <a href="H.J"> HuJian </a>
 */
@ChannelHandler.Sharable
public class ServerIdleHandler  extends ChannelDuplexHandler {

    /**
     *
     * @see io.netty.channel.ChannelInboundHandlerAdapter#userEventTriggered(io.netty.channel.ChannelHandlerContext, java.lang.Object)
     */
    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            try {
                PSLogger.error(UTILS.format("connection in idle state, close it：" + ctx.channel().remoteAddress()));
                ctx.close();
            } catch (Exception e) {
                PSLogger.error(UTILS.format("error when close idle connection:%s", UTILS.getErrorMsg(e)));
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
