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


package io.javadebug.core.enhance;

import io.javadebug.core.data.ClassField;
import io.javadebug.core.utils.FieldValueExtract;
import io.javadebug.core.utils.ObjectUtils;
import io.javadebug.core.log.PSLogger;
import io.javadebug.core.utils.UTILS;
import io.javadebug.core.data.VariableModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MethodTraceConverter {

    /**
     * 转换原始方法调用信息，处理成仅带行号的展示样式
     *
     * [input: params]
     * (linenumber)
     * [return val/throw exception at line]
     *
     * @param methodTraceFrames {@link MethodTraceFrame}
     * @return style
     */
    public static String toLineTrace(List<MethodTraceFrame> methodTraceFrames, Set<Integer> filterLineSet, boolean needCallStack, boolean needField) {
        return toStringWithConfigV2(methodTraceFrames, false, false, filterLineSet, needCallStack, needField);
    }

    /**
     *
     * 处理成:
     * [params]
     * (line, cost)
     * [return val/ throw exception with cost at line]
     *
     * @param methodTraceFrames {@link MethodTraceFrame}
     * @return style
     */
    public static String toLineWithCostTrace(List<MethodTraceFrame> methodTraceFrames, Set<Integer> filterLineSet, boolean needCallStack, boolean needField) {
        return toStringWithConfigV2(methodTraceFrames, true, false, filterLineSet, needCallStack, needField);
    }

    /**
     * 处理成
     * [params]
     * [(line, var, cost)]
     * [return val/ throw with cost at line]
     *
     * @param methodTraceFrames {@link MethodTraceFrame}
     * @return style
     */
    public static String toLineWithVarCostTrace(List<MethodTraceFrame> methodTraceFrames, Set<Integer> filterLineSet, boolean needCallStack, boolean needField) {
        return toStringWithConfigV2(methodTraceFrames, true, true, filterLineSet, needCallStack, needField);
    }

    /**
     *  the toString handler.
     *
     * @param methodTraceFrames {@link MethodTraceFrame}
     * @param needCost => need the cost
     * @param needVar => need the var
     * @return style
     */
    @Deprecated
    private static String toStringWithConfig(List<MethodTraceFrame> methodTraceFrames,
                                             boolean needCost, boolean needVar, Set<Integer> filterLineSet) {
        if (methodTraceFrames == null || methodTraceFrames.isEmpty()) {
            return "empty method trace\n";
        }

        StringBuilder sb = new StringBuilder();

        // get the params
        MethodTraceFrame frame = methodTraceFrames.get(0);

        if (!frame.isMethodEnter()) {
            throw new IllegalStateException("the first frame must be the enter frame");
        }

        // get the method enter time mills
        long methodEnterMills = frame.getInvokeTimeMills();

        sb.append("[").append(frame.getClassName())
                .append(".").append(frame.getMethodName()).append("] with params\n")
                .append(Arrays.toString(frame.getParams())).append("\n");

        // timeline
        List<Long> timeCost = null;
        long preLineInvokeTime = methodTraceFrames.get(1).getInvokeTimeMills();
        if (needCost) {
            timeCost = new ArrayList<>();
            for (int i = 2; i < methodTraceFrames.size(); i ++) {
                timeCost.add(methodTraceFrames.get(i).getInvokeTimeMills() - preLineInvokeTime);
                preLineInvokeTime = methodTraceFrames.get(i).getInvokeTimeMills();
            }
        }

        List<String> varList = null;
        if (needVar) {
            varList = new ArrayList<>();
            for (int i = 2; i < methodTraceFrames.size(); i ++) {
                List<VariableModel> list = methodTraceFrames.get(i).getLocalVariable();
                if (list == null || list.isEmpty()) {
                    varList.add("[]");
                } else {
                    varList.add(list.toString());
                }
            }
        }

        // foreach line
        int exitLine = -1;
        int j = 0;
        for (int i = 1; i < methodTraceFrames.size() - 1; i ++) {
            // filter
            int lineNo = methodTraceFrames.get(i).getLineNo();
            if ( (lineNo < 0) || (filterLineSet != null && !filterLineSet.contains(lineNo))) {
                j ++;
                continue;
            }

            /// [cost] [line] [var]
            if (needCost) {
                sb.append("[").append(timeCost.get(j)).append(" ms] ");
            }
            sb.append("(").append(methodTraceFrames.get(i).getLineNo()).append(") ");
            if (needVar && !"[]".equals(varList.get(j))) {
                sb.append(varList.get(j));
            }
            sb.append("\n");
            exitLine = methodTraceFrames.get(i).getLineNo();
            j ++;
        }

        // get the return ot throw
        frame = methodTraceFrames.get(methodTraceFrames.size() - 1);
        if (!frame.isEnd()) {
            throw new IllegalStateException("the last frame must be exit frame:" + methodTraceFrames);
        }
        if (frame.isReturn()) {
            sb.append("return value:[").append(frame.getReturnVal()).append("] ");
        } else if (frame.isThrow()) {
            sb.append("throw exception:[").append(frame.getThrowable()).append("] ");
        }
        sb.append(" at line:").append(exitLine).append(" with cost:")
                .append(frame.getInvokeTimeMills() - methodEnterMills).append(" ms \n");

        return sb.toString();
    }

    /**
     *  将对象数组打印出来
     *
     * @param objs 对象数组
     * @return 可识别的字符串
     */
    private static String printObjectArray(Object[] objs) {
        try {
            StringBuilder retSb = new StringBuilder();
            retSb.append("[\n");
            for (int i = 0; i < objs.length; i ++) {
                if (objs[i] == null) {
                    retSb.append("[").append(i).append("] ").append("@unknown")
                            .append(" -> ").append("NULL").append(",\n");
                    continue;
                }
                retSb.append("[").append(i).append("] ").append("@class:").append(objs[i].getClass().getName())
                        .append(" -> ").append(ObjectUtils.printObjectToString(objs[i])).append(",\n");
            }
            // ","
            retSb.deleteCharAt(retSb.length() - 2);
            retSb.append("]");
            return retSb.toString();
        } catch (Exception e) {
            PSLogger.error("打印对象数组遇到无法处理的错误:" + e);
            return Arrays.toString(objs);
        }
    }

    /**
     *  the toString handler.
     *
     * @param methodTraceFrames {@link MethodTraceFrame}
     * @param needCost => need the cost
     * @param needVar => need the var
     * @return style
     */
    private static String toStringWithConfigV2(List<MethodTraceFrame> methodTraceFrames,
                                             boolean needCost, boolean needVar, Set<Integer> filterLineSet,
                                               boolean needCallStack, boolean needField) {
        if (methodTraceFrames == null || methodTraceFrames.isEmpty()) {
            return "empty method trace\n";
        }

        StringBuilder sb = new StringBuilder();

        // get the params
        MethodTraceFrame frame = methodTraceFrames.get(0);

        if (!frame.isMethodEnter()) {
            throw new IllegalStateException("the first frame must be the enter frame");
        }

        // the target object
        Object targetObject = frame.getTargetObject();

        // get the method enter time mills
        long methodEnterMills = frame.getInvokeTimeMills();

        // the call
        String caller = frame.getCaller();

        sb.append("[").append(frame.getClassName())
                .append(".").append(frame.getMethodName()).append("] invoke by Thread:").append(caller)
                .append("\nwith params\n")
                .append(printObjectArray(frame.getParams())).append("\n");

        // timeline
        List<Long> timeCost = null;
        long preLineInvokeTime = methodTraceFrames.get(1).getInvokeTimeMills();
        if (needCost) {
            timeCost = new ArrayList<>();
            for (int i = 2; i < methodTraceFrames.size(); i ++) {
                timeCost.add(methodTraceFrames.get(i).getInvokeTimeMills() - preLineInvokeTime);
                preLineInvokeTime = methodTraceFrames.get(i).getInvokeTimeMills();
            }
        }

        // foreach line
        int exitLine = -1;
        int j = 0;
        for (int i = 1; i < methodTraceFrames.size() - 1; i ++) {
            // filter
            int lineNo = methodTraceFrames.get(i).getLineNo();
            if ( (lineNo < 0) || (filterLineSet != null && !filterLineSet.contains(lineNo))) {
                j ++;
                continue;
            }

            /// [cost] [line] [var]
            if (needCost) {
                sb.append("[").append(timeCost.get(j)).append(" ms] ");
            }
            sb.append("(").append(methodTraceFrames.get(i).getLineNo()).append(") ");

            // 是否需要变量信息
            if (needVar) {
                List<VariableModel> varList = methodTraceFrames.get(i).getLocalVariable();
                if (varList != null && !varList.isEmpty()) {
                    sb.append(varList.toString());
                }
            }

            sb.append("\n");
            exitLine = methodTraceFrames.get(i).getLineNo();
            j ++;
        }

        // get the return ot throw
        frame = methodTraceFrames.get(methodTraceFrames.size() - 1);
        if (!frame.isEnd()) {
            throw new IllegalStateException("the last frame must be exit frame:" + methodTraceFrames);
        }
        if (frame.isReturn()) {
            sb.append("return value:[").append(ObjectUtils.printObjectToString(frame.getReturnVal())).append("] ");
        } else if (frame.isThrow()) {
            sb.append("throw exception:[").append(frame.getThrowable()).append("] ");
        }
        sb.append(" at line:").append(exitLine).append(" with cost:")
                .append(frame.getInvokeTimeMills() - methodEnterMills).append(" ms \n");

        // call trace
        String callTrace = frame.getCallTrace();
        if (needCallStack && !UTILS.isNullOrEmpty(callTrace)) {
            sb.append(callTrace);
        }

        // field val
        if (needField) {
            appendFieldVal(sb, targetObject);
        } else {
            // fields snapshot
            List<ClassField> f1 = frame.getOnMethodEnterClassFields();
            List<ClassField> f2 = frame.getOnMethodExitClassFields();

            if (f1 != null && !f1.isEmpty()) {
                sb.append("\n").append("Before Invoking Method").append("\n");
                appendClassSnapshotFields(sb, f1);
            }

            if (f2 != null && !f2.isEmpty()) {
                sb.append("\n").append("Before Exiting Method").append("\n");
                appendClassSnapshotFields(sb, f2);
            }

        }

        return sb.toString();
    }

    private static void appendClassSnapshotFields(StringBuilder result, List<ClassField> classFields) {
        for (ClassField classField : classFields) {
            result.append(classField).append("\n");
        }
    }

    private static void appendFieldVal(StringBuilder result, Object obj) {
        try {
            result.append("Fields:").append("\n");
            List<ClassField> classFieldList = FieldValueExtract.extractFields(obj, true, true, true);
            if (classFieldList != null && !classFieldList.isEmpty()) {
                for (ClassField classField : classFieldList) {
                    result.append(classField).append("\n");
                }
            }
        } catch (Exception e) {
            PSLogger.error("error occ when appendFieldVal:" + UTILS.getErrorMsg(e));
        }

    }

}
