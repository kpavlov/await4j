package me.kpavlov.await4j;

/**
 * Utility class for working with virtual threads introduced in Project Loom.
 * <p>
 * This class contains utility methods for working with virtual threads, such as asserting
 * that the current thread is a virtual thread. Virtual threads are lightweight threads
 * introduced in Project Loom, designed to allow for a large number of threads to be created
 * and managed more efficiently than traditional platform threads.
 * </p>
 * <p>
 * More information about virtual threads can be found at
 * <a href="https://openjdk.org/projects/loom/">Project Loom</a>.
 * </p>
 */
public class LoomUtils {

    /**
     * Private constructor to prevent instantiation.
     * <p>
     * This class is not meant to be instantiated. It contains static utility methods
     * for working with virtual threads.
     * </p>
     */
    private LoomUtils() {
        // noop
    }

    /**
     * Asserts that the current thread is a virtual thread.
     * <p>
     * This method checks whether the current thread is a virtual thread and throws
     * an {@link AssertionError} if it is not. This is useful in scenarios where
     * you want to ensure that certain code is executed on a virtual thread.
     * </p>
     * <p>
     * A virtual thread is a lightweight thread introduced in
     * <a href="https://openjdk.org/projects/loom/">Project Loom</a>,
     * which allows for a large number of threads to be created and managed more
     * efficiently than traditional platform threads.
     * </p>
     *
     * @throws AssertionError if the current thread is not a virtual thread
     */
    public static void assertVirtualThread() {
        assert Thread.currentThread().isVirtual() : "Expect to run on Virtual Thread";
    }
}
