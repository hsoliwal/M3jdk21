/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.fill;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * No magic number and no floating string in the generated members: the rule of M3Culprits (m3/tooling/module-packs/
 * parser), applied here to source text. A numeric literal with an absolute value above 2, or not an integer, and a
 * non-empty string literal may only stand in a constant position (a static final field, an annotation, a case label);
 * a string may also stand in a message position (a throw statement, a format/info/warn/error/debug/trace call).
 * Constants are compile-time constants, so the JVM interns them in its string table (M3 String storage on M3JDK).
 */
class M3ArrayMapLiteralsTest {

    private static final double INDEX_ARITHMETIC_LIMIT = 2.0;
    private static final Pattern LITERAL = Pattern.compile(
            "\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])+'|(?<![\\w.])(?:0[xX][0-9a-fA-F_]+[lL]?|\\d[\\d_]*(?:\\.\\d+)?(?:[eE][+-]?\\d+)?[fFdDlL]?)(?![\\w.])");
    private static final Pattern MESSAGE_CALL = Pattern.compile("\\b(?:format|formatted|info|warn|error|debug|trace)\\s*\\(");

    /** Source text with comments blanked (string and char literals kept, line structure kept). */
    static String withoutComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '"' || c == '\'') {
                int end = i + 1;
                while (end < source.length() && source.charAt(end) != c) {
                    end += source.charAt(end) == '\\' ? 2 : 1;
                }
                out.append(source, i, Math.min(end + 1, source.length()));
                i = end + 1;
            } else if (source.startsWith("//", i)) {
                while (i < source.length() && source.charAt(i) != '\n') {
                    out.append(' ');
                    i++;
                }
            } else if (source.startsWith("/*", i)) {
                int end = source.indexOf("*/", i + 2);
                end = end < 0 ? source.length() : end + 2;
                for (; i < end; i++) {
                    out.append(source.charAt(i) == '\n' ? '\n' : ' ');
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** The statement around POSITION: from the previous ';', '{' or '}' to the next ';' or '{'. */
    static String statement(String text, int position) {
        int start = Math.max(Math.max(text.lastIndexOf(';', position), text.lastIndexOf('{', position)),
                text.lastIndexOf('}', position)) + 1;
        int semicolon = text.indexOf(';', position);
        int brace = text.indexOf('{', position);
        int end = semicolon < 0 ? text.length() : brace >= 0 && brace < semicolon ? brace : semicolon;
        return text.substring(start, end).strip();
    }

    static List<String> culprits(String source) {
        String text = withoutComments(source);
        List<String> found = new ArrayList<>();
        Matcher m = LITERAL.matcher(text);
        while (m.find()) {
            String literal = m.group();
            if (literal.startsWith("'")) {
                continue; // char literals are not culprits in M3Culprits
            }
            String statement = statement(text, m.start());
            boolean constant = statement.matches("(?s)(?:@\\w+.*|case .*|(?:(?:private|protected|public|static|final)\\s+)*static\\s+final\\s.*"
                    + "|(?:(?:private|protected|public)\\s+)?final\\s+static\\s.*)");
            if (constant) {
                continue;
            }
            int line = text.substring(0, m.start()).split("\n", -1).length;
            if (literal.startsWith("\"")) {
                boolean message = statement.startsWith("throw ") || MESSAGE_CALL.matcher(statement).find();
                if (literal.length() > 2 && !message) {
                    found.add("FLOATING_STRING line " + line + ": " + literal);
                }
            } else if (magic(literal)) {
                found.add("MAGIC_NUMBER line " + line + ": " + literal + " in `" + statement + "`");
            }
        }
        return found;
    }

    static boolean magic(String literal) {
        String digits = literal.replace("_", "").replaceAll("[lLfFdD]$", "");
        double value = digits.startsWith("0x") || digits.startsWith("0X") ? Long.parseLong(digits.substring(2), 16)
                : Double.parseDouble(digits);
        return Math.abs(value) > INDEX_ARITHMETIC_LIMIT || value != Math.rint(value);
    }

    @Test
    void theRuleMatchesM3Culprits() {
        assertEquals(List.of(), culprits("class A { static final int LIMIT = 64; int x = 2; int y = -1; }"));
        assertEquals(List.of(), culprits("class A { void f() { throw new IllegalStateException(\"Map is empty\"); } }"));
        assertEquals(List.of(), culprits("class A { String f() { return String.format(\"%d\", 3 > 2 ? 1 : 0).trim(); } }")
                .stream().filter(c -> c.startsWith("FLOATING")).toList());
        assertEquals(1, culprits("class A { int f(int a) { return a * 64; } }").size());
        assertEquals(1, culprits("class A { String f() { return \"(this Map)\"; } }").size());
        assertEquals(List.of(), culprits("class A { /* 64 \"x\" */ // 99 \"y\"\n char c = ','; String e = \"\"; }"));
    }

    @Test
    void theGeneratedMembersHaveNoMagicNumberAndNoFloatingString() throws Exception {
        Path base = Path.of(System.getProperty("user.dir"));
        Path implementations = base.resolve("../../implementations");
        List<String> all = new ArrayList<>();
        for (String table : new String[] {"MEMBERS.tsv", "ID_MEMBERS.tsv"}) {
            for (Map<String, String> member : M3ArrayMapFill.members(base.resolve(table))) {
                Path source = implementations.resolve(member.get("module")).resolve(M3ArrayMapFill.source(member));
                for (String culprit : culprits(Files.readString(source, StandardCharsets.UTF_8))) {
                    all.add(member.get("CLASS") + " " + culprit);
                }
            }
        }
        assertEquals(List.of(), all, "hoist every literal into a named constant (static final) of the template");
    }
}
