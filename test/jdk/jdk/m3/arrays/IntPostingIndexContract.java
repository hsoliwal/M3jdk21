// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/** Independent, deterministic oracles against the actual product class and JNI entry points. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-mat-collections/src/test/java/com/synexia/matcollections/MatPostingGallopingContract.java blob 395bb0dd6450; recipe m3/arrays/recipe/posting-index-20261106/relocate.py v2
public final class IntPostingIndexContract {
    private long checks;
    private long lowerBoundChecks;
    private long scenarios;
    private final MessageDigest digest;
    private final Method javaIntersection;
    private final Method nativeIntersection;
    private final Method nativeUnion;

    private IntPostingIndexContract() throws Exception {
        digest = MessageDigest.getInstance("SHA-256");
        javaIntersection = method("intersectByLowerBound", int[].class, int.class, int.class, int.class);
        nativeIntersection = method("intersectionNative", int[].class, int[].class, int[].class, int.class);
        nativeUnion = method("unionNative", int[].class, int[].class, int[].class, int.class);
    }

    private static Method method(String name, Class<?>... arguments) throws NoSuchMethodException {
        Method method = IntPostingIndex.class.getDeclaredMethod(name, arguments);
        method.setAccessible(true);
        return method;
    }

    private static Object invoke(Method method, Object owner, Object... arguments) throws Exception {
        try {
            return method.invoke(owner, arguments);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Error error) throw error;
            if (cause instanceof Exception exception) throw exception;
            throw new AssertionError(cause);
        }
    }

    private void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private void same(int[] expected, int[] actual, String message) {
        check(Arrays.equals(expected, actual), message + ": " + Arrays.toString(actual));
    }

    private void record(int[] values) {
        ByteBuffer bytes = ByteBuffer.allocate((values.length + 1) * Integer.BYTES);
        bytes.putInt(values.length);
        for (int value : values) bytes.putInt(value);
        digest.update(bytes.array());
    }

    private static int[] intersection(int[] left, int[] right) {
        // Deliberately different from binary/galloping search: a TreeSet oracle.
        TreeSet<Integer> values = new TreeSet<>();
        for (int value : left) values.add(value);
        TreeSet<Integer> peer = new TreeSet<>();
        for (int value : right) peer.add(value);
        values.retainAll(peer);
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    private void windows() throws Exception {
        Random random = new Random(0x4d3347414c4c4f50L);
        Method bound = null;
        try { bound = method("lowerBoundFrom", int.class, int.class, int.class); }
        catch (NoSuchMethodException baseline) {
            check(!Boolean.getBoolean("matposting.expectGalloping"), "candidate helper missing");
        }
        int[] lengths = {0, 1, 2, 3, 4, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 127, 128, 129};
        for (int length : lengths) {
            int[] rows = new int[length];
            for (int i = 0; i < length; i++) rows[i] = i * 3 + 2;
            IntPostingIndex index = IntPostingIndex.ofPairs(new int[length], rows, 1024);
            int repetitions = length <= 17 ? (length + 1) * (length + 1) : 256;
            for (int trial = 0; trial < repetitions; trial++) {
                int start = length <= 17 ? trial / (length + 1) : random.nextInt(length + 1);
                int end = length <= 17 ? trial % (length + 1) : start + random.nextInt(length - start + 1);
                if (end < start) continue;
                int[] targets = {-1, 0, 1, 2, 3, 4, start * 3 + 1, start * 3 + 2,
                        end * 3 - 2, end * 3 - 1, end * 3, Integer.MAX_VALUE};
                for (int target : targets) {
                    int expectedBound = start;
                    while (expectedBound < end && rows[expectedBound] < target) expectedBound++;
                    int expectedSize = expectedBound < end && rows[expectedBound] == target ? 1 : 0;
                    int[] lane = {target};
                    int actualSize = (Integer) invoke(javaIntersection, index, lane, 1, start, end);
                    check(expectedSize == actualSize, "window intersection size");
                    int[] actual = Arrays.copyOf(lane, actualSize);
                    same(expectedSize == 1 ? new int[] {target} : new int[0], actual, "window intersection");
                    record(actual);
                    if (bound != null) {
                        int actualBound = (Integer) invoke(bound, index, start, end, target);
                        if (actualBound != expectedBound) throw new AssertionError("lower-bound insertion point");
                        lowerBoundChecks++;
                    }
                    scenarios++;
                }
            }
        }
    }

    private void raw(int[] postings, int[] offsets, int[] positions, int universe) throws Exception {
        int[] before = postings.clone(), oldOffsets = offsets.clone(), oldPositions = positions.clone();
        TreeSet<Integer> union = new TreeSet<>();
        int[] expected = null;
        for (int position : positions) {
            int[] list = Arrays.copyOfRange(postings, offsets[position], offsets[position + 1]);
            for (int row : list) union.add(row);
            expected = expected == null ? list : intersection(expected, list);
        }
        if (expected == null) expected = java.util.stream.IntStream.range(0, universe).toArray();
        int[] actual = (int[]) invoke(nativeIntersection, null, postings, offsets, positions, universe);
        same(expected, actual, "direct native intersection");
        record(actual);
        int[] all = (int[]) invoke(nativeUnion, null, postings, offsets, positions, universe);
        same(union.stream().mapToInt(Integer::intValue).toArray(), all, "direct native union");
        record(all);
        same(before, postings, "postings not mutated");
        same(oldOffsets, offsets, "offsets not mutated");
        same(oldPositions, positions, "positions not mutated");
        scenarios++;
    }

    private void nativeCases() throws Exception {
        raw(new int[0], new int[] {0}, new int[0], 17);
        raw(new int[0], new int[] {0, 0, 0}, new int[] {0, 1}, 0);
        raw(new int[] {0, 1, 3, 1, 3}, new int[] {0, 0, 3, 5}, new int[] {0, 1, 2}, 4);
        raw(new int[] {Integer.MAX_VALUE - 4, Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1},
                new int[] {0, 2, 3}, new int[] {0, 1}, Integer.MAX_VALUE);
        Random random = new Random(0x4d334e4154495645L);
        for (int round = 0; round < 2048; round++) {
            int universe = 1 + random.nextInt(2048);
            int count = 1 + random.nextInt(9);
            List<int[]> lists = new ArrayList<>();
            int[] offsets = new int[count + 1];
            for (int k = 0; k < count; k++) {
                int[] list = random.ints(random.nextInt(100), 0, universe).distinct().sorted().toArray();
                lists.add(list);
                offsets[k + 1] = offsets[k] + list.length;
            }
            int[] postings = new int[offsets[count]];
            for (int k = 0; k < count; k++) System.arraycopy(lists.get(k), 0, postings, offsets[k], lists.get(k).length);
            int[] positions = java.util.stream.IntStream.range(0, count).filter(k -> random.nextBoolean()).toArray();
            raw(postings, offsets, positions, universe);
        }
        // Threshold seams, clustered hits, misses, nonzero list offsets and a long gap.
        for (int size : new int[] {1, 2, 7, 16, 63}) {
            for (int delta : new int[] {-1, 0, 1, 257}) {
                int length = size * 16 + delta;
                int[] postings = new int[size + length];
                for (int i = 0; i < size; i++) postings[i] = i == size - 1 ? length * 2 - 1 : i * 2;
                for (int i = 0; i < length; i++) postings[size + i] = i * 2;
                raw(postings, new int[] {0, size, size + length}, new int[] {0, 1}, length * 2 + 1);
            }
        }
        for (Method kernel : new Method[] {nativeIntersection, nativeUnion}) {
            invalid(kernel, null, new int[] {0}, new int[0], 1);
            invalid(kernel, new int[0], null, new int[0], 1);
            invalid(kernel, new int[0], new int[] {0}, null, 1);
            invalid(kernel, new int[0], new int[0], new int[0], 1);
            invalid(kernel, new int[0], new int[] {0}, new int[0], -1);
            invalid(kernel, new int[] {0}, new int[] {1, 1}, new int[] {0}, 1);
            invalid(kernel, new int[] {0}, new int[] {0, 2}, new int[] {0}, 1);
            invalid(kernel, new int[] {0}, new int[] {0, 1}, new int[] {-1}, 1);
            invalid(kernel, new int[] {0}, new int[] {0, 1}, new int[] {1}, 1);
            invalid(kernel, new int[] {0}, new int[] {0, 1}, new int[] {0, 0}, 1);
            invalid(kernel, new int[] {1, 0}, new int[] {0, 2}, new int[] {0}, 2);
            invalid(kernel, new int[] {0, 0}, new int[] {0, 2}, new int[] {0}, 1);
            invalid(kernel, new int[] {-1}, new int[] {0, 1}, new int[] {0}, 1);
            invalid(kernel, new int[] {1}, new int[] {0, 1}, new int[] {0}, 1);
        }
    }

    private void invalid(Method kernel, int[] postings, int[] offsets, int[] positions, int universe) throws Exception {
        try {
            invoke(kernel, null, postings, offsets, positions, universe);
            throw new AssertionError("invalid JNI input was accepted");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().startsWith("invalid posting")
                    || expected.getMessage().equals("empty posting offsets"), "exception contract");
        }
        scenarios++;
    }

    private void publicCases() throws Exception {
        Random random = new Random(0x4d335055424c4943L);
        int[] pool = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};
        for (int round = 0; round < 1024; round++) {
            int universe = 1 + random.nextInt(513);
            int[] keys = new int[random.nextInt(600)], rows = new int[keys.length];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = pool[random.nextInt(pool.length)]; rows[i] = random.nextInt(universe);
            }
            int[] query = new int[random.nextInt(8)];
            for (int i = 0; i < query.length; i++) query[i] = random.nextInt(8) == 0 ? 17 : pool[random.nextInt(pool.length)];
            int[] oldQuery = query.clone();
            IntPostingIndex index = IntPostingIndex.ofPairs(keys, rows, universe);
            BitSet and = new BitSet(), or = new BitSet();
            and.set(0, universe);
            for (int key : query) {
                BitSet list = new BitSet();
                for (int i = 0; i < keys.length; i++) if (keys[i] == key) list.set(rows[i]);
                and.and(list); or.or(list);
            }
            // Verify ownership after construction, not only query nonmutation.
            Arrays.fill(keys, 99); Arrays.fill(rows, 0);
            same(and.stream().toArray(), index.all(query).rowsSelected().toArray(), "public AND");
            same(or.stream().toArray(), index.any(query).rowsSelected().toArray(), "public OR");
            if (IntPostingIndex.nativeAvailable()) {
                same(and.stream().toArray(), index.all(IntPostingIndex.Backend.NATIVE_REQUIRED, query).rowsSelected().toArray(), "public JNI AND");
                same(or.stream().toArray(), index.any(IntPostingIndex.Backend.NATIVE_REQUIRED, query).rowsSelected().toArray(), "public JNI OR");
            }
            same(oldQuery, query, "query nonmutation");
            check(index.primitivePayloadBytes() == 4L * (2L * index.keyCount() + 1L + index.postingCount()), "payload accounting");
            record(and.stream().toArray()); record(or.stream().toArray()); scenarios++;
        }
        int[] rows = java.util.stream.IntStream.range(0, 4096).toArray();
        IntPostingIndex shared = IntPostingIndex.ofPairs(new int[rows.length], rows, rows.length);
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Callable<int[]>> work = new ArrayList<>();
            for (int task = 0; task < 16; task++) work.add(() -> {
                int[] result = null;
                for (int trial = 0; trial < 32; trial++) {
                    result = shared.all(IntPostingIndex.nativeAvailable()
                            ? IntPostingIndex.Backend.NATIVE_REQUIRED : IntPostingIndex.Backend.JAVA, 0, 0).rowsSelected().toArray();
                    if (!Arrays.equals(rows, result)) throw new AssertionError("concurrent immutable query");
                }
                return result;
            });
            for (var result : executor.invokeAll(work)) { same(rows, result.get(), "concurrent result"); record(result.get()); scenarios++; }
        }
    }

    public static void runAll() throws Exception {
        boolean required = Boolean.getBoolean("matposting.requireNative");
        String library = System.getProperty("matposting.native.path", "");
        if (required && library.isBlank()) throw new IllegalStateException("MAT_POSTING_REQUIRED_JNI_LIBRARY_MISSING");
        if (!library.isBlank()) IntPostingIndex.loadNative(Path.of(library));
        if (required && !IntPostingIndex.nativeAvailable()) throw new AssertionError("required JNI not initialized");
        IntPostingIndexContract oracle = new IntPostingIndexContract();
        oracle.windows();
        oracle.publicCases();
        if (IntPostingIndex.nativeAvailable()) oracle.nativeCases();
        System.out.println("MAT_GALLOP_SCENARIOS=" + oracle.scenarios);
        System.out.println("MAT_GALLOP_CHECKS=" + oracle.checks);
        System.out.println("MAT_GALLOP_LOWER_BOUND_CHECKS=" + oracle.lowerBoundChecks);
        System.out.println("MAT_GALLOP_DIGEST=" + HexFormat.of().formatHex(oracle.digest.digest()));
        System.out.println("MAT_GALLOP_BACKEND=" + (IntPostingIndex.nativeAvailable() ? "JAVA_AND_JNI" : "JAVA_ONLY"));
        System.out.println("MAT_GALLOP_RESULT=PASS");
    }

    public static void main(String[] args) throws Exception { runAll(); }
}
