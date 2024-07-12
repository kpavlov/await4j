package me.kpavlov.await4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractAsyncTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected static Exception[] exceptions() {
        return new Exception[]{
            new Exception("Failure"),
            new InterruptedException("Interrupt")
        };
    }

    protected static Thread.Builder[] threadBuilders() {
        return new Thread.Builder[]{
            Thread.ofVirtual(),
            Thread.ofPlatform(),
        };
    }

    protected static ThreadLocal<Integer> defineThreadLocal() {
        return InheritableThreadLocal.withInitial(() -> 42);
    }

    protected static void checkThreadLocalInvariant(ThreadLocal<Integer> threadLocal) {
        assertThat(threadLocal.get())
            .as("ThreadLocals MUST be inherited")
            .isEqualTo(42);
    }

    protected static Object[][] awaitHandlesThrowable() {
        return TestUtils.combine(threadBuilders(), exceptions());
    }

    /**
     * Checks the invariants for a virtual thread.
     * <p>
     * This method asserts that the current thread is a virtual thread, that its name starts with the "async-virtual-" prefix,
     * and that it is different from the original thread. It also checks that a given {@link ThreadLocal} value is inherited.
     * It uses assertions from AssertJ to verify these conditions and throws an {@link AssertionError} if they are not met.
     * </p>
     *
     * @param originalThread the original thread to compare with the current thread
     * @param threadLocal    the ThreadLocal variable to check for inheritance
     * @throws AssertionError if the current thread is not a virtual thread, if it is the same as the original thread,
     *                        if its name does not start with "async-virtual-", or if the ThreadLocal value is not inherited
     */
    protected void checkVirtualThreadInvariants(Thread originalThread, ThreadLocal<Integer> threadLocal) {
        final var currentThread = Thread.currentThread();
        logger.info("Running Lambda on thread: {}", currentThread);
        assertThat(currentThread.isVirtual())
            .as("Must be running on virtual thread")
            .isTrue();
        assertThat(currentThread)
            .as("Must be running on different thread")
            .isNotSameAs(originalThread);
        assertThat(currentThread.getName())
            .as("Thread name SHOULD start with \"async-\"")
            .startsWith("async-virtual-");
        checkThreadLocalInvariant(threadLocal);
    }
}
