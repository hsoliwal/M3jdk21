// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package com.m3.tools.cpu;

import com.m3.tools.text.diff.TextDelta;
import com.sun.source.tree.Tree;
import com.sun.source.util.M3AST;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ProgressMonitor;
import jdk.m3.io.M3File;
import jdk.m3.text.Canonical;

/**
 * Demand-driven CPU observations over ONE already admitted M3File source owner.
 *
 * <p>Consumers borrow its immutable String/M3String and optional detached
 * compiler-owned {@link M3AstTools.Snapshot}. A source is never reopened or
 * reparsed here. No per-character, per-line, or per-AST-node object is retained:
 * line starts, literal hit masks, and kind histograms are primitive arrays
 * created only when asked for. This is a scoped observation cache, not a
 * second text index, parser, DAG executor, string dictionary or AST pool.
 *
 * <p>Use the existing {@link CpuTools.Session} for the 22 selected text/DAG
 * transformations, which can materialize TextFrame. This class provides the
 * direct no-TextFrame path for repeated source-bound queries and source facts.
 */
public final class M3FileCpuFacts {
    private static final int MAX_CACHED_SEARCHES = 8;
    private static final long MAX_CACHED_HIT_BYTES = 1L << 20;
    private final M3File source;
    private final M3AstTools.Snapshot syntax;
    private final String identity;
    private final List<CachedSearch> searches = new ArrayList<>(MAX_CACHED_SEARCHES);
    private volatile int[] lineStarts;
    private volatile int[] kindCounts;

    private M3FileCpuFacts(M3File source, M3AstTools.Snapshot syntax) {
        this.source = Objects.requireNonNull(source, "M3File original source");
        if (syntax != null) {
            M3AstTools.SourceIdentity observed = syntax.source();
            if (!source.uri().equals(observed.uri())
                    || !source.utf16Sha256().equals(observed.utf16Sha256())
                    || source.length() != observed.utf16Length()
                    || observed.rawSha256().isPresent()
                    && !source.rawSha256().equals(observed.rawSha256().get())) {
                throw new IllegalArgumentException("foreign or stale compiler AST source generation");
            }
        }
        this.syntax = syntax;
        this.identity = new Canonical("m3-file-cpu-facts-v1")
                .text(CpuTools.implementationSha256())
                .text(source.uri().toASCIIString())
                .text(source.rawSha256()).text(source.utf16Sha256())
                .number(source.byteLength()).number(source.length())
                .text(syntax == null ? "NO_AST" : syntax.sha256()).finish();
    }

    /** No file read, lexer invocation, AST creation or eager secondary index. */
    public static M3FileCpuFacts of(M3File source) { return new M3FileCpuFacts(source, null); }

    /** Borrow a detached AST already captured from the original compiler callback. */
    public static M3FileCpuFacts of(M3File source, M3AstTools.Snapshot syntax) {
        return new M3FileCpuFacts(source, Objects.requireNonNull(syntax, "original AST snapshot"));
    }

    /**
     * Call DURING the original M3AST.PARSED event; never starts a javac task.
     * M3AstTools owns the existing source/binding verification and projection.
     */
    public static M3FileCpuFacts fromOriginalParse(M3File source, M3AST.Unit unit,
                                                    ProgressMonitor monitor) throws IOException {
        Objects.requireNonNull(unit, "live original compiler event");
        return of(source, M3AstTools.capture(unit, source, monitor));
    }

    public M3File source() { return source; }
    public Optional<M3AstTools.Snapshot> syntax() { return Optional.ofNullable(syntax); }
    /** Stable SHA-256 evidence of owner, original bytes, UTF-16 text, AST receipt and tools. */
    public String sha256() { return identity; }

    /**
     * A bounded LRU of exact literal probes delegated to the EXISTING CpuTools
     * literal search implementation; same probe has one computation and one
     * retained result until evicted. No TextFrame or original file reopening.
     */
    public synchronized Search search(String needle, int maxHits) {
        Objects.requireNonNull(needle, "literal");
        for (int i = 0; i < searches.size(); i++) {
            CachedSearch entry = searches.get(i);
            if (entry.maxHits == maxHits && entry.needle.equals(needle)) {
                if (i != searches.size() - 1) {
                    searches.remove(i);
                    searches.add(entry);
                }
                return entry.result;
            }
        }
        CpuTools.Search original = CpuTools.literal(source, needle, maxHits);
        Search result = new Search(this, original, needle.length(), new Canonical(
                "m3-file-cpu-exact-search-v1").text(identity)
                .text(original.sha256()).number(maxHits).finish());
        // Bound both the query count and retained coordinate bytes, never
        // caller-provided cache hints. Large result arrays are returned without
        // becoming session cache state. Heap pressure can clear the LRU.
        long requestedBytes = Integer.BYTES * (long) original.size();
        if (requestedBytes <= MAX_CACHED_HIT_BYTES && heapPressure() < 0.80d) {
            long retained = 0;
            for (CachedSearch item : searches) retained += item.result.retainedPrimitiveBytes();
            while (!searches.isEmpty() && (searches.size() == MAX_CACHED_SEARCHES
                    || retained + requestedBytes > MAX_CACHED_HIT_BYTES)) {
                retained -= searches.remove(0).result.retainedPrimitiveBytes();
            }
            searches.add(new CachedSearch(needle, maxHits, result));
        } else {
            searches.clear();
        }
        return result;
    }

    /** Return a defensive copy of the lazily retained UTF-16 line-start lane. */
    public int[] lineStarts() { return starts().clone(); }
    public int lineCount() { return starts().length; }

    /** 0-based line for a UTF-16 code-unit coordinate, including EOF. */
    public int lineOf(int utf16Offset) {
        if (utf16Offset < 0 || utf16Offset > source.length()) {
            throw new IndexOutOfBoundsException("UTF-16 source coordinate");
        }
        int[] starts = starts();
        int index = Arrays.binarySearch(starts, utf16Offset);
        return index >= 0 ? index : -index - 2;
    }

    /** Number of original compiler AST occurrences with one exact JDK Tree.Kind. */
    public int kindCount(Tree.Kind kind) {
        Objects.requireNonNull(kind, "AST kind");
        if (syntax == null) throw new IllegalStateException("original AST was not supplied");
        int[] counts = kindCounts;
        if (counts == null) {
            synchronized (this) {
                counts = kindCounts;
                if (counts == null) {
                    counts = new int[Tree.Kind.values().length];
                    for (int i = 0; i < syntax.structureCount(); i++) {
                        counts[syntax.structure(i).kind().ordinal()]++;
                    }
                    kindCounts = counts;
                }
            }
        }
        return counts[kind.ordinal()];
    }

    /** Original M3 AST facts remain borrowed; this does not parse or copy AST. */
    public int visitedAstNodes() {
        if (syntax == null) throw new IllegalStateException("original AST was not supplied");
        return syntax.visitedNodes();
    }

    /** Existing donor's minimal source delta (not a semantic diff). */
    public TextDelta.Replacement changedSpan(M3File later) {
        Objects.requireNonNull(later, "later M3File source");
        return TextDelta.changedSpan(source.text(), later.text());
    }

    /** Release only optional derivative arrays/results, never the borrowed source/AST. */
    public synchronized void discardDerivedFacts() {
        lineStarts = null;
        kindCounts = null;
        searches.clear();
    }

    /** Counts only retained primitive sidecars; excludes already-owned source and AST. */
    public synchronized long retainedPrimitiveBytes() {
        int[] lines = lineStarts;
        int[] kinds = kindCounts;
        long bytes = Integer.BYTES * ((lines == null ? 0L : lines.length)
                + (kinds == null ? 0L : kinds.length));
        for (CachedSearch entry : searches) {
            bytes += entry.result.retainedPrimitiveBytes();
        }
        return bytes;
    }

    private int[] starts() {
        int[] found = lineStarts;
        if (found == null) {
            synchronized (this) {
                found = lineStarts;
                if (found == null) {
                    int[] buffer = new int[16];
                    int used = 1;
                    buffer[0] = 0;
                    for (int i = 0; i < source.length(); i++) {
                        char ch = source.charAt(i);
                        if (ch == '\r' || ch == '\n') {
                            if (ch == '\r' && i + 1 < source.length()
                                    && source.charAt(i + 1) == '\n') i++;
                            if (used == buffer.length) buffer = Arrays.copyOf(buffer,
                                    Math.min(source.length() + 1, buffer.length * 2));
                            buffer[used++] = i + 1;
                        }
                    }
                    found = Arrays.copyOf(buffer, used);
                    lineStarts = found;
                }
            }
        }
        return found;
    }

    private static double heapPressure() {
        Runtime memory = Runtime.getRuntime();
        long maximum = memory.maxMemory();
        if (maximum <= 0) return 1.0;
        long used = memory.totalMemory() - memory.freeMemory();
        return (double) used / maximum;
    }

    private record CachedSearch(String needle, int maxHits, Search result) { }

    /** Per-probe retained original search, optionally a lazily computed line mask. */
    public static final class Search {
        private final M3FileCpuFacts owner;
        private final CpuTools.Search original;
        private final int needleLength;
        private final String sha256;
        private volatile long[] lineMask;

        private Search(M3FileCpuFacts owner, CpuTools.Search original,
                       int needleLength, String sha256) {
            this.owner = owner;
            this.original = original;
            this.needleLength = needleLength;
            this.sha256 = sha256;
        }
        /** Source-bound and tool-bound deterministic receipt, not a source hash. */
        public String sha256() { return sha256; }
        public boolean complete() { return original.complete(); }
        public int size() { return original.size(); }
        public int[] positions() { return original.positions(); }
        public long[] lineMask() {
            long[] computed = lineMask;
            if (computed == null) {
                synchronized (this) {
                    computed = lineMask;
                    if (computed == null) {
                        long[] words = new long[(owner.lineCount() + 63) >>> 6];
                        // Existing ExactSearch emits monotonically increasing hit
                        // positions. Coalesce overlapping line intervals so no
                        // source line is marked more than once (O(hits + lines)).
                        int coveredThrough = -1;
                        for (int start : original.positions()) {
                            int from = owner.lineOf(start);
                            int to = needleLength == 0 ? from
                                    : owner.lineOf(Math.addExact(start, needleLength - 1));
                            for (int line = Math.max(from, coveredThrough + 1);
                                    line <= to; line++) {
                                words[line >>> 6] |= 1L << (line & 63);
                            }
                            coveredThrough = Math.max(coveredThrough, to);
                        }
                        lineMask = computed = words;
                    }
                }
            }
            return computed.clone();
        }
        private long retainedPrimitiveBytes() {
            long[] mask = lineMask;
            // Original CpuTools.Search retains its source-hit lane; size is exact.
            return Integer.BYTES * (long) original.size()
                    + (mask == null ? 0L : Long.BYTES * (long) mask.length);
        }
    }
}
