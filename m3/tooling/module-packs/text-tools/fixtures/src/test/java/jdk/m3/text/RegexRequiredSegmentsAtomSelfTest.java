// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import jdk.m3.text.M3RegexRequiredSegments;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** End-to-end Atom/FrameCollector/native selected-ordinal contract verification. */
public final class RegexRequiredSegmentsAtomSelfTest {
    private static long checks;
    private RegexRequiredSegmentsAtomSelfTest() { }

    private static void check(boolean truth, String why) {
        checks++;
        if (!truth) throw new AssertionError(why);
    }
    private static void equal(Object actual, Object expected, String why) {
        check(actual.equals(expected), why + " actual=" + actual + " expected=" + expected);
    }
    private static List<TextLine> run(RegexRequiredSegmentsAtom atom, TextFrame frame) {
        List<TextLine> output = new ArrayList<>();
        atom.execute(frame, output::add);
        return List.copyOf(output);
    }
    private static void reject(Runnable action, String why) {
        checks++;
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected budget rejection: " + why);
    }

    public static long run() {
        Limits limits = Limits.standard();
        String value = "ab\r\na\nb\racb\npublic class R{}\r\na\u0000b\n"
                + "a🙂b\r\nabe\n";
        TextFrame frame = TextFrame.fromString(value);
        String frameHash = frame.sha256();
        RegexRequiredSegmentsAtom javaAtom = RegexRequiredSegmentsAtom.compile("^a.*b$", 0,
                limits, M3RegexRequiredSegments.Backend.JAVA);
        RegexRequiredSegmentsAtom jniAtom = RegexRequiredSegmentsAtom.compile("^a.*b$", 0,
                limits, M3RegexRequiredSegments.Backend.NATIVE_REQUIRED);
        List<TextLine> expected = run(javaAtom, frame);
        List<TextLine> selected = run(jniAtom, frame);
        equal(selected, expected, "native and Java Atom produce identical rows/endings");
        equal(frame.sha256(), frameHash, "source frame preserved by native selection");
        check(jniAtom.precomputedFacts().proven(), "static signal available");
        check(jniAtom.precomputedFacts().mandatoryCount() == 2, "two native mask passes");
        check(expected.size() >= 3, "meaningful native positive fixtures");
        for (TextLine output : selected) {
            check(frame.lines().stream().anyMatch(original -> original == output),
                    "line retains original M3 String/ending owner");
        }
        RegexRequiredSegmentsAtom nonNative = RegexRequiredSegmentsAtom.compile("a|b", 0,
                limits, M3RegexRequiredSegments.Backend.AUTO);
        equal(run(nonNative, frame), frame.lines().stream()
                .filter(line -> java.util.regex.Pattern.compile("a|b")
                        .matcher(line.content()).find()).toList(), "unproven regex Java oracle");
        Limits restricted = new Limits(10, 10, frame.lines().size() - 1, limits.maxBytes());
        RegexRequiredSegmentsAtom budget = RegexRequiredSegmentsAtom.compile("a.*b", 0,
                restricted, M3RegexRequiredSegments.Backend.JAVA);
        List<TextLine> delivered = new ArrayList<>();
        reject(() -> budget.execute(frame, delivered::add), "input budget");
        equal(delivered, List.of(), "no output callback on input budget refusal");
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<List<TextLine>>> tasks = new ArrayList<>();
            for (int i = 0; i < 48; i++) tasks.add(() -> run(jniAtom, frame));
            List<Future<List<TextLine>>> futures = executor.invokeAll(tasks);
            for (Future<List<TextLine>> future : futures) {
                equal(future.get(), expected, "virtual-thread native fanout/fanin");
            }
        } catch (Exception error) {
            throw new AssertionError("native virtual-thread fanout", error);
        }
        System.out.println("M3_REQUIRED_SEGMENTS_ATOM_PASS checks=" + checks);
        return checks;
    }

    public static void main(String[] args) { run(); }
}
