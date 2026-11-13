// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors

import jdk.m3.tools.cpu.M3MerkleDag;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

/** Independent byte-stream SHA-256 oracle for exact topological DAG framing. */
public final class M3MerkleDagContract {
    private static int checks;
    private M3MerkleDagContract() { }

    private static void require(boolean yes, String what) {
        checks++;
        if (!yes) { throw new AssertionError(what); }
    }

    private static void refuse(Class<? extends Throwable> type, Runnable call, String what) {
        checks++;
        try { call.run(); }
        catch (Throwable actual) {
            if (type.isInstance(actual)) { return; }
            throw new AssertionError("unexpected refusal in " + what, actual);
        }
        throw new AssertionError("missing refusal in " + what);
    }

    private static byte[] oracle(byte[] payload, int[] offsets, int[] children) {
        byte[] out = new byte[payload.length];
        for (int node = 0; node < offsets.length - 1; node++) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.writeBytes(new byte[] {'M', '3', 'D', 'A', 'G', '1'});
            stream.write(payload, node * 32, 32);
            int count = offsets[node + 1] - offsets[node];
            stream.write((byte) (count >>> 24));
            stream.write((byte) (count >>> 16));
            stream.write((byte) (count >>> 8));
            stream.write((byte) count);
            for (int e = offsets[node]; e < offsets[node + 1]; e++) {
                stream.write(out, children[e] * 32, 32);
            }
            try {
                byte[] hash = MessageDigest.getInstance("SHA-256")
                        .digest(stream.toByteArray());
                System.arraycopy(hash, 0, out, node * 32, 32);
            } catch (NoSuchAlgorithmException error) {
                throw new AssertionError(error);
            }
        }
        return out;
    }

    private static long cost(int nodes, int edges) { return 42L * nodes + 32L * edges; }

    public static void main(String[] args) {
        Random random = new Random(0x71234567L);
        require(M3MerkleDag.hashNodes(new byte[0], new int[] {0}, new int[0], 0).length
                == 0, "empty DAG output");
        refuse(IllegalArgumentException.class,
                () -> M3MerkleDag.hashNodes(new byte[32], new int[] {0, 1},
                        new int[] {0}, 100), "self-loop");
        refuse(IllegalArgumentException.class,
                () -> M3MerkleDag.hashNodes(new byte[32], new int[] {0, 0, 0},
                        new int[0], 100), "payload mismatch");
        refuse(IllegalArgumentException.class,
                () -> M3MerkleDag.hashNodes(new byte[64], new int[] {0, 0, 1},
                        new int[] {-1}, 100), "negative child");
        refuse(IllegalArgumentException.class,
                () -> M3MerkleDag.hashNodes(new byte[64], new int[] {0, 1, 1},
                        new int[] {0}, 100), "future child");
        refuse(IllegalArgumentException.class,
                () -> M3MerkleDag.hashNodes(new byte[32], new int[] {0, 0},
                        new int[0], -1), "negative budget");
        refuse(IllegalStateException.class,
                () -> M3MerkleDag.hashNodes(new byte[32], new int[] {0, 0},
                        new int[0], 41), "digest byte budget");
        byte[] example = new byte[96];
        for (int i = 0; i < example.length; i++) { example[i] = (byte) i; }
        int[] offsets = {0, 0, 0, 2};
        int[] children = {0, 1};
        byte[] base = M3MerkleDag.hashNodes(example, offsets, children, cost(3, 2));
        require(Arrays.equals(oracle(example, offsets, children), base),
                "two-child oracle");
        byte[] edited = example.clone();
        edited[0]++;
        byte[] changed = M3MerkleDag.hashNodes(edited, offsets, children, cost(3, 2));
        require(!Arrays.equals(Arrays.copyOfRange(base, 0, 32),
                Arrays.copyOfRange(changed, 0, 32)), "leaf digest should change");
        require(Arrays.equals(Arrays.copyOfRange(base, 32, 64),
                Arrays.copyOfRange(changed, 32, 64)), "unrelated leaf changed");
        require(!Arrays.equals(Arrays.copyOfRange(base, 64, 96),
                Arrays.copyOfRange(changed, 64, 96)), "parent did not change");
        byte[] reordered = M3MerkleDag.hashNodes(example, offsets, new int[] {1, 0},
                cost(3, 2));
        require(!Arrays.equals(base, reordered), "ordered edges must affect hash");
        for (int trial = 0; trial < 2000; trial++) {
            int nodes = random.nextInt(50);
            byte[] payload = new byte[32 * nodes];
            random.nextBytes(payload);
            int[] counts = new int[nodes];
            int edgeCount = 0;
            for (int node = 1; node < nodes; node++) {
                counts[node] = random.nextInt(Math.min(node + 1, 9));
                edgeCount += counts[node];
            }
            int[] csr = new int[nodes + 1];
            int[] edges = new int[edgeCount];
            int cursor = 0;
            for (int node = 0; node < nodes; node++) {
                for (int i = 0; i < counts[node]; i++) {
                    edges[cursor++] = random.nextInt(node);
                }
                csr[node + 1] = cursor;
            }
            byte[] oldPayload = payload.clone();
            int[] oldCsr = csr.clone();
            int[] oldEdges = edges.clone();
            long cost = cost(nodes, edgeCount);
            byte[] expected = oracle(payload, csr, edges);
            byte[] actual = M3MerkleDag.hashNodes(payload, csr, edges, cost);
            require(Arrays.equals(expected, actual), "random SHA-256 oracle");
            require(Arrays.equals(actual,
                    M3MerkleDag.hashNodes(payload, csr, edges, cost)),
                    "non-repeatable Merkle DAG");
            require(Arrays.equals(payload, oldPayload) && Arrays.equals(csr, oldCsr)
                    && Arrays.equals(edges, oldEdges), "input DAG mutated");
            if (cost > 0) {
                refuse(IllegalStateException.class, () -> M3MerkleDag.hashNodes(
                        payload, csr, edges, cost - 1), "one byte under minimum budget");
            }
        }
        int nodes = 4096;
        int[] chain = new int[nodes - 1];
        int[] chainOffsets = new int[nodes + 1];
        for (int node = 1; node < nodes; node++) {
            chain[node - 1] = node - 1;
            chainOffsets[node + 1] = node;
        }
        byte[] large = M3MerkleDag.hashNodes(new byte[nodes * 32], chainOffsets,
                chain, cost(nodes, nodes - 1));
        require(large.length == nodes * 32, "max nodes and sparse edges");
        require(Arrays.equals(large, oracle(new byte[nodes * 32], chainOffsets, chain)),
                "maximum nodes verified independently");
        System.out.println("M3_WAVE7_MERKLE_DAG_PASS checks=" + checks);
    }
}
