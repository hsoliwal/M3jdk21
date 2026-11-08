// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Reference contract of recipe/progress-algorithm-20261108 (M3JDK-authored): the relocated functional contract
 * executes its body with the supplied monitor, observes cancellation and interruption through the (now public)
 * M3ProgressChecks, composes by lambda, and exposes exactly one abstract method.
 */
//FLLDCIM: TOP - reference contract of recipe/progress-algorithm-20261108 (M3JDK-authored, test scope)
final class ProgressAlgorithmTest {

    @Test
    void executesTheBodyWithTheSuppliedMonitor() {
        AtomicInteger worked = new AtomicInteger();
        ProgressMonitor counting = new ProgressMonitor() {
            @Override
            public void worked(long work) {
                worked.addAndGet((int) work);
            }
        };
        ProgressAlgorithm<int[], Long> sum = (input, monitor) -> {
            long total = 0L;
            for (int value : input) {
                AlgorithmSupport.checkCanceled(monitor);
                total += value;
                monitor.worked(1L);
            }
            return total;
        };
        assertEquals(6L, sum.execute(new int[] {1, 2, 3}, counting));
        assertEquals(3, worked.get());
        assertEquals(0L, sum.execute(new int[0], ProgressMonitor.none()));
    }

    @Test
    void canceledMonitorAndInterruptionAreObservedThroughTheChecks() {
        ProgressAlgorithm<Integer, Integer> identity = (input, monitor) -> {
            AlgorithmSupport.checkCanceled(AlgorithmSupport.nonNull(monitor));
            return input;
        };
        CancelableProgressMonitor canceled = new CancelableProgressMonitor();
        canceled.setCanceled(true);
        assertThrows(CancellationException.class, () -> identity.execute(7, canceled));
        assertEquals(7, identity.execute(7, null));
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class, () -> identity.execute(7, ProgressMonitor.none()));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void composesByLambdaWithoutDroppingTheMonitor() {
        List<ProgressMonitor> seen = new java.util.ArrayList<>();
        ProgressAlgorithm<Integer, Integer> increment = (input, monitor) -> {
            seen.add(monitor);
            return input + 1;
        };
        ProgressAlgorithm<Integer, Integer> twice = (input, monitor) ->
                increment.execute(increment.execute(input, monitor), monitor);
        ProgressMonitor monitor = ProgressMonitor.none();
        assertEquals(5, twice.execute(3, monitor));
        assertEquals(2, seen.size());
        assertSame(monitor, seen.get(0));
        assertSame(monitor, seen.get(1));
    }

    @Test
    void contractIsOneAbstractMethodAndTheChecksArePublic() {
        assertTrue(ProgressAlgorithm.class.isAnnotationPresent(FunctionalInterface.class));
        List<Method> abstractMethods = Arrays.stream(ProgressAlgorithm.class.getMethods())
                .filter(method -> Modifier.isAbstract(method.getModifiers())).toList();
        assertEquals(1, abstractMethods.size());
        assertEquals("execute", abstractMethods.get(0).getName());
        assertEquals(List.of(Object.class, ProgressMonitor.class),
                List.of(abstractMethods.get(0).getParameterTypes()));
        assertTrue(Modifier.isPublic(AlgorithmSupport.class.getModifiers()));
        for (String name : List.of("nonNull", "checkCanceled")) {
            Method method = Arrays.stream(AlgorithmSupport.class.getMethods())
                    .filter(candidate -> candidate.getName().equals(name)).findFirst().orElseThrow();
            assertTrue(Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers()), name);
        }
    }
}
