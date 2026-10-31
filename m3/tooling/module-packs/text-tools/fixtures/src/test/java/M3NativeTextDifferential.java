// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;
import jdk.m3.text.TextLine;
import jdk.m3.text.TextTools;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Seeded, deterministic contract comparison of native and Java line ordering/filtering. */
public final class M3NativeTextDifferential {
    private static final Limits LIMITS = Limits.standard();
    private static final String[] SAMPLES = {
        "", "a", "A", "AA", "Ä", "é", "e\u0301", "😀", "🦋", "\uE000", "\u0000",
        "z", "a😀", "abc", "acb", "a".repeat(600) + "X", "a".repeat(600) + "Y"
    };

    private M3NativeTextDifferential() { }

    private static void same(Object expected, Object actual) {
        if (!expected.equals(actual))
            throw new AssertionError("native text parity: " + expected + " != " + actual);
    }

    private static void verify(List<TextLine> lines) {
        TextFrame frame = new TextFrame(lines);
        for (boolean reverse : new boolean[]{false, true}) {
            List<TextLine> expected = new ArrayList<>(lines);
            Comparator<TextLine> cmp = Comparator.comparing(TextLine::content);
            expected.sort(reverse ? cmp.reversed() : cmp);
            same(expected, TextTools.sort(frame, reverse, LIMITS).lines());
        }
        List<TextLine> adjacent = new ArrayList<>();
        String previous = null;
        for (TextLine line : lines) {
            if (!line.content().equals(previous)) adjacent.add(line);
            previous = line.content();
        }
        same(adjacent, TextTools.uniq(frame, LIMITS).lines());
    }

    public static void main(String[] args) {
        same(true, TextTools.nativeTextAvailable());
        Random random = new Random(0x4D33554EL);
        int runs = 0;
        for (int n : new int[]{0, 1, 2, 3, 15, 32, 63, 64, 65, 127, 128, 129, 255, 256, 257}) {
            for (int sample = 0; sample < 30; sample++) {
                List<TextLine> lines = new ArrayList<>(n);
                for (int i = 0; i < n; i++)
                    lines.add(new TextLine(SAMPLES[random.nextInt(SAMPLES.length)], "\n"));
                verify(lines);
                runs++;
            }
        }
        System.out.println("M3_NATIVE_TEXT_PARITY_PASS runs=" + runs);
    }
}
