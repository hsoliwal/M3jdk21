/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.nlp.search;

import com.m3.compiletime.ast.M3AST;
import com.m3.compiletime.ast.M3File;
import com.m3.compiletime.ast.M3FileSourceFacts;
import com.m3.text.M3StringPiece;
import java.util.Arrays;
import java.util.Objects;
import opennlp.tools.document.Document;

/**
 * One stable source identity shared with analysis, search, and the existing AST occurrence plane.
 * This holder retains the original owners; it neither parses nor creates a second text/AST pool.
 * All ranges are half-open UTF-16 code-unit coordinates in {@link #text()}.
 */
public final class SourceDocument {
    private final String id;
    private final CharSequence text;
    private final String sourceSha256;
    private final String byteSha256;
    private final M3AST ast;
    private volatile OccurrenceIndex occurrences;

    private SourceDocument(String id, CharSequence text, String byteSha256, M3AST ast) {
        this.id = Objects.requireNonNull(id, "id");
        if (id.isEmpty()) throw new IllegalArgumentException("empty source id");
        this.text = Objects.requireNonNull(text, "text");
        if (ast != null && !sameCharacters(text, ast.source())) {
            throw new IllegalArgumentException("AST and text must describe the same source characters");
        }
        this.sourceSha256 = M3FileSourceFacts.sourceSha256ImmutableView(text);
        this.byteSha256 = byteSha256;
        this.ast = ast;
    }

    /** Retain the existing compiler file snapshot and its separate original-byte identity. */
    public static SourceDocument of(M3File file) { return of(file, null); }

    /** Attach an existing AST only after exact source-content comparison. */
    public static SourceDocument of(M3File file, M3AST ast) {
        Objects.requireNonNull(file, "file");
        return new SourceDocument(file.toUri().toString(), file.source(), file.byteSha256(), ast);
    }

    /** Retain the existing immutable M3 text owner without flattening it. */
    public static SourceDocument of(String id, M3StringPiece text) { return of(id, text, null); }

    /** Retain M3 text and the matching original AST, including all occurrence identities. */
    public static SourceDocument of(String id, M3StringPiece text, M3AST ast) {
        return new SourceDocument(id, text, null, ast);
    }

    /** Retain the immutable source snapshot already owned by an existing AST. */
    public static SourceDocument of(String id, M3AST ast) {
        Objects.requireNonNull(ast, "ast");
        return new SourceDocument(id, ast.source(), null, ast);
    }

    /**
     * Explicit ingress for another stable immutable UTF-16 owner, including an M3-backed String.
     * The caller must keep the length and characters immutable for this holder's entire lifetime.
     * Use {@link M3File#of(java.net.URI, CharSequence)} when a mutable input requires a snapshot.
     */
    public static SourceDocument ofImmutableView(String id, CharSequence text) {
        return ofImmutableView(id, text, null);
    }

    /** The immutable-ingress contract also applies to text paired with an existing AST. */
    public static SourceDocument ofImmutableView(String id, CharSequence text, M3AST ast) {
        return new SourceDocument(id, text, null, ast);
    }

    public String id() { return id; }
    public CharSequence text() { return text; }
    public int length() { return text.length(); }
    /** Domain-separated raw UTF-16 digest defined by the existing M3FileSourceFacts owner. */
    public String sourceSha256() { return sourceSha256; }
    /** Original input-byte digest when constructed from a byte-backed M3File; otherwise null. */
    public String byteSha256() { return byteSha256; }
    /** The original AST owner, or null when this is plain text. */
    public M3AST ast() { return ast; }

    /** Create an empty OpenNLP annotation container that retains this exact immutable text owner. */
    public Document openNlpDocument() { return Document.ofImmutableView(text); }

    /** A lightweight range descriptor; only an explicit toString call materializes that range. */
    public CharSequence view(int start, int end) {
        Objects.checkFromToIndex(start, end, text.length());
        return new TextRange(text, start, end);
    }

    /**
     * Return the smallest existing AST occurrence containing this range, or -1 if there is none.
     * Ties select the original lowest occurrence index. Empty ranges use inclusive containment
     * at both endpoints; synthetic occurrences with unavailable source positions are ignored.
     * The lazy primitive index owns no source text, AST nodes, or interned symbols.
     */
    public int astOccurrence(int start, int end) {
        Objects.checkFromToIndex(start, end, text.length());
        if (ast == null) return -1;
        OccurrenceIndex index = occurrences;
        if (index == null) {
            synchronized (this) {
                index = occurrences;
                if (index == null) occurrences = index = new OccurrenceIndex(ast, text.length());
            }
        }
        return index.containing(start, end);
    }

    private static boolean sameCharacters(CharSequence left, CharSequence right) {
        if (left == right) return true;
        if (left.length() != right.length()) return false;
        for (int i = 0; i < left.length(); i++) if (left.charAt(i) != right.charAt(i)) return false;
        return true;
    }

    private record TextRange(CharSequence source, int start, int end) implements CharSequence {
        @Override public int length() { return end - start; }
        @Override public char charAt(int index) {
            return source.charAt(start + Objects.checkIndex(index, length()));
        }
        @Override public CharSequence subSequence(int lower, int upper) {
            Objects.checkFromToIndex(lower, upper, length());
            return new TextRange(source, start + lower, start + upper);
        }
        @Override public String toString() {
            char[] chars = new char[length()];
            for (int i = 0; i < chars.length; i++) chars[i] = source.charAt(start + i);
            return new String(chars);
        }
    }

    /** Balanced interval maximum tree over original occurrence numbers, with no per-node objects. */
    private static final class OccurrenceIndex {
        private final M3AST ast;
        private final long[] order;
        private final int[] maximumEnd;
        private final int capacity;

        OccurrenceIndex(M3AST ast, int length) {
            this.ast = ast;
            long[] entries = new long[ast.occurrenceCount()];
            int count = 0;
            for (int i = 0; i < entries.length; i++) {
                int start = ast.start(i);
                int end = ast.end(i);
                if (start < 0 || end < start) continue;
                if (end > length) throw new IllegalArgumentException("AST occurrence exceeds source bounds");
                entries[count++] = ((long) start << 32) | (i & 0xffff_ffffL);
            }
            order = Arrays.copyOf(entries, count);
            Arrays.sort(order);
            int size = 1;
            while (size < count) size = Math.multiplyExact(size, 2);
            capacity = size;
            maximumEnd = new int[Math.multiplyExact(size, 2)];
            Arrays.fill(maximumEnd, -1);
            for (int i = 0; i < count; i++) maximumEnd[size + i] = ast.end((int) order[i]);
            for (int i = size - 1; i > 0; i--) {
                maximumEnd[i] = Math.max(maximumEnd[2 * i], maximumEnd[2 * i + 1]);
            }
        }

        int containing(int start, int end) { return find(1, 0, capacity, start, end, -1); }

        private int find(int node, int lower, int upper, int start, int end, int winner) {
            if (lower >= order.length || (int) (order[lower] >>> 32) > start
                    || maximumEnd[node] < end) return winner;
            if (upper - lower == 1) {
                int candidate = (int) order[lower];
                int width = ast.end(candidate) - ast.start(candidate);
                if (winner < 0) return candidate;
                int bestWidth = ast.end(winner) - ast.start(winner);
                return width < bestWidth || width == bestWidth && candidate < winner ? candidate : winner;
            }
            int middle = (lower + upper) >>> 1;
            winner = find(2 * node, lower, middle, start, end, winner);
            return find(2 * node + 1, middle, upper, start, end, winner);
        }
    }
}
