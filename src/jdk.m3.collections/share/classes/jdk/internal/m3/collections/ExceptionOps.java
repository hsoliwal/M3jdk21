/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
/**
 * Exception handling utilities.
 *
 * <p>
 * Provides:
 * <ul>
 * <li>Silent execution (swallow exceptions)</li>
 * <li>Exception transformation</li>
 * <li>Safe operations with defaults</li>
 * <li>Stack trace utilities</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * import static jdk.internal.m3.collections.ExceptionOps.*;
 *
 * // Silent execution
 * silently(() -> riskyOperation());
 * String result = silentlyGet(() -> parseJson(text), "{}");
 *
 * // With default
 * int port = doOrDefault(() -> Integer.parseInt(str), 8080);
 * }</pre>
 *
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/ExceptionOps.java}
 * blob {@code 4632e26e0536084f7899001695666a16a2a1c568} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ExceptionOps {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ExceptionOps.class);


    private ExceptionOps() {}

    // --- FUNCTIONAL INTERFACES ---

    @FunctionalInterface
    public interface ThrowingSupplier<T> {

        T get() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingRunnable {

        void run() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {

        void accept(T t) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {

        R apply(T t) throws Exception;
    }

    // --- SILENT EXECUTION ---

    /** Execute silently, swallow all exceptions. */
    public static void silently(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception ignored) {
            log.debug("Suppressed: {}", ignored.getMessage(), ignored);
        }
    }

    /** Execute silently, return result or null on exception. */
    public static <T> T silentlyGet(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return null; // TODO: FM-36 — API contract (public method, silent error sentinel)
        }
    }

    /** Execute silently, return result or default on exception. */
    public static <T> T silentlyGet(ThrowingSupplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** Execute silently, return Optional. */
    public static <T> Optional<T> silentlyOpt(ThrowingSupplier<T> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // --- DO OR DEFAULT ---

    /** Execute supplier, return default on exception. */
    public static <T> T doOrDefault(ThrowingSupplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** Execute supplier, return null on exception. */
    public static <T> T doOrNull(ThrowingSupplier<T> supplier) {
        return doOrDefault(supplier, null);
    }

    /** Execute supplier, get default from supplier on exception. */
    public static <T> T doOrGet(ThrowingSupplier<T> supplier, Supplier<T> defaultSupplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return defaultSupplier.get();
        }
    }

    // --- EXECUTE WITH EXCEPTION HANDLING ---

    /** Execute with exception handler. */
    public static void withHandler(ThrowingRunnable runnable, Consumer<Exception> handler) {
        try {
            runnable.run();
        } catch (Exception e) {
            handler.accept(e);
        }
    }

    /** Execute with exception handler, return result. */
    public static <T> T withHandler(ThrowingSupplier<T> supplier, Function<Exception, T> handler) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return handler.apply(e);
        }
    }

    /** Execute and log exception (silent). */
    public static void withLog(ThrowingRunnable runnable, Consumer<String> logger) {
        try {
            runnable.run();
        } catch (Exception e) {
            logger.accept("Exception: " + e.getMessage());
        }
    }

    // --- EXCEPTION WRAPPING ---

    /** Wrap checked exception as unchecked. */
    public static <T> T unchecked(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Wrap checked exception as unchecked runnable. */
    public static void unchecked(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Convert ThrowingSupplier to Supplier (wraps as unchecked). */
    public static <T> Supplier<T> asSupplier(ThrowingSupplier<T> supplier) {
        return () -> unchecked(supplier);
    }

    /** Convert ThrowingRunnable to Runnable (wraps as unchecked). */
    public static Runnable asRunnable(ThrowingRunnable runnable) {
        return () -> unchecked(runnable);
    }

    /** Convert ThrowingFunction to Function (wraps as unchecked). */
    public static <T, R> Function<T, R> asFunction(ThrowingFunction<T, R> function) {
        return t -> {
            try {
                return function.apply(t);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /** Convert ThrowingConsumer to Consumer (wraps as unchecked). */
    public static <T> Consumer<T> asConsumer(ThrowingConsumer<T> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    // --- EXCEPTION UTILITIES ---

    /** Get stack trace as string. */
    public static String getStackTrace(Throwable t) {
        if (t == null) return "";
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    //FLLDCIM: MRL — FM-36 return null eliminated; Optional<Throwable> preserves contract
    /** Get root cause. */
    public static Optional<Throwable> getRootCause(Throwable t) {
        if (t == null) return Optional.empty();
        //FLLDCIM: NFL — bounded/alloc-free/cold — abort:mutation+cycle_guard
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return Optional.of(cause);
    }

    //FLLDCIM: MRL — FM-36 return null eliminated; Optional<String> preserves contract
    /** Get root cause message. */
    public static Optional<String> getRootMessage(Throwable t) {
        return getRootCause(t).map(Throwable::getMessage);
    }

    /** Check if exception chain contains type. */
    public static boolean containsCause(Throwable t, Class<? extends Throwable> type) {
        //FLLDCIM: NFL — bounded/alloc-free/cold — abort:mutation+return_in_body
        while (t != null) {
            if (type.isInstance(t)) return true;
            t = t.getCause();
        }
        return false;
    }

    /** Find specific cause type in chain. */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> Optional<T> findCause(Throwable t, Class<T> type) {
        while (t != null) {
            if (type.isInstance(t)) return Optional.of((T) t);
            t = t.getCause();
        }
        return Optional.empty();
    }

    // --- RETRY UTILITIES ---

    /** Retry operation up to n times. */
    public static <T> T retry(int maxAttempts, ThrowingSupplier<T> supplier) throws Exception {
        Exception last = null;
        for (int i = 0; i < maxAttempts; i++) { // NFL — retry loop with exception accumulation
            try {
                return supplier.get();
            } catch (Exception e) {
                last = e;
            }
        }
        throw last;
    }

    /** Retry operation with delay between attempts. */
    public static <T> T retry(int maxAttempts, long delayMs, ThrowingSupplier<T> supplier) throws Exception {
        Exception last = null;
        for (int i = 0; i < maxAttempts; i++) { // NFL — retry loop with delay between attempts
            try {
                return supplier.get();
            } catch (Exception e) {
                last = e;
                if (i < maxAttempts - 1) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
        throw last;
    }

    /** Retry silently, return default after max attempts. */
    public static <T> T retrySilently(int maxAttempts, ThrowingSupplier<T> supplier, T defaultValue) {
        for (int i = 0; i < maxAttempts; i++) { // NFL — retry loop with silent exception swallowing
            try {
                return supplier.get();
            } catch (Exception ignored) {
                log.debug("Suppressed: {}", ignored.getMessage(), ignored);
            }
        }
        return defaultValue;
    }

    // --- STANDALONE CLI ---

    /**
     * Standalone CLI for exception handling operations.
     *
     * <p>
     * Usage examples:
     *
     * <pre>{@code
     * # Execute silently with default
     * java ExceptionOps silently-get "risky-operation" "default-value"
     *
     * # Retry operation
     * java ExceptionOps retry 3 "operation"
     *
     * # Get stack trace simulation
     * java ExceptionOps stack-trace
     *
     * # Test exception wrapping
     * java ExceptionOps unchecked "test"
     *
     * # List available operations
     * java ExceptionOps list
     * }</pre>
     *
     * @param args
     *            command-line arguments: [operation] [arguments]
     */
    public static void main(String[] args) {
        log.info("Starting ExceptionOps CLI");

        if (args.length == 0) {
            printUsage();
            System.exit(0); // FM-40: acceptable in CLI main()
        }

        String command = args[0].toLowerCase();

        try {
            switch (command) {
                case "list" -> printOperations();
                case "silently-get" -> {
                    requireArgs(args, 2, "silently-get <default>");
                    // Simulate a risky operation
                    String result = silentlyGet(
                            () -> {
                                throw new RuntimeException("Test exception");
                            },
                            args[1]);
                    log.info("Result: {}", result);
                }
                case "retry" -> {
                    requireArgs(args, 2, "retry <maxAttempts>");
                    int maxAttempts = Integer.parseInt(args[1]);
                    int[] attempt = {0};
                    String result = retrySilently(
                            maxAttempts,
                            () -> {
                                attempt[0]++;
                                if (attempt[0] < maxAttempts) {
                                    throw new RuntimeException("Attempt " + attempt[0] + " failed");
                                }
                                return "Success on attempt " + attempt[0];
                            },
                            "Failed after " + maxAttempts + " attempts");
                    log.info("Result: {}", result);
                }
                case "stack-trace" -> {
                    Exception testException = new RuntimeException("Test exception");
                    String stackTrace = getStackTrace(testException);
                    log.info("Stack trace length: {} chars", stackTrace.length());
                    // CNPCDA[FLLDCIM:R13:P2:FM-08] complex concat — T2 escalation
                    log.info("First 200 chars: {}", stackTrace.substring(0, Math.min(200, stackTrace.length())));
                }
                case "root-cause" -> {
                    Exception inner = new IllegalArgumentException("Root cause");
                    Exception outer = new RuntimeException("Outer exception", inner);
                    Optional<Throwable> root = getRootCause(outer);
                    log.info("Root cause: {}", root.map(Throwable::getMessage).orElse("unknown"));
                }
                case "unchecked" -> {
                    requireArgs(args, 2, "unchecked <value>");
                    String result = unchecked(() -> {
                        if ("fail".equals(args[1])) {
                            throw new Exception("Checked exception");
                        }
                        return "Success: " + args[1];
                    });
                    log.info("Result: {}", result);
                }
                default -> {
                    log.error("Unknown command: {}", command);
                    printUsage();
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            log.error("Exception operation failed", e);
            System.exit(1);
        }
    }

    private static void printUsage() {
        log.info("ExceptionOps CLI - Exception handling operations");

        log.info("Usage: java ExceptionOps <command> [arguments]");

        log.info("Commands:");
        log.info("  list                        List available operations");
        log.info("  silently-get <default>      Execute with default on exception");
        log.info("  retry <maxAttempts>         Retry operation with attempts");
        log.info("  stack-trace                 Get stack trace from exception");
        log.info("  root-cause                  Get root cause from exception");
        log.info("  unchecked <value>           Wrap checked exception as unchecked");
    }

    private static void printOperations() {
        log.info("Exception handling operations available:");
        log.info("  Silent Execution:");
        log.info("    - silently, silentlyGet, silentlyOpt");
        log.info("  Do Or Default:");
        log.info("    - doOrDefault, doOrNull, doOrGet");
        log.info("  Execute With Exception Handling:");
        log.info("    - withHandler, withLog");
        log.info("  Exception Wrapping:");
        log.info("    - unchecked, asSupplier, asRunnable, asFunction, asConsumer");
        log.info("  Exception Utilities:");
        log.info("    - getStackTrace, getRootCause, getRootMessage");
        log.info("    - containsCause, findCause");
        log.info("  Retry Utilities:");
        log.info("    - retry, retrySilently (with delay support)");
    }

    private static void requireArgs(String[] args, int required, String usage) {
        if (args.length < required) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }
}
