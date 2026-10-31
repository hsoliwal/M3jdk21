/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileSystemManager;

/** Real manager/FileObject virtual dispatch, with synthetic provider transports. */
class M3FsManagerBulkTest {
    @Test
    void routesAcrossProvidersAndPreservesMetadataOrder() {
        Harness provider = new Harness();
        provider.entries.put("ram:/both", new Entry(true, true, true));
        M3FsBulk.Batch batch = M3FsBulk.builder(0).stat("ram:/missing")
                .stat("ram:/both").mkdir("zip:/created")
                .copy("ram:/both", "zip:/copied").move("zip:/copied", "ram:/moved").build();
        M3FsBulk.Result result = M3FsManagerBulk.execute(provider.manager(), batch);
        assertEquals(5, result.size());
        assertEquals(5, result.successfulCount());
        assertEquals(M3FsBulk.META_CHECKED, result.metadataMask(0));
        assertEquals(M3FsBulk.META_CHECKED | M3FsBulk.META_EXISTS |
                M3FsBulk.META_FILE | M3FsBulk.META_DIRECTORY, result.metadataMask(1));
        assertEquals(List.of("stat ram:/missing", "stat ram:/both", "mkdir zip:/created",
                "copy ram:/both -> zip:/copied", "move zip:/copied -> ram:/moved"), provider.events);
    }

    @Test
    void primitiveSelectionAndWindowLeaveUnselectedSlotsUntouched() {
        Harness provider = new Harness();
        M3FsBulk.Batch batch = M3FsBulk.builder(2).mkdir("a:/zero")
                .mkdir("a:/one").mkdir("b:/two").mkdir("b:/three").mkdir("a:/four").build();
        long[] bits = { (1L << 0) | (1L << 2) | (1L << 3) | (1L << 4) };
        M3FsBulk.Result result = M3FsManagerBulk.execute(provider.manager(), batch, 1, 3, bits);
        assertEquals(List.of("mkdir b:/two", "mkdir b:/three"), provider.events);
        for (int i : new int[] { 0, 1, 4 }) assertEquals(M3FsBulk.SKIPPED, result.status(i));
        assertEquals(M3FsBulk.OK, result.status(2));
        assertEquals(M3FsBulk.OK, result.status(3));
        assertEquals(2, result.successfulCount());
    }

    @Test
    void snapshotSelectionBeforeProviderCallbacksAndAcrossWordBoundaries() {
        Harness provider = new Harness();
        M3FsBulk.Builder builder = M3FsBulk.builder(0);
        for (int i = 0; i < 130; i++) builder.mkdir("ram:/" + i);
        long[] bits = new long[3];
        bits[0] = 1L << 63;
        bits[1] = (1L << 0) | (1L << 63);
        bits[2] = 1L;
        provider.onFirstOperation = () -> java.util.Arrays.fill(bits, 0L);
        M3FsBulk.Result result = M3FsManagerBulk.execute(provider.manager(), builder.build(), bits);
        assertEquals(List.of("mkdir ram:/63", "mkdir ram:/64", "mkdir ram:/127", "mkdir ram:/128"),
                provider.events);
        assertEquals(4, result.successfulCount());
        assertEquals(M3FsBulk.SKIPPED, result.status(129));
    }

    @Test
    void checkedFailureAndFailFastRetainPartialResults() {
        Harness provider = new Harness();
        AtomicInteger worked = new AtomicInteger();
        M3FsBulk.Batch batch = M3FsBulk.builder(0)
                .progress(M3FsProgress.of(() -> false, ignored -> worked.incrementAndGet()))
                .failFast(true).mkdir("ram:/ok").delete("zip:/failure")
                .stat("ram:/later").build();
        M3FsBulk.Result result = M3FsManagerBulk.execute(provider.manager(), batch);
        assertEquals(M3FsBulk.OK, result.status(0));
        assertEquals(M3FsBulk.FAILED, result.status(1));
        assertNotNull(result.error(1));
        assertEquals(M3FsBulk.SKIPPED, result.status(2));
        assertEquals(2, worked.get());
        assertEquals(List.of("mkdir ram:/ok", "delete zip:/failure"), provider.events);
    }

    @Test
    void geometryAndAtomicRequestsRefuseBeforeProviderIO() {
        Harness provider = new Harness();
        M3FsBulk.Batch batch = M3FsBulk.builder(0).mkdir("a:/0").build();
        assertThrows(IndexOutOfBoundsException.class,
                () -> M3FsManagerBulk.execute(provider.manager(), batch, -1, 1, null));
        assertThrows(IndexOutOfBoundsException.class,
                () -> M3FsManagerBulk.execute(provider.manager(), batch, 1, 1, null));
        assertThrows(IllegalArgumentException.class,
                () -> M3FsManagerBulk.execute(provider.manager(), batch, new long[0]));
        assertThrows(NullPointerException.class,
                () -> M3FsManagerBulk.execute(null, batch));
        assertThrows(UnsupportedOperationException.class,
                () -> M3FsManagerBulk.execute(provider.manager(),
                        M3FsBulk.builder(0).atomic(true).mkdir("a:/0").build()));
        assertTrue(provider.events.isEmpty());
    }

    @Test
    void fileSystemManagerOwnsBulkDefaultMethodsForAllRegisteredProviders() {
        Harness provider = new Harness();
        FileSystemManager manager = provider.manager();
        provider.entries.put("ram:/from", new Entry(true, true, false));
        M3FsBulk.Result stats = manager.statAll("ram:/from", "zip:/missing");
        assertEquals(2, stats.successfulCount());
        assertEquals(M3FsBulk.META_CHECKED | M3FsBulk.META_EXISTS | M3FsBulk.META_FILE,
                stats.metadataMask(0));
        assertEquals(M3FsBulk.META_CHECKED, stats.metadataMask(1));
        assertEquals(2, manager.createFolders("ram:/one", "zip:/two").successfulCount());
        assertEquals(1, manager.copyAll(new String[] { "ram:/from" },
                new String[] { "zip:/to" }).successfulCount());
        assertEquals(1, manager.moveAll(new String[] { "zip:/to" },
                new String[] { "ram:/moved" }).successfulCount());
        assertEquals(2, manager.deletePaths("ram:/one", "zip:/two").successfulCount());
        assertEquals(List.of("stat ram:/from", "stat zip:/missing", "mkdir ram:/one",
                "mkdir zip:/two", "copy ram:/from -> zip:/to",
                "move zip:/to -> ram:/moved", "delete ram:/one", "delete zip:/two"), provider.events);
    }

    @Test
    void managerDefaultMaskWindowsPreserveAbsolutePhysicalSlots() {
        Harness provider = new Harness();
        FileSystemManager manager = provider.manager();
        M3FsBulk.Batch batch = M3FsBulk.builder(0).mkdir("ram:/a")
                .mkdir("zip:/b").mkdir("ram:/c").mkdir("zip:/d").build();
        long[] selected = { 0b1101L };
        M3FsBulk.Result result = manager.executeM3Batch(batch, 1, 3, selected);
        assertEquals(List.of("mkdir ram:/c", "mkdir zip:/d"), provider.events);
        assertEquals(M3FsBulk.SKIPPED, result.status(0));
        assertEquals(M3FsBulk.SKIPPED, result.status(1));
        assertEquals(2, result.successfulCount());
        provider.events.clear();
        assertEquals(3, manager.executeM3Batch(batch, selected).successfulCount());
        provider.events.clear();
        assertEquals(4, manager.executeM3Batch(batch).successfulCount());
    }

    @Test
    void managerBulkPairedDefaultsRejectMalformedInputBeforeAnyIO() {
        Harness provider = new Harness();
        FileSystemManager manager = provider.manager();
        assertThrows(NullPointerException.class, () -> manager.statAll((String[]) null));
        assertThrows(NullPointerException.class, () -> manager.createFolders((String[]) null));
        assertThrows(IllegalArgumentException.class, () -> manager.copyAll(
                new String[] { "ram:/from" }, new String[0]));
        assertThrows(NullPointerException.class, () -> manager.moveAll(
                new String[] { "ram:/from" }, new String[] { null }));
        assertThrows(UnsupportedOperationException.class, () -> manager.executeM3Batch(
                M3FsBulk.builder(0).atomic(true).stat("ram:/from").build()));
        assertTrue(provider.events.isEmpty());
    }

    @Test
    void sparseWordCursorMatchesLinearReferenceForRandomWindows() {
        java.util.Random random = new java.util.Random(0x3B17);
        for (int trial = 0; trial < 320; trial++) {
            int length = random.nextInt(250);
            M3FsBulk.Builder builder = M3FsBulk.builder(0);
            for (int i = 0; i < length; i++) builder.mkdir("ram:/" + i);
            int first = random.nextInt(length + 1);
            int count = random.nextInt(length - first + 1);
            long[] selected = new long[(length + 63) >>> 6];
            for (int i = 0; i < length; i++) {
                if (random.nextInt(trial % 2 == 0 ? 43 : 2) == 0) {
                    selected[i >>> 6] |= 1L << (i & 63);
                }
            }
            Harness harness = new Harness();
            M3FsBulk.Result result = harness.manager().executeM3Batch(
                    builder.build(), first, count, selected);
            List<String> expected = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                boolean active = i >= first && i < first + count
                        && (selected[i >>> 6] & (1L << (i & 63))) != 0;
                if (active) expected.add("mkdir ram:/" + i);
                assertEquals(active ? M3FsBulk.OK : M3FsBulk.SKIPPED, result.status(i));
            }
            assertEquals(expected, harness.events);
            assertEquals(expected.size(), result.successfulCount());
        }
    }

    @Test
    void emptyMaskAndZeroWindowMustNotCallProviders() {
        Harness harness = new Harness();
        M3FsBulk.Builder builder = M3FsBulk.builder(0);
        for (int i = 0; i < 130; i++) builder.mkdir("ram:/" + i);
        M3FsBulk.Batch batch = builder.build();
        M3FsBulk.Result empty = harness.manager().executeM3Batch(batch, 13, 115, new long[3]);
        assertEquals(0, empty.successfulCount());
        M3FsBulk.Result zeroWindow = harness.manager().executeM3Batch(batch, 130, 0, null);
        assertEquals(0, zeroWindow.successfulCount());
        assertTrue(harness.events.isEmpty());
    }

    private record Entry(boolean exists, boolean file, boolean directory) { }

    private static final class Harness {
        final List<String> events = new ArrayList<>();
        final Map<String, Entry> entries = new HashMap<>();
        Runnable onFirstOperation;

        FileSystemManager manager() {
            return (FileSystemManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { FileSystemManager.class }, (p, method, args) -> {
                        if (method.isDefault()) return java.lang.reflect.InvocationHandler.invokeDefault(p, method, args);
                        if (method.getName().equals("resolveFile")) return file((String) args[0]);
                        throw new UnsupportedOperationException(method.getName());
                    });
        }

        FileObject file(String uri) {
            return (FileObject) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { FileObject.class }, (p, method, args) -> {
                        String op = method.getName();
                        if (op.equals("exists") || op.equals("isFile") || op.equals("isFolder")) {
                            if (op.equals("exists")) events.add("stat " + uri);
                            Entry entry = entries.getOrDefault(uri, new Entry(false, false, false));
                            return switch (op) {
                                case "exists" -> entry.exists();
                                case "isFile" -> entry.file();
                                default -> entry.directory();
                            };
                        }
                        if (onFirstOperation != null) {
                            Runnable callback = onFirstOperation;
                            onFirstOperation = null;
                            callback.run();
                        }
                        switch (op) {
                            case "createFolder" -> {
                                events.add("mkdir " + uri);
                                entries.put(uri, new Entry(true, false, true));
                                return null;
                            }
                            case "delete", "deleteAll" -> {
                                events.add("delete " + uri);
                                if (uri.endsWith("/failure")) throw new FileSystemException("synthetic failure");
                                Entry removed = entries.remove(uri);
                                return op.equals("delete") ? removed != null : removed == null ? 0 : 1;
                            }
                            case "copyFrom" -> {
                                String from = args[0].toString();
                                events.add("copy " + from + " -> " + uri);
                                entries.put(uri, new Entry(true, true, false));
                                return null;
                            }
                            case "moveTo" -> {
                                String to = args[0].toString();
                                events.add("move " + uri + " -> " + to);
                                entries.put(to, entries.getOrDefault(uri, new Entry(true, true, false)));
                                entries.remove(uri);
                                return null;
                            }
                            case "toString" -> { return uri; }
                            default -> throw new UnsupportedOperationException(op);
                        }
                    });
        }
    }
}
