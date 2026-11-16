/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary OutputStreamWriter.write(String) encodes long Strings through char windows exactly
 * @run main WriteStringWindows
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      WriteStringWindows
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * A String is written through windows of 8,192 chars. The bytes must be those of one contiguous
 * encode: a surrogate pair cut by a window edge, a lone surrogate at the edge, a high surrogate
 * left over from an earlier write, and ranges of a longer String. The oracle encodes the char
 * array with a CharsetEncoder and never goes through String.
 */
public class WriteStringWindows {
    private static final int WINDOW = 8192;
    private static final char HIGH = (char) 0xD83D;
    private static final char LOW = (char) 0xDE42;
    private static final List<String> CHARSETS =
            List.of("UTF-8", "ISO-8859-1", "US-ASCII", "UTF-16", "UTF-16LE");
    private static final List<Integer> LENGTHS =
            List.of(0, 1, 2, WINDOW - 1, WINDOW, WINDOW + 1, 2 * WINDOW + 1, 20_000);
    private static final List<Integer> MARKS = List.of(-1, 0, WINDOW - 1, WINDOW, 2 * WINDOW - 1);
    private static int checks;

    public static void main(String[] args) throws IOException {
        for (String name : CHARSETS) {
            Charset charset = Charset.forName(name);
            for (int length : LENGTHS) {
                for (int mark : MARKS) {
                    check(charset, units(length, mark, HIGH, LOW));
                    check(charset, units(length, mark, HIGH, 'x'));
                    check(charset, units(length, mark, LOW, (char) 0xE9));
                }
            }
        }
        System.out.println("PASS checks=" + checks);
    }

    /** Letters, with the two given units at {@code mark} and {@code mark + 1} when they fit. */
    private static char[] units(int length, int mark, char first, char second) {
        char[] units = new char[length];
        IntStream.range(0, length).forEach(index -> units[index] = (char) ('a' + index % 26));
        if (mark >= 0 && mark < length) units[mark] = first;
        if (mark >= 0 && mark + 1 < length) units[mark + 1] = second;
        return units;
    }

    private static void check(Charset charset, char[] units) throws IOException {
        String whole = new String(units);
        String framed = new String(("<" + whole + ">").toCharArray());
        same(charset, units, writer -> writer.write(whole));
        same(charset, units, writer -> writer.write(framed, 1, units.length));
        same(charset, units, writer -> writer.write(framed.substring(1, 1 + units.length)));
        same(charset, units, writer -> {
            int middle = units.length / 2;
            writer.write(whole, 0, middle);
            writer.write("");
            writer.write(whole.substring(middle).concat(""));
        });
        char[] paired = (HIGH + String.valueOf(LOW) + whole).toCharArray();
        same(charset, paired, writer -> {
            writer.write(HIGH);
            writer.write(LOW + whole);
        });
    }

    private interface Writes {
        void to(Writer writer) throws IOException;
    }

    private static void same(Charset charset, char[] units, Writes writes) throws IOException {
        checks++;
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(sink, charset)) {
            writes.to(writer);
        }
        ByteBuffer encoded = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .encode(CharBuffer.wrap(units));
        byte[] expected = Arrays.copyOfRange(encoded.array(), 0, encoded.limit());
        if (!Arrays.equals(sink.toByteArray(), expected)) {
            throw new AssertionError(charset + ": " + units.length + " chars gave "
                    + sink.size() + " bytes, expected " + expected.length);
        }
    }
}
