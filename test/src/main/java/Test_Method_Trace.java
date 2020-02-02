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
 *
 *  This Class use to test the 'mt' command.
 *
 *   run this class, then attach java-debug-tool agent to the jvm and
 *   execute the 'mt' command to test the function of java-debug-tool
 *
 *   1. the method trace base output must be ok.
 *   2. the java-debug-tool's enhance code must be safe for target jvm.
 *   3. the affect of java-debug-tool must be small to target jvm.
 *   4. test option -t
 *          'watch'
 *          'custom'
 *          'throw'
 *          'return'
 *          'record'
 *
 *  mt -c Test_Method_Trace -m #{method} -t #{type} #{options}
 */
public class Test_Method_Trace {


    public static void main(String[] args) {

    }

}
