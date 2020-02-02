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

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.Preconditions;
import io.javadebug.core.exception.CRC32CheckException;
import io.javadebug.core.security.CRC32Utils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 2019/4/20 11:42.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class CommandCodec {

    // ----------------------------------------------
    /// 在初始化Agent的时候，将Agent加到系统类搜索路径中，这样这里可以拿到
    /// Agent的类加载器，这样就可以加载到Agent中的类了，非常聪明吧
    /// 仅服务端需要关注，客户端不需要，因为客户端可以找到相关类，但是
    /// 服务端在进行hessian解码的时候是根据当前线程的classLoader来加载反序列化类的
    /// 而加载Agent的类加载器是自定义的，所以需要特殊处理一下
    // ----------------------------------------------
    public static ClassLoader agentClassLoader = null;

    static {

        try {
            Class<?> agentCls = Thread.currentThread().getContextClassLoader().loadClass("io.javadebug.agent.Agent");

            agentClassLoader = (ClassLoader) agentCls.getMethod("getAgentClassLoader").invoke(null);

            PSLogger.error("init agentClassloader:" + agentClassLoader);

        } catch (Exception e) {
            PSLogger.info("[client side ignore] could not find class ：io.javadebug.agent.Agent:" + e);
        }

    }

    /**
     *  获取到协议编码handler
     *
     * @return {@link ChannelHandler}
     */
    public static ChannelHandler getEncodeHandler() {
        return new MessageToByteEncoder<RemoteCommand>() {
            /**
             * Encode a message into a {@link ByteBuf}. This method will be called for each written message that can be handled
             * by this encoder.
             *
             * @param ctx the {@link ChannelHandlerContext} which this {@link MessageToByteEncoder} belongs to
             * @param msg the message to encode
             * @param out the {@link ByteBuf} into which the encoded message will be written
             * @throws Exception is thrown if an error occurs
             */
            @Override
            protected void encode(ChannelHandlerContext ctx, RemoteCommand msg, ByteBuf out) throws Exception {
                CommandCodec.en(msg, out);
            }
        };
    }

    /**
     *  获取到协议解码器
     *
     * @return {@link ChannelHandler}
     */
    public static ChannelHandler getDecodeHandler() {
        return new ByteToMessageDecoder() {
            @Override
            protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
                CommandCodec.de(in, out);
            }
        };
    }

    /**
     *  使用hessian进行序列化
     *
     * @param obj
     * @return
     */
    private static byte[] serialize(Object obj) {
        Preconditions.checkArgument(obj != null, "serialize: obj must not null");
        ByteArrayOutputStream byteArrayOutputStream = null;
        HessianOutput hessianOutput = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            hessianOutput = new HessianOutput(byteArrayOutputStream);
            hessianOutput.writeObject(obj);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception  e) {
            PSLogger.error("error serializer:" + e);
            throw new RuntimeException(String.format("error while serialize object:[%s]", obj.toString()), e);
        } finally {
            if (byteArrayOutputStream != null) {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (hessianOutput != null) {
                try {
                    hessianOutput.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     *  使用hessian反序列化对象
     *
     * @param data
     * @return
     */
    @SuppressWarnings("unchecked")
    private static <T> T deserialize(byte[] data) {
        ByteArrayInputStream byteArrayInputStream = null;
        HessianInput hessianInput = null;
        // get the current classloader
        ClassLoader cur = Thread.currentThread().getContextClassLoader();

        // agent loader
        if (agentClassLoader != null) {
            Thread.currentThread().setContextClassLoader(agentClassLoader);
        }
        try {
            byteArrayInputStream = new ByteArrayInputStream(data);
            hessianInput = new HessianInput(byteArrayInputStream);
            return (T) hessianInput.readObject();
        } catch (IOException e) {
            PSLogger.error("error deserializer:" + e);
        } finally {
            // rollback
            Thread.currentThread().setContextClassLoader(cur);
            if (byteArrayInputStream != null) {
                try {
                    byteArrayInputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (hessianInput != null) {
                hessianInput.close();
            }
        }
        throw new RuntimeException("deserialize fail.");
    }

    /**
     *  对消息进行编码
     *
     * @param msg
     * @param out
     */
    public static void en(RemoteCommand msg, ByteBuf out) throws CRC32CheckException {
        out.writeByte(100);
        byte[] bytes = serialize(msg);

        // calculate the crc32 val
        int crc32 = CRC32Utils.safeCalculateCRC32(bytes);
        if (crc32 == 0 || crc32 == -1) {
            throw new CRC32CheckException("[en]could not calculate CRC32 for this protocol");
        }

        // write the crc32 val
        out.writeInt(crc32);

        // write the protocol data
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }

    /**
     *  对消息进行解码
     *
     * @param in
     * @param out
     */
    public static void de(ByteBuf in, List<Object> out) throws CRC32CheckException {
        // special tag.
        if (in.readableBytes() < 1) {
            return;
        }
        in.markReaderIndex();
        if (in.readByte() != 100) {
            in.resetReaderIndex();
            PSLogger.error("invalid remote command protocol, error protocol magic number");
            return;
        }

        // crc32 check
        if (in.readableBytes() < 4) {
            in.resetReaderIndex();
            return;
        }

        int expectCrc32 = in.readInt();
        if (expectCrc32 == 0 || expectCrc32 == -1) {
            throw new CRC32CheckException("[de]could not get CRC32 for this protocol");
        }

        // protocol body
        if (in.readableBytes() < 4) {
            in.resetReaderIndex();
            return;
        }

        int len = in.readInt();
        if (in.readableBytes() < len) {
            in.resetReaderIndex();
            return;
        }
        byte[] bytes = new byte[len];
        in.readBytes(bytes);

        // CRC32 check
        int actualCrc32 = CRC32Utils.safeCalculateCRC32(bytes);
        if (actualCrc32 != expectCrc32) {
            throw new CRC32CheckException(String.format("the protocol body is invalid, %d:%d", expectCrc32, actualCrc32));
        }

        RemoteCommand remoteCommand = deserialize(bytes);
        out.add(remoteCommand);
    }


    /**
     * 对命令进行编码操作
     *
     * @param msg     {@link RemoteCommand}
     * @param out     {@link ByteBuf}
     * @throws Exception 处理异常
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static void encode(RemoteCommand msg, ByteBuf out) throws Exception {
        out.writeByte(msg.getTag());
        out.writeByte(msg.getClientTag());
        out.writeByte(msg.getVersion());
        out.writeByte(msg.getProtocolType());
        out.writeShort(msg.getTimeOut());
        out.writeInt(msg.getCallSeq());
        out.writeInt(msg.getContextId());
        out.writeLong(msg.getTimestamp());
        out.writeLong(msg.getCost());
        out.writeShort(msg.getCommandLen());
        out.writeShort(msg.getCommandParams().size());
        out.writeBytes(msg.getCommandName().getBytes("UTF-8"));

        for (Map.Entry<String, Object> entry : msg.getCommandParams().entrySet()) {
            if (entry != null) {
                out.writeInt(entry.getKey().getBytes("UTF-8").length);
                out.writeBytes(entry.getKey().getBytes("UTF-8"));
                Object v = entry.getValue();
                if (Map.class.isAssignableFrom(v.getClass())) {
                    // this is a map
                    out.writeByte(1); // 1 means map
                    Map<String, byte[]> bytesMap = new HashMap<>();
                    for (Map.Entry<String, Object> e : ((Map<String, Object>)v).entrySet()) {
                        bytesMap.put(e.getKey(), getBytes(e.getValue()));
                    }
                    // length
                    out.writeInt(bytesMap.size());
                    for (Map.Entry<String, byte[]> me : bytesMap.entrySet()) {
                        // key
                        out.writeInt(me.getKey().getBytes().length);
                        out.writeBytes(me.getKey().getBytes());
                        // val
                        out.writeInt(me.getValue().length);
                        out.writeBytes(me.getValue());
                    }

                } else if (v.getClass().isAssignableFrom(List.class)) {
                    // this is a list
                    out.writeByte(2); // 2 means list
                    throw new UnsupportedOperationException("暂不支持");
                } else if (v instanceof String) {
                    out.writeByte(3); // 3 means string
                    out.writeInt(((String)v).getBytes("UTF-8").length);
                    out.writeBytes(((String)v).getBytes("UTF-8"));
                } else {
                    throw new UnsupportedOperationException("不支持的编码类型:" + v.getClass());
                }
            }
        }
    }

    /**
     *  拿到byte
     *
     * @param v
     * @return
     */
    private static byte[] getBytes(Object v) {
        if (v instanceof String) {
            return ((String) v).getBytes();
        } else if (v instanceof byte[]) {
            return (byte[]) v;
        } else {
            throw new UnsupportedOperationException("不支持的编码类型:" + v.getClass());
        }
    }

    private static boolean ensureLen(ByteBuf byteBuf, int len, int rollbackIndex) {
        if (byteBuf.readableBytes() < len) {
            byteBuf.resetReaderIndex();
            PSLogger.error("没有读取到完整的数据，等待:" + len + " rollbackIndex:" + rollbackIndex);
            return false;
        }
        return true;
    }

    /**
     *  对命令进行解码处理
     *
     * @param in {@link ByteBuf}
     * @param out {@link RemoteCommand}
     * @throws Exception 解码异常
     */
    @Deprecated
    public static void decode(ByteBuf in, List<Object> out) throws Exception {

        if (in.readableBytes() > 102400) {
            PSLogger.error("太多的消息：" + in.readableBytes());
            in.skipBytes(in.readableBytes());
            return;
        }

        if (in.readableBytes() < CommandProtocol.PROTOCOL_MIN_LENGTH) {
            return; // no enough data
        }

        // 解决TCP粘包问题
        int beginReadIndex = in.readerIndex();
        in.markReaderIndex();

        RemoteCommand remoteCommand = new RemoteCommand();
        if (!ensureLen(in, 1 + 1 + 1 + 1 + 2 + 4 + 4 + 8 + 8, beginReadIndex)) {
            return;
        }

        remoteCommand.setTag(in.readByte()).setClientTag(in.readByte())
                .setVersion(in.readByte()).setProtocolType(in.readByte())
                .setTimeOut(in.readShort()).setCallSeq(in.readInt())
                .setContextId(in.readInt()).setTimeStamp(in.readLong()).setCost(in.readLong());

        if (!ensureLen(in, 2 + 2, beginReadIndex)) {
            return;
        }

        short commandLen = in.readShort();
        short pc = in.readShort();
        byte[] buf = new byte[commandLen];

        if (!ensureLen(in, commandLen, beginReadIndex)) {
            return;
        }

        in.readBytes(buf);
        remoteCommand.setCommandName(new String(buf, "UTF-8"));
        for (short i = 0; i < pc; i ++) {
            if (!ensureLen(in, 4, beginReadIndex)) {
                return;
            }

            int len = in.readInt();
            buf = new byte[len];

            if (!ensureLen(in, len, beginReadIndex)) {
                return;
            }

            in.readBytes(buf);
            String key = new String(buf, "UTF-8");

            if (!ensureLen(in, 1, beginReadIndex)) {
                return;
            }

            byte type = in.readByte();
            switch (type) {
                case 1:
                    handleMap(in, remoteCommand, key, beginReadIndex);
                    break;
                case 2:
                    handleList(in, remoteCommand, key);
                    break;
                case 3:
                    handleString(in, remoteCommand, key, beginReadIndex);
                    break;
                default:
                    throw new UnsupportedOperationException("不支持的编码类型");
            }
        }
        // decode done

        PSLogger.error("decode a message:" + remoteCommand);

        out.add(remoteCommand);
    }

    /**
     *  处理map
     *
     * @param in
     * @param remoteCommand
     * @throws Exception
     */
    private static void handleMap(ByteBuf in, RemoteCommand remoteCommand, String key, int startIndex) throws Exception {
        if (!ensureLen(in, 4, startIndex)) {
            return;
        }
        int size = in.readInt();
        Map<String, byte[]> params = new HashMap<>();
        for (int i = 0; i < size; i ++) {
            if (!ensureLen(in, 4, startIndex)) {
                return;
            }
            int len = in.readInt();

            if (!ensureLen(in, len, startIndex)) {
                return;
            }
            byte[] buf = new byte[len];
            in.readBytes(buf);
            String k = new String(buf, "UTF-8");

            if (!ensureLen(in, 4, startIndex)) {
                return;
            }

            len = in.readInt();

            if (!ensureLen(in, len, startIndex)) {
                return;
            }

            buf = new byte[len];
            in.readBytes(buf);
            params.put(k, buf);
        }
        remoteCommand.addParam(key, params);
    }

    /**
     *  处理String
     *
     * @param in
     * @param remoteCommand
     * @throws Exception
     */
    private static void handleString(ByteBuf in, RemoteCommand remoteCommand, String key, int startIndex) throws Exception {
        if (!ensureLen(in, 4, startIndex)) {
            return;
        }

        int len = in.readInt();

        if (!ensureLen(in, len, startIndex)) {
            return;
        }

        byte[] buf = new byte[len];
        in.readBytes(buf);
        String val = new String(buf, "UTF-8");
        remoteCommand.addParam(key, val);
    }

    /**
     *  处理list
     *
     * @param in
     * @param remoteCommand
     * @throws Exception
     */
    private static void handleList(ByteBuf in, RemoteCommand remoteCommand, String key) throws Exception {
        throw new UnsupportedOperationException("暂不支持list");
    }

}
