package me.kpavlov.await4j;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsyncCallableTest extends AbstractAsyncTest {

    static Object[][] awaitCallableToCompleteSuccessfully() {
        return TestUtils.combine(threadBuilders(), "OK", null);
    }

    @ParameterizedTest
    @MethodSource("awaitCallableToCompleteSuccessfully")
    void awaitCallableToCompleteSuccessfully(final Thread.Builder threadBuilder,
                                             final String expectedResult) throws InterruptedException {
        final var completed = new AtomicBoolean();
        threadBuilder.start(() -> {
            final var originalThread = Thread.currentThread();
            logger.info("Parent thread: {}", originalThread);
            final var threadLocal = defineThreadLocal();
            // when
            final var result = Async.await(() -> {
                // check invariants
                checkVirtualThreadInvariants(originalThread, threadLocal);
                completed.compareAndSet(false, true);
                // return result
                return expectedResult;
            });
            // then
            assertThat(result).isEqualTo(expectedResult);
        }).join();
        assertThat(completed).isTrue();
    }


    @ParameterizedTest
    @MethodSource("threadBuilders")
    void awaitSupplierReThrowsRuntimeException(final Thread.Builder threadBuilder) throws InterruptedException {
        // given
        final var runtimeException = new RuntimeException("Failure");
        final Callable<String> callable = () -> {
            throw runtimeException;
        };
        // When & Then
        threadBuilder.start(() -> assertThatThrownBy(() -> Async.await(callable)).isSameAs(runtimeException)).join();
    }

    @ParameterizedTest
    @MethodSource("exceptions")
    void awaitHandlesThrowable(Exception throwable) {
        // given
        final Callable<String> callable = () -> {
            throw throwable;
        };
        // When & Then
        try {
            Async.await(callable);
            Assertions.fail("Expected to fail with exception: %s", (Object) throwable);
        } catch (Exception e) {
            assertThat(e)
                .isInstanceOf(CompletionException.class)
                .hasCause(throwable);
        }
    }

    @Test
    void shouldRethrowError() {
        // When & Then
        final var error = new Error("Expected");
        final Callable<String> callable = () -> {
            throw error;
        };
        assertThatThrownBy(
            () -> Async.await(callable)
        ).isSameAs(error);
    }
}

