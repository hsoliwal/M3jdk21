/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary The scanning String methods answer alike on every storage form of texts longer than a
 *          bulk window (512 units), with the deciding unit placed at the window seams
 * @run junit StringWindowSeamTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringWindowSeamTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings StringWindowSeamTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Texts of 505..520 and 1018..1030 units whose deciding unit (a surrogate pair, a lone
 * surrogate, a case-fold match, whitespace, a no-break space, an accent, a digit) sits at the
 * window seams. Each storage form (the whole String, a range of a longer String, a concatenation
 * of two halves, which are M3 storage when it is enabled) must answer as the flat String.
 */
public class StringWindowSeamTest {
    private static final String PAIR = new String(Character.toChars(0x1D400));

    static Stream<String> texts() {
        String[] marks = {"ZQX", PAIR, PAIR + "q", String.valueOf((char) 0xD835),
            String.valueOf((char) 0xDC00), "\t", "  ", String.valueOf((char) 0x00A0),
            String.valueOf((char) 0xE9), String.valueOf((char) 0x212A), "#", "1", "A"};
        List<String> texts = new ArrayList<>();
        for (int k = 505; k <= 1030; k += (k == 520 ? 498 : 1)) {
            for (String mark : marks) {
                texts.add("x".repeat(k) + mark);
                texts.add((char) 0x0436 + "x".repeat(k - 1) + mark + "y");
                texts.add("x ".repeat(k / 2) + mark + " y");
            }
        }
        return texts.stream();
    }

    private static List<Object> answers(String s) {
        return List.of(s.indexOfAny('#', (char) 0xE9), s.indexOfAny((char) 0xD835, (char) 0xDC00),
                s.indexOfAnyBut('x', 'y', ' '), s.containsOnly('x', ' ', 'y'),
                s.containsWhitespace(), s.indexOfIgnoreCase("zqx"), s.lastIndexOfIgnoreCase("k"),
                s.indexOfIgnoreCase(PAIR), s.lastIndexOfIgnoreCase("x#", 515),
                s.normalizeSpace(), s.stripAccents(), s.isAlpha(), s.isAlphaSpace(),
                s.isAlphanumeric(), s.isAsciiPrintable(), s.isAllLowerCase(), s.isMixedCase());
    }

    private static List<Function<String, String>> forms() {
        return List.of(
            text -> new String(("<<" + text + ">>").toCharArray()).substring(2, 2 + text.length()),
            text -> new String(text.toCharArray(), 0, text.length() / 2)
                    .concat(new String(text.toCharArray(), text.length() / 2,
                            text.length() - text.length() / 2)),
            text -> text.length() <= 511 ? new String(text.toCharArray())
                    : new String(text.toCharArray(), 0, 511)
                            .concat(new String(text.toCharArray(), 511, text.length() - 511)));
    }

    @ParameterizedTest(name = "{index}")
    @MethodSource("texts")
    void everyFormAnswersAsTheFlatString(String text) {
        List<Object> expected = answers(new String(text.toCharArray()));
        for (Function<String, String> form : forms()) {
            assertEquals(expected, answers(form.apply(text)));
        }
    }
}
