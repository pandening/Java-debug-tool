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

import io.javadebug.core.utils.UTILS;

/**
 * Created on 2019/5/10 10:40.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class MethodDesc {

    private String className;
    private String name;
    private String desc;

    private Class<?> targetClass;

    // the param match metadata
    private String customParamIn;
    private String springExpression;

    public MethodDesc(String className, String name, String desc, Class<?> cls) {
        this.className = className;
        this.name = name;
        this.desc = desc;
        this.targetClass = cls;
    }

    /**
     *  如果需要匹配自定义输入，那么需要把参数信息放在这
     *
     * @param customParamIn 参数信息
     */
    public void setCustomParamIn(String customParamIn) {
        this.customParamIn = customParamIn;
    }

    public String getCustomParamIn() {
        return customParamIn;
    }

    /**
     *  如果需要进行spring表达式匹配，需要把表达式放在这里
     *
     * @param springExpression spring表达式
     */
    public void setSpringExpression(String springExpression) {
        this.springExpression = springExpression;
    }

    public String getSpringExpression() {
        return springExpression;
    }

    /**
     *  用于和当前方法进行对比，是否可以匹配上，匹配默认是当前类的方法，请保证这一点
     *
     * @param name 方法名称
     * @param desc 方法描述 example: int a(int) => (I)I
     * @return true 代表可以匹配
     */
    public boolean isMatch(String name, String desc) {
        if (UTILS.isNullOrEmpty(name) || UTILS.isNullOrEmpty(desc)) {
            return false;
        }
        if (!name.equals(this.name)) {
            return false;
        }
        if (!desc.equals(this.desc)) {
            return false;
        }
        return true;
    }

    /**
     *  匹配方法
     *
     * @param cls 类是否匹配
     * @param name 方法名字是否匹配
     * @param desc 方法描述是否匹配
     * @return true代表匹配到了目标方法
     */
    public boolean isMatch(String cls, String name, String desc) {
        if (UTILS.isNullOrEmpty(cls) || UTILS.isNullOrEmpty(name) || UTILS.isNullOrEmpty(desc)) {
            return false;
        }
        if (!cls.equals(this.className)) {
            return false;
        }

        if (!name.equals(this.name)) {
            return false;
        }
        if (!desc.equals(this.desc)) {
            return false;
        }
        return true;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "MethodDesc{" +
                "className='" + className + '\'' +
                ", name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                '}';
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
    }
}
