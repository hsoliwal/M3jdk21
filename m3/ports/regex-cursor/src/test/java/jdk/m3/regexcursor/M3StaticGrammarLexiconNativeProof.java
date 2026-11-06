// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.util.List;
import java.util.Random;

/** JNI only as independent test oracle; no production native dependency or JNI ABI. */
public final class M3StaticGrammarLexiconNativeProof {
    private M3StaticGrammarLexiconNativeProof() { }

    private static native int exactNative(M3StaticGrammarLexicon grammar, String source, int from, int to);

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("native library absolute path required");
        System.load(args[0]);
        List<M3StaticGrammarLexicon.Literal> rows = M3StaticGrammarLexiconProof.literals();
        M3StaticGrammarLexicon lex = M3StaticGrammarLexicon.compile("java", "21-jls3", rows);
        String[] cases = {"", ">", ">>", ">>>", ">>>>", "for", "forbidden", "record",
                "non-sealed", "₹", "😀", "a\ud800", "\\u0066or", "or", "_"};
        int checked = 0;
        for (String str : cases) {
            for (int from = 0; from <= str.length(); from++) {
                for (int to = from; to <= str.length(); to++) {
                    M3StaticGrammarLexiconProof.eq(lex.exact(str, from, to),
                            exactNative(lex, str, from, to), "JNI case=" + checked);
                    checked++;
                }
            }
        }
        Random rng = new Random(0xA77A21L);
        String[] alphabet = {"a", "b", "f", "o", "r", ">", "₹", "😀", "_", "\ud800"};
        for (int round = 0; round < 30_000; round++) {
            StringBuilder builder = new StringBuilder();
            for (int k = rng.nextInt(25); k > 0; k--) builder.append(alphabet[rng.nextInt(alphabet.length)]);
            String source = builder.toString();
            int from = rng.nextInt(source.length() + 1);
            int to = from + rng.nextInt(source.length() - from + 1);
            M3StaticGrammarLexiconProof.eq(lex.exact(source, from, to),
                    exactNative(lex, source, from, to), "JNI randomized=" + round);
            checked++;
        }
        System.out.println("M3_STATIC_GRAMMAR_JNI_PASS checks=" + checked);
    }
}
