package me.kpavlov.await4j;

import java.util.concurrent.CompletionException;

/**
 * Functional interface similar to {@link Runnable}, but allows throwing checked exceptions.
 */
@FunctionalInterface
public interface ThrowingRunnable {

    /**
     * Executes the runnable code that may throw a checked exception.
     *
     * @throws Exception if an exception occurs during execution
     */
    void run() throws Exception;

    /**
     * Converts a {@link ThrowingRunnable} into a standard {@link Runnable} by handling checked exceptions.
     *
     * @param throwingRunnable the throwing runnable to convert
     * @return a {@link Runnable} that wraps the throwing runnable and converts checked exceptions to unchecked {@link CompletionException}
     */
    static Runnable toRunnable(ThrowingRunnable throwingRunnable) {
        return () -> {
            try {
                throwingRunnable.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                throw new CompletionException("Can't execute async task: interrupted", e);
            } catch (Exception e) {
                throw new CompletionException(e); // Wrap checked exceptions in RuntimeException
            }
        };
    }
}
