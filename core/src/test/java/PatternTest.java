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


import java.util.regex.Pattern;

/**
 * Created on 2019/4/26 23:09.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class PatternTest {


    public static void main(String[] args) {

        Pattern pattern = Pattern.compile(".*String.*");


        String a = "java.lang.String";
        String b = "string";
        String c = "String";
        String d = "Strings";
        String e = "string";

        System.out.println(pattern.matcher(a).matches());
        System.out.println(pattern.matcher(b).matches());
        System.out.println(pattern.matcher(c).matches());
        System.out.println(pattern.matcher(d).matches());
        System.out.println(pattern.matcher(e).matches());


    }

}
