/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JUnit coverage of the String operations changed for M3 storage, in every storage form
 * @modules java.base/java.lang:+open
 * @run junit M3StringLeavesTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      M3StringLeavesTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings M3StringLeavesTest
 */

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * One test per operation, each run over the same texts in four storage forms: a whole String, a
 * range of a longer String, a concatenation of two halves, and a flat peer. Every expectation is
 * computed from the char array the forms were built from, never from another String operation
 * under test.
 */
public class M3StringLeavesTest {
    private static final char HIGH = (char) 0xD83D;
    private static final char LOW = (char) 0xDE42;
    private static final List<Charset> CHARSETS = List.of(
            StandardCharsets.ISO_8859_1, StandardCharsets.US_ASCII, StandardCharsets.UTF_8);

    private static List<char[]> texts() {
        List<char[]> texts = new ArrayList<>(List.of(
                new char[0], "a".toCharArray(), "  padded\ttext \n".toCharArray(),
                "one\r\ntwo\nthree\rfour".toCharArray(), "\r\n\r\n".toCharArray(),
                ("caf" + (char) 0xE9 + " cr" + (char) 0xE8 + "me").toCharArray(),
                ("wide " + (char) 0x3B1 + " text").toCharArray(),
                ("pair " + HIGH + LOW + " lone " + HIGH + " end " + LOW).toCharArray(),
                (" " + (char) 9 + (char) 0x2028 + " ").toCharArray(), "x,y,,z,".toCharArray()));
        texts.add(generated(5_000, 0));
        texts.add(generated(40_000, 1));
        return texts;
    }

    private static char[] generated(int length, int kind) {
        char[] units = new char[length];
        IntStream.range(0, length).forEach(index -> units[index] = index % 31 == 0 ? ','
                : kind == 1 && index % 97 == 0 ? (char) 0x3B1 : (char) ('a' + index % 26));
        return units;
    }

    static Stream<Arguments> forms() throws Exception {
        List<Arguments> forms = new ArrayList<>();
        for (char[] units : texts()) {
            String whole = new String(units);
            int middle = units.length / 2;
            forms.add(Arguments.of("whole", whole, units));
            forms.add(Arguments.of("range",
                    new String(("<<" + whole + ">>").toCharArray()).substring(2, 2 + units.length),
                    units));
            forms.add(Arguments.of("halves", new String(units, 0, middle)
                    .concat(new String(units, middle, units.length - middle)), units));
            forms.add(Arguments.of("flat", flat(units), units));
        }
        return forms.stream();
    }

    @ParameterizedTest(name = "{0} of {2}")
    @MethodSource("forms")
    void charAtAndLength(String form, String subject, char[] units) {
        assertEquals(units.length, subject.length(), form);
        char[] read = new char[units.length];
        IntStream.range(0, units.length).forEach(index -> read[index] = subject.charAt(index));
        assertArrayEquals(units, read, form);
    }

    @ParameterizedTest(name = "{0} of {2}")
    @MethodSource("forms")
    void lines(String form, String subject, char[] units) {
        List<String> expected = new ArrayList<>();
        int start = 0;
        for (int index = 0; index < units.length; index++) {
            if (units[index] != '\n' && units[index] != '\r') continue;
            expected.add(new String(units, start, index - start));
            boolean pair = units[index] == '\r' && index + 1 < units.length
                    && units[index + 1] == '\n';
            if (pair) index++;
            start = index + 1;
        }
        if (start < units.length) expected.add(new String(units, start, units.length - start));
        assertEquals(expected, subject.lines().toList(), form);
    }

    @ParameterizedTest(name = "{0} of {2}")
    @MethodSource("forms")
    void codePointCount(String form, String subject, char[] units) {
        int step = Math.max(1, units.length / 50);
        IntStream.iterate(0, begin -> begin <= units.length, begin -> begin + step)
                .forEach(begin -> assertEquals(
                        Character.codePointCount(units, begin, units.length - begin),
                        subject.codePointCount(begin, units.length), form + " from " + begin));
    }

    @ParameterizedTest(name = "{0} of {2}")
    @MethodSource("forms")
    void builderAppends(String form, String subject, char[] units) {
        for (String prefix : List.of("", "p", "wide" + (char) 0x20AC)) {
            int end = Math.min(units.length, 40);
            String expected = prefix + new String(units) + new String(units, 0, end);
            assertEquals(expected, chars(new StringBuilder(prefix).append(subject)
                    .append(subject, 0, end).toString()), form);
            assertEquals(expected, chars(new StringBuffer(prefix).append(subject)
                    .append(subject, 0, end).toString()), form);
        }
    }

    @ParameterizedTest(name = "{0} of {2}")
    @MethodSource("forms")
    void encodes(String form, String subject, char[] units) throws IOException {
        for (Charset charset : CHARSETS) {
            byte[] expected = encode(charset, units);
            assertArrayEquals(expected, subject.getBytes(charset), form + " " + charset);
            ByteArrayOutputStream sink = new ByteArrayOutputStream();
            try (Writer writer = new OutputStreamWriter(sink, charset)) {
                writer.write(subject);
            }
            assertArrayEquals(expected, sink.toByteArray(), form + " writer " + charset);
        }
    }

    @ParameterizedTest(name = "{0} of {2}")
    @MethodSource("forms")
    void prefixesAndSuffixes(String form, String subject, char[] units) throws Exception {
        for (int size : new int[] {0, 1, 4, 70}) {
            if (size > units.length) continue;
            String prefix = new String(units, 0, size);
            String suffix = new String(units, units.length - size, size);
            assertEquals(true, subject.startsWith(prefix), form + " prefix " + size);
            assertEquals(true, subject.endsWith(suffix), form + " suffix " + size);
            assertEquals(true, flat(units).startsWith(subject.substring(0, size)), form);
            assertEquals(false, subject.startsWith(prefix + (char) 1), form);
        }
    }

    @ParameterizedTest(name = "{0} of {2}")
    @MethodSource("forms")
    void whiteSpaceEdges(String form, String subject, char[] units) {
        int start = 0;
        while (start < units.length && Character.isWhitespace(units[start])) start++;
        int end = units.length;
        while (end > 0 && Character.isWhitespace(units[end - 1])) end--;
        assertEquals(new String(units, Math.min(start, end), Math.max(0, end - start)),
                chars(subject.strip()), form);
        assertEquals(new String(units, start, units.length - start),
                chars(subject.stripLeading()), form);
        assertEquals(new String(units, 0, end), chars(subject.stripTrailing()), form);
        assertEquals(start == units.length, subject.isBlank(), form);
        int from = 0;
        while (from < units.length && units[from] <= ' ') from++;
        int to = units.length;
        while (to > 0 && units[to - 1] <= ' ') to--;
        assertEquals(new String(units, Math.min(from, to), Math.max(0, to - from)),
                chars(subject.trim()), form);
    }

    @ParameterizedTest(name = "{0} of {2}")
    @MethodSource("forms")
    void unitSearchAndSplit(String form, String subject, char[] units) {
        List<Integer> hits = IntStream.range(0, units.length).filter(i -> units[i] == ',')
                .boxed().toList();
        List<Integer> found = new ArrayList<>();
        for (int at = subject.indexOf(','); at >= 0; at = subject.indexOf(',', at + 1)) {
            found.add(at);
        }
        assertEquals(hits, found, form);
        int last = hits.isEmpty() ? -1 : hits.get(hits.size() - 1);
        assertEquals(last, subject.lastIndexOf(','), form);
        assertEquals(-1, subject.indexOf((char) 2), form);
        assertEquals(hits.size() + 1, subject.split(",", -1).length, form);
    }

    @ParameterizedTest(name = "{0} of {2}")
    @MethodSource("forms")
    void regexArguments(String form, String subject, char[] units) {
        for (String regex : List.of(",", "[a-c]+", "\\s+", "x|y", "(\\w)\\1")) {
            Pattern fresh = Pattern.compile(regex);
            for (int round = 0; round < 2; round++) {
                assertEquals(fresh.matcher(subject).matches(), subject.matches(regex), form);
                assertEquals(fresh.matcher(subject).replaceAll("#"),
                        subject.replaceAll(regex, "#"), form);
                assertEquals(fresh.matcher(subject).replaceFirst("#"),
                        subject.replaceFirst(regex, "#"), form);
                assertArrayEquals(fresh.split(subject, -1), subject.split(regex, -1), form);
            }
        }
    }

    /** A copy read unit by unit, so a comparison does not go through equals of the subject. */
    private static String chars(String value) {
        char[] units = new char[value.length()];
        IntStream.range(0, units.length).forEach(index -> units[index] = value.charAt(index));
        return String.valueOf(units);
    }

    private static byte[] encode(Charset charset, char[] units) throws IOException {
        ByteBuffer encoded = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .encode(CharBuffer.wrap(units));
        return Arrays.copyOfRange(encoded.array(), 0, encoded.limit());
    }

    /** The per-thread admission guard gives a flat peer, as a bootstrap or refused String is. */
    @SuppressWarnings("unchecked")
    private static String flat(char[] units) throws Exception {
        Field field = optionalField("java.lang.M3String", "ADMITTING");
        if (field == null) return new String(units);
        ThreadLocal<Boolean> admitting = (ThreadLocal<Boolean>) field.get(null);
        Boolean previous = admitting.get();
        admitting.set(Boolean.TRUE);
        try {
            return new String(units);
        } finally {
            if (previous == null) admitting.remove();
            else admitting.set(previous);
        }
    }

    /** Null on a VM without the M3 String owners, so the tests also run with storage off. */
    private static Field optionalField(String owner, String name) {
        try {
            Field field = Class.forName(owner).getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ClassNotFoundException | NoSuchFieldException absent) {
            return null;
        }
    }
}
