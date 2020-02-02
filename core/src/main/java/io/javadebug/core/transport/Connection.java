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
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetSocketAddress;

/**
 * Created on 2019/4/28 16:51.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class Connection {

    private String ip;
    private int port;
    private RemoteCommand remoteCommand;
    private Channel connection;

    //---------------------------------
    /// 只有当round = true的时候才证明协议可以发送
    //---------------------------------
    private boolean round = false;

    //-----------------------------------
    /// 只有被选择了，才能进行命令发送，否则只能处于
    /// 静默状态，默认是true代表可以进行命令发送，用于
    /// 进行灰度控制等功能
    //-----------------------------------
    private boolean choose = true;

    /**
     *  选择或者静默该连接，一定调用该方法进行命令控制
     *
     * @param flag true 代表选择该连接，false代表该连接暂时不可以发送命令
     * @return this
     */
    public Connection choose(boolean flag) {
        this.choose = flag;
        return this;
    }

    public Connection(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public Channel getConnection() {
        return this.connection;
    }

    /**
     *  判断一下这个服务端是否已经连接上了
     *
     * @return true代表连接上了
     */
    public boolean isConnected() {
        return this.connection != null && this.connection .isActive();
    }

    /**
     *  调用这个方法，可以将该连接下的协议发往远程服务端
     *
     * @return {@link ChannelFuture}
     */
    public ChannelFuture writeAndFlush() {
        if (!choose) {
            PSLogger.error("connection:" + toString()  + " do not be choose.");
            return null;
        }
        if (!round) {
            throw new IllegalStateException("the remoteCommand is not readied now!");
        }
        if (!isConnected()) {
            throw new IllegalStateException("the connect is unavailable now:" + connection);
        }
        if (!this.connection.isWritable()) {
            PSLogger.error("连接不可写");
            this.connection.close().addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if (!future.isSuccess()) {
                        PSLogger.error("关闭连接：" + connection + " 出现错误：" + future.cause());
                    }
                }
            });
            return null;
        }
        try {
            return this.connection.writeAndFlush(this.remoteCommand).addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if (!future.isSuccess()) {
                        PSLogger.error(String.format("无法发送协议到:[%s], cause:[%s]", toString(), future.cause()));
                    }
                }
            });
        } catch (Exception e) {
            PSLogger.error(String.format("消息发送出来失败:[%s],[%s]", toString(), e));
            throw e;
        } finally {
            /// 发送之后这个协议就不能再次使用了，如果想要再次使用，调用assignRemoteCommand方法
            /// 激活一下
            this.round = false;
        }
    }

    /**
     *  配置好了之后就可以使用该方法生成一个{@link InetSocketAddress}
     *
     * @return {@link InetSocketAddress}
     */
    public InetSocketAddress toAddress() {
        if (UTILS.isNullOrEmpty(ip) || port <= 0 || port >= 65535) {
            throw new IllegalArgumentException(String.format("不合法的连接参数配置:[%s:%d]",
                    UTILS.nullToEmpty(ip), port));
        }

        if ("127.0.0.1".equals(ip) || "localhost".equals(ip)) {
            return new InetSocketAddress(port);
        } else {
            return new InetSocketAddress(ip, port);
        }
    }

    /**
     *  比如一个 {@link io.netty.channel.Channel}，用于向服务端发送命令
     *
     * @param con {@link io.netty.channel.Channel}
     */
    public void assignConnect(Channel con) {
        if (con != null) {
            this.connection = con;
        }
    }

    /**
     *  每个链接都有一个唯一的协议体，连接的生命周期内都使用这个协议体即可
     *
     * @param remoteCommand {@link RemoteCommand}
     */
    public void assignRemoteCommand(RemoteCommand remoteCommand) {
        this.remoteCommand = remoteCommand;
        this.round = true;
    }

    public RemoteCommand remoteCommand() {
        if (this.remoteCommand == null) {
            this.round = true;
            return new RemoteCommand();
        }
        return this.remoteCommand.clearShit();
    }

    /**
     *  获取到地址信息
     *
     * @return ip, port, ip:port
     */
    public String address() {
        if (UTILS.isNullOrEmpty(ip) && (port <= 0 || port >= 65535)) {
            return null;
        }
        if (UTILS.isNullOrEmpty(ip)) {
            return port + "";
        }
        if (port <= 0 || port >= 65535) {
            return ip;
        }
        return ip + ":" + port;
    }

    @Override
    public String toString() {
        return UTILS.nullToEmpty(ip) + ":" + port + "@" +
                (connection == null ? "null" : connection.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Connection)) {
            return false;
        }
        return ((Connection) obj).getIp().equals(this.ip)
                && ((Connection) obj).getPort() == this.port;
    }

    @Override
    public int hashCode() {
        return ip.hashCode() + port * 43;
    }
}
