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

import io.javadebug.core.data.LRModel;
import io.javadebug.core.enhance.MethodAdvice;
import io.javadebug.core.enhance.MethodTraceFrame;
import io.javadebug.core.transport.RemoteCommand;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Created on 2019/4/24 11:04.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public interface ServerHook {

    /**
     *  如果一个类的字节码被动态修改过，那么就会将原始字节码记录下来，主要
     *  是为了恢复
     *
     * @param className 类名称
     * @return 如果没有被增强过，那么就是null，这一点需要注意
     */
    byte[] getBackupClassByte(String className);

    /**
     *  某些命令会修改字节码，这个时候需要原类的原始字节码存储起来，该方法仅支持
     *  将类的原始字节码存储起来的功能，如果想要实现递进式类字节码存储，需要重新
     *  实现:参考 {@code backupClassByteWithLastVersion}
     *
     * @param className 类名字，全限定
     * @param bytes 字节码
     */
    void backupClassByte(String className, byte[] bytes);

    /**
     *  {@code backupClassByte}方法的另外一种版本，每次调用都会将该字节码存储起来作为
     *  回滚版本，所以，调用该方法之前，请确保你当前的字节码是稳定、正确的，因为该方法不会
     *  对类进行任何校验，如果字节码不合法，或者不符合规范，那么就会造成不可回滚类的严重后果
     *
     * @param className 类名称
     * @param bytes 类字节码数组
     */
    void backupClassByteWithLastVersion(String className, byte[] bytes);

    /**
     *  获取当前服务端记录的字节码锁定信息
     *
     * @return {@link io.javadebug.core.handler.CommandHandler#BYTE_LOCK_MAP}
     */
    ConcurrentMap<String, Integer> classLockInfoMap();

    /**
     *  获取持有类字节码锁的客户端信息
     *
     * @param className 类
     * @return contextId
     */
    int bytecodeOwner(String className);

    /**
     *  锁定字节码，不允许被别人变更
     *
     * @param className 类名
     * @param contextId 客户端id
     * @return 是否成功
     */
    boolean lockClassByte(String className, int contextId);

    /**
     *  解锁一个类，之后就允许其他客户端修改字节码了
     *
     * @param className 类
     * @param contextId id
     * @return 是否成功，较为严格的校验，如果该字节码并不是该客户端持有，也会认为解锁失败
     */
    boolean unlockClassByte(String className, int contextId);

    /**
     *  这个方法用来获取到当前客户端的 {@link MethodAdvice}，特别的是，如果发现该
     *  客户端还没有创建过给定key的advice的话，那么会创建一个；最后，无论是以前创建
     *  的还是本次创建的，都将这个advice返回
     *
     * @param contextId 客户端标志
     * @param cls 类
     * @param method 方法
     * @param desc 方法描述
     * @param mode mode
     * @param remoteCommand 一个参数一个参数的取，累死了，直接丢进去，想要什么就取什么吧，别删数据就行
     * @return {@link MethodAdvice}
     */
    MethodAdvice createNewMethodAdviceIfNeed(int contextId, String cls,
                                             String method, String desc, String mode,
                                             RemoteCommand remoteCommand);

    /**
     *  这个方法用来删除一个 {@link MethodAdvice}，这个方法一般在类被unlock的时候
     *  检测删除，当然如果被删除掉了，那么就没必要再删除了
     *
     * @param contextId 客户端标志
     * @param cls 类
     * @param method 方法
     * @param desc 方法描述
     * @return {@link MethodAdvice}
     */
    MethodAdvice removeMethodAdviceIfNeed(int contextId, String cls, String method, String desc);

    /**
     *  当某个愚蠢的人在使用了mt命令之后就退出了，或者执行了unlock命令，那么别人就无法再来增强
     *  这个类的任何方法了，所以有必要清理一下这些"垃圾" {@link MethodAdvice}
     *
     * @param contextId console id
     * @param cls 类
     * @return 所有匹配到的类
     */
    Set<MethodAdvice> removeAdvice(int contextId, String cls);

    /**
     *  根据客户端来删除资源
     *
     * @param contextId 客户端id
     * @return 被删除的advice
     */
    Set<MethodAdvice> removeAdvice(int contextId);

    /**
     *  这个方法的职责比较简单，看看客户端是否已经增强过某个具体的方法
     *
     * @param contextId 客户端标志
     * @param cls 类
     * @param method 方法
     * @param desc 方法描述
     * @return true代表已经被增强过了，不要再增强了
     */
    boolean isMethodClassWeaveDone(int contextId, String cls, String method, String desc);

    /**
     *  记录一个方法的流量，需要使用相应的Advice才能做到
     *  {@link io.javadebug.core.enhance.RecordCountLimitMethodTraceAdviceImpl}
     *
     * @param cls 目标类
     * @param method 目标方法
     * @param desc 方法描述
     * @param traces 一次方法调用的信息堆栈记录
     */
    void recordMethodFlow(String cls, String method, String desc, List<MethodTraceFrame> traces);

    /**
     *  需要回放流量来观察的时候，可以调用这个方法实现流量回放
     *
     * @param cls 目标类
     * @param method 目标方法
     * @param desc 方法描述
     * @param order 流量id，从0开始，值越小代表越早记录
     * @return 一次方法调用的历史堆栈，回放历史仅需要input即可
     */
    List<MethodTraceFrame> queryTraceByOrderId(String cls, String method, String desc, int order);

    /**
     *
     *  一个client将字节码增强过之后，标记一下，之后就会被其他客户端共享
     *
     * @param context 客户端id
     * @param cls 增强的类
     * @param method 增强的方法
     * @param dsc 方法描述
     */
    void setMethodTraceEnhanceStatus(int context, String cls, String method, String dsc);

    /**
     *  将所有该类的增强相关状态清空，用于在redefine一个类之后的后置动作，下次
     *  就需要重新增强了
     *
     * @param cls 需要清楚的类
     */
    void clearClassWeaveByteCode(String cls);

    /**
     *  如果一个类被增强/改变过，那么记录起来，调用这个方法可以获取到最新的类的字节码，如果类
     *  的字节码没有被变更过，那么调用这个方法直接返回null
     *
     * @param className 类名
     * @return 最新的字节码
     */
    byte[] lastBytesForClass(String className);

    /**
     *  因为目前rollback会将类回退到原始版本，所以需要将目前的类字节码删除
     *
     * @param className 类名
     */
    void clearBackupBytes(String className);

    /**
     *  如果执行了rollback了，那么就回退类
     *
     * @param className 类名
     */
    void removeTopBytes(String className);

    /**
     *  每次增强一个类，都调用一下这个方法，就可以将最新的字节码记录下来，下次增强就是基于这次变更的最新
     *  字节码来增强了
     *
     * @param className 类
     * @param bytes 本次需要记录的字节码
     */
    void recordEnhanceByteForClass(String className, byte[] bytes);

    /**
     *  用于将类的内部类缓存起来，因为如果每次都去增强一下原始类，听操蛋的
     *
     * @param cls 原始类
     * @param innerClass 内部类，然后不存在内部类，只要调用了该方法，则缓存一个空结果
     */
    void storeClassInnerClass(String cls, String innerClass);

    /**
     *  获取一个类的内部类信息
     *
     * @param cls 需要获取的类
     * @return 内部类信息，可能为空
     */
    String getClassInnerClass(String cls);

    /**
     *  get the cached method's line range [l,r]
     *
     * @param cacheKey the cache key
     * @return the lr-model
     */
    LRModel getLRModel(String cacheKey);

    /**
     * cache the lr model
     *
     * @param cacheKey key
     * @param lrModel value
     */
    void cacheLRModel(String cacheKey, LRModel lrModel);

    /**
     *  list commands
     *
     * @return the commands
     */
    String listCommands();

}
