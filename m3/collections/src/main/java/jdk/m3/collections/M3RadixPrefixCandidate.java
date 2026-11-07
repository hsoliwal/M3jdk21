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
package jdk.m3.collections;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.IntBinaryOperator;

/**
 * Finite-snapshot primitive evidence only, not a comparator proof or an authorization gate.
 *
 * <p>The evidence is sound for key-prefixed ordering ONLY if the canonical comparator is
 * independently proved a pure deterministic total preorder and the input is an immutable
 * owner snapshot. A nontransitive comparator can pass these finite boundary checks.
 * The existing M3 signal ReceiptVerifier must independently admit any intended use.
 * No source, value, cache, object payload or per-entry metadata is retained here.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/M3RadixPrefixCandidate.java}
 * blob {@code 30ccc82f5d78edaca5a0cc8aaefd3236a79de914} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
final class M3RadixPrefixCandidate {
    /** Bounded in-memory source snapshot for one qualification operation. */
    static final int MAX_ROWS = 100_000;
    private static final int RADIX = 256;
    private static final byte[] DOMAIN =
            "SYNEXIA/M3-SIG-001/RADIX-CANDIDATE/1\u0000".getBytes(StandardCharsets.US_ASCII);

    /**
     * An audit observation, NEVER a capability/certificate. No retained signal arrays.
     * Candidate monotonicity is conditional upon separately certified comparator laws.
     */
    record Observation(int rows, int groups, long comparatorCalls,
            boolean candidateMonotone, String transcriptSha256) { }

    private M3RadixPrefixCandidate() { }

    /**
     * Test one exact signed-long projection against a caller-owned canonical comparator.
     * The comparator must be pure, deterministic and a total preorder; this method cannot
     * establish those obligations and never bypasses any existing proof/epoch gate.
     *
     * @param keys immutable array of one exact signed-long coordinate per source row
     * @param canonical canonical comparator over original stable row ordinals
     * @return candidate observation, not permission to optimize or modify source
     */
    static Observation observe(long[] keys, IntBinaryOperator canonical) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(canonical, "canonical");
        int n = keys.length;
        if (n > MAX_ROWS) throw new IllegalArgumentException("M3 signal snapshot budget exceeded");
        long[] key = keys.clone();
        int[] order = new int[n];
        int[] scratch = new int[n];
        int[] counts = new int[RADIX];
        MessageDigest digest = sha256();
        digest.update(DOMAIN);
        ByteBuffer word = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.BIG_ENDIAN);
        add(digest, word, n);
        for (int i = 0; i < n; i++) {
            order[i] = i;
            add(digest, word, key[i]);
        }
        // Exact signed order: flip sign bit and radix the resulting unsigned key.
        for (int pass = 0; pass < Long.BYTES; pass++) {
            Arrays.fill(counts, 0);
            int shift = pass << 3;
            for (int row : order) {
                counts[(int) (((key[row] ^ Long.MIN_VALUE) >>> shift) & 255L)]++;
            }
            int first = 0;
            for (int bucket = 0; bucket < RADIX; bucket++) {
                int frequency = counts[bucket];
                counts[bucket] = first;
                first += frequency;
            }
            for (int row : order) {
                int bucket = (int) (((key[row] ^ Long.MIN_VALUE) >>> shift) & 255L);
                scratch[counts[bucket]++] = row;
            }
            int[] previous = order;
            order = scratch;
            scratch = previous;
        }
        long comparisons = 0L;
        boolean observed = true;
        for (int row = 0; row < n; row++) {
            int result = Integer.signum(canonical.applyAsInt(row, row));
            comparisons++;
            add(digest, word, result);
            if (result != 0) observed = false;
        }
        int groups = 0;
        int previousMax = -1;
        for (int start = 0; start < n; ) {
            long currentKey = key[order[start]];
            int minimum = order[start];
            int maximum = minimum;
            int end = start + 1;
            while (end < n && key[order[end]] == currentKey) {
                int row = order[end++];
                int minSign = Integer.signum(canonical.applyAsInt(row, minimum));
                comparisons++;
                add(digest, word, minSign);
                if (minSign < 0) minimum = row;
                int maxSign = Integer.signum(canonical.applyAsInt(row, maximum));
                comparisons++;
                add(digest, word, maxSign);
                if (maxSign > 0) maximum = row;
            }
            if (previousMax >= 0) {
                int ascending = Integer.signum(canonical.applyAsInt(previousMax, minimum));
                int descending = Integer.signum(canonical.applyAsInt(minimum, previousMax));
                comparisons += 2;
                add(digest, word, ascending);
                add(digest, word, descending);
                if (ascending != -1 || descending != 1) observed = false;
            }
            add(digest, word, currentKey);
            add(digest, word, start);
            add(digest, word, end);
            add(digest, word, minimum);
            add(digest, word, maximum);
            previousMax = maximum;
            groups++;
            start = end;
        }
        for (int row : order) add(digest, word, row);
        add(digest, word, groups);
        // Detection of observed drift is not a replacement for a verified owner read lease.
        if (!Arrays.equals(keys, key)) {
            throw new IllegalStateException("M3 signal source snapshot changed during observation");
        }
        return new Observation(n, groups, comparisons, observed && groups > 1,
                HexFormat.of().formatHex(digest.digest()));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 unavailable", failure);
        }
    }

    private static void add(MessageDigest digest, ByteBuffer word, long value) {
        word.clear();
        word.putLong(value);
        digest.update(word.array());
    }
}
