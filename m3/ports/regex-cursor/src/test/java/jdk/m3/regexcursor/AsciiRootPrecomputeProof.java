// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Deterministic, independently derived reference oracle for flat root dispatch. */
public final class AsciiRootPrecomputeProof {
    private AsciiRootPrecomputeProof() { }

    private static List<StaticGrammarLexicon.Literal> vocabulary(int asciiRoots) {
        List<StaticGrammarLexicon.Literal> rows = new ArrayList<>();
        for (int i = 0; i < asciiRoots; i++) {
            char first = (char) ('a' + i);
            rows.add(new StaticGrammarLexicon.Literal(first + "", i + 1, false));
            rows.add(new StaticGrammarLexicon.Literal(first + "long", asciiRoots + i + 1, (i & 1) == 0));
        }
        rows.add(new StaticGrammarLexicon.Literal("₹", asciiRoots * 2 + 1, false));
        rows.add(new StaticGrammarLexicon.Literal("😀", asciiRoots * 2 + 2, true));
        rows.add(new StaticGrammarLexicon.Literal("\uD800", asciiRoots * 2 + 3, false));
        rows.add(new StaticGrammarLexicon.Literal("\u0000", asciiRoots * 2 + 4, false));
        return rows;
    }

    private static int referenceExact(CharSequence source, int from, int to,
            List<StaticGrammarLexicon.Literal> rows) {
        for (var row : rows) {
            String word = row.spelling();
            if (to - from == word.length() && equalsAt(source, from, word)) return row.tokenId();
        }
        return -1;
    }

    private static long referenceLongest(CharSequence source, int from, int to,
            List<StaticGrammarLexicon.Literal> rows) {
        long best = -1L;
        int bestLength = -1;
        for (var row : rows) {
            String word = row.spelling();
            int size = word.length();
            if (size > bestLength && size <= to - from && equalsAt(source, from, word)) {
                bestLength = size;
                best = ((long) row.tokenId() << 32) | (from + size);
            }
        }
        return best;
    }

    private static boolean equalsAt(CharSequence chars, int from, String word) {
        for (int i = 0; i < word.length(); i++) {
            if (chars.charAt(from + i) != word.charAt(i)) return false;
        }
        return true;
    }

    private static void eq(long expected, long actual, String label) {
        if (expected != actual) throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
    }

    private static void required(boolean valid, String label) {
        if (!valid) throw new AssertionError(label);
    }

    private static int[] root(StaticGrammarLexicon lex) throws Exception {
        Field field = StaticGrammarLexicon.class.getDeclaredField("asciiRootTarget");
        field.setAccessible(true);
        return (int[]) field.get(lex);
    }

    public static void verify(int randomCases) throws Exception {
        long checks = 0;
        // The 512-byte lane must never be materialized for tiny vocabularies.
        for (int asciiRoots = 0; asciiRoots <= 26; asciiRoots++) {
            List<StaticGrammarLexicon.Literal> rows = vocabulary(asciiRoots);
            StaticGrammarLexicon lex = StaticGrammarLexicon.compile("java", "21", rows);
            int[] table = root(lex);
            required((table != null) == (asciiRoots + 1 >= 8), "threshold roots=" + asciiRoots); // includes NUL
            if (table != null) {
                eq(128, table.length, "ascii width");
                for (int c = 0; c < 128; c++) {
                    int actualId = table[c];
                    required(actualId == -1 || actualId > 0, "valid target root id");
                }
            }
            for (int c = 0; c <= 0xffff; c++) {
                String source = Character.toString((char) c);
                eq(referenceExact(source, 0, 1, rows), lex.exact(source, 0, 1),
                        "exhaustive one code unit: roots=" + asciiRoots + " c=" + c);
                checks++;
            }
        }
        List<StaticGrammarLexicon.Literal> rows = vocabulary(26);
        StaticGrammarLexicon lex = StaticGrammarLexicon.compile("java", "21", rows);
        String[] parts = {"a", "along", "z", "zlong", "₹", "😀", "\uD800", "\u0000", ">", "\uDBFF", "\uDC00", "unknown", ""};
        Random random = new Random(0x20261102L);
        for (int i = 0; i < randomCases; i++) {
            StringBuilder source = new StringBuilder("$");
            int words = random.nextInt(12);
            for (int j = 0; j < words; j++) source.append(parts[random.nextInt(parts.length)]);
            source.append('!');
            int from = random.nextInt(source.length() + 1);
            int to = from + random.nextInt(source.length() - from + 1);
            eq(referenceExact(source, from, to, rows), lex.exact(source, from, to), "random exact " + i);
            eq(referenceLongest(source, from, to, rows), lex.longest(source, from, to), "random longest " + i);
            checks += 2;
        }
        required(StaticGrammarLexicon.compile("java", "21", vocabulary(7)).grammarSha256()
                .equals(StaticGrammarLexicon.compile("java", "21", vocabulary(7)).grammarSha256()),
                "same grammar stable hash");
        System.out.println("M3_ASCII_ROOT_JAVA_PASS checks=" + checks + " rootThreshold=8");
    }

    /** Compare original JNI's sorted-array search against new Java root accelerator. */
    public static void verifyNative(String absoluteNativeLibrary, int cases) throws Exception {
        System.load(absoluteNativeLibrary);
        Method nativeExact = StaticGrammarLexiconNativeProof.class.getDeclaredMethod(
                "exactNative", StaticGrammarLexicon.class, String.class, int.class, int.class);
        nativeExact.setAccessible(true);
        List<StaticGrammarLexicon.Literal> rows = vocabulary(26);
        StaticGrammarLexicon grammar = StaticGrammarLexicon.compile("java", "21", rows);
        Random random = new Random(0xC17A2026L);
        String[] words = {"a", "along", "b", "bmore", "z", "zlong", "₹", "😀", "\uD800", "\u0000", "x", "42"};
        long checks = 0;
        for (int i = 0; i < cases; i++) {
            StringBuilder source = new StringBuilder("|");
            for (int j = random.nextInt(12); j >= 0; j--) source.append(words[random.nextInt(words.length)]);
            source.append('!');
            int from = random.nextInt(source.length() + 1);
            int to = from + random.nextInt(source.length() - from + 1);
            int actual = grammar.exact(source, from, to);
            int nativeId = (Integer) nativeExact.invoke(null, grammar, source.toString(), from, to);
            eq(actual, nativeId, "native exact " + i);
            checks++;
        }
        System.out.println("M3_ASCII_ROOT_JNI_PASS checks=" + checks);
    }

    public static void main(String[] args) throws Exception {
        verify(120_000);
        if (args.length == 1) verifyNative(args[0], 50_000);
        else if (args.length > 1) throw new IllegalArgumentException("one native library argument");
    }
}
