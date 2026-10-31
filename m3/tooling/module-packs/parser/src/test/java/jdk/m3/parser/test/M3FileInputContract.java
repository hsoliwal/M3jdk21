/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HexFormat;
import jdk.m3.parser.M3File;

/** Owned existing-input admission; reflection also makes a missing API a behavioral baseline refusal. */
public final class M3FileInputContract {
    private static final URI SOURCE = URI.create("zip:file:/fixture.zip!/src/%E2%82%AC.java");
    private static final String TEXT = "A\u00e9\ud83d\ude00\r\n";
    private static final byte[] UTF8 = HexFormat.of().parseHex("41c3a9f09f98800d0a");
    private static final String RAW_HASH = "d66c3a22327d97cd2dc833c69ae055002e732223fa218683eab348f89113ad06";
    private static final String UTF16_HASH = "224ba9c56cc593c4028636ec8401f33a42d8846bb57c6a7b5d8d35bfe66c9756";
    private static final String EMPTY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static Method inputReader;
    private static int checks;
    private static int groups;

    private M3FileInputContract() { }

    public static void main(String[] arguments) throws Exception {
        try {
            inputReader = M3File.class.getMethod("read", URI.class, InputStream.class, Charset.class, int.class);
        } catch (NoSuchMethodException absent) {
            throw new AssertionError("missing M3File.read(URI, InputStream, Charset, int) owned-input API", absent);
        }
        exactIdentityFromOneExistingFileStream();
        emptyExactAndOversizedBounds();
        setupRefusalStillOwnsClose();
        strictDecodeAndCharsetBomPolicy();
        readAndCloseFailureOwnership();
        identicalFailureCannotSelfSuppress();
        decoderSetupOccursAfterClose();
        unchangedPathParityAndIndependentReads();
        System.out.println("M3_FILE_INPUT_PASS checks=" + checks + " groups=" + groups);
    }

    private static void exactIdentityFromOneExistingFileStream() throws Exception {
        groups++;
        Path file = work().resolve("existing-input-\u00e9.bin");
        Files.write(file, UTF8, StandardOpenOption.CREATE_NEW);
        TrackedInput input = new TrackedInput(Files.newInputStream(file));
        input.maximumChunk = 1;
        M3File admitted = read(SOURCE, input, StandardCharsets.UTF_8, UTF8.length);
        check(admitted.uri().equals(SOURCE), "caller URI is retained, without reopening or normalization");
        check(admitted.text().equals(TEXT) && admitted.length() == 6, "supplementary characters and CRLF retain exact UTF-16");
        check(admitted.charAt(2) == '\ud83d' && admitted.charAt(3) == '\ude00', "supplementary pair positions remain exact");
        check(admitted.byteSha256().orElseThrow().equals(RAW_HASH), "raw SHA-256 covers the single byte sequence");
        check(admitted.utf16Sha256().equals(UTF16_HASH), "UTF-16BE SHA is independently bound to decoded text");
        check(input.delivered == 9 && input.eof, "exact budget still probes EOF once bytes are consumed");
        check(input.closes == 1 && input.closed, "successful input is closed exactly once");
        check(input.markCalls == 0 && input.resetCalls == 0 && input.skipped == 0, "admission never replays or skips the primary input");
        Files.writeString(file, "changed", StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        check(admitted.text().equals(TEXT) && admitted.byteSha256().orElseThrow().equals(RAW_HASH), "retained source is independent of the closed stream and later disk edit");
    }

    private static void emptyExactAndOversizedBounds() throws Exception {
        groups++;
        TrackedInput empty = bytes("");
        M3File zero = read(SOURCE, empty, StandardCharsets.UTF_8, 0);
        check(zero.length() == 0 && zero.byteSha256().orElseThrow().equals(EMPTY_HASH), "empty input has a real empty-byte identity");
        check(zero.utf16Sha256().equals(EMPTY_HASH) && empty.eof && empty.closes == 1, "zero budget accepts only EOF and still closes");

        TrackedInput exact = bytes("616263");
        M3File abc = read(SOURCE, exact, StandardCharsets.UTF_8, 3);
        check(abc.text().equals("abc") && exact.delivered == 3 && exact.eof && exact.closes == 1, "exact limit accepts complete input");

        TrackedInput oversize = bytes("6162636465666768");
        IOException tooLarge = expect(IOException.class, () -> read(SOURCE, oversize, StandardCharsets.UTF_8, 2));
        check(tooLarge.getMessage().contains("maxBytes=2"), "size refusal identifies the configured budget");
        check(oversize.delivered == 3 && !oversize.eof, "size refusal consumes only the one-byte sentinel beyond the budget");
        check(oversize.closes == 1, "oversized source closes exactly once");

        TrackedInput one = bytes("61");
        expect(IOException.class, () -> read(SOURCE, one, StandardCharsets.UTF_8, 0));
        check(one.delivered == 1 && one.closes == 1, "nonempty input fails zero budget after only the sentinel byte");
    }

    private static void setupRefusalStillOwnsClose() throws Exception {
        groups++;
        TrackedInput nullUri = bytes("61");
        expect(NullPointerException.class, () -> read(null, nullUri, StandardCharsets.UTF_8, 1));
        check(nullUri.reads == 0 && nullUri.closes == 1, "null URI refuses before reads but releases transferred ownership");
        TrackedInput nullCharset = bytes("61");
        expect(NullPointerException.class, () -> read(SOURCE, nullCharset, null, 1));
        check(nullCharset.reads == 0 && nullCharset.closes == 1, "null charset refuses before reads but still closes");
        for (int invalid : new int[] {-1, Integer.MAX_VALUE - 7, Integer.MAX_VALUE}) {
            TrackedInput input = bytes("61");
            expect(IllegalArgumentException.class, () -> read(SOURCE, input, StandardCharsets.UTF_8, invalid));
            check(input.reads == 0 && input.closes == 1, "unsupported array bound closes without reading: " + invalid);
        }
        expect(NullPointerException.class, () -> read(SOURCE, null, StandardCharsets.UTF_8, 1));
        TrackedInput cleanup = bytes("61");
        IOException closeFailure = new IOException("setup cleanup");
        cleanup.closeFailure = closeFailure;
        NullPointerException primary = expect(NullPointerException.class, () -> read(null, cleanup, StandardCharsets.UTF_8, 1));
        check(primary.getSuppressed().length == 1 && primary.getSuppressed()[0] == closeFailure,
                "setup failure remains primary and retains cleanup failure");
        check(cleanup.closes == 1 && cleanup.reads == 0, "failed setup cleanup is not retried");
    }

    private static void strictDecodeAndCharsetBomPolicy() throws Exception {
        groups++;
        for (String malformed : new String[] {"c328", "f09f98", "ff"}) {
            TrackedInput input = bytes(malformed);
            expect(CharacterCodingException.class, () -> read(SOURCE, input, StandardCharsets.UTF_8, 8));
            check(input.closes == 1 && input.eof, "malformed UTF-8 is refused after complete owned read");
        }
        TrackedInput ascii = bytes("e9");
        expect(CharacterCodingException.class, () -> read(SOURCE, ascii, StandardCharsets.US_ASCII, 1));
        check(ascii.closes == 1, "non-ASCII bytes are not replaced silently");
        TrackedInput oddUtf16 = bytes("00");
        expect(CharacterCodingException.class, () -> read(SOURCE, oddUtf16, StandardCharsets.UTF_16BE, 1));
        check(oddUtf16.closes == 1, "incomplete UTF-16 code unit is refused");
        M3File literalReplacement = read(SOURCE, bytes("efbfbd"), StandardCharsets.UTF_8, 3);
        check(literalReplacement.text().equals("\ufffd"), "an explicitly encoded replacement character remains valid content");

        M3File autodetect = read(SOURCE, bytes("feff0041"), StandardCharsets.UTF_16, 4);
        M3File explicit = read(SOURCE, bytes("feff0041"), StandardCharsets.UTF_16BE, 4);
        check(autodetect.text().equals("A") && explicit.text().equals("\ufeffA"), "the selected charset owns BOM handling");
        String raw = "da8a119778b37f63a166b7754543f8e88a5a3f2a2dafa5001a4bb1126c330cf3";
        check(autodetect.byteSha256().orElseThrow().equals(raw) && explicit.byteSha256().orElseThrow().equals(raw),
                "BOM bytes remain part of raw identity regardless of decoded text");
        check(autodetect.utf16Sha256().equals("c00b4d3c929cb5cc316691ed4636f634576f2c9b2954767234c5274e9dde185d")
                && explicit.utf16Sha256().equals(raw), "distinct BOM decoding has exact distinct UTF-16 seals");
    }

    private static void readAndCloseFailureOwnership() throws Exception {
        groups++;
        TrackedInput input = bytes("61");
        IOException readFailure = new IOException("read failed");
        IOException closeFailure = new IOException("close failed");
        input.readFailure = readFailure;
        input.closeFailure = closeFailure;
        IOException observed = expect(IOException.class, () -> read(SOURCE, input, StandardCharsets.UTF_8, 1));
        check(observed == readFailure, "original read failure identity survives");
        check(observed.getSuppressed().length == 1 && observed.getSuppressed()[0] == closeFailure,
                "distinct cleanup failure is suppressed on read failure");
        check(input.closes == 1 && input.delivered == 0, "failed read releases ownership once");

        TrackedInput closeOnly = bytes("61");
        IOException cleanup = new IOException("successful read failed close");
        closeOnly.closeFailure = cleanup;
        check(expect(IOException.class, () -> read(SOURCE, closeOnly, StandardCharsets.UTF_8, 1)) == cleanup,
                "successful read cannot publish a source after failed primary close");
        check(closeOnly.eof && closeOnly.closes == 1, "failed primary close is not retried");

        TrackedInput errorInput = bytes("61");
        AssertionError error = new AssertionError("read error");
        errorInput.readFailure = error;
        check(expect(AssertionError.class, () -> read(SOURCE, errorInput, StandardCharsets.UTF_8, 1)) == error,
                "Error identity survives while cleanup still occurs");
        check(errorInput.closes == 1, "Error path closes the owned stream");
    }

    private static void identicalFailureCannotSelfSuppress() throws Exception {
        groups++;
        TrackedInput input = bytes("61");
        IOException shared = new IOException("same read and cleanup failure");
        input.readFailure = shared;
        input.closeFailure = shared;
        check(expect(IOException.class, () -> read(SOURCE, input, StandardCharsets.UTF_8, 1)) == shared,
                "repeated failure identity is not replaced by self-suppression error");
        check(input.closes == 1 && shared.getSuppressed().length == 0, "identical cleanup failure does not cause a retry or duplicate evidence");
    }

    private static void decoderSetupOccursAfterClose() throws Exception {
        groups++;
        TrackedInput input = bytes("61");
        IllegalStateException decoderFailure = new IllegalStateException("decoder setup failed");
        Charset charset = new Charset("X-M3-Owned-Input-Test", new String[0]) {
            @Override public boolean contains(Charset other) { return false; }
            @Override public CharsetDecoder newDecoder() {
                check(input.closed && input.closes == 1, "decoder setup happens after the owned primary close");
                throw decoderFailure;
            }
            @Override public CharsetEncoder newEncoder() { throw new UnsupportedOperationException("not used"); }
        };
        check(expect(IllegalStateException.class, () -> read(SOURCE, input, charset, 1)) == decoderFailure,
                "decoder setup failure retains its identity");
        check(input.closes == 1 && input.eof, "decoder setup failure cannot leak or reclose the input");
    }

    private static void unchangedPathParityAndIndependentReads() throws Exception {
        groups++;
        Path path = work().resolve("path-parity.bin");
        Files.write(path, UTF8, StandardOpenOption.CREATE_NEW);
        M3File old = M3File.read(path, StandardCharsets.UTF_8, 9);
        M3File first = read(SOURCE, new TrackedInput(Files.newInputStream(path)), StandardCharsets.UTF_8, 9);
        M3File second = read(URI.create("memory:/other.java"), bytes("41c3a9f09f98800d0a"), StandardCharsets.UTF_8, 9);
        check(old.text().equals(first.text()) && old.byteSha256().equals(first.byteSha256())
                && old.utf16Sha256().equals(first.utf16Sha256()), "existing Path and owned-stream readers retain equal content contracts");
        check(old.uri().equals(path.toAbsolutePath().normalize().toUri()), "existing Path URI normalization remains intact");
        check(first != second && first.byteSha256().equals(second.byteSha256()) && !first.uri().equals(second.uri()),
                "independent reads retain independent source identities");
        check(M3File.of(SOURCE, TEXT).byteSha256().isEmpty(), "character-only ingress still has no invented raw byte seal");
    }

    private static Path work() {
        String supplied = System.getProperty("m3.parser.test.work");
        if (supplied == null) throw new IllegalStateException("explicit m3.parser.test.work is required");
        Path work = Path.of(supplied);
        if (!Files.isDirectory(work)) throw new IllegalStateException("test work directory does not exist");
        return work;
    }

    private static TrackedInput bytes(String hex) {
        return new TrackedInput(new ByteArrayInputStream(HexFormat.of().parseHex(hex)));
    }

    private static M3File read(URI uri, InputStream input, Charset charset, int maximum) throws Exception {
        try {
            return (M3File) inputReader.invoke(null, uri, input, charset, maximum);
        } catch (InvocationTargetException failed) {
            Throwable cause = failed.getCause();
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw new AssertionError("unexpected invocation failure", cause);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }

    private static <T extends Throwable> T expect(Class<T> type, CheckedAction action) throws Exception {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return type.cast(failure);
            throw new AssertionError("expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    /** Instrument the external input boundary, including actual filesystem streams. */
    private static final class TrackedInput extends InputStream {
        private final InputStream delegate;
        private int maximumChunk = Integer.MAX_VALUE;
        private int reads;
        private int delivered;
        private int closes;
        private int markCalls;
        private int resetCalls;
        private long skipped;
        private boolean eof;
        private boolean closed;
        private Throwable readFailure;
        private IOException closeFailure;

        TrackedInput(InputStream delegate) { this.delegate = delegate; }

        private void beforeRead() throws IOException {
            if (closed) throw new IOException("read after close");
            reads++;
            if (readFailure instanceof IOException checked) throw checked;
            if (readFailure instanceof RuntimeException runtime) throw runtime;
            if (readFailure instanceof Error error) throw error;
        }

        @Override public int read() throws IOException {
            beforeRead();
            int value = delegate.read();
            if (value < 0) eof = true;
            else delivered++;
            return value;
        }

        @Override public int read(byte[] bytes, int offset, int length) throws IOException {
            beforeRead();
            int count = delegate.read(bytes, offset, Math.min(length, maximumChunk));
            if (count < 0) eof = true;
            else delivered += count;
            return count;
        }

        @Override public long skip(long count) throws IOException {
            skipped += count;
            throw new IOException("skip forbidden");
        }

        @Override public void mark(int readLimit) { markCalls++; throw new AssertionError("mark forbidden"); }
        @Override public void reset() throws IOException { resetCalls++; throw new IOException("reset forbidden"); }

        @Override public void close() throws IOException {
            closes++;
            closed = true;
            delegate.close();
            if (closeFailure != null) throw closeFailure;
        }
    }
}
