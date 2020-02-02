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

import java.util.Objects;

/**
 * A tuple that holds three non-null values.
 *
 * @param <T1> The type of the first non-null value held by this tuple
 * @param <T2> The type of the second non-null value held by this tuple
 * @param <T3> The type of the third non-null value held by this tuple
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
public class Tuple3<T1, T2, T3> extends Tuple2<T1, T2> {

    private static final long serialVersionUID = -4430274211524723033L;

    T3 t3;

    Tuple3(T1 t1, T2 t2, T3 t3) {
        super(t1, t2);
        this.t3 = Objects.requireNonNull(t3, "t3");
    }

    /**
     * Create a {@link Tuple3} with the given objects.
     *
     * @param t1   The first value in the tuple. Not null.
     * @param t2   The second value in the tuple. Not null.
     * @param t3   The third value in the tuple. Not null.
     * @param <T1> The type of the first value.
     * @param <T2> The type of the second value.
     * @param <T3> The type of the third value.
     * @return The new {@link Tuple3}.
     */
    public static <T1, T2, T3> Tuple3<T1, T2, T3> of(T1 t1, T2 t2, T3 t3) {
        return new Tuple3<>(t1, t2, t3);
    }

    /**
     * Type-safe way to get the third object of this Tuples
     *
     * @return The third object
     */
    public T3 getT3() {
        return t3;
    }

    public void setT3(T3 t3) {
        this.t3 = t3;
    }

    @Override
    public Object get(int index) {
        switch (index) {
            case 0:
                return t1;
            case 1:
                return t2;
            case 2:
                return t3;
            default:
                return null;
        }
    }

    @Override
    public Object[] toArray() {
        return new Object[]{t1, t2, t3};
    }

    @Override
    public boolean equals( Object o) {
        if (this == o) return true;
        if (!(o instanceof Tuple3)) return false;
        if (!super.equals(o)) return false;

        @SuppressWarnings("rawtypes")
        Tuple3 tuple3 = (Tuple3) o;

        return t3.equals(tuple3.t3);
    }

    @Override
    public int size() {
        return 3;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + t3.hashCode();
        return result;
    }
}
