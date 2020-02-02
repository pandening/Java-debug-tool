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

import io.javadebug.core.handler.ClientCommandRequestHandler;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.transport.NettyTransportClient;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.IOException;

/**
 * Created on 2019/4/20 16:51.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class JavaDebugClientLauncher {

    public static void main(String[] args) throws IOException {

        String ip = Constant.DEFAULT_SERVER_IP;
        int port = Constant.DEFAULT_SERVER_PORT;

        final OptionParser parser = new OptionParser();
        parser.accepts("ip").withOptionalArg().ofType(String.class);
        parser.accepts("port").withOptionalArg().ofType(Integer.class);

        final OptionSet optionSet = parser.parse(args);
        if (optionSet.has("ip")) {
            ip = (String) optionSet.valueOf("ip");
        }

        if (optionSet.has("port")) {
            port = (int) optionSet.valueOf("port");
        }

        // init client logger
        PSLogger.initClientLogger();

        PSLogger.error("get the ip and port, start to connect remote server:" + ip + ":" + port);

        CommandRequestHandler commandRequestHandler = new ClientCommandRequestHandler();

        NettyTransportClient nettyTransportClient = new NettyTransportClient(ip, port, commandRequestHandler);

        try {
            nettyTransportClient.init();
        } catch (Exception e) {
            e.printStackTrace();
            nettyTransportClient.shutdown();
            System.exit(1);
        }
    }

}
