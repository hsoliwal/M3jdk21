// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.compute.vector;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * CPU-first integer-vector addition with opt-in hardware providers and exact CPU parity.
 * No arbitrary kernel IDs, code, native libraries or device selection are accepted here.
 * Addition uses Java's specified 32-bit two's-complement wraparound semantics.
 */
public final class VectorCompute {
    private static final int MAX_ELEMENTS = 1_000_000;
    private VectorCompute() { }

    /** Provider implementation is external to the M3 JDK image, e.g. TornadoVM. */
    public interface Provider {
        /** Stable provider identifier, for example {@code tornadovm-vector-add}. */
        String id();
        /** Inputs are disposable copies; result must have the same length. */
        int[] add(int[] left, int[] right);
    }

    /** Result never exposes a mutable internal array. Hash is evidence, not authorization. */
    public record Result(int[] values, String backend, String receiptSha256) {
        public Result {
            values = Objects.requireNonNull(values).clone();
            Objects.requireNonNull(backend);
            Objects.requireNonNull(receiptSha256);
        }
        @Override public int[] values() { return values.clone(); }
    }

    /** CPU reference implementation, used when no provider is explicitly requested. */
    public static Result add(int[] left, int[] right) {
        return addVerified(left, right, null);
    }

    /** Select an installed ServiceLoader backend by exact ID, never by class name. */
    public static Result addWithBackend(int[] left, int[] right, String backendId) {
        if (backendId == null || backendId.isBlank() || "cpu".equals(backendId))
            return add(left, right);
        if (!backendId.matches("[a-z][a-z0-9._-]{1,63}"))
            throw new IllegalArgumentException("backend ID");
        Provider selected = null;
        for (Provider provider : ServiceLoader.load(Provider.class)) {
            if (backendId.equals(provider.id())) {
                if (selected != null) throw new IllegalStateException("duplicate backend ID");
                selected = provider;
            }
        }
        if (selected == null) throw new IllegalStateException("backend unavailable: " + backendId);
        return addVerified(left, right, selected);
    }

    /**
     * Exact differential gate. A wrong answer, unexpected mutation or runtime error fails
     * closed; there is no silent GPU acceptance or parity-mismatch CPU fallback.
     */
    public static Result addVerified(int[] left, int[] right, Provider provider) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length != right.length || left.length > MAX_ELEMENTS)
            throw new IllegalArgumentException("vector length/limit");
        int[] oracle = new int[left.length];
        for (int i = 0; i < oracle.length; i++) oracle[i] = left[i] + right[i];
        String id = "cpu";
        if (provider != null) {
            id = Objects.requireNonNull(provider.id(), "provider ID");
            if (!id.matches("[a-z][a-z0-9._-]{1,63}") || "cpu".equals(id))
                throw new IllegalArgumentException("provider ID");
            int[] leftCopy = left.clone();
            int[] rightCopy = right.clone();
            int[] candidate = provider.add(leftCopy, rightCopy);
            if (!Arrays.equals(leftCopy, left) || !Arrays.equals(rightCopy, right))
                throw new IllegalStateException("backend mutated its input");
            if (candidate == null || !Arrays.equals(candidate, oracle))
                throw new IllegalStateException("accelerator CPU parity mismatch");
        }
        return new Result(oracle, id, sha256(left, right, oracle));
    }

    private static String sha256(int[] left, int[] right, int[] result) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("M3_VECTOR_ADD_V1\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            for (int[] array : new int[][] { left, right, result }) {
                digest.update(ByteBuffer.allocate(4).putInt(array.length).array());
                ByteBuffer scratch = ByteBuffer.allocate(4);
                for (int value : array) {
                    scratch.clear();
                    digest.update(scratch.putInt(value).array());
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** Standalone JMOD oracle; accelerator and native libraries are intentionally absent. */
    public static void main(String[] args) {
        if (args.length != 1 || !"--self-test".equals(args[0]))
            throw new IllegalArgumentException("--self-test");
        int[] left = { Integer.MAX_VALUE, -1, 3 };
        int[] right = { 1, 1, 5 };
        Result cpu = add(left, right);
        if (!Arrays.equals(cpu.values(), new int[] { Integer.MIN_VALUE, 0, 8 }))
            throw new AssertionError("Java int overflow");
        Result approved = addVerified(left, right, new Provider() {
            @Override public String id() { return "checked-test"; }
            @Override public int[] add(int[] a, int[] b) {
                int[] r = new int[a.length];
                for (int i = 0; i < r.length; i++) r[i] = a[i] + b[i];
                return r;
            }
        });
        if (!cpu.receiptSha256().equals(approved.receiptSha256()))
            throw new AssertionError("backend changed semantic hash");
        int[] leaked = approved.values();
        leaked[0] = 42;
        if (approved.values()[0] != Integer.MIN_VALUE) throw new AssertionError("mutable result");
        try {
            addVerified(left, right, new Provider() {
                @Override public String id() { return "bad-test"; }
                @Override public int[] add(int[] a, int[] b) { return new int[a.length]; }
            });
            throw new AssertionError("bad backend accepted");
        } catch (IllegalStateException expected) { /* parity correctly rejected */ }
        System.out.println("M3_JMOD_TEST_PASS compute.vector");
    }
}
