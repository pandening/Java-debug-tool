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
//  Author : HJ


package io.javadebug.core.thirdparty;

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.thirdparty.asyncpprofiler.AsyncProfilerAbility;
import io.javadebug.core.thirdparty.exception.ThirtPartyAbilityNotFindException;
import io.javadebug.core.thirdparty.exception.ThirtyPartyExecuteException;
import io.javadebug.core.transport.RemoteCommand;

import java.util.HashMap;
import java.util.Map;

/**
 *  Java-debug-tool引进第三方类库来快速聚合java调试工具，争取覆盖越来越多的java调试场景，每一种
 *  第三方类库解决的问题可能是不一样的，比如"async-profiler"类库的引进是为了进行java应用的性能
 *  分析，通过使用"async-profiler"优秀的性能分析技术来辅助开发者进行JVM性能优化；
 *
 *
 *  ThirdPartyRouter 看起来像是一个Factory的角色，是"third-party"和"Java-debug-tool core"
 *  之间的承接层，像是一个纽扣一样，使得Java-debug-tool可以通过依赖优秀的第三方类库来快速实现某种
 *  场景的工具；
 *
 *  当然，虽然引进不同的类库是为了解决不同的技术问题，但是为了Java-debug-tool可以快速进行类库引进，需要
 *  定义一种通过能力接口，每次引进不同的类库需要实现相应的能力接口，也就是说，当你引进一个新的类库的时候，需要
 *  告诉Java-debug-tool你具备什么能力，使得在遇到一个场景的时候，Java-debug-tool可以使用你来解决问题，具体
 *  的能力抽象参考 {@link ThirdPartyAbility}
 *
 *
 * @author pandening
 *
 * @since 2.0
 */
public class ThirdPartyRouter {

    //--------------------------------------
    // 第三方能力映射map
    //--------------------------------------
    private static final Map<String, ThirdPartyAbility> THIRD_PARTY_ABILITY_MAP = new HashMap<>();

    static {

        // async-profiler
        ThirdPartyAbility asyncProfiler = new AsyncProfilerAbility();
        THIRD_PARTY_ABILITY_MAP.put("async-profiler", asyncProfiler);

    }

    /**
     *  根据协议内容获取到第三方能力
     *
     * @param remoteCommand {@link RemoteCommand}
     * @return {@link ThirdPartyAbility}
     */
    public static ThirdPartyAbility findThirdParty(RemoteCommand remoteCommand)
            throws ThirtPartyAbilityNotFindException {
        ThirdPartyAbility thirdPartyAbility =
                THIRD_PARTY_ABILITY_MAP.get(remoteCommand.getCommandName());
        if (thirdPartyAbility == null) {
            String thirdPartyName = remoteCommand.getParam("$forward-third-party");
            if (!UTILS.isNullOrEmpty(thirdPartyName)) {
                thirdPartyAbility = THIRD_PARTY_ABILITY_MAP.get(thirdPartyName);
            }
//            if (thirdPartyAbility == null) {
//                throw new ThirtPartyAbilityNotFindException("could not find third-party ability by command name:" +
//                                                                    remoteCommand.getCommandName());
//            }
        }
        return thirdPartyAbility;
    }

    /**
     *  执行命令，通过桥接第三方类库来完成实际的工作
     *
     * @param thirtyPartyAbility {@link ThirdPartyAbility}
     * @param remoteCommand {@link RemoteCommand}
     * @throws ThirtyPartyExecuteException 如果第三方执行过程中抛出任何异常，都将会被包装成该类型的异常
     */
    public static String routeAndExecute(ThirdPartyAbility thirtyPartyAbility, RemoteCommand remoteCommand)
            throws ThirtyPartyExecuteException, ThirtPartyAbilityNotFindException {

        // check
        if (thirtyPartyAbility == null) {
            throw new ThirtPartyAbilityNotFindException("could not find third-party ability for command:" + remoteCommand);
        }

        // pre-execute
        checkThirdPartyCommandBeforeExec(thirtyPartyAbility, remoteCommand);

        // register the safety detective for this command
        ThirdPartySafetyDetective safetyDetective = thirtyPartyAbility.detective(remoteCommand);
        if (safetyDetective != null) {
            PSLogger.error("get the detective for third-party command :" + remoteCommand);
            // register it!
            ThirdPartySafetyController.registerSafetyDetective(safetyDetective, remoteCommand);
        }

        // get the response
        String resp = thirtyPartyAbility.exec(remoteCommand);

        // after exec
        afterExecThirdPartyCommand(thirtyPartyAbility, remoteCommand, resp);

        return resp;
    }

    /**
     *  第三方命令执行之前进行一些校验，如果命令不符合逻辑，那么就需要拒绝执行，或者系统觉得不应该执行
     *  这个命令，也会拒绝执行，当然，还可以进行一些参数传递等一部分工作
     *
     * @param thirdPartyAbility {@link ThirdPartyAbility} 第三方能力，不应该为null
     *
     * @param remoteCommand {@link RemoteCommand} 命令协议
     */
    private static void checkThirdPartyCommandBeforeExec(ThirdPartyAbility thirdPartyAbility,
                                                  RemoteCommand remoteCommand)
            throws ThirtPartyAbilityNotFindException {

    }

    /**
     *
     * 在一次第三方命令执行之后调用，用于记录一些状态信息
     *
     * @param thirdPartyAbility 第三方命令能力
     * @param remoteCommand 命令协议
     * @param resp 命令执行结果
     */
    private static void afterExecThirdPartyCommand(ThirdPartyAbility thirdPartyAbility,
                                            RemoteCommand remoteCommand, String resp)
            throws ThirtPartyAbilityNotFindException {

    }


}
