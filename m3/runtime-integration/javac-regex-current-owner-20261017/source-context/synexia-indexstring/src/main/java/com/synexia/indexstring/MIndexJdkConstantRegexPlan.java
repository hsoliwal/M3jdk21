// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Explicit opt-in, compile-once JDK regex plan for a javac-proven constant.
 *
 * <p>Pattern owns exact Java regular-expression semantics, including backrefs,
 * Unicode flags, zero-width matches and replacement group syntax. The compiled
 * Pattern can be shared safely. Residual matching uses a fresh Matcher per
 * invocation; proven whole-string literals instead use exact String equality.
 * No mutable Matcher pooling or GC-unstable JNI object references are used.
 *
 * <p>This plan does not itself rewrite String methods or move a
 * PatternSyntaxException to a class initializer. Callers prepare only after
 * the compiler evidence and behavioral gates qualify that exact call site.
 */
public final class MIndexJdkConstantRegexPlan {
    private final Pattern pattern;
    private final int requestedFlags;
    /**
     * Compiler-proven full-match literal only. Null means JDK Matcher owns
     * execution; never a speculative regex filter or a global Pattern cache.
     */
    private final String fullMatchLiteral;

    private MIndexJdkConstantRegexPlan(Pattern pattern, int flags,
            String fullMatchLiteral) {
        this.pattern = pattern;
        this.requestedFlags = flags;
        this.fullMatchLiteral = fullMatchLiteral;
    }

    public static MIndexJdkConstantRegexPlan compile(String expression) {
        return compile(expression, 0);
    }

    public static MIndexJdkConstantRegexPlan compile(String expression, int flags) {
        String checked = Objects.requireNonNull(expression, "expression");
        if (checked.length() > 8192) {
            throw new IllegalArgumentException("M3 compile-time regex budget exceeded");
        }
        // Validate via the JDK first, preserving syntax/flag exception timing
        // at the caller's explicit compile operation.
        Pattern compiled = Pattern.compile(checked, flags);
        return new MIndexJdkConstantRegexPlan(compiled, flags,
                exactWholeMatchLiteral(checked, flags));
    }

    public String expression() { return pattern.pattern(); }
    /** Caller-supplied flags, before any JDK-inferred Unicode bits. */
    public int flags() { return requestedFlags; }
    /** Exact effective flags reported by the running JDK Pattern owner. */
    public int javaEffectiveFlags() { return pattern.flags(); }
    public Pattern javaPattern() { return pattern; }

    /**
     * Exact literal equality is safe for Pattern.LITERAL with no other flags,
     * or a flag-free ASCII [A-Za-z0-9_]* regex (which has no regex syntax).
     * All other expressions delegate to the compiled JDK Pattern.
     */
    public boolean usesExactLiteralMatchFastPath() {
        return fullMatchLiteral != null;
    }

    public boolean matches(String input) {
        return matches((CharSequence) Objects.requireNonNull(input, "input"));
    }

    /**
     * Exact full-match execution over any String-shaped backing.
     *
     * <p>The literal fast path compares UTF-16 code units directly and therefore works for
     * canonical MIndex joined/range views without creating an intermediate java.lang.String.
     * Residual regex semantics remain owned by the compiled JDK Pattern.</p>
     */
    public boolean matches(CharSequence input) {
        CharSequence checked = Objects.requireNonNull(input, "input");
        return fullMatchLiteral != null
                ? contentEquals(fullMatchLiteral, checked)
                : pattern.matcher(checked).matches();
    }

    private static boolean contentEquals(String literal, CharSequence input) {
        if (input instanceof String string) return literal.equals(string);
        if (literal.length() != input.length()) return false;
        for (int index = 0; index < literal.length(); index++) {
            if (literal.charAt(index) != input.charAt(index)) return false;
        }
        return true;
    }

    private static String exactWholeMatchLiteral(String expression, int flags) {
        if (flags == Pattern.LITERAL) return expression;
        if (flags != 0) return null;
        for (int index = 0; index < expression.length(); index++) {
            char unit = expression.charAt(index);
            boolean ordinary = unit >= 'a' && unit <= 'z'
                    || unit >= 'A' && unit <= 'Z'
                    || unit >= '0' && unit <= '9'
                    || unit == '_';
            if (!ordinary) return null;
        }
        return expression;
    }

    public String[] split(String input) {
        return split(input, 0);
    }

    public String[] split(String input, int limit) {
        return pattern.split(Objects.requireNonNull(input, "input"), limit);
    }

    public String replaceAll(String input, String replacement) {
        Matcher matcher = pattern.matcher(Objects.requireNonNull(input, "input"));
        return matcher.replaceAll(replacement);
    }

    public String replaceFirst(String input, String replacement) {
        Matcher matcher = pattern.matcher(Objects.requireNonNull(input, "input"));
        return matcher.replaceFirst(replacement);
    }
}
