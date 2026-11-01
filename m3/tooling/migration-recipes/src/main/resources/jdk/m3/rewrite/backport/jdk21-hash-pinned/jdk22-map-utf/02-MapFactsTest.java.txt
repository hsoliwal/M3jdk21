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
 * @summary Validate mapped String facts and reject CRC-valid inconsistent headers
 * @modules java.base/jdk.internal.mindex
 * @build MIndexMappedStringBackingTest
 * @run main/othervm -Xmx64m MapFactsTest
 */

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;
import jdk.internal.mindex.MIndexMappedStringBacking;

/** Exercises the actual backing owner, using the unchanged original wire-format writer. */
public class MapFactsTest {
    private static final int[] FACT_OFFSETS = {20, 24, 28, 32, 36, 40};
    private static final String MIXED = "A\uD800x\uD83D\uDE00\uDC00Z";
    private static final Method WRITER = writer();
    private static int checks;
    private static int specimens;
    private static int fdChecks;

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "all" : args[0];
        Path directory = Files.createTempDirectory("m3-map-facts");
        if (mode.equals("all") || mode.equals("valid")) valid(directory);
        if (mode.equals("all") || mode.equals("corrupt")) {
            for (int field : FACT_OFFSETS) {
                corrupt(directory, false, field);
                corrupt(directory, true, field);
            }
            payload(directory);
            chainCorruption(directory);
        }
        if (mode.startsWith("scalar-")) corrupt(directory, false, Integer.parseInt(mode.substring(7)));
        if (mode.startsWith("alias-")) corrupt(directory, true, Integer.parseInt(mode.substring(6)));
        if (mode.equals("all") || mode.equals("fd")) failedOpen(directory);
        if (checks == 0) throw new IllegalArgumentException("unknown test mode: " + mode);
        System.out.println("MAP_FACTS_PASS checks=" + checks + " specimens=" + specimens
                + " fdChecks=" + fdChecks + " mode=" + mode);
    }

    private static void valid(Path directory) throws Exception {
        for (String value : List.of("", "A", "\0", "\uFFFF", "\uD800", "\uDC00",
                "\uD800\uDC00", "\uD800\uD800\uDC00", "\uDC00\uD800\uDC00",
                "\uD800\uDC00\uDC00", "\uD800\uD800", "\uDC00\uDC00", MIXED,
                "abcdefghijklmnopqrstuvwxyz".repeat(11))) {
            verify(directory, value, 3);
        }
        // Exhaustive UTF16 unit alphabet in one actual mapped scalar.
        char[] all = new char[65536];
        for (int index = 0; index < all.length; index++) all[index] = (char) index;
        verify(directory, new String(all), 1);
        Random random = new Random(0x4d334641435453L);
        for (int sample = 0; sample < 128; sample++) {
            char[] units = new char[random.nextInt(97)];
            for (int index = 0; index < units.length; index++) {
                units[index] = switch (random.nextInt(4)) {
                    case 0 -> (char) (0xd800 + random.nextInt(1024));
                    case 1 -> (char) (0xdc00 + random.nextInt(1024));
                    default -> (char) random.nextInt(65536);
                };
            }
            verify(directory, new String(units), 2);
        }
    }

    private static void verify(Path directory, String value, int extraAliases) throws Exception {
        Path path = fixture(directory, value);
        appendAliases(path, extraAliases);
        byte[] text = Files.readAllBytes(path);
        byte[] bytes = Files.readAllBytes(sibling(path));
        try (var backing = MIndexMappedStringBacking.open(path)) {
            check(backing.size() == 2 + extraAliases, "alias count");
            for (int row = 0; row < backing.size(); row++) {
                int language = row == 0 ? 0 : row == 1 ? 7 : 20 + row;
                long id = ((long) language << 32) | (row + 1L);
                int power = 1;
                for (int index = 0; index < value.length(); index++) power *= 31;
                check(backing.hashCode(id) == value.hashCode(), "String hash");
                check(backing.hash31Power(id) == power, "power31 with overflow");
                check(backing.codePointCount(id) == value.codePointCount(0, value.length()), "code points");
                long unpaired = value.codePoints().filter(cp -> cp >= 0xd800 && cp <= 0xdfff).count();
                check(backing.unpairedSurrogateCount(id) == unpaired, "unpaired surrogates");
                check(backing.firstUtf16Unit(id) == (value.isEmpty() ? -1 : value.charAt(0)), "first unit");
                check(backing.lastUtf16Unit(id) == (value.isEmpty() ? -1 : value.charAt(value.length() - 1)), "last unit");
                check(backing.view(id).hashCode() == value.hashCode(), "full-view hash");
                check(backing.view(id).codePointCount() == backing.codePointCount(id), "fact/view parity");
                check(backing.materialize(id).equals(value), "exact UTF16 payload");
                check(backing.contentEquals(id, value), "content equality");
                check(backing.utf16View(id).isReadOnly(), "read-only payload");
                check(backing.utf8Handle(id) == 1L, "same byte handle");
                for (int index = 0; index < value.length(); index++) {
                    check(backing.charAt(id, index) == value.charAt(index), "charAt");
                }
                int limit = Math.min(value.length(), 16);
                for (int end = 0; end <= limit; end++) {
                    for (int start = 0; start <= end; start++) {
                        String slice = value.substring(start, end);
                        var view = backing.view(id, start, end);
                        check(view.hashCode() == slice.hashCode(), "slice hash including surrogate seams");
                        check(view.codePointCount() == slice.codePointCount(0, slice.length()), "slice points");
                    }
                }
            }
        }
        check(Arrays.equals(text, Files.readAllBytes(path)), "text unchanged");
        check(Arrays.equals(bytes, Files.readAllBytes(sibling(path))), "bytes unchanged");
    }

    private static void corrupt(Path directory, boolean alias, int field) throws Exception {
        check(Arrays.stream(FACT_OFFSETS).anyMatch(value -> value == field), "known fact offset");
        Path path = fixture(directory, MIXED);
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        int record = alias ? 64 + image.getInt(68) : 64;
        int previous = image.getInt(record + field);
        image.putInt(record + field, previous ^ 0x40000001);
        // Correlated scalar+alias corruption must not pass merely because headers agree.
        if (!alias) image.putInt(64 + image.getInt(68) + field, previous ^ 0x40000001);
        Files.write(path, bytes);
        String expected = alias ? "text payload alias facts at " : "text record facts at ";
        reject(path, expected + record, "CRC-valid " + (alias ? "alias" : "scalar") + " field=" + field);
    }

    private static void payload(Path directory) throws Exception {
        Path path = fixture(directory, MIXED);
        byte[] bytes = Files.readAllBytes(path);
        bytes[64 + 120] ^= 1;
        Files.write(path, bytes);
        reject(path, "text record CRC at 64", "original CRC guard");
    }

    private static void chainCorruption(Path directory) throws Exception {
        Path path = fixture(directory, MIXED);
        appendAliases(path, 3);
        byte[] bytes = Files.readAllBytes(path);
        int last = bytes.length - 120;
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        image.putInt(last + 24, 100);
        Files.write(path, bytes);
        reject(path, "text payload alias facts at " + last, "transitive alias facts");
    }

    private static void failedOpen(Path directory) throws Exception {
        Path proc = Path.of("/proc/self/fd");
        if (!Files.isDirectory(proc)) {
            System.out.println("MAP_FACTS_FD_NOT_APPLICABLE platform=" + System.getProperty("os.name"));
            return;
        }
        Path path = fixture(directory, MIXED);
        byte[] image = Files.readAllBytes(path);
        ByteBuffer.wrap(image).putInt(64 + 28, 1234);
        Files.write(path, image);
        for (int attempt = 0; attempt < 24; attempt++) {
            reject(path, "text record facts at 64", "close on fact refusal");
            try (var fds = Files.list(proc)) {
                long open = fds.filter(fd -> {
                    try {
                        String target = Files.readSymbolicLink(fd).toString();
                        return target.equals(path.toString()) || target.equals(sibling(path).toString());
                    } catch (java.io.IOException concurrentClose) { return false; }
                }).count();
                check(open == 0, "failed-open file descriptors");
                fdChecks++;
            }
        }
    }

    private static void reject(Path path, String detail, String label) throws Exception {
        byte[] before = Files.readAllBytes(path);
        byte[] byteBefore = Files.readAllBytes(sibling(path));
        try (var accepted = MIndexMappedStringBacking.open(path)) {
            throw new AssertionError("ACCEPTED_INCONSISTENT_FACTS " + label
                    + " hash=" + accepted.hashCode(1L)
                    + " viewHash=" + accepted.view(1L).hashCode()
                    + " actualStringHash=" + accepted.materialize(1L).hashCode());
        } catch (IllegalStateException expected) {
            check(expected.getMessage().equals("corrupt MIndex mapped backing: " + detail),
                    label + ": " + expected.getMessage());
        }
        check(Arrays.equals(before, Files.readAllBytes(path)), "refused text unchanged");
        check(Arrays.equals(byteBefore, Files.readAllBytes(sibling(path))), "refused byte store unchanged");
    }

    private static Path fixture(Path directory, String text) throws Exception {
        Path path = directory.resolve("facts-" + specimens++ + ".midx");
        WRITER.invoke(null, path, text);
        return path;
    }

    private static Method writer() {
        try {
            Method method = MIndexMappedStringBackingTest.class.getDeclaredMethod("writeFixture", Path.class, String.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }

    private static Path sibling(Path path) { return Path.of(path.toString() + ".bytes"); }

    /** Extend the original writer's alias row to a backwards-only chain; no new payload format. */
    private static void appendAliases(Path path, int count) throws Exception {
        byte[] original = Files.readAllBytes(path);
        byte[] extended = Arrays.copyOf(original, original.length + count * 120);
        ByteBuffer bytes = ByteBuffer.wrap(extended).order(ByteOrder.BIG_ENDIAN);
        int alias = 64 + bytes.getInt(68);
        for (int index = 0; index < count; index++) {
            int target = original.length + index * 120;
            System.arraycopy(original, alias, extended, target, 120);
            bytes.putInt(target + 8, 22 + index);
            bytes.putInt(target + 44, 2 + index);
        }
        bytes.putLong(48, extended.length);
        bytes.putInt(56, 2 + count);
        CRC32 crc = new CRC32();
        crc.update(extended, 40, 20);
        bytes.putInt(60, (int) crc.getValue());
        Files.write(path, extended);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
