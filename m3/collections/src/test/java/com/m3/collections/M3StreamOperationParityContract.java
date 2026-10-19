// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.*;

/**
 * Java 21 operation-surface oracle over real M3 collection sources, not API stand-ins.
 * Every reflected non-bridge stream operation and Collectors overload must execute.
 * Static factories exercise the JDK itself; instance rows exercise M3 source traversal.
 * This finite corpus does not claim JCK, every collection family, or performance parity.
 */
public final class M3StreamOperationParityContract {
    private static final int[] VALUES = {1, 2, 2, 3, 4, 5, 0, -1, 31};
    private final List<String> rows = new ArrayList<>();
    private int checks;
    private M3StreamOperationParityContract() { }

    private enum Kind {
        REF(Stream.class), INT(IntStream.class), LONG(LongStream.class), DOUBLE(DoubleStream.class);
        final Class<?> api;
        Kind(Class<?> api) { this.api = api; }
        Object scalar(int value) {
            return switch (this) { case REF, INT -> Integer.valueOf(value);
                case LONG -> Long.valueOf(value); case DOUBLE -> Double.valueOf(value); };
        }
        Object array() {
            return switch (this) { case REF -> Arrays.stream(VALUES).boxed().toArray(Integer[]::new);
                case INT -> VALUES.clone(); case LONG -> Arrays.stream(VALUES).asLongStream().toArray();
                case DOUBLE -> Arrays.stream(VALUES).asDoubleStream().toArray(); };
        }
    }

    public static void main(String[] args) throws Exception {
        if (Runtime.version().feature() != 21) throw new IllegalStateException("Java 21 API required");
        var proof = new M3StreamOperationParityContract();
        for (Kind kind : Kind.values()) for (boolean parallel : new boolean[]{false, true}) {
            for (Method method : methods(kind.api)) {
                String actual = proof.execute(method, kind, true, parallel);
                String expected = proof.execute(method, kind, false, parallel);
                proof.equal(expected, actual, method.toGenericString());
                proof.rows.add(kind.api.getName() + "\t" + signature(method) + "\t"
                        + (Modifier.isStatic(method.getModifiers()) ? "STATIC_JDK" : "M3_SOURCE")
                        + "\t" + (parallel ? "parallel" : "sequential") + "\tPASS");
            }
        }
        proof.collectors();
        proof.edgeContracts();
        Collections.sort(proof.rows);
        String text = "api\toperation\towner\tmode\tstatus\n" + String.join("\n", proof.rows) + "\n";
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
        if (args.length == 1) {
            Path output = Path.of(args[0]);
            if (Files.exists(output)) throw new IllegalStateException("refuse proof overwrite");
            Files.writeString(output, text, StandardCharsets.UTF_8);
        }
        System.out.println("M3_STREAM_PARITY checks=" + proof.checks + " operation_rows="
                + proof.rows.size() + " catalogue_sha256=" + hash + " RESULT=PASS");
    }

    private static List<Method> methods(Class<?> api) {
        return Arrays.stream(api.getMethods())
                .filter(m -> !m.isBridge() && !m.isSynthetic() && m.getDeclaringClass() != Object.class)
                .sorted(Comparator.comparing(M3StreamOperationParityContract::signature)).toList();
    }
    private static String signature(Method m) {
        return m.getName() + "(" + String.join(",", Arrays.stream(m.getParameterTypes())
                .map(Class::getTypeName).toList()) + "):" + m.getReturnType().getTypeName();
    }

    /** Conversion streams remain primitive; this is not a new collection storage owner. */
    private static BaseStream<?, ?> source(Kind kind, boolean actual, boolean parallel) {
        if (kind == Kind.REF) {
            List<Integer> values = Arrays.stream(VALUES).boxed().toList();
            Stream<Integer> result = actual ? new M3ArrayList<>(values).stream() : values.stream();
            return parallel ? result.parallel() : result;
        }
        LongStream longs = actual
                ? M3LongArrayList.copyOf(Arrays.stream(VALUES).asLongStream().toArray()).longStream()
                : Arrays.stream(VALUES).asLongStream();
        BaseStream<?, ?> result = switch (kind) {
            case INT -> longs.mapToInt(v -> (int) v);
            case LONG -> longs;
            case DOUBLE -> longs.asDoubleStream();
            default -> throw new AssertionError(kind);
        };
        return parallel ? result.parallel() : result;
    }

    private String execute(Method method, Kind kind, boolean actual, boolean parallel) throws Exception {
        var events = new ConcurrentLinkedQueue<String>();
        BaseStream<?, ?> input = Modifier.isStatic(method.getModifiers()) ? null : source(kind, actual, parallel);
        try {
            Object result;
            try { result = method.invoke(input, arguments(method, kind, actual, parallel, events)); }
            catch (InvocationTargetException failure) {
                throw new AssertionError("nominal operation failed: " + method, failure.getCause());
            }
            String normalized;
            if (method.getName().equals("findAny")) {
                boolean valid = switch (kind) {
                    case REF -> result instanceof Optional<?> o && o.isPresent() && Arrays.stream(VALUES).anyMatch(v -> Objects.equals(v, o.get()));
                    case INT -> result instanceof OptionalInt o && o.isPresent() && Arrays.stream(VALUES).anyMatch(v -> v == o.getAsInt());
                    case LONG -> result instanceof OptionalLong o && o.isPresent() && Arrays.stream(VALUES).anyMatch(v -> v == o.getAsLong());
                    case DOUBLE -> result instanceof OptionalDouble o && o.isPresent() && Arrays.stream(VALUES).anyMatch(v -> v == o.getAsDouble());
                };
                require(valid, "findAny must return an input member"); normalized = "VALID_MEMBER";
            } else normalized = normalize(result);
            var effects = new ArrayList<>(events);
            if (!method.getName().equals("forEachOrdered")) Collections.sort(effects);
            return normalized + "|effects=" + effects;
        } finally { if (input != null) input.close(); }
    }

    /** Explicit argument vocabulary. A new/unmapped Java API fails, never silently skips. */
    private static Object[] arguments(Method m, Kind k, boolean actual, boolean parallel,
            ConcurrentLinkedQueue<String> events) {
        String name = m.getName(); int n = m.getParameterCount();
        if (n == 0) return new Object[0];
        if (name.equals("of")) return new Object[]{m.getParameterTypes()[0].isArray() ? k.array() : k.scalar(1)};
        if (name.equals("ofNullable")) return new Object[]{Integer.valueOf(1)};
        if (name.equals("limit") || name.equals("skip")) return new Object[]{2L};
        if (name.equals("range") || name.equals("rangeClosed")) return new Object[]{k.scalar(0), k.scalar(5)};
        if (name.equals("concat")) return new Object[]{source(k, actual, parallel), source(k, actual, parallel)};
        if (name.equals("sorted")) return new Object[]{Comparator.<Integer>reverseOrder()};
        if (name.equals("min") || name.equals("max")) return new Object[]{Comparator.<Integer>naturalOrder()};
        if (name.equals("toArray")) return new Object[]{(IntFunction<Integer[]>) Integer[]::new};
        if (name.equals("onClose")) return new Object[]{(Runnable) () -> events.add("close")};
        if (name.equals("filter") || name.equals("takeWhile") || name.equals("dropWhile")
                || name.equals("allMatch") || name.equals("anyMatch") || name.equals("noneMatch")) {
            return new Object[]{predicate(k, false)};
        }
        if (name.equals("map")) return new Object[]{unary(k)};
        if (name.equals("iterate")) return n == 2 ? new Object[]{k.scalar(0), unary(k)}
                : new Object[]{k.scalar(0), predicate(k, true), unary(k)};
        if (name.equals("generate")) return new Object[]{switch (k) {
            case REF -> (Supplier<Integer>) () -> 3; case INT -> (IntSupplier) () -> 3;
            case LONG -> (LongSupplier) () -> 3L; case DOUBLE -> (DoubleSupplier) () -> 3.0;
        }};
        if (name.equals("forEach") || name.equals("forEachOrdered") || name.equals("peek")) {
            return new Object[]{switch (k) {
                case REF -> (Consumer<Integer>) v -> events.add(v.toString());
                case INT -> (IntConsumer) v -> events.add(Integer.toString(v));
                case LONG -> (LongConsumer) v -> events.add(Long.toString(v));
                case DOUBLE -> (DoubleConsumer) v -> events.add(Double.toString(v));
            }};
        }
        if (name.equals("mapMulti")) return new Object[]{switch (k) {
            case REF -> (BiConsumer<Integer, Consumer<Integer>>) (v, out) -> { out.accept(v); out.accept(-v); };
            case INT -> (IntStream.IntMapMultiConsumer) (v, out) -> { out.accept(v); out.accept(-v); };
            case LONG -> (LongStream.LongMapMultiConsumer) (v, out) -> { out.accept(v); out.accept(-v); };
            case DOUBLE -> (DoubleStream.DoubleMapMultiConsumer) (v, out) -> { out.accept(v); out.accept(-v); };
        }};
        if (name.equals("flatMap")) return new Object[]{switch (k) {
            case REF -> (Function<Integer, Stream<Integer>>) v -> Stream.of(v, -v);
            case INT -> (IntFunction<IntStream>) v -> IntStream.of(v, -v);
            case LONG -> (LongFunction<LongStream>) v -> LongStream.of(v, -v);
            case DOUBLE -> (DoubleFunction<DoubleStream>) v -> DoubleStream.of(v, -v);
        }};
        if (name.equals("mapToObj")) return new Object[]{switch (k) {
            case INT -> (IntFunction<String>) Integer::toString;
            case LONG -> (LongFunction<String>) Long::toString;
            case DOUBLE -> (DoubleFunction<String>) Double::toString;
            default -> throw new AssertionError(name);
        }};
        if (name.equals("mapToInt")) return new Object[]{switch (k) {
            case REF -> (ToIntFunction<Integer>) v -> v + 1;
            case LONG -> (LongToIntFunction) v -> (int) v + 1;
            case DOUBLE -> (DoubleToIntFunction) v -> (int) v + 1;
            default -> throw new AssertionError(name);
        }};
        if (name.equals("mapToLong")) return new Object[]{switch (k) {
            case REF -> (ToLongFunction<Integer>) v -> v + 1L;
            case INT -> (IntToLongFunction) v -> v + 1L;
            case DOUBLE -> (DoubleToLongFunction) v -> (long) v + 1;
            default -> throw new AssertionError(name);
        }};
        if (name.equals("mapToDouble")) return new Object[]{switch (k) {
            case REF -> (ToDoubleFunction<Integer>) v -> v + 0.5;
            case INT -> (IntToDoubleFunction) v -> v + 0.5;
            case LONG -> (LongToDoubleFunction) v -> v + 0.5;
            default -> throw new AssertionError(name);
        }};
        if (name.equals("mapMultiToInt")) return new Object[]{(BiConsumer<Integer, IntConsumer>) (v, out) -> { out.accept(v); out.accept(-v); }};
        if (name.equals("mapMultiToLong")) return new Object[]{(BiConsumer<Integer, LongConsumer>) (v, out) -> { out.accept(v); out.accept(-v); }};
        if (name.equals("mapMultiToDouble")) return new Object[]{(BiConsumer<Integer, DoubleConsumer>) (v, out) -> { out.accept(v); out.accept(-v); }};
        if (name.equals("flatMapToInt")) return new Object[]{(Function<Integer, IntStream>) v -> IntStream.of(v, -v)};
        if (name.equals("flatMapToLong")) return new Object[]{(Function<Integer, LongStream>) v -> LongStream.of(v, -v)};
        if (name.equals("flatMapToDouble")) return new Object[]{(Function<Integer, DoubleStream>) v -> DoubleStream.of(v, -v)};
        if (name.equals("reduce")) {
            Object op = switch (k) {
                case REF -> (BinaryOperator<Integer>) Integer::sum; case INT -> (IntBinaryOperator) Integer::sum;
                case LONG -> (LongBinaryOperator) Long::sum; case DOUBLE -> (DoubleBinaryOperator) Double::sum;
            };
            return switch (n) { case 1 -> new Object[]{op}; case 2 -> new Object[]{k.scalar(0), op};
                case 3 -> new Object[]{Integer.valueOf(0), (BiFunction<Integer, Integer, Integer>) Integer::sum, op};
                default -> throw new AssertionError(m); };
        }
        if (name.equals("collect")) {
            if (n == 1) return new Object[]{Collectors.toList()};
            Object accumulate = switch (k) {
                case REF -> (BiConsumer<List<Object>, Integer>) List::add;
                case INT -> (ObjIntConsumer<List<Object>>) List::add;
                case LONG -> (ObjLongConsumer<List<Object>>) List::add;
                case DOUBLE -> (ObjDoubleConsumer<List<Object>>) List::add;
            };
            return new Object[]{(Supplier<List<Object>>) ArrayList::new, accumulate,
                    (BiConsumer<List<Object>, List<Object>>) List::addAll};
        }
        throw new AssertionError("UNMAPPED_STREAM_OPERATION " + m.toGenericString());
    }
    private static Object predicate(Kind k, boolean bound) {
        return switch (k) {
            case REF -> (Predicate<Integer>) v -> bound ? v < 20 : v > 0;
            case INT -> (IntPredicate) v -> bound ? v < 20 : v > 0;
            case LONG -> (LongPredicate) v -> bound ? v < 20 : v > 0;
            case DOUBLE -> (DoublePredicate) v -> bound ? v < 20 : v > 0;
        };
    }
    private static Object unary(Kind k) {
        return switch (k) {
            case REF -> (UnaryOperator<Integer>) v -> v * 2 + 1;
            case INT -> (IntUnaryOperator) v -> v * 2 + 1;
            case LONG -> (LongUnaryOperator) v -> v * 2 + 1;
            case DOUBLE -> (DoubleUnaryOperator) v -> v * 2 + 1;
        };
    }

    private static String normalize(Object value) {
        if (value instanceof BaseStream<?, ?> base) {
            try (base) {
                boolean parallel = base.isParallel();
                Object array;
                if (base instanceof Stream<?> s) array = s.limit(32).toArray();
                else if (base instanceof IntStream s) array = s.limit(32).toArray();
                else if (base instanceof LongStream s) array = s.limit(32).toArray();
                else if (base instanceof DoubleStream s) array = s.limit(32).toArray();
                else throw new AssertionError(base.getClass());
                return "parallel=" + parallel + ":" + normalize(array);
            }
        }
        if (value instanceof Stream.Builder<?> b) return normalize(buildReference(b));
        if (value instanceof IntStream.Builder b) { b.accept(1); b.add(2); return normalize(b.build()); }
        if (value instanceof LongStream.Builder b) { b.accept(1); b.add(2); return normalize(b.build()); }
        if (value instanceof DoubleStream.Builder b) { b.accept(1); b.add(2); return normalize(b.build()); }
        if (value instanceof int[] a) return Arrays.toString(a);
        if (value instanceof long[] a) return Arrays.toString(a);
        if (value instanceof double[] a) return Arrays.toString(a);
        if (value instanceof Object[] a) return Arrays.deepToString(a);
        if (value instanceof Iterator<?> iterator) {
            var output = new ArrayList<Object>(); iterator.forEachRemaining(output::add); return output.toString();
        }
        if (value instanceof Spliterator<?> iterator) {
            var output = new ArrayList<Object>(); iterator.forEachRemaining(output::add); return output.toString();
        }
        if (value instanceof Map<?, ?> map) {
            var sorted = new TreeMap<String, String>();
            map.forEach((key, entry) -> sorted.put(String.valueOf(key), normalize(entry))); return sorted.toString();
        }
        if (value instanceof Set<?> set) return set.stream().map(String::valueOf).sorted().toList().toString();
        return String.valueOf(value);
    }
    @SuppressWarnings("unchecked")
    private static Stream<?> buildReference(Stream.Builder<?> raw) {
        Stream.Builder<Object> b = (Stream.Builder<Object>) raw; b.accept(1); b.add(2); return b.build();
    }

    /** Only UNORDERED collectors may ignore order within their accumulated groups. */
    private static String normalizeUnordered(Object value) {
        if (value instanceof Map<?, ?> map) {
            var sorted = new TreeMap<String, String>();
            map.forEach((key, entry) -> sorted.put(String.valueOf(key), normalizeUnordered(entry)));
            return sorted.toString();
        }
        if (value instanceof Collection<?> values) {
            return values.stream().map(M3StreamOperationParityContract::normalizeUnordered).sorted().toList().toString();
        }
        return normalize(value);
    }

    @SuppressWarnings("unchecked")
    private void collectors() throws Exception {
        for (Method method : methods(Collectors.class)) {
            if (!Modifier.isStatic(method.getModifiers())) throw new AssertionError(method);
            for (boolean parallel : new boolean[]{false, true}) {
                Collector<Integer, ?, ?> collector = (Collector<Integer, ?, ?>) method.invoke(null, collectorArguments(method));
                List<Integer> values = Arrays.stream(VALUES).distinct().boxed().toList();
                Object actual, expected;
                try (Stream<Integer> left = parallel ? new M3ArrayList<>(values).parallelStream() : new M3ArrayList<>(values).stream();
                     Stream<Integer> right = parallel ? values.parallelStream() : values.stream()) {
                    if (method.getName().equals("joining")) {
                        Collector<String, ?, ?> textCollector = (Collector<String, ?, ?>) (Collector<?, ?, ?>) collector;
                        actual = left.map(String::valueOf).collect(textCollector);
                        expected = right.map(String::valueOf).collect(textCollector);
                    } else { actual = left.collect(collector); expected = right.collect(collector); }
                }
                boolean unordered = collector.characteristics().contains(Collector.Characteristics.UNORDERED);
                equal(unordered ? normalizeUnordered(expected) : normalize(expected),
                        unordered ? normalizeUnordered(actual) : normalize(actual), method.toGenericString());
                rows.add(Collectors.class.getName() + "\t" + signature(method) + "\tM3_SOURCE\t"
                        + (parallel ? "parallel" : "sequential") + "\tPASS");
            }
        }
    }
    private static Object[] collectorArguments(Method m) {
        String name = m.getName(); int n = m.getParameterCount();
        if (n == 0) return new Object[0];
        Function<Integer, Integer> identity = Function.identity();
        Function<Integer, Integer> classifier = v -> Math.floorMod(v, 3);
        ToIntFunction<Integer> ints = Integer::intValue;
        ToLongFunction<Integer> longs = Integer::longValue;
        ToDoubleFunction<Integer> doubles = Integer::doubleValue;
        if (name.equals("toCollection")) return new Object[]{(Supplier<List<Integer>>) ArrayList::new};
        if (name.equals("joining")) return n == 1 ? new Object[]{","} : new Object[]{",", "[", "]"};
        if (name.equals("mapping")) return new Object[]{(Function<Integer, Integer>) v -> v + 1, Collectors.toList()};
        if (name.equals("flatMapping")) return new Object[]{(Function<Integer, Stream<Integer>>) v -> Stream.of(v, -v), Collectors.toList()};
        if (name.equals("filtering")) return new Object[]{(Predicate<Integer>) v -> v > 0, Collectors.toList()};
        if (name.equals("collectingAndThen")) return new Object[]{Collectors.toList(), (Function<List<Integer>, Integer>) List::size};
        if (name.equals("minBy") || name.equals("maxBy")) return new Object[]{Comparator.<Integer>naturalOrder()};
        if (name.endsWith("Int")) return new Object[]{ints};
        if (name.endsWith("Long")) return new Object[]{longs};
        if (name.endsWith("Double")) return new Object[]{doubles};
        if (name.equals("reducing")) return switch (n) {
            case 1 -> new Object[]{(BinaryOperator<Integer>) Integer::sum};
            case 2 -> new Object[]{Integer.valueOf(0), (BinaryOperator<Integer>) Integer::sum};
            case 3 -> new Object[]{Integer.valueOf(0), identity, (BinaryOperator<Integer>) Integer::sum};
            default -> throw new AssertionError(m);
        };
        if (name.equals("groupingBy") || name.equals("groupingByConcurrent")) {
            if (n == 1) return new Object[]{classifier};
            if (n == 2) return new Object[]{classifier, Collectors.summingInt(ints)};
            Supplier<?> factory = name.endsWith("Concurrent") ? ConcurrentSkipListMap::new : TreeMap::new;
            return new Object[]{classifier, factory, Collectors.summingInt(ints)};
        }
        if (name.equals("partitioningBy")) return n == 1 ? new Object[]{(Predicate<Integer>) v -> v > 0}
                : new Object[]{(Predicate<Integer>) v -> v > 0, Collectors.counting()};
        if (name.equals("toMap") || name.equals("toConcurrentMap") || name.equals("toUnmodifiableMap")) {
            if (n == 2) return new Object[]{identity, identity};
            if (n == 3) return new Object[]{classifier, identity, (BinaryOperator<Integer>) Integer::sum};
            Supplier<?> factory = name.equals("toConcurrentMap") ? ConcurrentSkipListMap::new : TreeMap::new;
            return new Object[]{classifier, identity, (BinaryOperator<Integer>) Integer::sum, factory};
        }
        if (name.equals("teeing")) return new Object[]{Collectors.summingInt(ints), Collectors.counting(),
                (BiFunction<Integer, Long, String>) (sum, count) -> sum + "/" + count};
        throw new AssertionError("UNMAPPED_COLLECTOR_OPERATION " + m.toGenericString());
    }

    private void edgeContracts() {
        M3LongArrayList longs = M3LongArrayList.copyOf(new long[]{3, 1, 2, 2, -1, Long.MIN_VALUE, Long.MAX_VALUE});
        var effects = new AtomicInteger();
        LongStream pending = longs.longStream().peek(v -> effects.incrementAndGet()).filter(v -> v > 0).limit(1);
        require(effects.get() == 0, "no traversal before terminal");
        equal(3L, pending.findFirst().orElseThrow(), "ordered short circuit");
        require(effects.get() == 1, "limit must not read unnecessary source elements");
        raises(IllegalStateException.class, pending::count); pending.close();
        equal("[1, 2, 3, 9223372036854775807]", Arrays.toString(longs.parallelLongStream().filter(v -> v > 0).distinct().sorted().toArray()), "parallel primitive order");
        M3IntHashSet ints = new M3IntHashSet(); for (int v : VALUES) ints.add(v);
        equal(Arrays.stream(VALUES).distinct().asLongStream().sum(), ints.parallelIntStream().asLongStream().sum(), "native int owner");
        M3LongFilteredView view = new M3LongFilteredView(longs, v -> v > 0);
        LongStream late = view.longStream(); longs.add(9);
        equal("[3, 1, 2, 2, 9223372036854775807, 9]", Arrays.toString(late.toArray()), "late filtered source"); late.close();
        M3LongDag dag = M3LongDag.from(longs).filter(v -> v > 0).map(v -> v & 7);
        try (LongStream s = dag.stream()) {
            equal("[1, 2, 3, 7]", Arrays.toString(s.mapMulti((v, out) -> out.accept(v)).distinct().sorted().toArray()), "all operations after DAG stream");
        }
        var close = new ArrayList<Integer>();
        try (LongStream s = longs.longStream().onClose(() -> close.add(1)).onClose(() -> close.add(2))) { s.limit(0).toArray(); }
        equal(List.of(1, 2), close, "close order");
        var nested = new AtomicInteger();
        try (LongStream s = longs.longStream().flatMap(v -> LongStream.of(v, -v).onClose(nested::incrementAndGet)).limit(1)) { s.toArray(); }
        require(nested.get() == 1, "flatMap closes short-circuited child");
        M3ArrayList<Integer> refs = new M3ArrayList<>(Arrays.asList(1, null, 2, null));
        List<Integer> immutable = refs.stream().toList();
        equal(Arrays.asList(1, null, 2, null), immutable, "toList permits nulls");
        raises(UnsupportedOperationException.class, () -> immutable.add(3));
        raises(NullPointerException.class, () -> refs.stream().collect(Collectors.toUnmodifiableList()));
        M3ArrayList<Integer> materialized = refs.stream().filter(Objects::nonNull).collect(Collectors.toCollection(M3ArrayList::new));
        equal(List.of(1, 2), materialized, "M3 collection collector");
        var fp = new M3ArrayList<>(Arrays.asList(-0.0, 0.0, Double.NaN, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        double[] actual = fp.parallelStream().mapToDouble(Double::doubleValue).distinct().sorted().toArray();
        double[] expected = DoubleStream.of(-0.0, 0.0, Double.NaN, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY).distinct().sorted().toArray();
        equal(Arrays.toString(expected), Arrays.toString(actual), "double NaN and signed zero");
        require(Double.doubleToRawLongBits(actual[1]) != Double.doubleToRawLongBits(actual[2]), "signed zero survives distinct");
        raises(IllegalArgumentException.class, () -> longs.longStream().limit(-1));
        raises(IllegalArgumentException.class, () -> longs.longStream().skip(-1));
        try (Stream<Integer> s = Stream.ofNullable(null)) { equal(0L, s.count(), "null source"); }
        try (LongStream s = longs.longStream().filter(v -> false)) {
            equal(OptionalLong.empty(), s.findFirst(), "empty result");
        }
        RuntimeException primary = new IllegalArgumentException("primary"), cleanup = new IllegalStateException("close");
        try (LongStream s = longs.longStream().onClose(() -> { throw cleanup; })) {
            s.forEach(v -> { throw primary; }); throw new AssertionError("exception not thrown");
        } catch (RuntimeException observed) {
            require(observed == primary && observed.getSuppressed().length == 1 && observed.getSuppressed()[0] == cleanup,
                    "original callback exception and suppressed close preserved");
        }
    }
    private void equal(Object expected, Object actual, String detail) {
        checks++; if (!Objects.equals(expected, actual)) throw new AssertionError(detail + " expected=" + expected + " actual=" + actual);
    }
    private void require(boolean condition, String detail) {
        checks++; if (!condition) throw new AssertionError(detail);
    }
    private void raises(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable error) {
            require(error.getClass() == type, "exception type: " + error); return;
        }
        throw new AssertionError("missing " + type.getName());
    }
}
