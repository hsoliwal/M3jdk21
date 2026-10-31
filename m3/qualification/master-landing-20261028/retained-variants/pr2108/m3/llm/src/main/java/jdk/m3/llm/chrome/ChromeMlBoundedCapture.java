// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Byte-first bounded pipe reader. A newline-free stream cannot bypass the capture budget. */
final class ChromeMlBoundedCapture implements Runnable {
    private final InputStream input;
    private final int maximum;
    private final ChromeMlRunner.TokenSink sink;
    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    private final ByteArrayOutputStream line = new ByteArrayOutputStream();
    private volatile Throwable failure;
    private volatile String text;

    ChromeMlBoundedCapture(InputStream input, int maximum, ChromeMlRunner.TokenSink sink) {
        this.input = Objects.requireNonNull(input, "input");
        if (maximum < 1) { throw new IllegalArgumentException("maximum"); }
        this.maximum = maximum;
        this.sink = sink;
    }

    Throwable failure() { return failure; }

    String text() throws IOException {
        String result = text;
        if (result == null) { throw new IOException("RUNNER_CAPTURE_INCOMPLETE"); }
        return result;
    }

    @Override
    public void run() {
        try (input) {
            byte[] buffer = new byte[Math.min(maximum, 4096)];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (count > maximum - captured.size()) { throw new IOException("RUNNER_OUTPUT_LIMIT"); }
                captured.write(buffer, 0, count);
                if (sink != null) {
                    for (int i = 0; i < count; i++) {
                        if (buffer[i] == '\n') { emit(); }
                        else { line.write(buffer[i]); }
                    }
                }
            }
            if (sink != null && line.size() > 0) { emit(); }
            text = decode(captured.toByteArray());
        } catch (Throwable exception) {
            failure = exception;
        }
    }

    private void emit() throws IOException {
        String value = decode(line.toByteArray());
        line.reset();
        if (value.endsWith("\r")) { value = value.substring(0, value.length() - 1); }
        var token = ChromeMlRunnerOutputParser.streamingToken(value);
        if (token.isPresent()) { sink.accept(token.get()); }
    }

    private static String decode(byte[] bytes) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("RUNNER_INVALID_UTF8", exception);
        }
    }
    /** Compatibility facade for the former static reader; shares the same bounded parser. */
    static String read(InputStream input, int maximum, ChromeMlRunner.TokenSink sink) throws IOException {
        ChromeMlBoundedCapture capture = new ChromeMlBoundedCapture(input, maximum, sink);
        capture.run();
        Throwable error = capture.failure();
        if (error instanceof IOException io) { throw io; }
        if (error instanceof RuntimeException runtime) { throw runtime; }
        if (error instanceof Error fatal) { throw fatal; }
        if (error != null) { throw new IOException("RUNNER_CAPTURE_FAILED", error); }
        return capture.text();
    }
}
