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


package io.javadebug.core;

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.transport.NettyTransportClusterClient;
import io.javadebug.core.utils.UTILS;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Created on 2019/4/29 00:33.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class JavaDebugClusterClientLauncher {

    public static void main(String[] args) {

        final OptionParser parser = new OptionParser();
        parser.accepts("p").withOptionalArg().ofType(String.class);

        String p = "";
        final OptionSet optionSet = parser.parse(args);
        if (optionSet.has("p")) {
            p = (String) optionSet.valueOf("p");
        }

        if (UTILS.isNullOrEmpty(p)) {
            throw new IllegalArgumentException("不合法的参数设置");
        }

        /// 启动客户端
        NettyTransportClusterClient nettyTransportClusterClient = new NettyTransportClusterClient(p);

        try {
            nettyTransportClusterClient.start();
        } catch (Exception e) {
            PSLogger.error("无法启动客户端:" + e);
            nettyTransportClusterClient.exitClient();
        }

    }

}
