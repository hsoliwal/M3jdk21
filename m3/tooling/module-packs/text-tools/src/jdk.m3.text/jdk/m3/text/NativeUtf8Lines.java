// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Direct UTF-8 literal grep kernel. The source is borrowed, never retained by native code.
 * Selected String projections are made by the executing JDK (M3 String on M3JDK21).
 *
 * <p>The caller must not mutate, unmap, or close the direct memory during this method.
 * This is literal case-sensitive matching, not Unix4j's general regex/option surface.</p>
 */
final class NativeUtf8Lines {
    private static final int BATCH = 256;
    private static final int MAX_PATTERN_BYTES = 65536;

    private NativeUtf8Lines() { }

    static List<TextLine> grepLiteral(ByteBuffer source, String literal, int maxLines, int maxBytes) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(literal, "literal");
        if (!source.isDirect()) throw new IllegalArgumentException("direct buffer required");
        if (literal.indexOf('\r') >= 0 || literal.indexOf('\n') >= 0)
            throw new IllegalArgumentException("literal must stay inside line content");
        for (int i = 0; i < literal.length(); i++) {
            char ch = literal.charAt(i);
            if (Character.isHighSurrogate(ch)) {
                if (++i == literal.length() || !Character.isLowSurrogate(literal.charAt(i)))
                    throw new IllegalArgumentException("unpaired high surrogate");
            } else if (Character.isLowSurrogate(ch)) {
                throw new IllegalArgumentException("unpaired low surrogate");
            }
        }
        byte[] needle = literal.getBytes(StandardCharsets.UTF_8);
        if (needle.length > MAX_PATTERN_BYTES) throw new IllegalArgumentException("literal budget");
        ByteBuffer data = source.asReadOnlyBuffer();
        int from = data.position(), limit = data.limit();
        if (maxLines < 0 || maxBytes < 0 || limit - from > maxBytes)
            throw new IllegalArgumentException("input budget exceeded");
        Native.ensureLoaded();
        long rows = validate0(data, from, limit);
        if (rows > maxLines) throw new IllegalArgumentException("input line budget exceeded");
        // Prepared once for the whole query, not once per 256 matched records.
        // Direct buffers are borrowed only during native calls; no handle escapes.
        ByteBuffer nativeNeedle = null;
        IntBuffer failure = null;
        if (needle.length >= 8) {
            nativeNeedle = ByteBuffer.allocateDirect(needle.length);
            nativeNeedle.put(needle).flip();
            failure = ByteBuffer.allocateDirect(Math.multiplyExact(needle.length, Integer.BYTES))
                    .order(ByteOrder.nativeOrder()).asIntBuffer();
            prepareFailure(needle, failure);
        }
        int[] triples = new int[BATCH * 3];
        List<TextLine> found = new ArrayList<>();
        int cursor = from;
        while (cursor < limit) {
            long packed = failure == null
                    ? scan0(data, cursor, limit, needle, triples)
                    : scanPrepared0(data, cursor, limit, nativeNeedle, failure, triples);
            int next = (int)(packed >>> 32);
            int count = (int) packed;
            if (next <= cursor || next > limit || count < 0 || count > BATCH)
                throw new IllegalStateException("invalid native scan cursor");
            for (int i = 0; i < count; i++) {
                int a = triples[3 * i], b = triples[3 * i + 1], c = triples[3 * i + 2];
                if (a < cursor || b < a || c < b || c > next)
                    throw new IllegalStateException("invalid native line offsets");
                if (found.size() == maxLines) throw new IllegalArgumentException("output line budget exceeded");
                found.add(new TextLine(decode(data, a, b), decode(data, b, c)));
            }
            cursor = next;
        }
        return List.copyOf(found);
    }

    private static String decode(ByteBuffer buffer, int from, int to) {
        ByteBuffer view = buffer.duplicate();
        view.position(from).limit(to);
        try {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(view);
            return decoded.toString();
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("UTF-8 source changed during scan", invalid);
        }
    }

    /** Primitive plan values are offsets into needle, not pointers to source text. */
    private static void prepareFailure(byte[] needle, IntBuffer failure) {
        failure.put(0, 0);
        for (int i = 1, prefix = 0; i < needle.length; i++) {
            while (prefix > 0 && needle[i] != needle[prefix])
                prefix = failure.get(prefix - 1);
            if (needle[i] == needle[prefix]) prefix++;
            failure.put(i, prefix);
        }
    }

    private static native long validate0(ByteBuffer bytes, int from, int end);
    private static native long scanPrepared0(ByteBuffer bytes, int from, int end,
                                            ByteBuffer needle, IntBuffer failure, int[] triples);
    private static native long scan0(ByteBuffer bytes, int from, int end, byte[] needle, int[] triples);

    private static final class Native {
        static {
            Path bundled = Path.of(System.getProperty("java.home"), "lib", System.mapLibraryName("m3textio"));
            if (Files.isRegularFile(bundled)) System.load(bundled.toAbsolutePath().toString());
            else System.loadLibrary("m3textio");
        }
        static void ensureLoaded() { }
    }
}
