// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable, conservative regex precompute over the existing Unix4j/M3 native line selector.
 * No alternate regex engine: java.util.regex remains the semantic and capture authority.
 *
 * <p>A native preselection is admitted only when source inspection proves the entire
 * Pattern to be an exact, case-sensitive substring. Unknown constructs/flags never
 * become optimizations. Even admitted native positives are confirmed by Matcher.find().
 * Negative native results are sound only under the recorded exact-literal proof.
 * This is an in-process CPU selector, not Unix4j grep option equivalence.
 */
public final class M3RegexLiteralPlan {
    public enum Backend { JAVA, AUTO, NATIVE_REQUIRED }

    public record Signals(int patternHash, int flags, int utf16Units,
                          int utf8Bytes, boolean exactLiteral, String category) { }

    private static final String METACHARS = "\\.^$|?*+()[]{}";
    // Once NativeLineBatch.Native initialization fails, HotSpot marks that class erroneous.
    // The AUTO path must not attempt to initialize it again after an absent library.
    private static volatile boolean nativeLibraryAbsent;
    private final Pattern pattern;
    private final String literal; // null: proof not established, do not call native.
    private final Signals signals;

    private M3RegexLiteralPlan(Pattern pattern, String literal) {
        this.pattern = pattern;
        this.literal = literal;
        this.signals = new Signals(pattern.pattern().hashCode(), pattern.flags(),
                literal == null ? -1 : literal.length(),
                literal == null ? -1 : literal.getBytes(StandardCharsets.UTF_8).length,
                literal != null, literal == null ? "REGEX_JAVA_ORACLE" : "EXACT_LITERAL_JNI");
    }

    /** Java compilation and syntax failure happen before execution or native admission. */
    public static M3RegexLiteralPlan compile(String regex, int flags) {
        return fromPattern(Pattern.compile(Objects.requireNonNull(regex, "regex"), flags));
    }

    public static M3RegexLiteralPlan fromPattern(Pattern pattern) {
        Objects.requireNonNull(pattern, "pattern");
        return new M3RegexLiteralPlan(pattern, proveLiteral(pattern));
    }

    public Pattern pattern() { return pattern; }
    public Signals signals() { return signals; }

    /**
     * Primitive ordinal result, always in original order; no retained per-row Entry/Node.
     * AUTO falls back only for unavailable JNI or unrepresentable UTF-8 input.
     * NATIVE_REQUIRED refuses unknown patterns and malformed input before callbacks.
     */
    public int[] select(List<String> lines, Backend backend) {
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(backend, "backend");
        for (String line : lines) Objects.requireNonNull(line, "line");
        if (backend == Backend.JAVA) return selectJava(lines);
        if (literal == null) {
            if (backend == Backend.NATIVE_REQUIRED) {
                throw new UnsupportedOperationException("regex has no exact-literal native proof");
            }
            return selectJava(lines);
        }
        if (!validUtf16(lines)) {
            if (backend == Backend.NATIVE_REQUIRED) {
                throw new IllegalArgumentException("native input contains malformed UTF-16");
            }
            return selectJava(lines);
        }
        if (backend == Backend.AUTO && nativeLibraryAbsent) return selectJava(lines);
        try {
            NativeLineBatch selected = NativeLineBatch.fromUtf8Lines(lines).grepLiteral(literal);
            int[] positions = new int[selected.selectedCount()];
            int used = 0;
            for (int i = 0; i < lines.size(); i++) {
                if (selected.containsIndex(i) && pattern.matcher(lines.get(i)).find()) {
                    positions[used++] = i;
                }
            }
            return Arrays.copyOf(positions, used);
        } catch (UnsatisfiedLinkError missingNative) {
            if (isAbsentNativeLibrary(missingNative)) nativeLibraryAbsent = true;
            if (backend == Backend.AUTO && nativeLibraryAbsent) return selectJava(lines);
            throw missingNative;
        } catch (NoClassDefFoundError failedInitialization) {
            if (backend == Backend.AUTO
                    && (nativeLibraryAbsent || causedByAbsentLibrary(failedInitialization))) {
                nativeLibraryAbsent = true;
                return selectJava(lines);
            }
            throw failedInitialization;
        }
    }

    private static boolean isAbsentNativeLibrary(UnsatisfiedLinkError error) {
        String reason = error.getMessage();
        return reason != null && (reason.startsWith("no m3unixtext in java.library.path:")
                || reason.startsWith("Can't load library: "));
    }

    private static boolean causedByAbsentLibrary(Throwable error) {
        // JDK can wrap a failed native class initialization in ExceptionInInitializerError.
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof UnsatisfiedLinkError link && isAbsentNativeLibrary(link)) {
                return true;
            }
            String reason = cause.getMessage();
            if (reason != null && (reason.contains("no m3unixtext in java.library.path:")
                    || reason.contains("Can't load library: "))) return true;
        }
        return false;
    }

    private int[] selectJava(List<String> lines) {
        int[] positions = new int[Math.min(lines.size(), 16)];
        int used = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (pattern.matcher(lines.get(i)).find()) {
                if (used == positions.length) {
                    int grown = (int) Math.min((long) lines.size(),
                            Math.max((long) used + 1, (long) positions.length * 2));
                    positions = Arrays.copyOf(positions, grown);
                }
                positions[used++] = i;
            }
        }
        return Arrays.copyOf(positions, used);
    }

    /** Deliberately accepts a tiny, auditable proof language, not a regex parser. */
    private static String proveLiteral(Pattern pattern) {
        String regex = pattern.pattern();
        String candidate;
        if (pattern.flags() == Pattern.LITERAL) {
            candidate = regex;
        } else if (pattern.flags() == 0 && regex.startsWith("\\Q")
                && regex.endsWith("\\E") && regex.length() >= 4
                && !regex.substring(2, regex.length() - 2).contains("\\E")) {
            candidate = regex.substring(2, regex.length() - 2);
        } else if (pattern.flags() == 0 && isUnescapedLiteral(regex)) {
            candidate = regex;
        } else {
            return null;
        }
        return validUtf16(candidate) ? candidate : null;
    }

    private static boolean isUnescapedLiteral(String regex) {
        for (int i = 0; i < regex.length(); i++) {
            if (METACHARS.indexOf(regex.charAt(i)) >= 0) return false;
        }
        return true;
    }

    private static boolean validUtf16(List<String> lines) {
        for (String line : lines) if (!validUtf16(line)) return false;
        return true;
    }

    private static boolean validUtf16(String value) {
        for (int i = 0; i < value.length(); i++) {
            char unit = value.charAt(i);
            if (Character.isHighSurrogate(unit)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) return false;
            } else if (Character.isLowSurrogate(unit)) return false;
        }
        return true;
    }
}
