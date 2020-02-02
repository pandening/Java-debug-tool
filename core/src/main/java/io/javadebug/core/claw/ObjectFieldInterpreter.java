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


package io.javadebug.core.claw;

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.exception.ObjectFieldInterpreterException;

import java.lang.reflect.Method;
import java.util.List;

public class ObjectFieldInterpreter {

    /**
     *  this method need to do interpreter work for object field. the clawScript will
     *  describe the aim of user, like:
     *
     *
     *  p0.a="a",p2="100"
     *
     *
     *  this means user want to set the first params's field named a as 'a', and set the second
     *  params as 100(the second params 's type must be number {@link Number})
     *
     *
     * @param params the origin params.
     * @param clawScript the claw script.
     * @throws ObjectFieldInterpreterException error occur
     */
    public static Object[] interpreter(Object[] params, String clawScript, Method targetMethod)
            throws ObjectFieldInterpreterException {
        if (params == null || params.length == 0
                || UTILS.isNullOrEmpty(clawScript)) {
            PSLogger.error("invalid params");
            return null;
        }

        PSLogger.info("start to interpreter script:" + clawScript);

        // get the claw meta info
        List<ClawMeta> clawMetas = AutoMechanics.scan(clawScript, params, targetMethod);

        // set the fields
        if (clawMetas == null || clawMetas.isEmpty()) {
            PSLogger.error("the claw meta data is null after AutoMechanics.scan:" + clawScript);
            return params;
        }

        // do set work.
        for (ClawMeta clawMeta : clawMetas) {
            clawMeta.doSet();
        }

        PSLogger.info("end to interpreter script:" + clawScript);
        return params;
    }

}
