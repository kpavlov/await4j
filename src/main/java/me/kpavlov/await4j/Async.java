package me.kpavlov.await4j;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@code Async} class provides utilities to execute code asynchronously using virtual threads.
 * It allows running {@link ThrowingRunnable} and {@link Callable} tasks asynchronously, handling exceptions,
 * and returning results in a synchronous manner. It leverages the new virtual threads feature introduced in Java
 * to provide lightweight concurrency.
 * <p>
 * This class offers methods to:
 * <ul>
 *     <li>Run a block of code asynchronously and wait for its completion.</li>
 *     <li>Handle both checked and unchecked exceptions in asynchronous tasks.</li>
 *     <li>Retrieve results from {@link Future} and {@link CompletableFuture} objects.</li>
 * </ul>
 */
public class Async {

    private static final Thread.Builder virtualThreadBuilder = Thread.ofVirtual()
        .name("async-virtual-", 0);

    private Async() {
        // hide public constructor
    }

    /**
     * Executes a block of code asynchronously and waits for its completion.
     *
     * @param block  The code to be executed asynchronously
     * @param millis The maximum time to wait for the block to complete, in milliseconds
     * @throws CompletionException   if the virtual thread is interrupted or throws Exception
     * @throws Error                 if the block throws an Error
     * @throws IllegalStateException if an unexpected Throwable is encountered
     */
    @SuppressWarnings("java:S1181")
    public static void await(ThrowingRunnable block, long millis) {
        Objects.requireNonNull(block, "Block should not be null");
        try {
            final var failureHolder = new AtomicReference<Throwable>();
            virtualThreadBuilder.start(() -> {
                    try {
                        ThrowingRunnable
                            .toRunnable(block)
                            .run();
                    } catch (Error e) {
                        failureHolder.set(e);
                    } catch (Exception e) {
                        failureHolder.set(toRuntimeException(e));
                    }
                })
                .join(millis);
            final Throwable throwable = failureHolder.get();
            switch (throwable) {
                case null -> {
                    // success
                }
                case Error e -> throw e;
                case RuntimeException re -> throw re;
                default -> throw new IllegalStateException("Unexpected Throwable: " + throwable, throwable);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException("Interrupted virtual thread", e);
        }
    }

    /**
     * Executes a block of code asynchronously and waits indefinitely for its completion.
     *
     * @param block The code to be executed asynchronously
     * @throws CompletionException   if the virtual thread is interrupted
     * @throws RuntimeException      if the block throws an exception
     * @throws Error                 if the block throws an Error
     * @throws IllegalStateException if an unexpected Throwable is encountered
     */
    public static void await(ThrowingRunnable block) {
        await(block, 0);
    }

    /**
     * Executes a callable block asynchronously and returns its result.
     *
     * @param <T>    The type of the result
     * @param block  The callable block to be executed asynchronously. <strong>Block should not execute code
     *               that contains synchronized blocks or invokes synchronized methods to avoid scalability issues.</strong>
     * @param millis The maximum time to wait for the callable block to complete, in milliseconds
     * @return The result of the callable block
     * @throws CompletionException   if the virtual thread is interrupted or if the block throws an exception
     * @throws Error                 if the block throws an Error
     * @throws IllegalStateException if an unexpected throwable is encountered in the call result
     */
    public static <T> T await(Callable<T> block, long millis) {
        Objects.requireNonNull(block, "Callable should not be null");
        try {
            final var resultHolder = new AtomicReference<Result<T>>();
            virtualThreadBuilder.start(() -> {
                final Result<T> result = callWithErrorHandling(block);
                resultHolder.set(result);
            }).join(millis);
            final Result<T> result = resultHolder.get();
            if (result.isSuccess()) {
                return result.getOrNull();
            } else {
                final Throwable failure = result.failure();
                switch (failure) {
                    case RuntimeException re -> throw re;
                    case Error e -> throw e;
                    case null, default ->
                        throw new IllegalStateException("Unexpected throwable in call Result:" + failure, failure);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException("Interrupted virtual thread", e);
        }
    }

    /**
     * Executes a callable block asynchronously and returns its result.
     *
     * @param <T>   The type of the result
     * @param block The callable block to be executed asynchronously
     * @return The result of the callable block
     * @throws RuntimeException      if the virtual thread is interrupted or if the block throws an exception
     * @throws Error                 if the block throws an Error
     * @throws IllegalStateException if an unexpected throwable is encountered in the call result
     */
    public static <T> T await(Callable<T> block) {
        return await(block, 0);
    }

    /**
     * Waits for the completion of a Future and returns its result.
     *
     * @param <T>    The type of the result
     * @param future The Future to await
     * @return The result of the Future
     * @throws RuntimeException if the Future completes exceptionally
     */
    public static <T> T await(Future<T> future) {
        if (shortCircuitDoneFuture(future)) return future.resultNow();
        return await(() -> future.get());
    }

    /**
     * Waits for the completion of a Future and returns its result.
     *
     * @param <T>    The type of the result
     * @param future The Future to await
     * @param millis The maximum time to wait for the future to complete, in milliseconds
     * @return The result of the Future
     * @throws RuntimeException if the Future completes exceptionally
     */
    public static <T> T await(Future<T> future, long millis) {
        if (shortCircuitDoneFuture(future)) return future.resultNow();
        return await(() -> future.get(millis, TimeUnit.MILLISECONDS));
    }

    /**
     * Waits for the completion of a CompletableFuture and returns its result.
     *
     * @param <T>               The type of the result
     * @param completableFuture The CompletableFuture to await
     * @return The result of the CompletableFuture
     * @throws RuntimeException if the CompletableFuture completes exceptionally
     */
    public static <T> T await(CompletableFuture<T> completableFuture) {
        if (shortCircuitDoneFuture(completableFuture)) return completableFuture.resultNow();
        return await(completableFuture::join);
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
    @SuppressWarnings("java:S1181")
    private static <T> Result<T> callWithErrorHandling(Callable<T> block) {
        try {
            return Result.success(block.call());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            return Result.failure(
                new CompletionException("Can't execute async task: interrupted", e)
            );
        } catch (Error e) {
            return Result.failure(e);
        } catch (CompletionException | ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof Error) {
                return Result.failure(cause);
            } else {
                return Result.failure(toRuntimeException(cause));
            }
        } catch (Throwable e) {
            return Result.failure(toRuntimeException(e));
        }
    }

    @SuppressWarnings("java:S1181")
    private static <T> boolean shortCircuitDoneFuture(Future<T> future) {
        try {
            if (future.isDone()) {
                if (future.isCancelled()) {
                    throw new CancellationException("Execution is cancelled");
                }
                future.get();
                return true;
            }
            return false;
        } catch (Error e) {
            throw e;
        } catch (ExecutionException e) {
            throw toRuntimeException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException("Interrupted while waiting for future", e);
        }
    }

    private static RuntimeException toRuntimeException(Throwable cause) {
        return switch (cause) {
            case RuntimeException re -> re;
            case Error e -> throw e;
            default -> new CompletionException("Can't execute async task: exception", cause);
        };
    }

}
