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


package io.javadebug.core.record;

import io.javadebug.core.log.InternalLogger;
import io.javadebug.core.log.InternalLoggerFactory;

public class InternalLoggerRecord implements Log {

    // the history logger
    private static final InternalLogger LOGGER = InternalLoggerFactory.getLogger("h");

    /**
     * write the log to target output
     *
     * @param log the log data
     */
    @Override
    public void write(String log) {
        LOGGER.info(log);
    }

}
