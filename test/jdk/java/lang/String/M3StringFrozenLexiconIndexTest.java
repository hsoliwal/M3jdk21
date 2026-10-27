/*
 * Copyright (c) 2026, M3 contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

/*
 * @test
 * @summary Frozen UTF-16 one-candidate row lookup, collision authority and image validation
 * @modules java.base/java.lang:+open
 * @run main/othervm M3StringFrozenLexiconIndexTest
 * @run main/othervm -XX:-CompactStrings M3StringFrozenLexiconIndexTest
 */

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32C;

public class M3StringFrozenLexiconIndexTest {
    private static final Class<?> LEXICON;
    private static final Class<?> DIRECT_INDEX;
    private static final Method OPEN;
    private static final Method FIND;
    private static final Method FIND_COMPACT;
    private static final Method FIND_LATIN1;
    private static final Method FIND_POINTS;
    private static final Method FIND_UNIT;
    private static final Method ATOM;
    private static final Method ATOM_CHAR;
    private static final Method UTF16_BYTES;
    private static final Method DIRECT_BUILD;
    private static final Method DIRECT_CANDIDATE;
    private static final Method DIRECT_ROWS;
    private static final Method DIRECT_DISPLACEMENTS;
    private static final Field CODER;
    private static final boolean COMPACT;
    private static final List<Path> FILES = new ArrayList<>();
    private static int checks;

    static {
        try {
            LEXICON = Class.forName("java.lang.M3StringPool$Lexicon");
            DIRECT_INDEX = Class.forName("java.lang.M3FrozenLexiconIndex");
            OPEN = method(LEXICON, "open", Path.class);
            FIND = method(LEXICON, "find", char[].class, int.class, int.class);
            FIND_COMPACT = method(LEXICON, "findCompactBytes", byte[].class,
                    int.class, int.class, byte.class);
            FIND_LATIN1 = method(LEXICON, "findLatin1Bytes", byte[].class, int.class, int.class);
            FIND_POINTS = method(LEXICON, "findCodePoints", int[].class,
                    int.class, int.class, int.class);
            FIND_UNIT = method(LEXICON, "findUnit", char.class);
            ATOM = method(LEXICON, "atom", int.class);
            ATOM_CHAR = method(Class.forName("java.lang.M3StringAtom"), "charAt", int.class);
            UTF16_BYTES = method(Class.forName("java.lang.StringUTF16"), "toBytes",
                    char[].class, int.class, int.class);
            DIRECT_BUILD = method(DIRECT_INDEX, "build", int[].class, int[].class);
            DIRECT_CANDIDATE = method(DIRECT_INDEX, "candidate", int.class, int.class);
            DIRECT_ROWS = method(DIRECT_INDEX, "rows");
            DIRECT_DISPLACEMENTS = method(DIRECT_INDEX, "displacements");
            CODER = field(Class.forName("java.lang.M3StringOwner"), "coder");
            COMPACT = field(String.class, "COMPACT_STRINGS").getBoolean(null);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            String[] words = sorted("", "\0", "A", "Aa", "BB", "alpha", "x\0y",
                    "\u00e9", "\u00ff", "\u0100", "\u20ac", "\ud800", "\ud800z",
                    "\ud83d\ude42", "\udc00", "\uffff");
            for (int version : new int[] {1, 2}) {
                Object lexicon = open(image(words, version));
                check(rows(lexicon).length > 0, "ordinary fixture uses primitive slots");
                check(Arrays.stream(rows(lexicon)).allMatch(row -> row >= 0),
                        "full-content direct rows never carry ambiguity markers");
                verify(lexicon, words);
                verify(withoutCoders(lexicon), words);
                invalidImages(words, version);
            }
            Object empty = open(image(new String[0], 2));
            check(rows(empty).length == 0, "empty image uses binary fallback");
            query(empty, new String[0], "");
            query(empty, new String[0], "missing");
            collisionAuthority();
            generationIsolation();
            scaleHeadroom();
            allocationBounds();
            String[] shared = sorted("alpha", "duplicate", "duplicate", "\u00e9", "\ud800");
            Object arena = open(sharedImage(shared));
            verify(arena, shared);
            check(rows(arena).length == 0,
                    "duplicate shared spellings preserve binary compatibility fallback");
            byte[] corruptArena = sharedImage(shared);
            corruptArena[144] ^= 1;
            reject(corruptArena, "shared payload CRC");
            reject(Arrays.copyOf(sharedImage(shared), 90), "shared truncated record");
            System.out.println("M3StringFrozenLexiconIndexTest checks=" + checks
                    + " compact=" + COMPACT);
        } finally {
            // The mappings stay live with their owners. File deletion is supported on the
            // Linux target; on a platform retaining mapped files, jtreg owns scratch cleanup.
            for (Path path : FILES) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException retainedMapping) {
                    path.toFile().deleteOnExit();
                }
            }
        }
    }

    private static void verify(Object lexicon, String[] words) throws Exception {
        byte[] coders = (byte[]) field(LEXICON, "coders").get(lexicon);
        for (int row = 0; row < words.length; row++) {
            String word = words[row];
            query(lexicon, words, word);
            byte expected = (byte) (COMPACT && latin1(word) ? 0 : 1);
            if (coders != null) check(coders[row] == expected, "prepared coder row " + row);
            Object atom = ATOM.invoke(lexicon, row);
            check(ATOM.invoke(lexicon, row) == atom, "reverse owner identity row " + row);
            check(CODER.getByte(atom) == expected, "reverse coder row " + row);
            for (int unit = 0; unit < word.length(); unit++) {
                check((char) ATOM_CHAR.invoke(atom, unit) == word.charAt(unit),
                        "reverse exact UTF-16 row " + row + " unit " + unit);
            }
        }
        for (String missing : new String[] {"missing", "\u00fe", "\ud801", "alpha\0"}) {
            query(lexicon, words, missing);
        }
    }

    private static void query(Object lexicon, String[] words, String word) throws Exception {
        int expected = binaryRow(words, word);
        char[] chars = new char[word.length() + 2];
        Arrays.fill(chars, '!');
        word.getChars(0, word.length(), chars, 1);
        check((int) FIND.invoke(lexicon, chars, 1, word.length()) == expected, "char window");
        byte[] utf16 = (byte[]) UTF16_BYTES.invoke(null, chars, 0, chars.length);
        check((int) FIND_COMPACT.invoke(lexicon, utf16, 1, word.length(), (byte) 1)
                == expected, "UTF-16 compact window");
        if (latin1(word)) {
            byte[] bytes = new byte[word.length() + 2];
            Arrays.fill(bytes, (byte) '!');
            for (int index = 0; index < word.length(); index++) bytes[index + 1] = (byte) word.charAt(index);
            check((int) FIND_COMPACT.invoke(lexicon, bytes, 1, word.length(), (byte) 0)
                    == expected, "Latin-1 compact window");
            check((int) FIND_LATIN1.invoke(lexicon, bytes, 1, word.length()) == expected,
                    "Latin-1 window");
        }
        int[] points = word.codePoints().toArray();
        int[] padded = new int[points.length + 2];
        Arrays.fill(padded, '!');
        System.arraycopy(points, 0, padded, 1, points.length);
        check((int) FIND_POINTS.invoke(lexicon, padded, 1, points.length, word.length())
                == expected, "code-point window");
        if (word.length() == 1) {
            check((int) FIND_UNIT.invoke(lexicon, word.charAt(0)) == expected, "unit lookup");
        }
    }

    private static int binaryRow(String[] words, String word) {
        int low = 0;
        int high = words.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int comparison = word.compareTo(words[middle]);
            if (comparison == 0) return middle;
            if (comparison < 0) high = middle - 1;
            else low = middle + 1;
        }
        return -1;
    }

    private static void collisionAuthority() throws Exception {
        String[] collisions = new String[128];
        for (int value = 0; value < collisions.length; value++) {
            StringBuilder word = new StringBuilder();
            for (int bit = 0; bit < 7; bit++) word.append((value & (1 << bit)) == 0 ? "Aa" : "BB");
            collisions[value] = word.toString();
        }
        Arrays.sort(collisions);
        for (String word : collisions) check(word.hashCode() == collisions[0].hashCode(), "collision fixture");
        Object lexicon = open(image(collisions, 2));
        check(rows(lexicon).length > 0, "hostile Java-hash collision set keeps direct geometry");
        check(Arrays.stream(rows(lexicon)).allMatch(row -> row >= 0),
                "full-content keys remove Java-hash ambiguity markers");
        verify(lexicon, collisions);
    }

    private static void scaleHeadroom() throws Exception {
        for (int size : new int[] {60_000, 600_000}) {
            int[] hashes = new int[size];
            int[] lengths = new int[size];
            for (int index = 0; index < size; index++) {
                hashes[index] =
                        Integer.rotateLeft(index * 0x9e3779b9, 11) ^ 0x85ebca6b;
                lengths[index] = 1 + (index & 31);
            }
            Object direct = DIRECT_BUILD.invoke(null, hashes, lengths);
            int[] directRows = (int[]) DIRECT_ROWS.invoke(direct);
            int[] displacements = (int[]) DIRECT_DISPLACEMENTS.invoke(direct);
            check(directRows.length >= size, "direct row headroom " + size);
            check(directRows.length <= (1 << 22), "direct row cap " + size);
            check(displacements.length > 0, "direct displacement geometry " + size);
            int stride = Math.max(1, size / 4096);
            for (int row = 0; row < size; row += stride) {
                int encoded =
                        (int) DIRECT_CANDIDATE.invoke(
                                direct, hashes[row], lengths[row]);
                check(encoded == row + 1, "single direct candidate " + size + ":" + row);
            }
        }
    }

    private static void generationIsolation() throws Exception {
        // Reuse row zero with distinct spellings and coders. Indexes and reverse owners
        // must remain bound to their admitted immutable image, across later admissions.
        String[] firstWords = sorted("alpha", "common");
        String[] secondWords = sorted("\u0100", "\u20ac");
        Object first = open(image(firstWords, 2));
        Object firstAtom = ATOM.invoke(first, 0);
        Object second = open(image(secondWords, 2));
        Object secondAtom = ATOM.invoke(second, 0);
        check(field(LEXICON, "generation64").getLong(first)
                        != field(LEXICON, "generation64").getLong(second),
                "fixtures have distinct admitted generations");
        check(firstAtom != secondAtom, "row number does not alias reverse owners across images");
        check(field(LEXICON, "exactIndex").get(first)
                        != field(LEXICON, "exactIndex").get(second),
                "each image retains its own prepared exact index");
        verify(second, secondWords);
        verify(first, firstWords);
        for (String word : firstWords) query(second, secondWords, word);
        for (String word : secondWords) query(first, firstWords, word);
        check(ATOM.invoke(first, 0) == firstAtom, "later admission preserves first reverse owner");
        check(ATOM.invoke(second, 0) == secondAtom, "reverse lookup keeps second owner stable");
        // A failed admission must not alter either previously admitted generation.
        byte[] corrupt = image(secondWords, 2);
        corrupt[corrupt.length - 1] ^= 1;
        reject(corrupt, "failed generation admission checksum");
        verify(first, firstWords);
        verify(second, secondWords);
    }

    private static void allocationBounds() throws Exception {
        Method build = method(LEXICON, "buildExactIndex", int[].class, int[].class);
        int tooMany = (1 << 21) + 1;
        int[] hashes = new int[tooMany];
        int[] lengths = new int[tooMany];
        check(((int[]) build.invoke(null, hashes, lengths)).length == 0,
                "optional index cap refuses metadata without allocating slots");
        Method allocate = method(LEXICON, "allocateCoders", int.class);
        check(allocate.invoke(null, Integer.MAX_VALUE) == null,
                "optional coder allocation failure preserves the cold fallback");
    }

    private static Object withoutCoders(Object original) throws Exception {
        Constructor<?> constructor = LEXICON.getDeclaredConstructor(ByteBuffer.class,
                int.class, int[].class, int[].class, int[].class,
                Class.forName("[Ljava.lang.M3StringAtom;"), byte[].class, long.class);
        constructor.setAccessible(true);
        return constructor.newInstance(field(LEXICON, "mapping").get(original),
                field(LEXICON, "payloadOffset").getInt(original),
                field(LEXICON, "offsets").get(original), field(LEXICON, "lengths").get(original),
                field(LEXICON, "javaHashes").get(original),
                java.lang.reflect.Array.newInstance(Class.forName("java.lang.M3StringAtom"),
                        ((int[]) field(LEXICON, "offsets").get(original)).length), null,
                field(LEXICON, "generation64").getLong(original));
    }

    private static void invalidImages(String[] words, int version) throws Exception {
        reject(new byte[63], "truncated header");
        byte[] bytes = image(words, version);
        reject(Arrays.copyOf(bytes, bytes.length - 1), "truncated payload");
        byte[] corrupt = bytes.clone();
        corrupt[corrupt.length - 1] ^= 1;
        reject(corrupt, "checksum mismatch");
        byte[] badRecord = bytes.clone();
        ByteBuffer.wrap(badRecord).putInt(64, Integer.MAX_VALUE);
        checksum(badRecord);
        reject(badRecord, "record bounds after valid checksum");
        if (version == 2) {
            byte[] wrongHash = bytes.clone();
            ByteBuffer.wrap(wrongHash).putInt(72, 1);
            checksum(wrongHash);
            reject(wrongHash, "stored Java hash after valid checksum");
        }
        String[] unsorted = words.clone();
        String swap = unsorted[0];
        unsorted[0] = unsorted[1];
        unsorted[1] = swap;
        reject(image(unsorted, version), "unsorted records after valid checksum");
    }

    private static byte[] image(String[] words, int version) throws Exception {
        int directoryWidth = version == 1 ? 8 : 12;
        int payload = 64 + directoryWidth * words.length;
        int units = Arrays.stream(words).mapToInt(String::length).sum();
        byte[] bytes = new byte[payload + 2 * units];
        ByteBuffer header = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        header.putLong(0, 0x4d334c4558303031L);
        header.putInt(8, version);
        header.putInt(12, words.length);
        header.putLong(16, payload);
        header.putLong(24, units);
        int offset = 0;
        for (int row = 0; row < words.length; row++) {
            String word = words[row];
            int at = 64 + directoryWidth * row;
            header.putInt(at, offset);
            header.putInt(at + 4, word.length());
            if (version == 2) header.putInt(at + 8, word.hashCode());
            for (int index = 0; index < word.length(); index++) {
                char unit = word.charAt(index);
                bytes[payload + 2 * offset] = (byte) unit;
                bytes[payload + 2 * offset + 1] = (byte) (unit >>> 8);
                offset++;
            }
        }
        checksum(bytes);
        return bytes;
    }

    private static void checksum(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(bytes, 0, 32);
        digest.update(bytes, 64, bytes.length - 64);
        System.arraycopy(digest.digest(), 0, bytes, 32, 32);
    }

    private static byte[] sharedImage(String[] words) {
        int size = 64;
        for (String word : words) size += 88 + 2 * word.length();
        byte[] bytes = new byte[size];
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        image.putLong(0, 0x53594e4152523031L);
        image.putInt(8, 1);
        image.putLong(16, 0x1020304050607080L);
        image.putLong(24, 0x1122334455667788L);
        image.putLong(32, size);
        image.putInt(40, words.length);
        CRC32C crc = new CRC32C();
        crc.update(bytes, 0, 56);
        image.putLong(56, crc.getValue());
        int position = 64;
        for (String word : words) {
            int payload = 2 * word.length();
            int total = 88 + payload;
            image.putInt(position, 0x41525231);
            image.putInt(position + 4, total);
            image.putInt(position + 8, 2);
            image.putInt(position + 12, 0);
            image.putInt(position + 16, word.length());
            image.putInt(position + 20, -1);
            image.putInt(position + 24, word.hashCode());
            image.putInt(position + 28, payload);
            for (int index = 0; index < word.length(); index++) {
                image.putChar(position + 80 + 2 * index, word.charAt(index));
            }
            crc.reset();
            crc.update(bytes, position, 64);
            crc.update(bytes, position + 80, payload);
            image.putLong(position + 64, crc.getValue());
            image.putLong(position + total - 8, 0x434f4d4d49543031L ^ position ^ total);
            position += total;
        }
        return bytes;
    }

    private static Object open(byte[] bytes) throws Exception {
        Path path = Files.createTempFile("m3-frozen-index-", ".bin");
        FILES.add(path);
        Files.write(path, bytes);
        return OPEN.invoke(null, path);
    }

    private static void reject(byte[] bytes, String label) throws Exception {
        try {
            open(bytes);
            throw new AssertionError("accepted invalid image: " + label);
        } catch (InvocationTargetException expected) {
            check(expected.getCause() instanceof IOException, label + " reports IOException");
        }
    }

    private static int[] rows(Object lexicon) throws Exception {
        return (int[]) field(LEXICON, "exactRows").get(lexicon);
    }

    private static boolean latin1(String word) {
        for (int index = 0; index < word.length(); index++) if (word.charAt(index) > 255) return false;
        return true;
    }

    private static String[] sorted(String... words) {
        Arrays.sort(words);
        return words;
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters)
            throws ReflectiveOperationException {
        Method method = owner.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
