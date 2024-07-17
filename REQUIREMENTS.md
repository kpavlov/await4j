# Requirements

## `await(...)` should:

- Should run Callable/Runnable/Future should in a virtual thread;

- Done or failed `Future` should never start new virtual thread (short-circuiting);

- Should handle `null` inputs by throwing `NullPointerException`.

- Should ensure proper exception handling and propagation, converting checked exceptions to unchecked exceptions where appropriate.

- Should properly handle `InterruptedException` by restoring the interrupted status.

- Should differentiate between Error and Exception, rethrowing Error immediately.

- Should support timeout parameters and handle timeouts appropriately.

- Should name virtual threads using a consistent naming pattern for easier debugging and identification.
