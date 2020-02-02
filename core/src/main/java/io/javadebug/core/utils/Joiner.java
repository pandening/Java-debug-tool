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

import java.io.IOException;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * Created on 2019/4/21 00:30.
 *
 * @author <a href="H.J"> HuJian </a>
 */
public class Joiner {

    private final String separator;

    public static Joiner on(String separator) {
        return new Joiner(separator);
    }

    public static Joiner on(char separator) {
        return new Joiner(String.valueOf(separator));
    }

    private Joiner(String separator) {
        this.separator = (String) Preconditions.checkNotNull(separator);
    }

    private Joiner(Joiner prototype) {
        this.separator = prototype.separator;
    }

    
    public <A extends Appendable> A appendTo(A appendable, Iterable<?> parts) throws IOException {
        return this.appendTo(appendable, parts.iterator());
    }

    
    public <A extends Appendable> A appendTo(A appendable, Iterator<?> parts) throws IOException {
        Preconditions.checkNotNull(appendable);
        if(parts.hasNext()) {
            appendable.append(this.toString(parts.next()));

            while(parts.hasNext()) {
                appendable.append(this.separator);
                appendable.append(this.toString(parts.next()));
            }
        }

        return appendable;
    }

    
    public final <A extends Appendable> A appendTo(A appendable,  Object first,  Object second, Object... rest) throws IOException {
        return this.appendTo(appendable, iterable(first, second, rest));
    }

    
    public final StringBuilder appendTo(StringBuilder builder, Iterable<?> parts) {
        return this.appendTo(builder, parts.iterator());
    }

    
    public final StringBuilder appendTo(StringBuilder builder, Iterator<?> parts) {
        try {
            this.appendTo((Appendable)builder, (Iterator)parts);
            return builder;
        } catch (IOException var4) {
            throw new AssertionError(var4);
        }
    }

    
    public final StringBuilder appendTo(StringBuilder builder, Object[] parts) {
        return this.appendTo((StringBuilder)builder, (Iterable)Arrays.asList(parts));
    }

    
    public final StringBuilder appendTo(StringBuilder builder,  Object first,  Object second, Object... rest) {
        return this.appendTo(builder, iterable(first, second, rest));
    }

    public final String join(Iterable<?> parts) {
        return this.join(parts.iterator());
    }

    public final String join(Iterator<?> parts) {
        return this.appendTo(new StringBuilder(), parts).toString();
    }

    public final String join(Object[] parts) {
        return this.join((Iterable)Arrays.asList(parts));
    }

    public final String join( Object first,  Object second, Object... rest) {
        return this.join(iterable(first, second, rest));
    }

    public Joiner useForNull(final String nullText) {
        return new Joiner(this) {
            CharSequence toString( Object part) {
                return (CharSequence)(part == null?nullText:Joiner.this.toString(part));
            }

            public Joiner useForNull(String nullTextx) {
                throw new UnsupportedOperationException("already specified useForNull");
            }

            public Joiner skipNulls() {
                throw new UnsupportedOperationException("already specified useForNull");
            }
        };
    }

    public Joiner skipNulls() {
        return new Joiner(this) {
            public <A extends Appendable> A appendTo(A appendable, Iterator<?> parts) throws IOException {

                Object part;
                while(parts.hasNext()) {
                    part = parts.next();
                    if(part != null) {
                        appendable.append(Joiner.this.toString(part));
                        break;
                    }
                }

                while(parts.hasNext()) {
                    part = parts.next();
                    if(part != null) {
                        appendable.append(Joiner.this.separator);
                        appendable.append(Joiner.this.toString(part));
                    }
                }

                return appendable;
            }

            public Joiner useForNull(String nullText) {
                throw new UnsupportedOperationException("already specified skipNulls");
            }

            public Joiner.MapJoiner withKeyValueSeparator(String kvs) {
                throw new UnsupportedOperationException("can't use .skipNulls() with maps");
            }
        };
    }

    CharSequence toString(Object part) {
        return (CharSequence)(part instanceof CharSequence?(CharSequence)part:part.toString());
    }

    private static Iterable<Object> iterable(final Object first, final Object second, final Object[] rest) {
        return new AbstractList<Object>() {
            public int size() {
                return rest.length + 2;
            }

            public Object get(int index) {
                switch(index) {
                    case 0:
                        return first;
                    case 1:
                        return second;
                    default:
                        return rest[index - 2];
                }
            }
        };
    }

    public static final class MapJoiner {
        private final Joiner joiner;
        private final String keyValueSeparator;

        private MapJoiner(Joiner joiner, String keyValueSeparator) {
            this.joiner = joiner;
            this.keyValueSeparator = keyValueSeparator;
        }
        
        public StringBuilder appendTo(StringBuilder builder, Map<?, ?> map) {
            return this.appendTo((StringBuilder)builder, (Iterable)map.entrySet());
        }

        public String join(Map<?, ?> map) {
            return this.join((Iterable)map.entrySet());
        }
        
        public <A extends Appendable> A appendTo(A appendable, Iterable<? extends Map.Entry<?, ?>> entries) throws IOException {
            return this.appendTo(appendable, entries.iterator());
        }
        
        public <A extends Appendable> A appendTo(A appendable, Iterator<? extends Map.Entry<?, ?>> parts) throws IOException {
            if (appendable == null) {
                throw new IllegalArgumentException("param must not null");
            }
            if(parts.hasNext()) {
                Map.Entry<?, ?> entry = (Map.Entry)parts.next();
                appendable.append(this.joiner.toString(entry.getKey()));
                appendable.append(this.keyValueSeparator);
                appendable.append(this.joiner.toString(entry.getValue()));

                while(parts.hasNext()) {
                    appendable.append(this.joiner.separator);
                    Map.Entry<?, ?> e = (Map.Entry)parts.next();
                    appendable.append(this.joiner.toString(e.getKey()));
                    appendable.append(this.keyValueSeparator);
                    appendable.append(this.joiner.toString(e.getValue()));
                }
            }

            return appendable;
        }
        
        public StringBuilder appendTo(StringBuilder builder, Iterable<? extends Map.Entry<?, ?>> entries) {
            return this.appendTo(builder, entries.iterator());
        }
        
        public StringBuilder appendTo(StringBuilder builder, Iterator<? extends Map.Entry<?, ?>> entries) {
            try {
                this.appendTo((Appendable)builder, (Iterator)entries);
                return builder;
            } catch (IOException var4) {
                throw new AssertionError(var4);
            }
        }
        
        public String join(Iterable<? extends Map.Entry<?, ?>> entries) {
            return this.join(entries.iterator());
        }
        
        public String join(Iterator<? extends Map.Entry<?, ?>> entries) {
            return this.appendTo(new StringBuilder(), entries).toString();
        }

        public Joiner.MapJoiner useForNull(String nullText) {
            return new Joiner.MapJoiner(this.joiner.useForNull(nullText), this.keyValueSeparator);
        }
    }
    
}
