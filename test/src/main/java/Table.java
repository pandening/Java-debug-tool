////
////  ========================================================================
////  Copyright (c) 2018-2019 HuJian/Pandening soft collection.
////  ------------------------------------------------------------------------
////  All rights reserved. This program and the accompanying materials
////  are made available under the terms of the #{license} Public License #{version}
////  EG:
////      The Eclipse Public License is available at
////      http://www.eclipse.org/legal/epl-v10.html
////
////      The Apache License v2.0 is available at
////      http://www.opensource.org/licenses/apache2.0.php
////
////  You may elect to redistribute this code under either of these licenses.
////  You should bear the consequences of using the software (named 'java-debug-tool')
////  and any modify must be create an new pull request and attach an text to describe
////  the change detail.
////  ========================================================================
////
//
//
//import de.vandermeer.asciitable.AsciiTable;
//import de.vandermeer.asciithemes.a7.A7_Grids;
//import de.vandermeer.asciithemes.a8.A8_Grids;
//import de.vandermeer.asciithemes.u8.U8_Grids;
//
//import static io.javadebug.core.Constant.ANSI_RED;
//
//public class Table {
//
//    private static String output =
//            "I am hujian, I am a coder using java lang, this is a project\n" +
//                    "for dynamic debug for java programmer, it also provide  some\n" +
//                    "command for application profiler, like thread command,   and\n" +
//                    "the command cputime is a very special command for users, the\n" +
//                    "command will hold the client 30 secs for profile your   apps\n" +
//                    "thanks, repeat\n" +
//                    "I am hujian, I am a coder using java lang, this is a project\n" +
//                    "for dynamic debug for java programmer, it also provide  some\n" +
//                    "command for application profiler, like thread command,   and\n" +
//                    "the command cputime is a very special command for users, the\n" +
//                    "command will hold the client 30 secs for profile your   apps\n" +
//                    "thanks\n";
//
//
//    public static void main(String[] args) {
//
////        AsciiTable at = new AsciiTable();
////
////        at.addRule();
////
////        at.addRow(null,"hello");
////
////        at.addRule();
////
////        at.addRow("world", output);
////
////        at.addRule();
////
////        System.out.println(at.render());
//
//        test1();
//
//    }
//
//    private static void test1() {
//        AsciiTable at = new AsciiTable();
//        at.addRule();
//        at.addRow(null, "aa");
//        at.addRule();
//        at.addRow("rc 21", "\u001B[31mrc 22 ss aa\u001B[31m");
//        at.addRule();
//        at.getContext().setWidth(10);
//
//        //System.out.println(at.render());
//
//        at.getContext().setGrid(A7_Grids.minusBarPlusEquals());
//        System.out.println(at.render());
//        System.out.println("\u001B[31m A \u001B[31m");
//
////        at.getContext().setGrid(A8_Grids.lineDoubleBlocks());
////        System.out.println(at.render());
////
////        at.getContext().setGrid(U8_Grids.borderDoubleLight());
////        System.out.println(at.render());
////
////        at.getContext().setGrid(U8_Grids.borderDouble());
////        System.out.println(at.render());
//    }
//
//
//}
