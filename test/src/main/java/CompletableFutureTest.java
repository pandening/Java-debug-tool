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


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class CompletableFutureTest {

    public static void main(String[] args) throws ExecutionException, InterruptedException {


        CompletableFuture<String> f1 = new CompletableFuture<>();
        CompletableFuture<String> f2 = new CompletableFuture<>();
        CompletableFuture<Void> f3 = CompletableFuture.allOf(f1, f2);


        f3.thenAccept(new Consumer<Void>() {
            @Override
            public void accept(Void aVoid) {

                String fa = f1.getNow(null);
                String fb = f2.getNow(null);

                System.out.println(fa + fb);
            }
        });

        CompletableFuture.allOf(f1, f2).thenCompose(new Function<Void, CompletionStage<String>>() {
            @Override
            public CompletionStage<String> apply(Void aVoid) {
                String fa = f1.getNow(null);
                String fb = f2.getNow(null);
                CompletableFuture<String> ret = new CompletableFuture<>();
                ret.complete(fa + fb);
                return ret;
            }
        }).whenComplete(new BiConsumer<String, Throwable>() {
            @Override
            public void accept(String s, Throwable throwable) {
                System.out.println("s=" + s);
            }
        });

        //TimeUnit.SECONDS.sleep(10);

        f1.complete("f1");

        //TimeUnit.SECONDS.sleep(10);
        f2.completeExceptionally(new IllegalStateException());
        f2.complete("f2");


        //f3.get();  throw exception

        TimeUnit.SECONDS.sleep(10);

    }

}
