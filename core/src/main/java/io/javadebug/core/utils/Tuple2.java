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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class Tuple2<T1, T2> implements Iterable<Object>, Serializable {

    private static final long serialVersionUID = -3518082018884860684L;

    T1 t1;
    T2 t2;


    /**
     * Create a {@link Tuple2} with the given objects.
     *
     * @param t1   The first value in the tuple. Not null.
     * @param t2   The second value in the tuple. Not null.
     * @param <T1> The type of the first value.
     * @param <T2> The type of the second value.
     * @return The new {@link Tuple2}.
     */
    public static <T1, T2> Tuple2<T1, T2> of(T1 t1, T2 t2) {
        return new Tuple2<>(t1, t2);
    }

    Tuple2(T1 t1, T2 t2) {
        this.t1 = Objects.requireNonNull(t1, "t1");
        this.t2 = Objects.requireNonNull(t2, "t2");
    }

    /**
     * Type-safe way to get the fist object of this Tuples
     *
     * @return The first object
     */
    public T1 getT1() {
        return t1;
    }

    public void setT1(T1 t1) {
        this.t1 = t1;
    }

    /**
     * Type-safe way to get the second object of this Tuples
     *
     * @return The second object
     */
    public T2 getT2() {
        return t2;
    }

    public void setT2(T2 t2) {
        this.t2 = t2;
    }

    /**
     * Get the object at the given index.
     *
     * @param index The index of the object to retrieve. Starts at 0.
     * @return The object or {@literal null} if out of bounds.
     */
    public Object get(int index) {
        switch (index) {
            case 0:
                return t1;
            case 1:
                return t2;
            default:
                return null;
        }
    }

    /**
     * Turn this {@literal Tuples} into a plain Object list.
     *
     * @return A new Object list.
     */
    public List<Object> toList() {
        return Arrays.asList(toArray());
    }

    /**
     * Turn this {@literal Tuples} into a plain Object array.
     *
     * @return A new Object array.
     */
    public Object[] toArray() {
        return new Object[]{t1, t2};
    }

    @Override
    public Iterator<Object> iterator() {
        return Collections.unmodifiableList(toList()).iterator();
    }

    @Override
    public boolean equals( Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Tuple2<?, ?> tuple2 = (Tuple2<?, ?>) o;

        return t1.equals(tuple2.t1) && t2.equals(tuple2.t2);

    }

    @Override
    public int hashCode() {
        int result = size();
        result = 31 * result + t1.hashCode();
        result = 31 * result + t2.hashCode();
        return result;
    }

    /**
     * Return the number of elements in this {@literal Tuples}.
     *
     * @return The size of this {@literal Tuples}.
     */
    public int size() {
        return 2;
    }

    /**
     * A Tuple String representation is the comma separated list of values, enclosed
     * in square brackets.
     * @return the Tuple String representation
     */
    @Override
    public final String toString() {
        return tupleStringRepresentation(toArray()).insert(0, '[').append(']').toString();
    }

    /**
     * Prepare a string representation of the values suitable for a Tuple of any
     * size by accepting an array of elements. This builds a {@link StringBuilder}
     * containing the String representation of each object, comma separated. It manages
     * nulls as well by putting an empty string and the comma.
     *
     * @param values the values of the tuple to represent
     * @return a {@link StringBuilder} initialized with the string representation of the
     * values in the Tuple.
     */
    static StringBuilder tupleStringRepresentation(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            Object t = values[i];
            if (i != 0) {
                sb.append(',');
            }
            if (t != null) {
                sb.append(t);
            }
        }
        return sb;
    }
}

