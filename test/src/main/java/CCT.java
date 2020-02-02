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


import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class CCT {

    private static OutputStream os = System.out;

    public static void main(String[] args) throws Exception {


        String output =
                        "I am hujian, I am a coder using java lang, this is a project\n" +
                        "for dynamic debug for java programmer, it also provide  some\n" +
                        "command for application profiler, like thread command,   and\n" +
                        "the command cputime is a very special command for users, the\n" +
                        "command will hold the client 30 secs for profile your   apps\n" +
                        "thanks, repeat\n" +
                        "I am hujian, I am a coder using java lang, this is a project\n" +
                        "for dynamic debug for java programmer, it also provide  some\n" +
                        "command for application profiler, like thread command,   and\n" +
                        "the command cputime is a very special command for users, the\n" +
                        "command will hold the client 30 secs for profile your   apps\n" +
                        "thanks\n";


        while (true) {
            os.write(output.getBytes());


            TimeUnit.SECONDS.sleep(1);

            System.out.print("\033[H\033[2J");
            System.out.flush();

        }


    }

}
