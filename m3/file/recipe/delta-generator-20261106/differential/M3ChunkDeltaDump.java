// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package delta.differential;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Differential dump driver of recipe delta-generator-20261106 (compiled by differential.sh only, never part of the
 * module). It drives a chunk-delta generator class through reflection, so the same driver runs against the donor
 * {@code DeltaGenerator} (hash type {@code SHA256}, built by {@code fromHex}) and the relocated
 * {@code jdk.m3.file.M3ChunkDeltaGenerator} (hash type {@code java.lang.String}), and prints every edit script,
 * distance, stats, apply() reconstruction and exception class over the same inputs: the proof's 16,129 exhaustive
 * binary pairs and 300 seeded pairs, 100,000 seeded file-row pairs (an old row of 0..48 chunk hashes from an alphabet
 * of 16 and a new row derived by 0..8 random edits, every tenth pair fully random), 1,000 equal-root pairs,
 * 1,000 generateWithSizes cases over a store that maps a hash to its chunk bytes, the proof's exception cases and the
 * 5000 x 5000 probe. Hashes print as their hex, so the two sides must be byte-identical.
 *
 * <p>usage: {@code java delta.differential.M3ChunkDeltaDump <generator class> <hash class | java.lang.String>}
 */
public final class M3ChunkDeltaDump {
    private static final int ALPHABET = 16;
    private static final long FILE_ROWS_SEED = 0x4d3346494c4552L;
    private static final long PROOF_SEED = 0x4d3344494646L;
    private static final int FILE_ROWS = 100_000;
    private static final int MAX_ROW = 48;
    private static final int MAX_EDITS = 8;
    private static final HexFormat HEX = HexFormat.of();

    private final Class<?> generatorClass;
    private final Object generator;
    private final Method generate;
    private final Method generateWithSizes;
    private final Method editDistance;
    private final Method areIdentical;
    private final Method fromHex;
    private final String[] hexes = new String[ALPHABET];
    private final byte[][] chunks = new byte[ALPHABET][];
    private final Object[] hashes = new Object[ALPHABET];
    private final Map<String, Integer> indexOf = new HashMap<>();
    private final StringBuilder out = new StringBuilder(1 << 20);
    private long pairs;
    private long operations;
    private boolean inputsIntact = true;

    private M3ChunkDeltaDump(String generatorName, String hashName) throws Exception {
        generatorClass = Class.forName(generatorName);
        generator = generatorClass.getConstructor().newInstance();
        Class<?> hashClass = Class.forName(hashName);
        fromHex = hashClass == String.class ? null : hashClass.getMethod("fromHex", String.class);
        generate = generatorClass.getMethod("generate", hashClass, List.class, hashClass, List.class);
        generateWithSizes = Arrays.stream(generatorClass.getMethods())
                .filter(method -> method.getName().equals("generateWithSizes")).findFirst().orElseThrow();
        editDistance = generatorClass.getMethod("editDistance", List.class, List.class);
        areIdentical = generatorClass.getMethod("areIdentical", List.class, List.class);
        Random random = new Random(0x4d3343484e4b53L);
        for (int i = 0; i < ALPHABET; i++) {
            chunks[i] = new byte[1 + random.nextInt(4096)];
            random.nextBytes(chunks[i]);
            hexes[i] = HEX.formatHex(sha256(chunks[i]));
            hashes[i] = hash(hexes[i]);
            indexOf.put(hexes[i], i);
        }
    }

    public static void main(String[] args) throws Exception {
        M3ChunkDeltaDump dump = new M3ChunkDeltaDump(args[0], args[1]);
        dump.binaryLane();
        dump.proofSequentialLane();
        dump.fileRowsLane();
        dump.equalRootsLane();
        dump.sizesLane();
        dump.exceptionsLane();
        dump.probeLane();
        dump.out.append("TOTAL pairs=").append(dump.pairs).append(" operations=").append(dump.operations)
                .append(" inputs_intact=").append(dump.inputsIntact).append('\n');
        System.out.print(dump.out);
        System.out.flush();
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object hash(String hex) throws Exception {
        return fromHex == null ? hex : fromHex.invoke(null, hex);
    }

    private List<Object> row(int... indexes) {
        List<Object> row = new ArrayList<>(indexes.length);
        for (int index : indexes) row.add(hashes[index]);
        return row;
    }

    /** One line per pair: distance, areIdentical, apply() reconstruction, stats and the compact script. */
    private void pair(String label, Object oldRoot, List<Object> old, Object newRoot, List<Object> next,
                      boolean verbose) throws Exception {
        List<Object> oldCopy = new ArrayList<>(old), nextCopy = new ArrayList<>(next);
        Object delta = generate.invoke(generator, oldRoot, old, newRoot, next);
        int distance = (Integer) editDistance.invoke(null, old, next);
        boolean identical = (Boolean) areIdentical.invoke(null, old, next);
        List<?> ops = (List<?>) delta.getClass().getMethod("operations").invoke(delta);
        List<?> applied = (List<?>) delta.getClass().getMethod("apply", List.class).invoke(delta, old);
        boolean empty = (Boolean) delta.getClass().getMethod("isEmpty").invoke(delta);
        Object stats = delta.getClass().getMethod("getStats").invoke(delta);
        inputsIntact &= old.equals(oldCopy) && next.equals(nextCopy);
        pairs++;
        operations += ops.size();
        out.append(label).append(" n=").append(old.size()).append(" m=").append(next.size())
                .append(" d=").append(distance).append(" ident=").append(identical).append(" empty=").append(empty)
                .append(" apply=").append(applied.equals(next) ? "OK" : "DIFF").append(" stats=").append(statsOf(stats))
                .append(" ops=");
        for (Object op : ops) {
            out.append(letter(op)).append(index(op));
            if (verbose) out.append('[').append(op).append(']');
        }
        out.append('\n');
    }

    private static String statsOf(Object stats) throws Exception {
        Class<?> type = stats.getClass();
        return type.getMethod("kept").invoke(stats) + "/" + type.getMethod("inserted").invoke(stats) + "/"
                + type.getMethod("deleted").invoke(stats) + "/" + type.getMethod("insertedBytes").invoke(stats) + "/"
                + type.getMethod("total").invoke(stats) + "/" + type.getMethod("changeRatio").invoke(stats);
    }

    private static char letter(Object op) throws Exception {
        return ((Enum<?>) op.getClass().getMethod("type").invoke(op)).name().charAt(0);
    }

    private String index(Object op) throws Exception {
        Object hash = op.getClass().getMethod("hash").invoke(op);
        String hex = String.valueOf(hash);
        Integer index = indexOf.get(hex);
        return index == null ? "{" + hex + "}" : Integer.toHexString(index);
    }

    /** The proof's exhaustive lane: every pair of binary rows of length 0..6 (127 rows, 16,129 pairs). */
    private void binaryLane() throws Exception {
        List<List<Object>> corpus = new ArrayList<>();
        for (int length = 0; length <= 6; length++) {
            for (int bits = 0; bits < (1 << length); bits++) {
                int[] indexes = new int[length];
                for (int i = 0; i < length; i++) indexes[i] = (bits & (1 << i)) == 0 ? 0 : 1;
                corpus.add(row(indexes));
            }
        }
        int serial = 0;
        for (List<Object> old : corpus) {
            for (List<Object> next : corpus) {
                pair("B" + serial++, hashes[0], old, hashes[1], next, serial <= 64);
            }
        }
    }

    /** The proof's seeded lane: 300 pairs over four hashes, lengths below 80. */
    private void proofSequentialLane() throws Exception {
        Random random = new Random(PROOF_SEED);
        for (int trial = 0; trial < 300; trial++) {
            List<Object> old = new ArrayList<>(), next = new ArrayList<>();
            for (int i = random.nextInt(80); i > 0; i--) old.add(hashes[random.nextInt(4)]);
            for (int i = random.nextInt(80); i > 0; i--) next.add(hashes[random.nextInt(4)]);
            pair("S" + trial, hashes[0], old, hashes[1], next, false);
        }
    }

    /** 100,000 seeded file-row pairs: a new row derived from the old one by a few edits, every tenth fully random. */
    private void fileRowsLane() throws Exception {
        Random random = new Random(FILE_ROWS_SEED);
        for (int serial = 0; serial < FILE_ROWS; serial++) {
            List<Object> old = new ArrayList<>();
            for (int i = random.nextInt(MAX_ROW + 1); i > 0; i--) old.add(hashes[random.nextInt(ALPHABET)]);
            List<Object> next = new ArrayList<>(old);
            if (serial % 10 == 9) {
                next.clear();
                for (int i = random.nextInt(MAX_ROW + 1); i > 0; i--) next.add(hashes[random.nextInt(ALPHABET)]);
            } else {
                for (int edits = random.nextInt(MAX_EDITS + 1); edits > 0; edits--) {
                    int kind = random.nextInt(3);
                    if (kind == 0 || next.isEmpty()) {
                        next.add(random.nextInt(next.size() + 1), hashes[random.nextInt(ALPHABET)]);
                    } else if (kind == 1) {
                        next.remove(random.nextInt(next.size()));
                    } else {
                        next.set(random.nextInt(next.size()), hashes[random.nextInt(ALPHABET)]);
                    }
                }
            }
            pair("F" + serial, hashes[2], old, hashes[3], next, false);
        }
    }

    /** Equal roots take the fast path: every old hash is kept whatever the new row says. */
    private void equalRootsLane() throws Exception {
        Random random = new Random(FILE_ROWS_SEED ^ 0x524f4f5453L);
        for (int serial = 0; serial < 1000; serial++) {
            List<Object> old = new ArrayList<>(), next = new ArrayList<>();
            for (int i = random.nextInt(MAX_ROW + 1); i > 0; i--) old.add(hashes[random.nextInt(ALPHABET)]);
            for (int i = random.nextInt(MAX_ROW + 1); i > 0; i--) next.add(hashes[random.nextInt(ALPHABET)]);
            pair("R" + serial, hashes[4], old, hashes[4], next, false);
        }
    }

    /** generateWithSizes over a store holding a random subset of the alphabet's chunk bytes. */
    private void sizesLane() throws Exception {
        Random random = new Random(FILE_ROWS_SEED ^ 0x53495a4553L);
        Class<?> storeClass = generateWithSizes.getParameterTypes()[4];
        for (int serial = 0; serial < 1000; serial++) {
            Object store = store(storeClass, random);
            List<Object> old = new ArrayList<>(), next = new ArrayList<>();
            for (int i = random.nextInt(MAX_ROW + 1); i > 0; i--) old.add(hashes[random.nextInt(ALPHABET)]);
            for (int i = random.nextInt(MAX_ROW + 1); i > 0; i--) next.add(hashes[random.nextInt(ALPHABET)]);
            Object delta = generateWithSizes.invoke(generator, hashes[5], old, hashes[6], next, store);
            List<?> ops = (List<?>) delta.getClass().getMethod("operations").invoke(delta);
            Object stats = delta.getClass().getMethod("getStats").invoke(delta);
            pairs++;
            operations += ops.size();
            out.append("Z").append(serial).append(" n=").append(old.size()).append(" m=").append(next.size())
                    .append(" stats=").append(statsOf(stats)).append(" ops=");
            for (Object op : ops) {
                out.append(letter(op)).append(index(op));
                if (letter(op) == 'I') out.append(':').append(op.getClass().getMethod("size").invoke(op));
            }
            out.append('\n');
        }
    }

    /** A java.util.Map store for the relocated owner, the donor's own store class otherwise. */
    private Object store(Class<?> storeClass, Random random) throws Exception {
        boolean[] present = new boolean[ALPHABET];
        for (int i = 0; i < ALPHABET; i++) present[i] = random.nextBoolean();
        if (storeClass == Map.class) {
            Map<Object, byte[]> store = new HashMap<>();
            for (int i = 0; i < ALPHABET; i++) if (present[i]) store.put(hashes[i], chunks[i]);
            return store;
        }
        Object store = storeClass.getConstructor().newInstance();
        Class<?> chunkClass = Class.forName(storeClass.getPackageName() + ".Chunk");
        Method fromData = chunkClass.getMethod("fromData", byte[].class);
        Method put = storeClass.getMethod("put", chunkClass);
        for (int i = 0; i < ALPHABET; i++) if (present[i]) put.invoke(store, fromData.invoke(null, (Object) chunks[i]));
        return store;
    }

    /** The proof's exception cases: null hashes, null lists and Integer.MAX_VALUE dimensions. */
    private void exceptionsLane() throws Exception {
        List<Object> nulls = Arrays.asList(null, null);
        List<Object> one = row(0);
        List<Object> huge = Collections.nCopies(Integer.MAX_VALUE, hashes[0]);
        exception("E0 distance([],[null,null])", () -> editDistance.invoke(null, List.of(), nulls));
        exception("E1 distance([null,null],[])", () -> editDistance.invoke(null, nulls, List.of()));
        exception("E2 distance([null],[A])", () -> editDistance.invoke(null, Arrays.asList((Object) null), one));
        exception("E3 distance([A,null,B],[A])", () -> editDistance.invoke(null, Arrays.asList(hashes[0], null, hashes[1]), one));
        exception("E4 distance(null,[])", () -> editDistance.invoke(null, null, List.of()));
        exception("E5 distance([],null)", () -> editDistance.invoke(null, List.of(), null));
        exception("E6 distance(huge,[])", () -> editDistance.invoke(null, huge, List.of()));
        exception("E7 distance([],huge)", () -> editDistance.invoke(null, List.of(), huge));
        exception("E8 generate(A,huge,B,[B])", () -> generate.invoke(generator, hashes[0], huge, hashes[1], row(1)));
        exception("E9 generate(A,[B],B,huge)", () -> generate.invoke(generator, hashes[0], row(1), hashes[1], huge));
        // A null hash is a legal INSERT (the generator compares old.equals(new)); its toString would throw on
        // both sides, so these two pairs print the compact script only.
        pair("E10", hashes[0], row(0, 1), hashes[1], Arrays.asList((Object) null), false);
        pair("E11", hashes[0], row(0, 1), hashes[1], Arrays.asList(null, hashes[1], null), false);
    }

    private interface Call {
        Object call() throws Exception;
    }

    private void exception(String label, Call call) {
        String result;
        try {
            result = "value " + call.call();
        } catch (InvocationTargetException e) {
            result = "throws " + e.getCause().getClass().getName();
        } catch (Exception e) {
            result = "throws " + e.getClass().getName();
        }
        out.append(label).append(" -> ").append(result).append('\n');
    }

    /** The 48 MiB probe's inputs on the default heap: 5000 deletes then 5000 inserts, distance 5000. */
    private void probeLane() throws Exception {
        List<Object> old = Collections.nCopies(5000, hashes[0]);
        List<Object> next = Collections.nCopies(5000, hashes[1]);
        Object delta = generate.invoke(generator, hashes[0], old, hashes[1], next);
        List<?> ops = (List<?>) delta.getClass().getMethod("operations").invoke(delta);
        int distance = (Integer) editDistance.invoke(null, old, next);
        pairs++;
        operations += ops.size();
        out.append("P 5000x5000 operations=").append(ops.size()).append(" first=").append(letter(ops.get(0)))
                .append(index(ops.get(0))).append(" op5000=").append(letter(ops.get(5000))).append(index(ops.get(5000)))
                .append(" last=").append(letter(ops.get(ops.size() - 1))).append(index(ops.get(ops.size() - 1)))
                .append(" distance=").append(distance).append(" scriptSha256=")
                .append(HEX.formatHex(sha256(compact(ops).getBytes(StandardCharsets.UTF_8)))).append('\n');
    }

    private String compact(List<?> ops) throws Exception {
        StringBuilder script = new StringBuilder(ops.size() * 2);
        for (Object op : ops) script.append(letter(op)).append(index(op));
        return script.toString();
    }
}
