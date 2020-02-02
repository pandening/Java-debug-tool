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


import io.javadebug.core.handler.ClientCommandRequestHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2019/4/24 18:10.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class CommandCodecTest {

    public static void main(String[] args) throws Exception {

        RemoteCommand remoteCommand  = new RemoteCommand();

        ClientCommandRequestHandler clientCommandRequestHandler = new ClientCommandRequestHandler();

        remoteCommand = clientCommandRequestHandler.toCommand("rdf -p Runner:/Users/hujian06/github/java-debug/target/classes/Runner.class", remoteCommand);

        ByteBuf byteBuf = Unpooled.buffer();

        CommandCodec.encode(remoteCommand, byteBuf);

        List remoteCommands = new ArrayList<>();
        CommandCodec.decode(byteBuf, remoteCommands);
        System.out.println(remoteCommands);


        CommandCodec.encode(remoteCommand, byteBuf);
        CommandCodec.decode(byteBuf, remoteCommands);
        System.out.println(remoteCommands);

    }

}
