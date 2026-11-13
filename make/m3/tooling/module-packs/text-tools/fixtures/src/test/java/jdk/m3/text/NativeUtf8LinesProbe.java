// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Native-only differential probe; ordinary CoreSelfTest does not require a JNI library. */
public final class NativeUtf8LinesProbe {
    private static long checks;

    private NativeUtf8LinesProbe() { }

    private static void check(boolean yes, String what) {
        checks++;
        if (!yes) throw new AssertionError(what);
    }

    private static ByteBuffer direct(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        ByteBuffer result = ByteBuffer.allocateDirect(bytes.length + 19);
        result.position(7);
        result.put(bytes);
        result.flip();
        result.position(7);
        return result;
    }

    private static List<TextLine> oracle(String input, String literal) {
        List<TextLine> result = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch != '\r' && ch != '\n') continue;
            String content = input.substring(start, i);
            String ending = String.valueOf(ch);
            if (ch == '\r' && i + 1 < input.length() && input.charAt(i + 1) == '\n') {
                ending = "\r\n";
                i++;
            }
            if (content.contains(literal)) result.add(new TextLine(content, ending));
            start = i + 1;
        }
        if (start < input.length()) {
            String content = input.substring(start);
            if (content.contains(literal)) result.add(new TextLine(content, ""));
        }
        return result;
    }

    private static void compare(String input, String literal) {
        List<TextLine> expected = oracle(input, literal);
        List<TextLine> actual = NativeUtf8Lines.grepLiteral(direct(input), literal, 100000, 16777216);
        check(actual.equals(expected), "native mismatch for literal=" + literal
                + " inputLen=" + input.length());
        check(TextTools.grepLiteral(direct(input), literal, Limits.standard()).lines().equals(expected),
                "TextTools owner mismatch");
    }

    private static void refused(Runnable action) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("invalid input was accepted");
    }

    public static void main(String[] args) {
        for (String input : List.of("", "\n", "\r\n", "\r", "a", "a\n",
                "a\r\nb\rc\n", "😀a\néअ\r\n\0", "\0abc\0", "ababa\r\nzz\r")) {
            for (String literal : List.of("", "a", "b", "ab", "aba", "😀", "अ", "\0"))
                compare(input, literal);
        }
        Random random = new Random(102943);
        for (int iteration = 0; iteration < 500; iteration++) {
            StringBuilder input = new StringBuilder();
            int lines = random.nextInt(80);
            for (int i = 0; i < lines; i++) {
                for (int count = random.nextInt(70); count > 0; count--) {
                    int scalar = switch (random.nextInt(7)) {
                        case 0 -> 'a';
                        case 1 -> '\0';
                        case 2 -> 0x1F600;
                        case 3 -> 'é';
                        case 4 -> 'अ';
                        case 5 -> '\uFFFF';
                        default -> 'z';
                    };
                    input.appendCodePoint(scalar);
                }
                if (random.nextBoolean())
                    input.append(random.nextBoolean() ? "\r\n" : random.nextBoolean() ? "\r" : "\n");
            }
            for (String literal : List.of("", "a", "😀", "अ", "az", "\0"))
                compare(input.toString(), literal);
        }
        StringBuilder many = new StringBuilder();
        for (int i = 0; i < 1000; i++) many.append("a").append(i).append("\n");
        compare("aaaaaaab\r\nbaaaaaab\n", "aaaaaaab"); // first KMP-length needle
        compare("aaaaaaa\r\nbaaaaaaa\n", "aaaaaaa"); // short memchr boundary
        compare("abcabcabcx\r\nabcabcabc\n", "abcabcabcx");
        compare("aaaaaaab\n", "aaaaaaaab"); // adversarial near match
        compare("Pattern.compile(\"a.*\")\r\npublic class A {}\n",
                "Pattern.compile(\"a.*\")"); // code-looking payload is still literal
        compare("😀😀😀😀😀😀😀😀\n", "😀😀😀😀");
        compare("\uFFFF\uFFFF\uFFFF\uFFFF\uFFFF\uFFFF\uFFFF\uFFFF\n",
                "\uFFFF\uFFFF\uFFFF\uFFFF");
        String repeated = "a".repeat(50_000);
        compare(repeated + "\n" + "a".repeat(4_095) + "b\n",
                "a".repeat(4_095) + "b"); // KMP near-match worst-case
        compare(repeated + "\r\n", "a".repeat(4_095) + "b"); // no false positive
        compare("a".repeat(65_536) + "\n", "a".repeat(65_536)); // largest bounded needle
        refused(() -> NativeUtf8Lines.grepLiteral(direct("a\n"),
                "a".repeat(65_537), 100, 100_000)); // max-size refusal
        // Confirm primitive KMP plan reuse across >256 matching records and framing boundaries.
        StringBuilder pages = new StringBuilder();
        for (int i = 0; i < 1026; i++) {
            pages.append(i).append(":Pattern.compile(\"a.*\")😀LONG-NEEDLE")
                    .append(i % 3 == 0 ? "\r\n" : i % 3 == 1 ? "\r" : "\n");
        }
        compare(pages.toString(), "compile(\"a.*\")😀LONG-NEEDLE");
        compare(pages.toString(), "NEEDLE");
        compare(pages.toString(), "not-a-real-pattern.*");
        compare(many.toString(), "a");
        refused(() -> NativeUtf8Lines.grepLiteral(direct("a\n"), "x\n", 100, 100));
        refused(() -> NativeUtf8Lines.grepLiteral(direct("a\n"), "a", 0, 100));
        refused(() -> NativeUtf8Lines.grepLiteral(direct("a\n"), "a", 100, 1));
        refused(() -> NativeUtf8Lines.grepLiteral(ByteBuffer.wrap(new byte[] {'a'}), "a", 100, 10));
        refused(() -> NativeUtf8Lines.grepLiteral(direct("a\n"), "\ud800", 100, 10));
        byte[][] malformed = {
            {(byte)0x80}, {(byte)0xc0, (byte)0x80},
            {(byte)0xed, (byte)0xa0, (byte)0x80},
            {(byte)0xf4, (byte)0x90, (byte)0x80, (byte)0x80},
            {(byte)0xe2, (byte)0x82}
        };
        for (byte[] bytes : malformed) {
            ByteBuffer input = ByteBuffer.allocateDirect(bytes.length).put(bytes).flip();
            refused(() -> NativeUtf8Lines.grepLiteral(input, "", 100, 100));
        }
        System.out.println("M3_NATIVE_UTF8_GREP_PASS checks=" + checks);
    }
}
