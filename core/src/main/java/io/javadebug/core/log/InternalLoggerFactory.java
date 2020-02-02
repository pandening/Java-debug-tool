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


package io.javadebug.core.log;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 *  Stupid work for java-debug-tool's log
 *
 *  Auth : pandening
 *  Date : 2020-01-11 14:40
 */
public class InternalLoggerFactory {

    private static final String JAVA_DEBUG_TOOL_LOG_PATTERN = "%d{yyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n";
    private static final String JAVA_DEBUG_TOOL_LOG_FILE_PATTERN = "/opt/logs/java-debug-tool/main.log.%d{yyyy-MM-dd}";

    private static final String JAVA_DEBUG_TOOL_CLIENT_APPEND_NAME = "JAVA-DEBUG-TOOL-CLIENT";
    private static final String JAVA_DEBUG_TOOL_CLIENT_LOG_FILE_NAME = "/opt/logs/java-debug-tool/client.log";
    private static final String JAVA_DEBUG_TOOL_CLIENT_LOG_NAME = "SimpleDebugClientLogger";

    private static final String JAVA_DEBUG_TOOL_SERVER_APPEND_NAME = "JAVA-DEBUG-TOOL-SERVER";
    private static final String JAVA_DEBUG_TOOL_SERVER_LOG_FILE_NAME = "/opt/logs/java-debug-tool/server.log";
    private static final String JAVA_DEBUG_TOOL_SERVER_LOG_NAME = "SimpleDebugServerLogger";

    private static final String JAVA_DEBUG_TOOL_HISTORY_APPEND_NAME = "JAVA-DEBUG-TOOL-HISTORY";
    private static final String JAVA_DEBUG_TOOL_HISTORY_LOG_FILE_NAME = "/opt/logs/java-debug-tool/history.log";
    private static final String JAVA_DEBUG_TOOL_HISTORY_LOG_NAME = "SimpleDebugHistoryLogger";

    private static final String PATTERN_BUILDER_CLASS_NAME = "org.apache.logging.log4j.core.layout.PatternLayout.Builder";

    private static final String PATTERN_LAYOUT_CLASS_NAME = "org.apache.logging.log4j.core.layout.PatternLayout";

    private static final String POLICY_BUILDER_NAME = "org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy.Builder";

    private static final String TRIGGER_POLICY_CLASS_NAME = "org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy";

    private static final String APPENDER_CLASS_NAME = "org.apache.logging.log4j.core.appender.RollingFileAppender";

    private static final String LOGGER_MANAGER_CLASS_NAME = "org.apache.logging.log4j.LogManager";
    private static final String LOGGER_CONTEXT_CLASS_NAME = "org.apache.logging.log4j.core.LoggerContext";
    private static final String LOGGER_CONFIGURATION_CLASS_NAME ="org.apache.logging.log4j.core.config.Configuration";
    private static final String LOGGER_LEVEL_CLASS_NAME = "org.apache.logging.log4j.Level";

    /**
     *  CREATE AN LOG4J LOGGER
     *
     * @param appender THE APPENDER NAME
     * @param name  THE LOGGER NAME
     * @param file THE LOG FILE
     * @return THE LOGGER {@link InternalLogger}
     */
    private static InternalLogger createLogger(String appender, String name, String file) {
        try {
            // load the log manager
            Class<?> logManagerCls = Class.forName(LOGGER_MANAGER_CLASS_NAME);

            // get the method 'getContext'
            Method getContextMethod = logManagerCls.getMethod("getContext");

            // do invoke
            Object LOGGER_CONTEXT_OBJ = getContextMethod.invoke(null);

            // load the context class
            Class<?> contextClass = Class.forName(LOGGER_CONTEXT_CLASS_NAME);

            // get the config method
            Method getConfigurationMethod = contextClass.getMethod("getConfiguration");

            // do invoke
            Object LOGGER_CONFIGURATION_OBJ = getConfigurationMethod.invoke(LOGGER_CONTEXT_OBJ);

            // Create java-debug-tool pattern layout
            Class<?> patternLayoutCls = Class.forName(PATTERN_LAYOUT_CLASS_NAME);

            // the pattern layout builder
            Object PATTERN_BUILDER_OBJ = MethodUtils.invokeStaticMethod(patternLayoutCls, "newBuilder");

            // set charset
            MethodUtils.invokeMethod(PATTERN_BUILDER_OBJ, "withCharset", Charset.forName("UTF-8"));

            // set configure
            MethodUtils.invokeMethod(PATTERN_BUILDER_OBJ, "withConfiguration", LOGGER_CONFIGURATION_OBJ);

            // set pattern
            MethodUtils.invokeMethod(PATTERN_BUILDER_OBJ, "withPattern", JAVA_DEBUG_TOOL_LOG_PATTERN);

            // build pattern layout obj
            Object PATTERN_LAYOUT_OBJ = MethodUtils.invokeMethod(PATTERN_BUILDER_OBJ, "build");

            // create java-debug-tool policy
            Class<?> policyCls = Class.forName(TRIGGER_POLICY_CLASS_NAME);

//            POLICY_BUILD_OBJ = MethodUtils.invokeStaticMethod(policyCls, "newBuilder");

//            // set withModulate
//            MethodUtils.invokeMethod(POLICY_BUILD_OBJ, "withModulate", true);
//
//            // set withInterval
//            MethodUtils.invokeMethod(POLICY_BUILD_OBJ, "withInterval", 1);

            // build policy
            Object POLICY_OBJ = MethodUtils.invokeStaticMethod(policyCls, "createPolicy", "1", "true");

            // create java-debug-tool append
            Class<?> appenderCls = Class.forName(APPENDER_CLASS_NAME);
            Object APPENDER_BUILDER_OBJ = MethodUtils.invokeStaticMethod(appenderCls, "newBuilder");

            // set name
            MethodUtils.invokeMethod(APPENDER_BUILDER_OBJ, "withName", appender);

            // withImmediateFlush
            MethodUtils.invokeMethod(APPENDER_BUILDER_OBJ, "withImmediateFlush", true);

            // withFileName
            MethodUtils.invokeMethod(APPENDER_BUILDER_OBJ, "withFileName", file);

            // withFilePattern
            MethodUtils.invokeMethod(APPENDER_BUILDER_OBJ, "withFilePattern", JAVA_DEBUG_TOOL_LOG_FILE_PATTERN);

            // layout
            MethodUtils.invokeMethod(APPENDER_BUILDER_OBJ, "withLayout", PATTERN_LAYOUT_OBJ);

            // policy
            MethodUtils.invokeMethod(APPENDER_BUILDER_OBJ, "withPolicy", POLICY_OBJ);

            // withConfiguration
            MethodUtils.invokeMethod(APPENDER_BUILDER_OBJ, "withConfiguration", LOGGER_CONFIGURATION_OBJ);

            // build the appender
            Object APPENDER_OBJ = MethodUtils.invokeMethod(APPENDER_BUILDER_OBJ, "build");

            // append.start
            MethodUtils.invokeMethod(APPENDER_OBJ, "start");

            // config.addAppender
            MethodUtils.invokeMethod(LOGGER_CONFIGURATION_OBJ, "addAppender", APPENDER_OBJ);

            // with log
            Object LOG_CONFIG_OBJ = MethodUtils.invokeMethod(LOGGER_CONFIGURATION_OBJ, "getLoggerConfig", name);
            Class<?> loggerLevelCls = Class.forName(LOGGER_LEVEL_CLASS_NAME);
            Object loggerLevel = FieldUtils.getField(loggerLevelCls, "DEBUG").get(null);
            MethodUtils.invokeMethod(LOG_CONFIG_OBJ, "addAppender", APPENDER_OBJ, loggerLevel, null);
            // update logs
            MethodUtils.invokeMethod(LOGGER_CONTEXT_OBJ, "updateLoggers", LOGGER_CONFIGURATION_OBJ);

            // get log
            Object logger = MethodUtils.invokeMethod(LOGGER_CONTEXT_OBJ, "getLogger", name);
            // try to log something
            //MethodUtils.invokeMethod(logger, "error", "hello, java-debug-tool user");

            System.err.println("find the log4j and create new appender for java-debug-tool done !");
            return new Log4jImplLogger(logger);
        } catch (Throwable e) {
            //e.printStackTrace();
            //System.err.println("could not find log4j in target jvm, fallback to use stdout for logging ..");
        }
        // fallback
        return new StdImplLogger();
    }

    // logger
    // c -> client logger
    // s -> server logger
    // h -> history logger
    private static Map<String, InternalLogger> loggerMap = new HashMap<>();

    static {
        try {
            InternalLogger logger;
//            // server logger
//            logger = createLogger(JAVA_DEBUG_TOOL_CLIENT_APPEND_NAME, JAVA_DEBUG_TOOL_CLIENT_LOG_NAME, JAVA_DEBUG_TOOL_CLIENT_LOG_FILE_NAME);
//            loggerMap.put("c", logger);

            // client logger
            logger = createLogger(JAVA_DEBUG_TOOL_SERVER_APPEND_NAME, JAVA_DEBUG_TOOL_SERVER_LOG_NAME, JAVA_DEBUG_TOOL_SERVER_LOG_FILE_NAME);
            loggerMap.put("s", logger);

//            // history logger
//            logger = createLogger(JAVA_DEBUG_TOOL_HISTORY_APPEND_NAME, JAVA_DEBUG_TOOL_HISTORY_APPEND_NAME, JAVA_DEBUG_TOOL_HISTORY_LOG_FILE_NAME);
//            loggerMap.put("h", logger);
        } catch (Throwable e) {
            // ignore, maybe client side call here ~
            // use stdout in client side is ok ~
        }
    }

    /**
     *  get the java-debug-tool logger
     *   1) check whether the target jvm use log4j, if true, then create an new appender for java-debug-tool
     *   2) fallback to use stdout
     *
     *  this method will init once, all of the call after init will get a cached logger
     *
     * @return a logger
     */
    public static InternalLogger getLogger(String s) {
        InternalLogger logger = loggerMap.get(s);
        if (logger == null) {
            logger = new StdImplLogger();
            loggerMap.put(s, logger);
        }
        return logger;
    }

}
