package me.kpavlov.await4j;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class Async {

    private static final Thread.Builder virtualThreadBuilder = Thread.ofVirtual()
        .name("async-virtual-", 0);

    public static void await(ThrowingRunnable block) {
        Objects.requireNonNull(block, "Block should not be null");
        try {
            final var failureHolder = new AtomicReference<Throwable>();
            virtualThreadBuilder.start(() -> {
                    try {
                        ThrowingRunnable
                            .toRunnable(block)
                            .run();
                    } catch (RuntimeException | Error e) {
                        failureHolder.set(e);
                    }
                })
                .join();
            final Throwable throwable = failureHolder.get();
            if (throwable instanceof Error e) {
                throw e;
            } else if (throwable instanceof RuntimeException re) {
                throw re;
            } else {
                throw new IllegalStateException("Unexpected Throwable: " + throwable, throwable);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted virtual thread", e);
        }
    }

    public static <T> T await(Callable<T> block) throws RuntimeException {
        Objects.requireNonNull(block, "Callable should not be null");
        try {
            final var resultHolder = new AtomicReference<Result<T>>();
            virtualThreadBuilder.start(() -> {
                final Result<T> result = callWithErrorHandling(block);
                resultHolder.set(result);
            }).join();
            final Result<T> result = resultHolder.get();
            if (result.isSuccess()) {
                return result.getOrThrow();
            } else {
                final Throwable failure = result.failure();
                if (failure instanceof RuntimeException re) {
                    throw re;
                } else if (failure instanceof Error e) {
                    throw e;
                } else {
                    throw new IllegalStateException("Unexpected throwable in call Result:" + failure, failure);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted virtual thread", e);
        }
    }

    /**
     * Executes a block of code with error handling.
     * <p>
     * This method attempts to call the provided {@link Callable} block, handling
     * various exceptions that may be thrown during its execution. It specifically
     * handles {@link InterruptedException}, {@link ExecutionException}, and
     * other general exceptions by rethrowing them as {@link RuntimeException}.
     * </p>
     *
     * @param block the block of code to execute
     * @param <T>   the type of result returned by the block
     * @return the result of the block
     * @throws RuntimeException if an {@link InterruptedException},
     *                          {@link ExecutionException}, or any other exception occurs
     */
    private static <T> Result<T> callWithErrorHandling(Callable<T> block) {
        try {
            return Result.success(block.call());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            return Result.failure(
                new RuntimeException("Can't execute async task: interrupted", e)
            );
        } catch (ExecutionException | CompletionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                // re-throw RuntimeException as it is
                return Result.failure(re);
            } else if (cause instanceof Error error) {
                // re-throw Error as it is
                return Result.failure(error);
            } else {
                return Result.failure(new RuntimeException("Can't execute async task: exception", cause));
            }
        } catch (RuntimeException | Error e) {
            return Result.failure(e);
        } catch (Exception e) {
            return Result.failure(new RuntimeException("Can't execute async task: exception", e));
        }
    }

    public static <T> T await(Future<T> future) {
        return await(() -> future.get());
    }

    public static <T> T await(CompletableFuture<T> completableFuture) {
        return await(completableFuture::join);
    }

}
