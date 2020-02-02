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

/**
 * Created on 2019/5/11 13:40.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public interface LocalVariableTable {

    /**
     *  这个方法用于提供根据index查询局部变量名称的服务，本身就是方法级别的，所以
     *  不做区分
     *
     * @param index var index
     * @return 变量名称，可能不准确，不要强依赖
     */
    String valueAt(int index);

}
