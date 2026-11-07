// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package census3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import jdk.m3.file.M3File;
import jdk.m3.file.M3FileDictionary;
import jdk.m3.file.M3FileEventReader;
import jdk.m3.file.M3FileEventWriter;
import jdk.m3.file.M3FileProjection;
import jdk.m3.file.M3FileResolver;
import jdk.m3.file.M3FileSnapshot;
import jdk.m3.file.M3FileSpan;
import jdk.m3.file.M3FileStreamEvents;
import jdk.m3.file.M3FileStreamIndexer;
import jdk.m3.file.M3FileText;
import jdk.m3.file.M3FileTokenKind;
import jdk.m3.file.M3FileTokenizer;

/** Owner side: jdk.m3.file M3File family with the restored UTF-8 channel facades, channel writer and text readers. */
public final class OwnerFile implements Census3FileDifferential.Side {
    private static final int LANGUAGE = 7;
    private M3FileResolver resolver;
    private M3File file;
    private byte[] events;

    public void reset() {
        resolver = new M3FileDictionary();
    }

    public String tokenize(String text, boolean code) {
        file = code ? M3FileTokenizer.codeLike(resolver, LANGUAGE, text) : M3FileTokenizer.plainText(resolver, LANGUAGE, text);
        return file.tokenCount() + "/" + file.asM3String().length();
    }

    public String writeFile(int chunk) {
        return sink(chunk, target -> file.writeUtf8To(target));
    }

    public String writeSemantic(int chunk) {
        M3FileProjection projection = file.semanticProjection();
        return sink(chunk, target -> projection.writeUtf8To(target));
    }

    public String writeKinds(int chunk, int kindMask) {
        M3FileProjection projection = M3FileProjection.ofKinds(file, kinds(kindMask));
        return sink(chunk, target -> projection.writeUtf8To(target));
    }

    public String writeSpan(int chunk, int start, int end) {
        try {
            M3FileSpan span = file.span(start, end);
            return sink(chunk, target -> span.writeUtf8To(target));
        } catch (RuntimeException failure) {
            return "!" + failure.getClass().getSimpleName();
        }
    }

    public String writeSnapshot(int chunk) {
        M3FileSnapshot snapshot = file.snapshot();
        return sink(chunk, target -> snapshot.writeUtf8To(resolver, target));
    }

    /** Streams the current text through the event writer twice (OutputStream and channel): equality and both reads. */
    public String events(String text, int chunk) throws IOException {
        Path source = Files.createTempFile("census3-events-", ".txt");
        try {
            Files.writeString(source, text, StandardCharsets.UTF_8);
            ByteArrayOutputStream streamBytes = new ByteArrayOutputStream();
            PartialChannel channel = new PartialChannel(chunk);
            int[] count = {0};
            try (M3FileEventWriter streamWriter = new M3FileEventWriter(streamBytes);
                    M3FileEventWriter channelWriter = M3FileEventWriter.forChannel(channel)) {
                M3FileStreamEvents.Sink fanout = event -> {
                    count[0]++;
                    streamWriter.accept(event);
                    channelWriter.accept(event);
                };
                M3FileStreamIndexer.codeLike(source, resolver, LANGUAGE, fanout);
                events = streamBytes.toByteArray();
                return "events=" + count[0] + " channelEqual=" + java.util.Arrays.equals(events, channel.bytes())
                        + " finished=" + streamWriter.isFinished() + "/" + channelWriter.isFinished();
            }
        } finally {
            Files.deleteIfExists(source);
        }
    }

    public String readIndexed(int truncate, int tamperAt) {
        byte[] bytes = mutate(truncate, tamperAt);
        try {
            M3FileText text = M3FileEventReader.readIndexedText(new ByteArrayInputStream(bytes), resolver);
            return text.length() + ":" + sha(text.toString());
        } catch (IOException | RuntimeException failure) {
            return "!" + failure.getClass().getSimpleName();
        }
    }

    public String readProjection(int kindMask, int truncate, int tamperAt) {
        byte[] bytes = mutate(truncate, tamperAt);
        try {
            M3FileText text = M3FileEventReader.readProjectionText(new ByteArrayInputStream(bytes), resolver, kinds(kindMask));
            return text.length() + ":" + sha(text.toString());
        } catch (IOException | RuntimeException failure) {
            return "!" + failure.getClass().getSimpleName();
        }
    }

    public String readSummary(int truncate, int tamperAt) {
        byte[] bytes = mutate(truncate, tamperAt);
        try {
            M3FileEventReader.Summary summary = M3FileEventReader.read(new ByteArrayInputStream(bytes));
            return summary.tokenCount() + "/" + summary.uniqueTokenCount() + "/" + summary.byteCount();
        } catch (IOException | RuntimeException failure) {
            return "!" + failure.getClass().getSimpleName();
        }
    }

    /** tamperAt >= 0 flips one bit of the terminal content digest (the last 32 bytes), as the donor proof does. */
    private byte[] mutate(int truncate, int tamperAt) {
        byte[] bytes = events.clone();
        if (truncate > 0) bytes = java.util.Arrays.copyOf(bytes, Math.max(0, bytes.length - truncate));
        if (tamperAt >= 0 && bytes.length > 0) bytes[bytes.length - 1 - (tamperAt % Math.min(32, bytes.length))] ^= 1;
        return bytes;
    }

    private static M3FileTokenKind[] kinds(int mask) {
        List<M3FileTokenKind> selected = new ArrayList<>();
        M3FileTokenKind[] all = M3FileTokenKind.values();
        for (int i = 0; i < all.length; i++) if ((mask & (1 << i)) != 0) selected.add(all[i]);
        return selected.toArray(new M3FileTokenKind[0]);
    }

    private interface Sink {
        long write(WritableByteChannel target) throws IOException;
    }

    private static String sink(int chunk, Sink sink) {
        PartialChannel target = new PartialChannel(chunk);
        try {
            long count = sink.write(target);
            byte[] bytes = target.bytes();
            return count + ":" + bytes.length + ":" + sha(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException failure) {
            return "!" + failure.getClass().getSimpleName();
        }
    }

    private static String sha(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static class PartialChannel implements WritableByteChannel {
        private final int maximum;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private boolean open = true;

        PartialChannel(int maximum) {
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

        byte[] bytes() {
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
