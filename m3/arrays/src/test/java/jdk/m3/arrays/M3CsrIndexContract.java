// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Exact CSR builder oracle for source-pinned direct owner-array construction. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-mat-value/src/test/java/com/synexia/matvalue/MatCsrDirectContract.java blob 4c8e52d37040; recipe m3/arrays/recipe/csr-index-20261106/relocate.py v1
public final class M3CsrIndexContract {
    private static long checks;

    private M3CsrIndexContract() { }

    public static void main(String[] args) throws Exception {
        invalidInputs();
        boundaryCases();
        seededDifferential();
        immutableConcurrentReaders();
        System.out.println("MAT_CSR_DIRECT_CHECKS=" + checks);
        System.out.println("RESULT=PASS");
    }

    private static void invalidInputs() {
        fails(NullPointerException.class, () -> M3CsrIndex.fromOwners(0, null), "null owner first");
        fails(NullPointerException.class, () -> M3CsrIndex.fromOwners(-1, null), "null outranks invalid rows");
        fails(IllegalArgumentException.class, () -> M3CsrIndex.fromOwners(-1, new int[0]), "negative rows");
        fails(IndexOutOfBoundsException.class, () -> M3CsrIndex.fromOwners(0, new int[] {0}), "zero rows nonempty");
        fails(IndexOutOfBoundsException.class, () -> M3CsrIndex.fromOwners(1, new int[] {-1}), "negative row");
        fails(IndexOutOfBoundsException.class, () -> M3CsrIndex.fromOwners(1, new int[] {1}), "row upper bound");
        fails(IndexOutOfBoundsException.class, () -> M3CsrIndex.fromOwners(4, new int[] {1, 2, 4}), "later invalid row");
    }

    private static void boundaryCases() {
        for (int rows : new int[] {0, 1, 2, 63, 64, 65, 257}) {
            compare(rows, new int[0]);
            if (rows > 0) {
                for (int owner : new int[] {0, rows / 2, rows - 1}) {
                    for (int length : new int[] {1, 2, 3, 8, 33, 257}) {
                        int[] inputs = new int[length];
                        Arrays.fill(inputs, owner);
                        compare(rows, inputs);
                    }
                }
            }
        }
    }

    private static void seededDifferential() {
        Random rng = new Random(0x4333525F44495245L);
        for (int round = 0; round < 1600; round++) {
            int rows = rng.nextInt(2049);
            int length = rows == 0 ? 0 : rng.nextInt(1025);
            int[] owners = new int[length];
            for (int index = 0; index < owners.length; index++) {
                // Alternates uniform, skewed and ordered inputs without sharding.
                owners[index] = switch (round % 5) {
                    case 0 -> rng.nextInt(rows);
                    case 1 -> rng.nextInt(Math.min(3, rows));
                    case 2 -> (index % rows);
                    case 3 -> (rows - 1 - index % rows);
                    default -> rng.nextInt(rows) / 2;
                };
            }
            compare(rows, owners);
        }
    }

    private static void compare(int rows, int[] ownerByReference) {
        int[] original = ownerByReference.clone();
        M3CsrIndex actual = M3CsrIndex.fromOwners(rows, ownerByReference);
        M3CsrIndex.Builder staged = M3CsrIndex.builder(rows, ownerByReference.length);
        for (int ref = 0; ref < ownerByReference.length; ref++) staged.add(ownerByReference[ref], ref);
        M3CsrIndex oldOracle = staged.build();
        eq(rows, actual.rowCount(), "row count");
        eq(original.length, actual.size(), "reference count");
        eq(oldOracle.starts(), actual.starts(), "legacy builder offset parity");
        eq(oldOracle.references(), actual.references(), "legacy builder reference order");
        eq(oldOracle.payloadBytes(), actual.payloadBytes(), "exact retained payload bytes");
        List<List<Integer>> independent = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) independent.add(new ArrayList<>());
        for (int ref = 0; ref < original.length; ref++) independent.get(original[ref]).add(ref);
        int count = 0;
        for (int row = 0; row < rows; row++) {
            List<Integer> references = independent.get(row);
            eq(count, actual.start(row), "prefix start");
            eq(references.size(), actual.degree(row), "row degree");
            count += references.size();
            eq(count, actual.end(row), "prefix end");
            eq(references.stream().mapToInt(Integer::intValue).toArray(), actual.row(row), "stable row positions");
            for (int ordinal = 0; ordinal < references.size(); ordinal++) {
                eq(references.get(ordinal), actual.referenceAt(row, ordinal), "row ordinal");
            }
        }
        eq(original.length, count, "all references addressed");
        int[] detached = actual.starts();
        detached[0] = 999;
        eq(0, actual.starts()[0], "starts defensive copy");
        int[] exposed = actual.references();
        if (exposed.length > 0) {
            exposed[0] ^= 3;
            eq(oldOracle.references(), actual.references(), "references defensive copy");
        }
        if (ownerByReference.length > 0) {
            Arrays.fill(ownerByReference, -1);
            eq(oldOracle.references(), actual.references(), "caller mutation detached");
        }
        fails(IndexOutOfBoundsException.class, () -> actual.start(-1), "negative row query");
        fails(IndexOutOfBoundsException.class, () -> actual.end(rows), "end row query");
        fails(IndexOutOfBoundsException.class, () -> actual.referenceAtAbsolute(original.length),
                "absolute reference limit");
    }

    private static void immutableConcurrentReaders() throws ExecutionException, InterruptedException {
        int rows = 64;
        int[] owners = new int[16384];
        for (int i = 0; i < owners.length; i++) owners[i] = (i * 17) & (rows - 1);
        M3CsrIndex frozen = M3CsrIndex.fromOwners(rows, owners);
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Future<Long>> futures = new ArrayList<>();
            for (int task = 0; task < 4; task++) {
                futures.add(executor.submit(() -> {
                    long sum = 0;
                    for (int replay = 0; replay < 16; replay++) {
                        for (int row = 0; row < rows; row++) {
                            int[] refs = frozen.row(row);
                            for (int ref : refs) sum += ref;
                        }
                    }
                    return sum;
                }));
            }
            long first = futures.getFirst().get();
            for (Future<Long> future : futures) eq(first, future.get(), "deterministic frozen readers");
        }
    }

    private static void yes(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }

    private static void eq(Object expected, Object actual, String message) {
        boolean same = expected instanceof int[] ea && actual instanceof int[] aa
                ? Arrays.equals(ea, aa) : Objects.equals(expected, actual);
        yes(same, message);
    }

    private static void fails(Class<? extends Throwable> kind, Runnable action, String message) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError(message + ": wrong exception " + failure, failure);
        }
        throw new AssertionError(message + ": unexpected acceptance");
    }
}
