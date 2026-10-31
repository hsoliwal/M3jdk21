/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSystem;
import com.m3.fs.vfs.FileSystemException;

/** Contract tests use actual VFS method dispatch, with a synthetic provider transport. */
class M3FsBulkTest {
    @Test
    void metadataMasksRetainMissingAndHybridEntries() {
        Provider provider = new Provider();
        provider.entries.put("/hybrid", new Entry(true, true, true));
        M3FsBulk.Result result = M3FsBulk.fallback(provider.filesystem(),
                M3FsBulk.builder(0).stat("/missing").stat("/hybrid").build());
        assertEquals(2, result.successfulCount());
        assertEquals(M3FsBulk.META_CHECKED, result.metadataMask(0));
        assertEquals(M3FsBulk.META_CHECKED | M3FsBulk.META_EXISTS
                | M3FsBulk.META_FILE | M3FsBulk.META_DIRECTORY, result.metadataMask(1));
        assertEquals(List.of("stat /missing", "stat /hybrid"), provider.events);
    }

    @Test
    void orderedMixedOperationsReuseProviderAndFreezeArrayInputs() {
        Provider provider = new Provider();
        M3FsBulk.Builder builder = M3FsBulk.builder(1).mkdir("/folder")
                .copy("/source", "/copy").move("/copy", "/moved").delete("/folder");
        M3FsBulk.Batch snapshot = builder.build();
        builder.stat("/after");
        assertEquals(4, snapshot.size());
        M3FsBulk.Result result = M3FsBulk.fallback(provider.filesystem(), snapshot);
        assertEquals(4, result.successfulCount());
        assertEquals(List.of("mkdir /folder", "copy /source -> /copy", "move /copy -> /moved",
                "delete /folder"), provider.events);
    }

    @Test
    void nonRecursiveDeleteReportsNoEffectInsteadOfInventingSuccess() {
        Provider provider = new Provider();
        M3FsBulk.Result result = M3FsBulk.fallback(provider.filesystem(),
                M3FsBulk.builder(2).delete("/missing").deleteTree("/missing").build());
        assertEquals(M3FsBulk.NO_EFFECT, result.status(0));
        assertEquals(M3FsBulk.NO_EFFECT, result.status(1));
        assertEquals(0, result.successfulCount());
    }

    @Test
    void checkedFailureContinuesByDefaultButFailFastSkipsRemaining() {
        Provider provider = new Provider();
        M3FsBulk.Batch all = M3FsBulk.builder(0).mkdir("/good")
                .delete("/fail").stat("/later").build();
        M3FsBulk.Result continued = M3FsBulk.fallback(provider.filesystem(), all);
        assertEquals(M3FsBulk.OK, continued.status(0));
        assertEquals(M3FsBulk.FAILED, continued.status(1));
        assertNotNull(continued.error(1));
        assertEquals(M3FsBulk.OK, continued.status(2));
        provider.events.clear();
        M3FsBulk.Batch failFast = M3FsBulk.builder(3).failFast(true)
                .mkdir("/good").delete("/fail").stat("/later").build();
        M3FsBulk.Result stopped = M3FsBulk.fallback(provider.filesystem(), failFast);
        assertEquals(M3FsBulk.SKIPPED, stopped.status(2));
        assertEquals(List.of("mkdir /good", "delete /fail"), provider.events);
    }

    @Test
    void atomicRequestIsRejectedBeforeAnyProviderOperation() {
        Provider provider = new Provider();
        M3FsBulk.Batch batch = M3FsBulk.builder(1).atomic(true).delete("/good").build();
        assertThrows(UnsupportedOperationException.class,
                () -> M3FsBulk.fallback(provider.filesystem(), batch));
        assertTrue(provider.events.isEmpty());
    }

    @Test
    void progressAndCancellationArePerCompletedRequest() {
        Provider provider = new Provider();
        AtomicInteger work = new AtomicInteger();
        M3FsProgress progress = M3FsProgress.of(() -> work.get() >= 1, ignored -> work.incrementAndGet());
        M3FsBulk.Batch batch = M3FsBulk.builder(0).progress(progress)
                .mkdir("/first").mkdir("/second").build();
        assertThrows(CancellationException.class, () -> M3FsBulk.fallback(provider.filesystem(), batch));
        assertEquals(List.of("mkdir /first"), provider.events);
        assertEquals(1, work.get());
    }

    @Test
    void invalidBuilderInputsAndArrayGeometryAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> M3FsBulk.builder(-1));
        assertThrows(NullPointerException.class, () -> M3FsBulk.builder(0).copy("/a", null));
        assertThrows(IllegalArgumentException.class, () -> M3FsBulk.builder(0).move("/a", ""));
        assertThrows(IllegalArgumentException.class, () -> new M3FsBulk.Result(
                new byte[2], new int[1], new FileSystemException[2]));
    }


    @Test
    void resultProjectionsUseAbsoluteMasksAndPreserveResultColumns() {
        byte[] state = new byte[134];
        int[] meta = new int[134];
        FileSystemException[] errors = new FileSystemException[134];
        state[0] = M3FsBulk.OK;
        state[63] = M3FsBulk.FAILED;
        state[64] = M3FsBulk.SKIPPED;
        state[65] = M3FsBulk.NO_EFFECT;
        state[127] = M3FsBulk.OK;
        state[128] = M3FsBulk.OK;
        state[133] = (byte) 12; // Unknown provider state must never alias a known bit.
        meta[0] = M3FsBulk.META_CHECKED | M3FsBulk.META_EXISTS;
        meta[63] = M3FsBulk.META_CHECKED;
        meta[65] = M3FsBulk.META_CHECKED;
        meta[127] = M3FsBulk.META_CHECKED | M3FsBulk.META_DIRECTORY;
        meta[128] = M3FsBulk.META_CHECKED | M3FsBulk.META_EXISTS;
        errors[63] = new FileSystemException("retained exception");
        M3FsBulk.Result result = new M3FsBulk.Result(state, meta, errors);
        state[0] = M3FsBulk.SKIPPED;
        meta[0] = 0;
        errors[63] = null;

        int statuses = M3FsBulk.STATUS_FLAG_OK | M3FsBulk.STATUS_FLAG_NO_EFFECT;
        assertArrayEquals(new int[] {0, 65, 128},
                result.selectedOrdinals(statuses, M3FsBulk.META_CHECKED, M3FsBulk.META_DIRECTORY));
        assertEquals(3, result.selectedCount(statuses, M3FsBulk.META_CHECKED,
                M3FsBulk.META_DIRECTORY));
        assertArrayEquals(new long[] {1L, 1L << 1, 1L},
                result.selectedMaskWords(statuses, M3FsBulk.META_CHECKED,
                        M3FsBulk.META_DIRECTORY));
        assertArrayEquals(new int[] {65, 128}, result.selectedOrdinals(64, 66,
                statuses, M3FsBulk.META_CHECKED, M3FsBulk.META_DIRECTORY));
        assertArrayEquals(new long[] {0L, 1L << 1, 1L}, result.selectedMaskWords(64, 66,
                statuses, M3FsBulk.META_CHECKED, M3FsBulk.META_DIRECTORY));
        assertArrayEquals(new long[] {1L << 63, 0L, 0L},
                result.selectedMaskWords(M3FsBulk.STATUS_FLAG_FAILED, 0, 0));
        assertNotNull(result.error(63));
        assertEquals(M3FsBulk.OK, result.status(0));
        assertEquals(M3FsBulk.META_CHECKED | M3FsBulk.META_EXISTS, result.metadataMask(0));
        assertEquals(3, result.successfulCount());
    }

    @Test
    void resultSelectionRejectsUnknownFlagsAndInvalidOffsets() {
        M3FsBulk.Result empty = new M3FsBulk.Result(
                new byte[0], new int[0], new FileSystemException[0]);
        assertArrayEquals(new int[0], empty.selectedOrdinals(M3FsBulk.STATUS_FLAGS_ALL, 0, 0));
        assertArrayEquals(new long[0], empty.selectedMaskWords(M3FsBulk.STATUS_FLAGS_ALL, 0, 0));
        assertEquals(0, empty.selectedCount(M3FsBulk.STATUS_FLAGS_ALL, 0, 0));
        M3FsBulk.Result one = new M3FsBulk.Result(
                new byte[] { M3FsBulk.OK }, new int[] { M3FsBulk.META_EXISTS },
                new FileSystemException[1]);
        assertThrows(IllegalArgumentException.class,
                () -> one.selectedCount(1 << 8, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> one.selectedOrdinals(M3FsBulk.STATUS_FLAG_OK | (1 << 31), 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> one.selectedMaskWords(-1, 0, 0));
        assertThrows(IndexOutOfBoundsException.class,
                () -> one.selectedOrdinals(1, 1, M3FsBulk.STATUS_FLAGS_ALL, 0, 0));
        assertThrows(IndexOutOfBoundsException.class,
                () -> one.selectedMaskWords(-1, 1, M3FsBulk.STATUS_FLAGS_ALL, 0, 0));
        assertEquals(0, one.selectedCount(0, 0, 0));
        assertEquals(0, one.selectedCount(M3FsBulk.STATUS_FLAGS_ALL,
                M3FsBulk.META_EXISTS, M3FsBulk.META_EXISTS));
        assertArrayEquals(new int[0], one.selectedOrdinals(M3FsBulk.STATUS_FLAGS_ALL,
                M3FsBulk.META_EXISTS, M3FsBulk.META_EXISTS));
        assertArrayEquals(new long[] {0L}, one.selectedMaskWords(M3FsBulk.STATUS_FLAGS_ALL,
                M3FsBulk.META_EXISTS, M3FsBulk.META_EXISTS));
    }

    @Test
    void randomizedResultSelectionsMatchIndependentLinearOracle() {
        java.util.Random random = new java.util.Random(0x7E51E21);
        for (int trial = 0; trial < 520; trial++) {
            int length = trial < 10 ? new int[] {0, 1, 2, 63, 64, 65, 66, 127, 128, 129}[trial]
                    : random.nextInt(300);
            byte[] states = new byte[length];
            int[] meta = new int[length];
            for (int i = 0; i < length; i++) {
                states[i] = (byte) (random.nextInt(9) - 2);
                meta[i] = random.nextInt(32);
            }
            M3FsBulk.Result result = new M3FsBulk.Result(
                    states, meta, new FileSystemException[length]);
            int first = random.nextInt(length + 1);
            int window = random.nextInt(length - first + 1);
            int flags = random.nextInt(32) & M3FsBulk.STATUS_FLAGS_ALL;
            int required = random.nextInt(32);
            int excluded = random.nextInt(32);
            List<Integer> reference = new ArrayList<>();
            long[] words = new long[(length + 63) >>> 6];
            for (int i = first; i < first + window; i++) {
                int s = states[i];
                int metadata = meta[i];
                boolean accepted = s >= M3FsBulk.OK && s <= M3FsBulk.SKIPPED
                        && (flags & (1 << s)) != 0
                        && (metadata & required) == required
                        && (metadata & excluded) == 0;
                if (accepted) {
                    reference.add(i);
                    words[i >>> 6] |= 1L << (i & 63);
                }
            }
            int[] expected = reference.stream().mapToInt(Integer::intValue).toArray();
            assertArrayEquals(expected, result.selectedOrdinals(first, window,
                    flags, required, excluded));
            assertEquals(expected.length, result.selectedCount(first, window,
                    flags, required, excluded));
            assertArrayEquals(words, result.selectedMaskWords(first, window,
                    flags, required, excluded));
        }
    }

    private record Entry(boolean exists, boolean file, boolean directory) { }

    private static final class Provider {
        private final Map<String, Entry> entries = new HashMap<>();
        private final List<String> events = new ArrayList<>();

        private FileSystem filesystem() {
            return (FileSystem) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{FileSystem.class}, (proxy, method, arguments) -> {
                        if (method.getName().equals("resolveFile")) {
                            return file((String) arguments[0]);
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
        }

        private FileObject file(String path) {
            return (FileObject) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{FileObject.class}, (proxy, method, arguments) -> {
                        String op = method.getName();
                        if (op.equals("exists") || op.equals("isFile") || op.equals("isFolder")) {
                            if (op.equals("exists")) events.add("stat " + path);
                            Entry state = entries.getOrDefault(path, new Entry(false, false, false));
                            return switch (op) {
                                case "exists" -> state.exists();
                                case "isFile" -> state.file();
                                default -> state.directory();
                            };
                        }
                        String target = (arguments != null && arguments.length > 0
                                && arguments[0] instanceof FileObject other)
                                ? other.toString() : "";
                        switch (op) {
                            case "createFolder" -> {
                                events.add("mkdir " + path);
                                entries.put(path, new Entry(true, false, true));
                                return null;
                            }
                            case "delete", "deleteAll" -> {
                                events.add((op.equals("delete") ? "delete " : "deleteTree ") + path);
                                if (path.equals("/fail")) throw new FileSystemException("synthetic failure");
                                Entry removed = entries.remove(path);
                                return op.equals("delete") ? removed != null : removed == null ? 0 : 1;
                            }
                            case "copyFrom" -> {
                                events.add("copy " + target + " -> " + path);
                                entries.put(path, new Entry(true, true, false));
                                return null;
                            }
                            case "moveTo" -> {
                                events.add("move " + path + " -> " + target);
                                entries.put(target, entries.getOrDefault(path, new Entry(true, true, false)));
                                entries.remove(path);
                                return null;
                            }
                            case "toString" -> { return path; }
                            default -> throw new UnsupportedOperationException(op);
                        }
                    });
        }
    }
}
