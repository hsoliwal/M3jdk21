/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3Lexicons;
import com.m3.text.SharedLexiconCatalog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;

/** Contract proof for the target-side M3Lexicons adapter. */
public final class M3LexiconsFacadeTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3lexicons-facade-");
        try {
            writeCatalog(root);
            try (M3Lexicons lexicons = M3Lexicons.open(root)) {
                check(lexicons.catalog() == lexicons.catalog(), "catalog identity is stable");
                check(lexicons.shardCount() == 1, "one shard");
                check(lexicons.recordCount() == 1, "one record");
                SharedLexiconCatalog.Coordinate coordinate =
                        lexicons.find("alpha").orElseThrow();
                check(coordinate.equals(new SharedLexiconCatalog.Coordinate(0, 0)),
                        "coordinate preserved");
                check(lexicons.textAt(coordinate).equals("alpha"), "text boundary");
                check(lexicons.findMappings("alpha").size() == 1, "mapping lookup");
                check(lexicons.findMapping("source-a", "record-a").isPresent(),
                        "source identity lookup");
                check(lexicons.prefix("al", 4).equals(List.of(coordinate)),
                        "prefix lookup");
                check(lexicons.precomputeAt(coordinate).javaHash() == javaHash("alpha"),
                        "precompute facts");
                check(lexicons.findPrecomputePayload("source-a", "record-a").isPresent(),
                        "opaque payload lookup");
                check(lexicons.precomputeProfiles().size() == 1, "profile lookup");
                lexicons.warm();
            }
            M3Lexicons lazy = M3Lexicons.openLazy(root);
            check(lazy.find("alpha").isPresent(), "lazy facade lookup");
            lazy.close();
            expectIllegalState(() -> lazy.find("alpha"), "close fences facade");
            System.out.println("M3JDK_M3LEXICONS_FACADE_PASS checks=" + checks);
        } finally {
            deleteTree(root);
        }
    }

    private static void writeCatalog(Path root) throws Exception {
        Path image = root.resolve("synexia.m3lex");
        Files.writeString(root.resolve("words.txt"), "alpha\n", StandardCharsets.UTF_8);
        writeImageV2(image);
        String digest = sha256(Files.readAllBytes(image));
        write(root, "synexia.shards.tsv",
                "shard_id\tfile\tfirst_lexeme\tlast_lexeme\timage_records\tutf16_units\tsha256\n"
                        + "0\tsynexia.m3lex\talpha\talpha\t1\t5\t" + digest + "\n");
        write(root, "synexia.records.tsv",
                "source_id\tsource_path\tsource_kind\tlanguage_tag\trecord_id\tlexeme\tshard_id\timage_row\tmapping_id\tmapping_name\ttranslation_profile\tprecompute_profile\tprecompute_payload\n"
                        + "source-a\tpath\tdictionary\ten\trecord-a\talpha\t0\t0\t-\t-\t-\tfixture\t{}\n");
        write(root, "synexia.precompute.tsv",
                "shard_id\timage_row\tutf16_units\tjava_hash\tcode_points\tunpaired_surrogates\tnon_bmp_code_points\tascii\tlatin1\tcontains_whitespace\tprecompute_profile\n"
                        + "0\t0\t5\t" + javaHash("alpha")
                        + "\t5\t0\t0\tTrue\tTrue\tFalse\tfixture\n");
        write(root, "synexia.precompute-index.tsv",
                "precompute_profile\tsource_records\timage_records\tsha256\n"
                        + "fixture\t1\t1\t" + sha256("fixture\t1\t1\n") + "\n");
    }

    private static void writeImageV2(Path target) throws Exception {
        long payload = 64L + 12L;
        ByteBuffer bytes = ByteBuffer.allocate(Math.toIntExact(payload + 10L))
                .order(ByteOrder.BIG_ENDIAN);
        bytes.putLong(0x4d334c4558303031L).putInt(2).putInt(1)
                .putLong(payload).putLong(5L);
        bytes.putInt(64, 0).putInt(68, 5).putInt(72, javaHash("alpha"));
        String value = "alpha";
        for (int at = 0; at < value.length(); at++) {
            int position = Math.toIntExact(payload) + at * 2;
            char unit = value.charAt(at);
            bytes.put(position, (byte) unit).put(position + 1, (byte) (unit >>> 8));
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(bytes.array(), 0, 32);
        digest.update(bytes.array(), 64, bytes.limit() - 64);
        bytes.position(32);
        bytes.put(digest.digest());
        Files.write(target, bytes.array());
    }

    private static int javaHash(String value) {
        int hash = 0;
        for (int at = 0; at < value.length(); at++) hash = 31 * hash + value.charAt(at);
        return hash;
    }

    private static String sha256(String value) throws Exception {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] value) throws Exception {
        return java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value));
    }

    private static void write(Path root, String name, String content) throws Exception {
        Files.writeString(root.resolve(name), content, StandardCharsets.UTF_8);
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static void expectIllegalState(Throwing action, String message) throws Exception {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalStateException expected) {
            checks++;
        }
    }

    private static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                try { Files.deleteIfExists(path); }
                catch (java.io.IOException failure) { throw new RuntimeException(failure); }
            });
        }
    }

    @FunctionalInterface
    private interface Throwing {
        void run() throws Exception;
    }
}
