package me.kpavlov.await4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static me.kpavlov.await4j.TestUtils.sleepMillis;
import static me.kpavlov.await4j.TestUtils.sleepOneSecond;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsyncCompletableFutureTest extends AbstractAsyncTest {

    static Object[][] awaitCompletableFutureToCompleteSuccessfully() {
        return TestUtils.combine(threadBuilders(), "OK", null);
    }

    @ParameterizedTest
    @MethodSource("awaitCompletableFutureToCompleteSuccessfully")
    void awaitCompletableFutureToCompleteSuccessfully(Thread.Builder threadBuilder, String expectedResult) throws InterruptedException {
        final var completed = new AtomicBoolean();
        threadBuilder.start(() -> {
            final var originalThread = Thread.currentThread();
            logger.info("Parent thread: {}", originalThread);
            final var threadLocal = defineThreadLocal();

            final CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
                checkThreadLocalInvariant(threadLocal);
                completed.compareAndSet(false, true);
                return expectedResult;
            });
            // when
            final var result = Async.await(completableFuture);
            // then
            assertThat(result).isEqualTo(expectedResult);
        }).join();
        assertThat(completed).isTrue();
    }


    @ParameterizedTest
    @MethodSource("threadBuilders")
    void awaitSupplierReThrowsRuntimeException(Thread.Builder threadBuilder) throws InterruptedException {
        // Given
        RuntimeException runtimeException = new RuntimeException("Failure");
        final CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            sleepMillis(100);
            throw runtimeException;
        });
        // When & Then
        threadBuilder.start(() -> assertThatThrownBy(
            () -> Async.await(completableFuture)
        ).isSameAs(runtimeException)).join();
    }

    @Test
    void shouldRethrowError() {
        // Given
        final var error = new Error("Expected");
        final CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            sleepMillis(100);
            throw error;
        });
        // When & Then
        assertThatThrownBy(() ->
            Async.await(completableFuture)
        ).isSameAs(error);
    }

    @Test
    void shouldShortCircuitError() {
        // Given
        final var error = new Error("Expected");
        // When & Then
        assertThatThrownBy(() ->
            Async.await(CompletableFuture.failedFuture(error))
        ).isSameAs(error);
    }

    @Test
    void shouldShortCircuitResult() {
        // Given
        final var result = "OK";
        // When & Then
        assertThat(
            Async.await(CompletableFuture.completedFuture(result))
        ).isSameAs(result);
    }
}

