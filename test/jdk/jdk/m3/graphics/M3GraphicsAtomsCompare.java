/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Structural comparison of an M3 value with the oracle value of the same call. Primitives compare exactly (float and
 * double by their Float.equals/Double.equals bits: -0.0 differs from 0.0, every NaN is one value); enums by name and
 * ordinal; arrays, lists, sets and maps element by element; donor objects by their public fields, their public no-arg
 * getters (get/is/has) and, when the donor declares them, hashCode and toString, recursively to depth
 * {@link #MAX_DEPTH}; other JDK objects by equals when the class declares it, else by toString when declared. Types
 * compare through the rename (M3 class name = renamed donor class name). An M3 string equals the oracle string when
 * it does after removing the rename's {@code M3} prefix before an upper-case identifier (toString carries the class
 * name); such matches are counted ({@link #normalizedAndReset()}) and reported per atom. Two rules keep the
 * comparison independent of the run: a hashCode is compared only when it is reproducible
 * ({@link M3GraphicsAtomsHashes}: an enum or identity-hashed part makes it differ between any two objects), and a URL
 * compares by presence, since it points into the side's own class-loader layout; both are counted per atom.
 */
final class M3GraphicsAtomsCompare {
    /** A call result: a value, or the throwable of the call. */
    record Result(Object value, Throwable thrown) {}

    private static final int MAX_DEPTH = 3;
    private static final int MAX_SHOWN = 160;
    private static final Pattern RENAMED = Pattern.compile("(?<![\\w$])M3(?=[A-Z])");
    private static final Pattern GETTER = Pattern.compile("(get|is|has)[A-Z_].*");
    private static final String EQUALS = "equals";
    private static final String HASH_CODE = "hashCode";
    private static final String TO_STRING = "toString";
    private static final String EMPTY = "";
    private static final String DOT = ".";
    private static final String CALL = "()";
    private static final String OPEN = "[";
    private static final String CLOSE = "]";
    private static final String SIZE = ".size";
    private static final String KEY = ".key";
    private static final String COLON = ": M3 ";
    private static final String VERSUS = " / oracle ";
    private static final String ELLIPSIS = "...";
    private static final String MISSING = ": member missing on the M3 side: ";
    private static final Optional<String> SAME = Optional.empty();
    private static final AtomicInteger NORMALIZED = new AtomicInteger();
    private static final AtomicInteger ENVIRONMENT = new AtomicInteger();

    private M3GraphicsAtomsCompare() {}

    /** The first difference between an M3 value and an oracle value, empty when they agree. */
    static Optional<String> diff(String path, Object m3, Object oracle) {
        return diff(m3, oracle, 0, path);
    }

    /** The first difference between two call results (values, or throwables compared by type). */
    static Optional<String> outcomes(String path, Result m3, Result oracle) {
        return outcomes(m3, oracle, 0, path);
    }

    /** True when an M3 class is the renamed oracle class (arrays by component; hidden classes match each other). */
    static boolean sameType(Class<?> m3, Class<?> oracle) {
        if (m3.isArray() || oracle.isArray()) {
            return m3.isArray() && oracle.isArray() && sameType(m3.getComponentType(), oracle.getComponentType());
        }
        if (m3.isHidden() || oracle.isHidden()) {
            return m3.isHidden() && oracle.isHidden();
        }
        String o = oracle.getName();
        return m3.getName().equals(M3GraphicsAtomsOracle.isDonor(o) ? M3GraphicsAtomsOracle.m3Name(o) : o);
    }

    /** The number of string matches that needed the rename normalization since the last call. */
    static int normalizedAndReset() {
        return NORMALIZED.getAndSet(0);
    }

    /** A short printable form of a value for mismatch messages. */
    static String show(Object v) {
        String s = v instanceof Object[] a ? Arrays.deepToString(a) : v != null && v.getClass().isArray()
                ? IntStream.range(0, Array.getLength(v)).mapToObj(i -> String.valueOf(Array.get(v, i))).toList()
                        .toString()
                : String.valueOf(v);
        return s.length() > MAX_SHOWN ? s.substring(0, MAX_SHOWN) + ELLIPSIS : s;
    }

    private static Optional<String> diff(Object m, Object o, int depth, String path) {
        if (m == null || o == null) {
            return m == o ? SAME : differ(path, m, o);
        }
        if (!sameType(m.getClass(), o.getClass())) {
            return differ(path, m.getClass().getName(), o.getClass().getName());
        }
        return m.getClass().isArray() ? arrays(m, o, depth, path) : values(m, o, depth, path);
    }

    private static Optional<String> values(Object m, Object o, int depth, String path) {
        if (m instanceof Float f) {
            return check(Float.floatToIntBits(f) == Float.floatToIntBits((Float) o), path, m, o);
        }
        if (m instanceof Double d) {
            return check(Double.doubleToLongBits(d) == Double.doubleToLongBits((Double) o), path, m, o);
        }
        if (m instanceof String s) {
            return strings(s, (String) o, path);
        }
        if (m instanceof Enum<?> e) {
            Enum<?> oe = (Enum<?>) o;
            return check(e.name().equals(oe.name()) && e.ordinal() == oe.ordinal(), path, m, o);
        }
        if (M3GraphicsAtomsOracle.isDonor(o.getClass().getName())) {
            return depth < MAX_DEPTH ? objects(m, o, depth + 1, path) : SAME;
        }
        if (o instanceof URL) {
            ENVIRONMENT.incrementAndGet();
            return SAME;
        }
        return collections(m, o, depth, path).orElseGet(() -> jdk(m, o, path));
    }

    /** The number of comparisons settled by the environment rule (a URL compares by presence) since the last call. */
    static int environmentAndReset() {
        return ENVIRONMENT.getAndSet(0);
    }

    private static Optional<Optional<String>> collections(Object m, Object o, int depth, String path) {
        if (m instanceof Map<?, ?> mm && o instanceof Map<?, ?> om) {
            return Optional.of(sequences(sortedEntries(mm), sortedEntries(om), depth, path));
        }
        if (m instanceof List<?> ml && o instanceof List<?> ol) {
            return Optional.of(sequences(ml, ol, depth, path));
        }
        if (m instanceof Collection<?> mc && o instanceof Collection<?> oc) {
            return Optional.of(sequences(sorted(mc), sorted(oc), depth, path));
        }
        return Optional.empty();
    }

    private static List<Object> sortedEntries(Map<?, ?> map) {
        return map.entrySet().stream().sorted(Comparator.comparing(e -> normalized(String.valueOf(e.getKey()))))
                .flatMap(e -> Stream.of(e.getKey(), e.getValue())).toList();
    }

    private static List<?> sorted(Collection<?> c) {
        return c.stream().sorted(Comparator.comparing(v -> normalized(String.valueOf(v)))).toList();
    }

    private static Optional<String> sequences(List<?> m, List<?> o, int depth, String path) {
        if (m.size() != o.size()) {
            return differ(path + SIZE, m.size(), o.size());
        }
        return IntStream.range(0, m.size()).mapToObj(i -> diff(m.get(i), o.get(i), depth, path + OPEN + i + CLOSE))
                .flatMap(Optional::stream).findFirst();
    }

    private static Optional<String> arrays(Object m, Object o, int depth, String path) {
        int n = Array.getLength(m);
        if (n != Array.getLength(o)) {
            return differ(path + SIZE, n, Array.getLength(o));
        }
        return IntStream.range(0, n)
                .mapToObj(i -> diff(Array.get(m, i), Array.get(o, i), depth, path + OPEN + i + CLOSE))
                .flatMap(Optional::stream).findFirst();
    }

    private static Optional<String> objects(Object m, Object o, int depth, String path) {
        return Stream.<Supplier<Optional<String>>>of(() -> fields(m, o, depth, path), () -> getters(m, o, depth, path),
                () -> declared(m, o, depth, path)).map(Supplier::get).flatMap(Optional::stream).findFirst();
    }

    private static Optional<String> fields(Object m, Object o, int depth, String path) {
        return Arrays.stream(o.getClass().getFields()).filter(f -> !Modifier.isStatic(f.getModifiers()))
                .sorted(Comparator.comparing(Field::getName))
                .map(f -> field(m, o, f, depth, path + DOT + f.getName())).flatMap(Optional::stream).findFirst();
    }

    private static Optional<String> field(Object m, Object o, Field of, int depth, String path) {
        try {
            return diff(m.getClass().getField(of.getName()).get(m), of.get(o), depth, path);
        } catch (ReflectiveOperationException e) {
            return Optional.of(path + MISSING + e);
        }
    }

    private static Optional<String> getters(Object m, Object o, int depth, String path) {
        return Arrays.stream(o.getClass().getMethods()).filter(M3GraphicsAtomsCompare::isGetter)
                .map(Method::getName).distinct().sorted()
                .map(g -> outcomes(call(m, g), call(o, g), depth, path + DOT + g + CALL))
                .flatMap(Optional::stream).findFirst();
    }

    private static boolean isGetter(Method g) {
        return g.getParameterCount() == 0 && !Modifier.isStatic(g.getModifiers()) && g.getReturnType() != void.class
                && GETTER.matcher(g.getName()).matches()
                && M3GraphicsAtomsOracle.isDonor(g.getDeclaringClass().getName());
    }

    private static Optional<String> declared(Object m, Object o, int depth, String path) {
        boolean hashed = declares(o.getClass(), HASH_CODE);
        boolean stable = hashed && M3GraphicsAtomsHashes.stable(o);
        if (hashed && !stable) {
            M3GraphicsAtomsHashes.skipped();
        }
        Optional<String> hash = stable
                ? outcomes(call(m, HASH_CODE), call(o, HASH_CODE), depth, path + DOT + HASH_CODE + CALL) : SAME;
        return hash.or(() -> declares(o.getClass(), TO_STRING)
                ? outcomes(call(m, TO_STRING), call(o, TO_STRING), depth, path + DOT + TO_STRING + CALL) : SAME);
    }

    private static boolean declares(Class<?> c, String name) {
        try {
            return M3GraphicsAtomsOracle.isDonor(c.getMethod(name).getDeclaringClass().getName());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Result call(Object target, String name) {
        try {
            return new Result(target.getClass().getMethod(name).invoke(target), null);
        } catch (InvocationTargetException e) {
            return new Result(null, e.getCause());
        } catch (ReflectiveOperationException | RuntimeException e) {
            return new Result(null, e);
        }
    }

    private static Optional<String> outcomes(Result m, Result o, int depth, String path) {
        if (m.thrown() == null && o.thrown() == null) {
            return diff(m.value(), o.value(), depth, path);
        }
        boolean both = m.thrown() != null && o.thrown() != null;
        return both && sameType(m.thrown().getClass(), o.thrown().getClass()) ? SAME
                : differ(path, m.thrown() != null ? m.thrown() : m.value(),
                        o.thrown() != null ? o.thrown() : o.value());
    }

    private static Optional<String> jdk(Object m, Object o, String path) {
        if (overrides(o.getClass(), EQUALS, Object.class)) {
            Result same = guarded(() -> m.equals(o));
            return same.thrown() == null && Boolean.TRUE.equals(same.value()) ? SAME : differ(path, m, o);
        }
        return overrides(o.getClass(), TO_STRING) ? outcomes(guarded(m::toString), guarded(o::toString), 0, path)
                : SAME;
    }

    private static Result guarded(Supplier<Object> call) {
        try {
            return new Result(call.get(), null);
        } catch (RuntimeException e) {
            return new Result(null, e);
        }
    }

    private static boolean overrides(Class<?> c, String name, Class<?>... params) {
        try {
            return c.getMethod(name, params).getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Optional<String> strings(String m, String o, String path) {
        if (m.equals(o)) {
            return SAME;
        }
        if (!normalized(m).equals(o)) {
            return differ(path, m, o);
        }
        NORMALIZED.incrementAndGet();
        return SAME;
    }

    private static String normalized(String s) {
        return RENAMED.matcher(s).replaceAll(EMPTY);
    }

    private static Optional<String> check(boolean same, String path, Object m, Object o) {
        return same ? SAME : differ(path, m, o);
    }

    private static Optional<String> differ(String path, Object m, Object o) {
        return Optional.of(path + COLON + show(m) + VERSUS + show(o));
    }
}
