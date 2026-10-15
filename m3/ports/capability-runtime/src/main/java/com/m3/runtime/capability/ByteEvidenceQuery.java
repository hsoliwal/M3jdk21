// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Immutable, CPU-first composition of the existing source, byte-search and line owners.
 *
 * <p>No second parser, index, interner or native ABI is introduced. A query holds an
 * immutable source range, the existing KMP plan and a single byte-line directory.
 * Results retain one primitive offset array, not per-hit byte[] or String payloads.
 * The receipt binds the exact source/range/pattern, limit and ordered hit offsets.
 * All line/column coordinates are zero-based <em>bytes</em>, never UTF-16 units.</p>
 */
public final class ByteEvidenceQuery {
    private static final String DOMAIN = "M3_BYTE_EVIDENCE_QUERY_V1";

    private final SourceArtifact.Span source;
    private final BytePatternSearch pattern;
    private final ByteLineIndex lines;
    private final String sourceRoot;
    private final String rangeRoot;
    private final String patternRoot;

    private ByteEvidenceQuery(SourceArtifact.Span source, BytePatternSearch pattern,
                              ByteLineIndex lines, String patternRoot) {
        this.source = source;
        this.pattern = pattern;
        this.lines = lines;
        this.sourceRoot = source.source().root();
        this.rangeRoot = source.sha256();
        this.patternRoot = patternRoot;
    }

    /**
     * Snapshot an immutable needle, then prepare exactly the existing owners.
     * This prepares the Java line directory even if a later search uses JNI.
     */
    public static ByteEvidenceQuery compile(SourceArtifact.Span source, byte[] needle) {
        SourceArtifact.Span checked = Objects.requireNonNull(source, "source");
        byte[] snapshot = Objects.requireNonNull(needle, "needle").clone();
        BytePatternSearch pattern = BytePatternSearch.compile(snapshot);
        ByteLineIndex lines = ByteLineIndex.javaIndex(checked);
        return new ByteEvidenceQuery(checked, pattern, lines,
                SourceArtifact.digest(snapshot, 0, snapshot.length));
    }

    public String sourceId() { return source.source().id(); }
    public String sourceRoot() { return sourceRoot; }
    public String rangeRoot() { return rangeRoot; }
    public String patternRoot() { return patternRoot; }
    public int rangeLength() { return source.length(); }
    public int patternLength() { return pattern.patternLength(); }
    public int lineCount() { return lines.lineCount(); }
    public long retainedPrimitiveBytes() {
        return Math.addExact(lines.retainedPrimitiveBytes(), pattern.retainedPrimitiveBytes());
    }

    /** CPU semantic authority; bounded KMP returns exact, overlapping source-byte hits. */
    public Results execute(int maxMatches) {
        return result(pattern.findAll(source, maxMatches), maxMatches);
    }

    /**
     * Explicit JNI acceleration, using the already pinned byte-search library.
     * Does not install, relink, or retain a native address. The independently
     * executable Java proof can be requested using {@link #verify(Results)}.
     */
    public Results executeNative(int maxMatches) {
        return result(pattern.findAllNative(source, maxMatches), maxMatches);
    }

    /** Recompute the exact witness using the Java oracle, including its receipt. */
    public void verify(Results candidate) {
        Results received = Objects.requireNonNull(candidate, "candidate");
        if (received.owner != this) {
            throw new IllegalArgumentException("different immutable evidence query owner");
        }
        Results expected = execute(received.maxMatches);
        if (!Arrays.equals(expected.offsets, received.offsets)
                || !expected.receipt.equals(received.receipt)) {
            throw new IllegalArgumentException("source-backed evidence receipt differs");
        }
    }

    private Results result(int[] starts, int maxMatches) {
        return new Results(this, starts, maxMatches);
    }

    /**
     * Compact immutable result: one ordered offset directory, no per-hit payloads.
     * Source and pattern identity are supplied once by the query owner.
     */
    public static final class Results {
        private final ByteEvidenceQuery owner;
        private final int[] offsets;
        private final int maxMatches;
        private final String receipt;

        private Results(ByteEvidenceQuery owner, int[] starts, int maxMatches) {
            this.owner = owner;
            this.offsets = Objects.requireNonNull(starts, "starts");
            this.maxMatches = maxMatches;
            if (maxMatches < 0 || maxMatches > BytePatternSearch.MAX_MATCHES
                    || offsets.length > maxMatches) {
                throw new IllegalArgumentException("evidence query result cap");
            }
            int previous = -1;
            for (int i = 0; i < offsets.length; i++) {
                int at = offsets[i];
                if ((i & 4095) == 0 && Thread.currentThread().isInterrupted()) {
                    throw new CancellationException("evidence query interrupted");
                }
                if (at <= previous || at < 0
                        || at > owner.source.length() - owner.pattern.patternLength()) {
                    throw new IllegalArgumentException("noncanonical evidence hit offset");
                }
                previous = at;
            }
            this.receipt = ByteEvidenceQuery.receipt(owner, maxMatches, offsets);
        }

        public int count() { return offsets.length; }
        public int maxMatches() { return maxMatches; }
        public String sourceId() { return owner.sourceId(); }
        public String sourceRoot() { return owner.sourceRoot; }
        public String rangeRoot() { return owner.rangeRoot; }
        public String patternRoot() { return owner.patternRoot; }
        public String receipt() { return receipt; }
        public long retainedPrimitiveBytes() { return (long) offsets.length * Integer.BYTES; }

        /** Hit offset in the selected byte span. */
        public int start(int ordinal) {
            return offsets[Objects.checkIndex(ordinal, offsets.length)];
        }

        public int endExclusive(int ordinal) {
            return Math.addExact(start(ordinal), owner.pattern.patternLength());
        }

        /** Absolute offset into the original immutable source byte array. */
        public int absoluteStart(int ordinal) {
            return Math.addExact(owner.source.start(), start(ordinal));
        }

        public int absoluteEndExclusive(int ordinal) {
            return Math.addExact(owner.source.start(), endExclusive(ordinal));
        }

        /** Inclusive first/last line containing hit bytes (zero based). */
        public int firstLine(int ordinal) {
            return owner.lines.lineAtOffset(start(ordinal));
        }

        public int lastLine(int ordinal) {
            return owner.lines.lineAtOffset(endExclusive(ordinal) - 1);
        }

        /** First line-relative byte offset, never a character offset. */
        public int firstColumnByte(int ordinal) {
            int start = start(ordinal);
            return start - owner.lines.startOffset(owner.lines.lineAtOffset(start));
        }

        /** Exclusive byte coordinate relative to the last occupied line. */
        public int lastColumnByteExclusive(int ordinal) {
            int end = endExclusive(ordinal);
            return end - owner.lines.startOffset(owner.lines.lineAtOffset(end - 1));
        }

        /** Zero-copy immutable range over original source bytes; the consumer may explicitly copy. */
        public SourceArtifact.Span evidence(int ordinal) {
            return owner.source.slice(start(ordinal), owner.pattern.patternLength());
        }
    }

    private static String receipt(ByteEvidenceQuery query, int limit, int[] offsets) {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            // Encode Java UTF-16 units explicitly, including unpaired surrogates in IDs.
            text(hash, DOMAIN);
            text(hash, query.sourceId());
            text(hash, query.sourceRoot);
            text(hash, query.rangeRoot);
            text(hash, query.patternRoot);
            number(hash, query.source.start());
            number(hash, query.source.length());
            number(hash, query.pattern.patternLength());
            number(hash, limit);
            number(hash, offsets.length);
            for (int i = 0; i < offsets.length; i++) {
                if ((i & 4095) == 0 && Thread.currentThread().isInterrupted()) {
                    throw new CancellationException("evidence receipt interrupted");
                }
                number(hash, offsets[i]);
            }
            return HexFormat.of().formatHex(hash.digest());
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException("SHA-256 unavailable", missing);
        }
    }

    private static void text(MessageDigest hash, String value) {
        number(hash, value.length());
        for (int i = 0; i < value.length(); i++) {
            char unit = value.charAt(i);
            hash.update((byte) unit);
            hash.update((byte) (unit >>> 8));
        }
    }

    private static void number(MessageDigest hash, long value) {
        for (int i = 0; i < Long.BYTES; i++) hash.update((byte) (value >>> (i * 8)));
    }
}
