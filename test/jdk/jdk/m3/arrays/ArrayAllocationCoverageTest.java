// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** Detect allocation signature changes without matching runnable correctness/performance selectors. */
final class ArrayAllocationCoverageTest {
    private static final String PREFIX = "allocate";
    private static final String SUFFIX = "Required";

    @Test void eachAllocationAtomOwnsCorrectnessAndPerformanceSelectors() {
        Map<String, Class<?>> returns = Map.of(
                "allocateObjectsRequired", Object[].class,
                "allocateBooleansRequired", boolean[].class,
                "allocateBytesRequired", byte[].class,
                "allocateCharsRequired", char[].class,
                "allocateShortsRequired", short[].class,
                "allocateIntsRequired", int[].class,
                "allocateLongsRequired", long[].class,
                "allocateFloatsRequired", float[].class,
                "allocateDoublesRequired", double[].class);
        Set<String> expected = returns.entrySet().stream()
                .map(e -> "public static " + e.getKey() + "(int):" + e.getValue().getTypeName())
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> actual = Arrays.stream(ArrayNative.class.getDeclaredMethods())
                .filter(m -> allocationName(m.getName())).map(ArrayAllocationCoverageTest::signature)
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(expected, actual, "each typed allocation atom needs its declared public static API");

        Set<String> atoms = Arrays.stream(ArrayNative.class.getDeclaredMethods())
                .map(Method::getName).filter(ArrayAllocationCoverageTest::allocationName)
                .map(n -> n.substring(PREFIX.length(), n.length() - SUFFIX.length()).toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(Set.of("objects", "booleans", "bytes", "chars", "shorts", "ints", "longs", "floats", "doubles"), atoms);
        Class<?> performance = assertDoesNotThrow(() -> Class.forName("jdk.m3.arrays.ArrayAllocationPerformanceTest"),
                "each allocation atom needs a performance test");
        assertEquals(atoms, selectors(performance));
        Set<String> correctness = selectors(ArrayAllocationAtomsTest.class);
        assertTrue(correctness.containsAll(atoms), "missing correctness selector");
    }

    private static boolean allocationName(String name) {
        return name.startsWith(PREFIX) && name.endsWith(SUFFIX);
    }

    private static String signature(Method method) {
        String visibility = Modifier.isPublic(method.getModifiers()) ? "public " : "nonpublic ";
        String storage = Modifier.isStatic(method.getModifiers()) ? "static " : "instance ";
        String parameters = Arrays.stream(method.getParameterTypes()).map(Class::getTypeName)
                .collect(Collectors.joining(","));
        return visibility + storage + method.getName() + "(" + parameters + "):"
                + method.getReturnType().getTypeName();
    }

    private static Set<String> selectors(Class<?> owner) {
        if (owner.isAnnotationPresent(Disabled.class)) return Set.of();
        return Arrays.stream(owner.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Test.class))
                .filter(m -> !m.isAnnotationPresent(Disabled.class))
                .filter(m -> !Modifier.isPrivate(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()))
                .filter(m -> m.getParameterCount() == 0 && m.getReturnType() == void.class)
                .map(Method::getName).collect(Collectors.toCollection(TreeSet::new));
    }
}
