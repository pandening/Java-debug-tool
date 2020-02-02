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

import io.javadebug.core.utils.UTILS;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.util.List;

public enum  ShellChannelHttpServer {
    SHELL_CHANNEL_HTTP_SERVER;

    /**
     *  arg[0] => server ip
     *  arg[1] => server port
     *  arg[2] => http listen port
     *
     * @param args the args
     */
    public static void main(String[] args) {

//        if (args == null || args.length != 2) {
//            throw new IllegalArgumentException("invalid params, could not set up the http server ...");
//        }

//        String ip = args[0];
//        String port = args[1];

        String ip = "127.0.0.1";
        String port = "11234";
        String listenPort = "10234";

        int portNumber = UTILS.safeParseInt(port, -1);
        if (portNumber <= 0) {
            throw new IllegalArgumentException("invalid port number :" + port);
        }
        int listenOn = UTILS.safeParseInt(listenPort, -1);
        if (listenOn <= 0) {
            throw new IllegalArgumentException("invalid http listen port : " + listenPort);
        }

        // set up the http server on 10234
        SHELL_CHANNEL_HTTP_SERVER.setup(ip, portNumber, listenOn);

    }

    private static JavaDebugToolClientDriver.JavaDebugToolStandardClient javaDebugToolStandardClient;

    public void setup(String serverIp, int serverPort, int listenOn) {

        // get the standard console
        javaDebugToolStandardClient = JavaDebugToolClientDriver.client(serverIp, serverPort);

        RxNetty.<ByteBuf, ByteBuf>newHttpServerBuilder(listenOn, (request, response) -> {
            try {
                return handleRequest(request, response);
            } catch (Throwable e) {
                System.err.println("Server => Error [" + request.getPath() + "] => " + e);
                response.setStatus(HttpResponseStatus.BAD_REQUEST);
                return response.writeStringAndFlush("Error 500: Bad Request\n" + e.getMessage() + '\n');
            }
        }).build().startAndWait();
    }

    private static Observable<Void> handleRequest(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {

        if (request.getUri().startsWith("/hello")) {
            return response.writeStringAndFlush("Hello world!");
        }

        if (!request.getUri().startsWith("/java-debug-tool/shell/http/")) {
            return response.writeStringAndFlush("fatal error by invoking with error uri:" + request.getUri());
        }

        if (!request.getHttpMethod().equals(HttpMethod.GET)) {
            return response.writeStringAndFlush("fatal error by invoking with error http method:" + request.getHttpMethod().name());
        }

        // using seq to ensure the c-s loop
        List<String> callSeq = request.getQueryParameters().get("seq");
        if (callSeq == null || callSeq.isEmpty()) {
            return response.writeStringAndFlush("fatal error by invoking without call seq");
        }
        long seq = UTILS.safeParseLong(callSeq.get(0), -1);
        if (seq <= 0) {
            return response.writeStringAndFlush("fatal error by invoking with error call seq :" + callSeq);
        }

        // the command
        List<String> command = request.getQueryParameters().get("command");
        if (command == null || command.isEmpty()) {
            return response.writeStringAndFlush("fatal error by invoking without command");
        }

        String cmd = "";
        for (String c : command) {
            if (!UTILS.isNullOrEmpty(c)) {
                cmd = c;
                break;
            }
        }
        if (UTILS.isNullOrEmpty(cmd)) {
            return response.writeStringAndFlush("fatal error by invoking with empty command");
        }

        ShellReq shellReq = new ShellReq();
        shellReq.seq = seq;
        shellReq.cmd = cmd;

        // handle and response
        return Observable.create((Subscriber<? super ByteBuf> subscriber) -> {
            Scheduler.Worker worker = Schedulers.io().createWorker();
            subscriber.add(worker);
            worker.schedule(() -> {
                // set the resp status
                response.setStatus(HttpResponseStatus.OK);

                // application/json;charset=UTF-8
                response.getHeaders()
                        .setHeader(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=utf-8");

                // send and wait the resp from server
                String resp = javaDebugToolStandardClient.sendToServer(shellReq.cmd, shellReq.seq);

                // write to buffer
                ByteBuf buffer = Unpooled.buffer();
                buffer.writeBytes(resp.getBytes());

                // on next
                subscriber.onNext(buffer);
                subscriber.onCompleted();
            });
        }).flatMap(response::writeAndFlush).doOnTerminate(response::close);
    }

    static class ShellReq {
        long seq;
        String cmd;
    }

}