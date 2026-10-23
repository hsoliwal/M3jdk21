/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

/**
 * Internal String-to-M3 precompute dispatch membrane.
 *
 * <p>This class owns no cache and no text payload. {@link String} already carries an optional
 * canonical {@link M3String} owner+coordinate. The bridge centralizes the decision of which
 * existing M3 fact/search owner to consult before falling back to the historical exact String
 * path. It never admits a flat String merely to obtain an optimization hit.</p>
 *
 * <p>All indexes are Java UTF-16 code-unit indexes. Candidate facts can reject impossible
 * matches, but positive results remain exact M3/String comparisons. Eviction or unavailable
 * precompute changes performance only.</p>
 */
final class M3StringPrecomputeBridge {
    private M3StringPrecomputeBridge() {}

    /** Return only already-published facts for this exact String coordinate. */
    static java.util.function.LongPredicate trigramFactsIfPrepared(M3String source) {
        if (source == null) return null;
        M3StringFacts prepared = source.factsIfPrepared();
        return prepared != null && prepared.utf16Length == source.length() ? prepared : null;
    }

    static boolean contentEquals(
            String left, String right, M3String leftStorage, M3String rightStorage) {
        if (leftStorage != null) {
            if (rightStorage == null && right.cachedHashDiffers(leftStorage)) return false;
            return leftStorage.contentEquals(right);
        }
        if (rightStorage != null) {
            if (left.cachedHashDiffers(rightStorage)) return false;
            return rightStorage.contentEquals(left);
        }
        throw new IllegalArgumentException("M3 bridge requires canonical backing");
    }

    static int compareTo(
            String left, String right, M3String leftStorage, M3String rightStorage) {
        if (leftStorage != null && leftStorage.sameCoordinate(rightStorage)) return 0;
        int length1 = left.length();
        int length2 = right.length();
        int limit = Math.min(length1, length2);
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        int difference;
        if (leftStorage != null) {
            difference = rightStorage != null
                    ? leftStorage.comparePrefix(rightStorage, limit)
                    : leftStorage.compareFlatPrefix(right, limit);
        } else if (rightStorage != null) {
            difference = -rightStorage.compareFlatPrefix(left, limit);
        } else {
            difference = 0;
            for (int index = 0; index < limit; index++) {
                difference = left.charAt(index) - right.charAt(index);
                if (difference != 0) break;
            }
=======
        if (limit >= 256
                && (leftStorage != null && leftStorage.owner() instanceof M3StringTuple
                        || rightStorage != null && rightStorage.owner() instanceof M3StringTuple)) {
            int firstDifference = left.charAt(0) - right.charAt(0);
            if (firstDifference != 0) return firstDifference;
            if (leftStorage != null && rightStorage != null) {
                int difference = new M3String.CodeUnitCursor(leftStorage).compareRange(
                        new M3String.CodeUnitCursor(rightStorage), 1, 1, limit - 1);
                return difference != 0 ? difference : length1 - length2;
            }
            M3String.CodeUnitCursor leftUnits = leftStorage != null
                    && leftStorage.owner() instanceof M3StringTuple
                    ? new M3String.CodeUnitCursor(leftStorage) : null;
            M3String.CodeUnitCursor rightUnits = rightStorage != null
                    && rightStorage.owner() instanceof M3StringTuple
                    ? new M3String.CodeUnitCursor(rightStorage) : null;
            for (int index = 1; index < limit; index++) {
                char leftUnit = leftUnits == null ? left.charAt(index) : leftUnits.at(index);
                char rightUnit = rightUnits == null ? right.charAt(index) : rightUnits.at(index);
                int difference = leftUnit - rightUnit;
                if (difference != 0) return difference;
            }
            return length1 - length2;
        }
        for (int index = 0; index < limit; index++) {
            int difference = left.charAt(index) - right.charAt(index);
            if (difference != 0) return difference;
>>>>>>> 0cd0944136c1a87b1bb8e15e8052ed1fbdfb74c5
        }
        return difference != 0 ? difference : length1 - length2;
    }

    static boolean startsWith(M3String source, String prefix, int offset) {
        M3String prefixStorage = prefix.m3();
        if (prefixStorage != null) {
            return source.startsWith(prefixStorage, offset);
        }

        M3StringFacts prepared = source.factsIfPrepared();
        if (prepared != null) {
            if (!prepared.mayContain(prefix)) return false;
            if (offset == 0 && !prepared.prefixMayMatch(prefix)) return false;
            if (offset == source.length() - prefix.length()
                    && !prepared.suffixMayMatch(prefix)) {
                return false;
            }
        }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        // Preserve flat-prefix admission (never intern solely for a match).
        // A long source tuple is read by consecutive scalar runs rather than
        // descending its binary DAG for every compared UTF-16 code unit.
        if (prefix.length() >= 64 && source.owner() instanceof M3StringTuple) {
            M3String.CodeUnitCursor units = new M3String.CodeUnitCursor(source);
            for (int index = 0; index < prefix.length(); index++) {
=======
        // Flat-prefix lookup keeps admission unchanged; only the tuple needs a cursor.
        if (prefix.length() >= 256 && source.owner() instanceof M3StringTuple) {
            if (source.charAt(offset) != prefix.charAt(0)) return false;
            M3String.CodeUnitCursor units = new M3String.CodeUnitCursor(source);
            for (int index = 1; index < prefix.length(); index++) {
>>>>>>> 0cd0944136c1a87b1bb8e15e8052ed1fbdfb74c5
                if (units.at(offset + index) != prefix.charAt(index)) return false;
            }
            return true;
        }
        for (int index = 0; index < prefix.length(); index++) {
            if (source.charAt(offset + index) != prefix.charAt(index)) return false;
        }
        return true;
    }

    static int hashCodeValue(M3String source) {
        return source.hashCodeValue();
    }

    static int indexOf(M3String source, String target, int fromIndex, int endIndex) {
        int end = Math.min(source.length(), endIndex);
        int from = Math.clamp(fromIndex, 0, source.length());

        M3String targetStorage = target.m3();
        if (targetStorage != null) {
            return source.indexOf(targetStorage, from, end);
        }

        int targetLength = target.length();
        if (targetLength == 0) return Math.min(from, end);
        if (from > end - targetLength) return -1;

        M3StringFacts prepared = source.factsIfPrepared();
        if (prepared != null && !prepared.mayContain(target)) return -1;
        // The exact-trigram gate of the source's repeat use (A42): absence answers without a scan.
        if (!M3StringSearchPrecompute.mayContain(source, target)) return -1;

        int limit = end - targetLength;
        char first = target.charAt(0);
        for (int start = from; start <= limit; start++) {
            if (source.charAt(start) != first) continue;
            int index = 1;
            while (index < targetLength
                    && source.charAt(start + index) == target.charAt(index)) {
                index++;
            }
            if (index == targetLength) return start;
        }
        return -1;
    }

    static int lastIndexOf(M3String source, String target, int fromIndex) {
        int targetLength = target.length();
        int start = Math.min(fromIndex, source.length() - targetLength);
        if (start < 0) return -1;
        if (targetLength == 0) return start;

        M3String targetStorage = target.m3();
        if (targetStorage != null) {
            return source.lastIndexOf(targetStorage, fromIndex);
        }

        M3StringFacts prepared = source.factsIfPrepared();
        if (prepared != null && !prepared.mayContain(target)) return -1;
        // The exact-trigram gate of the source's repeat use (A42): absence answers without a scan.
        if (!M3StringSearchPrecompute.mayContain(source, target)) return -1;

        for (int candidate = start; candidate >= 0; candidate--) {
            int index = 0;
            while (index < targetLength
                    && source.charAt(candidate + index) == target.charAt(index)) {
                index++;
            }
            if (index == targetLength) return candidate;
        }
        return -1;
    }

    static String substring(M3String source, int beginIndex, int endIndex) {
        return new String(source.slice(beginIndex, endIndex));
    }

    static String trim(String original, M3String source) {
        M3StringFacts facts = source.facts();
        if (facts.trimStart == 0 && facts.trimEnd == source.length()) return original;
        if (facts.trimStart == source.length()) return "";
        return new String(source.slice(facts.trimStart, facts.trimEnd));
    }

    static String strip(String original, M3String source) {
        M3StringFacts facts = source.facts();
        if (facts.stripStart == 0 && facts.stripEnd == source.length()) return original;
        if (facts.stripStart == source.length()) return "";
        return new String(source.slice(facts.stripStart, facts.stripEnd));
    }

    static String stripLeading(String original, M3String source) {
        int start = source.facts().stripStart;
        if (start == 0) return original;
        if (start == source.length()) return "";
        return new String(source.slice(start, source.length()));
    }

    static String stripTrailing(String original, M3String source) {
        int end = source.facts().stripEnd;
        if (end == source.length()) return original;
        if (end == 0) return "";
        return new String(source.slice(0, end));
    }
}
