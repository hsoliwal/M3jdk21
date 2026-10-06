/*
 * Copyright (c) 2026, Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @summary Bound mapped-store row allocation and preserve failed-open ownership
 * @modules java.base/jdk.internal.mindex
 * @build M3MappedStringBackingTest
 * @run main/othervm -Xmx32m --add-opens=java.base/jdk.internal.mindex=ALL-UNNAMED MapGuardTest
 */

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import jdk.internal.mindex.M3MappedStringBacking;

/** ContractProbe: real files, the actual parser, bounded heap, no alternate implementation. */
public class MapGuardTest {
    private static final int HEADER = 64;
    private static int checks;

    public static void main(String[] args) throws Exception {
        String group = args.length == 0 ? "all" : args[0];
        Path dir = Files.createTempDirectory("m3-map-guard");
        if (group.equals("all") || group.equals("text")) badCounts(dir, true);
        if (group.equals("all") || group.equals("byte")) badCounts(dir, false);
        if (group.equals("all") || group.equals("valid")) valid(dir);
        if (group.equals("all") || group.equals("fd")) channels(dir);
        if (group.equals("all") || group.equals("cleanup")) cleanup();
        if (!List.of("all", "text", "byte", "valid", "fd", "cleanup").contains(group)) {
            throw new IllegalArgumentException(group);
        }
        check(checks > 0, "requested group executed");
        System.out.println("MAP_GUARD_PASS group=" + group + " checks=" + checks);
    }

    private static void badCounts(Path dir, boolean text) throws Exception {
        Path path = dir.resolve(text ? "text.midx" : "byte.midx");
        Path bytes = Path.of(path + ".bytes");
        for (int rows : new int[] {Integer.MAX_VALUE, 1, 2, 100_000_000}) {
            frame(path, true, HEADER, text ? rows : 0);
            frame(bytes, false, HEADER, text ? 0 : rows);
            reject(path, "invalid " + (text ? "text" : "byte") + " commit");
        }
        // There is room for one record, but not two. Both commits have valid CRCs.
        int size = HEADER + (text ? 120 : 32);
        frame(path, true, text ? size : HEADER, text ? 2 : 0);
        frame(bytes, false, text ? HEADER : size, text ? 0 : 2);
        reject(path, "invalid " + (text ? "text" : "byte") + " commit");

        // File length, not physical uncommitted trailing bytes, bounds the count.
        Path target = text ? path : bytes;
        byte[] file = Files.readAllBytes(target);
        slot(file, 40, 2L, HEADER, 1);
        Files.write(target, file);
        reject(path, "invalid " + (text ? "text" : "byte") + " commit");

        // A corrupt CRC remains a slot-recovery case, not selected-commit corruption.
        file = Files.readAllBytes(target);
        file[63] ^= 1;
        Files.write(target, file);
        try (var backing = M3MappedStringBacking.open(path)) {
            check(backing.size() == 0, "older CRC-valid empty snapshot retained");
        }
    }

    private static void valid(Path dir) throws Exception {
        M3MappedStringBackingTest.main(new String[0]);
        Path path = dir.resolve("empty.midx");
        Path bytes = Path.of(path + ".bytes");
        frame(path, true, HEADER, 0);
        frame(bytes, false, HEADER, 0);
        byte[] originalText = Files.readAllBytes(path);
        byte[] originalBytes = Files.readAllBytes(bytes);
        M3MappedStringBacking backing = M3MappedStringBacking.open(path);
        try {
            check(backing.size() == 0, "empty is valid");
            check(backing.path().equals(path.toAbsolutePath().normalize()), "path preserved");
            try {
                backing.length(1);
                throw new AssertionError("unknown ID accepted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
        } finally {
            backing.close();
        }
        backing.close();
        try {
            backing.length(1);
            throw new AssertionError("closed backing accepted");
        } catch (IllegalStateException expected) {
            checks++;
        }
        check(Arrays.equals(originalText, Files.readAllBytes(path)), "text never mutated");
        check(Arrays.equals(originalBytes, Files.readAllBytes(bytes)), "bytes never mutated");
        try (var reopened = M3MappedStringBacking.open(path)) {
            check(reopened.size() == 0, "reopen remains valid");
        }

        // Reuse the original fixture writer at the exact minimum record sizes:
        // empty text records/aliases are120 bytes; the empty byte record is32 bytes.
        Method fixture = M3MappedStringBackingTest.class
                .getDeclaredMethod("writeFixture", Path.class, String.class);
        fixture.setAccessible(true);
        Path minimum = dir.resolve("minimum.midx");
        fixture.invoke(null, minimum, "");
        try (var smallest = M3MappedStringBacking.open(minimum)) {
            check(smallest.size() == 2, "minimum-sized text records and alias admitted");
            check(smallest.length(1L) == 0, "empty scalar");
            check(smallest.utf8Length(1L) == 0, "minimum-sized byte record admitted");
            check(smallest.length((7L << 32) | 2L) == 0, "empty alias");
            check(smallest.materialize(1L).isEmpty(), "empty materialization");
        }

        // Only slot zero valid and greater committed length rejected.
        byte[] text = originalText.clone();
        Arrays.fill(text, 40, 64, (byte) 0);
        Files.write(path, text);
        try (var first = M3MappedStringBacking.open(path)) {
            check(first.size() == 0, "slot zero alone");
        }
        slot(text, 40, 3L, HEADER + 1L, 0);
        Files.write(path, text);
        reject(path, "invalid text commit");
        Files.write(path, originalText);
        Files.delete(bytes);
        try {
            M3MappedStringBacking.open(path);
            throw new AssertionError("missing byte store accepted");
        } catch (UncheckedIOException expected) {
            check(expected.getCause() instanceof java.nio.file.NoSuchFileException,
                    "original checked cause is retained");
        }
    }

    private static void channels(Path dir) throws Exception {
        Path fdDir = Path.of("/proc/self/fd");
        if (!Files.isDirectory(fdDir)) {
            System.out.println("MAP_GUARD_FD_NOT_APPLICABLE /proc/self/fd absent");
            checks++; // Applicability, not a successful non-Linux FD observation.
            return;
        }
        Path text = dir.resolve("short.midx");
        Files.write(text, new byte[1]);
        long initial = descriptors(text);
        for (int i = 0; i < 16; i++) reject(text, "short text header");
        check(descriptors(text) == initial, "failed outer header must close its channel");

        Path bytes = dir.resolve("short.bytes");
        Files.write(bytes, new byte[1]);
        Class<?> type = Class.forName("jdk.internal.mindex.M3MappedStringBacking$ByteStore");
        Constructor<?> ctor = type.getDeclaredConstructor(Path.class);
        ctor.setAccessible(true);
        initial = descriptors(bytes);
        for (int i = 0; i < 16; i++) {
            try {
                ctor.newInstance(bytes);
                throw new AssertionError("short byte store accepted");
            } catch (InvocationTargetException expected) {
                check(expected.getCause() instanceof IllegalStateException, "byte rejection cause");
            }
        }
        check(descriptors(bytes) == initial, "failed byte header must close its channel");
        Path pair = dir.resolve("pair.midx");
        Path sibling = Path.of(pair + ".bytes");
        frame(pair, true, HEADER, 0);
        Files.write(sibling, new byte[1]);
        reject(pair, "short byte header");
        check(descriptors(pair) == 0 && descriptors(sibling) == 0,
                "nested failure closes both channels");

        frame(sibling, false, HEADER + 32, 1);
        byte[] badRecord = Files.readAllBytes(sibling);
        ByteBuffer.wrap(badRecord).putInt(HEADER, 0xdeadbeef);
        Files.write(sibling, badRecord);
        reject(pair, "byte record magic at " + HEADER);
        check(descriptors(pair) == 0 && descriptors(sibling) == 0,
                "byte parse failure after mapping closes both channels");

        frame(sibling, false, HEADER, 0);
        frame(pair, true, HEADER + 120, 1);
        badRecord = Files.readAllBytes(pair);
        ByteBuffer.wrap(badRecord).putInt(HEADER, 0xdeadbeef);
        Files.write(pair, badRecord);
        reject(pair, "text record magic at " + HEADER);
        check(descriptors(pair) == 0 && descriptors(sibling) == 0,
                "text parse failure closes the successfully acquired byte store");

        frame(pair, true, HEADER, 0);
        Files.delete(sibling);
        try {
            M3MappedStringBacking.open(pair);
            throw new AssertionError("missing sibling accepted");
        } catch (UncheckedIOException expected) {
            check(expected.getCause() instanceof java.nio.file.NoSuchFileException,
                    "I/O cause retained");
        }
        check(descriptors(pair) == 0, "I/O acquisition failure closes text channel");
        System.out.println("MAP_GUARD_FD_PASS outer=16 inner=16 nested=4");
    }

    private static void cleanup() throws Exception {
        Method close = M3MappedStringBacking.class.getDeclaredMethod(
                "close", AutoCloseable.class, Throwable.class);
        close.setAccessible(true);
        Throwable primary = new IOException("primary");
        AtomicInteger calls = new AtomicInteger();
        close.invoke(null, (AutoCloseable) () -> calls.incrementAndGet(), primary);
        check(calls.get() == 1, "close invoked once");
        check(primary.getSuppressed().length == 0, "successful cleanup adds no failure");
        close.invoke(null, null, primary);
        check(primary.getSuppressed().length == 0, "unacquired resource");

        IOException secondary = new IOException("secondary");
        close.invoke(null, (AutoCloseable) () -> { throw secondary; }, primary);
        check(primary.getSuppressed().length == 1 && primary.getSuppressed()[0] == secondary,
                "checked cleanup failure suppressed on original");
        close.invoke(null, (AutoCloseable) () -> { throw (IOException) primary; }, primary);
        check(primary.getSuppressed().length == 1, "no self-suppression");

        AssertionError error = new AssertionError("cleanup error");
        close.invoke(null, (AutoCloseable) () -> { throw error; }, primary);
        check(primary.getSuppressed().length == 2 && primary.getSuppressed()[1] == error,
                "cleanup error cannot mask original");
    }

    private static long descriptors(Path path) throws IOException {
        String target = path.toAbsolutePath().normalize().toString();
        long count = 0;
        List<Path> descriptors;
        try (var files = Files.list(Path.of("/proc/self/fd"))) {
            descriptors = files.toList();
        }
        for (Path descriptor : descriptors) {
            try {
                if (Files.readSymbolicLink(descriptor).toString().equals(target)) count++;
            } catch (java.nio.file.NoSuchFileException closedDuringEnumeration) {
                // The enumeration's own descriptor closes when the stream closes.
            }
        }
        return count;
    }

    private static void reject(Path path, String detail) {
        try {
            M3MappedStringBacking.open(path);
            throw new AssertionError("corrupt snapshot accepted: " + detail);
        } catch (IllegalStateException expected) {
            check(expected.getMessage().equals("corrupt MIndex mapped backing: " + detail),
                    "precise rejection: " + expected);
        }
    }

    private static void frame(Path path, boolean text, int size, int rows) throws IOException {
        byte[] data = new byte[size];
        ByteBuffer header = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        header.putInt(0, text ? 0x4d49584d : 0x4d49424d);
        header.putInt(4, text ? 3 : 1);
        header.putInt(8, HEADER);
        slot(data, 16, 1L, HEADER, 0);
        slot(data, 40, 2L, size, rows);
        Files.write(path, data);
    }

    private static void slot(byte[] data, int offset, long generation, long committed, int rows) {
        ByteBuffer header = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        header.putLong(offset, generation);
        header.putLong(offset + 8, committed);
        header.putInt(offset + 16, rows);
        CRC32 crc = new CRC32();
        crc.update(data, offset, 20);
        header.putInt(offset + 20, (int) crc.getValue());
    }

    private static void check(boolean result, String contract) {
        checks++;
        if (!result) throw new AssertionError(contract);
    }
}
