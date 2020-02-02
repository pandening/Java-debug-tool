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


package io.javadebug.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static io.javadebug.core.utils.UTILS.isNullOrEmpty;
import static io.javadebug.core.utils.UTILS.isNullOrEmptyArray;
import static io.javadebug.core.utils.UTILS.safeParseInt;

/**
 * Created on 2019/4/17 15:29.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class Configure {

    private String pid;
    private String targetIp;
    private int targetPort;
    private String agentJar;
    private String coreJar;

    private static final Configure EMPTY_CONFIG = new Configure();

    private static final Map<String, BiConsumer<Configure, String>> FIELD_APPLY_MAP = new HashMap<>();
    static {
        FIELD_APPLY_MAP.put("pid", new BiConsumer<Configure, String>() {
            @Override
            public void accept(Configure configure, String s) {
                configure.setPid(s);
            }
        });
        FIELD_APPLY_MAP.put("targetIp", new BiConsumer<Configure, String>() {
            @Override
            public void accept(Configure configure, String s) {
                configure.setTargetIp(s);
            }
        });
        FIELD_APPLY_MAP.put("targetPort", new BiConsumer<Configure, String>() {
            @Override
            public void accept(Configure configure, String s) {
                int port = safeParseInt(s, -1);
                configure.setTargetPort(port);
            }
        });
        FIELD_APPLY_MAP.put("agentJar", new BiConsumer<Configure, String>() {
            @Override
            public void accept(Configure configure, String s) {
                configure.setAgentJar(s);
            }
        });
        FIELD_APPLY_MAP.put("coreJar", new BiConsumer<Configure, String>() {
            @Override
            public void accept(Configure configure, String s) {
                configure.setCoreJar(s);
            }
        });
    }

    @Override
    public String toString() {
        return String.format(
                "pid=%s,targetIp=%s,targetPort=%d,agentJar=%s,coreJar=%s",
                 pid, targetIp, targetPort, agentJar, coreJar);
    }

    public static Configure toConfigure(String conf) {
        if (isNullOrEmpty(conf)) {
            return EMPTY_CONFIG;
        }
        String[] kvs = conf.split(",");
        if (isNullOrEmptyArray(kvs)) {
            return EMPTY_CONFIG;
        }
        Configure configure = new Configure();
        for (String kv : kvs) {
            String[] keyVal = kv.split("=");
            if (isNullOrEmptyArray(keyVal) || keyVal.length != 2) {
                continue;
            }
            if (null == FIELD_APPLY_MAP.get(keyVal[0])) {
                continue;
            }
            FIELD_APPLY_MAP.get(keyVal[0]).accept(configure, keyVal[1]);
        }
        return configure;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public String getAgentJar() {
        return agentJar;
    }

    public void setAgentJar(String agentJar) {
        this.agentJar = agentJar;
    }

    public String getCoreJar() {
        return coreJar;
    }

    public void setCoreJar(String coreJar) {
        this.coreJar = coreJar;
    }

}
