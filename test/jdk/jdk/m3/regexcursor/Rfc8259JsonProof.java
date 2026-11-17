// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.util.Arrays;
import java.util.Random;

/** RFC 8259 spec-image, lexical, structural and randomized valid document proof. */
public final class Rfc8259JsonProof {
    private static long checks;
    private Rfc8259JsonProof() { }

    public static void main(String[] args) {
        byte[] recompiled = Rfc8259JsonGrammar.compileReference().image();
        yes(Arrays.equals(recompiled, Rfc8259JsonGrammar.prepared().image()));
        yes("JSON".equals(Rfc8259JsonGrammar.prepared().language()));
        yes("RFC8259-2017".equals(Rfc8259JsonGrammar.prepared().revision()));
        for (String source : new String[] {
                "null", "true", "false", "0", "-0", "42", "-0.2", "1e2", "-12.5E-3",
                "\"\"", "\"simple\"", "\"hello\\nworld\"",
                "\"\\\\\\\"\\/\\b\\f\\n\\r\\t\"", "\"\\uD83D\\uDE03\"",
                "\"😀\"", "{}", "[]", "[1, 2, 3]", "{\"x\":1}",
                "{\"nested\":[true,null,{\"a\":\"b\"}]}", "[{}, [], 1]",
                " \r\n {\"a\":0,\"b\":\"x\",\"c\":[1,2]} \t",
        }) {
            yes(Rfc8259JsonGrammar.accepts(source));
            yes(Rfc8259JsonGrammar.acceptsStreaming(source));
        }
        for (String invalid : new String[] {
                "", " ", "+1", "01", "-01", "--1", "1.", "1e", "1e+", ".4", "-",
                "NaN", "Infinity", "False", "undefined", "none", "NULL", "-0x10",
                "[", "]", "{", "}", "{ } x", "[1,]", "[,]", "[1,,2]", "[1 2]",
                "{\"a\":}", "{a:1}", "{\"a\" 1}", "{\"a\":1,}",
                "{\"a\":1 \"b\":2}", "{\"a\":1,\"b\":2,}", "true false",
                "/* hi */true", "//hi\ntrue", "# comment", "true#",
                "\"bad\\q\"", "\"bad\\u12\"", "\"bad\\u12gg\"",
                "\"raw\nnewline\"", "\"raw\ttab\"", "\"unterminated",
                "\"bad\ud800\"", "\"bad\udc00\"", "[1,2]]", "[[1]", "1,2",
                "{\"x\":-}", "[+1]", "[01]", "{\"x\":.2}", "[1e]",
        }) {
            no(Rfc8259JsonGrammar.accepts(invalid));
            no(Rfc8259JsonGrammar.acceptsStreaming(invalid));
        }
        Random rng = new Random(0xB8259L);
        for (int trial = 0; trial < 25_000; trial++) {
            String document = randomJson(rng, 4);
            yes(Rfc8259JsonGrammar.accepts(document));
            yes(Rfc8259JsonGrammar.acceptsStreaming(document));
            no(Rfc8259JsonGrammar.accepts(document + " /* forbidden */"));
            no(Rfc8259JsonGrammar.acceptsStreaming(document + " /* forbidden */"));
            no(Rfc8259JsonGrammar.accepts("!" + document));
            no(Rfc8259JsonGrammar.acceptsStreaming("!" + document));
        }
        var read = Rfc8259JsonGrammar.prepared();
        var a = read.bind("[true, false, null, -0.125e+1]");
        yes(a.classify(1, 5) == Rfc8259JsonGrammar.TRUE);
        yes(a.classify(7, 12) == Rfc8259JsonGrammar.FALSE);
        yes(read.contextual(Rfc8259JsonGrammar.TRUE) == false);
        System.out.println("Rfc8259JsonProof PASS checks=" + checks);
    }

    private static String randomJson(Random random, int maxDepth) {
        int n = maxDepth == 0 ? random.nextInt(4) : random.nextInt(7);
        return switch (n) {
            case 0 -> "null";
            case 1 -> random.nextBoolean() ? "true" : "false";
            case 2 -> random.nextBoolean() ? "0" : "-" + (1 + random.nextInt(9999));
            case 3 -> random.nextInt(900) + "." + random.nextInt(999) + "e+" + random.nextInt(10);
            case 4 -> quoted(random);
            case 5 -> {
                StringBuilder arr = new StringBuilder("[");
                int length = random.nextInt(5);
                for (int i=0; i<length; i++) {
                    if (i != 0) arr.append(',');
                    arr.append(randomJson(random,maxDepth - 1));
                }
                yield arr.append(']').toString();
            }
            default -> {
                StringBuilder obj = new StringBuilder("{");
                int length = random.nextInt(5);
                for(int i=0;i<length;i++) {
                    if(i!=0) obj.append(',');
                    obj.append(quoted(random)).append(':').append(randomJson(random,maxDepth - 1));
                }
                yield obj.append('}').toString();
            }
        };
    }

    private static String quoted(Random random) {
        String[] units = {"a", "0", "\u20b9", "😀", "\\u000A", "\\n", "\\t",
                "\\\"", "\\\\", "\\/", "b", "c", " "};
        StringBuilder text = new StringBuilder("\"");
        int length = random.nextInt(6);
        for (int i = 0; i < length; i++) text.append(units[random.nextInt(units.length)]);
        return text.append('"').toString();
    }

    private static void yes(boolean value) {
        checks++;
        if (!value) throw new AssertionError("unexpected JSON reject at check " + checks);
    }
    private static void no(boolean value) { yes(!value); }
}
