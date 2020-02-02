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

/**
 * Created on 2019/4/21 13:13.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public enum CommandType {

    /// 命令的类型，分为下面几类

    OBSERVE,        // 观察型，这类命令将会对某种事物进行观察，并进行报告
    COMPUTE,        // 计算型，这类命令会在目标JVM上执行一些计算，并进行响应
    EXECUTE,        // 代码执行，这类命令称为"远程代码执行"，较为危险，暂不支持
    ENHANCE         // 增强型，这类命令的特点就是对目标JVM进行字节码在增强

    ;
}
