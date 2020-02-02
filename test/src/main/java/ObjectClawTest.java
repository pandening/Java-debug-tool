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


import io.javadebug.core.claw.AutoMechanics;
import io.javadebug.core.claw.ClawMeta;
import io.javadebug.core.claw.ObjectFieldInterpreter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ObjectClawTest {

    public static class ClawModelA {

        public ClawModelA() {

        }

        @Override
        public String toString() {
            return "ClawModelA{" +
                           "a=" + a +
                           ", b='" + b + '\'' +
                           ", d=" + d +
                           ", e=" + e +
                           ", f=" + f +
                           ", g=" + g +
                           ", h=" + h +
                           ", i=" + i +
                           ", j =" + j +
                           '}';
        }

        private int a;
        private String b;
        private static ClawModelB c = new ClawModelB();
        private boolean d;
        private char e;
        private short f;
        private long g;
        private float h;
        private double i;
        private ClawModelA j;
        private List<Double> k;

    }

    static class ClawModelB {

        @Override
        public String toString() {
            return "ClawModelB{" +
                           "a =" + a + '\'' +
                           ",b='" + b + '\'' +
                           ", c=" + c +
                           '}';
        }

        private static int a;
        private String b;
        private ClawModelC c = new ClawModelC();
        private int[] d = new int[]{};
    }

    static class ClawModelC {
        @Override
        public String toString() {
            return "ClawModelC{" +
                           "a='" + a + '\'' +
                           '}';
        }

        private String a;
    }

    static void testZero() {
        int a = 100;
        Object[] params = new Object[] {new ClawModelA(), new ClawModelB(), a};
        String clawScript = "p0.a=\"10\",p0.b=\"i am hujian\",p0.c.a=\"100\",p0.c.c.a=\"hello world\"";
        clawScript = "p2 = \"-100\" p0.a=\"-1000\" p0.b = \"i am hujian\" p0.d = \"true\" p0.e = \"b\" p0.f = \"12\" p0.g = \"10010\", p0.h = \"1.234f\" p0.i = \"3.1415926\" p1.a=\"4\" p1.c.a=\"hujian\"";

        List<ClawMeta> clawMetaList = AutoMechanics.scan(clawScript, params, null);

        System.out.println(clawMetaList);

        // run the script
        params = ObjectFieldInterpreter.interpreter(params, clawScript, null);
        assert params != null;
        System.out.println(Arrays.asList(params));
    }

    static void testA() {

        String a = "abc";
        Object[] params = new Object[] {a, new ClawModelA(), new ClawModelB()};
        String script = "p0=\"100\"";

        List<ClawMeta> clawMetaList = AutoMechanics.scan(script, params, null);

        System.out.println(clawMetaList);



    }

    static void testB() {

        int[] a = new int[]{0, 1, 2};
        ClawModelB clawModelB = new ClawModelB();
        clawModelB.d = a;
        String b = "";
        Object[] params = new Object[]{a, b, clawModelB};
        String script = "p0 = \"[[2] [6] [4] [5]]\"";
        //script = "p2= \"{a=[aaa] b=[bbb]}\"";

        //List<ClawMeta> clawMetaList = AutoMechanics.scan(script, params);
        ObjectFieldInterpreter.interpreter(params, script, null);

        System.out.println(Arrays.toString(params));

    }

    static void testMethod(List<Double> doubles) {
//
//        Type type =ObjectClawTest.class.getDeclaredMethod("testMethod", List.class).getGenericParameterTypes()[0];
//        ((ParameterizedType)(type)).getActualTypeArguments()[0]

    }

    static void testParams(int a, String b, List<Double> c, Set<Long> d, ClawModelA pe, int[] pf) throws NoSuchMethodException {

        Method method = ObjectClawTest.class.getDeclaredMethod("testParams", int.class, String.class, List.class, Set.class, ClawModelA.class, int[].class);

        int pa = 100;
        String pb = "hello";
        List<Double> pc = new ArrayList<>();
        Set<Long> pd = new HashSet<>();

        Object[] params = new Object[]{pa, pb, pc, pd};
        params = new Object[]{null, null, null, null, null, null};

        String script = "p0 = \"999\", p1 = \"i am hujian\" p3 = \"[[100],[200]]\" p4.a = \"100\" p4.j.a = \"300\" p5 = \"[[1] [2]]\"";

        ObjectFieldInterpreter.interpreter(params, script, method);

        System.out.println(Arrays.asList(params));

    }

    public static void main(String[] args) throws NoSuchMethodException {

        int[] arr = new int[]{};
        testParams(0, "", null, null, null, arr);

//        Float[] floats = new Float[]{};
//        List<Double> doubles = new ArrayList<>();
//        testMethod(doubles);
//
//        List<String> list = new ArrayList<>();
//        list.add("a");
//        list.add("b");
//        list.add("c");
//        Object[] array = list.toArray();
//        System.out.println(Arrays.asList(array));
//
//
//        //testA();
//        testB();
//
//        String[] a = new String[]{"a ", "b,", "ssC S  s"};
//        int b  =10;
//        System.out.println(Arrays.asList(a));
//        Object[] params = new Object[]{a, b};
//        System.out.println(JacksonUtils.serialize(params));

    }

}
