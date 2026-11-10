/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.LocalM3Arena;
import jdk.m3.text.SharedLexiconImage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Black-box admission contract of the versioned M3LEX image: every header, dimension,
 * record-range, Java-hash, ordering and checksum refusal, the writer's snapshot and size
 * refusals, and the refusal to publish or read an image when no SHA-256 provider exists.
 */
public final class SharedLexiconImageContractTest {
    private static final long MAGIC = 0x4d334c4558303031L;
    private static final int HEADER = 64;
    private static int checks;

    private interface Action { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3lex-image-contract-");
        try {
            writerRoundTrip(root);
            writerRefusals(root);
            sizeRefusals(root);
            headerAndDimensionRefusals(root);
            recordRefusals(root);
            versionTwoOrdering(root);
            sha256ProviderAbsent(root);
        } finally {
            delete(root);
        }
        System.out.println("M3_LEXICON_IMAGE_CONTRACT_PASS checks=" + checks);
    }

    private static void writerRoundTrip(Path root) throws Exception {
        Path path = root.resolve("round-trip.m3lex");
        List<String> words = List.of("", "abcd", "abcdefgh", "abce", "\ud800");
        SharedLexiconImage.create(path, words);
        SharedLexiconImage image = SharedLexiconImage.open(path);
        check(image.version() == 1, "writer emits version 1");
        check(image.size() == words.size(), "record count");
        check(image.utf16Units() == 17L, "UTF-16 payload units");
        check(image.mappedBytes() == Files.size(path), "mapping covers the whole file");
        check(image.mappedBytes() == HEADER + 8 * words.size() + 2 * 17, "v1 layout size");
        check(image.imageIdentity().matches("[0-9a-f]{64}"), "identity is a lowercase SHA-256");
        LocalM3Arena arena = new LocalM3Arena();
        for (int i = 0; i < words.size(); i++) {
            check(image.copyRecord(i, arena).flatten().equals(words.get(i)), "copied record " + i);
        }
        expect(NullPointerException.class, () -> image.copyRecord(0, null), "arena required");
        expect(IndexOutOfBoundsException.class, () -> image.copyRecord(words.size(), arena), "row bound");
    }

    private static void writerRefusals(Path root) throws Exception {
        Path target = root.resolve("refused.m3lex");
        int max = SharedLexiconImage.MAX_RECORDS;
        refuseCreate(target, Collections.nCopies(max + 1, "a"), "too many records");
        // A caller list that grows between the size check and the snapshot is refused.
        refuseCreate(target, new GrowingList(max + 1), "too many records");
        refuseCreate(target, List.of("b", "a"), "records must be UTF-16 sorted");
        refuseCreate(target, List.of("a", "a"), "records must be UTF-16 sorted");
        // UTF-16 order, not code-point order: U+FFFF sorts after the surrogate pair U+1F600.
        refuseCreate(target, List.of("￿", "😀"), "records must be UTF-16 sorted");
        int tooManyUnits = (SharedLexiconImage.MAX_IMAGE_BYTES - HEADER - 8) / 2 + 1;
        refuseCreate(target, List.of("a".repeat(tooManyUnits)), "image too large");
        SharedLexiconImage.create(target, List.of("😀", "￿"));
        check(SharedLexiconImage.open(target).size() == 2, "UTF-16 ordered writer accepted");
    }

    private static void refuseCreate(Path target, List<String> words, String message) throws Exception {
        expectMessage(IOException.class, () -> SharedLexiconImage.create(target, words), message);
        check(Files.notExists(target), "refused writer publishes nothing: " + message);
    }

    /** Reports a small size first, then exposes {@code finalSize} elements to the snapshot. */
    private static final class GrowingList extends AbstractList<String> {
        private final int finalSize;
        private boolean reported;
        GrowingList(int finalSize) { this.finalSize = finalSize; }
        @Override public int size() {
            if (!reported) { reported = true; return 1; }
            return finalSize;
        }
        @Override public String get(int index) { return "w"; }
    }

    private static void sizeRefusals(Path root) throws Exception {
        Path tiny = root.resolve("tiny.m3lex");
        Files.write(tiny, new byte[HEADER - 1]);
        expectMessage(IOException.class, () -> SharedLexiconImage.open(tiny), "image size out of bounds");
        Path huge = root.resolve("huge.m3lex");
        try (RandomAccessFile file = new RandomAccessFile(huge.toFile(), "rw")) {
            file.setLength(SharedLexiconImage.MAX_IMAGE_BYTES + 1L);
        }
        try {
            expectMessage(IOException.class, () -> SharedLexiconImage.open(huge), "image size out of bounds");
        } finally {
            Files.delete(huge);
        }
    }

    private static void headerAndDimensionRefusals(Path root) throws Exception {
        List<String> words = List.of("a", "b");
        refuseOpen(root, "zero-magic", new byte[HEADER + 8], "unsupported image header");
        refuseOpen(root, "version-0", mutate(image(1, words), b -> b.putInt(8, 0)), "unsupported image version");
        refuseOpen(root, "version-3", mutate(image(2, words), b -> b.putInt(8, 3)), "unsupported image version");
        String dimensions = "invalid image dimensions";
        refuseOpen(root, "count-negative", mutate(image(1, words), b -> b.putInt(12, -1)), dimensions);
        refuseOpen(root, "count-over-max", mutate(image(1, words),
                b -> b.putInt(12, SharedLexiconImage.MAX_RECORDS + 1)), dimensions);
        refuseOpen(root, "units-negative", mutate(image(1, words), b -> b.putLong(24, -1L)), dimensions);
        refuseOpen(root, "units-over-max", mutate(image(1, words),
                b -> b.putLong(24, SharedLexiconImage.MAX_IMAGE_BYTES / 2 + 1L)), dimensions);
        refuseOpen(root, "payload-offset", mutate(image(1, words), b -> b.putLong(16, HEADER + 12L * 2)), dimensions);
        refuseOpen(root, "payload-units", mutate(image(1, words), b -> b.putLong(24, 3L)), dimensions);
        // Dimensions are self-consistent, but the stored identity does not seal the bytes.
        refuseOpen(root, "checksum", mutate(image(1, words), b -> b.put(b.limit() - 1, (byte) 'c')),
                "image checksum mismatch");
    }

    private static void recordRefusals(Path root) throws Exception {
        List<String> words = List.of("ab", "cd");
        String range = "invalid record range";
        refuseOpen(root, "offset-negative", seal(mutate(image(1, words), b -> b.putInt(HEADER, -1))), range);
        refuseOpen(root, "length-negative", seal(mutate(image(1, words), b -> b.putInt(HEADER + 4, -1))), range);
        refuseOpen(root, "range-overflow", seal(mutate(image(2, words), b -> b.putInt(HEADER + 12 + 4, 3))), range);
        refuseOpen(root, "java-hash", seal(mutate(image(2, words), b -> b.putInt(HEADER + 8, 7))),
                "record Java hash mismatch");
        String order = "records must be UTF-16 sorted";
        refuseOpen(root, "unsorted-lane", image(2, List.of("abcz", "abcd")), order);
        refuseOpen(root, "unsorted-tail", image(2, List.of("ab", "aa")), order);
        refuseOpen(root, "unsorted-length", image(1, List.of("abcde", "abcd")), order);
        refuseOpen(root, "duplicate", image(2, List.of("abcd", "abcd")), order);
    }

    private static void versionTwoOrdering(Path root) throws Exception {
        List<String> words = List.of("abcd", "abcdefgh", "abce", "b", "😀", "￿");
        Path path = root.resolve("ordered-v2.m3lex");
        Files.write(path, image(2, words));
        SharedLexiconImage image = SharedLexiconImage.open(path);
        check(image.version() == 2 && image.size() == words.size(), "v2 dimensions");
        check(image.mappedBytes() == HEADER + 12 * words.size() + 2 * image.utf16Units(), "v2 layout size");
        LocalM3Arena arena = new LocalM3Arena();
        for (int i = 0; i < words.size(); i++) {
            check(image.copyRecord(i, arena).flatten().equals(words.get(i)), "v2 record " + i);
        }
        image.warm();
    }

    private static void sha256ProviderAbsent(Path root) throws Exception {
        Path admitted = root.resolve("admitted.m3lex");
        SharedLexiconImage.create(admitted, List.of("a", "b"));
        SharedLexiconImage image = SharedLexiconImage.open(admitted);
        LocalM3Arena arena = new LocalM3Arena();
        Path target = root.resolve("unsealed.m3lex");
        withoutSha256(() -> {
            expectAssertion(() -> SharedLexiconImage.create(target, List.of("a")), "writer refuses to seal");
            check(Files.notExists(target), "no unsealed image is published");
            expectAssertion(() -> SharedLexiconImage.open(admitted), "open refuses unverifiable image");
            expectAssertion(() -> image.copyRecord(0, arena), "copy refuses unverifiable record");
        });
        check(image.copyRecord(1, arena).flatten().equals("b"), "provider restored");
    }

    static void withoutSha256(Action body) throws Exception {
        Provider[] installed = Security.getProviders();
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < installed.length; i++) {
            if (installed[i].getService("MessageDigest", "SHA-256") != null) positions.add(i);
        }
        for (int position : positions) Security.removeProvider(installed[position].getName());
        try {
            expect(NoSuchAlgorithmException.class, () -> MessageDigest.getInstance("SHA-256"),
                    "SHA-256 provider removed");
            body.run();
        } finally {
            for (int position : positions) Security.insertProviderAt(installed[position], position + 1);
        }
        check(MessageDigest.getInstance("SHA-256").getAlgorithm().equals("SHA-256"), "SHA-256 restored");
    }

    private static void refuseOpen(Path root, String name, byte[] bytes, String message) throws Exception {
        Path path = root.resolve(name + ".m3lex");
        Files.write(path, bytes);
        expectMessage(IOException.class, () -> SharedLexiconImage.open(path), message);
    }

    private interface Mutation { void apply(ByteBuffer bytes); }

    private static byte[] mutate(byte[] bytes, Mutation mutation) {
        mutation.apply(ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN));
        return bytes;
    }

    /** Builds an image of either version without the writer's ordering check. */
    static byte[] image(int version, List<String> words) throws Exception {
        int directory = version == 1 ? 8 : 12;
        long units = 0;
        for (String word : words) units += word.length();
        int payload = HEADER + directory * words.size();
        ByteBuffer bytes = ByteBuffer.allocate(Math.toIntExact(payload + 2 * units)).order(ByteOrder.BIG_ENDIAN);
        bytes.putLong(0, MAGIC).putInt(8, version).putInt(12, words.size()).putLong(16, payload).putLong(24, units);
        int offset = 0;
        for (int row = 0; row < words.size(); row++) {
            String word = words.get(row);
            int entry = HEADER + directory * row;
            bytes.putInt(entry, offset).putInt(entry + 4, word.length());
            if (version == 2) bytes.putInt(entry + 8, word.hashCode());
            for (int at = 0; at < word.length(); at++) {
                char unit = word.charAt(at);
                int destination = payload + 2 * (offset + at);
                bytes.put(destination, (byte) unit).put(destination + 1, (byte) (unit >>> 8));
            }
            offset += word.length();
        }
        return seal(bytes.array());
    }

    /** Recomputes the image identity over the first 32 header bytes and everything after the header. */
    static byte[] seal(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(bytes, 0, 32);
        digest.update(bytes, HEADER, bytes.length - HEADER);
        System.arraycopy(digest.digest(), 0, bytes, 32, 32);
        return bytes;
    }

    private static void delete(Path root) throws IOException {
        // Mapped buffers may pin files on some platforms until they are collected.
        for (int attempt = 0; ; attempt++) {
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                return;
            } catch (IOException failure) {
                if (attempt == 5) throw failure;
                System.gc();
            }
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Action action, String message) {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError(message + ": unexpected " + thrown, thrown);
        }
        throw new AssertionError(message + ": no " + type.getSimpleName());
    }

    private static void expectMessage(Class<? extends Throwable> type, Action action, String fragment) {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown) && String.valueOf(thrown.getMessage()).contains(fragment)) return;
            throw new AssertionError(fragment + ": unexpected " + thrown, thrown);
        }
        throw new AssertionError(fragment + ": no " + type.getSimpleName());
    }

    private static void expectAssertion(Action action, String message) {
        checks++;
        try {
            action.run();
        } catch (AssertionError expected) {
            if (expected.getCause() instanceof NoSuchAlgorithmException) return;
            throw new AssertionError(message + ": wrong cause", expected);
        } catch (Throwable thrown) {
            throw new AssertionError(message + ": unexpected " + thrown, thrown);
        }
        throw new AssertionError(message + ": no AssertionError");
    }
}
