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


import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Signal;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;
import reactor.util.function.Tuple2;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class ReactorTest {

    static void monoVars() {

//        Mono<String> m1 = Mono.just("mono-11");
//        m1.flatMap(new Function<String, Mono<Optional<Boolean>>>() {
//            @Override
//            public Mono<Optional<Boolean>> apply(String s) {
//                if ("mono-1".equals(s)) {
//                    return Mono.just(Optional.empty());
//                } else {
//                    throw new IllegalStateException("test");
//                    //return Mono.just(Optional.of(true));
//                }
//            }
//        }).flatMap(new Function<Optional<Boolean>, Mono<Integer>>() {
//            @Override
//            public Mono<Integer> apply(Optional<Boolean> aBoolean) {
//                return null;
//            }
//        }).subscribe();

//        String key = "message";
//        Mono.just("hello")
//                .flatMap(new Function<String, Mono<? extends String>>() {
//                    @Override
//                    public Mono<? extends String> apply(String s) {
//                        return Mono.subscriberContext().map(new Function<Context, String>() {
//                            @Override
//                            public String apply(Context context) {
//                                return s + context.get(key);
//                            }
//                        });
//                    }
//                })//.subscriberContext(ctx -> ctx.put(key, "Reactor"))
//                .flatMap(new Function<String, Mono<? extends String>>() {
//                    @Override
//                    public Mono<? extends String> apply(String s) {
//                        return Mono.subscriberContext().map(new Function<Context, String>() {
//                            @Override
//                            public String apply(Context context) {
//                                return s + " " + context.get(key);
//                            }
//                        });
//                    }
//                }).subscriberContext(context -> {
//                    context = context.put("m2", "hello");
//                    return context.put(key, "Word");
//        }).subscribe();


        Mono.just("start").flatMap(new Function<String, Mono<Integer>>() {
            @Override
            public Mono<Integer> apply(String s) {
                Mono<Integer> m1 = Mono.just(1);
                return m1.subscriberContext(new Function<Context, Context>() {
                    @Override
                    public Context apply(Context context) {
                        return context.put("m1", m1);
                    }
                });
            }
        }).flatMap(new Function<Integer, Mono<Boolean>>() {
            @Override
            public Mono<Boolean> apply(Integer integer) {
                return Mono.just(true);
            }
        }).flatMap(new Function<Boolean, Mono<String>>() {
            @Override
            public Mono<String> apply(Boolean aBoolean) {
                return Mono.subscriberContext().map(new Function<Context, String>() {
                    @Override
                    public String apply(Context context) {
                        return "ok";
                    }
                });
            }
        }).subscribe();



        try {
            TimeUnit.SECONDS.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    static void testMono() throws InterruptedException {

        Mono<String> publisher = Mono.create(new Consumer<MonoSink<String>>() {
            @Override
            public void accept(MonoSink<String> stringMonoSink) {
                //stringMonoSink.success("hello");
                stringMonoSink.error(new IllegalStateException("test"));
            }
        }).doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                System.out.println("error:" + throwable);
            }
        }).doOnNext(new Consumer<String>() {
            @Override
            public void accept(String s) {
                System.out.println("success:" + s);
            }
        }).subscribeOn(Schedulers.elastic());

        // do it
        publisher.subscribe();
        //Mono.fromFuture(null);

        TimeUnit.SECONDS.sleep(1);
    }

    static void testCompletableFuture() {

    }


    public static void main(String[] args) throws InterruptedException {

        //testMono();

        monoVars();

    }


}
