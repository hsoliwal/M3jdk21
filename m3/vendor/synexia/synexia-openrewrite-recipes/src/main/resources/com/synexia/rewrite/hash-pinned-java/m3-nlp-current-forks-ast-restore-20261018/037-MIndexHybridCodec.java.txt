// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Exact hybrid encoder: frozen lexicons first, dynamic literal ids as fallback.
 *
 * <p>The default encoder performs bounded greedy joining of Unicode runs, so
 * entries such as "Dr.", "New York" and "E = mc²" can resolve as one known
 * symbol without requiring raw String storage in the resulting atom stream.</p>
 */
public final class MIndexHybridCodec {
    public static final int DEFAULT_MAX_JOINED_SEGMENTS = 8;

    private MIndexHybridCodec() {}

    public static MIndexLexicalView encode(MIndexLexiconSpace space, CharSequence value) {
        return encode(space, value, DEFAULT_MAX_JOINED_SEGMENTS);
    }

    public static MIndexLexicalView encode(
            MIndexLexiconSpace space, CharSequence value, int maxJoinedSegments) {
        Objects.requireNonNull(space, "space");
        Objects.requireNonNull(value, "value");
        if (maxJoinedSegments < 1) throw new IllegalArgumentException("maxJoinedSegments must be >= 1");

        String source = value.toString();
        List<String> segments = segment(source);
        List<Long> bits = new ArrayList<>(segments.size());

        int index = 0;
        while (index < segments.size()) {
            MIndexAtomRef resolved = null;
            int resolvedEnd = index + 1;
            int limit = Math.min(segments.size(), index + maxJoinedSegments);
            for (int end = limit; end > index; end--) {
                String candidate = join(segments, index, end);
                var known = space.resolveKnown(candidate);
                if (known.isPresent()) {
                    resolved = known.get();
                    resolvedEnd = end;
                    break;
                }
            }
            if (resolved == null) resolved = space.resolve(segments.get(index));
            bits.add(resolved.bits());
            index = resolvedEnd;
        }

        return new MIndexLexicalView(MIndexString.literal(source), space, toPrimitive(bits));
    }

    public static MIndexLexicalView encodeSegments(
            MIndexLexiconSpace space, List<? extends CharSequence> segments) {
        Objects.requireNonNull(space, "space");
        Objects.requireNonNull(segments, "segments");
        StringBuilder source = new StringBuilder();
        long[] bits = new long[segments.size()];
        for (int index = 0; index < segments.size(); index++) {
            CharSequence segment = Objects.requireNonNull(segments.get(index), "segment");
            String value = segment.toString();
            source.append(value);
            bits[index] = space.resolve(value).bits();
        }
        return new MIndexLexicalView(MIndexString.literal(source.toString()), space, bits);
    }

    public static List<String> segment(CharSequence value) {
        Objects.requireNonNull(value, "value");
        List<String> result = new ArrayList<>();
        int index = 0;
        while (index < value.length()) {
            int start = index;
            int codePoint = Character.codePointAt(value, index);
            int kind = kind(codePoint);
            index += Character.charCount(codePoint);
            while (index < value.length()) {
                int next = Character.codePointAt(value, index);
                if (kind(next) != kind) break;
                index += Character.charCount(next);
            }
            result.add(value.subSequence(start, index).toString());
        }
        return List.copyOf(result);
    }

    public static String decode(MIndexLexicalView view) {
        return Objects.requireNonNull(view, "view").materializeAtoms();
    }

    private static String join(List<String> segments, int start, int end) {
        if (end == start + 1) return segments.get(start);
        StringBuilder result = new StringBuilder();
        for (int index = start; index < end; index++) result.append(segments.get(index));
        return result.toString();
    }

    private static int kind(int codePoint) {
        if (Character.isWhitespace(codePoint)) return 0;
        int type = Character.getType(codePoint);
        if (Character.isLetterOrDigit(codePoint)
                || type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK
                || type == Character.CONNECTOR_PUNCTUATION
                || codePoint == '\''
                || codePoint == 0x2019
                || codePoint == '-') {
            return 1;
        }
        return 2;
    }

    private static long[] toPrimitive(List<Long> values) {
        long[] result = new long[values.size()];
        for (int index = 0; index < values.size(); index++) result[index] = values.get(index);
        return result;
    }
}
