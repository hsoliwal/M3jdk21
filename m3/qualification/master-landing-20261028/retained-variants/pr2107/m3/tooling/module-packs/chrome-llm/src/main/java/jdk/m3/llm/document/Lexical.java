/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK 21 from m3-flldcim/tools/m3-text-pipeline/src/main/java/jdk/internal/m3/flldcim/tools/Lexical.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Offset-stable Java lexical projection. Offsets are UTF-16, not byte positions. */
public final class Lexical {
    private static final Pattern UNICODE_ESCAPE = Pattern.compile("\\\\u+[0-9a-fA-F]{4}");
    private static final Pattern TOKEN = Pattern.compile(
            "[\\p{javaJavaIdentifierStart}][\\p{javaJavaIdentifierPart}]*|"
            + "(?:0[xX][0-9a-fA-F_]+|[0-9][0-9a-zA-Z_.]*)|"
            + ">>>=|>>>=|<<=|>>=|>>>|->|::|==|!=|<=|>=|&&|\\|\\||\\+\\+|--|"
            + "[+*/%&|^!-]=|<<|>>|[^\\s]");
    public record Span(int start, int end, String kind) { }
    public record Token(int start, int end, String text) { }
    public record View(String masked, List<Span> protectedSpans, List<Token> tokens,
                       List<String> residues) {
        public View {
            protectedSpans = List.copyOf(protectedSpans);
            tokens = List.copyOf(tokens); residues = List.copyOf(residues);
        }
        public boolean safe() { return residues.isEmpty(); }
    }
    private Lexical() { }
    public static View scan(String text) {
        List<Span> spans = new ArrayList<>();
        List<String> residues = new ArrayList<>();
        if (UNICODE_ESCAPE.matcher(text).find()) residues.add("JAVA_UNICODE_ESCAPE_REQUIRES_OFFSET_MAPPING");
        int i = 0;
        while (i < text.length()) {
            int start = i;
            String kind;
            if (text.startsWith("//", i)) {
                i += 2;
                while (i < text.length() && text.charAt(i) != '\n' && text.charAt(i) != '\r') i++;
                kind = "COMMENT";
            } else if (text.startsWith("/*", i)) {
                int end = text.indexOf("*/", i + 2);
                if (end < 0) { residues.add("UNCLOSED_COMMENT"); i = text.length(); }
                else i = end + 2;
                kind = text.startsWith("/**", start) ? "JAVADOC" : "COMMENT";
            } else if (text.startsWith("\"\"\"", i)) {
                i += 3; boolean closed = false;
                while (i < text.length()) {
                    if (text.startsWith("\"\"\"", i) && !escaped(text, i)) { i += 3; closed = true; break; }
                    i++;
                }
                if (!closed) residues.add("UNCLOSED_TEXT_BLOCK");
                kind = "LITERAL";
            } else if (text.charAt(i) == '"' || text.charAt(i) == '\'') {
                char quote = text.charAt(i++); boolean closed = false;
                while (i < text.length()) {
                    char current = text.charAt(i++);
                    if (current == '\\' && i < text.length()) { i++; continue; }
                    if (current == quote) { closed = true; break; }
                    if (current == '\n' || current == '\r') break;
                }
                if (!closed) residues.add("UNCLOSED_LITERAL");
                kind = "LITERAL";
            } else { i++; continue; }
            spans.add(new Span(start, i, kind));
        }
        char[] masked = text.toCharArray();
        for (Span span : spans) for (int k = span.start(); k < span.end(); k++) {
            if (masked[k] != '\n' && masked[k] != '\r') masked[k] = ' ';
        }
        List<Token> tokens = new ArrayList<>();
        var matcher = TOKEN.matcher(new String(masked));
        while (matcher.find()) tokens.add(new Token(matcher.start(), matcher.end(), matcher.group()));
        // Preserve the complete raw spelling of every literal in lexical hashes.
        for (Span span : spans) if (span.kind().equals("LITERAL"))
            tokens.add(new Token(span.start(), span.end(), text.substring(span.start(), span.end())));
        tokens.sort(java.util.Comparator.comparingInt(Token::start));
        return new View(new String(masked), spans, tokens, residues);
    }
    private static boolean escaped(String text, int offset) {
        int count = 0;
        for (int i = offset - 1; i >= 0 && text.charAt(i) == '\\'; i--) count++;
        return (count & 1) != 0;
    }
    /** Idempotent edit; comments, documentation and all literal bytes remain unchanged. */
    public static String trimCodeTrailingSpace(String text, View view) {
        if (!view.safe()) return text;
        boolean[] protectedPosition = new boolean[text.length()];
        for (Span span : view.protectedSpans())
            java.util.Arrays.fill(protectedPosition, span.start(), span.end(), true);
        StringBuilder out = new StringBuilder(text.length());
        int line = 0;
        while (line < text.length()) {
            int end = line;
            while (end < text.length() && text.charAt(end) != '\n' && text.charAt(end) != '\r') end++;
            int keep = end;
            while (keep > line && !protectedPosition[keep - 1]
                    && (text.charAt(keep - 1) == ' ' || text.charAt(keep - 1) == '\t')) keep--;
            out.append(text, line, keep);
            if (end < text.length()) out.append(text.charAt(end++));
            if (end > 0 && text.charAt(end - 1) == '\r' && end < text.length()
                    && text.charAt(end) == '\n') out.append(text.charAt(end++));
            line = end;
        }
        return out.toString();
    }
}
