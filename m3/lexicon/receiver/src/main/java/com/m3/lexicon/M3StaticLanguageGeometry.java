// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Frozen language geometry keyed directly by M3LEX001 canonical word IDs (CWID 1..V).
 *
 * <p>Runtime state is primitive structure-of-arrays only. A word has no retained metadata object:
 * frequency, broad classes, morphology, concept/subject CSR and pair geometry are flat lanes.
 * Build-time collections are discarded by {@link Builder#freeze()}.</p>
 */
public final class M3StaticLanguageGeometry {
    public static final int Q20_ONE = 1 << 20;

    private final int wordCount;
    private final long[] frequency;
    private final long[] classMask;
    private final int[] morphologyMask;
    private final int[] conceptOffsets;
    private final long[] concepts;
    private final int[] subjectOffsets;
    private final long[] subjects;
    private final int[] pairOffsets;
    private final int[] pairTargets;
    private final long[] pairCounts;
    private final int[] pairAssociationQ20;
    private final long totalFrequency;
    private final long maximumFrequency;
    private final long minimumPositiveFrequency;
    private final int positiveFrequencyWords;

    private M3StaticLanguageGeometry(
            int wordCount,
            long[] frequency,
            long[] classMask,
            int[] morphologyMask,
            int[] conceptOffsets,
            long[] concepts,
            int[] subjectOffsets,
            long[] subjects,
            int[] pairOffsets,
            int[] pairTargets,
            long[] pairCounts,
            int[] pairAssociationQ20) {
        this.wordCount = wordCount;
        this.frequency = frequency;
        this.classMask = classMask;
        this.morphologyMask = morphologyMask;
        this.conceptOffsets = conceptOffsets;
        this.concepts = concepts;
        this.subjectOffsets = subjectOffsets;
        this.subjects = subjects;
        this.pairOffsets = pairOffsets;
        this.pairTargets = pairTargets;
        this.pairCounts = pairCounts;
        this.pairAssociationQ20 = pairAssociationQ20;

        long total = 0L;
        long maximum = 0L;
        long minimum = Long.MAX_VALUE;
        int positive = 0;
        for (int cwid = 1; cwid <= wordCount; cwid++) {
            long value = frequency[cwid];
            total = saturatedAdd(total, value);
            if (value > 0L) {
                positive++;
                maximum = Math.max(maximum, value);
                minimum = Math.min(minimum, value);
            }
        }
        totalFrequency = total;
        maximumFrequency = maximum;
        minimumPositiveFrequency = positive == 0 ? 0L : minimum;
        positiveFrequencyWords = positive;
    }

    public static Builder builder(int wordCount) {
        return new Builder(wordCount);
    }

    public int wordCount() { return wordCount; }
    public long totalFrequency() { return totalFrequency; }
    public long maximumFrequency() { return maximumFrequency; }
    public long minimumPositiveFrequency() { return minimumPositiveFrequency; }
    public int positiveFrequencyWords() { return positiveFrequencyWords; }

    public long frequency(int cwid) {
        return frequency[checkWord(cwid)];
    }

    public long classMask(int cwid) {
        return classMask[checkWord(cwid)];
    }

    public int morphologyMask(int cwid) {
        return morphologyMask[checkWord(cwid)];
    }

    public boolean hasClass(int cwid, long requiredMask) {
        return (classMask(cwid) & requiredMask) == requiredMask;
    }

    public boolean hasMorphology(int cwid, int requiredMask) {
        return (morphologyMask(cwid) & requiredMask) == requiredMask;
    }

    public int conceptCount(int cwid) {
        int row = checkWord(cwid);
        return conceptOffsets[row + 1] - conceptOffsets[row];
    }

    public long conceptAt(int cwid, int ordinal) {
        int row = checkWord(cwid);
        int start = conceptOffsets[row];
        return concepts[start + Objects.checkIndex(ordinal, conceptOffsets[row + 1] - start)];
    }

    public int subjectCount(int cwid) {
        int row = checkWord(cwid);
        return subjectOffsets[row + 1] - subjectOffsets[row];
    }

    public long subjectAt(int cwid, int ordinal) {
        int row = checkWord(cwid);
        int start = subjectOffsets[row];
        return subjects[start + Objects.checkIndex(ordinal, subjectOffsets[row + 1] - start)];
    }

    /** Number of exact concept coordinates shared by two words. */
    public int conceptOverlap(int leftCwid, int rightCwid) {
        int left = checkWord(leftCwid);
        int right = checkWord(rightCwid);
        return overlap(
                concepts,
                conceptOffsets[left], conceptOffsets[left + 1],
                conceptOffsets[right], conceptOffsets[right + 1]);
    }

    /** Number of exact subject/domain coordinates shared by two words. */
    public int subjectOverlap(int leftCwid, int rightCwid) {
        int left = checkWord(leftCwid);
        int right = checkWord(rightCwid);
        return overlap(
                subjects,
                subjectOffsets[left], subjectOffsets[left + 1],
                subjectOffsets[right], subjectOffsets[right + 1]);
    }

    public int classOverlap(int leftCwid, int rightCwid) {
        return Long.bitCount(classMask(leftCwid) & classMask(rightCwid));
    }

    public long pairCount(int leftCwid, int rightCwid) {
        int row = pairRow(leftCwid, rightCwid);
        return row < 0 ? 0L : pairCounts[row];
    }

    /** Symmetric/consumer-defined precomputed association in unsigned Q20. */
    public int pairAssociationQ20(int leftCwid, int rightCwid) {
        int row = pairRow(leftCwid, rightCwid);
        return row < 0 ? 0 : pairAssociationQ20[row];
    }

    public FrequencyBand defaultLuhnFrequencyBand() {
        if (positiveFrequencyWords == 0) return new FrequencyBand(0L, 0L);
        long mean = Math.max(1L, totalFrequency / positiveFrequencyWords);
        long lower = minimumPositiveFrequency;
        if (maximumFrequency >= 16L && lower == 1L) lower = 2L;
        long upper = Math.min(maximumFrequency, Math.max(lower, saturatedMultiply(mean, 8L)));
        if (positiveFrequencyWords > 1 && upper >= maximumFrequency && maximumFrequency > lower) {
            upper = maximumFrequency - 1L;
        }
        return new FrequencyBand(lower, Math.max(lower, upper));
    }

    public long primitiveBytes() {
        return 8L * (frequency.length + classMask.length + concepts.length + subjects.length
                        + pairCounts.length)
                + 4L * (morphologyMask.length + conceptOffsets.length + subjectOffsets.length
                        + pairOffsets.length + pairTargets.length + pairAssociationQ20.length);
    }

    private int pairRow(int leftCwid, int rightCwid) {
        int left = checkWord(leftCwid);
        checkWord(rightCwid);
        int low = pairOffsets[left];
        int high = pairOffsets[left + 1] - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int target = pairTargets[middle];
            if (target == rightCwid) return middle;
            if (target < rightCwid) low = middle + 1;
            else high = middle - 1;
        }
        return -1;
    }

    private int checkWord(int cwid) {
        if (cwid <= 0 || cwid > wordCount) throw new IndexOutOfBoundsException("CWID " + cwid);
        return cwid;
    }

    private static int overlap(long[] lane, int a, int aEnd, int b, int bEnd) {
        int count = 0;
        while (a < aEnd && b < bEnd) {
            long left = lane[a];
            long right = lane[b];
            int order = Long.compareUnsigned(left, right);
            if (order == 0) {
                count++;
                a++;
                b++;
            } else if (order < 0) a++;
            else b++;
        }
        return count;
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    private static long saturatedMultiply(long left, long right) {
        if (left == 0L || right == 0L) return 0L;
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    public record FrequencyBand(long minimumInclusive, long maximumInclusive) {
        public FrequencyBand {
            if (minimumInclusive < 0L || maximumInclusive < minimumInclusive) {
                throw new IllegalArgumentException("invalid frequency band");
            }
        }

        public boolean contains(long value) {
            return value >= minimumInclusive && value <= maximumInclusive;
        }
    }

    private record PairDraft(int left, int right, long count, int associationQ20) { }

    /** Cold-path compiler. Frozen runtime retains none of these object collections. */
    public static final class Builder {
        private final int wordCount;
        private final long[] frequency;
        private final long[] classMask;
        private final int[] morphologyMask;
        private final long[][] conceptsByWord;
        private final long[][] subjectsByWord;
        private final List<PairDraft> pairs = new ArrayList<>();
        private boolean frozen;

        private Builder(int wordCount) {
            if (wordCount < 0) throw new IllegalArgumentException("wordCount < 0");
            this.wordCount = wordCount;
            frequency = new long[wordCount + 1];
            classMask = new long[wordCount + 1];
            morphologyMask = new int[wordCount + 1];
            conceptsByWord = new long[wordCount + 1][];
            subjectsByWord = new long[wordCount + 1][];
            for (int cwid = 0; cwid <= wordCount; cwid++) {
                conceptsByWord[cwid] = new long[0];
                subjectsByWord[cwid] = new long[0];
            }
        }

        public Builder word(
                int cwid,
                long wordFrequency,
                long broadClassMask,
                int wordMorphologyMask,
                long[] conceptIds,
                long[] subjectIds) {
            ensureMutable();
            checkWord(cwid);
            if (wordFrequency < 0L) throw new IllegalArgumentException("negative frequency");
            frequency[cwid] = wordFrequency;
            classMask[cwid] = broadClassMask;
            morphologyMask[cwid] = wordMorphologyMask;
            conceptsByWord[cwid] = sortedUnique(conceptIds, "concept");
            subjectsByWord[cwid] = sortedUnique(subjectIds, "subject");
            return this;
        }

        public Builder pair(int leftCwid, int rightCwid, long count, int associationQ20) {
            ensureMutable();
            checkWord(leftCwid);
            checkWord(rightCwid);
            if (count < 0L || associationQ20 < 0 || associationQ20 > Q20_ONE) {
                throw new IllegalArgumentException("invalid pair geometry");
            }
            pairs.add(new PairDraft(leftCwid, rightCwid, count, associationQ20));
            return this;
        }

        public M3StaticLanguageGeometry freeze() {
            ensureMutable();
            frozen = true;

            int[] conceptOffsets = offsets(conceptsByWord);
            long[] concepts = flatten(conceptsByWord, conceptOffsets[wordCount + 1]);
            int[] subjectOffsets = offsets(subjectsByWord);
            long[] subjects = flatten(subjectsByWord, subjectOffsets[wordCount + 1]);

            PairDraft[] sorted = pairs.toArray(PairDraft[]::new);
            Arrays.sort(sorted, Comparator.comparingInt(PairDraft::left)
                    .thenComparingInt(PairDraft::right));
            for (int index = 1; index < sorted.length; index++) {
                if (sorted[index - 1].left() == sorted[index].left()
                        && sorted[index - 1].right() == sorted[index].right()) {
                    throw new IllegalArgumentException("duplicate pair");
                }
            }

            int[] pairOffsets = new int[wordCount + 2];
            int[] pairTargets = new int[sorted.length];
            long[] pairCounts = new long[sorted.length];
            int[] pairAssociationQ20 = new int[sorted.length];
            for (int row = 0; row < sorted.length; row++) {
                PairDraft pair = sorted[row];
                pairOffsets[pair.left() + 1]++;
                pairTargets[row] = pair.right();
                pairCounts[row] = pair.count();
                pairAssociationQ20[row] = pair.associationQ20();
            }
            for (int index = 1; index < pairOffsets.length; index++) {
                pairOffsets[index] = Math.addExact(pairOffsets[index], pairOffsets[index - 1]);
            }

            return new M3StaticLanguageGeometry(
                    wordCount,
                    frequency.clone(),
                    classMask.clone(),
                    morphologyMask.clone(),
                    conceptOffsets,
                    concepts,
                    subjectOffsets,
                    subjects,
                    pairOffsets,
                    pairTargets,
                    pairCounts,
                    pairAssociationQ20);
        }

        private void checkWord(int cwid) {
            if (cwid <= 0 || cwid > wordCount) throw new IndexOutOfBoundsException("CWID " + cwid);
        }

        private void ensureMutable() {
            if (frozen) throw new IllegalStateException("language builder already frozen");
        }

        private static long[] sortedUnique(long[] source, String label) {
            long[] values = Objects.requireNonNull(source, label + "Ids").clone();
            for (long value : values) {
                if (value <= 0L) throw new IllegalArgumentException(label + " ID must be positive");
            }
            Arrays.sort(values);
            for (int index = 1; index < values.length; index++) {
                if (values[index - 1] == values[index]) {
                    throw new IllegalArgumentException("duplicate " + label + " ID");
                }
            }
            return values;
        }

        private static int[] offsets(long[][] rows) {
            int[] offsets = new int[rows.length + 1];
            for (int index = 0; index < rows.length; index++) {
                offsets[index + 1] = Math.addExact(offsets[index], rows[index].length);
            }
            return offsets;
        }

        private static long[] flatten(long[][] rows, int size) {
            long[] flat = new long[size];
            int cursor = 0;
            for (long[] row : rows) {
                System.arraycopy(row, 0, flat, cursor, row.length);
                cursor += row.length;
            }
            return flat;
        }
    }
}
