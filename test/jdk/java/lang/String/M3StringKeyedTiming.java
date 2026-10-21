/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Matched-image diagnostic, not a pass/fail microbenchmark or a release qualification.
 * Run the identical class/parameters with the pinned baseline and candidate M3 images.
 * The optional m3.oracle.only property permits fixture checking on a stock VM and prints
 * referenceOnly=true; such a run is not M3 performance evidence.
 */
public final class M3StringKeyedTiming {
    private static final int COUNT = 64;
    private static volatile long sink;

    public static void main(String[] args) throws Exception {
        String label = args.length == 0 ? "unspecified" : args[0];
        int units = Integer.getInteger("m3.timing.units", 256);
        int piece = Integer.getInteger("m3.timing.piece", 8);
        int operations = Integer.getInteger("m3.timing.ops", 1024);
        int warmup = Integer.getInteger("m3.timing.warmup", 2);
        int samples = Integer.getInteger("m3.timing.samples", 5);
        boolean referenceOnly = Boolean.getBoolean("m3.oracle.only");
        if (units < 12 || piece < 1 || piece >= units || operations < 1
                || warmup < 0 || samples < 1) {
            throw new IllegalArgumentException("invalid timing dimensions");
        }
        System.out.println("# label=" + label + " java=" + System.getProperty("java.runtime.version")
                + " vm=" + System.getProperty("java.vm.name")
                + " referenceOnly=" + referenceOnly
                + " units=" + units + " piece=" + piece + " keys=" + COUNT
                + " operations=" + operations + " warmup=" + warmup + " samples=" + samples);
        System.out.println("label\tworkload\tsample\toperations\tnanoseconds\tchecksum");
        for (boolean late : new boolean[] {false, true}) {
            Fixture f = new Fixture(units, piece, late, referenceOnly);
            String position = late ? "late" : "early";
            measure(label, "hash-" + position + "-tuple", warmup, samples, operations,
                    () -> lookup(f.tupleHash, f.queries, operations));
            measure(label, "hash-" + position + "-scalar", warmup, samples, operations,
                    () -> lookup(f.scalarHash, f.queries, operations));
            measure(label, "tree-" + position + "-tuple", warmup, samples, operations,
                    () -> lookup(f.tupleTree, f.queries, operations));
            measure(label, "tree-" + position + "-scalar", warmup, samples, operations,
                    () -> lookup(f.scalarTree, f.queries, operations));
            measure(label, "compare-" + position + "-tuple", warmup, samples, operations,
                    () -> compare(f.tuples, f.queries, operations));
            measure(label, "compare-" + position + "-scalar", warmup, samples, operations,
                    () -> compare(f.scalars, f.queries, operations));
            measure(label, "equals-hit-" + position + "-tuple", warmup, samples, operations,
                    () -> equal(f.tuples, f.queries, operations, false));
            measure(label, "equals-miss-" + position + "-tuple", warmup, samples, operations,
                    () -> equal(f.tuples, f.queries, operations, true));
            Reference.reachabilityFence(f);
        }
        System.out.println("# sink=" + sink);
    }

    private static final class Fixture {
        final String[] tuples = new String[COUNT];
        final String[] scalars = new String[COUNT];
        final String[] queries = new String[COUNT];
        final Map<String, Integer> tupleHash = new HashMap<>(128);
        final Map<String, Integer> scalarHash = new HashMap<>(128);
        final Map<String, Integer> tupleTree = new TreeMap<>();
        final Map<String, Integer> scalarTree = new TreeMap<>();

        Fixture(int length, int piece, boolean late, boolean referenceOnly) throws Exception {
            for (int key = 0; key < COUNT; key++) {
                char[] units = new char[length];
                Arrays.fill(units, 'x');
                int offset = late ? length - 12 : 0;
                for (int bit = 0; bit < 6; bit++) {
                    units[offset + bit * 2] = (key & (1 << bit)) == 0 ? 'A' : 'B';
                    units[offset + bit * 2 + 1] = (key & (1 << bit)) == 0 ? 'a' : 'B';
                }
                String[] pieces = new String[(length + piece - 1) / piece];
                for (int start = 0; start < length; start += piece) {
                    int count = Math.min(piece, length - start);
                    char[] padded = new char[count + 2];
                    padded[0] = '!';
                    padded[count + 1] = '?';
                    System.arraycopy(units, start, padded, 1, count);
                    pieces[start / piece] = new String(padded).substring(1, count + 1);
                }
                tuples[key] = String.join("", pieces);
                scalars[key] = new String(units.clone());
                queries[key] = new String(units.clone());
                int expectedHash = 0;
                for (char unit : units) expectedHash = 31 * expectedHash + unit;
                if (!tuples[key].equals(queries[key]) || tuples[key].hashCode() != expectedHash
                        || expectedHash != (key == 0 ? expectedHash : tuples[0].hashCode())) {
                    throw new AssertionError("fixture must have exact equal keys and 64 hash collisions");
                }
                tupleHash.put(tuples[key], key);
                scalarHash.put(scalars[key], key);
                tupleTree.put(tuples[key], key);
                scalarTree.put(scalars[key], key);
            }
            if (!referenceOnly) requireTupleAndScalar(tuples[0], queries[0]);
            if (tupleHash.size() != COUNT || tupleTree.size() != COUNT
                    || scalarHash.size() != COUNT || scalarTree.size() != COUNT) {
                throw new AssertionError("fixture lost distinct collision keys");
            }
        }
    }

    private static void requireTupleAndScalar(String tuple, String scalar) throws Exception {
        Field storageField = String.class.getDeclaredField("m3");
        storageField.setAccessible(true);
        Object left = storageField.get(tuple);
        Object right = storageField.get(scalar);
        if (left == null || right == null) throw new AssertionError("M3 image/admission required");
        Field ownerField = Class.forName("java.lang.M3String").getDeclaredField("owner");
        ownerField.setAccessible(true);
        Class<?> tupleClass = Class.forName("java.lang.M3StringTuple");
        Object leftOwner = ownerField.get(left);
        Object rightOwner = ownerField.get(right);
        if (!tupleClass.isInstance(leftOwner) || tupleClass.isInstance(rightOwner)
                || leftOwner == rightOwner) {
            throw new AssertionError("fixture requires distinct tuple and scalar canonical owners");
        }
    }

    private static long lookup(Map<String, Integer> map, String[] queries, int operations) {
        long result = 0;
        for (int i = 0; i < operations; i++) result += map.get(queries[(i * 17) & (COUNT - 1)]);
        return result;
    }

    private static long compare(String[] keys, String[] queries, int operations) {
        long result = 0;
        for (int i = 0; i < operations; i++) {
            int at = (i * 17) & (COUNT - 1);
            result += keys[at].compareTo(queries[(at + 1) & (COUNT - 1)]);
        }
        return result;
    }

    private static long equal(String[] keys, String[] queries, int operations, boolean miss) {
        long result = 0;
        for (int i = 0; i < operations; i++) {
            int at = (i * 17) & (COUNT - 1);
            result += keys[at].equals(queries[(at + (miss ? 1 : 0)) & (COUNT - 1)]) ? 1 : 0;
        }
        return result;
    }

    @FunctionalInterface
    private interface Work { long run(); }

    private static void measure(String label, String name, int warmup, int samples,
            int operations, Work action) {
        for (int i = 0; i < warmup; i++) sink = action.run();
        for (int sample = 0; sample < samples; sample++) {
            long start = System.nanoTime();
            long result = action.run();
            long elapsed = System.nanoTime() - start;
            sink = result;
            System.out.println(label + "\t" + name + "\t" + sample + "\t" + operations
                    + "\t" + elapsed + "\t" + result);
        }
    }
}
