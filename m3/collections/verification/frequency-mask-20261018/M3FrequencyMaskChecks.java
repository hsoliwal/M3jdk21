/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import com.synexia.primitives.IntIntHashMap;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Random;

/** Frozen, dependency-free differential checks; run unchanged on baseline and candidate. */
public final class M3FrequencyMaskChecks {
    private static final MessageDigest TAPE = digest();
    private static long checks;
    private M3FrequencyMaskChecks() { }

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "java" : args[0];
        mapCases();
        M3IndexMaskCompactor.Backend backend = switch (mode) {
            case "java", "unavailable" -> M3IndexMaskCompactor.Backend.JAVA;
            case "native" -> M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
            default -> throw new IllegalArgumentException("java|native|unavailable");
        };
        lonelyCases(backend);
        windowCases(backend);
        if (mode.equals("native")) nativeRefusals();
        if (mode.equals("unavailable")) {
            require(!M3IndexMaskCompactor.nativeAvailable(), "unexpected native library");
            expect(UnsatisfiedLinkError.class, () -> M3IndexMaskCompactor.selectWindow(
                    new int[0], 0, new byte[0], 0, 0, 0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
        System.out.println("PASS checks=" + checks + " tape=" + HexFormat.of().formatHex(TAPE.digest()));
    }

    private static void mapCases() throws Exception {
        Random random = new Random(0x1362150L);
        for (int round = 0; round < 12; round++) {
            M3IntIntHashMap map = new M3IntIntHashMap(round % 3);
            IntIntHashMap donor = new IntIntHashMap(round % 3, round % 2 == 0 ? 0.65f : 0.3f);
            Map<Integer, Integer> oracle = new HashMap<>();
            for (int step = 0; step < 4000; step++) {
                int key = switch (step % 23) {
                    case 0 -> 0;
                    case 1 -> Integer.MIN_VALUE;
                    case 2 -> Integer.MAX_VALUE;
                    default -> random.nextInt(513) - 256;
                };
                int value = step % 7 == 0 ? 0 : random.nextInt();
                switch (random.nextInt(8)) {
                    case 0, 1, 2, 3 -> {
                        int expected = oracle.getOrDefault(key, 0) + value;
                        require(map.addTo(key, value) == expected, "target addTo");
                        require(donor.addTo(key, value) == expected, "donor addTo");
                        oracle.put(key, expected);
                        tape(expected);
                    }
                    case 4 -> {
                        boolean present = oracle.containsKey(key);
                        int old = oracle.getOrDefault(key, 0);
                        require(map.put(key, value) == !present, "target put");
                        require(donor.put(key, value) == old, "donor put");
                        oracle.put(key, value);
                    }
                    case 5 -> {
                        boolean present = oracle.containsKey(key);
                        int old = oracle.getOrDefault(key, 0);
                        require(map.remove(key) == present, "target remove");
                        require(donor.remove(key) == old, "donor remove");
                        oracle.remove(key);
                    }
                    case 6 -> {
                        require(map.containsKey(key) == oracle.containsKey(key), "target contains");
                        require(donor.containsKey(key) == oracle.containsKey(key), "donor contains");
                    }
                    default -> {
                        if (step % 53 == 0) { map.clear(); donor.clear(); oracle.clear(); }
                    }
                }
                require(map.size() == oracle.size() && donor.size() == oracle.size(), "size");
                require(map.getOrDefault(key, 42) == oracle.getOrDefault(key, 42), "target get");
                require(donor.getOrDefault(key, 42) == oracle.getOrDefault(key, 42), "donor get");
                if (step % 29 == 0) {
                    Map<Integer, Integer> targetContents = new HashMap<>();
                    map.forEach(targetContents::put);
                    Map<Integer, Integer> donorContents = new HashMap<>();
                    donor.forEach(donorContents::put);
                    require(targetContents.equals(oracle), "target contents");
                    require(donorContents.equals(oracle), "donor contents");
                    stateTape(map); stateTape(donor);
                }
            }
        }
        for (int key : new int[]{0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            M3IntIntHashMap map = new M3IntIntHashMap();
            require(map.addTo(key, 0) == 0 && map.containsKey(key), "zero delta inserts");
            map.put(key, Integer.MAX_VALUE);
            M3IntIterator it = map.keyIterator();
            require(map.addTo(key, 1) == Integer.MIN_VALUE, "overflow wraps");
            require(it.hasNext() && it.nextInt() == key && !it.hasNext(), "overwrite iterator");
            M3IntIterator invalidated = map.keyIterator();
            map.addTo(key ^ 0x4a4a4a4a, 0);
            expect(ConcurrentModificationException.class, invalidated::hasNext);
        }
        // Force wraparound collision runs, delete interior keys, then count again.
        M3IntIntHashMap map = new M3IntIntHashMap(30);
        Map<Integer, Integer> oracle = new HashMap<>();
        int found = 0;
        for (int key = 1; found < 25; key++) {
            if ((mix(key) & (map.capacity() - 1)) != map.capacity() - 1) continue;
            found++; map.addTo(key, 1); oracle.put(key, 1);
        }
        int[] keys = map.keysToArray();
        for (int i = 1; i < keys.length; i += 2) { map.remove(keys[i]); oracle.remove(keys[i]); }
        for (int key : keys) {
            int expected = oracle.getOrDefault(key, 0) - 2;
            require(map.addTo(key, -2) == expected, "collision backshift addTo");
            oracle.put(key, expected);
        }
        stateTape(map);
    }

    private static void lonelyCases(M3IndexMaskCompactor.Backend backend) {
        // Never treat XOR parity as general singleton validation.
        require(xor(new int[]{1, 1, 1, 2, 2}) == 1, "odd is not singleton");
        require(xor(new int[]{1, 2, 3}) == 0, "xor can be absent from input");
        Random random = new Random(2150);
        for (int test = 0; test < 1800; test++) {
            int from = random.nextInt(9), n = random.nextInt(90);
            int[] input = new int[from + n + 5];
            Arrays.fill(input, 123456789);
            for (int i = from; i < from + n; i++) {
                input[i] = switch (random.nextInt(30)) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MIN_VALUE + 1;
                    case 2 -> Integer.MAX_VALUE;
                    case 3 -> Integer.MAX_VALUE - 1;
                    default -> random.nextInt(81) - 40;
                };
            }
            int[] saved = input.clone();
            M3IntIntHashMap counts = new M3IntIntHashMap();
            for (int i = from; i < from + n; i++) counts.addTo(input[i], 1);
            byte[] flags = new byte[n];
            for (int i = 0; i < n; i++) {
                int value = input[from + i];
                if (counts.getOrDefault(value, 0) == 1) flags[i] |= 1;
                if ((value == Integer.MIN_VALUE || !counts.containsKey(value - 1))
                        && (value == Integer.MAX_VALUE || !counts.containsKey(value + 1))) flags[i] |= 2;
            }
            int[] actual = M3IndexMaskCompactor.selectWindow(input, from, flags, 0, n, 3, backend);
            int[] expected = new int[n]; int write = 0;
            for (int i = from; i < from + n; i++) {
                int count = 0; boolean neighbor = false;
                for (int j = from; j < from + n; j++) {
                    if (input[j] == input[i]) count++;
                    long difference = (long) input[j] - input[i];
                    if (difference == -1 || difference == 1) neighbor = true;
                }
                if (count == 1 && !neighbor) expected[write++] = input[i];
            }
            require(Arrays.equals(actual, Arrays.copyOf(expected, write)), "2150 frequency+mask");
            require(Arrays.equals(input, saved), "input isolation");
            for (int value : actual) tape(value);
        }
        Random pairRandom = new Random(136);
        for (int t = 0; t < 1000; t++) {
            int unique = pairRandom.nextInt();
            int[] values = new int[1 + 2 * pairRandom.nextInt(100)]; values[0] = unique;
            for (int i = 1; i < values.length; i += 2) {
                int value = unique + i; values[i] = value; values[i + 1] = value;
            }
            for (int i = values.length - 1; i > 0; i--) {
                int j = pairRandom.nextInt(i + 1), value = values[i]; values[i] = values[j]; values[j] = value;
            }
            require(xor(values) == unique, "136/HackerRank pairs");
        }
    }

    private static void windowCases(M3IndexMaskCompactor.Backend backend) {
        Random r = new Random(0x256L);
        int[] sizes = {0, 1, 2, 31, 32, 63, 64, 127, 128, 255, 256, 257, 511, 512, 513, 1025};
        for (int n : sizes) for (int mask = 0; mask < 256; mask++) window(r, n, mask, backend);
        for (int i = 0; i < 2000; i++) window(r, r.nextInt(1600), r.nextInt(256), backend);
        for (int mask : new int[]{-1, 256, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            expect(IllegalArgumentException.class, () -> M3IndexMaskCompactor.selectWindow(
                    new int[1], 0, new byte[1], 0, 1, mask, backend));
        }
        for (int[] invalid : new int[][]{{-1,0,0},{0,-1,0},{0,0,-1},{2,0,0},{0,2,0},
                {0,0,2},{Integer.MAX_VALUE,0,1},{0,Integer.MAX_VALUE,1},{1,0,Integer.MAX_VALUE}}) {
            expect(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectWindow(
                    new int[1], invalid[0], new byte[1], invalid[1], invalid[2], 0, backend));
        }
        expect(NullPointerException.class, () -> M3IndexMaskCompactor.selectWindow(null, 0, new byte[0], 0, 0, 0, backend));
        expect(NullPointerException.class, () -> M3IndexMaskCompactor.selectWindow(new int[0], 0, null, 0, 0, 0, backend));
    }

    private static void window(Random r, int n, int mask, M3IndexMaskCompactor.Backend backend) {
        int ro = r.nextInt(17), so = r.nextInt(17);
        int[] rows = new int[ro + n + 3]; long[] longs = new long[rows.length];
        byte[] flags = new byte[so + n + 4]; r.nextBytes(flags);
        for (int i = 0; i < rows.length; i++) { rows[i] = r.nextInt(); longs[i] = r.nextLong(); }
        int[] saved = rows.clone(); long[] savedLongs = longs.clone(); byte[] savedFlags = flags.clone();
        int[] expected = new int[n]; long[] expectedLong = new long[n]; int k = 0;
        for (int i = 0; i < n; i++) if (((flags[so + i] & 255) & mask) == mask) {
            expected[k] = rows[ro + i]; expectedLong[k++] = longs[ro + i];
        }
        int[] a = M3IndexMaskCompactor.selectWindow(rows, ro, flags, so, n, mask, backend);
        long[] b = M3IndexMaskCompactor.selectLongWindow(longs, ro, flags, so, n, mask, backend);
        require(Arrays.equals(a, Arrays.copyOf(expected, k)), "int window");
        require(Arrays.equals(b, Arrays.copyOf(expectedLong, k)), "long window");
        require(a != rows && b != longs, "detached output");
        require(Arrays.equals(rows, saved) && Arrays.equals(longs, savedLongs)
                && Arrays.equals(flags, savedFlags), "inputs unchanged");
        for (int x : a) tape(x);
        for (long x : b) { tape((int)(x >>> 32)); tape((int)x); }
    }

    private static void nativeRefusals() throws Exception {
        require(M3IndexMaskCompactor.nativeAvailable(), "native required");
        for (String name : new String[]{"selectWindowNative", "selectLongWindowNative"}) {
            boolean longs = name.contains("Long");
            Method method = M3IndexMaskCompactor.class.getDeclaredMethod(name,
                    longs ? long[].class : int[].class, int.class, byte[].class,
                    int.class, int.class, int.class);
            method.setAccessible(true);
            Object rows = longs ? new long[1] : new int[1];
            Object[][] cases = {{null,0,new byte[1],0,1,0},{rows,0,null,0,1,0},
                    {rows,-1,new byte[1],0,0,0},{rows,0,new byte[1],-1,0,0},
                    {rows,0,new byte[1],0,-1,0},{rows,0,new byte[1],0,1,-1},
                    {rows,0,new byte[1],0,1,256},{rows,Integer.MAX_VALUE,new byte[1],0,1,0},
                    {rows,1,new byte[1],0,Integer.MAX_VALUE,0}};
            for (Object[] args : cases) {
                try { method.invoke(null, args); }
                catch (InvocationTargetException e) {
                    require(e.getCause() instanceof IllegalArgumentException, "direct native refusal");
                    continue;
                }
                throw new AssertionError("native accepted invalid input");
            }
        }
    }

    private static void stateTape(Object map) throws Exception {
        for (String name : new String[]{"keys", "values"}) {
            var field = map.getClass().getDeclaredField(name); field.setAccessible(true);
            for (int value : (int[]) field.get(map)) tape(value);
        }
        for (String name : new String[]{"mask", "threshold", "size", "zeroValue"}) {
            var field = map.getClass().getDeclaredField(name); field.setAccessible(true); tape(field.getInt(map));
        }
        var zero = map.getClass().getDeclaredField("hasZero"); zero.setAccessible(true); tape(zero.getBoolean(map) ? 1 : 0);
        if (map instanceof M3IntIntHashMap) {
            var mod = map.getClass().getDeclaredField("modCount"); mod.setAccessible(true); tape(mod.getInt(map));
        }
    }
    private static int mix(int x) { x ^= x >>> 16; x *= 0x7feb352d; x ^= x >>> 15; x *= 0x846ca68b; return x ^ (x >>> 16); }
    private static int xor(int[] values) { int x = 0; for (int value : values) x ^= value; return x; }
    private static void tape(int value) { for (int shift = 24; shift >= 0; shift -= 8) TAPE.update((byte)(value >>> shift)); }
    private static MessageDigest digest() { try { return MessageDigest.getInstance("SHA-256"); } catch (Exception e) { throw new ExceptionInInitializerError(e); } }
    private static void require(boolean test, String message) { checks++; if (!test) throw new AssertionError(message); }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); } catch (Throwable failure) { if (type.isInstance(failure)) return; throw new AssertionError(failure); }
        throw new AssertionError("expected " + type.getName());
    }
}
