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


package io.javadebug.core.handler;

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.transport.RemoteCommand;

public abstract class StopAbleRunnable implements Runnable {

    // which console hold this runnable
    private int contextId;

    // the start time mills
    private long startMills;

    // the task thread
    private Thread taskThread;

    // true if the task run done.
    private  volatile  boolean isDone = false;

    // the command name
    private RemoteCommand command;

    protected StopAbleRunnable(RemoteCommand command) {
        this.command = command;
    }

    public void setContextId(int contextId) {
        this.contextId = contextId;
    }

    public int getContextId() {
        return this.contextId;
    }

    public RemoteCommand getCommand() {
        return command;
    }

    public long getStartMills() {
        return this.startMills;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {

        // get the start time mills
        startMills = System.currentTimeMillis();

        // get the task thread
        taskThread = Thread.currentThread();

        // start to run the tack
        try {
            execute();
        } catch (Exception e) {
            PSLogger.error("error : " + UTILS.getErrorMsg(e));
        } finally {
            // done
            isDone = true;
        }

    }

    /**
     *  你应该继承这个接口，用于实现一个线程的执行任务，如果需要在任意时刻终止
     *  任务的执行，调用stop方法可以打断该线程的执行
     *
     */
    public abstract void execute();

    /**
     *  如果任务执行超时了，或者任意时刻想要终止任务执行，就可以调用这个方法
     *
     */
    public void stop() {
        if (isDone) {
            return;
        }
        if (taskThread != null && !taskThread.isInterrupted()) {
            PSLogger.error("start to interrupt the thread:" + taskThread);
            taskThread.interrupt(); // stop
        } else {
            PSLogger.error("the task thread is null or the task thread has been interrupted");
        }
    }

    @Override
    public String toString() {
        return contextId + "@" + command + " with start mills:"  +startMills;
    }
}
