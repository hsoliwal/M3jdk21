/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary String range encoding preserves UTF-16, state, failures and ordinary I/O contracts
 * @run main M3StringIOTest
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class M3StringIOTest {
    private static int checks;

    private record Result(byte[] bytes, String failure, int step) { }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    private static Result encode(String text, boolean array, Charset cs,
                                 CodingErrorAction action, int chunk, boolean flush) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CharsetEncoder encoder = cs.newEncoder().onMalformedInput(action)
                .onUnmappableCharacter(action);
        OutputStreamWriter writer = new OutputStreamWriter(bytes, encoder);
        String framed = "!" + text + "?";
        char[] chars = framed.toCharArray();
        int step = 0;
        try {
            for (int i = 0; i < text.length(); i += chunk) {
                int count = Math.min(chunk, text.length() - i);
                step++;
                if (array) writer.write(chars, i + 1, count);
                else writer.write(framed, i + 1, count);
                // Especially important between the two halves of a surrogate pair.
                step++;
                if (array) writer.write(chars, i + 1, 0);
                else writer.write(framed, i + 1, 0);
                if (flush) { step++; writer.flush(); }
            }
            step++;
            writer.close();
            return new Result(bytes.toByteArray(), "", step);
        } catch (IOException ex) {
            Result failure = new Result(bytes.toByteArray(), ex.getClass().getName(), step);
            try { writer.close(); } catch (IOException ignored) { }
            return failure;
        }
    }

    private static void parity(String text, Charset cs, CodingErrorAction action,
                               int chunk, boolean flush) {
        Result expected = encode(text, true, cs, action, chunk, flush);
        Result actual = encode(text, false, cs, action, chunk, flush);
        check(Arrays.equals(expected.bytes, actual.bytes), "encoded bytes: " + cs);
        check(expected.failure.equals(actual.failure), "exception class: " + cs);
        check(expected.step == actual.step, "exception timing: " + cs);
    }

    private interface Throwing { void run() throws Exception; }

    private static Class<?> failure(Throwing op) {
        try { op.run(); return null; }
        catch (Exception ex) { return ex.getClass(); }
    }

    private static void validation() throws Exception {
        try (OutputStreamWriter out = new OutputStreamWriter(
                new ByteArrayOutputStream(), StandardCharsets.UTF_8)) {
            check(failure(() -> out.write((String) null, 0, -1))
                    == IndexOutOfBoundsException.class, "negative length before null");
            check(failure(() -> out.write((String) null, -1, 0))
                    == NullPointerException.class, "null before range");
            for (int[] range : new int[][]{{-1, 0}, {4, 0}, {2, 2}, {Integer.MAX_VALUE, 2}}) {
                check(failure(() -> out.write("abc", range[0], range[1]))
                        == StringIndexOutOfBoundsException.class, "range class");
            }
            out.write("abc", 3, 0);
        }
        OutputStreamWriter closed = new OutputStreamWriter(
                new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        closed.close();
        check(failure(() -> closed.write("abc", 0, 0)) == IOException.class, "closed empty");
        check(failure(() -> closed.write("abc", -1, 0))
                == StringIndexOutOfBoundsException.class, "range before closed");
        check(failure(() -> closed.write((String) null, 0, 0))
                == NullPointerException.class, "null before closed");
    }

    private static final class ProbeCharset extends Charset {
        ProbeCharset() { super("X-M3-STRING-IO-PROBE", null); }
        public boolean contains(Charset other) { return other == this; }
        public CharsetDecoder newDecoder() { throw new UnsupportedOperationException(); }
        public CharsetEncoder newEncoder() { return new ProbeEncoder(this); }
    }

    private static final class ProbeEncoder extends CharsetEncoder {
        int calls;
        boolean sawArray;
        boolean sawReadOnly;
        int initialPosition = -1;
        int initialCapacity = -1;
        ProbeEncoder(Charset cs) { super(cs, 1, 1); }
        @Override public boolean isLegalReplacement(byte[] replacement) { return true; }
        @Override protected CoderResult encodeLoop(CharBuffer input, ByteBuffer output) {
            calls++;
            if (input.hasRemaining()) {
                sawArray |= input.hasArray();
                sawReadOnly |= input.isReadOnly();
                if (initialPosition == -1) {
                    initialPosition = input.position();
                    initialCapacity = input.capacity();
                }
            }
            while (input.hasRemaining()) {
                if (!output.hasRemaining()) return CoderResult.OVERFLOW;
                char c = input.get(input.position());
                if (Character.isHighSurrogate(c) && input.remaining() == 1)
                    return CoderResult.UNDERFLOW;
                output.put((byte) input.get());
            }
            return CoderResult.UNDERFLOW;
        }
    }

    private static void directInput(boolean expectDirect) throws Exception {
        ProbeEncoder probe = new ProbeEncoder(new ProbeCharset());
        probe.onMalformedInput(CodingErrorAction.REPLACE);
        try (OutputStreamWriter writer = new OutputStreamWriter(new ByteArrayOutputStream(), probe)) {
            writer.write("!ABC?", 1, 3);
            check(probe.initialPosition == 0 && probe.initialCapacity == 3, "range confinement");
            if (expectDirect) {
                check(!probe.sawArray && probe.sawReadOnly, "no full String char[] input shadow");
            } else {
                check(probe.sawArray, "baseline really used a char[]");
            }
            writer.write("\ud800", 0, 1);
            int before = probe.calls;
            writer.write("abc", 1, 0);
            check(probe.calls == before, "empty write must not enter encoder");
        }
    }

    private static void readerWriterCompatibility() throws Exception {
        String text = "prefix\u0000\ud83d\ude00\r\nend";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (StringReader reader = new StringReader(text);
             OutputStreamWriter writer = new OutputStreamWriter(bytes, StandardCharsets.UTF_8)) {
            check(reader.transferTo(writer) == text.length(), "Reader -> Writer count");
        }
        check(Arrays.equals(bytes.toByteArray(), text.getBytes(StandardCharsets.UTF_8)), "I/O pipeline");
        StringWriter writer = new StringWriter();
        writer.write("a");
        StringBuffer live = writer.getBuffer();
        live.append('b');
        writer.close();
        writer.write('c');
        check(writer.getBuffer() == live && writer.toString().equals("abc"), "StringWriter live buffer");
        StringReader reader = new StringReader(text);
        reader.skip(3);
        reader.mark(0);
        check(reader.skip(-2) == -2 && reader.read() == text.charAt(1), "backward skip");
        reader.reset();
        check(reader.read() == text.charAt(3), "mark/reset");
        reader.close();
        check(failure(reader::read) == IOException.class, "reader close");
    }

    public static void main(String[] args) throws Exception {
        boolean expectDirect = args.length == 0 || !args[0].equals("baseline");
        validation();
        directInput(expectDirect);
        readerWriterCompatibility();
        String[] fixed = {"", "ascii", "\u0000\u00ff\u0100", "\ud800", "\udc00",
                "x\ud83d\ude00y", "\ud800\ud800\udc00\udc00", "\r\n\u2028\u2029", "日本語"};
        String[] names = {"UTF-8", "UTF-16", "UTF-16BE", "UTF-16LE", "US-ASCII",
                "ISO-8859-1", "ISO-2022-JP", "CESU-8"};
        CodingErrorAction[] actions = {CodingErrorAction.REPLACE, CodingErrorAction.IGNORE,
                CodingErrorAction.REPORT};
        for (String name : names) {
            Charset cs = Charset.forName(name);
            for (String text : fixed)
                for (CodingErrorAction action : actions)
                    for (int chunk : new int[]{1, 2, 7, 8192})
                        for (boolean flush : new boolean[]{false, true})
                            parity(text, cs, action, chunk, flush);
        }
        Random random = new Random(0x4d33494fL);
        for (int sample = 0; sample < 256; sample++) {
            char[] chars = new char[random.nextInt(100)];
            for (int i = 0; i < chars.length; i++) chars[i] = (char) random.nextInt(65536);
            for (String name : names)
                for (CodingErrorAction action : actions)
                    parity(new String(chars), Charset.forName(name), action,
                            1 + random.nextInt(17), (sample & 1) == 0);
        }
        String large = "abc\u0100\ud83d\ude00".repeat(20000);
        parity(large, StandardCharsets.UTF_8, CodingErrorAction.REPLACE, large.length(), false);
        Throwable[] failure = new Throwable[1];
        Thread virtual = Thread.ofVirtual().start(() -> {
            try { directInput(expectDirect); }
            catch (Throwable ex) { failure[0] = ex; }
        });
        virtual.join();
        check(failure[0] == null, "virtual-thread writer: " + failure[0]);
        System.out.println("M3_STRING_IO_PASS checks=" + checks + " direct=" + expectDirect);
    }
}
