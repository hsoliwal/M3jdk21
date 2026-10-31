/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real files exercise the capture bridge; reflection lets the same tests demonstrate missing-API RED. */
class MFileCaptureTest {
    private static final String ABC_SHA256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    private static final String EMPTY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @TempDir
    Path temp;

    // Publishing at EOF is premature: primary close can still fail after the last byte.
    @Test
    void localCapturePublishesOnlyAfterCleanCloseWithSampledMetadataAndRawManifest() throws Exception {
        Path path = Files.writeString(temp.resolve("sample.txt"), "abc");
        Files.setLastModifiedTime(path, FileTime.fromMillis(1_700_000_000_000L));
        List<Object> captures = new ArrayList<>();
        AtomicLong worked = new AtomicLong();
        Object captured;
        long before = System.currentTimeMillis();
        try (Stream<MFile> entries = MFiles.from(path).stream()) {
            MFile file = entries.findFirst().orElseThrow();
            MFileContent content = progress(capture(file, captures::add), M3FsProgress.of(() -> false, worked::addAndGet));
            InputStream input = content.openInputStream();
            assertEquals("abc", new String(input.readAllBytes(), StandardCharsets.UTF_8));
            assertEquals(-1, input.read());
            assertTrue(captures.isEmpty());
            input.close();
            input.close();
            assertEquals(1, captures.size());
            assertEquals(3, worked.get());
            captured = captures.getFirst();
            MFileObservation observation = observation(captured);
            assertEquals(file.sourceAddress(), observation.sourceKey());
            assertEquals(OptionalLong.of(3), observation.size());
            assertEquals(OptionalLong.of(1_700_000_000_000L), observation.modifiedTimeMillis());
            assertEquals(ABC_SHA256, observation.content().orElseThrow().id().hex());
            assertEquals(3, observation.content().orElseThrow().size());
            assertEquals(M3ObjectKind.RAW_BYTES, observation.content().orElseThrow().kind());
            assertTrue(observation.manifest().isEmpty());
            M3FileManifest manifest = manifest(captured);
            assertEquals(observation.content().orElseThrow(), manifest.raw());
            assertEquals(Map.of(), manifest.structures());
            assertEquals(Map.of(), manifest.relations());
            assertEquals(Map.of(), manifest.precomputes());
        }
        assertTrue(capturedAt(captured) >= before);
        assertTrue(capturedAt(captured) <= System.currentTimeMillis());
        assertEquals(ABC_SHA256, manifest(captured).raw().id().hex());
    }

    // A zero-byte file still needs EOF and a successful close before its empty-content identity is evidence.
    @Test
    void emptyFileProducesTheKnownEmptySha256AfterClose() throws Exception {
        Path path = Files.createFile(temp.resolve("empty"));
        List<Object> captures = new ArrayList<>();
        try (Stream<MFile> entries = MFiles.from(path).stream()) {
            try (InputStream input = capture(entries.findFirst().orElseThrow(), captures::add).openInputStream()) {
                assertEquals(-1, input.read());
                assertTrue(captures.isEmpty());
            }
        }
        assertEquals(1, captures.size());
        assertEquals(EMPTY_SHA256, manifest(captures.getFirst()).raw().id().hex());
        assertEquals(0, manifest(captures.getFirst()).raw().size());
    }

    // Knowing the sampled length does not substitute for observing EOF; a short-circuit read is incomplete.
    @Test
    void closingAfterExactlyTheSampledBytesWithoutEofDoesNotCapture() throws Exception {
        RecordingContent provider = provider("abc");
        List<Object> captures = new ArrayList<>();
        try (InputStream input = capture(provider, captures::add).openInputStream()) {
            assertEquals('a', input.read());
            assertEquals('b', input.read());
            assertEquals('c', input.read());
        }
        assertTrue(captures.isEmpty());
        assertEquals(1, provider.closeCalls);
    }

    // Capturing the sample when decorating would reuse stale sizes and modification times on reopen.
    @Test
    void everyOpenSamplesAgainAndProducesAnIndependentCapture() throws Exception {
        RecordingContent provider = provider("abc");
        Files.setLastModifiedTime(provider.path, FileTime.fromMillis(1_000));
        List<Object> captures = new ArrayList<>();
        MFileContent content = capture(provider, captures::add);
        assertEquals(0, provider.observationCalls);
        readFully(content);
        Files.writeString(provider.path, "");
        Files.setLastModifiedTime(provider.path, FileTime.fromMillis(2_000));
        readFully(content);
        assertEquals(2, provider.observationCalls);
        assertEquals(2, provider.openCalls);
        assertEquals(2, provider.closeCalls);
        assertEquals(2, captures.size());
        assertNotSame(captures.get(0), captures.get(1));
        assertEquals(OptionalLong.of(1_000), observation(captures.get(0)).modifiedTimeMillis());
        assertEquals(OptionalLong.of(2_000), observation(captures.get(1)).modifiedTimeMillis());
        assertEquals(ABC_SHA256, manifest(captures.get(0)).raw().id().hex());
        assertEquals(EMPTY_SHA256, manifest(captures.get(1)).raw().id().hex());
    }

    // An open-time mutation is a deterministic race between sampled attributes and the bytes actually read.
    @Test
    void sizeDriftFailsCloseWithoutRewritingTheSampleOrPublishingACapture() throws Exception {
        RecordingContent provider = provider("abc");
        provider.appendBeforeOpen = true;
        List<Object> captures = new ArrayList<>();
        InputStream input = capture(provider, captures::add).openInputStream();
        assertEquals("abcd", new String(input.readAllBytes(), StandardCharsets.UTF_8));
        IOException failure = assertThrows(IOException.class, input::close);
        assertTrue(hasMessage(failure, "size"));
        input.close();
        assertEquals(OptionalLong.of(3), provider.lastSample.size());
        assertTrue(provider.lastSample.content().isEmpty());
        assertEquals(1, provider.closeCalls);
        assertTrue(captures.isEmpty());
    }

    // Failed observation must stop before opening content, instead of reporting deletion or an empty capture.
    @Test
    void observationFailureDoesNotOpenTheProvider() throws Exception {
        RecordingContent provider = provider("abc");
        IOException expected = new IOException("controlled observation failure");
        provider.observationFailure = expected;
        List<Object> captures = new ArrayList<>();
        MFileContent content = capture(provider, captures::add);
        assertSame(expected, assertThrows(IOException.class, content::openInputStream));
        assertEquals(1, provider.observationCalls);
        assertEquals(0, provider.openCalls);
        assertTrue(captures.isEmpty());
    }

    // A successful attribute query cannot stand in for a failed provider content open.
    @Test
    void providerOpenFailureDoesNotPublishTheSuccessfulMetadataSample() throws Exception {
        RecordingContent provider = provider("abc");
        IOException expected = new IOException("controlled open failure");
        provider.openFailure = expected;
        List<Object> captures = new ArrayList<>();
        MFileContent content = capture(provider, captures::add);
        assertSame(expected, assertThrows(IOException.class, content::openInputStream));
        assertEquals(1, provider.observationCalls);
        assertEquals(1, provider.openCalls);
        assertEquals(0, provider.closeCalls);
        assertTrue(captures.isEmpty());
    }

    // EOF followed by a close failure must not publish a completed object reference.
    @Test
    void primaryCloseFailurePreventsCaptureAndDoesNotRetryTheProvider() throws Exception {
        RecordingContent provider = provider("abc");
        provider.closeFailure = new IOException("controlled close failure");
        List<Object> captures = new ArrayList<>();
        InputStream input = capture(provider, captures::add).openInputStream();
        assertEquals(3, input.readAllBytes().length);
        assertSame(provider.closeFailure, assertThrows(IOException.class, input::close));
        input.close();
        assertEquals(1, provider.closeCalls);
        assertTrue(captures.isEmpty());
    }

    // A callback can have side effects before throwing; it must never be retried or let later callbacks run.
    @Test
    void captureCallbackFailureRunsOnceAndStopsLaterCaptureConsumers() throws Exception {
        RecordingContent provider = provider("abc");
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        IllegalStateException expected = new IllegalStateException("capture callback failed");
        MFileContent content = capture(capture(provider, value -> {
            first.incrementAndGet();
            throw expected;
        }), value -> second.incrementAndGet());
        InputStream input = content.openInputStream();
        assertEquals(3, input.readAllBytes().length);
        assertSame(expected, assertThrows(IllegalStateException.class, input::close));
        input.close();
        assertEquals(1, first.get());
        assertEquals(0, second.get());
        assertEquals(1, provider.closeCalls);
    }

    // Even when registered later, a failing user shadow completes before capture publication.
    @Test
    void shadowCompletionFailurePreventsTheCaptureCallback() throws Exception {
        RecordingContent provider = provider("abc");
        List<Object> captures = new ArrayList<>();
        AtomicInteger shadowCompletions = new AtomicInteger();
        Class<?> shadowType = Class.forName("com.m3.fs.MFileShadow");
        Object shadow = Proxy.newProxyInstance(shadowType.getClassLoader(), new Class<?>[] {shadowType},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("onComplete")) {
                        shadowCompletions.incrementAndGet();
                        throw new IOException("shadow completion failed");
                    }
                    return null;
                });
        MFileContent content = (MFileContent) invoke(MFileContent.class, capture(provider, captures::add),
                "shadow", new Class<?>[] {shadowType}, shadow);
        InputStream input = content.openInputStream();
        assertEquals(3, input.readAllBytes().length);
        assertTrue(hasMessage(assertThrows(IOException.class, input::close), "shadow completion failed"));
        input.close();
        assertEquals(1, shadowCompletions.get());
        assertEquals(1, provider.closeCalls);
        assertTrue(captures.isEmpty());
    }

    // Output may append or overwrite only part of a destination; its sequence is never a whole-file capture.
    @Test
    void outputOnACaptureViewIsMonitoredButNeverSamplesOrCapturesInputFacts() throws Exception {
        RecordingContent provider = provider("abc");
        List<Object> captures = new ArrayList<>();
        AtomicLong worked = new AtomicLong();
        MFileContent content = progress(capture(provider, captures::add), M3FsProgress.of(() -> false, worked::addAndGet));
        try (OutputStream output = content.openOutputStream(true)) {
            output.write('d');
        }
        assertEquals("abcd", Files.readString(provider.path));
        assertEquals(1, worked.get());
        assertEquals(0, provider.observationCalls);
        assertTrue(captures.isEmpty());
    }

    // Structural facts remain optional owner-produced references, bound to the captured raw input and key.
    @Test
    void optionalWordIdAndMetadataStructuresReuseManifestAndPrecomputeBindings() throws Exception {
        RecordingContent provider = provider("abc");
        List<Object> captures = new ArrayList<>();
        readFully(capture(provider, captures::add));
        Object rawCapture = captures.getFirst();
        M3ContentId rawId = manifest(rawCapture).raw().id();
        M3ObjectRef words = new M3ObjectRef(M3ContentId.digest(new byte[] {7, 11}), 2,
                M3ObjectKind.MINDEX_FILE_IMAGE);
        M3PrecomputeKey wordsKey = new M3PrecomputeKey("word-ids", "existing-test-owner", 1,
                List.of(rawId), Map.of("encoding", "UTF-8"));
        Object withWords = withStructure(rawCapture, "word-ids", words, wordsKey);
        M3ObjectRef metadata = new M3ObjectRef(M3ContentId.digest(new byte[] {3}), 1,
                M3ObjectKind.MINDEX_SOURCE_FACTS_IMAGE);
        M3PrecomputeKey metadataKey = new M3PrecomputeKey("source-facts", "existing-test-owner", 1,
                List.of(rawId), Map.of());
        Object withBoth = withStructure(withWords, "source-facts", metadata, metadataKey);
        assertEquals(Map.of(), manifest(rawCapture).structures());
        assertEquals(Map.of("word-ids", words), manifest(withWords).structures());
        assertEquals(Map.of("word-ids", words, "source-facts", metadata), manifest(withBoth).structures());
        assertSame(manifest(withBoth), observation(withBoth).manifest().orElseThrow());
        assertEquals(capturedAt(rawCapture), capturedAt(withBoth));
        assertEquals(manifest(rawCapture).raw(), manifest(withBoth).raw());
        M3PrecomputeKey wrongInput = new M3PrecomputeKey("word-ids", "existing-test-owner", 1,
                List.of(M3ContentId.parse(EMPTY_SHA256)), Map.of());
        assertThrows(IllegalArgumentException.class, () -> withStructure(rawCapture, "word-ids", words, wrongInput));
    }

    // A public completion factory must not promote a write-sequence hash into complete input evidence.
    @Test
    void captureFactoryRejectsOutputCompletionAndKeepsSignedSampleTimes() throws Exception {
        RecordingContent provider = provider("abc");
        MFileObservation sample = provider.observation();
        Object output = completion("OUTPUT_SEQUENCE", 3, ABC_SHA256);
        assertThrows(IOException.class, () -> fromInput(sample, -17, output));
        Object captured = fromInput(sample, -17, completion("INPUT_EOF", 3, ABC_SHA256));
        assertEquals(-17, capturedAt(captured));
        assertEquals(sample.modifiedTimeMillis(), observation(captured).modifiedTimeMillis());
    }

    private RecordingContent provider(String text) throws IOException {
        return new RecordingContent(Files.writeString(temp.resolve("content"), text));
    }

    private static MFileContent capture(MFileContent content, Consumer<Object> consumer) throws Exception {
        return (MFileContent) invoke(MFileContent.class, content, "captureReads", new Class<?>[] {Consumer.class}, consumer);
    }

    private static MFileContent progress(MFileContent content, M3FsProgress monitor) throws Exception {
        return (MFileContent) invoke(MFileContent.class, content, "progressify", new Class<?>[] {M3FsProgress.class}, monitor);
    }

    private static void readFully(MFileContent content) throws IOException {
        try (InputStream input = content.openInputStream()) {
            input.transferTo(OutputStream.nullOutputStream());
        }
    }

    private static MFileObservation observation(Object captured) throws Exception {
        return (MFileObservation) invoke(captured.getClass(), captured, "observation", new Class<?>[0]);
    }

    private static M3FileManifest manifest(Object captured) throws Exception {
        return (M3FileManifest) invoke(captured.getClass(), captured, "manifest", new Class<?>[0]);
    }

    private static long capturedAt(Object captured) throws Exception {
        return (long) invoke(captured.getClass(), captured, "capturedAtMillis", new Class<?>[0]);
    }

    private static Object withStructure(Object captured, String name, M3ObjectRef ref, M3PrecomputeKey key) throws Exception {
        return invoke(captured.getClass(), captured, "withStructure",
                new Class<?>[] {String.class, M3ObjectRef.class, M3PrecomputeKey.class}, name, ref, key);
    }

    private static Object completion(String kind, long count, String hash) throws Exception {
        Class<?> type = Class.forName("com.m3.fs.MFileStreams$Completion");
        Class<?> kinds = Class.forName("com.m3.fs.MFileStreams$CompletionKind");
        Object value = List.of(kinds.getEnumConstants()).stream().filter(item -> item.toString().equals(kind)).findFirst().orElseThrow();
        return type.getConstructor(String.class, long.class, M3ContentId.class, kinds)
                .newInstance("selected-file-layer", count, M3ContentId.parse(hash), value);
    }

    private static Object fromInput(MFileObservation sample, long time, Object completion) throws Exception {
        return invoke(Class.forName("com.m3.fs.MFileCapture"), null, "fromInput",
                new Class<?>[] {MFileObservation.class, long.class, completion.getClass()}, sample, time, completion);
    }

    private static Object invoke(Class<?> declaring, Object receiver, String name, Class<?>[] types, Object... arguments)
            throws Exception {
        try {
            return declaring.getMethod(name, types).invoke(receiver, arguments);
        } catch (InvocationTargetException wrapper) {
            Throwable failure = wrapper.getCause();
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            throw new AssertionError(failure);
        }
    }

    private static boolean hasMessage(Throwable failure, String text) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(text)) return true;
        }
        return false;
    }

    /** Boundary fixture delegates real storage IO and injects one explicitly selected provider failure. */
    private static final class RecordingContent implements MFileContent {
        private final Path path;
        private int observationCalls;
        private int openCalls;
        private int closeCalls;
        private boolean appendBeforeOpen;
        private IOException observationFailure;
        private IOException openFailure;
        private IOException closeFailure;
        private MFileObservation lastSample;

        RecordingContent(Path path) {
            this.path = path;
        }

        public MFileObservation observation() throws IOException {
            observationCalls++;
            if (observationFailure != null) throw observationFailure;
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            lastSample = MFileObservation.metadata(path.toUri().toString(),
                    MFileObservation.EXISTS_KNOWN | MFileObservation.EXISTS | MFileObservation.TYPE_KNOWN
                            | MFileObservation.FILE | MFileObservation.SIZE_KNOWN | MFileObservation.MODIFIED_KNOWN,
                    attributes.size(), attributes.lastModifiedTime().toMillis(), MFileObservation.ChangeStamp.unavailable());
            return lastSample;
        }

        @Override
        public InputStream openInputStream() throws IOException {
            openCalls++;
            if (openFailure != null) throw openFailure;
            if (appendBeforeOpen) Files.writeString(path, "d", StandardOpenOption.APPEND);
            return new FilterInputStream(Files.newInputStream(path)) {
                @Override
                public void close() throws IOException {
                    closeCalls++;
                    super.close();
                    if (closeFailure != null) throw closeFailure;
                }
            };
        }

        @Override
        public OutputStream openOutputStream(boolean append) throws IOException {
            return Files.newOutputStream(path, append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}
