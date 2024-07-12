package me.kpavlov.await4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AsyncRunnableTest extends AbstractAsyncTest {

    @ParameterizedTest
    @MethodSource("threadBuilders")
    void awaitRunnableToCompletesSuccessfully(Thread.Builder threadBuilder) throws InterruptedException {
        final var completed = new AtomicBoolean();
        threadBuilder.start(() -> {
            final var originalThread = Thread.currentThread();
            logger.info("Parent thread: {}", originalThread);
            final var threadLocal = defineThreadLocal();
            // when
            Async.await(() -> {
                // check invariants
                checkVirtualThreadInvariants(originalThread, threadLocal);
                // mark call completed
                completed.compareAndSet(false, true);
            });
        }).join();
        // then
        assertThat(completed).isTrue();
    }

    @ParameterizedTest
    @MethodSource("awaitHandlesThrowable")
    void awaitRunnableShouldHandleException(Thread.Builder threadBuilder, Exception exception) throws InterruptedException {
        // When & Then
        final ThrowingRunnable runnable = () -> {
            throw exception;
        };
        threadBuilder.start(() ->
            assertThatThrownBy(() -> Async.await(runnable))
                .isInstanceOf(RuntimeException.class)
                .hasCause(exception)
        ).join();
    }

    @Test
    void shouldRethrowError() {
        // When & Then
        final var error = new Error("Expected");
        final ThrowingRunnable runnable = () -> {
            throw error;
        };
        assertThatThrownBy(
            () -> Async.await(runnable)
        ).isSameAs(error);
    }


}
