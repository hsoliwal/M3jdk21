// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Census pass 3 leaf P3-012 restored members (donor com.synexia develop 78cf2ad4c7e5), ported from the donor proofs
 * MIndexEventChannelWriterProof, MIndexEventReaderIndexedTextRecoveryProof and MIndexFileUtf8ChannelFacadeProof: the
 * UTF-8 channel facades of M3File, M3FileProjection, M3FileSpan and M3FileSnapshot, M3FileEventWriter.forChannel and the
 * validated-pass readers M3FileEventReader.read(InputStream) / readIndexedText / readProjectionText. Every member is
 * absent on master (this class does not compile there). The donor's mapped-file section is not ported (MappedM3File is
 * outside the leaf) and its zero-progress channel refusal is not asserted: the owner M3FileText.writeUtf8To spins on
 * such a channel (owner observation, README.md).
 */
//FLLDCIM: MRL - census3 P3-012 donor proof ports (recipe/census3-gaps-20261107)
class M3FileCensus3Test {
    private static final int LANGUAGE = 7;

    @Test
    void channelWriterMatchesStreamWriterInOnePass() throws IOException {
        Path source = Files.createTempFile("m3-census3-channel-", ".java");
        try {
            Files.writeString(source, "alpha + 2\n// note\n\"beta β\"\n", StandardCharsets.UTF_8);
            M3FileDictionary resolver = new M3FileDictionary();
            ByteArrayOutputStream streamBytes = new ByteArrayOutputStream();
            PartialChannel channel = new PartialChannel(3);
            int[] eventCount = {0};
            try (M3FileEventWriter streamWriter = new M3FileEventWriter(streamBytes);
                    M3FileEventWriter channelWriter = M3FileEventWriter.forChannel(channel)) {
                M3FileStreamEvents.Sink fanout = event -> {
                    eventCount[0]++;
                    streamWriter.accept(event);
                    channelWriter.accept(event);
                };
                M3FileStreamIndexer.codeLike(source, resolver, LANGUAGE, fanout);
                assertTrue(streamWriter.isFinished(), "OutputStream writer did not finish");
                assertTrue(channelWriter.isFinished(), "channel writer did not finish");
            }
            assertFalse(channel.isOpen(), "closing the writer closes the bridged channel");
            byte[] expected = streamBytes.toByteArray();
            byte[] actual = channel.bytes();
            assertArrayEquals(expected, actual, "channel output differs from OutputStream output");
            assertTrue(eventCount[0] > 4, "one-pass event stream was not observed");
            assertEquals("MIDXEVT2", new String(actual, 0, 8, StandardCharsets.US_ASCII));

            List<M3FileStreamEvents.Event> replayed = new ArrayList<>();
            M3FileEventReader.Summary summary =
                    M3FileEventReader.read(new ByteArrayInputStream(actual), replayed::add);
            assertEquals(summary.tokenCount(), M3FileEventReader.read(new ByteArrayInputStream(actual)).tokenCount());
            assertTrue(summary.tokenCount() > 0);
            assertTrue(summary.uniqueTokenCount() > 0);
            assertEquals(Files.size(source), summary.byteCount());
            boolean started = false;
            boolean bytes = false;
            boolean lexicon = false;
            boolean token = false;
            boolean finished = false;
            for (M3FileStreamEvents.Event event : replayed) {
                started |= event instanceof M3FileStreamEvents.Started;
                bytes |= event instanceof M3FileStreamEvents.Bytes;
                lexicon |= event instanceof M3FileStreamEvents.Lexicon;
                token |= event instanceof M3FileStreamEvents.Token;
                finished |= event instanceof M3FileStreamEvents.Finished;
            }
            assertTrue(started && bytes && lexicon && token && finished, "event fact lane was incomplete");
            assertThrows(NullPointerException.class, () -> M3FileEventWriter.forChannel(null));
        } finally {
            Files.deleteIfExists(source);
        }
    }

    @Test
    void indexedTextIsRecoveredFromTheValidatedEventPass() throws IOException {
        M3FileDictionary resolver = new M3FileDictionary();
        String text = "alpha + 2\n// note\n\"quoted\"\t";
        Path source = Files.createTempFile("m3-census3-indexed-text-", ".txt");
        try {
            Files.writeString(source, text, StandardCharsets.UTF_8);
            ByteArrayOutputStream encoded = new ByteArrayOutputStream();
            M3File original;
            try (M3FileEventWriter writer = new M3FileEventWriter(encoded)) {
                original = M3FileStreamIndexer.codeLike(source, resolver, LANGUAGE, writer::accept);
                assertTrue(writer.isFinished(), "source event stream did not finish");
            }
            byte[] eventBytes = encoded.toByteArray();
            assertEquals("MIDXEVT2", new String(eventBytes, 0, 8, StandardCharsets.US_ASCII));
            Files.delete(source);

            M3FileText full = M3FileEventReader.readIndexedText(new ByteArrayInputStream(eventBytes), resolver);
            assertEquals(original.asM3String(), full, "complete resolver-ID replay changed");
            assertEquals(text, full.toString());
            M3FileTokenKind[] kinds = {
                M3FileTokenKind.WORD, M3FileTokenKind.NUMBER, M3FileTokenKind.QUOTED, M3FileTokenKind.SYMBOL
            };
            M3FileText selected =
                    M3FileEventReader.readProjectionText(new ByteArrayInputStream(eventBytes), resolver, kinds);
            assertEquals(original.projection(kinds).asM3String(), selected, "selected-kind resolver-ID replay changed");
            assertTrue(selected.length() < full.length());

            M3FileText empty = M3FileEventReader.readProjectionText(new ByteArrayInputStream(eventBytes), resolver);
            assertEquals(0, empty.length(), "empty selection emitted tokens");
            byte[] truncated = Arrays.copyOf(eventBytes, eventBytes.length - 1);
            assertThrows(IOException.class,
                    () -> M3FileEventReader.readIndexedText(new ByteArrayInputStream(truncated), resolver));
            byte[] tampered = eventBytes.clone();
            tampered[tampered.length - 1] ^= 1;
            assertThrows(IOException.class,
                    () -> M3FileEventReader.readProjectionText(new ByteArrayInputStream(tampered), resolver, kinds));
            assertThrows(IOException.class, () -> M3FileEventReader.read(new ByteArrayInputStream(tampered)));
            assertThrows(NullPointerException.class,
                    () -> M3FileEventReader.readIndexedText(new ByteArrayInputStream(eventBytes), null));
            assertFalse(Files.exists(source), "replay reopened the physical source");
        } finally {
            Files.deleteIfExists(source);
        }
    }

    @Test
    void utf8ChannelFacadesStreamEveryRetainedView() throws IOException {
        M3FileDictionary resolver = new M3FileDictionary();
        String source = "alpha beta alpha🙂\n";
        M3File file = M3FileTokenizer.plainText(resolver, LANGUAGE, source);
        M3FileProjection projection = file.semanticProjection();
        M3FileSpan span = file.span(1, Math.min(file.tokenCount(), 4));
        M3FileSnapshot snapshot = file.snapshot();

        assertSink("heap-file", file::writeUtf8To, file.asM3String());
        assertEquals(source, file.asM3String().toString());
        assertSink("heap-snapshot", target -> snapshot.writeUtf8To(resolver, target), snapshot.indexedText(resolver));
        assertSink("projection", projection::writeUtf8To, projection.asM3String());
        assertSink("span", span::writeUtf8To, span.asM3String());
        assertTrue(span.asM3String().length() < file.asM3String().length());

        M3FileProjection empty = M3FileProjection.ofKinds(file);
        PartialChannel emptyTarget = new PartialChannel(1);
        assertEquals(0L, empty.writeUtf8To(emptyTarget), "empty projection count");
        assertArrayEquals(new byte[0], emptyTarget.bytes(), "empty projection bytes");
        assertThrows(NullPointerException.class, () -> file.writeUtf8To(null));
        assertThrows(NullPointerException.class, () -> snapshot.writeUtf8To(null, new PartialChannel(1)));
    }

    private static void assertSink(String label, Sink sink, M3FileText indexed) throws IOException {
        byte[] expected = indexed.getBytes(StandardCharsets.UTF_8);
        PartialChannel target = new PartialChannel(2);
        assertEquals(expected.length, sink.write(target), label + " byte count");
        assertArrayEquals(expected, target.bytes(), label + " bytes");
    }

    @FunctionalInterface
    private interface Sink {
        long write(WritableByteChannel target) throws IOException;
    }

    private static final class PartialChannel implements WritableByteChannel {
        private final int maximum;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private boolean open = true;

        private PartialChannel(int maximum) {
            this.maximum = maximum;
        }

        @Override
        public int write(ByteBuffer source) throws IOException {
            if (!open) throw new ClosedChannelException();
            int count = Math.min(maximum, source.remaining());
            byte[] bytes = new byte[count];
            source.get(bytes);
            output.write(bytes, 0, bytes.length);
            return count;
        }

        private byte[] bytes() {
            return output.toByteArray();
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            open = false;
        }
    }
}
