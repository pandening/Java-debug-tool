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
import io.javadebug.core.exception.ClawScriptScanException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoMechanics {

    private static final String CLAW_SCRIPT_PATTERN_PARAM_CHOOSE_SOURCE = "p[0-9]+(\\.[a-zA-Z0-9]+)*";

    //a-zA-Z0-9.\s
    private static final String CLAW_SCRIPT_PATTERN_OBJ_VALUE_SOURCE = "\\s*=\\s*\"-?[^\"]*\"";

    private static final String CLAW_SCRIPT_PATTERN_ARRAY_VAL_SOURCE = "\"\\[\\s*(\\[.*])*\\s*]\"";

    private static final String CLAW_SCRIPT_PATTERN_ARRAY_VAL_EACH_SOURCE = "\\[.*]";

    private static final String CLAW_SCRIPT_PATTERN_MAP_VAL_EACH_SOURCE = "\\{.*}";

    private static final String CLAW_SCRIPT_PATTERN_OBJ_VALUE_ONLY_SOURCE = "\"-?[^\"]*\"";

    private static final String CLAW_SCRIPT_SIMPLE_ASSIGN_SOURCE = "p[0-9]+" + CLAW_SCRIPT_PATTERN_OBJ_VALUE_SOURCE;

    private static final String CLAW_SCRIPT_PATTERN_SOURCE = CLAW_SCRIPT_PATTERN_PARAM_CHOOSE_SOURCE +
                                                                     CLAW_SCRIPT_PATTERN_OBJ_VALUE_SOURCE;
    //"p[0-9]+\\.[a-zA-Z0-9]+\\s*=\\s*\"[a-zA-Z0-9.\\s]+\"";

    private static final Pattern CLAW_SCRIPT_PARAM_CHOOSE_PATTERN = Pattern.compile(CLAW_SCRIPT_PATTERN_PARAM_CHOOSE_SOURCE);
    //private static final Pattern CLAW_SCRIPT_OBJ_VALUE_PATTERN = Pattern.compile(CLAW_SCRIPT_PATTERN_OBJ_VALUE_SOURCE);
    private static final Pattern CLAW_SCRIPT_OBJ_VALUE_ONLY_PATTERN = Pattern.compile(CLAW_SCRIPT_PATTERN_OBJ_VALUE_ONLY_SOURCE);
    private static final Pattern CLAW_SCRIPT_PATTERN = Pattern.compile(CLAW_SCRIPT_PATTERN_SOURCE);
    private static final Pattern CLAW_SCRIPT_SIMPLE_ASSIGN_PATTERN = Pattern.compile(CLAW_SCRIPT_SIMPLE_ASSIGN_SOURCE);
    private static final Pattern CLAW_SCRIPT_PATTERN_ARRAY_VAL_PATTERN = Pattern.compile(CLAW_SCRIPT_PATTERN_ARRAY_VAL_SOURCE);
    private static final Pattern CLAW_SCRIPT_PATTERN_ARRAY_VAL_EACH_PATTERN = Pattern.compile(CLAW_SCRIPT_PATTERN_ARRAY_VAL_EACH_SOURCE);
    private static final Pattern CLAW_SCRIPT_PATTERN_MAP_VAL_EACH_PATTERN = Pattern.compile(CLAW_SCRIPT_PATTERN_MAP_VAL_EACH_SOURCE);

    /**
     *  this method will scan the input claw script {@code clawScript}, and
     *  do the compiler work. finlay, this method will return {@link ClawMeta}
     *  to caller, the caller can run {@link ClawMeta#doSet()} method to
     *  execute the "claw script"
     *
     *  the claw script is very simple and stupid, you can just input a script like this:
     *
     *  {@link AutoMechanics#CLAW_SCRIPT_PATTERN_SOURCE}
     *
     * @param clawScript the origin claw script
     * @param params the params
     * @return the result.
     * @throws ClawScriptScanException exception.
     */
    public static List<ClawMeta> scan(String clawScript, Object[] params, Method targetMethod) throws ClawScriptScanException {

        // first step is parse the script to "meta group"
        List<String> metaGroupList = matchAll(clawScript);

        if (metaGroupList == null || metaGroupList.isEmpty()) {
            PSLogger.error("claw script syntax error:" + clawScript);
            return null;
        }

        // second step is handle the "meta group" to clawMeta
        List<ClawMeta> clawMetas = new ArrayList<>();
        for (String metaGroup : metaGroupList) {
            ClawMeta clawMeta = scanEach(metaGroup, params, targetMethod);
            if (clawMeta != null) {
                clawMetas.add(clawMeta);
            }
        }

        return clawMetas;
    }

    /**
     *  parse the "meta group" to Claw Meta data
     *
     * @param params the params
     * @param metaGroup the "meta group"
     * @return the claw meta {@link ClawMeta}
     * @throws ClawScriptScanException exception
     */
    private static ClawMeta scanEach(String metaGroup, Object[] params, Method targetMethod) throws ClawScriptScanException {

        // PARAMS = OBJ_VAL
        Matcher matcher = CLAW_SCRIPT_PARAM_CHOOSE_PATTERN.matcher(metaGroup);
        String paramChooseToken = null;
        if (matcher.find()) {
            paramChooseToken = matcher.group();
        }
        if (UTILS.isNullOrEmpty(paramChooseToken)) {
            throw new ClawScriptScanException("[param choose] claw script syntax error:" + metaGroup);
        }

        // debug
        PSLogger.info("get the params choose token:" + paramChooseToken);

        matcher = CLAW_SCRIPT_SIMPLE_ASSIGN_PATTERN.matcher(metaGroup);
        if (matcher.find()) {
            return handleSimpleAssignGroup(metaGroup, params, targetMethod);
        }

        // check the params
        String paramsOrderToken = paramChooseToken.substring(1, paramChooseToken.indexOf('.'));
        int paramsOrder = UTILS.safeParseInt(paramsOrderToken, -1);
        if(paramsOrder < 0 || paramsOrder >= params.length) {
            throw new ClawScriptScanException(
                    String.format("[param choose] claw script's param order is out of bound, find: %d,%d",
                    paramsOrder, params.length));
        }
        Object target = params[paramsOrder];
//        if (target == null) {
//            throw new ClawScriptScanException("[param choose] the target param is null, the order is:" + paramsOrder);
//        }
        if (target == null) {
            Class<?> targetClass = determineTargetParamClass(targetMethod, paramsOrder);
            if (targetClass == null) {
                throw new ClawScriptScanException("[new object] can not determine the target class by params:" + paramChooseToken);
            }

            target = newInstance(targetClass);

            // init the target object
            params[paramsOrder] = target;
        }

        // get the target's field name
        String fieldClawChain = paramChooseToken.substring(paramsOrderToken.length() + 2);

        if (UTILS.isNullOrEmpty(fieldClawChain) || fieldClawChain.length() == 0) {
            throw new ClawScriptScanException("[param choose] claw script syntax error, could not find the filed chain:" + metaGroup);
        }

        // get field claw chain.
        List<String> fieldsChain = Arrays.asList(fieldClawChain.split("\\."));

        // get the claw meta info
        ClawMeta clawMeta = handleFieldClawChain(target, fieldsChain);

        // handle the target val here
        handleTargetValue(clawMeta, metaGroup);

        // set the target params
        clawMeta.setParams(params);

        // the params order
        clawMeta.setParamOrder(paramsOrder);

        // the target method
        clawMeta.setTargetMethod(targetMethod);

        // return
        return clawMeta;
    }

    private static Object newInstance(Class<?> targetClass) {
        if (targetClass == null) {
            throw new ClawScriptScanException("[new object] the target class is null");
        }
        try {
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ClawScriptScanException("[new object] sorry, the class can not Instant:" + targetClass.getName());
        }
    }

    /**
     *  this method handle the script meta group like:
     *  {@link AutoMechanics#CLAW_SCRIPT_SIMPLE_ASSIGN_SOURCE}
     *
     * @param metaGroup the origin meta group
     * @param params the params
     * @return the target {@link ClawMeta}
     */
    private static ClawMeta handleSimpleAssignGroup(String metaGroup, Object[] params, Method targetMethod) {
        Matcher matcher = CLAW_SCRIPT_PARAM_CHOOSE_PATTERN.matcher(metaGroup);
        String paramsChooseToken = null;
        if (matcher.find()) {
            paramsChooseToken = matcher.group();
        }
        if (UTILS.isNullOrEmpty(paramsChooseToken)) {
            PSLogger.error("impossible null:" + metaGroup);
            return null;
        }

        String paramsChooseOrderToken = paramsChooseToken.substring(1);
        int paramOrder = UTILS.safeParseInt(paramsChooseOrderToken, -1);
        if (paramOrder < 0 || paramOrder >= params.length) {
            throw new ClawScriptScanException("[simple assign] params order is invalid:" + paramsChooseOrderToken);
        }

        // dispatch array\list\set\map here
        if (handleComplexAssignGroup(metaGroup, params, paramOrder, targetMethod)) {
            return null;
        }

//        if (target == null) {
//            throw new ClawScriptScanException("[simple assign] sorry, the target object is null for your choose:" + paramsChooseOrderToken);
//        }

        ClawMeta clawMeta = new ClawMeta();
        handleTargetValue(clawMeta, metaGroup);

        Class<?> targetClass = determineTargetParamClass(targetMethod, paramOrder);
        Object targetVal = Transformer.trans(clawMeta.getVal(), targetClass);
        params[paramOrder] = targetVal;

        return null;
    }

    /**
     *  handle the complex assign:
     *  Array
     *  List
     *  Set
     *  Map
     *
     * @param metaGroup the meta group
     * @param params the origin params
     * @return true is handled, or false, the {@link AutoMechanics#handleSimpleAssignGroup(String, Object[], Method)}
     *         will continue handle;
     */
    @SuppressWarnings("unchecked")
    private static boolean handleComplexAssignGroup(String metaGroup, Object[] params, int paramOrder, Method targetMethod) {
        // determine array or map type here ~
        if (CLAW_SCRIPT_PATTERN_MAP_VAL_EACH_PATTERN.matcher(metaGroup).find()) {
            Map<String, String> mapKvs = extractMapItems(metaGroup);
        } else if (CLAW_SCRIPT_PATTERN_ARRAY_VAL_EACH_PATTERN.matcher(metaGroup).find()) {
            // get the arrays item.
            List<String> arrayItems = extractArrayItems(metaGroup);

            // determine target class
            Class<?> targetClass = determineTargetParamClass(targetMethod, paramOrder);
            // array ?
            if (targetClass.isArray()) {
                // get the item class
                Class<?> itemClass = targetClass.getComponentType();
                List<Object> arrayItemList = new ArrayList<>();
                for (String item : arrayItems) {
                    Object obj = Transformer.trans(item, itemClass);
                    arrayItemList.add(obj);
                }

                // which type
                switch (itemClass.getName()) {
                    case "int": {
                        int[] array = new int[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = Integer.valueOf(arrayItemList.get(i).toString());
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    case "java.lang.Integer": {
                        Integer[] array = new Integer[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = Integer.valueOf(arrayItemList.get(i).toString());
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    case "char": {
                        char[] array = new char[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = (char) arrayItemList.get(i);
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    case "java.lang.Character": {
                        Character[] array = new Character[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = (char) arrayItemList.get(i);
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    case "boolean": {
                        boolean[] array = new boolean[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = (boolean) arrayItemList.get(i);
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    case "java.lang.Boolean": {
                        Boolean[] array = new Boolean[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = (boolean) arrayItemList.get(i);
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    case "short": {
                        short[] array = new short[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = (short) arrayItemList.get(i);
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    case "java.lang.Short": {
                        Short[] array = new Short[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = (short) arrayItemList.get(i);
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    case "long": {
                        long[] array = new long[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = (long) arrayItemList.get(i);
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    case "java.lang.Long": {
                        Long[] array = new Long[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = (long) arrayItemList.get(i);
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    case "float": {
                        float[] array = new float[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = (float) arrayItemList.get(i);
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    case "java.lang.Float": {
                        Float[] array = new Float[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = (float) arrayItemList.get(i);
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    case "double": {
                        double[] array = new double[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = (double) arrayItemList.get(i);
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    case "java.lang.Double": {
                        Double[] array = new Double[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = (double) arrayItemList.get(i);
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    case "byte": {
                        byte[] array = new byte[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = (byte) arrayItemList.get(i);
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    case "java.lang.Byte": {
                        Byte[] array = new Byte[arrayItemList.size()];
                        for (int i = 0; i < arrayItemList.size(); i ++) {
                            array[i] = (byte) arrayItemList.get(i);
                        }
                        params[paramOrder] = array;
                        break;
                    }
                    default: {
                        throw new ClawScriptScanException("[dispatch array type]: unknown array type");
                    }
                }
            }
            // set ?
            else if (Set.class.isAssignableFrom(targetClass)) {
                // determine the target type
                Class<?> targetType = determineItemType(targetMethod, paramOrder);
                Set pSet = new HashSet();
                for (String item : arrayItems) {
                    pSet.add(Transformer.trans(item, targetType));
                }
                // set the params
                params[paramOrder] = pSet;
            }
            // list ?
            else if (List.class.isAssignableFrom(targetClass)) {
                // determine the target type
                Class<?> targetType = determineItemType(targetMethod, paramOrder);
                List pList = new ArrayList();
                for (String item : arrayItems) {
                    pList.add(Transformer.trans(item, targetType));
                }
                // set the params
                params[paramOrder] = pList;
            } else {
                throw new ClawScriptScanException("[unknown array] array? set? list?");
            }
        } else {
            return false;
        }
        return true;
    }

    private static Class<?> determineTargetParamClass(Method targetMethod, int paramsOrder) {
        Class<?>[] css = targetMethod.getParameterTypes();
        if (paramsOrder >= css.length) {
            throw new ClawScriptScanException("[determine target class] out of bound for param order:" + paramsOrder + " method:" + targetMethod);
        }
        return css[paramsOrder];
    }

    private static Class<?> determineItemType(Method targetMethod, int paramsOder) {
        // get the type of item
        Type[] types = targetMethod.getGenericParameterTypes();
        if (paramsOder >= types.length) {
            throw new ClawScriptScanException("[determine item type] out of bound for param order:" + paramsOder + " method:" + targetMethod);
        }

        Type targetType = types[paramsOder];
        if (!(targetType instanceof ParameterizedType)) {
            throw new ClawScriptScanException("[determine item type] impossible target type:" + targetType);
        }
        String typeName = ((ParameterizedType) targetType).getActualTypeArguments()[0].getTypeName();
        switch (typeName) {
            case "java.lang.Byte":
                return Byte.class;
            case "java.lang.Boolean":
                return Boolean.class;
            case "java.lang.Short":
                return Short.class;
            case "java.lang.Integer":
                return Integer.class;
            case "java.lang.Float":
                return Float.class;
            case "java.lang.Double":
                return Double.class;
            case "java.lang.Long":
                return Long.class;
            case "java.lang.String":
                return String.class;
        }
        throw new ClawScriptScanException("[determine item type] unknown type:" + typeName);
    }

    private static Map<String, String> extractMapItems(String metaGroup) {
        throw new UnsupportedOperationException("sorry, the map not support yet!");
    }

    private static List<String> extractArrayItems(String metaGroup) {
        // get the value;
        Matcher matcher = CLAW_SCRIPT_PATTERN_ARRAY_VAL_PATTERN.matcher(metaGroup);
        String arrayVal = null;
        if (matcher.find()) {
            arrayVal = matcher.group();
        }

        if (UTILS.isNullOrEmpty(arrayVal)) {
            throw new ClawScriptScanException("[array claw] impossible null:" + metaGroup);
        }

        List<String> arrayValItems = new ArrayList<>();
        matcher = CLAW_SCRIPT_PATTERN_ARRAY_VAL_EACH_PATTERN.matcher(metaGroup);
        String arrayValItemsToken = null;
        if (matcher.find()) {
            arrayValItemsToken = matcher.group();
        }
        if (arrayValItemsToken == null) {
            throw new ClawScriptScanException("[array item token] impossible null:" + metaGroup);
        }
        arrayValItemsToken = arrayValItemsToken.substring(1);
        arrayValItemsToken = arrayValItemsToken.substring(0, arrayValItemsToken.length() - 1);

        StringBuilder curItem = new StringBuilder();
        Deque<Character> tokenQueue = new LinkedList<>();
        boolean push = false;
        for (char token : arrayValItemsToken.toCharArray()) {
            if (token == '[') {
                push = true;
                curItem = new StringBuilder(); // ensure this is the first token
            } else if (token == ']') {
                push = false;
                // drain the queue
                while (!tokenQueue.isEmpty()) {
                    curItem.insert(0, tokenQueue.pop());
                }
                arrayValItems.add(curItem.toString());
            } else if (push) {
                tokenQueue.push(token);
            }
        }

        if (arrayValItems.isEmpty()) {
            throw new ClawScriptScanException("[array item token] impossible empty:" + metaGroup);
        }

        return arrayValItems;
    }

    /**
     *  this method need to get the user's target value.
     *
     * @param clawMeta the claw info.
     * @param metaGroup the meta group
     * @throws ClawScriptScanException exception
     */
    private static void handleTargetValue(ClawMeta clawMeta, String metaGroup) throws ClawScriptScanException {

        // get the target val.
        Matcher matcher = CLAW_SCRIPT_OBJ_VALUE_ONLY_PATTERN.matcher(metaGroup);
        String val = null;
        if (matcher.find()) {
            val = matcher.group();
        }

        if (UTILS.isNullOrEmpty(val)) {
            throw new ClawScriptScanException("[val claw] could not get the target val with meta group:" + metaGroup);
        }

        // remove the \"\"
        val = val.replaceAll("\"", "");

        // set the val
        clawMeta.setVal(val);

        // debug
        PSLogger.info(String.format("[val claw] get the target val :[%s] with meta group: [%s]", val, metaGroup));
    }

    /**
     *  this method will traversal the target's field. find the target field which name
     *  equals {@code fieldsChain}, the caller will get an {@link ClawScriptScanException}
     *  if the target field not find.
     *
     * @param object the target object;
     * @param fieldsChain the field claw chain
     * @return the {@link ClawMeta}
     * @throws ClawScriptScanException exception
     */
    private static ClawMeta handleFieldClawChain(Object object, List<String> fieldsChain) throws ClawScriptScanException {

        // determine the target field here, claw ...
        PSLogger.info("start to determine the target field with claw field chain:" + fieldsChain);

        // the current object.
        Object currentObj = object;
        Field currentField = matchField(fieldsChain.get(0), object.getClass());

        for (int i = 1; i < fieldsChain.size(); i ++) {
            if (currentField == null) {
                throw new ClawScriptScanException(
                        "[determine field] could not get the target field which field name equals:" + fieldsChain.get(i - 1));
            }

            // get the target object here
            boolean accessible = true;
            try {
                if (!currentField.isAccessible()) {
                    currentField.setAccessible(true);
                    accessible = false;
                }

                // the object may be null
                Object tObj = currentField.get(currentObj);
                if (tObj == null) {
                    Class<?> fClass = currentField.getType();
                    tObj = newInstance(fClass);
                    currentField.set(currentObj, tObj);
                }
                currentObj = tObj;
            } catch (Exception e) {
                 throw new ClawScriptScanException(
                         "[determine field] error while get the target field object:" + UTILS.getErrorMsg(e));
            } finally {
                if (!accessible) {
                    currentField.setAccessible(false);
                }
            }

            // deep claw
            currentField = matchField(fieldsChain.get(i), currentField.getType());
        }

        if (currentField == null) {
            throw new ClawScriptScanException(
                    "[determine field] could not determine target field:" + fieldsChain);
        }

        // get the claw handler
        ObjectClawDefine objectClawDefine = ObjectClawDefine.ObjectClawFactory.dispatchClaw(currentField);

        if (objectClawDefine == null) {
            throw new ClawScriptScanException("[dispatch claw handler] could not dispatch to target claw handler, ensure" +
                                                      "the field is primitive");
        }

        ClawMeta clawMeta = new ClawMeta();
        clawMeta.setClawDefine(objectClawDefine);
        clawMeta.setClassField(currentField);
        clawMeta.setObject(currentObj);

        return clawMeta;
    }

    /**
     *  this method will return a {@link Field} of the target object {@code obj} to caller.
     *  this method is stupid, the caller will get an exception of {@link ClawScriptScanException}
     *  if the target object's {@link Class} has not exist the target field which field name equals {@code fieldName}
     *
     * @param fieldName the target field name
     * @param targetClass the field type
     * @return  the result filed.
     * @throws ClawScriptScanException any exception will be wrap as an {@link ClawScriptScanException}
     */
    private static Field matchField(String fieldName, Class<?> targetClass) throws ClawScriptScanException {
        if (targetClass == null) {
            throw new ClawScriptScanException(
                    "the target class is null. maybe you searching the field of this object which name equals:" + fieldName);
        }

        if (UTILS.isNullOrEmpty(fieldName)) {
            throw new ClawScriptScanException("the target field name is null at matchField.");
        }

        // get the target field
        try {
            return targetClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new ClawScriptScanException(
                    "could not get the Field by field name:" + fieldName + " cause:" + UTILS.getErrorMsg(e));
        }
    }

    /**
     *  using {@link AutoMechanics#CLAW_SCRIPT_PATTERN} to match the source.
     *
     *
     * @param originClawScript the source script
     * @return the match group list
     */
    private static List<String> matchAll(String originClawScript) {
        if (UTILS.isNullOrEmpty(originClawScript)) {
            PSLogger.error("the origin slaw script is null or empty ~");
            return Collections.emptyList();
        }
        Matcher matcher = CLAW_SCRIPT_PATTERN.matcher(originClawScript);

        List<String> matchResult = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group();
            matchResult.add(group);
        }

        return matchResult;
    }

}
