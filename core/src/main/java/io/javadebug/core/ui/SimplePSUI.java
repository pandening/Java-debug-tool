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


package io.javadebug.core.ui;

import io.javadebug.core.utils.UTILS;
import io.javadebug.core.transport.RemoteCommand;

import java.util.Date;

import static io.javadebug.core.transport.CommandProtocol.COMMAND_RES_PROTOCOL_TYPE;

/**
 * Created on 2019/4/22 16:33.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class SimplePSUI implements UI {

    private static final String LINE_SHOW               =       "---------------------------------------------------------------------------------------------";

    private static final String COMMAND                 =       "Command            \t：";

    private static final String ERROR_CODE              =       "ErrorCode          \t：";

    private static final String ERROR_MSG               =       "ErrorMsg           \t：";

    private static final String COST                    =       "CommandCost        \t：";

    private static final String STW_COST                =       "STW_Cost           \t：";

    private static final String CONTEXT_ID              =       "ClientId           \t：";

    private static final String CALL_SEQ                =       "Round              \t：";

    private static final String CLIENT_TAG              =       "ClientType         \t：";

    private static final String PROTOCOL_VERSION        =       "Version            \t：";

    private static final String EXECUTE_TIME            =       "Time:              \t: ";

    private static final String COMMAND_ERROR_CODE_KEY  =       "$back-errorCode";

    private static final String COMMAND_ERROR_MSG_KEY   =       "$back-errorMsg";

    private static final String COMMON_REMOTE_RESP_KEY  =       "$back-data";

    private static final int ESCAPE_LENGTH = 30;

    /**
     * 每一个命令返回结果都是一种通用模型，所以可以做一个通用UI，如果需要实现
     * 个性化展示样式，可以实现该方法
     *
     * @param remoteCommand {@link RemoteCommand}
     * @return 展示字符串
     */
    @Override
    public String toUI(RemoteCommand remoteCommand) {
        if (remoteCommand == null) {
            return "error:响应结果为空!\n";
        }
        // error type
        if (remoteCommand.getProtocolType() != COMMAND_RES_PROTOCOL_TYPE) {
            remoteCommand.setProtocolType(COMMAND_RES_PROTOCOL_TYPE);
            return "error:不合法的协议状态:" + remoteCommand + "\n";
        }

        if (remoteCommand.getCallSeq() <= 0) {
            return "error:不合法的seq:" + remoteCommand + "\n";
        }

        if (remoteCommand.getContextId() < 0) {
            return "error:客户端还没有获取到ContextId:" + remoteCommand + "\n";
        }

        StringBuilder sb = new StringBuilder();

        String titleBar = remoteCommand.getParam("$common-show-set-no-title");
        if (!"true".equals(titleBar)) {
            // 是否要展示出头
            /// header
            sb.append(LINE_SHOW).append("\n");

            //// command name
            append(COMMAND, remoteCommand.getCommandName(), sb);

            //// error info
            String errorCode = remoteCommand.getParam(COMMAND_ERROR_CODE_KEY);
            if (!UTILS.isNullOrEmpty(errorCode) && !"0".equals(errorCode)) {
                String errorMsg = remoteCommand.getParam(COMMAND_ERROR_MSG_KEY);
                if (UTILS.isNullOrEmpty(errorMsg)) {
                    errorMsg = "服务端执行错误";
                }
                append(ERROR_CODE, errorCode, sb);

                String[] splitErrors = errorMsg.split("\n");
                StringBuilder errorSb = new StringBuilder();
                errorSb.append(splitErrors[0]).append("\n");


                StringBuilder escapeSpaceStr = new StringBuilder();
                for (int i = 0; i < ESCAPE_LENGTH; i ++) {
                    escapeSpaceStr.append(" ");
                }

                if (splitErrors.length > 1) {
                    for (int i = 1; i < splitErrors.length; i ++) {
                        errorSb.append(escapeSpaceStr.toString()).append(splitErrors[i]).append("\n");
                    }
                }
                append(ERROR_MSG, errorSb.toString(), sb);
            }

            //// call seq
            int callSeq = remoteCommand.getCallSeq();
            append(CALL_SEQ, "" + callSeq, sb);

            //// context id
            int contextId = remoteCommand.getContextId();
            append(CONTEXT_ID, "" + contextId, sb);

            //// console type
            int clientTag = remoteCommand.getClientTag();
            append(CLIENT_TAG, "console:" + clientTag, sb);

            //// protocol version
            int pv = remoteCommand.getVersion();
            append(PROTOCOL_VERSION, "version:" + pv, sb);

            //// cost
            long cost = System.currentTimeMillis() - remoteCommand.getTimestamp();
            append(COST, cost + " (ms)", sb);

            int stwCost = remoteCommand.getStwCost();
            append(STW_COST, stwCost + " (ms)", sb);

            // time
            append(EXECUTE_TIME, new Date().toString(), sb);
        }

        /// resp
        String resp = remoteCommand.getParam(COMMON_REMOTE_RESP_KEY);
        if (!UTILS.isNullOrEmpty(resp)) {
            append("", resp, sb);
        }
        /// bottom
        sb.append(LINE_SHOW).append("\n");

        return sb.toString();
    }

    /**
     *  这个方法用于实现向输出sb中追加内容
     *
     * @param key 可以是啥
     * @param content 内容是啥
     * @param sb sb
     */
    private void append(String key, String content, StringBuilder sb) {
        if (UTILS.isNullOrEmpty(content)) {
            return;
        }
        if (UTILS.isNullOrEmpty(key)) {
            sb.append(LINE_SHOW).append("\n").append(content).append("\n");
            return;
        }
        sb.append(key).append(content).append("\n");
    }
}
