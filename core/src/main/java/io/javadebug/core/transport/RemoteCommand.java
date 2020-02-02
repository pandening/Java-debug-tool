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
//  Auth : HJ


package io.javadebug.core.transport;

import io.javadebug.core.utils.UTILS;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created on 2019/4/20 11:08.
 *
 *
 * @since 2.0
 *    $back-response-type:
 *              不存在：保持1.0的处理方式，不用管直接输出
 *              1: 正常展示$back-data即可
 *              2: 输出到文件，具体的文件格式从 $back-store-suffix中获取
 *              3：忽略，进行下一个命令处理
 *
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class RemoteCommand implements Serializable {

    ///---------------------------------
    /// 这个tag用来标记客户端的状态，如果是首次链接，那么这个tag就是0
    /// 否则这个tag就是1，后续可能会被优化掉
    ///---------------------------------
    private byte tag;

    ///---------------------------------
    /// 客户端属性tag，这个tag非常重要，和命令执行权限相关，但是命令权限
    /// 控制并不完全依赖这个属性，这个属性仅区分"master console"和"normal console"
    ///---------------------------------
    private byte clientTag;

    ///---------------------------------
    /// client和server交换信息的主要载体，当当前对象代表Request-forward的时候，这个字段里面
    /// 的内容就是命令执行所需要的参数信息，server要根据不同的命令来获取参数，当当前对象代表的
    /// 是Response-forward的时候，这里面包含了返回信息，之所以这样设计是因为这样设计实现起来
    /// 比较简单，并且非常灵活
    /// 规范：
    /// 对于Client -> Server的参数携带，前缀为:$forward-[cmd]-[key]
    /// 对于Server -> Client的参数携带，前缀为:$back-[cmd]-[key]
    /// 如果和命令无关，则可以将[cmd]去掉，比如Server->Client的错误码:$back-errorCode
    ///---------------------------------
    private Map<String, Object> params;

    ///---------------------------------
    /// 协议版本，为了避免不必要的麻烦，所有连接的客户端必须和当前服务端的协议版本保持一致，否则
    /// Server将拒绝执行任何命令，甚至有可能会将链接关闭掉
    ///---------------------------------
    private byte version;

    ///---------------------------------
    /// 协议类型，标记当前对象代表的是Request-forward还是Response-forward
    ///---------------------------------
    private byte protocolType;

    ///---------------------------------
    /// 命令执行超时时间，如果客户端希望控制命令执行的时间，那么设置这个参数，服务
    /// 端会定期检查任务进度，并检查当前时间，如果发现正在执行的命令设置了超时
    /// 时间，并且已经超时，那么就会停止执行命令
    ///---------------------------------
    private short timeOut;

    ///---------------------------------
    /// 命令执行Round，一次C-S将会把计数器 + 1
    ///---------------------------------
    private int callSeq;

    ///---------------------------------
    /// 一个连接在进行命令执行之前，必须先获取到ContextId，并且连接的生命周期之内都需要持有该
    /// Context id，否则命令无法执行；Context Id用来唯一标记一个客户端连接，后续可能会基于
    /// 该字段来做一些有趣的事情
    ///---------------------------------
    private int contextId;

    ///---------------------------------
    /// 命令名称
    ///---------------------------------
    private String commandName;

    ///---------------------------------
    /// 客户端设置的请求开始时间，这个时间服务端不用动，在响应的时候直接将其设置到Response的
    /// 对应字段中去即可，这个时间是客户端为了计算一个命令的整体执行时间的，包括了
    /// 客户端设置时间起执行的逻辑 + 命令传输到服务端的时间 + 服务端执行时间 + 命令传输到客户端的时间
    ///  + 客户端处理响应的时间
    /// 这个字段客户端比较关心，但是比较泛，和多种因素相关，比如网络
    ///---------------------------------
    private long timestamp;

    ///---------------------------------
    /// 服务端开始执行命令的时间戳
    ///---------------------------------
    private long executeStartTime;

    ///---------------------------------
    /// 心跳检测
    /// 服务端会将发送一个alive值为1的消息，客户端
    /// 需要响应一个alive为2的消息，否则会直接
    /// 将客户端连接关闭
    ///---------------------------------
    private byte alive;

    ///---------------------------------
    /// 服务端执行的耗时，从接收到命令开始计算，到命令写到Channel为止
    ///---------------------------------
    private long cost;

    ///---------------------------------
    /// 命令执行到敏感位置，需要STW，那么粗略的计算一下这个STW的耗时，参考
    /// 价值不大，后续再优化；
    /// 如果命令执行是纯计算逻辑，不涉及STW，那么这个字段的值为0
    ///---------------------------------
    private int stwCost;

    ///---------------------------------
    /// 需要在传输之前进行删除的参数，请放在这里
    ///---------------------------------
    private Set<String> customKeys = new HashSet<>();

    public RemoteCommand simpleCopy(RemoteCommand target) {
        if (target == null) {
            return null;
        }

        // 清理客户key
        clearCustomParams();

        // 清空
        customKeys.clear();

        target.setTag(tag).setClientTag(clientTag).setVersion(version).setProtocolType(protocolType)
                .setCallSeq(callSeq).setContextId(contextId).setCommandName(commandName)
                .setTimeStamp(timestamp).setStwCost(stwCost).setCost(cost);

        // set the params
        target.params = params;

        return target;
    }

    /**
     *  需要将命令相关的上下文传输内容去掉，不然会影响序列化等步骤
     *
     */
    private void clearCustomParams() {
        if (customKeys == null || customKeys.isEmpty()) {
            return;
        }

        for (String key : customKeys) {
            params.remove(key);
        }
    }

    /**
     *  某些时候，你只是想在上下文中进行参数传递，那么可以将那些key放在这里
     *  在传输之前清理一下，否则会产生无法进行cs通信等后果
     *
     * @param key 需要记录的key
     */
    private void addCustomKey(String key) {
        if (customKeys == null) {
            customKeys = new HashSet<>();
        }
        customKeys.add(key);
    }

    /**
     *  你可以使用这个方法进行stop tag的设置
     *
     * @param tag "true"代表需要停止命令执行，其他值代表命令可以继续执行
     */
    public void setStopTag(String tag) {
        addParam("$command-common-stop-tag", tag);
        setResponseData("命令执行已经超时!\n");
    }

    /**
     *  判断一下命令是否需要停止执行
     *
     *  一般情况下，你不需要关心这个tag，但是有些命令是block类型的，会一直等到有结果匹配才会
     *  结束，比如trace命令，这个时候如果不加以控制，因为服务端命令处理线程非常有限的缘故，会
     *  造成客户端提交的命令执行任何被拒绝的现象，所以对这些命令进行超时探测是非常有必要的
     *
     * @return true代表你需要停止执行
     */
    public boolean needStop() {
        String stopTag = getParam("$command-common-stop-tag");
        return !UTILS.isNullOrEmpty(stopTag) && "true".equals(stopTag);
    }

    /**
     *  有些时候需要在命令处理过程中设置命令处理失败，调用这个方法后命令的状态就会变为处理失败，这个
     *  时候你需要提供具体的错误信息，否则会被标记为"未识别异常"
     *
     * @param error 具体的错误信息
     */
    public void setErrorStatusWithErrorMessage(String error) {
        if (UTILS.isNullOrEmpty(error)) {
            error = "unknown error";
        }
        addParam("$back-errorCode", "-1");
        addParam("$back-errorMsg", error);
    }

    /**
     *  判断一下是否设置过error相关信息
     *
     * @return true代表已经设置过了
     */
    public boolean hasSetErrorStatus() {
        String errorCode = getParam("$back-errorCode");
        return !UTILS.isNullOrEmpty(errorCode);
    }

    /**
     *  有些时候需要需要在命令中途就设置返回结果，那么这个方法就会变得有用
     *
     * @param response 响应结果
     */
    public void setResponseData(String response) {
        if (UTILS.isNullOrEmpty(response)) {
            return;
        }
        addParam("$back-errorCode", "0");
        addParam("$back-data", response);
    }

    /**
     *  判断一下是否已经设置过响应结果了，这个方法用来在设置相应结果之前进行判断，如果
     *  已经设置过了，那么就不需要再设置了，当然完全看你自己
     *
     * @return true代表已经设置过了
     */
    public boolean hasResult() {
        String resp = getParam("$back-data");
        String error = getParam("$back-errorCode");
        return !UTILS.isNullOrEmpty(resp) || (!UTILS.isNullOrEmpty(error) && "-1".equals(error));
    }

    /**
     *  因为一个连接一直在复用一个协议对象，所以可能会不同的命令会相互干扰，为此
     *  在将协议发送到服务端之前，进行清理工作
     *
     * @return this
     */
    public RemoteCommand clearShit() {
        if (this.params == null) {
            this.params = new ConcurrentHashMap<>();
            return this;
        }

        // ------------------
        /// 请确认不会产生影响
        // ------------------
        this.params.clear();

        /// 确保重新设置命令名称
        this.commandName = "";

        // 确保重新设置时间
        this.timestamp = 0;

        // stw cost
        this.stwCost = 0;

        return this;
    }

    public RemoteCommand setClientTag(byte clientTag) {
        this.clientTag = clientTag;
        return this;
    }

    public RemoteCommand setVersion(byte version) {
        this.version = version;
        return this;
    }

    public RemoteCommand setTimeStamp(long timeStamp) {
        this.timestamp = timeStamp;
        return this;
    }

    public RemoteCommand setCost(long cost) {
        this.cost = cost;
        return this;
    }

    public RemoteCommand setStwCost(int stwCost) {
        this.stwCost = stwCost;
        return this;
    }

    public RemoteCommand setTag(byte tag) {
        this.tag = tag;
        return this;
    }
   
    public RemoteCommand setProtocolType(byte protocolType) {
        this.protocolType = protocolType;
        return this;
    }

   
    public RemoteCommand setTimeOut(short timeOut) {
        this.timeOut = timeOut;
        return this;
    }

   
    public RemoteCommand setCallSeq(int callSeq) {
        this.callSeq = callSeq;
        return this;
    }

   
    public RemoteCommand setContextId(int contextId) {
        this.contextId = contextId;
        return this;
    }

   
    public RemoteCommand setCommandName(String commandName) {
        this.commandName  = commandName;
        return this;
    }

    public RemoteCommand removeParam(String key) {
        if (this.params == null) {
            this.params = new ConcurrentHashMap<>();
        }
        if (UTILS.isNullOrEmpty(key)) {
            return this;
        }
        this.params.remove(key);
        return this;
    }

    public <T> RemoteCommand addCustomParam(String key, T val) {
        addCustomKey(key);
        return addParam(key, val);
    }
   
    public <T> RemoteCommand addParam(String key, T val) {
        if (this.params == null) {
            this.params = new ConcurrentHashMap<>();
        }
        this.params.put(key, val);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getParam(String key) {
        if (this.params == null) {
            this.params = new ConcurrentHashMap<>();
        }
        return (T) this.params.get(key);
    }

    public byte getClientTag() {
        return this.clientTag;
    }

    public long getCost() {
        return this.cost;
    }

    public int getStwCost() {
        return this.stwCost;
    }

    public byte getTag() {
        return this.tag;
    }

    public long getTimestamp() {
        return this.timestamp;
    }
   
    public byte getVersion() {
        return this.version;
    }

   
    public byte getProtocolType() {
        return this.protocolType;
    }

   
    public short getTimeOut() {
        return this.timeOut;
    }

   
    public int getCallSeq() {
        return this.callSeq;
    }

   
    public int getContextId() {
        return this.contextId;
    }

   
    public short getCommandLen() {
        if (this.commandName == null) {
            return 0;
        }
        return (short) this.commandName.getBytes().length;
    }

   
    public short getCommandParamCount() {
        if (this.params == null) {
            return 0;
        }
        return (short) this.params.size();
    }

   
    public String getCommandName() {
        return commandName;
    }

   
    public Map<String, Object> getCommandParams() {
        if (this.params == null) {
            this.params = new ConcurrentHashMap<>();
        }
        return params;
    }

    @Override
    public String toString() {
        return "RemoteCommand{" +
                "tag=" + tag +
                ", clientTag=" + clientTag +
                ", params=" + params +
                ", version=" + version +
                ", protocolType=" + protocolType +
                ", timeOut=" + timeOut +
                ", callSeq=" + callSeq +
                ", contextId=" + contextId +
                ", commandName='" + commandName + '\'' +
                ", timestamp=" + timestamp +
                ", cost=" + cost +
                ", stwCost=" + stwCost +
                ", params=" + params +
                '}';
    }

    public long getExecuteStartTime() {
        return executeStartTime;
    }

    public RemoteCommand setExecuteStartTime(long executeStartTime) {
        this.executeStartTime = executeStartTime;
        return this;
    }

    public byte getAlive() {
        return alive;
    }

    public RemoteCommand setAlive(byte alive) {
        this.alive = alive;
        return this;
    }
}
