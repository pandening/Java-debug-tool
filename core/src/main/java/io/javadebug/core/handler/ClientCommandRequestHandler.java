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


package io.javadebug.core.handler;

import io.javadebug.core.CommandInputHandler;
import io.javadebug.core.command.ThreadBlockHoundCommand;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.exception.CommandNotFindException;
import io.javadebug.core.transport.RemoteCommand;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 2019/4/20 16:54.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class ClientCommandRequestHandler extends BaseCommandRequestHandler {

    /// --------------------------------------------
    /// 某些情况下，希望可以快速重复上次发送的命令，那么这个字段就变得很有用了
    /// --------------------------------------------
    private static String lastCommandStr;

    /// ------------------------------------------
    /// 如果命名输入不合法，应该抛出异常，而这个常量用于标记一次命令输入
    /// 处理以失败告终，需要重新输入，鉴于本系统严格的
    /// C -> S -> C -> S ...
    /// 的交互套路，需要十分谨慎的处理，否则可能造成客户端"假死"
    /// ------------------------------------------
    private static final RemoteCommand ERROR_EMPTY_REMOTE_COMMAND = null;

    /// ------------------------------------------
    /// 持有当前支持命令的输入处理handler
    ///
    /// ------------------------------------------
    private static final Map<String, CommandInputHandler> COMMAND_INPUT_HANDLER_MAP = new HashMap<>();

    static {
        COMMAND_INPUT_HANDLER_MAP.put("findClass", FindClassCommandInputHandler.FIND_CLASS_COMMAND_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("fc", FindClassCommandInputHandler.FIND_CLASS_COMMAND_INPUT_HANDLER);

        COMMAND_INPUT_HANDLER_MAP.put("help", HelpCommandInputHandler.HELP_COMMAND_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("h", HelpCommandInputHandler.HELP_COMMAND_INPUT_HANDLER);

        COMMAND_INPUT_HANDLER_MAP.put("ct", CpuTimeCommandInputHandler.CPU_TIME_COMMAND_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("cputime", CpuTimeCommandInputHandler.CPU_TIME_COMMAND_INPUT_HANDLER);

        COMMAND_INPUT_HANDLER_MAP.put("th", ThreadCommandInputHandler.THREAD_COMMAND_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("thread", ThreadCommandInputHandler.THREAD_COMMAND_INPUT_HANDLER);

        COMMAND_INPUT_HANDLER_MAP.put("exit", ExitCommandInputHandler.EXIT_COMMAND_INPUT_HANDLER);

        COMMAND_INPUT_HANDLER_MAP.put("redefine", RedefineClassInputHandler.REDEFINE_CLASS_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("rdf", RedefineClassInputHandler.REDEFINE_CLASS_INPUT_HANDLER);

        COMMAND_INPUT_HANDLER_MAP.put("rollback", RollbackClassInputHandler.ROLLBACK_CLASS_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("back", RollbackClassInputHandler.ROLLBACK_CLASS_INPUT_HANDLER);

        COMMAND_INPUT_HANDLER_MAP.put("lockClass", LockClassByteInputHandler.LOCK_CLASS_BYTE_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("lock", LockClassByteInputHandler.LOCK_CLASS_BYTE_INPUT_HANDLER);

        COMMAND_INPUT_HANDLER_MAP.put("option", OptionInputHandler.OPTION_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("set", OptionInputHandler.OPTION_INPUT_HANDLER);

        COMMAND_INPUT_HANDLER_MAP.put("info", AgentInfoInputHandler.AGENT_INFO_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("if", AgentInfoInputHandler.AGENT_INFO_INPUT_HANDLER);

        COMMAND_INPUT_HANDLER_MAP.put("list", ListInputHandler.LIST_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("all", ListInputHandler.LIST_INPUT_HANDLER);

        COMMAND_INPUT_HANDLER_MAP.put("tb", ThreadBlockHoundInputHandler.THREAD_BLOCK_HOUND_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("block", ThreadBlockHoundInputHandler.THREAD_BLOCK_HOUND_INPUT_HANDLER);

        COMMAND_INPUT_HANDLER_MAP.put("monitor", MonitorInputHandler.MONITOR_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("collect", MonitorInputHandler.MONITOR_INPUT_HANDLER);

        COMMAND_INPUT_HANDLER_MAP.put("alive", AliveInputHandler.ALIVE_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("check", AliveInputHandler.ALIVE_INPUT_HANDLER);

        COMMAND_INPUT_HANDLER_MAP.put("trace", MethodTraceInputHandler.METHOD_TRACE_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("methodTrace", MethodTraceInputHandler.METHOD_TRACE_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("mTrace", MethodTraceInputHandler.METHOD_TRACE_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("mt", MethodTraceInputHandler.METHOD_TRACE_INPUT_HANDLER);

        /// 重发上次的命令
        COMMAND_INPUT_HANDLER_MAP.put("p", RepeatSendLastCommandInputHandler.REPEAT_SEND_LAST_COMMAND_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("r", RepeatSendLastCommandInputHandler.REPEAT_SEND_LAST_COMMAND_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("s", RepeatSendLastCommandInputHandler.REPEAT_SEND_LAST_COMMAND_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("go", RepeatSendLastCommandInputHandler.REPEAT_SEND_LAST_COMMAND_INPUT_HANDLER);
        COMMAND_INPUT_HANDLER_MAP.put("last", RepeatSendLastCommandInputHandler.REPEAT_SEND_LAST_COMMAND_INPUT_HANDLER);

    }

    /**
     *  获取到命令输入处理handler
     *
     * @param cmd 命令
     * @return {@link CommandInputHandler}
     * @throws Exception 找不到抛出异常
     */
    private CommandInputHandler getInputHandler(String cmd) throws Exception {
        if (UTILS.isNullOrEmpty(cmd)) {
            throw new NullPointerException("命令为null");
        }
        CommandInputHandler commandInputHandler = COMMAND_INPUT_HANDLER_MAP.get(cmd);
        if (commandInputHandler == null) {
            throw new CommandNotFindException("找不到处理命令输入的handler:" + cmd);
        }
        return commandInputHandler;
    }

    /**
     * 这个方法用于实现将输入变成命令
     *
     * @param cmd 这是原始命令
     * @return {@link RemoteCommand} 转换成一个命令
     */
    @Override
    public RemoteCommand toCommand(String cmd, RemoteCommand remoteCommand) {

        if (UTILS.isNullOrEmpty(cmd) || remoteCommand == null) {
            return ERROR_EMPTY_REMOTE_COMMAND;
        }
        String[] params = cmd.split(" ");
        if (params.length == 0) {
            return ERROR_EMPTY_REMOTE_COMMAND;
        }

        String cmdName = params[0];
        try {
            CommandInputHandler commandInputHandler = getInputHandler(cmdName);
            RemoteCommand command = commandInputHandler.toCommand(params, remoteCommand);
            if (command == null) {
                PSLogger.error("无法转换成命令协议:" + cmdName);
                return ERROR_EMPTY_REMOTE_COMMAND;
            }

            // record
            String tag = command.getParam("$special-tag-is-copy");
            if (UTILS.isNullOrEmpty(tag) || "false".equals(tag)) {

                // record it
                lastCommandStr = cmd;
            }

            return command;
        } catch (Exception e) {
            PSLogger.error("无法处理命令输入:" + e);
            return ERROR_EMPTY_REMOTE_COMMAND;
        }
    }


    //---------------------------------------------------------------
    ///
    ///   命令输入处理handler区域
    ///
    //---------------------------------------------------------------

    enum RepeatSendLastCommandInputHandler implements CommandInputHandler {
        REPEAT_SEND_LAST_COMMAND_INPUT_HANDLER
        ;

        /**
         * 这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
         * 是命令在客户端输入之后的处理
         *
         * @param args   命令参数
         * @param origin 客户端持有的协议对象
         * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
         * @throws Exception 处理异常
         */
        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            if (UTILS.isNullOrEmpty(lastCommandStr)) {
                throw new RuntimeException("没有执行过任何命令，请开始命令输入");
            }

            String[] params = lastCommandStr.split(" ");

            // create the command
            CommandInputHandler commandInputHandler = COMMAND_INPUT_HANDLER_MAP.get(params[0]);
            if (commandInputHandler == null) {
                throw new CommandNotFindException("找不到处理命令输入的handler:" + params[0]);
            }

            RemoteCommand lastCommand = commandInputHandler.toCommand(params, origin);

            // tag
            lastCommand.addParam("$special-tag-is-copy", "true");

            return lastCommand;
        }
    }

    enum MethodTraceInputHandler implements CommandInputHandler {
        METHOD_TRACE_INPUT_HANDLER;

        /**
         * 这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
         * 是命令在客户端输入之后的处理
         *
         * @param args   命令参数
         * @param origin 客户端持有的协议对象
         * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
         * @throws Exception 处理异常
         */
        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            final OptionParser parser = new OptionParser();
            parser.accepts("c").withOptionalArg().ofType(String.class);
            parser.accepts("m").withOptionalArg().ofType(String.class);
            parser.accepts("d").withOptionalArg().ofType(String.class);
            parser.accepts("l").withOptionalArg().ofType(String.class);
            parser.accepts("t").withOptionalArg().ofType(String.class);
            parser.accepts("n").withOptionalArg().ofType(String.class);
            parser.accepts("u").withOptionalArg().ofType(String.class);
            parser.accepts("i").withOptionalArg().ofType(String.class);
            parser.accepts("time").withOptionalArg().ofType(String.class);
            parser.accepts("s").withOptionalArg().ofType(String.class);
            parser.accepts("w").withOptionalArg().ofType(String.class);
            parser.accepts("e").withOptionalArg().ofType(String.class);
            parser.accepts("timeout").withOptionalArg().ofType(String.class);
            //parser.accepts("evil").withOptionalArg();
            parser.accepts("claw").withOptionalArg().ofType(String.class);
            parser.accepts("tl").withOptionalArg().ofType(String.class);
            parser.accepts("te").withOptionalArg().ofType(String.class);

            final OptionSet optionSet = parser.parse(args);

            String cls, method, timeoutVal = "", desc = "", lop = "", top = "", nop = "", uop = "", iop = "",
                    timeOption = "", sop = "", wop = "", eop = "", evil = "", claw = "", tlo = "", teo = "";
            if (optionSet.has("c")) {
                cls = (String) optionSet.valueOf("c");
            } else {
                return ERROR_EMPTY_REMOTE_COMMAND;
            }

            if (optionSet.has("m")) {
                method = (String) optionSet.valueOf("m");
            } else {
                return ERROR_EMPTY_REMOTE_COMMAND;
            }

            if (optionSet.has("d")) {
                desc = (String) optionSet.valueOf("d");
            }

            if (optionSet.has("l")) {
                lop = (String) optionSet.valueOf("l");
            }

            if (optionSet.has("t")) {
                top = (String) optionSet.valueOf("t");
            }

            if (optionSet.has("n")) {
                nop = (String) optionSet.valueOf("n");
            }

            if (optionSet.has("u")) {
                uop = (String) optionSet.valueOf("u");
            }

            if (optionSet.has("i")) {
                iop = (String) optionSet.valueOf("i");
            }

            if (optionSet.has("time")) {
                timeOption = (String) optionSet.valueOf("time");
            }

            if (optionSet.has("s")) {
                sop = (String) optionSet.valueOf("s");
            }

            if (optionSet.has("w")) {
                wop = (String) optionSet.valueOf("w");
            }

            if (optionSet.has("e")) {
                eop = (String) optionSet.valueOf("e");
            }

            if (optionSet.has("timeout")) {
                timeoutVal = (String) optionSet.valueOf("timeout");
            }

            //if (optionSet.has("evil")) {
            //    evil = "true";
            //}

            if (optionSet.has("claw")) {
                claw = (String) optionSet.valueOf("claw");
            }

            if (optionSet.has("tl")) {
                tlo = (String) optionSet.valueOf("tl");
            }

            if (optionSet.has("te")) {
                teo = (String) optionSet.valueOf("te");
            }

            return origin.clearShit().setCommandName("mt")
                           .addParam("$forward-trace-cls", cls)
                           .addParam("$forward-trace-method", method)
                           .addParam("$forward-trace-desc", desc)
                           .addParam("$forward-trace-option-l", lop)
                           .addParam("$forward-trace-option-t", top)
                           .addParam("$forward-trace-option-u", uop)
                           .addParam("$forward-trace-option-i", iop)
                           .addParam("$forward-trace-option-n", nop)
                           .addParam("$forward-trace-option-time", timeOption)
                           .addParam("$forward-trace-option-s", sop)
                           .addParam("$forward-trace-option-w", wop)
                           .addParam("$forward-trace-option-e", eop)
                           .addParam("$forward-timeout-check-tag", timeoutVal)
                           .addParam("$forward-common-tl", tlo)
                           .addParam("$forward-common-te", teo)
                           //.addParam("$forward-trace-evil-mode", evil)
                           .addParam("$forward-trace-option-clawScript", claw)
                    ;
        }

    }

    enum AgentInfoInputHandler implements CommandInputHandler {
        AGENT_INFO_INPUT_HANDLER
        ;

        /**
         * 这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
         * 是命令在客户端输入之后的处理
         *
         * @param args   命令参数
         * @param origin 客户端持有的协议对象
         * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
         * @throws Exception 处理异常
         */
        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            final OptionParser parser = new OptionParser();
            parser.accepts("p").withOptionalArg().ofType(String.class);

            final OptionSet optionSet = parser.parse(args);

            if (!optionSet.has("p")) {
                return ERROR_EMPTY_REMOTE_COMMAND;
            }
            String p = (String) optionSet.valueOf("p");

            return origin.clearShit().setCommandName("if").addParam("$forward-info-p", p);
        }
    }

    enum AliveInputHandler implements CommandInputHandler {
        ALIVE_INPUT_HANDLER
        ;

        /**
         * 这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
         * 是命令在客户端输入之后的处理
         *
         * @param args   命令参数
         * @param origin 客户端持有的协议对象
         * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
         * @throws Exception 处理异常
         */
        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            return origin.clearShit().setCommandName("alive");
        }
    }

    enum MonitorInputHandler implements CommandInputHandler {
        MONITOR_INPUT_HANDLER
        ;

        /**
         * 这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
         * 是命令在客户端输入之后的处理
         *
         * @param args   命令参数
         * @param origin 客户端持有的协议对象
         * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
         * @throws Exception 处理异常
         */
        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            final OptionParser parser = new OptionParser();
            parser.accepts("p").withOptionalArg().ofType(String.class);
            parser.accepts("t").withOptionalArg().ofType(String.class);
            parser.accepts("i").withOptionalArg().ofType(String.class);

            final OptionSet optionSet = parser.parse(args);

            String port = "", type = "", iop = "";
            if (optionSet.has("p")) {
                port = (String) optionSet.valueOf("p");
            }

            if (optionSet.has("t")) {
                type = (String) optionSet.valueOf("t");
            }

            if (optionSet.has("i")) {
                iop = (String) optionSet.valueOf("i");
            }

            return origin.clearShit().setCommandName("monitor")
                           .addParam("$forward-monitor-port", port)
                           .addParam("$forward-monitor-type", type)
                           .addParam("$forward-monitor-interval", iop)
                    ;
        }
    }

    enum ThreadBlockHoundInputHandler implements CommandInputHandler {
        THREAD_BLOCK_HOUND_INPUT_HANDLER
        ;

        /**
         * 这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
         * 是命令在客户端输入之后的处理
         *
         * @param args   命令参数
         * @param origin 客户端持有的协议对象
         * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
         * @throws Exception 处理异常
         */
        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            final OptionParser parser = new OptionParser();
            parser.accepts("name").withOptionalArg().ofType(String.class);
            parser.accepts("re").withOptionalArg().ofType(String.class);

            final OptionSet optionSet = parser.parse(args);

            String name = "", reEnhance = "";

            if (optionSet.has("name")) {
                name = (String) optionSet.valueOf("name");
            }

            if (optionSet.has("re")) {
                reEnhance = (String) optionSet.valueOf("re");
            }

            return origin.clearShit().addParam("$forward-tb-name", name)
                           .addParam("$forward-tb-re", reEnhance)
                           .setCommandName("tb");
        }
    }

    enum ListInputHandler implements CommandInputHandler {
        LIST_INPUT_HANDLER
        ;

        /**
         * 这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
         * 是命令在客户端输入之后的处理
         *
         * @param args   命令参数
         * @param origin 客户端持有的协议对象
         * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
         * @throws Exception 处理异常
         */
        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            return origin.clearShit().setCommandName("list");
        }
    }

    enum OptionInputHandler implements CommandInputHandler {
        OPTION_INPUT_HANDLER
        ;

        /**
         * 这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
         * 是命令在客户端输入之后的处理
         *
         * @param args   命令参数
         * @param origin 客户端持有的协议对象
         * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
         * @throws Exception 处理异常
         */
        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            final OptionParser parser = new OptionParser();
            parser.accepts("p").withOptionalArg().ofType(String.class);
            parser.accepts("o").withOptionalArg().ofType(String.class);

            final OptionSet optionSet = parser.parse(args);

            String name = "";
            String op = "";

            if (optionSet.has("p")) {
                name = (String) optionSet.valueOf("p");
            }

            if (optionSet.has("o")) {
                op = (String) optionSet.valueOf("o");
            }

            if (UTILS.isNullOrEmpty(name) || UTILS.isNullOrEmpty(op)) {
                return ERROR_EMPTY_REMOTE_COMMAND;
            }

            return origin.clearShit().setCommandName("set")
                    .addParam("$forward-set-name", name)
                    .addParam("$forward-set-op", op);
        }
    }

    enum LockClassByteInputHandler implements CommandInputHandler {
        LOCK_CLASS_BYTE_INPUT_HANDLER
        ;

        /**
         * 这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
         * 是命令在客户端输入之后的处理
         *
         * @param args   命令参数
         * @param origin 客户端持有的协议对象
         * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
         * @throws Exception 处理异常
         */
        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            final OptionParser parser = new OptionParser();
            parser.accepts("c").withOptionalArg().ofType(String.class);
            parser.accepts("op").withOptionalArg().ofType(String.class);

            final OptionSet optionSet = parser.parse(args);

            String classes = "";
            if (optionSet.has("c")) {
                classes = (String) optionSet.valueOf("c");
            }

            if (UTILS.isNullOrEmpty(classes)) {
                return ERROR_EMPTY_REMOTE_COMMAND;
            }

            String op = "";
            if (optionSet.has("op")) {
                op = (String) optionSet.valueOf("op");
            }

            if (UTILS.isNullOrEmpty(op)) {
                return ERROR_EMPTY_REMOTE_COMMAND;
            }

            return origin.clearShit().setCommandName("lock").addParam("$forward-lock-class", classes).addParam("$forward-lock-op", op);
        }
    }

    enum RollbackClassInputHandler implements CommandInputHandler {
        ROLLBACK_CLASS_INPUT_HANDLER
        ;

        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            final OptionParser parser = new OptionParser();
            parser.accepts("c").withOptionalArg().ofType(String.class);

            final OptionSet optionSet = parser.parse(args);

            String classes = "";
            if (optionSet.has("c")) {
                classes = (String) optionSet.valueOf("c");
            }

            if (UTILS.isNullOrEmpty(classes)) {
                return ERROR_EMPTY_REMOTE_COMMAND;
            }

            return origin.clearShit().setCommandName("back").addParam("$forward-back-class", classes);
        }
    }

    enum RedefineClassInputHandler implements CommandInputHandler {
        REDEFINE_CLASS_INPUT_HANDLER
        ;

        /**
         * 这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
         * 是命令在客户端输入之后的处理
         *
         * @param args   命令参数
         * @param origin 客户端持有的协议对象
         * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
         * @throws Exception 处理异常
         */
        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            final OptionParser parser = new OptionParser();
            parser.accepts("p").withOptionalArg().ofType(String.class);

            final OptionSet optionSet = parser.parse(args);

            String filePath;
            if (optionSet.has("p")) {
                filePath = (String) optionSet.valueOf("p");
            } else {
                return ERROR_EMPTY_REMOTE_COMMAND;
            }

            /// split with " "
            String[] files = filePath.split(" ");
            Map<String, byte[]> map = new HashMap<>();
            for (String namePathKv : files) {
                String[] kv = namePathKv.split(":");
                if (kv.length != 2) {
                    PSLogger.error("不合理的参数设置，className:classPath");
                    continue;
                }
                String className = kv[0];
                String bytecodeFilePath = kv[1];
                byte[] bytes;
                try {
                    bytes = UTILS.loadFile(bytecodeFilePath, ".class");
                } catch (Exception e) {
                    PSLogger.error("无法读取字节码文件:" + bytecodeFilePath + ":" + e);
                    continue;
                }
                if (bytes == null || bytes.length == 0) {
                    PSLogger.error("读取到的字节码为空:" + className + ":" + bytecodeFilePath);
                    continue;
                }
                map.put(className, bytes);
            }
            if (map.isEmpty()) {
                return ERROR_EMPTY_REMOTE_COMMAND;
            }

            return origin.setCommandName("rdf").addParam("$forward-rdf-kv", map);
        }
    }

    enum ExitCommandInputHandler implements CommandInputHandler {
        EXIT_COMMAND_INPUT_HANDLER
        ;

        /**
         * 这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
         * 是命令在客户端输入之后的处理
         *
         * @param args   命令参数
         * @param origin 客户端持有的协议对象
         * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
         * @throws Exception 处理异常
         */
        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            return origin.clearShit().setCommandName("exit");
        }
    }

    enum ThreadCommandInputHandler implements CommandInputHandler {
        THREAD_COMMAND_INPUT_HANDLER;

        /**
         * 这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
         * 是命令在客户端输入之后的处理
         *
         * @param args   命令参数
         * @param origin 客户端持有的协议对象
         * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
         * @throws Exception 处理异常
         */
        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            final OptionParser parser = new OptionParser();
            parser.accepts("top").withOptionalArg().ofType(String.class);
            parser.accepts("status").withOptionalArg().ofType(String.class);
            parser.accepts("tid").withOptionalArg().ofType(String.class);

            final OptionSet optionSet = parser.parse(args);

            String topNOption = "";
            if (optionSet.has("top")) {
                topNOption = (String) optionSet.valueOf("top");
            }
            String status = "";
            if (optionSet.has("status")) {
                status = (String) optionSet.valueOf("status");
            }
            String tid = "";
            if (optionSet.has("tid")) {
                tid = (String) optionSet.valueOf("tid");
            }

            return origin.clearShit().setCommandName("th")
                           .addParam("$forward-th-top", topNOption)
                           .addParam("$forward-th-tid", tid)
                           .addParam("$forward-th-status", status);
        }
    }

    enum CpuTimeCommandInputHandler implements CommandInputHandler {
        CPU_TIME_COMMAND_INPUT_HANDLER;

        /**
         * 这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
         * 是命令在客户端输入之后的处理
         *
         * @param args   命令参数
         * @param origin 客户端持有的协议对象
         * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
         * @throws Exception 处理异常
         */
        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            final OptionParser parser = new OptionParser();
            parser.accepts("o").withOptionalArg().ofType(String.class);
            parser.accepts("pr").withOptionalArg().ofType(String.class);

            final OptionSet optionSet = parser.parse(args);

            String outputStyle = "";
            if (optionSet.has("o")) {
                outputStyle = (String) optionSet.valueOf("o");
            }
            String perReqOption = "";
            if (optionSet.has("pr")) {
                OptionSpec option;
                perReqOption = (String) optionSet.valueOf("pr");
            }

            return origin.clearShit().setCommandName("ct")
                           .addParam("$forward-ct-o", outputStyle)
                           .addParam("$forward-ct-pr", perReqOption);
        }
    }

    enum HelpCommandInputHandler implements CommandInputHandler {
        HELP_COMMAND_INPUT_HANDLER;

        /**
         * 这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
         * 是命令在客户端输入之后的处理
         *
         * @param args   命令参数
         * @param origin 客户端持有的协议对象
         * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
         * @throws Exception 处理异常
         */
        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            final OptionParser parser = new OptionParser();
            parser.accepts("cmd").withOptionalArg().ofType(String.class);

            final OptionSet optionSet = parser.parse(args);

            String helpCmdName;
            if (optionSet.has("cmd")) {
                helpCmdName = (String) optionSet.valueOf("cmd");
            } else {
                return ERROR_EMPTY_REMOTE_COMMAND;
            }
            if (UTILS.isNullOrEmpty(helpCmdName)) {
                return ERROR_EMPTY_REMOTE_COMMAND;
            }

            return origin.clearShit().setCommandName("h").addParam("$forward-help-cmd", helpCmdName);
        }
    }

    enum FindClassCommandInputHandler implements CommandInputHandler {
        FIND_CLASS_COMMAND_INPUT_HANDLER;

        /**
         * 这个方法用来实现将客户端的命令输入转换成{@link RemoteCommand}的过程
         * 是命令在客户端输入之后的处理
         *
         * @param args   命令参数
         * @param origin 客户端持有的协议对象
         * @return {@link RemoteCommand} 最终将被传输到服务端的命令协议
         * @throws Exception 处理异常
         */
        @Override
        public RemoteCommand toCommand(String[] args, RemoteCommand origin) throws Exception {
            final OptionParser parser = new OptionParser();
            parser.accepts("class").withOptionalArg().ofType(String.class);
            parser.accepts("r").withOptionalArg().ofType(String.class);
            parser.accepts("l").withOptionalArg().ofType(Integer.class);

            final OptionSet optionSet = parser.parse(args);

            String cls = "";
            if (optionSet.has("class")) {
                cls = (String) optionSet.valueOf("class");
            }
            String regex = "";
            if (optionSet.has("r")) {
                regex = (String) optionSet.valueOf("r");
            }
            if (UTILS.isNullOrEmpty(cls) && UTILS.isNullOrEmpty(regex)) {
                return ERROR_EMPTY_REMOTE_COMMAND;
            }
            int limit = 10;
            if (optionSet.has("l")) {
                try {
                    limit = (int) optionSet.valueOf("l");
                } catch (Exception e) {
                    PSLogger.info("error handle fc input:" + e);
                }
            }

            origin.clearShit().setCommandName("fc")
                    .addParam("$forward-fc-class", cls)
                    .addParam("$forward-fc-regex", regex)
                    .addParam("$forward-fc-limit", limit);
            return origin;
        }
    }

}
