// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import jdk.m3.nlp.M3BackoffWordNgrams;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Independent row-count oracle; no replacement for official SDK/JUnit qualification. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipes/nlp-wordrank-20261031/WordTrainingOracle.java blob 17781eecf1b2; recipe m3/nlp/recipe/backoff-word-ngrams-20261106/relocate.py v3
public final class M3BackoffWordNgramsTrainingOracle {
    private static final Class<?> ROW = nested("MutableCounts");
    private static final Class<?> FROZEN = nested("Counts");
    private static final Constructor<?> CONSTRUCTOR = constructor();
    private static final MethodHandle ADD = handle(ROW, "add", void.class, String.class);
    private static final MethodHandle HAS = handle(ROW, "has", boolean.class, String.class);
    private static final MethodHandle FREEZE = handle(ROW, "freeze", Object.class);
    private static final MethodHandle COUNT = handle(FROZEN, "count", long.class, String.class);
    private static long checks;
    private static volatile Object sink;
    private M3BackoffWordNgramsTrainingOracle() { }
    private static Class<?> nested(String name) {
        try { return Class.forName(M3BackoffWordNgrams.class.getName() + "$" + name); }
        catch (ClassNotFoundException e) { throw new AssertionError(e); }
    }
    private static Constructor<?> constructor() {
        try { var c = ROW.getDeclaredConstructor(); c.setAccessible(true); return c; }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static MethodHandle handle(Class<?> owner, String name, Class<?> result, Class<?>... parameters) {
        try {
            Method method = owner.getDeclaredMethod(name, parameters); method.setAccessible(true);
            Class<?>[] arguments = new Class<?>[parameters.length + 1]; arguments[0] = Object.class;
            System.arraycopy(parameters, 0, arguments, 1, parameters.length);
            return MethodHandles.lookup().unreflect(method).asType(MethodType.methodType(result, arguments));
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static Object row() throws ReflectiveOperationException { return CONSTRUCTOR.newInstance(); }
    private static Field field(Class<?> type, String name) {
        try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
        catch (ReflectiveOperationException e) { throw new AssertionError("missing array field " + name, e); }
    }
    private static void check(boolean ok, String message) {
        checks++; if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Throwable {
        if (Boolean.getBoolean("m3.proof.requireNamedModule")
                && !"org.m3jdk.nlp".equals(M3BackoffWordNgrams.class.getModule().getName()))
            throw new AssertionError("named wordngram module not loaded");
        if (args.length != 1) throw new IllegalArgumentException("--behavior|--layout|--overflow|--allocation|--public-allocation");
        switch (args[0]) {
            case "--behavior" -> behavior();
            case "--layout" -> layout();
            case "--overflow" -> overflow();
            case "--allocation" -> allocation();
            case "--public-allocation" -> publicAllocation();
            default -> throw new IllegalArgumentException(args[0]);
        }
    }
    private static List<String> collisionKeys() {
        List<String> words = new ArrayList<>(List.of(""));
        for (int i = 0; i < 8; i++) {
            List<String> next = new ArrayList<>();
            for (String s : words) { next.add(s + "Aa"); next.add(s + "BB"); }
            words = next;
        }
        int hash = words.get(0).hashCode();
        for (String word : words) check(word.hashCode() == hash, "collision fixture");
        return words;
    }
    private static void behavior() throws Throwable {
        List<String> vocabulary = new ArrayList<>(collisionKeys());
        vocabulary.addAll(List.of("\0", "\uD800", "\uDC00", "😀", "猫", "a b", "café"));
        Random random = new Random(20261031149L);
        for (int trial = 0; trial < 120; trial++) {
            Object row = row(); Map<String, Long> expected = new HashMap<>();
            List<String> input = new ArrayList<>(vocabulary);
            if (trial % 3 == 0) Collections.sort(input);
            else if (trial % 3 == 1) input.sort(Collections.reverseOrder());
            else Collections.shuffle(input, random);
            for (String token : input) {
                check(!(boolean)HAS.invokeExact(row, token), "absent new key");
                ADD.invokeExact(row, token); expected.put(token, 1L);
            }
            for (int i = 0; i < 1200; i++) {
                String token = vocabulary.get(random.nextInt(vocabulary.size()));
                ADD.invokeExact(row, token); expected.merge(token, 1L, Long::sum);
            }
            Object frozen = (Object)FREEZE.invokeExact(row);
            long sum = 0;
            for (var e : expected.entrySet()) {
                check((boolean)HAS.invokeExact(row, e.getKey()), "present key");
                check((long)COUNT.invokeExact(frozen, e.getKey()) == e.getValue(), "independent frequency");
                sum += e.getValue();
            }
            check(field(FROZEN, "total").getLong(frozen) == sum, "row total");
            check((long)COUNT.invokeExact(frozen, "absent") == 0, "missing count");
            String first = vocabulary.get(0); long old = expected.get(first);
            ADD.invokeExact(row, first);
            check((long)COUNT.invokeExact(frozen, first) == old, "frozen independence");
        }
        // Preserve the first equal String object separately within each row.
        String first = new String("same"), equal = new String("same");
        var model = M3BackoffWordNgrams.train(List.of(List.of("left", first), List.of("left", equal)), 2);
        check(model.rank(List.of("left"), 1).get(0).token() == first, "first string reference preserved");
        check(model.exactCount(List.of("left"), equal) == 2, "equal distinct string objects");
        System.out.println("TRAINING_BEHAVIOR_PASS trials=120 checks=" + checks);
    }
    private static void layout() throws Throwable {
        for (Field f : ROW.getDeclaredFields()) if (!Modifier.isStatic(f.getModifiers()))
            check(f.getType().isPrimitive() || f.getType().isArray(), "ROW_OBJECT_CONTAINER: " + f.getName());
        for (String ordering : List.of("ascending", "descending", "shuffled")) {
            List<String> keys = new ArrayList<>(collisionKeys());
            for (int i = 0; i < 4096; i++) keys.add("word-" + i);
            Collections.sort(keys);
            if (ordering.equals("descending")) Collections.reverse(keys);
            if (ordering.equals("shuffled")) Collections.shuffle(keys, new Random(934L));
            Object row = row();
            for (int i = 0; i < keys.size(); i++) {
                ADD.invokeExact(row, keys.get(i));
                if (i < 256 || (i & 255) == 255) verifyTree(row);
            }
            verifyTree(row);
            check(field(ROW, "size").getInt(row) == keys.size(), "unique nodes");
        }
        System.out.println("TRAINING_LAYOUT_PASS orders=3 checks=" + checks);
    }
    private static void verifyTree(Object row) throws ReflectiveOperationException {
        String[] keys = (String[])field(ROW, "keys").get(row);
        long[] values = (long[])field(ROW, "values").get(row);
        int[] left = (int[])field(ROW, "left").get(row), right = (int[])field(ROW, "right").get(row);
        byte[] heights = (byte[])field(ROW, "heights").get(row);
        int size = field(ROW, "size").getInt(row), root = field(ROW, "root").getInt(row);
        check(keys.length == values.length && keys.length == left.length && keys.length == right.length
                && keys.length == heights.length, "parallel array lengths");
        boolean[] visited = new boolean[size + 1];
        visit(root, null, null, keys, values, left, right, heights, visited);
        for (int i = 1; i <= size; i++) check(visited[i], "reachable node " + i);
        check(!visited[0] && heights[0] == 0, "zero sentinel");
    }
    private static int visit(int node, String low, String high, String[] keys, long[] values,
            int[] left, int[] right, byte[] heights, boolean[] visited) {
        if (node == 0) return 0;
        check(node > 0 && node < visited.length && !visited[node], "acyclic indexed nodes");
        visited[node] = true;
        check(keys[node] != null && values[node] > 0, "populated positive node");
        check(low == null || low.compareTo(keys[node]) < 0, "left ordering");
        check(high == null || high.compareTo(keys[node]) > 0, "right ordering");
        int a = visit(left[node], low, keys[node], keys, values, left, right, heights, visited);
        int b = visit(right[node], keys[node], high, keys, values, left, right, heights, visited);
        check(Math.abs(a - b) <= 1, "AVL_BALANCE");
        check(heights[node] == 1 + Math.max(a, b), "stored height");
        return 1 + Math.max(a, b);
    }
    private static void overflow() throws Throwable {
        Object row = row(); ADD.invokeExact(row, "a");
        long[] values = (long[])field(ROW, "values").get(row);
        int root = field(ROW, "root").getInt(row);
        values[root] = Long.MAX_VALUE; field(ROW, "total").setLong(row, Long.MAX_VALUE);
        try { ADD.invokeExact(row, "a"); throw new AssertionError("missing overflow"); }
        catch (ArithmeticException expected) { checks++; }
        check(values[root] == Long.MAX_VALUE && field(ROW, "total").getLong(row) == Long.MAX_VALUE, "overflow leaves values");
        try { ADD.invokeExact(row, "b"); throw new AssertionError("missing total overflow"); }
        catch (ArithmeticException expected) { checks++; }
        check(!(boolean)HAS.invokeExact(row, "b") && field(ROW, "size").getInt(row) == 1, "no partial insertion");
        values[root] = (1L << 53) + 1; field(ROW, "total").setLong(row, values[root]);
        ADD.invokeExact(row, "a");
        Object frozen = (Object)FREEZE.invokeExact(row);
        check((long)COUNT.invokeExact(frozen, "a") == (1L << 53) + 2, "exact long update");
        System.out.println("TRAINING_OVERFLOW_PASS checks=" + checks);
    }
    private static com.sun.management.ThreadMXBean bean() {
        var bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation instrumentation unavailable");
        bean.setThreadAllocatedMemoryEnabled(true); return bean;
    }
    private static void allocation() throws Throwable {
        var bean = bean(); Object row = row(); String token = "repeated";
        for (int i = 0; i < 30000; i++) ADD.invokeExact(row, token);
        long id = Thread.currentThread().threadId(), start = bean.getThreadAllocatedBytes(id);
        for (int i = 0; i < 300000; i++) ADD.invokeExact(row, token);
        long bytes = bean.getThreadAllocatedBytes(id) - start;
        Object frozen = (Object)FREEZE.invokeExact(row);
        check((long)COUNT.invokeExact(frozen, token) == 330000, "allocation fixture count");
        System.out.println("TRAINING_UPDATE_ALLOCATION bytes=" + bytes + " updates=300000");
        check(bytes < 600000, "TRAINING_BOXING_ALLOCATION: " + bytes);
    }
    private static void publicAllocation() {
        var bean = bean(); List<String> doc = Collections.nCopies(100000, "repeated");
        List<List<String>> docs = List.of(doc);
        for (int i = 0; i < 10; i++) sink = M3BackoffWordNgrams.train(docs, 1);
        long id = Thread.currentThread().threadId(), start = bean.getThreadAllocatedBytes(id);
        for (int i = 0; i < 10; i++) sink = M3BackoffWordNgrams.train(docs, 1);
        long per = (bean.getThreadAllocatedBytes(id) - start) / 10;
        check(((M3BackoffWordNgrams.Model)sink).exactCount(List.of(), "repeated") == 100000, "public training count");
        System.out.println("PUBLIC_TRAINING_ALLOCATION bytesPerTraining=" + per + " tokens=100000 order=1");
    }
}
