// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import jdk.m3.nlp.M3BackoffWordNgrams;
import jdk.m3.nlp.M3BackoffWordNgramsTrainingArrayFault;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Source-instrumented growth test; not a claim of real JVM memory exhaustion. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipes/nlp-wordrank-20261031/TrainingGrowthOracle.java blob 4b898d740867; recipe m3/nlp/recipe/backoff-word-ngrams-20261106/relocate.py v3
public final class M3BackoffWordNgramsTrainingGrowthOracle {
    private static int checks;
    private M3BackoffWordNgramsTrainingGrowthOracle() { }
    private static void check(boolean ok, String label) {
        checks++; if (!ok) throw new AssertionError(label);
    }
    private record Saved(Object reference, Object copy) { }
    private static Map<Field,Saved> snapshot(Object row) throws IllegalAccessException {
        Map<Field,Saved> result = new LinkedHashMap<>();
        for (Field f : row.getClass().getDeclaredFields()) if (!Modifier.isStatic(f.getModifiers())) {
            f.setAccessible(true); Object value = f.get(row), copy = value;
            if (value.getClass().isArray()) {
                int n = Array.getLength(value); copy = Array.newInstance(value.getClass().getComponentType(), n);
                System.arraycopy(value, 0, copy, 0, n);
            }
            result.put(f, new Saved(value, copy));
        }
        return result;
    }
    private static void same(Object row, Map<Field,Saved> snapshot) throws IllegalAccessException {
        for (var entry : snapshot.entrySet()) {
            Object current = entry.getKey().get(row); Saved saved = entry.getValue();
            if (current.getClass().isArray()) {
                check(current == saved.reference(), "unchanged array reference " + entry.getKey().getName());
                check(Arrays.deepEquals(new Object[] {current}, new Object[] {saved.copy()}), "unchanged array contents");
            } else check(current.equals(saved.copy()), "unchanged scalar " + entry.getKey().getName());
        }
    }
    public static void main(String[] args) throws Exception {
        Class<?> type = Class.forName(M3BackoffWordNgrams.class.getName() + "$MutableCounts");
        var constructor = type.getDeclaredConstructor(); constructor.setAccessible(true);
        Method add = type.getDeclaredMethod("add", String.class); add.setAccessible(true);
        Method freeze = type.getDeclaredMethod("freeze"); freeze.setAccessible(true);
        Class<?> frozen = Class.forName(M3BackoffWordNgrams.class.getName() + "$Counts");
        Method count = frozen.getDeclaredMethod("count", String.class); count.setAccessible(true);
        int injected = 0;
        for (int initial : new int[] {0, 3, 7, 15, 31}) for (int at = 1; at <= 5; at++) {
            M3BackoffWordNgramsTrainingArrayFault.configure(0); Object row = constructor.newInstance();
            for (int i = 0; i < initial; i++) add.invoke(row, "word-" + i);
            var before = snapshot(row); String next = "word-" + initial;
            M3BackoffWordNgramsTrainingArrayFault.configure(at);
            try { add.invoke(row, next); throw new AssertionError("missing injected failure"); }
            catch (InvocationTargetException expected) {
                check(expected.getCause() instanceof OutOfMemoryError, "allocation error type");
                check(expected.getCause().getMessage().equals("injected-array-copy-" + at), "failure location");
                injected++;
            }
            check(M3BackoffWordNgramsTrainingArrayFault.attempts() == at, "no later allocations after failure");
            same(row, before);
            M3BackoffWordNgramsTrainingArrayFault.configure(0); add.invoke(row, next);
            Object snapshot = freeze.invoke(row);
            for (int i = 0; i <= initial; i++) check((long)count.invoke(snapshot, "word-" + i) == 1, "recovery preserves every count");
        }
        System.out.println("TRAINING_GROWTH_PASS injected=" + injected + " checks=" + checks);
    }
}
