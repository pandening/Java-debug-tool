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


package io.javadebug.core.utils;

public class ConsoleUtils {

    /**
     *  get the console window's width
     *
     *
     *   NOTICE:
     *     THIS METHOD WILL RISE SIGNAL:
     *         SIGTTIN / SIGTTOUT
     *     THEN , THE JVM WILL (BACKEND MODE) STOP TO RUN ......
     *
     * @return the console's width
     */
    public static int getConsoleWidth() {
//        int width = jline.TerminalFactory.get().getWidth();
//        return width > 40 ? 40 : width;
        return 80;
    }

    /**
     *  get the console window's height
     *
     * @return the console's height
     */
    @Deprecated
    public static int getConsoleHeight() {
//        return jline.TerminalFactory.get().getHeight();
        return -1;
    }

    /**
     *  draw a line with fixed width
     *
     * @return line
     */
    public static String newLineFixConsoleWidth() {
        int width = getConsoleWidth();
        StringBuilder lsb = new StringBuilder();
        for (int i = 0; i < width; i ++) {
            lsb.append("-");
        }
        return lsb.toString();
    }

    public static void main(String[] args) {
        System.out.println(newLineFixConsoleWidth());
        System.out.println("ok");
        System.out.println(newLineFixConsoleWidth());
    }

}
