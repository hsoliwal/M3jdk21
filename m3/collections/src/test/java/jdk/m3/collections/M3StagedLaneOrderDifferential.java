/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Original-owner/candidate observations in one JVM. Shared identity keys and values make
 * identity-map encounter order comparable without assuming stable identity hashes across VMs.
 * No recipe or compiler-engine dependency: existing isolated mastery loaders can call compare.
 */
public final class M3StagedLaneOrderDifferential {
    private static final Class<?>[] NONE = {};
    private static final Class<?>[] INT = {int.class};
    private static final Class<?>[] LONG = {long.class};
    private static final Class<?>[] II = {int.class, int.class};
    private static final Class<?>[] IL = {int.class, long.class};
    private static final Class<?>[] LL = {long.class, long.class};
    private static final Class<?>[] OO = {Object.class, Object.class};

    private M3StagedLaneOrderDifferential() { }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2) throw new IllegalArgumentException("expected original-classes candidate-classes");
        try (URLClassLoader original = isolated(arguments[0]); URLClassLoader candidate = isolated(arguments[1])) {
            compare(original, candidate);
        }
    }

    private static URLClassLoader isolated(String directory) throws Exception {
        return new URLClassLoader(new URL[] {Path.of(directory).toUri().toURL()}, ClassLoader.getPlatformClassLoader());
    }

    public static void compare(ClassLoader original, ClassLoader candidate) throws Exception {
        if (original == candidate) throw new IllegalArgumentException("original and candidate need distinct loaders");
        for (String owner : new String[] {"M3IntIntHashMap", "M3IntLongHashMap", "M3ObjectIdentityMap",
                "M3LongLongHashMap", "M3LongLongTreeMap", "M3LongPatriciaTrie", "M3LongTreeSet"}) {
            Pair pair = new Pair(owner, original, candidate);
            boolean identity = owner.equals("M3ObjectIdentityMap");
            boolean intInt = owner.equals("M3IntIntHashMap");
            boolean intLong = owner.equals("M3IntLongHashMap");
            boolean ints = intInt || intLong;
            boolean trie = owner.equals("M3LongPatriciaTrie");
            boolean pages = owner.equals("M3LongTreeSet");
            boolean sorted = owner.equals("M3LongLongTreeMap");
            Object[] identityKeys = new Object[513], identityValues = new Object[31];
            for (int i = 1; i < identityKeys.length; i++) identityKeys[i] = new String("equal-key-" + (i % 11));
            for (int i = 1; i < identityValues.length; i++) identityValues[i] = new String("equal-value-" + (i % 7));
            Random random = new Random(0x51a1_0b5e_2026L);
            for (int step = 0; step < 5000; step++) {
                pair.step = step;
                int ordinal = step < 513 ? step : random.nextInt(1025) - 512;
                long key = switch (step % 97) {
                    case 0 -> 0L;
                    case 1 -> Long.MIN_VALUE;
                    case 2 -> Long.MAX_VALUE;
                    case 3 -> -1L;
                    default -> ordinal;
                };
                long value = random.nextLong();
                int operation = step < 513 ? 0 : random.nextInt(5);
                if (identity) {
                    Object objectKey = identityKeys[Math.floorMod(ordinal, identityKeys.length)];
                    Object objectValue = identityValues[Math.floorMod(ordinal, identityValues.length)];
                    if (operation < 2) pair.call("put", OO, objectKey, objectValue);
                    else if (operation == 2) pair.call("remove", new Class<?>[] {Object.class}, objectKey);
                    else if (operation == 3) pair.call("putIfAbsent", OO, objectKey, objectValue);
                    else pair.call("getOrDefault", OO, objectKey, objectValue);
                } else if (intInt) {
                    if (operation < 2) pair.call("put", II, (int) key, (int) value);
                    else if (operation == 2) pair.call("remove", INT, (int) key);
                    else if (operation == 3) pair.call("addTo", II, (int) key, (int) value);
                    else pair.call("getOrDefault", II, (int) key, (int) value);
                } else if (intLong) {
                    if (operation < 2) pair.call("put", IL, (int) key, value);
                    else if (operation == 2) pair.call("remove", INT, (int) key);
                    else if (operation == 3) pair.call("addTo", IL, (int) key, value);
                    else pair.call("getOrDefault", IL, (int) key, value);
                } else if (pages) {
                    if (operation < 2) pair.call("add", LONG, key);
                    else if (operation == 2) pair.call("remove", LONG, key);
                    else pair.call("contains", LONG, key);
                } else {
                    if (operation < 2) pair.call(trie ? "putKey" : "put", LL, key, value);
                    else if (operation == 2) pair.call(trie ? "removeKey" : "remove", LONG, key);
                    else pair.call("getOrDefault", LL, key, value);
                }
                pair.call("size", NONE);
                pair.call("capacity", NONE);
                if ((step & 31) == 0) {
                    pair.call(pages ? "toArray" : "keysToArray", NONE);
                    if (sorted) pair.call("valuesToArray", NONE);
                    if (identity) pair.call("referenceSlots", NONE);
                    else pair.iterators(pages ? "iterator" : "keyIterator", ints ? "nextInt" : "nextLong");
                    if (pages) { pair.call("pageCount", NONE); pair.call("payloadBytes", NONE); }
                    if (trie) pair.call("prefixKeys", new Class<?>[] {long.class, int.class}, key, step % 65);
                }
                if ((pages || sorted) && step % 37 == 0) pair.call("trimToSize", NONE);
                if (sorted && step % 53 == 0) {
                    long start = ordinal;
                    pair.call("putAllSorted", new Class<?>[] {long[].class, long[].class},
                            new long[] {start, start, start + 1}, new long[] {value, ~value, -value});
                }
                if (step == 2047 || step == 4095) pair.call("clear", NONE);
            }
            pair.call(pages ? "toArray" : "keysToArray", NONE);
            System.out.println("PASS original/candidate order and capacity " + owner + " observations=" + pair.observations);
        }
    }

    private static final class Pair {
        private final String owner;
        private final Object original;
        private final Object candidate;
        private final Map<String, Method[]> methods = new HashMap<>();
        private int step;
        private long observations;

        private Pair(String owner, ClassLoader first, ClassLoader second) throws Exception {
            this.owner = owner;
            Class<?> a = Class.forName("jdk.m3.collections." + owner, true, first);
            Class<?> b = Class.forName("jdk.m3.collections." + owner, true, second);
            if (a == b) throw new AssertionError("owner resolved to same class; isolation missing: " + owner);
            original = a.getConstructor().newInstance();
            candidate = b.getConstructor().newInstance();
        }

        private Object call(String name, Class<?>[] parameters, Object... arguments) throws Exception {
            String signature = name + Arrays.toString(parameters);
            Method[] cached = methods.get(signature);
            if (cached == null) {
                cached = new Method[] {original.getClass().getMethod(name, parameters), candidate.getClass().getMethod(name, parameters)};
                methods.put(signature, cached);
            }
            Object first = invoke(cached[0], original, arguments);
            Object second = invoke(cached[1], candidate, arguments);
            same(first, second, cached[0].getReturnType().isPrimitive(), name);
            return second;
        }

        private void iterators(String factory, String next) throws Exception {
            Object first = original.getClass().getMethod(factory).invoke(original);
            Object second = candidate.getClass().getMethod(factory).invoke(candidate);
            Method aHas = first.getClass().getMethod("hasNext"), bHas = second.getClass().getMethod("hasNext");
            Method aNext = first.getClass().getMethod(next), bNext = second.getClass().getMethod(next);
            aHas.setAccessible(true); bHas.setAccessible(true); aNext.setAccessible(true); bNext.setAccessible(true);
            for (;;) {
                boolean a = (boolean) aHas.invoke(first), b = (boolean) bHas.invoke(second);
                same(a, b, true, "iterator.hasNext");
                if (!a) return;
                same(aNext.invoke(first), bNext.invoke(second), true, "iterator." + next);
            }
        }

        private void same(Object first, Object second, boolean primitive, String operation) {
            observations++;
            boolean equal;
            if (first instanceof int[] a && second instanceof int[] b) equal = Arrays.equals(a, b);
            else if (first instanceof long[] a && second instanceof long[] b) equal = Arrays.equals(a, b);
            else if (first instanceof Object[] a && second instanceof Object[] b) {
                equal = a.length == b.length;
                for (int i = 0; equal && i < a.length; i++) equal = a[i] == b[i];
            } else equal = primitive ? Objects.equals(first, second) : first == second;
            if (!equal) throw new AssertionError(owner + " step=" + step + " observation differs: " + operation);
        }

        private static Object invoke(Method method, Object receiver, Object... args) throws Exception {
            try { return method.invoke(receiver, args); }
            catch (InvocationTargetException wrapped) {
                Throwable cause = wrapped.getCause();
                if (cause instanceof Error error) throw error;
                if (cause instanceof Exception error) throw error;
                throw wrapped;
            }
        }
    }
}
