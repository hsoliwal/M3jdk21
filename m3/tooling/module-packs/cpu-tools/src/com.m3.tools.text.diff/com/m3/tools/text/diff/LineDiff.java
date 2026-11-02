// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.text.diff;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Lossless line-indexed shortest insert/delete script over exact UTF-16 text.
 * Every line token includes its actual CR, LF, CRLF or absent terminator; a
 * change to an ending is therefore an edit, not invisible normalization.
 *
 * <p>One primitive 16-bit ID is assigned to each distinct line, with exact
 * source-region comparisons after a 64-bit hash candidate. No retained line
 * substring or second text corpus is created. The existing {@link BoundedDiff}
 * engine operates on these IDs; its edit budget now counts <em>lines</em>, not
 * UTF-16 units. An over-budget patch is explicitly incomplete. This is a
 * bounded SDK atom, not the GNU diff output format or an unbounded diff tool.
 */
public final class LineDiff {
    public static final int MAX_SOURCE_UTF16 = 16 * 1024 * 1024;
    public static final int MAX_LINES = 100_000;
    public static final int MAX_UNIQUE_LINES = 65_536;
    public static final int MAX_EDITS = BoundedDiff.MAX_EDITS;
    private static final Runnable NOOP = () -> { };

    private LineDiff() { }

    /** Half-open source line and UTF-16 coordinates and exact replacement text. */
    public record LineChange(int startLine, int endLine, int startUtf16, int endUtf16,
                             int insertedLines, String inserted) {
        public LineChange {
            if (startLine < 0 || endLine < startLine || startUtf16 < 0 || endUtf16 < startUtf16
                    || insertedLines < 0 || (startLine == endLine && insertedLines == 0)) {
                throw new IllegalArgumentException("invalid line change");
            }
            Objects.requireNonNull(inserted, "inserted");
            if ((insertedLines == 0) != inserted.isEmpty()) {
                throw new IllegalArgumentException("line insertion/text mismatch");
            }
        }
    }

    /** Complete immutable line patch, or a source/target-sealed budget miss. */
    public static final class Patch {
        private final int sourceLength;
        private final int targetLength;
        private final int sourceLines;
        private final int targetLines;
        private final int distance;
        private final String sourceSha256;
        private final String targetSha256;
        private final List<LineChange> changes;

        private Patch(int sourceLength, int targetLength, int sourceLines, int targetLines,
                      int distance, String sourceSha256, String targetSha256,
                      List<LineChange> changes) {
            this.sourceLength = sourceLength;
            this.targetLength = targetLength;
            this.sourceLines = sourceLines;
            this.targetLines = targetLines;
            this.distance = distance;
            this.sourceSha256 = sourceSha256;
            this.targetSha256 = targetSha256;
            this.changes = List.copyOf(changes);
        }

        public boolean complete() { return distance >= 0; }
        public int distance() { return distance; }
        public int sourceLength() { return sourceLength; }
        public int targetLength() { return targetLength; }
        public int sourceLines() { return sourceLines; }
        public int targetLines() { return targetLines; }
        public String sourceSha256() { return sourceSha256; }
        public String targetSha256() { return targetSha256; }
        public List<LineChange> changes() { return changes; }

        /** Apply only to the identical UTF-16 source; validate the entire result. */
        public String apply(String source) {
            Objects.requireNonNull(source, "source");
            if (!complete()) throw new IllegalStateException("line edit budget exceeded");
            if (source.length() != sourceLength || !utf16Sha256(source, NOOP).equals(sourceSha256)) {
                throw new IllegalArgumentException("line patch source identity mismatch");
            }
            StringBuilder output = new StringBuilder(targetLength);
            int offset = 0;
            for (LineChange change : changes) {
                if (change.startUtf16() < offset || change.endUtf16() > sourceLength) {
                    throw new IllegalStateException("unordered line patch");
                }
                output.append(source, offset, change.startUtf16()).append(change.inserted());
                offset = change.endUtf16();
            }
            output.append(source, offset, sourceLength);
            String result = output.toString();
            if (result.length() != targetLength || !utf16Sha256(result, NOOP).equals(targetSha256)) {
                throw new IllegalStateException("line patch target identity mismatch");
            }
            return result;
        }
    }

    public static Patch diff(String source, String target, int maxEdits) {
        return diff(source, target, maxEdits, NOOP);
    }

    /** Reuses the source/target String owners and checks cancellation cooperatively. */
    public static Patch diff(String source, String target, int maxEdits, Runnable checkpoint) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(checkpoint, "checkpoint");
        if (maxEdits < 0 || maxEdits > MAX_EDITS) {
            throw new IllegalArgumentException("line edit budget");
        }
        if (source.length() > MAX_SOURCE_UTF16 || target.length() > MAX_SOURCE_UTF16) {
            throw new IllegalArgumentException("line source UTF-16 budget");
        }
        checkpoint.run();
        String beforeSha = utf16Sha256(source, checkpoint);
        String afterSha = utf16Sha256(target, checkpoint);
        Dictionary dictionary = new Dictionary(source, target);
        Indexed before = index(source, dictionary, checkpoint);
        Indexed after = index(target, dictionary, checkpoint);
        BoundedDiff.Patch indexed = BoundedDiff.diff(before.ids(), after.ids(), maxEdits, checkpoint);
        if (!indexed.complete()) {
            return new Patch(source.length(), target.length(), before.lineCount(), after.lineCount(),
                    -1, beforeSha, afterSha, List.of());
        }
        List<LineChange> changes = new ArrayList<>(indexed.changes().size());
        for (BoundedDiff.Change change : indexed.changes()) {
            checkpoint.run();
            StringBuilder inserted = new StringBuilder();
            for (int i = 0; i < change.inserted().length(); i++) {
                if ((i & 1023) == 0) checkpoint.run();
                dictionary.append(inserted, change.inserted().charAt(i));
            }
            changes.add(new LineChange(change.start(), change.end(),
                    before.boundaries()[change.start()], before.boundaries()[change.end()],
                    change.inserted().length(), inserted.toString()));
        }
        checkpoint.run();
        return new Patch(source.length(), target.length(), before.lineCount(), after.lineCount(),
                indexed.distance(), beforeSha, afterSha, changes);
    }

    /** Tokenizes without extracting line Strings; boundaries include exact terminators. */
    private static Indexed index(String text, Dictionary dictionary, Runnable checkpoint) {
        char[] tokens = new char[64];
        int[] boundaries = new int[65];
        int count = 0;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if ((i & 8191) == 0) checkpoint.run();
            char unit = text.charAt(i);
            if (unit != '\r' && unit != '\n') continue;
            int end = i + 1;
            if (unit == '\r' && end < text.length() && text.charAt(end) == '\n') end++;
            if (count == MAX_LINES) throw new IllegalArgumentException("line count budget");
            if (count == tokens.length) {
                tokens = Arrays.copyOf(tokens, Math.min(MAX_LINES, tokens.length * 2));
                boundaries = Arrays.copyOf(boundaries, tokens.length + 1);
            }
            tokens[count] = dictionary.id(text, start, end, checkpoint);
            boundaries[++count] = end;
            start = end;
            i = end - 1;
        }
        if (start < text.length()) {
            if (count == MAX_LINES) throw new IllegalArgumentException("line count budget");
            if (count == tokens.length) {
                tokens = Arrays.copyOf(tokens, Math.min(MAX_LINES, tokens.length * 2));
                boundaries = Arrays.copyOf(boundaries, tokens.length + 1);
            }
            tokens[count] = dictionary.id(text, start, text.length(), checkpoint);
            boundaries[++count] = text.length();
        }
        checkpoint.run();
        return new Indexed(new String(tokens, 0, count), Arrays.copyOf(boundaries, count + 1));
    }

    private record Indexed(String ids, int[] boundaries) {
        int lineCount() { return ids.length(); }
    }

    /** Open-addressed hash-candidate table; a hash collision is never accepted as equality. */
    private static final class Dictionary {
        private static final int TABLE_SIZE = 1 << 17;
        private final String source;
        private final String target;
        private final int[] table = new int[TABLE_SIZE];
        private final long[] hashes = new long[MAX_UNIQUE_LINES];
        private final int[] offsets = new int[MAX_UNIQUE_LINES];
        private final int[] lengths = new int[MAX_UNIQUE_LINES];
        private final boolean[] fromTarget = new boolean[MAX_UNIQUE_LINES];
        private int size;

        Dictionary(String source, String target) {
            this.source = source;
            this.target = target;
        }

        char id(String text, int start, int end, Runnable checkpoint) {
            long hash = 0xcbf29ce484222325L;
            for (int i = start; i < end; i++) {
                if (((i - start) & 8191) == 0) checkpoint.run();
                hash = (hash ^ text.charAt(i)) * 0x100000001b3L;
            }
            int length = end - start;
            hash = (hash ^ length) * 0x100000001b3L;
            int slot = (int) (hash ^ (hash >>> 32)) & (TABLE_SIZE - 1);
            while (table[slot] != 0) {
                int candidate = table[slot] - 1;
                if (hashes[candidate] == hash && lengths[candidate] == length) {
                    String owner = fromTarget[candidate] ? target : source;
                    if (text.regionMatches(start, owner, offsets[candidate], length)) {
                        return (char) candidate;
                    }
                }
                slot = (slot + 1) & (TABLE_SIZE - 1);
                if ((slot & 1023) == 0) checkpoint.run();
            }
            if (size == MAX_UNIQUE_LINES) throw new IllegalArgumentException("line dictionary budget");
            int assigned = size++;
            hashes[assigned] = hash;
            offsets[assigned] = start;
            lengths[assigned] = length;
            fromTarget[assigned] = text == target && source != target;
            table[slot] = assigned + 1;
            return (char) assigned;
        }

        void append(StringBuilder output, char id) {
            int ordinal = id;
            String owner = fromTarget[ordinal] ? target : source;
            output.append(owner, offsets[ordinal], offsets[ordinal] + lengths[ordinal]);
        }
    }

    /** Domain-separated, length-framed UTF-16 SHA-256, including unmatched surrogates. */
    private static String utf16Sha256(String text, Runnable checkpoint) {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
        digest.update("m3-line-diff-utf16-v1".getBytes(StandardCharsets.US_ASCII));
        int length = text.length();
        digest.update((byte) (length >>> 24));
        digest.update((byte) (length >>> 16));
        digest.update((byte) (length >>> 8));
        digest.update((byte) length);
        for (int i = 0; i < length; i++) {
            if ((i & 8191) == 0) checkpoint.run();
            char unit = text.charAt(i);
            digest.update((byte) (unit >>> 8));
            digest.update((byte) unit);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
