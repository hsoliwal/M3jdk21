// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Conservative regex necessary-substring facts over existing M3 array/offset/mask JNI owners.
 * Pattern and Matcher remain authoritative for actual regex semantics, captures and flags.
 * This only handles a bounded grammar of mandatory literal segments separated by .*, .+.
 * No regex text is rewritten and no native match is accepted without Java confirmation.
 */
public final class M3RegexRequiredSegments {
    public enum Backend { JAVA, AUTO, NATIVE_REQUIRED }

    public record Facts(int patternHash, int flags, int mandatoryCount,
                        int mandatoryUtf8Bytes, boolean proven, String category) { }

    private static final int MAX_PATTERN_UNITS = 8192;
    private static final int MAX_SEGMENTS = 4;
    private static final int MAX_LITERAL_BYTES = 65536;
    private static final String SPECIAL = "\\.^$|?*+()[]{}";
    private static volatile boolean nativeMissing;

    private final Pattern pattern;
    private final List<String> required;
    private final M3NativeAho multiLiteral;
    private final Facts facts;

    private M3RegexRequiredSegments(Pattern pattern) {
        this.pattern = Objects.requireNonNull(pattern, "pattern");
        this.required = parseNecessarySegments(pattern);
        this.multiLiteral = required.size() > 1 ? M3NativeAho.compile(required) : null;
        int bytes = 0;
        for (String literal : required) {
            bytes = Math.addExact(bytes, literal.getBytes(StandardCharsets.UTF_8).length);
        }
        this.facts = new Facts(pattern.pattern().hashCode(), pattern.flags(),
                required.size(), bytes, !required.isEmpty(), required.isEmpty()
                ? "JAVA_REGEX_ONLY" : "NATIVE_MANDATORY_SEGMENTS_CONSERVATIVE");
    }

    public static M3RegexRequiredSegments compile(String regex, int flags) {
        return fromPattern(Pattern.compile(Objects.requireNonNull(regex, "regex"), flags));
    }

    public static M3RegexRequiredSegments fromPattern(Pattern pattern) {
        return new M3RegexRequiredSegments(pattern);
    }

    public Pattern pattern() { return pattern; }
    public Facts facts() { return facts; }
    public List<String> necessarySegments() { return required; }

    /**
     * Returns original line ordinals. Native kernels only reject impossible candidates;
     * every positive is independently checked with Pattern.matcher(...).find().
     * AUTO falls back on a missing library or malformed UTF-16; wrong ABI is fatal.
     */
    public int[] select(List<String> lines, Backend backend) {
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(backend, "backend");
        for (String line : lines) Objects.requireNonNull(line, "line");
        if (backend == Backend.JAVA) return selectJava(lines);
        if (required.isEmpty()) {
            if (backend == Backend.NATIVE_REQUIRED) {
                throw new UnsupportedOperationException("regex lacks a necessary-segment proof");
            }
            return selectJava(lines);
        }
        if (!validUnicode(lines)) {
            if (backend == Backend.NATIVE_REQUIRED) {
                throw new IllegalArgumentException("invalid UTF-16 for native regex prefilter");
            }
            return selectJava(lines);
        }
        if (backend == Backend.AUTO && nativeMissing) return selectJava(lines);
        try {
            // Aho is an optional *single-pass native* refinement. If its library
            // is absent or a source batch exceeds the admitted window, the
            // pre-existing native grep chain remains the authoritative native path.
            // No externally visible exception or public contract changes.
            long[] nativeMask = multiLiteral != null && multiLiteral.fits(lines)
                    && M3NativeAho.available() ? multiLiteral.selectNative(lines) : null;
            NativeLineBatch selected = null;
            if (nativeMask == null) {
                selected = NativeLineBatch.fromUtf8Lines(lines);
                for (String segment : required) selected = selected.grepLiteral(segment);
            }
            // Preserve the incumbent's bounded result-allocation behavior.
            int capacity = nativeMask == null ? selected.selectedCount()
                    : Arrays.stream(nativeMask).mapToInt(Long::bitCount).sum();
            int[] ordinals = new int[capacity];
            int used = 0;
            for (int i = 0; i < lines.size(); i++) {
                boolean candidate = nativeMask != null
                        ? (nativeMask[i >>> 6] & (1L << (i & 63))) != 0
                        : selected.containsIndex(i);
                if (candidate && pattern.matcher(lines.get(i)).find()) {
                    ordinals[used++] = i;
                }
            }
            return Arrays.copyOf(ordinals, used);
        } catch (UnsatisfiedLinkError missing) {
            if (isMissing(missing)) nativeMissing = true;
            if (backend == Backend.AUTO && nativeMissing) return selectJava(lines);
            throw missing;
        } catch (NoClassDefFoundError failedInitialization) {
            if (backend == Backend.AUTO && nativeMissing) return selectJava(lines);
            throw failedInitialization;
        }
    }

    private int[] selectJava(List<String> lines) {
        int[] matches = new int[Math.min(lines.size(), 16)];
        int count = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (!pattern.matcher(lines.get(i)).find()) continue;
            if (count == matches.length) {
                int next = (int) Math.min((long) lines.size(), Math.max((long) count + 1, 2L * count));
                matches = Arrays.copyOf(matches, next);
            }
            matches[count++] = i;
        }
        return Arrays.copyOf(matches, count);
    }

    private static boolean isMissing(UnsatisfiedLinkError error) {
        String reason = error.getMessage();
        return reason != null && (reason.startsWith("no m3unixtext in java.library.path:")
                || reason.startsWith("Can't load library: "));
    }

    /** Exact, deliberately small proof language; metasyntax not recognized is rejected. */
    private static List<String> parseNecessarySegments(Pattern pattern) {
        if (pattern.flags() != 0) return List.of();
        String regex = pattern.pattern();
        if (regex.length() > MAX_PATTERN_UNITS) return List.of();
        int start = regex.startsWith("^") ? 1 : 0;
        int end = regex.endsWith("$") ? regex.length() - 1 : regex.length();
        if (start >= end) return List.of();
        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean variable = false;
        for (int i = start; i < end; i++) {
            char ch = regex.charAt(i);
            if (ch == '.' && i + 1 < end
                    && (regex.charAt(i + 1) == '*' || regex.charAt(i + 1) == '+')) {
                variable = true;
                if (!addPiece(pieces, current)) return List.of();
                i++;
                continue;
            }
            if (SPECIAL.indexOf(ch) >= 0) return List.of();
            current.append(ch);
        }
        if (!variable || !addPiece(pieces, current) || pieces.isEmpty()
                || pieces.size() > MAX_SEGMENTS) return List.of();
        int bytes = 0;
        for (String piece : pieces) {
            if (!validUnicode(piece)) return List.of();
            bytes += piece.getBytes(StandardCharsets.UTF_8).length;
            if (bytes > MAX_LITERAL_BYTES) return List.of();
        }
        return List.copyOf(pieces);
    }

    private static boolean addPiece(List<String> pieces, StringBuilder current) {
        if (!current.isEmpty()) {
            if (pieces.size() == MAX_SEGMENTS) return false;
            pieces.add(current.toString());
            current.setLength(0);
        }
        return true;
    }

    private static boolean validUnicode(List<String> lines) {
        for (String line : lines) if (!validUnicode(line)) return false;
        return true;
    }

    private static boolean validUnicode(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isHighSurrogate(ch)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) return false;
            } else if (Character.isLowSurrogate(ch)) return false;
        }
        return true;
    }
}
