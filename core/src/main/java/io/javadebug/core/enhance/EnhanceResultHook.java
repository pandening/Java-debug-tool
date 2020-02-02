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


package io.javadebug.core.enhance;

import java.lang.instrument.ClassDefinition;

/**
 * Created on 2019/5/10 10:40.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public interface EnhanceResultHook {

    /**
     *  仅仅是获取到命令的结果，这里玩不出什么花样
     * 
     * @return 返回给client的响应结果 {@see $back-data}
     */
    String getEnhanceResult();

    /**
     *  获取到增强过的字节码 {@link java.lang.instrument.Instrumentation#redefineClasses(ClassDefinition...)}
     *
     * @return 增强过的字节码
     */
    byte[] getEnhanceBytes();
    
}
