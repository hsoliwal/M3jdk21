/*
 * @test
 * @summary M3LEX001 v2 mapped VM reader refuses rechecksummed unused UTF-16 payload; v1 remains compatible
 * @modules java.base/java.lang
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED M3LexiconV2MappedReaderTest
 */
// SPDX-License-Identifier: Apache-2.0

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Exercises the real java.lang.M3StringPool$Lexicon.open(Path), not a duplicate
 * wire reader. --fixture-only verifies source-generated image construction
 * on a stock Java 21 JDK without claiming to execute M3JDK's VM reader.
 */
public final class M3LexiconV2MappedReaderTest {
    private static final long MAGIC = 0x4d334c4558303031L;
    private static final int HEADER = 64;
    private static int checks;

    private M3LexiconV2MappedReaderTest() { }

    public static void main(String[] args) throws Exception {
        List<String> words = new ArrayList<>(List.of(
                "", "a", "Aa", "BB", "e\u0301", "\u00e9",
                "\u0928\u092e\u0938\u094d\u0924\u0947", "\u732b", "\ud83d\ude00"));
        words.add("\ud800");
        words.add("\udc00");
        words.sort(Comparator.naturalOrder()); // Java's UTF-16 code-unit order.

        byte[] valid = encodeV2(words);
        byte[] empty = encodeV2(List.of());
        byte[] prefix = addUnusedUnit(valid, 0);
        byte[] middle = addUnusedUnit(valid, 2); // strictly between two nonempty words
        byte[] trailing = addUnusedUnit(valid, words.size());
        byte[] emptyTrailing = addUnusedUnit(empty, 0);
        byte[] damagedHash = valid.clone();
        damagedHash[damagedHash.length - 1] ^= 1;
        byte[] legacyValid = asVersion1(valid);
        byte[] legacySparse = asVersion1(prefix);

        // Verify the generator itself without requiring M3JDK21.
        check(words.size() == 11, "fixture word count");
        check(!words.get(1).isEmpty() && !words.get(2).isEmpty(),
                "middle gap separates two nonempty records");
        check("Aa".hashCode() == "BB".hashCode(), "test genuine Java hash collision");
        check(header(valid, 8) == 2 && header(legacyValid, 8) == 1, "version headers");
        check(ByteBuffer.wrap(valid).order(ByteOrder.BIG_ENDIAN).getInt(12) == words.size(),
                "record count");
        check(Arrays.equals(digest(valid), Arrays.copyOfRange(valid, 32, 64)),
                "valid image checksum");
        for (byte[] image : List.of(prefix, middle, trailing, emptyTrailing, legacySparse)) {
            check(Arrays.equals(digest(image), Arrays.copyOfRange(image, 32, 64)),
                    "rechecksummed gap must remain SHA-valid");
        }
        check(!Arrays.equals(digest(damagedHash), Arrays.copyOfRange(damagedHash, 32, 64)),
                "tampered image must have bad checksum");

        if (args.length == 1 && args[0].equals("--fixture-only")) {
            System.out.println("M3LEX_V2_VM_FIXTURE_PASS assertions=" + checks
                    + " (stock JDK, VM reader NOT executed)");
            return;
        }
        if (args.length != 0) {
            throw new IllegalArgumentException("usage: [--fixture-only]");
        }

        Class<?> type = Class.forName("java.lang.M3StringPool$Lexicon");
        Method open = type.getDeclaredMethod("open", Path.class);
        open.setAccessible(true);
        Method available = type.getDeclaredMethod("available");
        available.setAccessible(true);

        try (var unused = new Scratch()) {
            admit(open, available, unused, "valid-v2", valid);
            admit(open, available, unused, "empty-v2", empty);
            admit(open, available, unused, "legacy-v1", legacyValid);
            admit(open, available, unused, "legacy-v1-sparse", legacySparse);
            refuse(open, unused, "v2-prefix-gap", prefix, "noncontiguous");
            refuse(open, unused, "v2-middle-gap", middle, "noncontiguous");
            refuse(open, unused, "v2-trailing-gap", trailing, "unreferenced");
            refuse(open, unused, "v2-empty-trailing-gap", emptyTrailing, "unreferenced");
            refuse(open, unused, "checksum-corruption", damagedHash, "checksum");
        }
        System.out.println("M3LEX_V2_REAL_VM_MAPPED_READER_PASS assertions=" + checks);
    }

    private static int header(byte[] image, int at) {
        return ByteBuffer.wrap(image).order(ByteOrder.BIG_ENDIAN).getInt(at);
    }

    private static byte[] encodeV2(List<String> words) throws Exception {
        int units = words.stream().mapToInt(String::length).sum();
        ByteBuffer image = ByteBuffer.allocate(HEADER + 12 * words.size() + 2 * units)
                .order(ByteOrder.BIG_ENDIAN);
        int payload = HEADER + 12 * words.size();
        image.putLong(0, MAGIC);
        image.putInt(8, 2);
        image.putInt(12, words.size());
        image.putLong(16, payload);
        image.putLong(24, units);
        int cursor = 0;
        for (int row = 0; row < words.size(); row++) {
            String word = words.get(row);
            if (row > 0 && words.get(row - 1).compareTo(word) >= 0) {
                throw new AssertionError("not strictly UTF-16-sorted");
            }
            image.putInt(HEADER + 12 * row, cursor);
            image.putInt(HEADER + 12 * row + 4, word.length());
            image.putInt(HEADER + 12 * row + 8, word.hashCode());
            for (int i = 0; i < word.length(); i++) {
                char ch = word.charAt(i);
                int pos = payload + 2 * (cursor++);
                image.put(pos, (byte) ch);
                image.put(pos + 1, (byte) (ch >>> 8));
            }
        }
        check(cursor == units, "encoded all declared UTF-16 units");
        return seal(image.array());
    }

    private static byte[] addUnusedUnit(byte[] source, int beforeRow) throws Exception {
        ByteBuffer input = ByteBuffer.wrap(source).order(ByteOrder.BIG_ENDIAN);
        int count = input.getInt(12);
        if (beforeRow < 0 || beforeRow > count) throw new IllegalArgumentException("beforeRow");
        int payload = Math.toIntExact(input.getLong(16));
        int units = Math.toIntExact(input.getLong(24));
        int insertion = beforeRow == count ? units : input.getInt(HEADER + 12 * beforeRow);
        int offset = payload + 2 * insertion;
        byte[] data = new byte[source.length + 2];
        System.arraycopy(source, 0, data, 0, offset);
        data[offset] = '#';
        data[offset + 1] = 0;
        System.arraycopy(source, offset, data, offset + 2, source.length - offset);
        ByteBuffer out = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        out.putLong(24, units + 1L);
        for (int row = beforeRow; row < count; row++) {
            int entry = HEADER + 12 * row;
            out.putInt(entry, out.getInt(entry) + 1);
        }
        return seal(data);
    }

    private static byte[] asVersion1(byte[] version2) throws Exception {
        ByteBuffer src = ByteBuffer.wrap(version2).order(ByteOrder.BIG_ENDIAN);
        int count = src.getInt(12);
        int payload = Math.toIntExact(src.getLong(16));
        int oldPayload = HEADER + 8 * count;
        byte[] data = new byte[oldPayload + version2.length - payload];
        System.arraycopy(version2, 0, data, 0, HEADER);
        ByteBuffer out = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        out.putInt(8, 1);
        out.putLong(16, oldPayload);
        for (int row = 0; row < count; row++) {
            out.putInt(HEADER + 8 * row, src.getInt(HEADER + 12 * row));
            out.putInt(HEADER + 8 * row + 4, src.getInt(HEADER + 12 * row + 4));
        }
        System.arraycopy(version2, payload, data, oldPayload, version2.length - payload);
        return seal(data);
    }

    private static byte[] digest(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(data, 0, 32);
        digest.update(data, 64, data.length - 64);
        return digest.digest();
    }

    private static byte[] seal(byte[] data) throws Exception {
        System.arraycopy(digest(data), 0, data, 32, 32);
        return data;
    }

    private static void admit(Method open, Method available, Scratch scratch,
            String label, byte[] image) throws Exception {
        Path path = scratch.write(label, image);
        Object mapping;
        try {
            mapping = open.invoke(null, path);
        } catch (InvocationTargetException failed) {
            throw new AssertionError("VM refused valid image " + label, failed.getCause());
        }
        check(mapping != null && Boolean.TRUE.equals(available.invoke(mapping)),
                "mapped VM admitted valid image " + label);
    }

    private static void refuse(Method open, Scratch scratch,
            String label, byte[] image, String expected) throws Exception {
        Path path = scratch.write(label, image);
        try {
            open.invoke(null, path);
            throw new AssertionError("VM accepted malformed image: " + label);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            check(cause instanceof IOException && cause.getMessage().contains(expected),
                    "VM refused " + label + " for expected reason " + expected + ": " + cause);
        }
    }

    private static void check(boolean valid, String caseName) {
        if (!valid) throw new AssertionError(caseName);
        checks++;
    }

    private static final class Scratch implements AutoCloseable {
        private final Path directory;
        private final List<Path> paths = new ArrayList<>();

        Scratch() throws IOException {
            directory = Files.createTempDirectory("m3lex-v2-mapped-vm-");
            directory.toFile().deleteOnExit();
        }

        Path write(String label, byte[] bytes) throws IOException {
            Path file = directory.resolve(label + ".m3lex");
            Files.write(file, bytes);
            // Windows cannot always delete a still-mapped file immediately.
            file.toFile().deleteOnExit();
            paths.add(file);
            return file;
        }

        @Override public void close() throws IOException {
            for (Path path : paths) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException mappingStillAlive) {
                    // A mapped file may stay locked on Windows (AccessDenied or
                    // FileSystemException). It is registered for delete-on-exit.
                }
            }
            try {
                Files.deleteIfExists(directory);
            } catch (IOException mappingStillAlive) {
                // Deferred mapped-file cleanup, not a parser test failure.
            }
        }
    }
}
