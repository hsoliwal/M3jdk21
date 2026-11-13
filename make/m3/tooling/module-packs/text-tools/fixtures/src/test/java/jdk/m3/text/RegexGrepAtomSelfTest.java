// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import jdk.m3.text.M3RegexLiteralPlan;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/** Exact original TextLine/ending and ordered Atom emission under Java/JNI modes. */
public final class RegexGrepAtomSelfTest {
    private static long checks;

    private RegexGrepAtomSelfTest() { }

    private static void same(Object left, Object right) {
        checks++;
        if (!left.equals(right)) throw new AssertionError("check=" + checks
                + " actual=" + left + " expected=" + right);
    }

    private static TextFrame run(RegexGrepAtom atom, TextFrame input, Limits limits) {
        FrameCollector output = new FrameCollector(limits);
        atom.execute(input, output);
        return output.frame();
    }

    private static TextFrame expected(TextFrame input, Pattern pattern, Limits limits) {
        FrameCollector out = new FrameCollector(limits);
        for (TextLine line : input.lines()) {
            if (pattern.matcher(line.content()).find()) out.accept(line);
        }
        return out.frame();
    }

    private static void probe(String regex, int flags, TextFrame frame, Limits limits) {
        TextFrame before = frame;
        Pattern pattern = Pattern.compile(regex, flags);
        TextFrame oracle = expected(frame, pattern, limits);
        RegexGrepAtom java = RegexGrepAtom.compile(regex, flags, limits, M3RegexLiteralPlan.Backend.JAVA);
        RegexGrepAtom auto = RegexGrepAtom.fromPattern(pattern, limits, M3RegexLiteralPlan.Backend.AUTO);
        same(run(java, frame, limits), oracle);
        same(run(auto, frame, limits), oracle);
        if (auto.precomputedSignals().exactLiteral()) {
            RegexGrepAtom nativeAtom = RegexGrepAtom.fromPattern(pattern, limits,
                    M3RegexLiteralPlan.Backend.NATIVE_REQUIRED);
            same(run(nativeAtom, frame, limits), oracle);
        } else {
            checks++;
            try {
                run(RegexGrepAtom.compile(regex, flags, limits,
                        M3RegexLiteralPlan.Backend.NATIVE_REQUIRED), frame, limits);
                throw new AssertionError("unsupported regex admitted to JNI");
            } catch (UnsupportedOperationException expected) { /* fail closed */ }
        }
        same(frame, before);
        same(frame.text(), before.text());
    }

    public static void main(String[] args) {
        Limits limits = Limits.standard();
        String raw = "public class A { String s = \"a.*\"; }\r\n"
                + "// JNIEXPORT jint Java_fake(...)\n"
                + "record R(int n) { R { if(n<0) throw new Error(); } }\r"
                + "String regex = \"\\\\Qx\\\\E\";\n"
                + "a+b\r\n中\r\n😀\n\n"
                + "public final class X { @Override public String toString() { return \"\"; }}";
        TextFrame frame = TextFrame.fromString(raw);
        same(frame.text(), raw);
        for (String regex : List.of("", "a+b", "public class", "中", "😀",
                "\\Q(?s).*\\E", "\\d+", ".*", "(?i)public", "(?<=a)b", "(a)\\1")) {
            probe(regex, 0, frame, limits);
        }
        for (int flag : new int[] {0, Pattern.LITERAL, Pattern.CASE_INSENSITIVE,
                Pattern.LITERAL | Pattern.CASE_INSENSITIVE}) {
            probe("public class", flag, frame, limits);
        }
        Random random = new Random(0x4c13610e89L);
        String[] atoms = {"a", "ab", "class", "[x]", "(?=a)", "\u0000", "中", "😀", "\"", "\\Q"};
        for (int iteration = 0; iteration < 400; iteration++) {
            StringBuilder input = new StringBuilder();
            int count = random.nextInt(36);
            for (int i = 0; i < count; i++) {
                input.append(atoms[random.nextInt(atoms.length)]);
                input.append(switch (random.nextInt(4)) { case 0 -> "\r\n"; case 1 -> "\n";
                    case 2 -> "\r"; default -> ""; });
            }
            TextFrame generated = TextFrame.fromString(input.toString());
            for (String expression : List.of("", "class", "😀", "\\Q[x]\\E", "[ax]", "(?=a)")) {
                probe(expression, 0, generated, limits);
            }
        }
        System.out.println("M3_UNIX_REGEX_ATOM_PASS checks=" + checks + " randomFrames=400");
    }
}
