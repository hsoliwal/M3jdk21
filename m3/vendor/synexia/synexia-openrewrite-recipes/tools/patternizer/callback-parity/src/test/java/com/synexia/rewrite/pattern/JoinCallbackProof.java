// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.pattern;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

/** Finite, deterministic callback-observation oracle; not recipe application authority. */
public final class JoinCallbackProof {
    private static final int[] UNIVERSE = {0, 1, 2, 63, 64, 4095, 4096, Integer.MAX_VALUE};
    private long cases;
    private long assertions;
    private final MessageDigest digest;

    private enum Mode {
        NORMAL, CANCEL, CANCEL_THROW, CANCEL_ERROR, PROGRESS_THROW,
        PROGRESS_CANCEL, PROGRESS_INTERRUPT, INITIAL_INTERRUPT,
        NULL_CANCEL, NULL_PROGRESS, NULL_BOTH
    }

    private record Outcome(int[] values, String trace, String error, String message,
                           boolean sameException, boolean interrupted) { }

    private JoinCallbackProof() throws Exception {
        digest = MessageDigest.getInstance("SHA-256");
    }

    private void require(boolean accepted, String message) {
        assertions++;
        if (!accepted) throw new AssertionError(message);
    }

    private Outcome observe(M3PatternizerJoins.Intersection backend, int[] left, int[] right,
                            Mode mode, int cutoff) {
        require(!Thread.currentThread().isInterrupted(), "driver interrupt leak");
        int[] a = left == null ? null : left.clone();
        int[] b = right == left ? a : right == null ? null : right.clone();
        StringBuilder trace = new StringBuilder();
        int[] calls = {0};
        boolean[] canceled = {false};
        RuntimeException marker = new IllegalStateException("callback marker");
        AssertionError errorMarker = new AssertionError("callback error marker");
        BooleanSupplier cancel = () -> {
            trace.append('C').append(++calls[0]).append(';');
            if (calls[0] == cutoff) {
                if (mode == Mode.CANCEL_THROW) throw marker;
                if (mode == Mode.CANCEL_ERROR) throw errorMarker;
                if (mode == Mode.CANCEL) return true;
            }
            return canceled[0];
        };
        LongConsumer progress = work -> {
            trace.append('P').append(work).append(';');
            if (mode == Mode.PROGRESS_THROW) throw marker;
            if (mode == Mode.PROGRESS_CANCEL) canceled[0] = true;
            if (mode == Mode.PROGRESS_INTERRUPT) Thread.currentThread().interrupt();
        };
        if (mode == Mode.NULL_CANCEL || mode == Mode.NULL_BOTH) cancel = null;
        if (mode == Mode.NULL_PROGRESS || mode == Mode.NULL_BOTH) progress = null;
        int[] values = null;
        Throwable failure = null;
        boolean interrupted;
        try {
            if (mode == Mode.INITIAL_INTERRUPT) Thread.currentThread().interrupt();
            values = backend.apply(a, b, cancel, progress);
        } catch (RuntimeException | Error observed) {
            failure = observed;
        } finally {
            interrupted = Thread.currentThread().isInterrupted();
            Thread.interrupted();
        }
        require(Arrays.equals(left, a) && Arrays.equals(right, b), "input mutation");
        if (values != null) require(values != a && values != b, "returned input alias");
        return new Outcome(values, trace.toString(), failure == null ? "" : failure.getClass().getName(),
                failure == null ? "" : Objects.toString(failure.getMessage(), ""),
                failure == marker || failure == errorMarker, interrupted);
    }

    private void compare(M3PatternizerJoins.Intersection backend, int[] a, int[] b,
                         Mode mode, int cutoff, int[] expected) {
        Outcome java = observe(M3PatternizerJoins.JAVA, a, b, mode, cutoff);
        Outcome nativeResult = observe(backend, a, b, mode, cutoff);
        require(Arrays.equals(java.values(), nativeResult.values()), "result parity " + mode);
        require(java.trace().equals(nativeResult.trace()),
                "callback trace mismatch: mode=" + mode + " cutoff=" + cutoff
                        + " java=" + java.trace() + " native=" + nativeResult.trace());
        require(java.error().equals(nativeResult.error()), "exception type parity " + mode);
        require(java.message().equals(nativeResult.message()), "exception message parity " + mode);
        require(java.sameException() == nativeResult.sameException(), "callback exception identity " + mode);
        require(java.interrupted() == nativeResult.interrupted(), "interrupt status parity " + mode);
        if (expected != null) {
            require(java.error().isEmpty(), "oracle unexpectedly failed");
            require(Arrays.equals(expected, java.values()), "independent subset oracle");
        }
        String row = cases++ + "\t" + mode + "\t" + cutoff + "\t"
                + Arrays.toString(java.values()) + "\t" + java.trace() + "\t" + java.error()
                + "\t" + java.message() + "\t" + java.sameException() + "\t" + java.interrupted() + "\n";
        digest.update(row.getBytes(StandardCharsets.UTF_8));
    }

    private static int[] subset(int bits) {
        int[] result = new int[Integer.bitCount(bits)];
        int at = 0;
        for (int i = 0; i < UNIVERSE.length; i++) {
            if ((bits & (1 << i)) != 0) result[at++] = UNIVERSE[i];
        }
        return result;
    }

    private static int[] sequence(int length, int step) {
        int[] result = new int[length];
        for (int i = 0; i < length; i++) result[i] = i * step;
        return result;
    }

    private void run(M3PatternizerJoins.Intersection backend, boolean smoke) {
        compare(backend, new int[0], new int[0], Mode.NORMAL, 0, new int[0]);
        if (smoke) return;
        for (int left = 0; left < 256; left++) {
            for (int right = 0; right < 256; right++) {
                compare(backend, subset(left), subset(right), Mode.NORMAL, 0, subset(left & right));
            }
        }
        int[] alias = {0, 2, Integer.MAX_VALUE};
        compare(backend, alias, alias, Mode.NORMAL, 0, alias);
        int[][] invalid = {null, {-1}, {1, 1}, {2, 1}, {0, 2, 1},
                {Integer.MIN_VALUE}, {Integer.MAX_VALUE, Integer.MAX_VALUE}};
        for (int[] bad : invalid) {
            compare(backend, bad, new int[0], Mode.NORMAL, 0, null);
            compare(backend, new int[0], bad, Mode.NORMAL, 0, null);
            compare(backend, bad, bad, Mode.CANCEL, 1, null);
            compare(backend, bad, bad, Mode.CANCEL_THROW, 1, null);
        }
        for (int length : List.of(0, 1, 4095, 4096, 4097, 8192, 8193)) {
            int[] left = sequence(length, 2);
            int[] right = sequence(length, 3);
            for (Mode mode : Mode.values()) {
                for (int cutoff = 1; cutoff <= 16; cutoff++) {
                    compare(backend, left, right, mode, cutoff, null);
                }
            }
        }
        // The oracle contract itself is asserted, not merely Java compared with Java.
        Outcome empty = observe(M3PatternizerJoins.JAVA, new int[0], new int[0], Mode.NORMAL, 0);
        require(empty.trace().equals("C1;P0;C2;"), "empty oracle checkpoint contract");
        Outcome throwing = observe(backend, alias, alias, Mode.PROGRESS_THROW, 0);
        require(throwing.sameException() && throwing.message().equals("callback marker"),
                "progress must propagate the identical exception object");
        Outcome interrupted = observe(backend, alias, alias, Mode.INITIAL_INTERRUPT, 0);
        require(interrupted.interrupted() && interrupted.trace().isEmpty(), "entry interruption contract");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2 || args.length == 2 && !args[1].equals("--smoke")) {
            throw new IllegalArgumentException("usage: JoinCallbackProof <java|absolute-native-library> [--smoke]");
        }
        M3PatternizerJoins.Intersection backend = args[0].equals("java")
                ? M3PatternizerJoins.JAVA : M3PatternizerJoins.nativeBackend(Path.of(args[0]));
        JoinCallbackProof proof = new JoinCallbackProof();
        proof.run(backend, args.length == 2);
        System.out.println("{\"status\":\"PASS\",\"backend\":\"" + (args[0].equals("java") ? "JAVA" : "JNI")
                + "\",\"cases\":" + proof.cases + ",\"assertions\":" + proof.assertions
                + ",\"observationSha256\":\"" + HexFormat.of().formatHex(proof.digest.digest())
                + "\",\"recipeApplication\":false}");
    }
}
