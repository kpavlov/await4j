package me.kpavlov.await4j;

import java.util.concurrent.*;

import static me.kpavlov.await4j.Async.await;

@SuppressWarnings("SameParameterValue")
class Sample {

    public static void main(String... args) {
        long startMillis = System.currentTimeMillis();

        final var result1 = await(() -> slowCode("One"));
        final var result2 = await(slowCompletableFuture("Two"));
        final var result3 = await(slowFuture("Three"));

        System.out.println("result1 = " + result1);
        System.out.println("result2 = " + result2);
        System.out.println("result3 = " + result3);

        System.out.println("Total duration = " + (System.currentTimeMillis() - startMillis) + "ms");
    }

    private static <T> CompletableFuture<T> slowCompletableFuture(T result) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Running slow code on thread: " + Thread.currentThread());
            LoomUtils.assertVirtualThread();
            sleepOneSecond();
            return result;
        });
    }

    private static <T> Future<T> slowFuture(T result) {
        try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            return executor.submit(() -> {
                System.out.println("Running slow Future on thread: " + Thread.currentThread());
                sleepOneSecond();
                return result;
            });
        }
    }

    private static <T> T slowCode(T result) {
        LoomUtils.assertVirtualThread();
        System.out.println("Running slow code on thread: " + Thread.currentThread());
        sleepOneSecond();
        return result;
    }

    private static void sleepOneSecond() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
