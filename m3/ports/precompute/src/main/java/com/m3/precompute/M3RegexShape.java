// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.Objects;

/**
 * Conservative Java-regex shape precompute.
 *
 * <p>Only expressions whose entire body is provably literal are specialized. Anything else is
 * GENERAL and must execute through the authoritative regex engine.</p>
 */
public final class M3RegexShape {
    public enum Kind {
        LITERAL,
        STRICT_PREFIX,
        STRICT_SUFFIX,
        STRICT_EXACT,
        GENERAL
    }

    private final String expression;
    private final Kind kind;
    private final String literal;

    private M3RegexShape(String expression, Kind kind, String literal) {
        this.expression = expression;
        this.kind = kind;
        this.literal = literal;
    }

    public static M3RegexShape analyze(CharSequence regex) {
        String expression = Objects.requireNonNull(regex, "regex").toString();
        boolean start = expression.startsWith("\\A");
        boolean end =
                expression.endsWith("\\z")
                        && expression.length() >= (start ? 4 : 2);

        int begin = start ? 2 : 0;
        int finish = end ? expression.length() - 2 : expression.length();
        String literal = decodeLiteral(expression, begin, finish);
        if (literal == null) return new M3RegexShape(expression, Kind.GENERAL, "");

        Kind kind =
                start && end
                        ? Kind.STRICT_EXACT
                        : start
                                ? Kind.STRICT_PREFIX
                                : end ? Kind.STRICT_SUFFIX : Kind.LITERAL;
        return new M3RegexShape(expression, kind, literal);
    }

    public String expression() {
        return expression;
    }

    public Kind kind() {
        return kind;
    }

    public boolean specialized() {
        return kind != Kind.GENERAL;
    }

    public String literal() {
        if (!specialized()) {
            throw new IllegalStateException("general regex has no exact literal shape");
        }
        return literal;
    }

    private static String decodeLiteral(String expression, int begin, int end) {
        StringBuilder out = new StringBuilder(Math.max(0, end - begin));
        boolean quoted = false;
        for (int index = begin; index < end; index++) {
            char value = expression.charAt(index);

            if (Character.isSurrogate(value)) {
                if (!Character.isHighSurrogate(value)
                        || index + 1 >= end
                        || !Character.isLowSurrogate(expression.charAt(index + 1))) {
                    return null;
                }
                out.append(value).append(expression.charAt(++index));
                continue;
            }

            if (quoted) {
                if (value == '\\'
                        && index + 1 < end
                        && expression.charAt(index + 1) == 'E') {
                    quoted = false;
                    index++;
                } else {
                    out.append(value);
                }
                continue;
            }

            if (value == '\\') {
                if (index + 1 >= end) return null;
                char escaped = expression.charAt(++index);
                if (escaped == 'Q') {
                    quoted = true;
                    continue;
                }
                if (isEscapableLiteral(escaped)) {
                    out.append(escaped);
                    continue;
                }
                return null;
            }

            if (isRegexMeta(value)) return null;
            out.append(value);
        }
        return quoted ? null : out.toString();
    }

    private static boolean isRegexMeta(char value) {
        return switch (value) {
            case '.', '*', '+', '?', '(', ')', '[', ']', '{', '}', '|', '^', '$' -> true;
            default -> false;
        };
    }

    private static boolean isEscapableLiteral(char value) {
        return switch (value) {
            case '.', '*', '+', '?', '(', ')', '[', ']', '{', '}', '|', '^', '$', '\\' -> true;
            default -> false;
        };
    }
}
