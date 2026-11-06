// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import jdk.m3.nlp.M3BackoffWordNgrams;
import jdk.m3.nlp.M3BackoffWordNgramsTrainingContextArrayFault;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/** Fault/AVL probe for the private training-context builder; not SDK/JUnit execution. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipes/nlp-wordrank-20261031/TrainingContextGrowthOracle.java blob 68d8bbbaba4b; recipe m3/nlp/recipe/backoff-word-ngrams-20261106/relocate.py v3
public final class M3BackoffWordNgramsTrainingContextGrowthOracle {
    private static long checks;
    private M3BackoffWordNgramsTrainingContextGrowthOracle() { }
    private static void check(boolean ok, String label) { checks++; if (!ok) throw new AssertionError(label); }
    public static void main(String[] args) throws Exception {
        Class<?> type = Class.forName(M3BackoffWordNgrams.class.getName() + "$MutableContextIndex");
        var ctor = type.getDeclaredConstructor(); ctor.setAccessible(true);
        Method root = type.getDeclaredMethod("root", int.class); root.setAccessible(true);
        Method child = type.getDeclaredMethod("childOrCreate", int.class, String.class, int.class); child.setAccessible(true);
        Field rootIdField = type.getDeclaredField("ROOT"); rootIdField.setAccessible(true);
        int rootId = rootIdField.getInt(null);
        String[] arrays = {"labels", "childRoots", "left", "right", "heights", "rows"};
        for (int failure = 1; failure <= 6; failure++) {
            Object index = ctor.newInstance();
            M3BackoffWordNgramsTrainingContextArrayFault.healthy();
            root.invoke(index, 1000);
            child.invoke(index, rootId, "b", 1000);
            child.invoke(index, rootId, "d", 1000);
            Object[] refs = new Object[arrays.length];
            Object[] copies = new Object[arrays.length];
            for (int i = 0; i < arrays.length; i++) {
                Field f = type.getDeclaredField(arrays[i]); f.setAccessible(true);
                refs[i] = f.get(index);
                copies[i] = cloneArray(refs[i]);
            }
            Field size = type.getDeclaredField("size"); size.setAccessible(true);
            Field contexts = type.getDeclaredField("contexts"); contexts.setAccessible(true);
            int sizeBefore = size.getInt(index), contextsBefore = contexts.getInt(index);
            M3BackoffWordNgramsTrainingContextArrayFault.failAt(failure);
            try {
                child.invoke(index, rootId, "f", 1000);
                throw new AssertionError("missing injected OOME " + failure);
            } catch (InvocationTargetException expected) {
                check(expected.getCause() instanceof OutOfMemoryError, "failure type " + failure);
            }
            for (int i = 0; i < arrays.length; i++) {
                Field f = type.getDeclaredField(arrays[i]); f.setAccessible(true);
                Object current = f.get(index);
                check(current == refs[i], "array reference unchanged " + arrays[i] + " failure=" + failure);
                check(arrayEquals(current, copies[i]), "array content unchanged " + arrays[i] + " failure=" + failure);
            }
            check(size.getInt(index) == sizeBefore, "size unchanged");
            check(contexts.getInt(index) == contextsBefore, "context count unchanged");
            M3BackoffWordNgramsTrainingContextArrayFault.healthy();
            int created = (Integer) child.invoke(index, rootId, "f", 1000);
            check(created > 0 && size.getInt(index) == sizeBefore + 1, "healthy recovery");
        }
        // Ascending sibling insertion is adversarial for an unbalanced tree.
        Object index = ctor.newInstance(); root.invoke(index, 10_000);
        for (int i = 0; i < 1000; i++) child.invoke(index, rootId, String.format("k%04d", i), 10_000);
        Field childRoots = type.getDeclaredField("childRoots"); childRoots.setAccessible(true);
        Field left = type.getDeclaredField("left"); left.setAccessible(true);
        Field right = type.getDeclaredField("right"); right.setAccessible(true);
        Field heights = type.getDeclaredField("heights"); heights.setAccessible(true);
        int[] roots = (int[]) childRoots.get(index), l = (int[]) left.get(index), r = (int[]) right.get(index);
        byte[] h = (byte[]) heights.get(index);
        int seen = verifyTree(roots[rootId], l, r, h);
        check(seen == 1000, "all siblings reachable");
        System.out.println("TRAINING_CONTEXT_GROWTH_PASS checks=" + checks + " siblings=" + seen);
    }
    private static int verifyTree(int node, int[] left, int[] right, byte[] heights) {
        if (node == 0) return 0;
        int lh = left[node] == 0 ? 0 : Byte.toUnsignedInt(heights[left[node]]);
        int rh = right[node] == 0 ? 0 : Byte.toUnsignedInt(heights[right[node]]);
        check(Math.abs(lh - rh) <= 1, "AVL balance node=" + node);
        check(Byte.toUnsignedInt(heights[node]) == 1 + Math.max(lh, rh), "AVL height node=" + node);
        return 1 + verifyTree(left[node], left, right, heights) + verifyTree(right[node], left, right, heights);
    }
    private static Object cloneArray(Object array) {
        if (array instanceof Object[] a) return a.clone();
        if (array instanceof int[] a) return a.clone();
        if (array instanceof byte[] a) return a.clone();
        throw new AssertionError(array.getClass());
    }
    private static boolean arrayEquals(Object a, Object b) {
        if (a instanceof Object[] x && b instanceof Object[] y) return Arrays.equals(x, y);
        if (a instanceof int[] x && b instanceof int[] y) return Arrays.equals(x, y);
        if (a instanceof byte[] x && b instanceof byte[] y) return Arrays.equals(x, y);
        return false;
    }
}
