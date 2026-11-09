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

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/LambdaOps.java}
 * blob {@code 7f9161d6c4c1e4524ec9bc804bb065374974c7b5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LambdaOps {
    private static final PlaceholderLogger log = PlaceholderLogger.of(LambdaOps.class);


    private LambdaOps() {}

    // Function composition
    public static <A, B, C> Function<A, C> compose(Function<A, B> f, Function<B, C> g) {
        return a -> g.apply(f.apply(a));
    }

    public static <A, B, C> Function<A, C> andThen(Function<A, B> f, Function<B, C> g) {
        return compose(f, g);
    }

    public static <A, B, C, D> Function<A, D> compose(Function<A, B> f, Function<B, C> g, Function<C, D> h) {
        return a -> h.apply(g.apply(f.apply(a)));
    }

    // Predicate composition
    public static <T> Predicate<T> and(Predicate<T> a, Predicate<T> b) {
        return t -> a.test(t) && b.test(t);
    }

    public static <T> Predicate<T> or(Predicate<T> a, Predicate<T> b) {
        return t -> a.test(t) || b.test(t);
    }

    public static <T> Predicate<T> not(Predicate<T> p) {
        return t -> !p.test(t);
    }

    public static <T> Predicate<T> xor(Predicate<T> a, Predicate<T> b) {
        return t -> a.test(t) ^ b.test(t);
    }

    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> Predicate<T> allOf(Predicate<T>... predicates) {
        return t -> java.util.Arrays.stream(predicates).allMatch(p -> p.test(t));
    }

    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> Predicate<T> anyOf(Predicate<T>... predicates) {
        return t -> java.util.Arrays.stream(predicates).anyMatch(p -> p.test(t));
    }

    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> Predicate<T> noneOf(Predicate<T>... predicates) {
        return t -> java.util.Arrays.stream(predicates).noneMatch(p -> p.test(t));
    }

    // Consumer composition
    public static <T> Consumer<T> chain(Consumer<T> a, Consumer<T> b) {
        return t -> {
            a.accept(t);
            b.accept(t);
        };
    }

    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    //FLLDCIM: MRL — T2-L3 for-each → forEach; behaviour preserved
    public static <T> Consumer<T> sequence(Consumer<T>... consumers) {
        return t -> java.util.Arrays.stream(consumers).forEach(consumer -> consumer.accept(t));
    }

    public static <T> Consumer<T> doNothing() {
        return t -> {};
    }

    // Suppliers
    public static <T> Supplier<T> constant(T value) {
        return () -> value;
    }

    public static <T> Supplier<T> lazy(Supplier<T> supplier) {
        return new Supplier<>() {

            T value;
            boolean computed;

            @Override
            public T get() {
                if (!computed) {
                    value = supplier.get();
                    computed = true;
                }
                return value;
            }
        };
    }

    public static <T> Supplier<T> memoize(Supplier<T> supplier) {
        return lazy(supplier);
    }

    // Partial application
    public static <A, B, R> Function<B, R> partial(BiFunction<A, B, R> f, A a) {
        return b -> f.apply(a, b);
    }

    public static <A, B, R> Function<A, R> partialRight(BiFunction<A, B, R> f, B b) {
        return a -> f.apply(a, b);
    }

    // Currying
    public static <A, B, R> Function<A, Function<B, R>> curry(BiFunction<A, B, R> f) {
        return a -> b -> f.apply(a, b);
    }

    public static <A, B, R> BiFunction<A, B, R> uncurry(Function<A, Function<B, R>> f) {
        return (a, b) -> f.apply(a).apply(b);
    }

    // Safe wrappers
    public static <T, R> Function<T, R> safe(Function<T, R> f, R defaultValue) {
        return t -> {
            try {
                return f.apply(t);
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }

    public static <T> Predicate<T> safePredicate(Predicate<T> p, boolean defaultValue) {
        return t -> {
            try {
                return p.test(t);
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }

    public static <T> Consumer<T> safeConsumer(Consumer<T> c) {
        return t -> {
            try {
                c.accept(t);
            } catch (Exception ignored) {
                log.debug("Suppressed: {}", ignored.getMessage(), ignored);
            }
        };
    }

    public static <T> Supplier<T> safeSupplier(Supplier<T> s, T defaultValue) {
        return () -> {
            try {
                return s.get();
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }

    // Null-safe wrappers
    public static <T, R> Function<T, R> nullSafe(Function<T, R> f) {
        return t -> t == null ? null : f.apply(t);
    }

    public static <T, R> Function<T, R> nullSafe(Function<T, R> f, R defaultValue) {
        return t -> t == null ? defaultValue : f.apply(t);
    }

    public static <T> Predicate<T> nullSafe(Predicate<T> p, boolean defaultValue) {
        return t -> t == null ? defaultValue : p.test(t);
    }

    @SuppressWarnings("overloads")
    public static <T> Consumer<T> nullSafe(Consumer<T> c) {
        return t -> {
            if (t != null) c.accept(t);
        };
    }

    // Identity and constants
    public static <T> Function<T, T> identity() {
        return t -> t;
    }

    public static <T> Predicate<T> alwaysTrue() {
        return t -> true;
    }

    public static <T> Predicate<T> alwaysFalse() {
        return t -> false;
    }

    public static <T> Predicate<T> isNull() {
        return Objects::isNull;
    }

    public static <T> Predicate<T> notNull() {
        return Objects::nonNull;
    }

    // Type-based predicates
    public static <T> Predicate<T> instanceOf(Class<?> clazz) {
        return t -> clazz.isInstance(t);
    }

    public static <T> Predicate<T> equalTo(T value) {
        return t -> Objects.equals(t, value);
    }

    public static <T extends Comparable<T>> Predicate<T> greaterThan(T value) {
        return t -> t != null && t.compareTo(value) > 0;
    }

    public static <T extends Comparable<T>> Predicate<T> lessThan(T value) {
        return t -> t != null && t.compareTo(value) < 0;
    }

    public static <T extends Comparable<T>> Predicate<T> between(T low, T high) {
        return t -> t != null && t.compareTo(low) >= 0 && t.compareTo(high) <= 0;
    }

    // Runnable/Callable utilities
    public static Runnable safeRunnable(Runnable r) {
        return () -> {
            try {
                r.run();
            } catch (Exception ignored) {
                log.debug("Suppressed: {}", ignored.getMessage(), ignored);
            }
        };
    }

    public static Runnable compose(Runnable a, Runnable b) {
        return () -> {
            a.run();
            b.run();
        };
    }

    //FLLDCIM: MRL — T2-L3 for-each → forEach; behaviour preserved
    public static Runnable sequence(Runnable... runnables) {
        return () -> java.util.Arrays.stream(runnables).forEach(Runnable::run);
    }

    // BiFunction utilities
    public static <A, B, R> BiFunction<B, A, R> flip(BiFunction<A, B, R> f) {
        return (b, a) -> f.apply(a, b);
    }

    public static <A, B, R> BiFunction<A, B, R> safeBiFunction(BiFunction<A, B, R> f, R defaultValue) {
        return (a, b) -> {
            try {
                return f.apply(a, b);
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }

    // Predicate utilities for strings
    public static Predicate<String> contains(String sub) {
        return s -> s != null && s.contains(sub);
    }

    public static Predicate<String> startsWith(String prefix) {
        return s -> s != null && s.startsWith(prefix);
    }

    public static Predicate<String> endsWith(String suffix) {
        return s -> s != null && s.endsWith(suffix);
    }

    public static Predicate<String> matches(String regex) {
        return s -> s != null && s.matches(regex);
    }

    public static Predicate<String> isEmpty() {
        return s -> s == null || s.isEmpty();
    }

    public static Predicate<String> isBlank() {
        return s -> s == null || s.isBlank();
    }

    public static Predicate<String> hasLength(int length) {
        return s -> s != null && s.length() == length;
    }

    /**
     * Command-line interface for LambdaOps utilities.
     *
     * <p>
     * Provides interactive access to lambda composition, predicate operations,
     * currying, and functional utilities.
     *
     * <h4>Usage</h4>
     *
     * <pre>{@code
     * java jdk.internal.m3.collections.LambdaOps list
     * java jdk.internal.m3.collections.LambdaOps compose "hello"
     * java jdk.internal.m3.collections.LambdaOps predicate "test"
     * java jdk.internal.m3.collections.LambdaOps curry 10 20
     * java jdk.internal.m3.collections.LambdaOps lazy
     * }</pre>
     *
     * @param args
     *            command-line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1); // FM-40: acceptable in CLI main()
        }

        try {
            String command = args[0].toLowerCase();
            switch (command) {
                case "list" -> printOperations();
                case "compose" -> {
                    if (args.length < 2) {
                        log.error("compose requires an input value");
                        System.exit(1);
                    }
                    String input = args[1];
                    Function<String, String> toUpper = String::toUpperCase;
                    Function<String, Integer> length = String::length;
                    Function<String, Integer> composed = compose(toUpper, length);
                    Integer result = composed.apply(input);
                    log.info("Composed (toUpper -> length): {} -> {}", input, result);
                }
                case "predicate" -> {
                    if (args.length < 2) {
                        log.error("predicate requires a test string");
                        System.exit(1);
                    }
                    String test = args[1];
                    Predicate<String> notNull = notNull();
                    Predicate<String> notBlank = not(isEmpty());
                    Predicate<String> both = and(notNull, notBlank);
                    boolean result = both.test(test);
                    log.info("Test '{}' is not-null AND not-empty: {}", test, result);
                }
                case "curry" -> {
                    if (args.length < 3) {
                        log.error("curry requires two integer arguments");
                        System.exit(1);
                    }
                    int a = Integer.parseInt(args[1]);
                    int b = Integer.parseInt(args[2]);
                    BiFunction<Integer, Integer, Integer> add = Integer::sum;
                    Function<Integer, Function<Integer, Integer>> curried = curry(add);
                    int result = curried.apply(a).apply(b);
                    log.info("Curried addition: {} + {} = {}", a, b, result);
                }
                case "lazy" -> {
                    log.info("Demonstrating lazy evaluation:");
                    Supplier<String> expensive = () -> {
                        log.info("  Computing expensive value...");
                        return "Expensive Result";
                    };
                    Supplier<String> lazySupplier = lazy(expensive);
                    log.info("  Lazy supplier created (not computed yet)");
                    log.info("  First get: {}", lazySupplier.get());
                    log.info("  Second get: {} (cached)", lazySupplier.get());
                }
                case "safe" -> {
                    if (args.length < 2) {
                        log.error("safe requires a test string");
                        System.exit(1);
                    }
                    String input = args[1];
                    Function<String, Integer> parse = Integer::parseInt;
                    Function<String, Integer> safeParse = safe(parse, -1);
                    int result = safeParse.apply(input);
                    log.info("Safe parse of '{}': {}", input, result);
                }
                default -> {
                    log.error("Unknown command: {}", command);
                    printUsage();
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            log.error("Error executing command", e);
            System.exit(1);
        }
    }

    private static void printUsage() {
        log.info("Usage: java jdk.internal.m3.collections.LambdaOps <command> [args...]");

        log.info("Commands:");
        log.info("  list                - List all available operations");
        log.info("  compose <input>     - Demonstrate function composition");
        log.info("  predicate <test>    - Demonstrate predicate composition");
        log.info("  curry <a> <b>       - Demonstrate currying");
        log.info("  lazy                - Demonstrate lazy evaluation");
        log.info("  safe <input>        - Demonstrate safe execution");
    }

    private static void printOperations() {
        log.info("LambdaOps - Functional programming utilities");

        log.info("Function Composition:");
        log.info("  compose(f, g)       - Compose f then g");
        log.info("  andThen(f, g)       - Same as compose");
        log.info("  compose(f, g, h)    - Compose three functions");

        log.info("Predicate Composition:");
        log.info("  and(a, b)           - Logical AND");
        log.info("  or(a, b)            - Logical OR");
        log.info("  not(p)              - Logical NOT");
        log.info("  xor(a, b)           - Logical XOR");
        log.info("  allOf(...)          - All predicates must match");
        log.info("  anyOf(...)          - Any predicate must match");
        log.info("  noneOf(...)         - No predicate must match");

        log.info("Consumer Composition:");
        log.info("  chain(a, b)         - Chain two consumers");
        log.info("  sequence(...)       - Sequence multiple consumers");
        log.info("  doNothing()         - No-op consumer");

        log.info("Suppliers:");
        log.info("  constant(T)         - Always return same value");
        log.info("  lazy(Supplier)      - Lazy evaluation with caching");
        log.info("  memoize(Supplier)   - Same as lazy");

        log.info("Partial Application & Currying:");
        log.info("  partial(BiFunc, A)  - Partially apply first arg");
        log.info("  partialRight(BiFunc, B) - Partially apply second arg");
        log.info("  curry(BiFunction)   - Convert to curried form");
        log.info("  uncurry(Function)   - Convert from curried form");

        log.info("Safe Wrappers:");
        log.info("  safe(Function, default) - Catch exceptions, return default");
        log.info("  safePredicate(...)  - Safe predicate evaluation");
        log.info("  safeConsumer(...)   - Safe consumer execution");
        log.info("  safeSupplier(...)   - Safe supplier evaluation");

        log.info("Null-Safe Wrappers:");
        log.info("  nullSafe(Function)  - Return null if input is null");
        log.info("  nullSafe(Predicate, default) - Return default if null");

        log.info("Common Predicates:");
        log.info("  isNull(), notNull() - Null checks");
        log.info("  instanceOf(Class)   - Type check");
        log.info("  equalTo(T)          - Equality check");
        log.info("  greaterThan(T)      - Comparison");
        log.info("  between(T, T)       - Range check");
        log.info("  contains(String)    - String contains");
        log.info("  startsWith(String)  - String prefix");
        log.info("  matches(String)     - Regex match");
    }
}
