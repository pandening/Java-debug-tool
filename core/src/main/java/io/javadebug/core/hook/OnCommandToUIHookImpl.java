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

public class OnCommandToUIHookImpl implements DefaultHookImpl<String, Void> {
    /**
     * Applies this function to the given argument.
     *
     * @param s the function argument
     * @return the function result
     */
    @Override
    public Void apply(String s) {

        // consume it!
        try {
            RECORD_OPERATOR_LOG.write(s);
        } catch (Exception e) {
            PSLogger.error("error when consume to-ui msg:" + s + " with exception : " + e);
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
        return stage.equals(RuntimeStage.DEBUG_COMMAND_SHOW);
    }
}
