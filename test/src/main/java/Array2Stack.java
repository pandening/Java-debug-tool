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


import java.util.ArrayList;

public class Array2Stack {

    public static void main(String[] args) {

        ArrayStack arrayStack = new ArrayStack(2);

        System.out.println("size:" + arrayStack.size());

        arrayStack.push(1);

        System.out.println("size:" + arrayStack.size());

        arrayStack.push(2);

        System.out.println("size:" + arrayStack.size());

        traversalStack(arrayStack);
    }

    private static void traversalStack(ArrayStack arrayStack) {
        while (arrayStack.size() != 0) {
            System.out.println(arrayStack.pop());
        }
    }

}

class ArrayStack {

    private int topIndex = -1;
    private int array[];

    ArrayStack(int capacity) {
        this.array = new int[capacity];
    }

    public int pop() {
        if (topIndex == -1) {
            throw new IllegalStateException("the stack is empty!");
        }
        return array[topIndex --];
    }

    public void push(int val) {
        if (topIndex == array.length - 1) {
            throw new ArrayIndexOutOfBoundsException("max:" + array.length);
        }
        array[++ topIndex] = val;
    }

    public int size() {
        return topIndex + 1;
    }

}

