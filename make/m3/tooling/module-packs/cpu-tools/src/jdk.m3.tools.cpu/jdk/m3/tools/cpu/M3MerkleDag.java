// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.tools.cpu;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Content-derived SHA-256 DAG node hashes without retained per-edge objects. */
public final class M3MerkleDag {
    public static final int HASH_BYTES = 32;
    private static final int MAX_NODES = 4096;
    private static final int MAX_EDGES = 131072;
    private static final long MAX_DIGEST_BYTES = 16_000_000L;
    private static final byte[] DOMAIN = {'M', '3', 'D', 'A', 'G', '1'};

    private M3MerkleDag() { }

    /**
     * SHA256(M3DAG1 || payloadSHA256 || uint32BE(childCount)
     *        || ordered child hashes). Every edge points to a strictly earlier node.
     * Root extraction is an ordinary caller-owned 32-byte slice of the result.
     */
    public static byte[] hashNodes(byte[] payloadHashes, int[] offsets,
            int[] children, long maxDigestBytes) {
        int nodes = validate(payloadHashes, offsets, children, maxDigestBytes);
        long required = 42L * nodes + HASH_BYTES * (long) children.length;
        if (required > maxDigestBytes) {
            throw new IllegalStateException("Merkle DAG digest byte budget exhausted");
        }
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("required SHA-256 provider unavailable", error);
        }
        byte[] result = new byte[nodes * HASH_BYTES];
        byte[] arity = new byte[4];
        for (int node = 0; node < nodes; node++) {
            md.update(DOMAIN);
            md.update(payloadHashes, node * HASH_BYTES, HASH_BYTES);
            int count = offsets[node + 1] - offsets[node];
            arity[0] = (byte) (count >>> 24);
            arity[1] = (byte) (count >>> 16);
            arity[2] = (byte) (count >>> 8);
            arity[3] = (byte) count;
            md.update(arity);
            for (int edge = offsets[node]; edge < offsets[node + 1]; edge++) {
                md.update(result, children[edge] * HASH_BYTES, HASH_BYTES);
            }
            try {
                if (md.digest(result, node * HASH_BYTES, HASH_BYTES) != HASH_BYTES) {
                    throw new IllegalStateException("SHA-256 returned an invalid digest length");
                }
            } catch (DigestException error) {
                throw new IllegalStateException("SHA-256 digest output was rejected", error);
            }
        }
        return result;
    }

    private static int validate(byte[] payloadHashes, int[] offsets,
            int[] children, long maxDigestBytes) {
        if (payloadHashes == null || offsets == null || children == null
                || offsets.length == 0 || offsets.length - 1 > MAX_NODES
                || children.length > MAX_EDGES || maxDigestBytes < 0
                || maxDigestBytes > MAX_DIGEST_BYTES) {
            throw new IllegalArgumentException("invalid Merkle DAG or digest budget");
        }
        int nodes = offsets.length - 1;
        if (payloadHashes.length != nodes * HASH_BYTES
                || offsets[0] != 0 || offsets[nodes] != children.length) {
            throw new IllegalArgumentException("invalid Merkle DAG CSR endpoints");
        }
        for (int node = 0; node < nodes; node++) {
            int first = offsets[node];
            int end = offsets[node + 1];
            if (first < 0 || end < first || end > children.length) {
                throw new IllegalArgumentException("invalid Merkle DAG CSR order");
            }
            for (int e = first; e < end; e++) {
                if (children[e] < 0 || children[e] >= node) {
                    throw new IllegalArgumentException("Merkle DAG must be topologically ordered");
                }
            }
        }
        return nodes;
    }
}
