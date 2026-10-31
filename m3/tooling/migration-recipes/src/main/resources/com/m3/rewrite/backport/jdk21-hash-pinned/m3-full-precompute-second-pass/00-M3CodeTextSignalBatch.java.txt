// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/** Batch UTF-16 lexical signal extractor for code-looking and regex-looking text. */
public interface M3CodeTextSignalBatch {
    record Limits(long maxTransferBytes, long maxUtf16Units, int maxRows) {
        public static final Limits DEFAULT =
                new Limits(64L << 20, 16_000_000L, 1_000_000);

        public Limits {
            if (maxTransferBytes < 0 || maxUtf16Units < 0 || maxRows < 1) {
                throw new IllegalArgumentException("invalid code-text signal limits");
            }
        }
    }

    String name();

    boolean accelerated();

    long[] analyze(
            char[] units,
            int[] offsets,
            int[] lengths,
            Limits limits,
            M3Progress monitor);
}
