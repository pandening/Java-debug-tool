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


package io.javadebug.core;

import io.javadebug.core.utils.UTILS;

import static io.javadebug.core.http.ShellChannelHttpServer.SHELL_CHANNEL_HTTP_SERVER;

public class JavaDebugHttpServerLauncher {

    public static void main(String[] args) throws InterruptedException {

        if (args == null || args.length != 3) {
            throw new IllegalArgumentException("invalid params, could not set up the http server ...");
        }

        String ip = args[0];
        String port = args[1];
        String listenPort = args[2];

        int portNumber = UTILS.safeParseInt(port, -1);
        if (portNumber <= 0) {
            throw new IllegalArgumentException("invalid port number :" + port);
        }

        int listenOn = UTILS.safeParseInt(listenPort, -1);
        if (listenOn <= 0) {
            throw new IllegalArgumentException("invalid http listen port : " + listenPort);
        }

        // set up the http server on 10234
        SHELL_CHANNEL_HTTP_SERVER.setup(ip, portNumber, listenOn);

    }

}
