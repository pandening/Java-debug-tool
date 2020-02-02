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


package io.javadebug.core.console;

import io.javadebug.core.log.PSLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created on 2019/4/29 22:30.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class ConsoleCommandSource implements CommandSource {

    private static final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    /**
     * 用于生成一条命令输入，比如可以使用命令行，或者从某个队列取，只要能生成
     * 一条命令输入即可
     *
     * @return 命令输入，比如 "fc -class String"
     */
    @Override
    public String source() {
        try {
            return br.readLine();
        } catch (IOException e) {
            PSLogger.error("error while readLine from console：" + e);
            return source();
        }
    }
}
