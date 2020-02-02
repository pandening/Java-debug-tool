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

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.exception.CommandExecuteWithStageException;

public class OnErrorHookImpl implements DefaultHookImpl<CommandExecuteWithStageException, Void> {
    /**
     * Applies this function to the given argument.
     *
     * @param e the function argument
     * @return the function result
     */
    @Override
    public Void apply(CommandExecuteWithStageException e) {

        // consume it!
        try {

            StringBuilder esb = new StringBuilder();
            String cause = UTILS.getErrorMsg(e);
            esb.append("cause : ").append(cause).append("\n");

            int deep = 0;
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (StackTraceElement element : stackTrace) {
                esb.append(element.getClassName()).append(".").append(element.getMethodName()).append(" at line:").append(element.getLineNumber()).append("\n");
                if (++ deep >= 10) {
                    break;
                }
            }

            RECORD_OPERATOR_LOG.write(esb.toString());
        } catch (Exception ee) {
            PSLogger.error("error when consume to-ui msg:" + ee);
        }

        return null;
    }

    /**
     * check the data before you consume it!
     *
     * @param stage the stage of current
     * @return true means you will consume the data,
     * the {@link HookOperator#apply(Object)} method will call with param {@see data}
     */
    @Override
    public boolean isInterest(RuntimeStage stage) {
        return RuntimeStage.ERROR.equals(stage);
    }
}
