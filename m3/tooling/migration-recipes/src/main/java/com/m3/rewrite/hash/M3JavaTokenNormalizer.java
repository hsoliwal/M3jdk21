// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.hash;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministic Java lexical normalizer used only after OpenRewrite has parsed the source.
 *
 * <p>Whitespace and comments disappear, token boundaries remain explicit, and literal text is
 * preserved. This is a structural signal, not a semantic-equivalence oracle.
 */
final class M3JavaTokenNormalizer {
    private static final List<String> OPERATORS = List.of(
                    ">>>=", "<<=", ">>=", "...", ">>>", "::", "->",
                    "==", "!=", "<=", ">=", "&&", "||", "++", "--",
                    "+=", "-=", "*=", "/=", "&=", "|=", "^=", "%=", "<<", ">>",
                    "+", "-", "*", "/", "%", "&", "|", "^", "~", "!", "<", ">",
                    "=", "?", ":", ".", ",", ";", "(", ")", "[", "]", "{", "}", "@")
            .stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();

    private M3JavaTokenNormalizer() {}

    static String normalize(String source) {
        List<String> tokens = tokens(source);
        return String.join("\u001f", tokens);
    }

    static List<String> tokens(String source) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < source.length()) {
            char ch = source.charAt(i);

            if (Character.isWhitespace(ch)) {
                i++;
                continue;
            }
            if (starts(source, i, "//")) {
                i = lineCommentEnd(source, i + 2);
                continue;
            }
            if (starts(source, i, "/*")) {
                i = blockCommentEnd(source, i + 2);
                continue;
            }
            if (starts(source, i, "\"\"\"")) {
                int end = textBlockEnd(source, i + 3);
                tokens.add(source.substring(i, end));
                i = end;
                continue;
            }
            if (ch == '"') {
                int end = quotedEnd(source, i + 1, '"');
                tokens.add(source.substring(i, end));
                i = end;
                continue;
            }
            if (ch == '\'') {
                int end = quotedEnd(source, i + 1, '\'');
                tokens.add(source.substring(i, end));
                i = end;
                continue;
            }
            if (Character.isJavaIdentifierStart(ch)) {
                int end = i + 1;
                while (end < source.length()
                        && Character.isJavaIdentifierPart(source.charAt(end))) {
                    end++;
                }
                tokens.add(source.substring(i, end));
                i = end;
                continue;
            }
            if (Character.isDigit(ch)
                    || (ch == '.' && i + 1 < source.length()
                            && Character.isDigit(source.charAt(i + 1)))) {
                int end = numberEnd(source, i);
                tokens.add(source.substring(i, end));
                i = end;
                continue;
            }

            String operator = operatorAt(source, i);
            if (operator != null) {
                tokens.add(operator);
                i += operator.length();
                continue;
            }

            tokens.add(String.valueOf(ch));
            i++;
        }
        return List.copyOf(tokens);
    }

    private static int lineCommentEnd(String source, int i) {
        while (i < source.length() && source.charAt(i) != '\n' && source.charAt(i) != '\r') {
            i++;
        }
        return i;
    }

    private static int blockCommentEnd(String source, int i) {
        while (i + 1 < source.length()) {
            if (source.charAt(i) == '*' && source.charAt(i + 1) == '/') {
                return i + 2;
            }
            i++;
        }
        return source.length();
    }

    private static int quotedEnd(String source, int i, char quote) {
        boolean escaped = false;
        while (i < source.length()) {
            char ch = source.charAt(i++);
            if (escaped) {
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == quote) {
                return i;
            }
        }
        return source.length();
    }

    private static int textBlockEnd(String source, int i) {
        while (i + 2 < source.length()) {
            if (source.charAt(i) == '"'
                    && source.charAt(i + 1) == '"'
                    && source.charAt(i + 2) == '"'
                    && !escaped(source, i)) {
                return i + 3;
            }
            i++;
        }
        return source.length();
    }

    private static boolean escaped(String source, int position) {
        int slashes = 0;
        for (int i = position - 1; i >= 0 && source.charAt(i) == '\\'; i--) {
            slashes++;
        }
        return (slashes & 1) == 1;
    }

    private static int numberEnd(String source, int start) {
        int i = start;
        char previous = 0;
        while (i < source.length()) {
            char ch = source.charAt(i);
            boolean ordinary = Character.isLetterOrDigit(ch) || ch == '_' || ch == '.';
            boolean exponentSign = (ch == '+' || ch == '-')
                    && (previous == 'e' || previous == 'E' || previous == 'p' || previous == 'P');
            if (!ordinary && !exponentSign) {
                break;
            }
            previous = ch;
            i++;
        }
        return i;
    }

    private static String operatorAt(String source, int i) {
        for (String operator : OPERATORS) {
            if (starts(source, i, operator)) {
                return operator;
            }
        }
        return null;
    }

    private static boolean starts(String source, int offset, String token) {
        return source.regionMatches(offset, token, 0, token.length());
    }
}
