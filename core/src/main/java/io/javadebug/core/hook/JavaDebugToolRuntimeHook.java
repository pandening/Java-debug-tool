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


package io.javadebug.core.hook;

import io.javadebug.core.exception.CommandExecuteWithStageException;
import io.javadebug.core.transport.RemoteCommand;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *  this runtime hook work on Client side, the Server side is not available now;
 *
 *  {@link io.javadebug.core.JavaDebugClientLauncher}
 *
 *  NOTICE:
 *  !!!
 *  DO NOT SUPPORT {@link io.javadebug.core.JavaDebugClusterClientLauncher}
 *
 */
final public class JavaDebugToolRuntimeHook {

    private static final List<HookOperator> hookOperators = new CopyOnWriteArrayList<>();

    private static class Worker {

        public static <T, R> void doRegister(HookOperator<T, R> hookOperator) {
            if (hookOperator != null) {
                hookOperators.add(hookOperator);
            }
        }

        public static <T, R> void doUnRegister(HookOperator<T, R> hookOperator) {
            if (hookOperator != null) {
                hookOperators.remove(hookOperator);
            }
        }

        @SuppressWarnings("unchecked")
        public static <T> void doNotice(T data, RuntimeStage stage) {
            for (HookOperator operator : hookOperators) {
                if (operator.isInterest(stage)) {
                    operator.apply(data);
                }
            }
        }

    }

    public static class UnRegister {

        public static <T, R> void unRegister(HookOperator<T, R> hookOperator) {
            Worker.doUnRegister(hookOperator);
        }

    }

    public static class Register {

        public static <R> void onCommandInput(HookOperator<String, R> hookOperator) {
            Worker.doRegister(hookOperator);
        }

        public static <R> void onCommandCreate(HookOperator<RemoteCommand, R> hookOperator) {
            Worker.doRegister(hookOperator);
        }

        public static <R> void onCommandSend(HookOperator<RemoteCommand, R> hookOperator) {
            Worker.doRegister(hookOperator);
        }

        public static <R> void onCommendResp(HookOperator<RemoteCommand, R> hookOperator) {
            Worker.doRegister(hookOperator);
        }

        public static <R> void onCommandToUI(HookOperator<String, R> hookOperator) {
            Worker.doRegister(hookOperator);
        }

        public static <R> void onExecuteError(HookOperator<CommandExecuteWithStageException, R> hookOperator) {
            Worker.doRegister(hookOperator);
        }

    }

    public static class Notice {

        public static void onCommandInput(String command, RuntimeStage stage) {
            Worker.doNotice(command, stage);
        }

        public static void onCommandCreate(RemoteCommand remoteCommand, RuntimeStage stage) {
            Worker.doNotice(remoteCommand, stage);
        }

        public static void onCommandSend(RemoteCommand remoteCommand, RuntimeStage stage) {
            Worker.doNotice(remoteCommand, stage);
        }

        public static void onCommendResp(RemoteCommand remoteCommand, RuntimeStage stage) {
            Worker.doNotice(remoteCommand, stage);
        }

        public static void onCommandToUI(String show, RuntimeStage stage) {
            Worker.doNotice(show, stage);
        }

        public static void onExecuteError(CommandExecuteWithStageException e, RuntimeStage stage) {
            Worker.doNotice(e, stage);
        }

    }

}
