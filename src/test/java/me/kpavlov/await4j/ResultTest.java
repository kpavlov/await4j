package me.kpavlov.await4j;

import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ResultTest {

    @Test
    void testSuccess() {
        Result<Integer> result = Result.success(42);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.getOrNull()).isEqualTo(42);
        assertThat(result.getOrThrow()).isEqualTo(42);
        assertThat(result.getOrDefault(100)).isEqualTo(42);
        assertThat(result.toString()).isEqualTo("Result{42}");
    }

    @Test
    void testFailure() {
        RuntimeException exception = new RuntimeException("Test exception");
        Result<Integer> result = Result.failure(exception);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure()).isEqualTo(exception);
        assertThatThrownBy(result::getOrThrow)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failure result")
            .hasCause(exception);
        assertThat(result.getOrDefault(100)).isEqualTo(100);
        assertThat(result.toString()).isEqualTo("Result{throwable=" + exception + "}");
    }

    @Test
    void testMapSuccess() {
        Result<Integer> result = Result.success(42);
        Function<Integer, String> mapper = num -> "Result: " + num;

        Result<String> mappedResult = result.map(mapper);

        assertThat(mappedResult.isSuccess()).isTrue();
        assertThat(mappedResult.getOrNull()).isEqualTo("Result: 42");
    }

    @Test
    void testMapFailure() {
        RuntimeException exception = new RuntimeException("Test exception");
        Result<Integer> result = Result.failure(exception);
        Function<Integer, String> mapper = num -> "Result: " + num;

        assertThatThrownBy(() -> result.map(mapper))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Can't map result with Throwable. Use mapThrowable if needed.");
    }

    @Test
    void testMapThrowableSuccess() {
        RuntimeException originalException = new RuntimeException("Original exception");
        Result<Integer> result = Result.failure(originalException);
        Function<Throwable, Throwable> mapper = throwable -> new RuntimeException("Mapped exception", throwable);

        Result<Integer> mappedResult = result.mapThrowable(mapper);

        assertThat(mappedResult.isFailure()).isTrue();
        assertThat(mappedResult.failure()).isInstanceOf(RuntimeException.class)
            .hasMessage("Mapped exception")
            .hasCause(originalException);
    }

    @Test
    void testMapThrowableFailure() {
        Result<Integer> result = Result.success(42);
        Function<Throwable, Throwable> mapper = throwable -> new RuntimeException("Mapped exception", throwable);

        assertThatThrownBy(() -> result.mapThrowable(mapper))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Can't map empty Throwable. Use map if needed.");
    }
}
