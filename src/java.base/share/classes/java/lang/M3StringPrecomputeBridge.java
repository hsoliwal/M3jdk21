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

        // Preserve flat-prefix admission (never intern solely for a match).
        // A long source tuple is read by consecutive scalar runs rather than
        // descending its binary DAG for every compared UTF-16 code unit.
        if (prefix.length() >= 64 && source.owner() instanceof M3StringTuple) {
            M3String.CodeUnitCursor units = new M3String.CodeUnitCursor(source);
            for (int index = 0; index < prefix.length(); index++) {
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

        return M3StringSearchPrecompute.indexOf(source, target, from, end);
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

        return M3StringSearchPrecompute.lastIndexOf(source, target, start);
    }

    static String substring(M3String source, int beginIndex, int endIndex) {
        return new String(source.slice(beginIndex, endIndex));
    }

    static String trim(String original, M3String source) {
        return bothEdges(original, source, false);
    }

    static String strip(String original, M3String source) {
        return bothEdges(original, source, true);
    }

    private static String bothEdges(String original, M3String source, boolean strip) {
        int start = edgeStart(source, strip);
        int end = start == source.length() ? 0 : edgeEnd(source, strip);
        if (start == 0 && end == source.length()) return original;
        if (start == source.length()) return "";
        return new String(source.slice(start, end));
    }

    static String stripLeading(String original, M3String source) {
        int start = edgeStart(source, true);
        if (start == 0) return original;
        if (start == source.length()) return "";
        return new String(source.slice(start, source.length()));
    }

    static String stripTrailing(String original, M3String source) {
        int end = edgeEnd(source, true);
        if (end == source.length()) return original;
        if (end == 0) return "";
        return new String(source.slice(0, end));
    }

    /** Longest edge run that is read unit by unit before the cached facts are prepared. */
    private static final int EDGE_SCAN_UNITS = 64;
    /** Result of an edge scan that reached its bound before the edge was decided. */
    static final int EDGE_UNDECIDED = -1;

    /**
     * Leading edge of {@code strip} (code points, {@link Character#isWhitespace(int)}) or of
     * {@code trim} (units up to U+0020): the index of the first unit that stays, or the length
     * when none stays. Reads the prepared measure when there is one, else scans at most
     * {@link #EDGE_SCAN_UNITS} units, and prepares the facts only for a longer run.
     */
    static int edgeStart(M3String source, boolean strip) {
        M3StringFacts prepared = source.factsIfPrepared();
        if (prepared == null) {
            int edge = leadingEdge(source, strip, EDGE_SCAN_UNITS);
            if (edge != EDGE_UNDECIDED) return edge;
            prepared = source.facts();
        }
        return strip ? prepared.stripStart : prepared.trimStart;
    }

    /**
     * Trailing edge, with the rules of {@link #edgeStart}: the end-exclusive index of the last
     * unit that stays, or 0 when none stays.
     */
    static int edgeEnd(M3String source, boolean strip) {
        M3StringFacts prepared = source.factsIfPrepared();
        if (prepared == null) {
            int edge = trailingEdge(source, strip, EDGE_SCAN_UNITS);
            if (edge != EDGE_UNDECIDED) return edge;
            prepared = source.facts();
        }
        return strip ? prepared.stripEnd : prepared.trimEnd;
    }

    /** Bounded leading scan: gives up before it reads the unit at index {@code bound}. */
    static int leadingEdge(CharSequence units, boolean strip, int bound) {
        int length = units.length();
        int index = 0;
        while (index < length) {
            if (index >= bound) return EDGE_UNDECIDED;
            int step = strip ? stripStepForward(units, index, length)
                    : (units.charAt(index) <= ' ' ? 1 : 0);
            if (step == 0) return index;
            index += step;
        }
        return length;
    }

    /** Bounded trailing scan: gives up once {@code bound} units have been removed. */
    static int trailingEdge(CharSequence units, boolean strip, int bound) {
        int length = units.length();
        int end = length;
        while (end > 0) {
            if (length - end >= bound) return EDGE_UNDECIDED;
            int step = strip ? stripStepBackward(units, end)
                    : (units.charAt(end - 1) <= ' ' ? 1 : 0);
            if (step == 0) return end;
            end -= step;
        }
        return 0;
    }

    /** Units of the white-space code point that starts at {@code index}; 0 when it is none. */
    private static int stripStepForward(CharSequence units, int index, int length) {
        char unit = units.charAt(index);
        if (Character.isHighSurrogate(unit) && index + 1 < length) {
            char next = units.charAt(index + 1);
            if (Character.isLowSurrogate(next)) {
                return Character.isWhitespace(Character.toCodePoint(unit, next)) ? 2 : 0;
            }
        }
        return Character.isWhitespace(unit) ? 1 : 0;
    }

    /** Units of the white-space code point that ends before {@code end}; 0 when it is none. */
    private static int stripStepBackward(CharSequence units, int end) {
        char unit = units.charAt(end - 1);
        if (Character.isLowSurrogate(unit) && end > 1) {
            char previous = units.charAt(end - 2);
            if (Character.isHighSurrogate(previous)) {
                return Character.isWhitespace(Character.toCodePoint(previous, unit)) ? 2 : 0;
            }
        }
        return Character.isWhitespace(unit) ? 1 : 0;
    }
}
