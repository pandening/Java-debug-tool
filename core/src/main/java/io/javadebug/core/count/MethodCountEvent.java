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


package io.javadebug.core.count;

import io.javadebug.com.netflix.numerus.NumerusRollingNumberEvent;

/**
 *  For count the method invoke
 *
 *   REQUESTS -> TPS
 *   SUCCESS  -> success invoke
 *   ERROR    -> error invoke
 *
 */
public enum MethodCountEvent implements NumerusRollingNumberEvent {
    REQUESTS(true), SUCCESS(true), ERROR(true);

    private final boolean isCounter;
    private final boolean isMaxUpdater;

    MethodCountEvent(boolean isCounter) {
        this.isCounter = isCounter;
        this.isMaxUpdater = !isCounter;
    }

    @Override
    public boolean isCounter() {
        return isCounter;
    }

    @Override
    public boolean isMaxUpdater() {
        return isMaxUpdater;
    }

    @Override
    public NumerusRollingNumberEvent[] getValues() {
        return values();
    }
}
