/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3BigString;
import jdk.m3.text.SharedLexiconCatalog;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Streaming contracts of two owners whose loops no longer carry a redundant guard.
 *
 * <p>{@code M3BigString.appendTo} hands an {@link Appendable} only non-empty, in-range chunk
 * windows, for every range of ropes made of whole chunks, boundary slices and slices of slices.
 * Catalog shard admission digests image files read in 8 KiB blocks exactly like a one-shot SHA-256:
 * images ending on a block boundary and inside a block are admitted against the one-shot digest,
 * and a one-byte change is refused.
 */
public final class M3StreamingContractTest {
    private static final int BLOCK = 8192;
    private static long checks;

    private M3StreamingContractTest() {}

    private interface Action {
        void run() throws Exception;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }

    private static void refused(Class<? extends Throwable> type, String reason, Action action) {
        try {
            action.run();
        } catch (Throwable thrown) {
            check(
                    type.isInstance(thrown) && reason.equals(thrown.getMessage()),
                    "wrong refusal " + thrown);
            return;
        }
        throw new AssertionError("accepted: " + reason);
    }

    /** Accepts only the ranged append, and only a non-empty window inside the chunk. */
    private static final class Windows implements Appendable {
        private final StringBuilder text = new StringBuilder();
        private int calls;

        @Override
        public Appendable append(CharSequence chunk, int start, int end) {
            calls++;
            check(0 <= start && start < end && end <= chunk.length(), "non-empty chunk window");
            text.append(chunk, start, end);
            return this;
        }

        @Override
        public Appendable append(CharSequence chunk) {
            throw new AssertionError("unranged append");
        }

        @Override
        public Appendable append(char unit) {
            throw new AssertionError("single-unit append");
        }
    }

    private static void ropeWindows() throws IOException {
        M3BigString base = M3BigString.of("ab", "", "cde", "f", "😀g", "hij");
        String text = base.flatten();
        M3BigString sliced = M3BigString.concat(base.subSequence(1, 6), base.subSequence(7, 10));
        String slicedText = text.substring(1, 6) + text.substring(7, 10);
        M3BigString nested = M3BigString.concat(sliced.subSequence(1, 7), base.subSequence(0, 2));
        String nestedText = slicedText.substring(1, 7) + text.substring(0, 2);
        List<M3BigString> ropes = List.of(base, sliced, nested);
        List<String> oracles = List.of(text, slicedText, nestedText);
        for (int r = 0; r < ropes.size(); r++) {
            M3BigString rope = ropes.get(r);
            String oracle = oracles.get(r);
            check(rope.flatten().equals(oracle), "rope oracle " + r);
            for (int from = 0; from <= oracle.length(); from++) {
                for (int to = from; to <= oracle.length(); to++) {
                    M3BigString range = rope.subSequence(from, to);
                    Windows windows = new Windows();
                    range.appendTo(windows);
                    String expected = oracle.substring(from, to);
                    check(
                            windows.text.toString().equals(expected),
                            "appendTo " + r + ":" + from + "," + to);
                    check(
                            (windows.calls == 0) == expected.isEmpty(),
                            "empty ranges append nothing");
                    check(windows.calls <= expected.length(), "at most one window per unit");
                    StringWriter writer = new StringWriter();
                    range.appendTo(writer);
                    check(writer.toString().equals(expected), "Writer streaming " + r);
                }
            }
        }
    }

    private static byte[] image(List<String> words) throws Exception {
        long units = 0;
        for (String word : words) units += word.length();
        int payload = 64 + 12 * words.size();
        ByteBuffer bytes =
                ByteBuffer.allocate(Math.toIntExact(payload + 2 * units))
                        .order(ByteOrder.BIG_ENDIAN);
        bytes.putLong(0, 0x4d334c4558303031L)
                .putInt(8, 2)
                .putInt(12, words.size())
                .putLong(16, payload)
                .putLong(24, units);
        int offset = 0;
        for (int row = 0; row < words.size(); row++) {
            String word = words.get(row);
            int entry = 64 + 12 * row;
            bytes.putInt(entry, offset)
                    .putInt(entry + 4, word.length())
                    .putInt(entry + 8, word.hashCode());
            for (int at = 0; at < word.length(); at++) {
                int destination = payload + 2 * (offset + at);
                bytes.put(destination, (byte) word.charAt(at))
                        .put(destination + 1, (byte) (word.charAt(at) >>> 8));
            }
            offset += word.length();
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(bytes.array(), 0, 32);
        digest.update(bytes.array(), 64, bytes.capacity() - 64);
        System.arraycopy(digest.digest(), 0, bytes.array(), 32, 32);
        return bytes.array();
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static void write(Path file, String header, List<String> rows) throws IOException {
        StringBuilder text = new StringBuilder(header).append('\n');
        for (String row : rows) text.append(row).append('\n');
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    /** One-shard export: every image row has one mapping, one fact row and profile "p". */
    private static Path export(Path dir, int count) throws Exception {
        List<String> words = new ArrayList<>();
        for (int row = 0; row < count; row++) words.add(String.format("w%05d", row));
        byte[] bytes = image(words);
        Files.write(dir.resolve("part-0.m3lex"), bytes);
        write(
                dir.resolve("m3.shards.tsv"),
                "shard_id\tfile\tfirst_lexeme\tlast_lexeme\timage_records\tutf16_units\tsha256",
                List.of(
                        "0\tpart-0.m3lex\t"
                                + words.get(0)
                                + "\t"
                                + words.get(count - 1)
                                + "\t"
                                + count
                                + "\t"
                                + 6L * count
                                + "\t"
                                + sha256(bytes)));
        List<String> records = new ArrayList<>();
        List<String> facts = new ArrayList<>();
        for (int row = 0; row < count; row++) {
            String word = words.get(row);
            records.add(
                    String.join(
                            "\t",
                            "s" + row,
                            "path/s" + row,
                            "kind",
                            "en",
                            "r" + row,
                            word,
                            "0",
                            Integer.toString(row),
                            "m" + row,
                            "M" + row,
                            "-",
                            "p",
                            "{}"));
            facts.add(
                    "0\t"
                            + row
                            + "\t6\t"
                            + Integer.toUnsignedString(word.hashCode())
                            + "\t6\t0\t0\tTrue\tTrue\tFalse\tp");
        }
        write(
                dir.resolve("m3.records.tsv"),
                "source_id\tsource_path\tsource_kind\tlanguage_tag\t"
                        + "record_id\tlexeme\tshard_id\timage_row\tmapping_id\tmapping_name\t"
                        + "translation_profile\tprecompute_profile\tprecompute_payload",
                records);
        write(
                dir.resolve("m3.precompute.tsv"),
                "shard_id\timage_row\tutf16_units\tjava_hash\t"
                        + "code_points\tunpaired_surrogates\tnon_bmp_code_points\tascii\tlatin1\t"
                        + "contains_whitespace\tprecompute_profile",
                facts);
        String profile = "p\t" + count + "\t" + count + "\n";
        write(
                dir.resolve("m3.precompute-index.tsv"),
                "precompute_profile\tsource_records\timage_records\tsha256",
                List.of(
                        "p\t"
                                + count
                                + "\t"
                                + count
                                + "\t"
                                + sha256(profile.getBytes(StandardCharsets.UTF_8))));
        return dir.resolve("part-0.m3lex");
    }

    private static void shardDigests(Path root) throws Exception {
        // 680 rows end exactly on the second 8 KiB block; 2000 rows end inside the sixth.
        for (int count : new int[] {680, 2000}) {
            Path dir = Files.createDirectories(root.resolve("rows-" + count));
            Path shard = export(dir, count);
            long size = Files.size(shard);
            check(count != 680 || size == 2 * BLOCK, "image ends on a block boundary");
            check(
                    count != 2000 || size % BLOCK != 0 && size > 5 * BLOCK,
                    "image ends inside a block");
            SharedLexiconCatalog catalog = SharedLexiconCatalog.open(dir);
            check(catalog.recordCount() == count, "one-shot digest admits the blockwise read");
            String lastWord = String.format("w%05d", count - 1);
            check(
                    catalog.find(lastWord).orElseThrow().imageRow() == count - 1,
                    "admitted rows resolve");
            catalog.close();
            SharedLexiconCatalog lazy = SharedLexiconCatalog.openLazy(dir);
            check(lazy.find("w00000").orElseThrow().imageRow() == 0, "first-touch digest admits");
            lazy.close();
            // Rewrite one byte in place: a closed catalog's mapping may outlive close() on Windows.
            byte[] last = {(byte) (Files.readAllBytes(shard)[(int) size - 1] ^ 1)};
            try (FileChannel channel = FileChannel.open(shard, StandardOpenOption.WRITE)) {
                channel.write(ByteBuffer.wrap(last), size - 1);
            }
            refused(
                    IOException.class,
                    "shard SHA-256 mismatch",
                    () -> SharedLexiconCatalog.open(dir));
        }
    }

    private static void delete(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var children = Files.list(path)) {
                for (Path child : children.toList()) delete(child);
            }
        }
        for (int attempt = 0; ; attempt++) {
            try {
                Files.deleteIfExists(path);
                return;
            } catch (IOException stillMappedOnWindows) {
                if (attempt == 100) throw stillMappedOnWindows;
                System.gc();
                try {
                    Thread.sleep(20);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw stillMappedOnWindows;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ropeWindows();
        Path root = Files.createTempDirectory("m3-streaming-");
        try {
            shardDigests(root);
        } finally {
            delete(root);
        }
        System.out.println("M3_STREAMING_CONTRACT_PASS checks=" + checks);
    }
}
