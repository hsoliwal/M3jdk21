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
 * @summary Bind mapped UTF8 payloads to UTF16, including replacement and chunk boundaries
 * @modules java.base/jdk.internal.mindex
 * @build MIndexMappedStringBackingTest
 * @run main/othervm -Xmx64m MapUtfTest
 */

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;
import jdk.internal.mindex.MIndexMappedStringBacking;

/** Tests the actual mapped owner, reusing the existing independently written fixture writer. */
public class MapUtfTest {
    private static final List<String> BAD_TEXT = List.of(
            "ABC", "ABC", "ABC", "", "\0", "\uD800", "\uDC00", "\uD83D\uDE00",
            "\uFFFF", "\uFFFF", "\uD83D\uDE00", "\uD83D\uDE00", "\u00e9",
            "\uD800\uD800", "\uD800\uDC00", "A");
    private static final List<byte[]> BAD_BYTES = List.of(
            bytes(65, 66, 68), bytes(65, 66), bytes(65, 66, 67, 68), bytes(65),
            bytes(0xc0, 0x80), bytes(0xed, 0xa0, 0x80), bytes(0xef, 0xbf, 0xbd),
            bytes(0xed, 0xa0, 0xbd, 0xed, 0xb8, 0x80), bytes(0xe2, 0x28, 0xa1),
            bytes(0x80), bytes(0xf4, 0x90, 0x80, 0x80), bytes(0xf0, 0x9f, 0x98),
            bytes(0x65, 0xcc, 0x81), bytes(63), bytes(63, 63), bytes(65, 0));
    private static int checks;
    private static int specimens;
    private static int descriptors;

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "all" : args[0];
        Path directory = Files.createTempDirectory("m3-map-utf");
        if (mode.equals("all") || mode.equals("valid")) valid(directory);
        if (mode.equals("all") || mode.equals("invalid")) {
            for (int index = 0; index < BAD_TEXT.size(); index++) invalid(directory, index);
        }
        if (mode.startsWith("bad-")) invalid(directory, Integer.parseInt(mode.substring(4)));
        if (checks == 0) throw new IllegalArgumentException(mode);
        System.out.println("MAP_UTF_PASS checks=" + checks + " specimens=" + specimens
                + " fdChecks=" + descriptors + " mode=" + mode);
    }

    private static void valid(Path directory) throws Exception {
        for (String text : List.of("", "\0", "ABC", "\u007f\u0080\u07ff\u0800\uffff",
                "\uD800", "\uDC00", "\uD800\uD800\uDC00", "\uDC00\uD800\uDC00",
                "\uD800\uDC00\uDC00", "\uD800\uDC00", "\uDBFF\uDFFF", "\ufeff")) {
            verify(directory, text);
        }
        for (int prefix : new int[] {4093, 4094, 4095, 4096, 4097, 8191}) {
            for (String seam : List.of("\uD83D\uDE00", "\uD800", "\uDC00", "\uFFFF", "\u0080")) {
                verify(directory, "x".repeat(prefix) + seam + "y".repeat(4097));
            }
        }
        char[] all = new char[65536];
        for (int i = 0; i < all.length; i++) all[i] = (char) i;
        verify(directory, new String(all));
        Random random = new Random(0x4d33555446L);
        for (int sample = 0; sample < 128; sample++) {
            char[] units = new char[random.nextInt(200)];
            for (int i = 0; i < units.length; i++) units[i] = (char) random.nextInt(65536);
            verify(directory, new String(units));
        }
    }

    private static void verify(Path directory, String text) throws Exception {
        Path path = fixture(directory, text);
        byte[] before = Files.readAllBytes(path);
        byte[] priorBytes = Files.readAllBytes(sibling(path));
        byte[] expected = text.getBytes(StandardCharsets.UTF_8);
        try (var backing = MIndexMappedStringBacking.open(path)) {
            for (long id : new long[] {1L, (7L << 32) | 2L}) {
                check(backing.materialize(id).equals(text), "UTF16 not normalized or replaced");
                check(backing.utf8Length(id) == expected.length, "UTF8 byte count");
                ByteBuffer actual = backing.utf8View(id);
                check(actual.isReadOnly(), "read-only byte cursor");
                check(actual.remaining() == expected.length, "view length");
                for (byte unit : expected) check(actual.get() == unit, "exact UTF8 projection");
                check(!actual.hasRemaining(), "exact byte extent");
            }
        }
        check(Arrays.equals(before, Files.readAllBytes(path)), "text file unchanged");
        check(Arrays.equals(priorBytes, Files.readAllBytes(sibling(path))), "byte file unchanged");
    }

    private static void invalid(Path directory, int index) throws Exception {
        String text = BAD_TEXT.get(index);
        byte[] wrong = BAD_BYTES.get(index);
        check(!Arrays.equals(text.getBytes(StandardCharsets.UTF_8), wrong), "genuine mismatch");
        Path path = fixture(directory, text);
        replaceProjection(path, wrong);
        byte[] before = Files.readAllBytes(path);
        byte[] priorBytes = Files.readAllBytes(sibling(path));
        try (var accepted = MIndexMappedStringBacking.open(path)) {
            throw new AssertionError("ACCEPTED_UTF8_MISMATCH case=" + index
                    + " scalarLength=" + accepted.length(1L) + " bytes=" + wrong.length);
        } catch (IllegalStateException refused) {
            check(refused.getMessage().equals("corrupt MIndex mapped backing: text UTF-8 payload at 64"),
                    "typed refusal: " + refused);
        }
        check(Arrays.equals(before, Files.readAllBytes(path)), "refused text unchanged");
        check(Arrays.equals(priorBytes, Files.readAllBytes(sibling(path))), "refused bytes unchanged");
        Path proc = Path.of("/proc/self/fd");
        if (Files.isDirectory(proc)) {
            try (var fds = Files.list(proc)) {
                for (Path fd : fds.toList()) {
                    try {
                        Path target = Files.readSymbolicLink(fd);
                        check(!target.equals(path) && !target.equals(sibling(path)), "failed-open channel released");
                    } catch (java.nio.file.NoSuchFileException closedDuringListing) {
                        // The enumeration descriptor may already have closed.
                    }
                }
            }
            descriptors++;
        }
    }

    /** Reframe one well-formed byte record; CRC, FNV and both text lengths agree with wrong bytes. */
    private static void replaceProjection(Path path, byte[] payload) throws Exception {
        byte[] old = Files.readAllBytes(sibling(path));
        int recordBytes = (32 + payload.length + 7) & ~7;
        byte[] bytes = Arrays.copyOf(old, 64 + recordBytes);
        Arrays.fill(bytes, 96, bytes.length, (byte) 0);
        ByteBuffer file = ByteBuffer.wrap(bytes);
        file.putInt(68, recordBytes);
        file.putInt(72, payload.length);
        int hash = 0x811c9dc5;
        for (byte b : payload) { hash ^= b & 0xff; hash *= 0x01000193; }
        file.putInt(76, hash);
        CRC32 crc = new CRC32();
        crc.update(payload);
        file.putInt(80, (int) crc.getValue());
        System.arraycopy(payload, 0, bytes, 96, payload.length);
        file.putLong(48, bytes.length);
        crc.reset(); crc.update(bytes, 40, 20); file.putInt(60, (int) crc.getValue());
        Files.write(sibling(path), bytes);
        byte[] text = Files.readAllBytes(path);
        ByteBuffer header = ByteBuffer.wrap(text);
        header.putInt(64 + 16, payload.length);
        header.putInt(64 + header.getInt(68) + 16, payload.length);
        Files.write(path, text);
    }

    private static Path fixture(Path directory, String text) throws Exception {
        Path path = directory.resolve("utf-" + specimens++ + ".midx");
        Method writer = MIndexMappedStringBackingTest.class.getDeclaredMethod("writeFixture", Path.class, String.class);
        writer.setAccessible(true);
        writer.invoke(null, path, text);
        return path;
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) result[i] = (byte) values[i];
        return result;
    }

    private static Path sibling(Path path) { return Path.of(path.toString() + ".bytes"); }

    private static void check(boolean condition, String why) {
        checks++;
        if (!condition) throw new AssertionError(why);
    }
}
