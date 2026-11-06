// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK 21 module name.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.text.search;
import java.util.Arrays;
import java.util.Objects;
/** Exact UTF-16 String.indexOf semantics; overlapping hits and bounded output. */
public final class ExactSearch {
    private ExactSearch() { }
    public static int[] positions(String text, String needle, int maxHits) {
        Objects.requireNonNull(text); Objects.requireNonNull(needle);
        if (maxHits < 0) throw new IllegalArgumentException("negative limit");
        int[] buffer = new int[Math.min(maxHits, 16)];
        int used = 0, next = 0;
        while (used < maxHits && next <= text.length()) {
            int at = text.indexOf(needle, next);
            if (at < 0) break;
            if (used == buffer.length) buffer = Arrays.copyOf(buffer, (int) Math.min(maxHits, 2L * used + 1L));
            buffer[used++] = at;
            if (at == text.length()) break;
            next = at + 1;
        }
        return Arrays.copyOf(buffer, used);
    }
    /**
     * Prepare reusable UTF-16 KMP metadata without creating another spelling store.
     * The immutable pattern reference remains owned by String/M3String; only the
     * failure lane is derived. The original positions(String, String, int) path
     * remains unchanged, including its String.indexOf implementation.
     *
     * @throws IllegalArgumentException if the pattern exceeds 1,048,576 code units
     */
    public static Prepared prepare(String needle) {
        return new Prepared(Objects.requireNonNull(needle, "needle"));
    }

    /**
     * Immutable, reusable literal-search plan. Text is borrowed for each call,
     * never retained in a result and never flattened. A supplied CharSequence
     * must remain immutable throughout a call. Coordinates are absolute UTF-16
     * offsets in the supplied source, including intentionally unpaired surrogates.
     * This is not a normalization, code-point or grapheme-search contract.
     */
    public static final class Prepared {
        private static final int MAX_PATTERN_UNITS = 1 << 20;
        private static final int MAX_CORPUS_SOURCES = 4096;
        private static final int MAX_SOURCE_UNITS = 16 << 20;
        private static final long MAX_CORPUS_UNITS = 64L << 20;
        private static final int MAX_CORPUS_HITS = 1_000_000;
        private final String needle;
        private final int[] failure;

        private Prepared(String needle) {
            if (needle.length() > MAX_PATTERN_UNITS)
                throw new IllegalArgumentException("pattern exceeds UTF-16 budget");
            this.needle = needle;
            failure = new int[needle.length()];
            for (int i = 1, matched = 0; i < needle.length(); i++) {
                char unit = needle.charAt(i);
                while (matched > 0 && needle.charAt(matched) != unit)
                    matched = failure[matched - 1];
                if (needle.charAt(matched) == unit) matched++;
                failure[i] = matched;
            }
        }

        /** Number of pattern UTF-16 units; no pattern payload is projected. */
        public int patternLength() { return needle.length(); }

        /** First absolute hit inside [from,to), or -1. Empty patterns hit from. */
        public int firstIndex(CharSequence text, int from, int to) {
            checkRange(text, from, to);
            int[] first = {-1};
            scan(text, from, to, at -> { first[0] = at; return false; });
            return first[0];
        }

        /** Last absolute overlapping hit; empty patterns hit to. */
        public int lastIndex(CharSequence text, int from, int to) {
            checkRange(text, from, to);
            if (needle.isEmpty()) return to;
            int[] last = {-1};
            scan(text, from, to, at -> { last[0] = at; return true; });
            return last[0];
        }

        /** Literal membership of the selected range, with no source projection. */
        public boolean contains(CharSequence text, int from, int to) {
            return firstIndex(text, from, to) >= 0;
        }

        /** Exact overlapping count; long also represents Integer.MAX_VALUE + 1 boundaries. */
        public long count(CharSequence text, int from, int to) {
            checkRange(text, from, to);
            if (needle.isEmpty()) return (long) to - from + 1;
            long[] count = {0};
            scan(text, from, to, at -> { count[0]++; return true; });
            return count[0];
        }

        /** True only when the entire selected range equals the pattern code units. */
        public boolean matches(CharSequence text, int from, int to) {
            checkRange(text, from, to);
            if (to - from != needle.length()) return false;
            for (int i = 0; i < needle.length(); i++)
                if (text.charAt(from + i) != needle.charAt(i)) return false;
            return true;
        }

        /**
         * Exact ordered capped prefix. A cap of zero is complete only when the
         * selected range contains no hit. One additional hit proves truncation;
         * reaching the cap alone never proves it. No source is retained.
         */
        public Matches find(CharSequence text, int from, int to, int maxHits) {
            checkRange(text, from, to);
            if (maxHits < 0) throw new IllegalArgumentException("negative limit");
            HitLane hits = new HitLane(maxHits, false);
            boolean complete = scan(text, from, to, at -> hits.add(0, at));
            return new Matches(Arrays.copyOf(hits.positions, hits.size), complete);
        }

        /**
         * Search ordered independent owners without concatenation or cross-source
         * phantom matches. Each source uses its own zero-based UTF-16 coordinates;
         * source order then position defines the result order. Empty patterns match
         * every boundary of every source. All inputs are admitted before scanning,
         * even when an earlier source will exhaust the cap. Limits are 4096 sources,
         * 16 Mi units per source, 64 Mi units total and 1,000,000 reported hits.
         */
        public CorpusMatches findCorpus(java.util.List<? extends CharSequence> sources,
                                        int maxHits) {
            Objects.requireNonNull(sources, "sources");
            if (sources.size() > MAX_CORPUS_SOURCES)
                throw new IllegalArgumentException("source count exceeds budget");
            if (maxHits < 0 || maxHits > MAX_CORPUS_HITS)
                throw new IllegalArgumentException("corpus hit limit");
            CharSequence[] owners = sources.toArray(CharSequence[]::new);
            if (owners.length > MAX_CORPUS_SOURCES)
                throw new IllegalArgumentException("source count exceeds budget");
            int[] lengths = new int[owners.length];
            long units = 0;
            for (int i = 0; i < owners.length; i++) {
                lengths[i] = Objects.requireNonNull(owners[i], "source").length();
                if (lengths[i] < 0 || lengths[i] > MAX_SOURCE_UNITS)
                    throw new IllegalArgumentException("source exceeds UTF-16 budget");
                units += lengths[i];
                if (units > MAX_CORPUS_UNITS)
                    throw new IllegalArgumentException("corpus exceeds UTF-16 budget");
            }
            HitLane hits = new HitLane(maxHits, true);
            int scanned = 0;
            boolean complete = true;
            for (int i = 0; i < owners.length; i++) {
                final int source = i;
                scanned++;
                if (!scan(owners[i], 0, lengths[i], at -> hits.add(source, at))) {
                    complete = false;
                    break;
                }
            }
            return new CorpusMatches(Arrays.copyOf(hits.sources, hits.size),
                    Arrays.copyOf(hits.positions, hits.size), complete, scanned);
        }

        /**
         * Search consecutive UTF-16 chunks as ONE text, retaining KMP state at
         * seams. This differs deliberately from findCorpus's independent sources.
         * Coordinates are global and overlaps are retained. Empty chunks do not
         * duplicate empty-pattern boundaries; zero chunks denotes empty text.
         * All owners are admitted before scanning. Existing corpus budgets apply.
         */
        public Matches findChunks(java.util.List<? extends CharSequence> chunks, int maxHits) {
            if (maxHits < 0 || maxHits > MAX_CORPUS_HITS)
                throw new IllegalArgumentException("chunk hit limit");
            ChunkInput input = admitChunks(chunks);
            HitLane hits = new HitLane(maxHits, false);
            boolean complete = scanChunks(input, at -> hits.add(0, at));
            return new Matches(Arrays.copyOf(hits.positions, hits.size), complete);
        }

        /** Exact overlapping count over one segmented text; no concatenation. */
        public long countChunks(java.util.List<? extends CharSequence> chunks) {
            ChunkInput input = admitChunks(chunks);
            if (needle.isEmpty()) return (long) input.units + 1;
            long[] count = {0};
            scanChunks(input, at -> { count[0]++; return true; });
            return count[0];
        }

        /** First global UTF-16 hit, or -1. An empty pattern hits boundary zero. */
        public int firstIndexChunks(java.util.List<? extends CharSequence> chunks) {
            ChunkInput input = admitChunks(chunks);
            int[] first = {-1};
            scanChunks(input, at -> { first[0] = at; return false; });
            return first[0];
        }

        /** Last global overlapping hit; an empty pattern hits the total length. */
        public int lastIndexChunks(java.util.List<? extends CharSequence> chunks) {
            ChunkInput input = admitChunks(chunks);
            if (needle.isEmpty()) return input.units;
            int[] last = {-1};
            scanChunks(input, at -> { last[0] = at; return true; });
            return last[0];
        }

        /** Literal membership across chunk seams, not independent-source membership. */
        public boolean containsChunks(java.util.List<? extends CharSequence> chunks) {
            return firstIndexChunks(chunks) >= 0;
        }

        /** Exact whole-text equality across chunks, with no payload projection. */
        public boolean matchesChunks(java.util.List<? extends CharSequence> chunks) {
            ChunkInput input = admitChunks(chunks);
            if (input.units != needle.length()) return false;
            int at = 0;
            for (int part = 0; part < input.owners.length; part++)
                for (int i = 0; i < input.lengths[part]; i++)
                    if (input.owners[part].charAt(i) != needle.charAt(at++)) return false;
            return true;
        }

        /** Invocation-local owner references and primitive geometry; never retained. */
        private record ChunkInput(CharSequence[] owners, int[] lengths, int units) { }

        private static ChunkInput admitChunks(java.util.List<? extends CharSequence> chunks) {
            Objects.requireNonNull(chunks, "chunks");
            if (chunks.size() > MAX_CORPUS_SOURCES)
                throw new IllegalArgumentException("chunk count exceeds budget");
            CharSequence[] owners = chunks.toArray(CharSequence[]::new);
            if (owners.length > MAX_CORPUS_SOURCES)
                throw new IllegalArgumentException("chunk count exceeds budget");
            int[] lengths = new int[owners.length];
            long units = 0;
            for (int i = 0; i < owners.length; i++) {
                int length = Objects.requireNonNull(owners[i], "chunk").length();
                if (length < 0 || length > MAX_SOURCE_UNITS)
                    throw new IllegalArgumentException("chunk exceeds UTF-16 budget");
                lengths[i] = length;
                units += length;
                if (units > MAX_CORPUS_UNITS)
                    throw new IllegalArgumentException("chunks exceed UTF-16 budget");
            }
            return new ChunkInput(owners, lengths, (int) units);
        }

        /** False means an observed hit was rejected, not merely that a cap was reached. */
        private boolean scanChunks(ChunkInput input, MatchSink sink) {
            int size = needle.length();
            if (size == 0) {
                for (int at = 0; ; at++) {
                    if (!sink.accept(at)) return false;
                    if (at == input.units) return true;
                }
            }
            if (size > input.units) return true;
            int at = 0, matched = 0;
            for (int part = 0; part < input.owners.length; part++) {
                CharSequence owner = input.owners[part];
                for (int i = 0; i < input.lengths[part]; i++, at++) {
                    char unit = owner.charAt(i);
                    while (matched > 0 && needle.charAt(matched) != unit)
                        matched = failure[matched - 1];
                    if (needle.charAt(matched) == unit) matched++;
                    if (matched == size) {
                        if (!sink.accept(at - size + 1)) return false;
                        matched = failure[matched - 1];
                    }
                }
            }
            return true;
        }

        /**
         * First/last absolute UTF-16 coordinates and exact overlapping count in
         * one scan. No source or per-hit object is retained. Empty patterns match
         * every boundary, including a virtual Integer.MAX_VALUE EOF boundary.
         */
        public Summary summary(CharSequence text, int from, int to) {
            checkRange(text, from, to);
            if (needle.isEmpty()) return new Summary(from, to, (long) to - from + 1);
            long[] facts = {-1, -1, 0};
            scan(text, from, to, at -> {
                if (facts[2] == 0) facts[0] = at;
                facts[1] = at;
                facts[2]++;
                return true;
            });
            return new Summary((int) facts[0], (int) facts[1], facts[2]);
        }

        /** One summary of a segmented text; reuses the seam-correct chunk scanner. */
        public Summary summaryChunks(java.util.List<? extends CharSequence> chunks) {
            ChunkInput input = admitChunks(chunks);
            if (needle.isEmpty()) return new Summary(0, input.units, (long) input.units + 1);
            long[] facts = {-1, -1, 0};
            scanChunks(input, at -> {
                if (facts[2] == 0) facts[0] = at;
                facts[1] = at;
                facts[2]++;
                return true;
            });
            return new Summary((int) facts[0], (int) facts[1], facts[2]);
        }

        /**
         * Explicit JNI summary over the same UTF-16 range contract. The caller
         * must load the qualified m3textio library; a missing symbol throws
         * UnsatisfiedLinkError, never a silent Java fallback. Native source reads
         * use bounded regions. This does not prove that a particular M3 VM avoids
         * internal materialization while implementing GetStringRegion.
         */
        public Summary summaryNative(String text, int from, int to) {
            checkRange(text, from, to);
            long[] facts = summaryNative0(text, from, to, needle, failure);
            if (facts == null || facts.length != 3)
                throw new IllegalStateException("invalid native summary geometry");
            long maximum = Math.max(0L, (long) to - from - needle.length() + 1);
            if (facts[2] < 0 || facts[2] > maximum
                    || (facts[2] == 0 && (facts[0] != -1 || facts[1] != -1))
                    || (facts[2] > 0 && (facts[0] < from || facts[1] < facts[0]
                        || facts[1] > (long) to - needle.length())))
                throw new IllegalStateException("invalid native summary coordinates");
            return new Summary(Math.toIntExact(facts[0]), Math.toIntExact(facts[1]), facts[2]);
        }


        /**
         * Summarize independent, ordered UTF-16 ranges over one immutable owner.
         * Coordinates are absolute; overlapping/repeated ranges remain separate.
         * All ranges are admitted before source reads. Geometry is detached from
         * caller arrays, but arrays must not change during admission. No source
         * or per-row Summary object is retained in the packed result.
         */
        public RangeSummaries summaryRanges(CharSequence text, int[] starts, int[] ends) {
            RangeInput input = admitRanges(text, starts, ends);
            long[] facts = new long[3 * input.starts.length];
            for (int row = 0; row < input.starts.length; row++) {
                int from = input.starts[row], to = input.ends[row], base = 3 * row;
                if (needle.isEmpty()) {
                    facts[base] = from; facts[base + 1] = to;
                    facts[base + 2] = (long) to - from + 1;
                } else {
                    facts[base] = -1; facts[base + 1] = -1;
                    scan(text, from, to, at -> {
                        if (facts[base + 2] == 0) facts[base] = at;
                        facts[base + 1] = at; facts[base + 2]++;
                        return true;
                    });
                }
            }
            return new RangeSummaries(facts);
        }

        /**
         * Explicit one-call JNI counterpart. Pattern/prefix validation and scratch
         * are shared by every range in the batch; KMP state resets for each range.
         * Caller must load the matching library. Missing native code is an error,
         * never a silent fallback. GetStringRegion is an ABI-copy boundary, not
         * a promise about M3 VM-internal representation or zero-copy behavior.
         */
        public RangeSummaries summaryRangesNative(String text, int[] starts, int[] ends) {
            RangeInput input = admitRanges(text, starts, ends);
            long[] facts = summaryRangesNative0(text, input.starts, input.ends, needle, failure);
            if (facts == null || facts.length != 3 * input.starts.length)
                throw new IllegalStateException("invalid native range geometry");
            for (int row = 0; row < input.starts.length; row++) {
                int base = 3 * row, from = input.starts[row], to = input.ends[row];
                long first = facts[base], last = facts[base + 1], count = facts[base + 2];
                long maximum = Math.max(0L, (long) to - from - needle.length() + 1);
                if (count < 0 || count > maximum
                        || count == 0 && (first != -1 || last != -1)
                        || count > 0 && (first < from || last < first
                            || last > (long) to - needle.length() || count > last - first + 1)
                        || needle.isEmpty() && (first != from || last != to || count != maximum))
                    throw new IllegalStateException("invalid native range facts");
            }
            return new RangeSummaries(facts);
        }

        private record RangeInput(int[] starts, int[] ends) { }

        private static RangeInput admitRanges(CharSequence text, int[] starts, int[] ends) {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(starts, "starts"); Objects.requireNonNull(ends, "ends");
            if (starts.length != ends.length || starts.length > 1_000_000)
                throw new IllegalArgumentException("range geometry/budget");
            int length = text.length();
            if (length < 0) throw new IllegalArgumentException("negative source length");
            int[] a = starts.clone(), b = ends.clone();
            long units = 0;
            for (int row = 0; row < a.length; row++) {
                Objects.checkFromToIndex(a[row], b[row], length);
                units += (long) b[row] - a[row];
                if (units > MAX_CORPUS_UNITS)
                    throw new IllegalArgumentException("selected UTF-16 budget");
            }
            return new RangeInput(a, b);
        }

        private static void checkRange(CharSequence text, int from, int to) {
            Objects.requireNonNull(text, "text");
            Objects.checkFromToIndex(from, to, text.length());
        }

        /** Returns false only when the sink rejects an actually observed hit. */
        private boolean scan(CharSequence text, int from, int to, MatchSink sink) {
            int size = needle.length();
            if (size == 0) {
                // Do not increment past Integer.MAX_VALUE for a virtual EOF range.
                for (int at = from; ; at++) {
                    if (!sink.accept(at)) return false;
                    if (at == to) return true;
                }
            }
            if (size > to - from) return true;
            for (int at = from, matched = 0; at < to; at++) {
                char unit = text.charAt(at);
                while (matched > 0 && needle.charAt(matched) != unit)
                    matched = failure[matched - 1];
                if (needle.charAt(matched) == unit) matched++;
                if (matched == size) {
                    if (!sink.accept(at - size + 1)) return false;
                    matched = failure[matched - 1];
                }
            }
            return true;
        }
    }

    /** Caller-owned immutable coordinate result; array export always makes a copy. */
    public static final class Matches {
        private final int[] positions;
        private final boolean complete;
        private Matches(int[] positions, boolean complete) {
            this.positions = positions;
            this.complete = complete;
        }
        public int size() { return positions.length; }
        public int position(int index) { return positions[Objects.checkIndex(index, positions.length)]; }
        public int[] positions() { return positions.clone(); }
        public boolean complete() { return complete; }
    }

    /** Primitive source-id/offset lanes; no input String, range view or Match object retained. */
    public static final class CorpusMatches {
        private final int[] sources;
        private final int[] positions;
        private final boolean complete;
        private final int scannedSources;
        private CorpusMatches(int[] sources, int[] positions, boolean complete, int scannedSources) {
            this.sources = sources;
            this.positions = positions;
            this.complete = complete;
            this.scannedSources = scannedSources;
        }
        public int size() { return positions.length; }
        public int sourceIndex(int index) { return sources[Objects.checkIndex(index, positions.length)]; }
        public int position(int index) { return positions[Objects.checkIndex(index, positions.length)]; }
        public int[] sourceIndices() { return sources.clone(); }
        public int[] positions() { return positions.clone(); }
        public boolean complete() { return complete; }
        public int scannedSources() { return scannedSources; }
    }

    /** Immutable primitive facts; zero hits are exactly (-1,-1,0). */
    public record Summary(int first, int last, long count) {
        public Summary {
            if (count < 0 || (count == 0 && (first != -1 || last != -1))
                    || (count > 0 && (first < 0 || last < first
                        || count > (long) last - first + 1)))
                throw new IllegalArgumentException("summary geometry");
        }
    }

    private static native long[] summaryNative0(String text, int from, int to,
                                                String needle, int[] failure);


    /**
     * Immutable first/last/count triples in original range order. No text owner,
     * caller array or per-row object is retained. Repeated ranges count again;
     * totals are not a union of hit coordinates. Exported arrays are copies.
     */
    public static final class RangeSummaries {
        private final long[] facts;
        private RangeSummaries(long[] facts) { this.facts = facts; }
        public int size() { return facts.length / 3; }
        public int first(int row) { return (int) facts[3 * Objects.checkIndex(row, size())]; }
        public int last(int row) { return (int) facts[3 * Objects.checkIndex(row, size()) + 1]; }
        public long count(int row) { return facts[3 * Objects.checkIndex(row, size()) + 2]; }
        /** Fresh compatibility projection for one requested row. */
        public Summary summary(int row) { return new Summary(first(row), last(row), count(row)); }
        public long[] triples() { return facts.clone(); }
        public long[] counts() {
            long[] result = new long[size()];
            for (int row = 0; row < result.length; row++) result[row] = facts[3 * row + 2];
            return result;
        }
        /** Exact range-local total, with checked arithmetic. */
        public long totalCount() {
            long total = 0;
            for (int row = 0; row < size(); row++) total = Math.addExact(total, facts[3 * row + 2]);
            return total;
        }
        public long[] matchingMask() { return countMask(1, Long.MAX_VALUE); }
        /** Inclusive count interval; unused high mask bits are always zero. */
        public long[] countMask(long minimum, long maximum) {
            if (minimum < 0 || maximum < minimum)
                throw new IllegalArgumentException("count interval");
            long[] mask = new long[(size() + 63) >>> 6];
            for (int row = 0; row < size(); row++) {
                long count = facts[3 * row + 2];
                if (count >= minimum && count <= maximum) mask[row >>> 6] |= 1L << (row & 63);
            }
            return mask;
        }
    }

    private static native long[] summaryRangesNative0(String text, int[] starts, int[] ends,
                                                      String needle, int[] failure);

    @FunctionalInterface
    private interface MatchSink { boolean accept(int position); }

    /** One bounded primitive builder per invocation, never one structural object per hit. */
    private static final class HitLane {
        private final int limit;
        private int[] positions;
        private int[] sources;
        private int size;
        private HitLane(int limit, boolean corpus) {
            this.limit = limit;
            positions = new int[Math.min(limit, 16)];
            if (corpus) sources = new int[positions.length];
        }
        private boolean add(int source, int position) {
            if (size == limit) return false;
            if (size == positions.length) {
                int capacity = (int) Math.min((long) limit, 2L * size + 1);
                positions = Arrays.copyOf(positions, capacity);
                if (sources != null) sources = Arrays.copyOf(sources, capacity);
            }
            positions[size] = position;
            if (sources != null) sources[size] = source;
            size++;
            return true;
        }
    }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--self-test")) {
            if (!Arrays.equals(positions("aaaa","aa",99),new int[]{0,1,2}))
                throw new AssertionError("overlap");
            if (!Arrays.equals(positions("xy","",99),new int[]{0,1,2}))
                throw new AssertionError("empty needle");
            if (positions("a","a",0).length != 0) throw new AssertionError("bounded");
            System.out.println("M3_JMOD_TEST_PASS text.search");
        } else if (args.length >= 2) {
            System.out.println(Arrays.toString(positions(args[0],args[1],1000)));
        } else { throw new IllegalArgumentException("text and needle required"); }
    }

    /* Prefix-profile Z-window adapted from TheAlgorithms/Java ZAlgorithm.java
     * revision 4f535f583503c5225d3f2d8cb681b0cb0492bc5d; UTF-16 view contract,
     * half-open bounds, self-score and packed derived queries added by Synexia.
     * This adapted section retains the following MIT grant (other original
     * ExactSearch sections retain their existing Apache-2.0 notices):
     * MIT License
     *
     * Copyright (c) 2021 The Algorithms
     *
     * Permission is hereby granted, free of charge, to any person obtaining a copy
     * of this software and associated documentation files (the "Software"), to deal
     * in the Software without restriction, including without limitation the rights
     * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
     * copies of the Software, and to permit persons to whom the Software is
     * furnished to do so, subject to the following conditions:
     *
     * The above copyright notice and this permission notice shall be included in all
     * copies or substantial portions of the Software.
     *
     * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
     * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
     * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
     * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
     * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
     * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
     * SOFTWARE.
     */

    /**
     * Exact prefix-versus-suffix UTF-16 facts over an immutable String/M3 view.
     * At most 1,048,576 units. The input is borrowed for this call, not retained
     * or flattened. NUL and unpaired surrogates are ordinary units. The caller
     * must keep custom views immutable. Work is linear in code-unit comparisons;
     * an expensive charAt implementation adds its own access cost.
     */
    public static PrefixProfile prefixProfile(CharSequence source) {
        int length = prefixLength(source);
        int[] z = new int[length];
        if (length != 0) z[0] = length;
        // [left,right) is the rightmost verified prefix-equal window. Reuse only
        // its proven interior; every extension is checked against actual units.
        for (int i = 1, left = 0, right = 0; i < length; i++) {
            if (i < right) z[i] = Math.min(right - i, z[i - left]);
            while (i + z[i] < length && source.charAt(z[i]) == source.charAt(i + z[i])) z[i]++;
            if (i + z[i] > right) { left = i; right = i + z[i]; }
        }
        return new PrefixProfile(z);
    }

    /**
     * Explicit JNI counterpart. Caller loads the qualified library; a missing
     * symbol fails, never falls back. Native reads use two bounded region pages,
     * not a retained pointer or whole-text scratch. JNI regions are ABI copies,
     * not proof of zero-copy behavior in an untested M3 VM.
     */
    public static PrefixProfile prefixProfileNative(String source) {
        int length = prefixLength(source);
        int[] z = prefixProfileNative0(source);
        if (z == null || z.length != length || length != 0 && z[0] != length)
            throw new IllegalStateException("native prefix geometry");
        for (int i = 0; i < length; i++)
            if (z[i] < 0 || z[i] > length - i)
                throw new IllegalStateException("native prefix bounds");
        return new PrefixProfile(z);
    }

    private static int prefixLength(CharSequence source) {
        int length = Objects.requireNonNull(source, "source").length();
        if (length < 0 || length > (1 << 20))
            throw new IllegalArgumentException("prefix UTF-16 budget");
        return length;
    }

    /**
     * Packed exact prefix facts. z[0] includes the complete nonempty source;
     * the empty suffix at offset length has LCP zero. No text or view retained.
     * Derived queries read only facts. All returned arrays are caller-owned.
     */
    public static final class PrefixProfile {
        private final int[] z;
        private final long similarity;
        private PrefixProfile(int[] z) {
            this.z = z;
            long total = 0;
            for (int value : z) total += value;
            similarity = total;
        }
        public int length() { return z.length; }
        public int suffixLcp(int offset) {
            Objects.checkIndex(offset, z.length + 1);
            return offset == z.length ? 0 : z[offset];
        }
        public int[] lengths() { return z.clone(); }
        /** Sum of LCPs with all suffixes, including the complete source; O(1). */
        public long similarity() { return similarity; }
        /**
         * Overlapping occurrences for every prefix length 0..n, built in O(n).
         * The empty prefix occurs at all n+1 boundaries, including EOF.
         */
        public int[] prefixOccurrenceCounts() {
            int[] counts = new int[z.length + 1];
            for (int value : z) counts[value]++;
            for (int k = z.length - 1; k > 0; k--) counts[k] += counts[k + 1];
            counts[0] = z.length + 1;
            return counts;
        }
        /** Positive proper prefix=suffix lengths, ascending. Empty/full excluded. */
        public int[] properBorderLengths() {
            int count = 0;
            for (int k = 1; k < z.length; k++) if (z[z.length - k] == k) count++;
            int[] borders = new int[count];
            int out = 0;
            for (int k = 1; k < z.length; k++) if (z[z.length - k] == k) borders[out++] = k;
            return borders;
        }
        /** Smallest complete tiling unit; n if not repeated, zero for empty. O(n). */
        public int minimalTilingPeriod() {
            for (int p = 1; p < z.length; p++)
                if (z.length % p == 0 && z[p] == z.length - p) return p;
            return z.length;
        }
        /**
         * Mask over n+1 start boundaries for prefix length 0..n. EOF is selected
         * only for the empty prefix. Unused high bits are zero; O(n) output work.
         */
        public long[] prefixMask(int prefixLength) {
            Objects.checkIndex(prefixLength, z.length + 1);
            long[] mask = new long[(z.length + 1 + 63) >>> 6];
            for (int i = 0; i <= z.length; i++)
                if (prefixLength == 0 || i < z.length && z[i] >= prefixLength)
                    mask[i >>> 6] |= 1L << (i & 63);
            return mask;
        }
    }

    private static native int[] prefixProfileNative0(String source);
}
