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


package io.javadebug.core.command.perf;

import io.javadebug.core.utils.Tuple2;

public class ThreadRichnessInfo {

    // the cpu tuple info
    // t1: -> cpu pct
    // t2: -> usr cpu pct
    private Tuple2<Long, Long> cpuTuple;

    // the origin thread ref
    private Thread t;

    // the thread count
    private int threadCnt = 0;

    private int rCount = 0;

    private int wCount = 0;

    private int twCount = 0;

    private int tCount = 0;

    private int bCount = 0;

    private int nCount = 0;

    public void setT(Thread t) {
        this.t = t;
    }

    public int getrCount() {
        return rCount;
    }

    public void setrCount(int rCount) {
        this.rCount = rCount;
    }

    public int getwCount() {
        return wCount;
    }

    public void setwCount(int wCount) {
        this.wCount = wCount;
    }

    public int getTwCount() {
        return twCount;
    }

    public void setTwCount(int twCount) {
        this.twCount = twCount;
    }

    public int getbCount() {
        return bCount;
    }

    public void setbCount(int bCount) {
        this.bCount = bCount;
    }

    public int getnCount() {
        return nCount;
    }

    public void setnCount(int nCount) {
        this.nCount = nCount;
    }

    public void setThread(Thread t) {
        this.t = t;
    }

    public void setCpuTuple(Tuple2<Long, Long> cpuTuple) {
        this.cpuTuple = cpuTuple;
    }

    public Tuple2<Long, Long> getCpuTuple() {
        return cpuTuple;
    }

    public Thread getT() {
        return t;
    }

    public int getThreadCnt() {
        return threadCnt;
    }

    public void setThreadCnt(int threadCnt) {
        this.threadCnt = threadCnt;
    }

    public int gettCount() {
        return tCount;
    }

    public void settCount(int tCount) {
        this.tCount = tCount;
    }
}
