// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Detect an allocation API addition without matching correctness/performance selectors. */
final class M3ArrayAllocationCoverageTest {
    private static final String PREFIX = "allocate";
    private static final String SUFFIX = "Required";

    @Test void eachAllocationAtomOwnsCorrectnessAndPerformanceSelectors() {
        Set<String> atoms = Arrays.stream(M3ArrayNative.class.getDeclaredMethods())
                .map(Method::getName).filter(n -> n.startsWith(PREFIX) && n.endsWith(SUFFIX))
                .map(n -> n.substring(PREFIX.length(), n.length() - SUFFIX.length()).toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(Set.of("objects", "booleans", "bytes", "chars", "shorts", "ints", "longs", "floats", "doubles"), atoms);
        Class<?> performance = assertDoesNotThrow(() -> Class.forName("com.m3.arrays.M3ArrayAllocationPerformanceTest"),
                "each allocation atom needs a performance test");
        assertEquals(atoms, selectors(performance));
        Set<String> correctness = selectors(M3ArrayAllocationAtomsTest.class);
        assertTrue(correctness.containsAll(atoms), "missing correctness selector");
    }

    private static Set<String> selectors(Class<?> owner) {
        return Arrays.stream(owner.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Test.class))
                .map(Method::getName).collect(Collectors.toCollection(TreeSet::new));
    }
}
