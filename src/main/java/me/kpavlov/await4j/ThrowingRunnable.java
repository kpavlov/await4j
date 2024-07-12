package me.kpavlov.await4j;

/**
 * Functional interface similar to {@link Runnable}, but allows throwing checked exceptions.
 */
@FunctionalInterface
public interface ThrowingRunnable {

    /**
     * Converts a {@link ThrowingRunnable} into a standard {@link Runnable} by handling checked exceptions.
     *
     * @param throwingRunnable the throwing runnable to convert
     * @return a {@link Runnable} that wraps the throwing runnable and converts checked exceptions to {@link RuntimeException}
     */
    static Runnable toRunnable(ThrowingRunnable throwingRunnable) {
        return () -> {
            try {
                throwingRunnable.run();
            } catch (RuntimeException e) {
                throw e; // Rethrow RuntimeExceptions directly
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                throw new RuntimeException("Can't execute async task: interrupted", e);
            } catch (Exception e) {
                throw new RuntimeException(e); // Wrap checked exceptions in RuntimeException
            }
        };
    }

    /**
     * Executes the runnable code that may throw a checked exception.
     *
     * @throws Exception if an exception occurs during execution
     */
    void run() throws Exception;
}
