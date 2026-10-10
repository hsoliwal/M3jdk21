// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.pattern;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

/** Deterministic finite reentrancy/thread-affinity proof; no source-write or promotion authority. */
public final class JoinLifecycleProof {
    private enum Mode {
        NESTED_PROGRESS, NESTED_CANCEL, CAUGHT_NESTED_FAILURE, PROPAGATED_NESTED_FAILURE,
        PROGRESS_ERROR, SUPPLIER_INTERRUPT, NULL_CALLBACKS, NORMAL
    }

    private record Observation(String result, String trace, String failure, String message,
                               boolean markerIdentity, boolean interrupted) { }

    private final MessageDigest digest;
    private long cases;
    private long assertions;

    private JoinLifecycleProof() throws Exception {
        digest = MessageDigest.getInstance("SHA-256");
    }

    private void require(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }

    private static int[] expected(int[] left, int[] right) {
        // Independent bounded nested-loop oracle, not the owner's two-pointer implementation.
        int[] buffer = new int[Math.min(left.length, right.length)];
        int count = 0;
        for (int a : left) {
            for (int b : right) {
                if (a == b) { buffer[count++] = a; break; }
            }
        }
        return Arrays.copyOf(buffer, count);
    }

    private void nested(M3PatternizerJoins.Intersection backend, StringBuilder trace) {
        int[] a = {0, 7, 17, Integer.MAX_VALUE};
        int[] b = {3, 7, 17};
        int[] value = backend.apply(a, b, null, null);
        require(Arrays.equals(new int[] {7, 17}, value), "independent nested oracle");
        require(Arrays.equals(new int[] {0, 7, 17, Integer.MAX_VALUE}, a), "nested left mutation");
        require(Arrays.equals(new int[] {3, 7, 17}, b), "nested right mutation");
        require(value != a && value != b, "nested result alias");
        trace.append("N[7,17];");
    }

    private Observation observe(M3PatternizerJoins.Intersection backend, int[] left, int[] right,
                                Mode mode) {
        require(!Thread.currentThread().isInterrupted(), "fixture interrupt leak");
        int[] beforeLeft = left.clone();
        int[] beforeRight = right.clone();
        Thread caller = Thread.currentThread();
        AssertionError marker = new AssertionError("lifecycle callback marker");
        StringBuilder trace = new StringBuilder();
        int[] polls = {0};
        BooleanSupplier canceled = () -> {
            require(Thread.currentThread() == caller, "cancellation callback thread affinity");
            trace.append('C').append(++polls[0]).append(';');
            if (polls[0] == 1) {
                if (mode == Mode.NESTED_CANCEL) nested(backend, trace);
                if (mode == Mode.CAUGHT_NESTED_FAILURE) {
                    try {
                        backend.apply(new int[] {0, 0}, new int[] {0}, null, null);
                        throw new AssertionError("nested duplicate accepted");
                    } catch (IllegalArgumentException invalid) {
                        require(Objects.equals(invalid.getMessage(),
                                "posting must be sorted unique nonnegative"), "nested error contract");
                        trace.append("caught-duplicate;");
                    }
                }
                if (mode == Mode.PROPAGATED_NESTED_FAILURE) {
                    backend.apply(new int[] {0}, new int[] {0}, () -> true, null);
                    throw new AssertionError("nested cancellation accepted");
                }
            }
            if (polls[0] == 2 && mode == Mode.SUPPLIER_INTERRUPT) caller.interrupt();
            return false;
        };
        LongConsumer progress = work -> {
            require(Thread.currentThread() == caller, "progress callback thread affinity");
            trace.append('P').append(work).append(';');
            if (mode == Mode.NESTED_PROGRESS) nested(backend, trace);
            if (mode == Mode.PROGRESS_ERROR) throw marker;
        };
        int[] value = null;
        Throwable failure = null;
        boolean interrupted;
        try {
            value = backend.apply(left, right, mode == Mode.NULL_CALLBACKS ? null : canceled,
                                  mode == Mode.NULL_CALLBACKS ? null : progress);
        } catch (RuntimeException | Error caught) {
            failure = caught;
        } finally {
            interrupted = caller.isInterrupted();
            Thread.interrupted(); // Fixture cleanup only; observe before clearing.
        }
        require(Arrays.equals(beforeLeft, left) && Arrays.equals(beforeRight, right), "input mutation");
        if (value != null) {
            require(value != left && value != right, "result aliases a caller array");
            require(Arrays.equals(expected(beforeLeft, beforeRight), value), "independent result oracle");
        }
        if (mode == Mode.PROGRESS_ERROR) require(failure == marker, "Error object identity");
        if (mode == Mode.SUPPLIER_INTERRUPT) require(interrupted, "interrupt state preserved");
        if (mode == Mode.PROPAGATED_NESTED_FAILURE || mode == Mode.SUPPLIER_INTERRUPT) {
            require(failure instanceof java.util.concurrent.CancellationException,
                    "cancellation failure contract");
        } else if (mode != Mode.PROGRESS_ERROR) {
            require(failure == null, "unexpected failure: " + failure);
        }
        return new Observation(Arrays.toString(value), trace.toString(),
                failure == null ? "" : failure.getClass().getName(),
                failure == null ? "" : Objects.toString(failure.getMessage(), ""),
                failure == marker, interrupted);
    }

    private void compare(M3PatternizerJoins.Intersection backend, int[] left, int[] right,
                         Mode mode, String label) {
        Observation oracle = observe(M3PatternizerJoins.JAVA, left, right, mode);
        Observation candidate = observe(backend, left, right, mode);
        require(oracle.equals(candidate), "lifecycle parity " + label + ": " + oracle + " != " + candidate);
        digest.update((label + "\t" + mode + "\t" + oracle + "\n").getBytes(StandardCharsets.UTF_8));
        cases++;
    }

    private static int[] sequence(int size, int offset, int step) {
        int[] values = new int[size];
        for (int i = 0; i < size; i++) values[i] = offset + i * step;
        return values;
    }

    private String root() { return HexFormat.of().formatHex(digest.digest()); }

    private void run(M3PatternizerJoins.Intersection backend) throws Exception {
        for (int index = 0; index < 128; index++) {
            int[] left = sequence(1 + index % 32, index % 4, 2);
            int[] right = sequence(1 + index % 24, index % 3, 3);
            for (Mode mode : Mode.values()) compare(backend, left, right, mode, "serial-" + index);
        }
        // Roaring's char-lane contract is not this owner's 31-bit row-ID domain.
        for (int boundary : new int[] {65535, 65536, 1 << 24, Integer.MAX_VALUE}) {
            for (Mode mode : Mode.values()) {
                compare(backend, new int[] {0, boundary}, new int[] {1, boundary}, mode,
                        "width-" + boundary);
            }
        }
        int[] alias = {0, 7, 17, Integer.MAX_VALUE};
        for (Mode mode : Mode.values()) compare(backend, alias, alias, mode, "alias");
        // Shared immutable inputs; every worker retains its own callbacks, digest and counters.
        int[] sharedLeft = sequence(64, 0, 2);
        int[] sharedRight = sequence(64, 0, 3);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReferenceArray<JoinLifecycleProof> proofs = new AtomicReferenceArray<>(4);
        AtomicReferenceArray<Throwable> failures = new AtomicReferenceArray<>(4);
        String[] roots = new String[4];
        Thread[] workers = new Thread[4];
        for (int worker = 0; worker < workers.length; worker++) {
            final int partition = worker;
            workers[worker] = new Thread(() -> {
                try {
                    start.await();
                    JoinLifecycleProof proof = new JoinLifecycleProof();
                    for (int index = 0; index < 64; index++) {
                        proof.compare(backend, sharedLeft, sharedRight,
                                Mode.values()[index % Mode.values().length], partition + "-" + index);
                    }
                    roots[partition] = proof.root();
                    proofs.set(partition, proof);
                } catch (Throwable failure) {
                    failures.set(partition, failure);
                }
            }, "callback-proof-" + worker);
            workers[worker].setDaemon(true);
            workers[worker].start();
        }
        start.countDown();
        for (int worker = 0; worker < workers.length; worker++) {
            workers[worker].join(30_000);
            require(!workers[worker].isAlive(), "worker deadline");
            if (failures.get(worker) != null) throw new AssertionError("worker failure", failures.get(worker));
            JoinLifecycleProof proof = proofs.get(worker);
            require(proof != null, "missing worker proof");
            cases += proof.cases;
            assertions += proof.assertions;
            digest.update(("worker-" + worker + "\t" + roots[worker] + "\n").getBytes(StandardCharsets.UTF_8));
        }
        require(Arrays.equals(sharedLeft, sequence(64, 0, 2)), "shared left mutation");
        require(Arrays.equals(sharedRight, sequence(64, 0, 3)), "shared right mutation");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: JoinLifecycleProof <java|absolute-library>");
        M3PatternizerJoins.Intersection backend = args[0].equals("java") ? M3PatternizerJoins.JAVA
                : M3PatternizerJoins.nativeBackend(Path.of(args[0]));
        JoinLifecycleProof proof = new JoinLifecycleProof();
        proof.run(backend);
        System.out.println("{\"status\":\"PASS\",\"backend\":\"" + (args[0].equals("java") ? "JAVA" : "JNI")
                + "\",\"cases\":" + proof.cases + ",\"assertions\":" + proof.assertions
                + ",\"observationSha256\":\"" + proof.root() + "\",\"recipeApplication\":false}");
    }
}
