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


import io.javadebug.test.TestClass;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReturnTest implements IFace {

//    static ExecutorService executorService = Executors.newCachedThreadPool();
    public static int  staticVal = 100;
    private String open = "aab";
    private Integer iiii =new Integer(100);
    private static Boolean aBoolean = true;
    private boolean aBooleana = false;
    private long al = 100L;
    private Long all = new Long(1000L);
    private Float aFloata = new Float(1.000);
    private float plpslsps = 1.2334f;
    private Double aDoubleaaaa = new Double(123456.0001);
    private Double ssspppl = 1.234;
    private double sss = ssspppl;
    private char ccccccppp = '1';
    private  Character sssCCC = new Character(ccccccppp);


    class InnerCalssA {

        public InnerCalssA() {

        }

        private int callMe(int in) {
            String show = "in = " + in;
            if (in > 5) {
                return 10;
            } else {
                return 1;
            }
        }

    }

    static class InnerClassB {
        private int callMe(int in) {
            String show = "in = " + in;
            if (in > 5) {
                return 10;
            } else {
                return 1;
            }
        }
    }


    @Resource
    private TestClass testClass;

    public static void say(int a) {
        int b = a * 10;
        System.out.println("hello:" + b);
        //return b;
    }

    private int say12(String msg) {
        String copy = msg + " -> what ?";
        System.out.println(copy);
        return getSubIntVal(null);
    }

    private String call123(int a, String b, StringBuilder c) {
        String ret = c.append(a).append(b).toString();
        if (ret.length() > 10) {
            return "out_of_flow";
        }
        return ret;
    }

    private void call10(C c) {
        System.out.println(c);
    }

    private static void call234(int in) {
        in = in - 1;
    }

    public int getIntVal(int in) {
//        if (in < 4) {
//            System.out.println("in < 7, return");
//            throw new UnsupportedOperationException("test");
//        }

        call10(new C());
        Integer integerss = new Integer(1);
        int b = integerss;
        Integer pp = in;
        Float f = 0.1f;
        char chchch = 'a';
        Character character = new Character('s');
        float ff = f;
        Double d = new Double(1.0);
        double dop = d * 1.0;
        boolean abbbb = true;
        Boolean abbsnns = true;
        call234(in);
        say12("test say");
        staticVal = in;
        open = "open=>" + in;

        call123(10, "123", new StringBuilder());

        EvilModel evilModel = new EvilModel(123, new String[]{"A", "B", "C"}, EvilEnum.A);

        // todo : 这种放在开始的地方的代码执行起来有问题
        // todo: BUGBUGBUG.....
        getSubIntVal(null);

        InnerCalssA innerCalssA = new InnerCalssA();
        InnerClassB innerClassB = new InnerClassB();
        innerCalssA.callMe(in);

        CompletableFuture<String> stringCompletableFuture = new CompletableFuture<>();
        stringCompletableFuture.complete("hello world");

        List<String> strings = new ArrayList<>();
        strings.add("a");
        strings.add("b");
        strings.add("c");
        strings.add("d");
        strings.add("e");
        strings.add("f");

        CompletableFuture<Integer> integerFuture = stringCompletableFuture.thenCompose(s -> {
            CompletableFuture<Integer> integerCompletableFuture = new CompletableFuture<>();
            int len = s.length();
            len += strings.stream().map(str -> {
                if (str != null && !str.isEmpty()) {
                    return str.length();
                } else {
                    return 0;
                }
            }).collect(Collectors.toList()).stream().mapToInt(l -> l).sum();
            integerCompletableFuture.complete(len);
            return integerCompletableFuture;
        });

        System.out.println(integerFuture.getNow(0));

        Function<String, Integer> intConverter = s -> {
            if (s == null || s.isEmpty()) {
                return -1;
            }
            for (char c : s.toCharArray()) {
                if (c < '0' || c > '9') {
                    return -1;
                }
            }
            return Integer.parseInt(s);
        };

        Function<Integer, String> strConverter = integer -> {
            if (integer < 0) {
                return "low";
            }
            String ret = "result-";
            if (integer == 0) {
                ret += "0";
            } else {
                ret += integer;
            }
            return ret;
        };

        System.out.println(intConverter.apply("1234"));
        System.out.println(strConverter.apply(1010));

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                String s = "sss";

                System.out.println(s);

                Random random = new Random();

                if (random.nextInt(10) > 5) {
                    System.out.println("> 5");
                } else {
                    System.out.println("< 5");
                }
            }
        };

        //executorService.submit(runnable);

        if (in == 5) {
            String msg = null;
            // produce npe
            in += msg.length();
        }

        List<Integer> integers = new ArrayList<>();
        integers.forEach(System.out::println);

        long startTime = System.currentTimeMillis() + fibonacci(2);
        String strTag = "the return/throw line test tag";
        if (in < 0) {
            return strTag.charAt(0);
        } else if (in == 0) {
            return 1000;
        }
        // > 0
        if (in < 2) {
            double dbVal = 1.1;
            return (int) (dbVal + 100);
        } else if (in == 2) {
            float fVal = 1.2f;
            return (int) (fVal + 200);
        }
        // > 2
        if (in % 2 == 0) {
            Random random = new Random();
            int rdm = random.nextInt(100);
            if (rdm >= 50) {
                throw new IllegalArgumentException("npe test");
            } else if (rdm <= 20) {
                throw new NullPointerException("< 20");
            }
            // end time
            long end = System.currentTimeMillis();
            long cost = startTime - end;
            int ret = testClass.test(in);
            return (int) (rdm * 10 + ret + (cost / 1000));
        } else {
            ParamModel paramModel = new ParamModel();
            paramModel.setIntVal(in);
            paramModel.setDoubleVal(1.0 * in);
            int subVal = 0;
            getSubIntVal(paramModel);

            int a = say12("i am in") + call(in);
            a = 1000;

            if (subVal == 100) {
                throw new IllegalArgumentException("err occ with in:" + subVal);
            }

            throw new IllegalStateException("error occ with in:" + in);
        }
    }

    /**
     *  不支持递归函数
     *
     * @param n
     * @return
     */
    public int fibonacci(int n) {
        if (n < 0) {
            return -1;
        }
        if (n == 0) {
            return 0;
        }
        if (n <= 2) {
            return 1;
        }
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    public int getSubIntVal(ParamModel paramModel) {
        if (paramModel == null) {
            return -1;
        }

        Function<Integer, String> strConverter = integer -> {
            if (integer < 0) {
                return "low";
            }
            String ret = "result-";
            if (integer == 0) {
                ret += "0";
            } else {
                ret += integer;
            }
            return ret;
        };

        System.out.println("hello world" + strConverter.apply(1234567890));
        if (paramModel.getIntVal() <= 0) {
            return (int) paramModel.getDoubleVal();
        } else if (paramModel.getIntVal() <= 5) {
            return 100;
        } else if (paramModel.getIntVal() <= 8) {
            return 200;
        } else {
            throw new RuntimeException("ill");
        }
    }

    public static void main(String[] args) {

        Class<?> cls = ReturnTest.class;

        cls.getDeclaredClasses();

        System.out.println(Arrays.asList(cls.getDeclaredClasses()));


        new Thread(new Runnable() {
            private Random random = new Random();
            private ReturnTest returnTest = new ReturnTest();

            @Override
            public void run() {
                while (true) {
                    try {
                        System.err.println(returnTest.getIntVal(random.nextInt(10)));
                    } catch (Exception e) {
                        //e.printStackTrace();
                        System.out.println("error:" + e.getMessage());
                    } finally {
                        try {
                            TimeUnit.MILLISECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    @Override
    public int call(int p) {
        p = (int) (p + 1 * (p / 1.234));
        if (p > 10) {
            return 100;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                String s = "sss";

                System.out.println(s);

                Random random = new Random();

                if (random.nextInt(10) > 5) {
                    System.out.println("> 5");
                } else {
                    System.out.println("< 5");
                }
            }
        };

        //executorService.submit(runnable);

        return p;
    }

    static class EvilModel {
        public EvilModel(int evilInt, String[] evilStringArrays, EvilEnum evilEnum) {
            this.evilInt = evilInt;
            this.evilStringArrays = evilStringArrays;
            this.evilEnum = evilEnum;
        }

        private int evilInt;
        private String[] evilStringArrays;
        private EvilEnum evilEnum;
    }

    enum EvilEnum {
        A, B, C;
    }

}
