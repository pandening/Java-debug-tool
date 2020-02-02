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


package io.javadebug.core.transport;

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2019/4/28 23:03.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class CDLatchTransportSubject implements TransportSubject {

    private CountDownLatch countDownLatch;
    private int unavailableCnt;
    private int availableCnt;
    private Map<Channel, RemoteCommand> responseMap;
    private Set<Connection> connectionSet;

    //-----------------------------------------
    /// 是否是简单模式，简单模式意味着命令执行的输出内容更少，复杂
    /// 模式下输出是所有客户端的执行结果，但是如果连接的JVM比较多的
    /// 时候，可能就不那么好看，用户仅仅想看看哪些jvm执行成功了，哪些没有成功
    /// 命令耗时是多少等等
    //-----------------------------------------
    private static final boolean simpleMode = true;

    //------------------------------------------
    /// 命令发送之前都需要进行灰度发布，这个tag用于标记一次round
    /// 是否已经进行过灰度了，如果没有进行过，那么会选择一连连接进行
    /// 灰度控制，否则不会进行拦截
    /// 0 : 未进行灰度控制
    /// 1 : 正在进行灰度控制
    /// 2 : 灰度控制结束
    //------------------------------------------
    private int greyTag = 0;

    public CDLatchTransportSubject(Set<Connection> connections) {
        this.unavailableCnt = 0;
        this.responseMap = new HashMap<>();
        this.connectionSet = connections;
        this.availableCnt = connectionSet.size();
        this.countDownLatch = new CountDownLatch(connectionSet.size());
    }

    /**
     * 有些命令发送需要进行灰度，否则容易造成事故，因此，在进行命令发送之前，请调用该方法进行
     * 灰度发布控制
     */
    @Override
    public void greyController(int cnt) {
        greyTag = 1;
        this.countDownLatch = new CountDownLatch(cnt);
    }

    /**
     * 释放灰度发布开关
     */
    @Override
    public void releaseGreyController(int cnt) {
        greyTag = 2;
        this.countDownLatch = new CountDownLatch(cnt);
    }

    /**
     * 开启新的一轮请求，每次开始发送命令之前都应该调用这个方法，否则
     * 会造成不同批次的请求互相干扰的情况
     *
     * @param connSize 链接数量
     */
    @Override
    public void anotherRound(int connSize) {
        if (greyTag == 1) {
            this.countDownLatch = new CountDownLatch(1); // 只允许灰度一台
            return; // 正在灰度
        }
        //PSLogger.info("another round with need response size:" + connSize);
        this.countDownLatch = new CountDownLatch(connSize);
        this.availableCnt = connectionSet.size();
        this.unavailableCnt = 0;
        this.responseMap.clear();
    }

    /**
     * 某些情况下，连接已经变得不可用，这个时候如果不改变cd值，那么客户端会一直在
     * 等待，为了解决这个问题，需要动态控制cd值
     *
     * @param size 失效的客户端数量
     */
    @Override
    public void countdownConnect(int size) {
        this.unavailableCnt += size;
    }

    /**
     *  根据 {@link Channel} 找到连接
     *
     * @param connection {@link Channel}
     * @return {@link Connection} 与这个Channel对应的连接
     */
    private Connection getConnect(Channel connection) {
        for (Connection conn : connectionSet) {
            if (conn.getConnection() == connection) {
                return conn;
            }
        }
        PSLogger.error("没有找到合适的连接，请确认连接列表");
        return null;
    }

    /**
     * 当服务端有响应的时候，这个方法就会被回调，客户端可以从这里拿到响应内容
     *
     * @param channel       {@link Channel}
     * @param remoteCommand {@link RemoteCommand}
     */
    @Override
    public void onResponse(Channel channel, RemoteCommand remoteCommand) {
        try {
            this.responseMap.put(channel, remoteCommand);
            // 通知connect
            Connection connection = getConnect(channel);
            if (connection != null) {
                connection.assignRemoteCommand(remoteCommand);
            }
        } catch (Exception e) {
            PSLogger.error(String.format("error while handle onResponse:[%s] [%s] [%s]",
                    channel.remoteAddress(), remoteCommand, e));
        } finally {
            this.countDownLatch.countDown();
        }
    }

    /**
     * 当客户端发生错误的时候，这个方法会被回调，并将出现错误的原因传递进来
     *
     * @param channel       {@link Channel}
     * @param e             具体的错误原因，优先从cause中拿错误原因
     */
    @Override
    public void onError(Channel channel, Throwable e) {
        try {
            Connection connection = getConnect(channel);
            if (connection != null) {
                RemoteCommand remoteCommand = connection.remoteCommand();
                remoteCommand.addParam("$back-errorCode", "-1").addParam("$back-errorMsg", e);
                this.responseMap.put(channel, remoteCommand);
            }
        } catch (Exception ee) {
            PSLogger.error(String.format("error while handle onResponse:[%s] [%s]",
                    channel.remoteAddress(), ee));
        } finally {
            this.countDownLatch.countDown();
        }
    }

    /**
     * 判断一下当前Subject是否已经完成了，无论是通过什么方式完成的，都算是完成，比如
     * {@code onResponse} 或者 {@code onError}
     *
     * @return true 代表完成
     */
    @Override
    public boolean isDone() {
        return this.countDownLatch.getCount() - unavailableCnt <= 0;
    }

    /**
     * 无论处理结果是正确的还是错误的，都可以从这里拿到
     * <p>
     * 注意，这个方法应该是阻塞的，需要将调用方阻塞起来，直到可以返回结果给调用方
     *
     * @return 结果，是多个服务端处理结果的综合，不是某一个连接的处理结果
     */
    @Override
    public RemoteCommand resp() {
        // 每一秒判断一次，总会走出循环
        // 客户端死等，不要主动超时
        while (!isDone()) {
            // wait to done
            try {
                this.countDownLatch.await(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                PSLogger.error("InterruptedException:" + e);
            }
        }
        //PSLogger.error("start to collect round response");
        return collectRemoteCommand();
    }

    /**
     *  将所有的返回组合起来，形成一个统一的响应结果，这里面需要处理的细节比较多，可能
     *  还会动态调整生成的结果内容
     *
     * @return {@link RemoteCommand}
     */
    private RemoteCommand collectRemoteCommand() {
       if (simpleMode) {
           return simpleMode();
       } else {
           return complexMode();
       }
    }

    /**
     *  复杂模式输出，会将所有命令的输出累计起来
     *
     * @return 输出
     */
    private RemoteCommand complexMode() {
        ///----
        // 这是最后响应的结果，需要将所有的请求收集起来，再报告出去
        RemoteCommand remoteCommand = new RemoteCommand();

        // response type
        remoteCommand.setProtocolType(CommandProtocol.COMMAND_RES_PROTOCOL_TYPE);

        // resp
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<Channel, RemoteCommand> entry : responseMap.entrySet()) {
            Channel channel = entry.getKey();
            RemoteCommand rc = entry.getValue();
            // 命令名称
            if (!"-1".equals(rc.getParam("$back-errorCode"))) {
                if (UTILS.isNullOrEmpty(remoteCommand.getCommandName()) && !UTILS.isNullOrEmpty(rc.getCommandName())) {
                    remoteCommand.setCommandName(rc.getCommandName());
                }
            }

            // round
            if (remoteCommand.getCallSeq() < rc.getCallSeq()) {
                remoteCommand.setCallSeq(rc.getCallSeq());
            }

            // context id
            if (remoteCommand.getContextId() < rc.getContextId()) {
                remoteCommand.setContextId(rc.getContextId());
            }

            // timestamp
            if ((remoteCommand.getTimestamp() == 0L) || (remoteCommand.getTimestamp() > rc.getTimestamp())) {
                remoteCommand.setTimeStamp(rc.getTimestamp());
            }

            // stw
            if (remoteCommand.getStwCost() < rc.getStwCost()) {
                remoteCommand.setStwCost(rc.getStwCost());
            }

            // resp
            if ("-1".equals(remoteCommand.getParam("$back-errorCode"))) {
                sb.append(channel.remoteAddress()).append(" 处理失败\t：\n")
                        .append((String)rc.getParam("$back-errorMsg"))
                        .append("\n");
            } else {
                sb.append(channel.remoteAddress()).append(" 处理成功\t：\n")
                        .append((String) rc.getParam("$back-data"))
                        .append("\n");
            }
        }

        // 设置处理结果
        remoteCommand.addParam("$back-data", sb.toString());

        return remoteCommand;
    }

    /**
     *  简单模式输出，会对命令执行响应进行分析统计，输出一些简单的结果
     *
     * @return 输出
     */
    private RemoteCommand simpleMode() {
        ///----
        // 这是最后响应的结果，需要将所有的请求收集起来，再报告出去
        RemoteCommand remoteCommand = new RemoteCommand();

        // response type
        remoteCommand.setProtocolType(CommandProtocol.COMMAND_RES_PROTOCOL_TYPE);

        // resp
        StringBuilder errorSb = new StringBuilder();
        StringBuilder successSb = new StringBuilder();
        int errorCount = 0, successCount = 0;
        int totalCost = 0, minCost = 0, maxCost = 0, minStw =0, maxStw = 0, totalStw = 0;
        double avgCost = 0.0, avgStw = 0.0;
        int round = 0; // round is same, choose one

        for (Map.Entry<Channel, RemoteCommand> entry : responseMap.entrySet()) {
            Channel channel = entry.getKey();
            RemoteCommand command = entry.getValue();

            if ("-1".equals(command.getParam("$back-errorCode"))) {
                errorCount ++;
                errorSb.append(channel.remoteAddress()).append(" 执行失败:")
                        .append((String) command.getParam("$back-errorMsg")).append("\n");
            } else {
                successCount ++;
                successSb.append(channel.remoteAddress()).append(" 执行成功:")
                        .append((String) command.getParam("$back-data")).append("\n");

                // command name
                if (UTILS.isNullOrEmpty(remoteCommand.getCommandName()) && !UTILS.isNullOrEmpty(command.getCommandName())) {
                    remoteCommand.setCommandName(command.getCommandName());
                }
            }

            // round
            if (round == 0) {
                round = command.getCallSeq();
                remoteCommand.setCallSeq(round);
            }

            // context id
            if (remoteCommand.getContextId() < command.getContextId()) {
                remoteCommand.setContextId(command.getContextId());
            }

            int theCost = (int) (System.currentTimeMillis() - command.getTimestamp());

            // cost
            totalCost += theCost;

            // min cost
            if (minCost == 0 || minCost > (theCost)) {
                minCost = theCost;
            }

            // max cost
            if (maxCost == 0 || maxCost < theCost) {
                maxCost = theCost;
            }

            // stw
            if (minStw == 0 || minStw > command.getStwCost()) {
                minStw = command.getStwCost();
            }

            if (maxStw == 0 || maxStw < command.getStwCost()) {
                maxStw = command.getStwCost();
            }

            totalStw += command.getStwCost();

        }

        avgCost = totalCost / (responseMap.size() * 1.0);
        avgStw  = totalStw  / (successCount * 1.0);

        StringBuilder respSb = new StringBuilder();

        //// 连接数是初始化时的所有连接，（执行成功 + 执行失败） <= 连接数
        //// 如果小于，那么小于的部分说明已经被剔除掉了，需要通过其他的命令获取到
        //// 这些被剔除的连接到底是哪些，以便进行补充处理

        // count
        respSb.append("连接数   \t: ").append(availableCnt)
                .append(" \t执行成功数\t: ").append(successCount)
                .append(" \t执行失败数\t: ").append(errorCount).append("\n");

        // cost
        respSb.append("最小耗时  \t: ").append(minCost)
                .append(" \t最长耗时 \t: ").append(maxCost)
                .append(" \t平均耗时\t: ").append(avgCost).append("\n");

        respSb.append("最小STW  \t: ").append(minStw)
                .append(" \t最长STW \t: ").append(maxStw)
                .append(" \t平均STW \t: ").append(avgStw).append("\n");

        // error resp
        respSb.append(errorSb).append("\n").append(successSb);

        return remoteCommand.addParam("$back-data", respSb.toString())
                .addParam("$common-show-set-no-title", "true"); // 不要把头展示出来
    }

}
