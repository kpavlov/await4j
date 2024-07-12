package me.kpavlov.await4j;

import java.util.function.Function;

/**
 * Represents the result of an operation that can either succeed with a value or fail with a throwable.
 * <p>
 * This class provides methods to create instances of success or failure and to manipulate these results
 * through mapping functions. It allows checking whether the result is successful or failed, and provides
 * access to the success value or failure throwable.
 * </p>
 *
 * @param <T> the type of the value in case of success
 */
public class Result<T> {

    private final Throwable throwable;
    private final T value;

    /**
     * Private constructor to create a Result instance.
     *
     * @param throwable the throwable indicating failure, or {@code null} for success
     * @param value     the value representing success, or {@code null} if there's a throwable
     */
    private Result(Throwable throwable, T value) {
        this.throwable = throwable;
        this.value = value;
    }

    /**
     * Creates a Result instance representing a failure with the specified throwable.
     *
     * @param throwable the throwable indicating failure
     * @param <T>       irrelevant type parameter (can be omitted)
     * @return a Result instance representing failure with the specified throwable
     */
    public static <T> Result<T> failure(Throwable throwable) {
        return new Result<>(throwable, null);
    }

    /**
     * Creates a Result instance representing success with the specified value.
     *
     * @param value the value representing success
     * @param <T>   the type of the value
     * @return a Result instance representing success with the specified value
     */
    public static <T> Result<T> success(T value) {
        return new Result<>(null, value);
    }

    /**
     * Checks if this instance represents a successful outcome.
     *
     * @return {@code true} if successful, otherwise {@code false}
     */
    public boolean isSuccess() {
        return throwable == null;
    }

    /**
     * Checks if this instance represents a failed outcome.
     *
     * @return {@code true} if failed, otherwise {@code false}
     */
    public boolean isFailure() {
        return throwable != null;
    }

    /**
     * Maps the success value using the provided function.
     *
     * @param <R>      the type of the mapped result
     * @param function the mapping function for success value
     * @return the result of applying the mapping function
     * @throws IllegalStateException if this instance represents failure
     */
    public <R> Result<R> map(Function<T, R> function) {
        if (throwable != null) {
            throw new IllegalStateException("Can't map result with Throwable. Use mapThrowable if needed.");
        }
        return success(function.apply(value));
    }

    public <R> Result<R> mapThrowable(Function<Throwable, Throwable> function) {
        if (throwable == null) {
            throw new IllegalStateException("Can't map empty Throwable. Use map if needed.");
        }
        return failure(function.apply(throwable));
    }

    /**
     * Returns the failure throwable, if any.
     *
     * @return the throwable indicating failure, or {@code null} if success
     */
    public Throwable failure() {
        return throwable;
    }

    /**
     * Returns the success value, or {@code null} if failed.
     *
     * @return the value representing success, or {@code null} if failure
     */
    public T getOrNull() {
        return value;
    }

    /**
     * Returns the success value, or throws an exception if failed.
     *
     * @return the value representing success
     * @throws IllegalStateException if this instance represents failure
     */
    public T getOrThrow() {
        if (isSuccess()) {
            return value;
        } else {
            throw new IllegalStateException("Failure result", throwable);
        }
    }

    /**
     * Returns the success value, or a default value if failed.
     *
     * @param defaultValue the default value to return if failed
     * @return the value representing success, or the default value if failed
     */
    public T getOrDefault(T defaultValue) {
        return isSuccess() ? value : defaultValue;
    }

    /**
     * Returns a string representation of this Result instance.
     *
     * @return a string representation of this Result instance
     */
    @Override
    public String toString() {
        return isSuccess() ? "Result{" + value + '}' : "Result{throwable=" + throwable + '}';
    }
}
