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


package io.javadebug.core.command.perf;

import io.javadebug.core.CommandServer;
import io.javadebug.core.ServerHook;
import io.javadebug.core.annotation.CommandDescribe;
import io.javadebug.core.annotation.CommandType;
import io.javadebug.core.command.Command;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.transport.RemoteCommand;
import io.javadebug.core.utils.JacksonUtils;
import io.javadebug.core.utils.UTILS;

import java.lang.instrument.Instrumentation;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 *  100 ms one time;
 *
 */
@CommandDescribe(
        name = "cputime",
        simpleName = "ct",
        function = "Query Cpu Usage",
        usage = "ct -o [ csv | json] -pr [per/req]",
        cmdType = CommandType.COMPUTE
)
public class CpuTimeUsageCommand  implements Command {

    @Override
    public boolean preExecute(RemoteCommand remoteCommand) throws Exception {
        remoteCommand.addParam("$forward-timeout-check-tag", String.valueOf(65));
        return true;
    }

    @Override
    public String execute(Instrumentation ins, RemoteCommand reqRemoteCommand, CommandServer commandServer, ServerHook serverHook) throws Exception {
        final StringWrap stringWrap = new StringWrap();
        CountDownLatch latch = new CountDownLatch(1);
        Date startDate = new Date();
        CpuTimeWorker.collect(reqRemoteCommand.getContextId(), new CpuTimeWorker.WaitListener() {
            @Override
            public void done(CpuTimeReportStruct cpuTimeReportStruct) {
                stringWrap.val = handleResult(cpuTimeReportStruct);
                latch.countDown();
            }

            private String handleResult(CpuTimeReportStruct cpuTimeReportStruct) {

                int perReq = 1;
                String perReqOption = reqRemoteCommand.getParam("$forward-ct-pr");
                if (!UTILS.isNullOrEmpty(perReqOption)) {
                    perReq = UTILS.safeParseInt(perReqOption, perReq);
                }
                PSLogger.error("get the per-req val:" + perReq);

                String style = reqRemoteCommand.getParam("$forward-ct-o");
                if (UTILS.isNullOrEmpty(style)) {
                    return cpuTimeReportStruct.toString();
                }

                if ("s".equals(style)) {
                    return processToSimple(cpuTimeReportStruct);
                }

                if ("p".equals(style)) {
                    return processToPlot(cpuTimeReportStruct);
                }

                if ("json".equals(style)) {
                    return processToJson(cpuTimeReportStruct);
                }

                if ("csv".equals(style)) {
                    return processToCSV(cpuTimeReportStruct, 1.0 * perReq, startDate);
                }

                return cpuTimeReportStruct.toString();
            }

            /**
             *  csv format
             *
             *  time;usr;sys;avg_usr;avg_sys
             *  t1;v1;v2;v3;v4
             *  ...
             *
             *
             * @param cpuTimeReportStruct the origin report info
             * @return csv format
             */
            private String processToCSV(CpuTimeReportStruct cpuTimeReportStruct, double pr, Date start) {

                // calc the avg cost
                AvgCpuUsage avgCpuUsage = calcAvgCost(cpuTimeReportStruct);

                StringBuilder csvBuilder = new StringBuilder();

                // start time info
                csvBuilder.append("Start Time : ").append(start).append("\n");

                // end time info
                csvBuilder.append("Stop Time : ").append(new Date()).append("\n");

                int baseTime = 0;
                String split = ";";
                String timeFormat = "%.3f";

                String usrTab = "usr_ms", sysTab = "sys_ms", avgUsrTag = "avg_usr_ms", avgSysTab = "avg_sys_ms",
                        nivcSwitchTab = "nivc_switch_per_sec", nvcSwitchTab = "nvc_switch_per_sec";
                        //load1Tab = "load1", load5Tab = "load5", load15Tab = "load15";
                String perReqSuffix = "_per_req";
                if (Double.compare(pr, 1.0) > 0) {
                    usrTab += perReqSuffix;
                    sysTab += perReqSuffix;
                    avgUsrTag += perReqSuffix;
                    avgSysTab += perReqSuffix;
                } else {
                    // total cpu usage
                    csvBuilder.append("Total Avg cpu usage : ").append((avgCpuUsage.avgUsr * 1000) + avgCpuUsage.avgSys * 1000)
                            .append(" ms").append("\n\n");
                }
                csvBuilder.append("time").append(split).append(usrTab).append(split).append(sysTab).append(split)
                        .append(avgUsrTag).append(split).append(avgSysTab).append(split)
                        .append(nivcSwitchTab).append(split).append(nvcSwitchTab)
                        //.append(split).append(load1Tab).append(split).append(load5Tab).append(split).append(load15Tab)
                        .append("\n");
                TimeInfo preTM = cpuTimeReportStruct.baseTimeLine;
                for (TimeInfo timeInfo : cpuTimeReportStruct.timeInfoList) {
                    csvBuilder.append(baseTime).append(split)
                            .append(String.format(timeFormat, (1000 * (timeInfo.userSecs - preTM.userSecs)) / pr)).append(split)
                            .append(String.format(timeFormat, ((timeInfo.systemSecs - preTM.systemSecs) * 1000) / pr))
                            .append(split).append(String.format(timeFormat, (avgCpuUsage.avgUsr * 1000) / pr))
                            .append(split).append(String.format(timeFormat, (avgCpuUsage.avgSys * 1000) / pr))
                            .append(split).append(timeInfo.nivcsw - preTM.nivcsw).append(split).append(timeInfo.nvcsw - preTM.nvcsw)
                            //.append(split).append(timeInfo.loadavg[0]).append(split).append(timeInfo.loadavg[1]).append(split).append(timeInfo.loadavg[2])
                            .append("\n");
                    preTM = timeInfo;
                    baseTime += 1;
                }
                return csvBuilder.toString();
            }

            class AvgCpuUsage {
                /**
                 *  the avg usr cpu time
                 *
                 */
                double avgUsr = 0.0;

                /**
                 *  the avg sys cpu time
                 *
                 */
                double avgSys = 0.0;
            }

            private AvgCpuUsage calcAvgCost(CpuTimeReportStruct cpuTimeReportStruct) {
                List<TimeInfo> timeInfoList = cpuTimeReportStruct.timeInfoList;
                if (timeInfoList.isEmpty() || timeInfoList.size() < 2) {
                    return new AvgCpuUsage();
                }
                TimeInfo preTm = cpuTimeReportStruct.baseTimeLine;
                double usrMin = timeInfoList.get(0).userSecs - preTm.userSecs,
                        usrMax = timeInfoList.get(0).userSecs - preTm.userSecs,
                        sysMin = timeInfoList.get(0).systemSecs - preTm.systemSecs,
                        sysMax = timeInfoList.get(0).systemSecs - preTm.systemSecs,
                        totalUsr = usrMin, totalSys = sysMax;
                preTm = timeInfoList.get(0);
                for (int i = 1; i < timeInfoList.size(); i ++) {

                    double curSys = timeInfoList.get(i).systemSecs - preTm.systemSecs;
                    double curUsr = timeInfoList.get(i).userSecs - preTm.userSecs;

                    preTm = timeInfoList.get(i);

                    totalUsr += curUsr;
                    totalSys += curSys;
                    if (curUsr < usrMin) {
                        usrMin = curUsr;
                    }
                    if (curUsr > usrMax) {
                        usrMax = curUsr;
                    }
                    if (curSys < sysMin) {
                        sysMin = curSys;
                    }
                    if (curSys > sysMax) {
                        sysMax = curSys;
                    }
                }

                totalUsr -= (usrMin + usrMax);
                totalSys -= (sysMin + sysMax);

                AvgCpuUsage avgCpuUsage = new AvgCpuUsage();
                avgCpuUsage.avgUsr = totalUsr / (1.0 * (timeInfoList.size() - 2));
                avgCpuUsage.avgSys = totalSys / (1.0 * (timeInfoList.size() - 2));

                return avgCpuUsage;
            }

            private String processToJson(CpuTimeReportStruct cpuTimeReportStruct) {
                try {
                    return JacksonUtils.JACKSON_UTILS.getOriginMapper().writeValueAsString(cpuTimeReportStruct);
                } catch (Exception e) {
                    return String.format("{\"error\": \"%s\"}", UTILS.getErrorMsg(e));
                }
            }

            private String processToPlot(CpuTimeReportStruct cpuTimeReportStruct) {
                StringBuilder show = new StringBuilder();
                show.append("total cost:").append((cpuTimeReportStruct.stopMills - cpuTimeReportStruct.startMills) / 1000.0)
                        .append(" secs ").append("with ").append(cpuTimeReportStruct.timeInfoList.size()).append(" points").append("\n")
                        .append("-----------------------------").append("\n")
                        .append("usr").append("     ").append("\t").append("sys").append("     ").append("\t").append("\n");
                TimeInfo preTM = cpuTimeReportStruct.baseTimeLine;
                for (TimeInfo timeInfo : cpuTimeReportStruct.timeInfoList) {
                    show.append(String.format("%.3f     %.3f\n", timeInfo.userSecs - preTM.userSecs,
                            timeInfo.systemSecs - preTM.systemSecs));
                    preTM = timeInfo;
                }
                return show.toString();
            }

            private String processToSimple(CpuTimeReportStruct cpuTimeReportStruct) {
                StringBuilder show = new StringBuilder();
                show.append("total cost:").append((cpuTimeReportStruct.stopMills - cpuTimeReportStruct.startMills) / 1000.0)
                        .append(" secs ").append("with ").append(cpuTimeReportStruct.timeInfoList.size()).append(" points").append("\n")
                        .append("-----------------------------").append("\n")
                        .append("usr").append("     ").append("\t").append("sys").append("     ").append("\t").append("\n");
                for (TimeInfo timeInfo : cpuTimeReportStruct.timeInfoList) {
                    show.append(String.format("%.3f     %.3f\n", timeInfo.userSecs, timeInfo.systemSecs));
                }
                return show.toString();
            }

            @Override
            public void error(Throwable e) {
                stringWrap.val = UTILS.getErrorMsg(e);
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (Exception e) {
            PSLogger.error("error occ on execute:" + UTILS.getErrorMsg(e));
        }
        return stringWrap.val;
    }

    @Override
    public boolean stop(Instrumentation ins, RemoteCommand reqRemoteCommand, CommandServer commandServer, ServerHook serverHook) throws Exception {
        return true;
    }

    static class StringWrap {
        String val = "un-touch";
    }

}
