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


package io.javadebug.core.utils;

import io.javadebug.core.log.PSLogger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2019/4/29 23:04.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class CDHelper {

    // -----------------------------------------
    /// 用于存储所有的cd
    // -----------------------------------------
    private static final ConcurrentMap<String, CountDownLatch> CD_LATCH_MAP = new ConcurrentHashMap<>();

    /**
     *  设置一个cd
     *
     * @param key key
     * @param lock cd值
     */
    public static void set(String key, int lock) {
        CountDownLatch countDownLatch = new CountDownLatch(lock);
        CD_LATCH_MAP.put(key, countDownLatch);
    }

    /**
     *  执行countdown操作，每次减1
     *
     * @param key key
     */
    public static void cd(String key) {
        if (isDone(key)) {
            return; // 如果已经完成了，那么就不要继续执行了
        }
        CountDownLatch countDownLatch = CD_LATCH_MAP.get(key);
        if (countDownLatch == null) {
            return;
        }
        countDownLatch.countDown();
    }

    /**
     *  {@link CountDownLatch#await()}
     *
     * @param key key
     */
    public static void await(String key) {
        CountDownLatch countDownLatch = CD_LATCH_MAP.get(key);
        if (countDownLatch == null) {
            return;
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            PSLogger.error("error:" + e);
            throw new RuntimeException("命令执行超时被阻断:" + e);
        }
    }

    /**
     *  {@link CountDownLatch#await(long, TimeUnit)}
     *
     * @param key key
     * @param timeVal timeout
     * @param unit unit
     */
    public static void await(String key, long timeVal, TimeUnit unit) {
        CountDownLatch countDownLatch = CD_LATCH_MAP.get(key);
        if (countDownLatch == null) {
            return;
        }
        try {
            countDownLatch.await(timeVal, unit);
        } catch (InterruptedException e) {
            PSLogger.error("error:" + e);
        }
    }

    /**
     *  判断一下当前CD是否完成
     *
     * @param key key
     * @return true代表已经完成
     */
    public static boolean isDone(String key) {
        CountDownLatch countDownLatch = CD_LATCH_MAP.get(key);
        return countDownLatch == null || countDownLatch.getCount() == 0;
    }

    /**
     *  某些情况下不需要等待CD值到0，这个时候可以提供一个reduce值，只要CD
     *  值等于reduce就会返回成功
     *
     * @param key key
     * @param reduceVal reduce val
     * @return 是否完成
     */
    public static boolean isDone(String key, int reduceVal) {
        CountDownLatch countDownLatch = CD_LATCH_MAP.get(key);
        return countDownLatch == null || countDownLatch.getCount() <= reduceVal;
    }

}
