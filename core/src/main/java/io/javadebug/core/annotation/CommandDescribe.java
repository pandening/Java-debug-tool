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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created on 2019/4/21 12:59.
 *
 * @author <a href="H.J"> HuJian </a>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CommandDescribe {

    /**
     *  命令的全名，比如"info"，命令匹配可以用这个字段
     *
     * @return 命令名称
     */
    public String name();

    /**
     *  {@code name}返回的是命令的全称，simpleName则是简易模式，可以根据这个
     *  匹配命令
     *
     * @return 简易名称，可以有多个
     */
    public String simpleName();

    /**
     *  详细描述一下这个命令是用来干什么的，它的功能是什么，它需要说明样的参数，哪些
     *  参数是干嘛的，是否必须等等，这里面尽可能详细的描述出来
     *
     * @return 详细描述
     */
    public String function();

    /**
     *  和function不同的是，usage专注于解释命令的使用方法，需要罗列出使用该命令的各种
     *  情况，并尽可能多的描述每一个执行模式下的响应结果信息
     *
     * @return 使用方法
     */
    public String usage();

    /**
     *  用于标记这个命令的阻塞属性，如果命令能实时处理并尽可能快速的返回，那么就是false，否则
     *  比如目标JVM需要等待一段时间才能收集到命令的结果，那么该值就是true，理论上对客户端是无
     *  感知的，因为比如客户端 {@link io.javadebug.core.transport.NettyTransportClient}
     *  被实现成了
     *  C->S->C->S ...
     *  的模式，当然存在一个客户端超时时间，避免客户端出现"卡死"假象
     *
     * @return 默认实时返回
     */
    public boolean needBlock() default false;

    /**
     *  命令的等级，用于权限控制，-1代表不做控制，真正控制权限的值从0开始，数字越小则
     *  代表执行该命令所需要的权限也越小
     *
     * @return 命令执行权限要求
     */
    public int cmdLevel() default -1;

    /**
     *  这个命令的类型，可以让用户对命令有一个大概的认识
     *
     * @return {@link CommandType}
     */
    public CommandType cmdType();

    /**
     * 出于对目标JVM的保护，执行该命令的时间如果超过设定的时间，那么命令就会停止执行；
     * 单位是分钟，默认没有限制
     *
     * @return -1则代表没有限制
     */
    public long timeLimit() default -1;

}
