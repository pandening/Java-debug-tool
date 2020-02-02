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


package io.javadebug.core.annotation;

import io.javadebug.core.command.Command;

/**
 * Created on 2019/4/21 13:57.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class CommandDescribeUtil {

    /**
     *  用于将Command类上的注解拿到，并做成格式化可输出的形式
     *
     * @param command {@link Command} 命令
     * @return 输出
     */
    @SuppressWarnings("unchecked")
    public static String collectFromCommand(Command command) {
        Class<Command> cls = (Class<Command>) command.getClass();
        return collectFromCommand(cls);
    }

    public static String collectFromCommand(Class<Command> commandClass) {
        CommandDescribe commandDescribe = commandClass.getAnnotation(CommandDescribe.class);
        if (commandDescribe == null) {
            return "Command Not Find !";
        }
        StringBuilder sb = new StringBuilder();

        // name
        sb.append("Command    \t: ").append(commandDescribe.name());
        sb.append(" | ").append(commandDescribe.simpleName()).append("\n");

        // function
        sb.append("Function   \t: ").append(commandDescribe.function()).append("\n");

        // usage
        sb.append("Usage      \t: ").append(commandDescribe.usage()).append("\n");

        // type
        sb.append("Type       \t: ").append(commandDescribe.cmdType().name()).append("\n");

        return sb.toString();
    }

}
