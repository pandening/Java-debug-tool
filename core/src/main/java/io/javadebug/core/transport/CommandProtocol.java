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

/**
 * V1 Request command protocol
 *
 * 0     1     2     3     4           6    7      8          10     11     12          14         16
 * +-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+------+-----+-----+-----+-----+
 * |  v  | t1  |   t2      |      callSeqId        |       contextId        | commandLen |    pc    |
 * +-----------+-----------+-----------+-----------+-----------+------------+-----------+-----------+
 * |  command name    ...........................................................................   |
 * |  param length   |   param content                                                              |
 * |  param length   |   param content                                                              |
 * +-----------+-----------+-----------+-----------+                                                +
 * +                                                                                                +
 * |                               ... ...                                                          |
 * +------------------------------------------------------------------------------------------------+
 *
 * v             : the protocol version
 * t1            : the protocol type 0: request, 1: response
 * t2            ：timeout (sec)
 * callSeqId     : record the round from console to server
 * contextId     : the console's unique id
 * commandLen    : the command's name length
 * pc            : command params count
 * command name  : the command name, read by commandLen
 * param length  : each param length
 * param content : the param context, read by param length
 *
 * V2 Request command protocol layout
 *
 *
 * 0     1     2     3     4           6    7      8          10     11     12          14         16
 * +-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+------+-----+-----+-----+-----+
 * |magic|    CRC32              |  protocol body length |                                          |
 * +-----------+-----------+-----------+-----------+-----------+------------+-----------+-----------+
 * |  param length   |   param content                                                              |
 * |  param length   |   param content                                                              |
 * |                                                                                                |
 * +------------------------------------------------------------------------------------------------+
 *
 *
 * 0     1     2     3     4           6    7      8     9
 * +-----+-----+-----+-----+-----+-----+-----+-----+-----+
 * |magic|    CRC32              |  protocol body length |
 * +-----------+-----------+-----------+-----------+-----|
 * |   key length          |   key content               |
 * |  value length         |   value content             |
 * |              .. protocol body ..                    |
 * +-----------------------------------------------------+
 *
 *
 *
 * @author Hu Jian
 */
public class CommandProtocol {

    public static final int PROTOCOL_MIN_LENGTH = 16;

    public static final byte COMMAND_REQ_PROTOCOL_TYPE = (byte) 0;

    public static final byte COMMAND_RES_PROTOCOL_TYPE = (byte) 1;

    public static final byte CONNECTION_TYPE_NORMAL = (byte)0;

    public static final byte CONNECTION_TYPE_MASTER = (byte)1;

    public static final byte CONNECTION_TAG_ZERO = (byte) 0;

    public static final byte CONNECTION_TAG_ONE = (byte) 1;

    public static final byte CURRENT_SERVER_VERSION = (byte) 1;


}
