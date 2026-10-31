/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Donor objects in the structural comparison of M3GraphicsAtomsCompare: their public fields, their public no-arg
 * getters (get/is/has) and, when the donor declares them, hashCode and toString, recursively. A pair already under
 * comparison counts as equal (cycles); below {@link #MAX_DEPTH} levels only hashCode and toString compare, so a deep
 * pair is still compared instead of passing unseen. A hashCode compares only when it is exact
 * (M3GraphicsAtomsHashes.exact); a skipped one is counted.
 */
final class M3GraphicsAtomsObjects {
    private static final int MAX_DEPTH = 8;
    private static final Pattern GETTER = Pattern.compile("(get|is|has)[A-Z_].*");
    private static final String HASH_CODE = "hashCode";
    private static final String TO_STRING = "toString";
    private static final String DOT = ".";
    private static final String CALL = "()";
    private static final String MISSING = ": member missing on the M3 side: ";
    private static final ThreadLocal<Map<Object, Object>> VISITING = ThreadLocal.withInitial(IdentityHashMap::new);

    private M3GraphicsAtomsObjects() {}

    /** Donor objects: a pair under comparison is equal (cycle); deep pairs compare by hashCode and toString only. */
    static Optional<String> donor(Object m, Object o, int depth, String path) {
        Map<Object, Object> visiting = VISITING.get();
        if (visiting.get(m) == o) {
            return Optional.empty();
        }
        Object outer = visiting.put(m, o);
        try {
            return depth < MAX_DEPTH ? objects(m, o, depth + 1, path) : declared(m, o, depth, path);
        } finally {
            if (outer == null) {
                visiting.remove(m);
            } else {
                visiting.put(m, outer);
            }
        }
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
            return M3GraphicsAtomsCompare.diff(m.getClass().getField(of.getName()).get(m), of.get(o), depth,
                    path);
        } catch (ReflectiveOperationException e) {
            return Optional.of(path + MISSING + e);
        }
    }

    private static Optional<String> getters(Object m, Object o, int depth, String path) {
        return Arrays.stream(o.getClass().getMethods()).filter(M3GraphicsAtomsObjects::isGetter)
                .map(Method::getName).distinct().sorted()
                .map(g -> M3GraphicsAtomsCompare.outcomes(call(m, g), call(o, g), depth, path + DOT + g + CALL))
                .flatMap(Optional::stream).findFirst();
    }

    private static boolean isGetter(Method g) {
        return g.getParameterCount() == 0 && !Modifier.isStatic(g.getModifiers()) && g.getReturnType() != void.class
                && GETTER.matcher(g.getName()).matches()
                && M3GraphicsAtomsOracle.isDonor(g.getDeclaringClass().getName());
    }

    private static Optional<String> declared(Object m, Object o, int depth, String path) {
        boolean hashed = declares(o.getClass(), HASH_CODE);
        boolean exact = hashed && M3GraphicsAtomsHashes.exact(o);
        if (hashed && !exact) {
            M3GraphicsAtomsHashes.skipped();
        }
        Optional<String> hash = exact ? compared(m, o, HASH_CODE, depth, path) : Optional.empty();
        return hash.or(() -> declares(o.getClass(), TO_STRING) ? compared(m, o, TO_STRING, depth, path)
                : Optional.empty());
    }

    private static Optional<String> compared(Object m, Object o, String method, int depth, String path) {
        return M3GraphicsAtomsCompare.outcomes(call(m, method), call(o, method), depth, path + DOT + method + CALL);
    }

    private static boolean declares(Class<?> c, String name) {
        try {
            return M3GraphicsAtomsOracle.isDonor(c.getMethod(name).getDeclaringClass().getName());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static M3GraphicsAtomsCompare.Result call(Object target, String name) {
        try {
            return new M3GraphicsAtomsCompare.Result(target.getClass().getMethod(name).invoke(target), null);
        } catch (InvocationTargetException e) {
            return new M3GraphicsAtomsCompare.Result(null, e.getCause());
        } catch (ReflectiveOperationException | RuntimeException e) {
            return new M3GraphicsAtomsCompare.Result(null, e);
        }
    }
}
