# Await4J

[![Java CI with Maven](https://github.com/kpavlov/await4j/actions/workflows/maven.yml/badge.svg)](https://github.com/kpavlov/await4j/actions/workflows/maven.yml)
[![CodeQL](https://github.com/kpavlov/await4j/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/kpavlov/await4j/actions/workflows/github-code-scanning/codeql)

_Simplify Java async programming with virtual threads using an async/await style API._

## TL;DR: Features

The `Async` class provides utility methods for executing code asynchronously on virtual threads. It simplifies handling of asynchronous operations and exceptions and provides familiar API.

- **`await(() -> {/* blocking code */})`**:
  Executes a code block on a virtual thread. Handles checked exceptions and wraps them into `RuntimeException`. `RuntimeException` and `Error`are thrown as it is. If any other unexpected `Throwable` occurs, it throws an `IllegalStateException`.

- **`await(Callable<T> block)`**:
  Executes a `Callable<T>` block on a virtual thread and returns the result. Handles `InterruptedException`, `ExecutionException`, and other general exceptions by wrapping them in a `RuntimeException`.

- **`await(Future<T> future)`**:
  Waits for the `Future<T>` to complete and returns its result. Internally calls `await(Callable<T> block)`.

- **`await(CompletableFuture<T> completableFuture)`**:
  Waits for the `CompletableFuture<T>` to complete and returns its result. Internally calls `await(Callable<T> block)`.

See [Sample.java](src/test/java/me/kpavlov/await4j/Sample.java).

## Background

Project Loom has introduced Virtual Threads, but the API requires some boilerplate code to use it effectively in real-life projects:

To run blocking code in a Virtual Thread, it should be wrapped in:
```java
Thread.ofVirtual().start(() -> {
    // run some blocking code here
}).join()
```

When you need to get the execution result back, a common approach is to run it in an executor:

```java
int returnFromVirtualThread() {
    try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        final var task = executor.submit(() -> {
            // Do some expensive calculation here
            return 42; // Return result
        });
        return task.get(); // Get result from task
    } catch (ExecutionException e) {
        throw new RuntimeException(e);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
    }
}
```

## The Better Async API

What if it were possible to use syntax similar to Javascript's `async/await` style?

This library introduces helpful utilities to simplify calls in Virtual Threads:

For example, to call a lambda function, even throwing exceptions, use:

```java
import java.util.concurrent.atomic.AtomicBoolean;

import me.kpavlov.await4j.Async.await;

final var completed = new AtomicBoolean();

await(() -> {
    // Do something slow in a virtual thread
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
    // Set flag to "true" to indicate, that the task is completed
    completed.set(true);
});

// Verify that calculation has been completed
System.out.println("Completed: "+completed.get()); // "Completed: true"
```

To call a lambda that returns a value (Callable), use:
```java
final var result = await(() -> {
    // Do some expensive calculation here
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
    // Return the result
    return 42; 
});
System.out.println("Result: " + result); // "Result: 42"
```

Lambdas may throw exceptions, unlike the `java.lang.Runnable` interface. All non-runtime exceptions are wrapped in `java.lang.RuntimeException`, and `java.lang.Error` will be re-thrown.

## Wrapping `CompletableFuture` and `Future`

Using `java.util.concurrent.CompletableFuture` and `java.util.concurrentFuture` from the Java API is also simplified:

```java
final CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
  // Do some expensive calculation here
  try {
    Thread.sleep(1000);
  } catch (InterruptedException e) {
    throw new RuntimeException(e);
  }
  // Return the result
  return 42;
});

final var completableFutureResult = await(completableFuture);
System.out.println("CompletableFuture result: " + completableFutureResult); // "Result: 42"

final var futureResult = await((Future<Integer>) completableFuture);
System.out.println("Future Result: " + futureResult); // "Result: 42"
```

## Useful Utility Classes

- [Result&lt;T&gt;](src/main/java/me/kpavlov/utils/Result.java) - A discriminated union that encapsulates a successful outcome with a value of type T or a failure with an arbitrary Throwable exception. Similar to [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/) in Kotlin.
- [ThrowingRunnable](src/main/java/me/kpavlov/utils/ThrowingRunnable.java) - Runnable, which can throw Exception

## Final Notes

**If you want to write better code on JVM, use [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html).** This library remains a simpler choice for Java projects where adopting or migrating to [Kotlin](https://kotlinlang.org) is not feasible.

The library focuses on running blocking code on Virtual Threads without providing additional parallelism optimizations. If your IO operations are slow, they will not run faster. If a lambda takes one second to run, `await(...)` will also take approximately one second, but on a virtual thread.
