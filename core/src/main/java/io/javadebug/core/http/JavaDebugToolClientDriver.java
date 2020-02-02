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


package io.javadebug.core.http;

import io.javadebug.core.CommandRequestHandler;
import io.javadebug.core.console.ReadShellCommandSource;
import io.javadebug.core.console.WriteShellCommandSink;
import io.javadebug.core.handler.ClientCommandRequestHandler;
import io.javadebug.core.transport.NettyTransportClient;
import io.javadebug.core.transport.RemoteCommand;

import java.util.concurrent.atomic.AtomicLong;

/**
 *   this class play the role of driving the java-debug-tool's console, something
 *   like an interface to invoke the Server by standard console.
 *   the driver must ensure the console is healthy on work.
 *
 */
public class JavaDebugToolClientDriver {

    interface JavaDebugToolStandardClient {

        /**
         *  send the command  {@code command} to debug server, and get the
         *  response {@link RemoteCommand}
         *
         * @param seq the call seq
         * @param command the input command
         * @return the server's response
         */
        String sendToServer(String command, long seq);

    }

    // the standard remote console
    private static final NettyTransportClient nettyTransportClient = new NettyTransportClient();

    private static JavaDebugToolStandardClient javaDebugToolStandardClient = null;

    /**
     *  this method can get the standard console like {@link NettyTransportClient}
     *  or like {@link io.javadebug.core.transport.NettyTransportClusterClient}
     *
     *  @param ip  the target server ip
     *  @param port  the target server port
     *
     * @return the console {@link JavaDebugToolStandardClient}
     *
     * @see io.javadebug.core.console.CommandSource the input
     * @see io.javadebug.core.console.CommandSink   the output
     */
    public synchronized static JavaDebugToolStandardClient client(String ip, int port) {

        if (javaDebugToolStandardClient != null) {
            return javaDebugToolStandardClient;
        }

//        if (UTILS.isNullOrEmpty(ip) || (port < 1000 || port > 65535)) {
//            throw new IllegalArgumentException("fatal error with invalid params, please check the ip / port");
//        }

        // config the console

        final ReadShellCommandSource commandSource = new ReadShellCommandSource();
        final WriteShellCommandSink commandSink = new WriteShellCommandSink();

        nettyTransportClient.setIP(ip).setPort(port).setCommandSource(commandSource)
                .addCommandSink(commandSink).silence(true).asyncInit();

        final AtomicLong seqInc = new AtomicLong(1);

        // command source
//        Subject<String, String> subject = ReplaySubject.create();
//        subject.subscribe(commandSource::onCommand);

        javaDebugToolStandardClient = new JavaDebugToolStandardClient() {
            @Override
            public String sendToServer(String command, long seq) {

                // check seq
                if (seqInc.get() != seq) {
                    return "fatal error with error seq : " + seq;
                }

                // produce a command
                commandSource.onCommand(command);

                if (!seqInc.compareAndSet(seq, seq + 1)) {
                    return "fatal error with invalid seq, maybe multi console run in same time !";
                }

                // get the resp
                return commandSink.getResp();
            }
        };

        return javaDebugToolStandardClient;
    }


    public static void main(String[] args) {

        String ip = "127.0.0.1";
        int port = 11234;
        CommandRequestHandler commandRequestHandler = new ClientCommandRequestHandler();

        NettyTransportClient nettyTransportClient = new NettyTransportClient(ip, port, commandRequestHandler);

        nettyTransportClient.init();

    }

}