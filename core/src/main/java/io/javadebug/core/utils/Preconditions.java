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


package io.javadebug.core.utils;

/**
 * Created on 2019/4/21 00:33.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class Preconditions {

    private Preconditions() {
    }

    public static void checkArgument(boolean expression) {
        if(!expression) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkArgument(boolean expression,  Object errorMessage) {
        if(!expression) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
    }

    public static void checkArgument(boolean expression,  String errorMessageTemplate,  Object... errorMessageArgs) {
        if(!expression) {
            throw new IllegalArgumentException(format(errorMessageTemplate, errorMessageArgs));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate, char p1) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{Character.valueOf(p1)}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate, int p1) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{Integer.valueOf(p1)}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate, long p1) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{Long.valueOf(p1)}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate,  Object p1) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{p1}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate, char p1, char p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{Character.valueOf(p1), Character.valueOf(p2)}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate, char p1, int p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{Character.valueOf(p1), Integer.valueOf(p2)}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate, char p1, long p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{Character.valueOf(p1), Long.valueOf(p2)}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate, char p1,  Object p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{Character.valueOf(p1), p2}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate, int p1, char p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{Integer.valueOf(p1), Character.valueOf(p2)}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate, int p1, int p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{Integer.valueOf(p1), Integer.valueOf(p2)}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate, int p1, long p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{Integer.valueOf(p1), Long.valueOf(p2)}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate, int p1,  Object p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{Integer.valueOf(p1), p2}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate, long p1, char p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{Long.valueOf(p1), Character.valueOf(p2)}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate, long p1, int p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{Long.valueOf(p1), Integer.valueOf(p2)}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate, long p1, long p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{Long.valueOf(p1), Long.valueOf(p2)}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate, long p1,  Object p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{Long.valueOf(p1), p2}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate,  Object p1, char p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{p1, Character.valueOf(p2)}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate,  Object p1, int p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{p1, Integer.valueOf(p2)}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate,  Object p1, long p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{p1, Long.valueOf(p2)}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate,  Object p1,  Object p2) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{p1, p2}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate,  Object p1,  Object p2,  Object p3) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{p1, p2, p3}));
        }
    }

    public static void checkArgument(boolean b,  String errorMessageTemplate,  Object p1,  Object p2,  Object p3,  Object p4) {
        if(!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, new Object[]{p1, p2, p3, p4}));
        }
    }

    public static void checkState(boolean expression) {
        if(!expression) {
            throw new IllegalStateException();
        }
    }

    public static void checkState(boolean expression,  Object errorMessage) {
        if(!expression) {
            throw new IllegalStateException(String.valueOf(errorMessage));
        }
    }

    public static void checkState(boolean expression,  String errorMessageTemplate,  Object... errorMessageArgs) {
        if(!expression) {
            throw new IllegalStateException(format(errorMessageTemplate, errorMessageArgs));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate, char p1) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{Character.valueOf(p1)}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate, int p1) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{Integer.valueOf(p1)}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate, long p1) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{Long.valueOf(p1)}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate,  Object p1) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{p1}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate, char p1, char p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{Character.valueOf(p1), Character.valueOf(p2)}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate, char p1, int p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{Character.valueOf(p1), Integer.valueOf(p2)}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate, char p1, long p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{Character.valueOf(p1), Long.valueOf(p2)}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate, char p1,  Object p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{Character.valueOf(p1), p2}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate, int p1, char p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{Integer.valueOf(p1), Character.valueOf(p2)}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate, int p1, int p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{Integer.valueOf(p1), Integer.valueOf(p2)}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate, int p1, long p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{Integer.valueOf(p1), Long.valueOf(p2)}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate, int p1,  Object p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{Integer.valueOf(p1), p2}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate, long p1, char p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{Long.valueOf(p1), Character.valueOf(p2)}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate, long p1, int p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{Long.valueOf(p1), Integer.valueOf(p2)}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate, long p1, long p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{Long.valueOf(p1), Long.valueOf(p2)}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate, long p1,  Object p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{Long.valueOf(p1), p2}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate,  Object p1, char p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{p1, Character.valueOf(p2)}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate,  Object p1, int p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{p1, Integer.valueOf(p2)}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate,  Object p1, long p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{p1, Long.valueOf(p2)}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate,  Object p1,  Object p2) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{p1, p2}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate,  Object p1,  Object p2,  Object p3) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{p1, p2, p3}));
        }
    }

    public static void checkState(boolean b,  String errorMessageTemplate,  Object p1,  Object p2,  Object p3,  Object p4) {
        if(!b) {
            throw new IllegalStateException(format(errorMessageTemplate, new Object[]{p1, p2, p3, p4}));
        }
    }

    
    public static <T> T checkNotNull(T reference) {
        if(reference == null) {
            throw new NullPointerException();
        } else {
            return reference;
        }
    }

    
    public static <T> T checkNotNull(T reference,  Object errorMessage) {
        if(reference == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        } else {
            return reference;
        }
    }

    
    public static <T> T checkNotNull(T reference,  String errorMessageTemplate,  Object... errorMessageArgs) {
        if(reference == null) {
            throw new NullPointerException(format(errorMessageTemplate, errorMessageArgs));
        } else {
            return reference;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate, char p1) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{Character.valueOf(p1)}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate, int p1) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{Integer.valueOf(p1)}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate, long p1) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{Long.valueOf(p1)}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate,  Object p1) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{p1}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate, char p1, char p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{Character.valueOf(p1), Character.valueOf(p2)}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate, char p1, int p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{Character.valueOf(p1), Integer.valueOf(p2)}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate, char p1, long p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{Character.valueOf(p1), Long.valueOf(p2)}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate, char p1,  Object p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{Character.valueOf(p1), p2}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate, int p1, char p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{Integer.valueOf(p1), Character.valueOf(p2)}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate, int p1, int p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{Integer.valueOf(p1), Integer.valueOf(p2)}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate, int p1, long p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{Integer.valueOf(p1), Long.valueOf(p2)}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate, int p1,  Object p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{Integer.valueOf(p1), p2}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate, long p1, char p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{Long.valueOf(p1), Character.valueOf(p2)}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate, long p1, int p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{Long.valueOf(p1), Integer.valueOf(p2)}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate, long p1, long p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{Long.valueOf(p1), Long.valueOf(p2)}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate, long p1,  Object p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{Long.valueOf(p1), p2}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate,  Object p1, char p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{p1, Character.valueOf(p2)}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate,  Object p1, int p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{p1, Integer.valueOf(p2)}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate,  Object p1, long p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{p1, Long.valueOf(p2)}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate,  Object p1,  Object p2) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{p1, p2}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate,  Object p1,  Object p2,  Object p3) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{p1, p2, p3}));
        } else {
            return obj;
        }
    }

    
    public static <T> T checkNotNull(T obj,  String errorMessageTemplate,  Object p1,  Object p2,  Object p3,  Object p4) {
        if(obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, new Object[]{p1, p2, p3, p4}));
        } else {
            return obj;
        }
    }

    
    public static int checkElementIndex(int index, int size) {
        return checkElementIndex(index, size, "index");
    }

    
    public static int checkElementIndex(int index, int size,  String desc) {
        if(index >= 0 && index < size) {
            return index;
        } else {
            throw new IndexOutOfBoundsException(badElementIndex(index, size, desc));
        }
    }

    private static String badElementIndex(int index, int size, String desc) {
        if(index < 0) {
            return format("%s (%s) must not be negative", new Object[]{desc, Integer.valueOf(index)});
        } else if(size < 0) {
            throw new IllegalArgumentException("negative size: " + size);
        } else {
            return format("%s (%s) must be less than size (%s)", new Object[]{desc, Integer.valueOf(index), Integer.valueOf(size)});
        }
    }

    
    public static int checkPositionIndex(int index, int size) {
        return checkPositionIndex(index, size, "index");
    }

    
    public static int checkPositionIndex(int index, int size,  String desc) {
        if(index >= 0 && index <= size) {
            return index;
        } else {
            throw new IndexOutOfBoundsException(badPositionIndex(index, size, desc));
        }
    }

    private static String badPositionIndex(int index, int size, String desc) {
        if(index < 0) {
            return format("%s (%s) must not be negative", new Object[]{desc, Integer.valueOf(index)});
        } else if(size < 0) {
            throw new IllegalArgumentException("negative size: " + size);
        } else {
            return format("%s (%s) must not be greater than size (%s)", new Object[]{desc, Integer.valueOf(index), Integer.valueOf(size)});
        }
    }

    public static void checkPositionIndexes(int start, int end, int size) {
        if(start < 0 || end < start || end > size) {
            throw new IndexOutOfBoundsException(badPositionIndexes(start, end, size));
        }
    }

    private static String badPositionIndexes(int start, int end, int size) {
        return start >= 0 && start <= size?(end >= 0 && end <= size?format("end index (%s) must not be less than start index (%s)", new Object[]{Integer.valueOf(end), Integer.valueOf(start)}):badPositionIndex(end, size, "end index")):badPositionIndex(start, size, "start index");
    }

    static String format(String template,  Object... args) {
        template = String.valueOf(template);
        StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
        int templateStart = 0;

        int i;
        int placeholderStart;
        for(i = 0; i < args.length; templateStart = placeholderStart + 2) {
            placeholderStart = template.indexOf("%s", templateStart);
            if(placeholderStart == -1) {
                break;
            }

            builder.append(template, templateStart, placeholderStart);
            builder.append(args[i++]);
        }

        builder.append(template, templateStart, template.length());
        if(i < args.length) {
            builder.append(" [");
            builder.append(args[i++]);

            while(i < args.length) {
                builder.append(", ");
                builder.append(args[i++]);
            }

            builder.append(']');
        }

        return builder.toString();
    }
    
}
