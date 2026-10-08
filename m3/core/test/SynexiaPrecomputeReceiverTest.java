/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.SharedLexiconCatalog;
import com.m3.text.SharedLexiconImage;
import com.m3.text.SynexiaPrecomputePayload;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

public final class SynexiaPrecomputeReceiverTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String profileHash(String profile, long sourceRecords, long imageRecords) throws Exception {
        byte[] value = (profile + "\t" + sourceRecords + "\t" + imageRecords + "\n")
                .getBytes(StandardCharsets.UTF_8);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }

    private static void writeV2(Path path, String word) throws Exception {
        int payload = 64 + 12;
        ByteBuffer bytes = ByteBuffer.allocate(payload + word.length() * 2).order(ByteOrder.BIG_ENDIAN);
        bytes.putLong(0, 0x4d334c4558303031L).putInt(8, 2).putInt(12, 1)
                .putLong(16, payload).putLong(24, word.length());
        bytes.putInt(64, 0).putInt(68, word.length());
        int hash = 0;
        for (int at = 0; at < word.length(); at++) {
            char unit = word.charAt(at);
            hash = 31 * hash + unit;
            int destination = payload + at * 2;
            bytes.put(destination, (byte) unit).put(destination + 1, (byte) (unit >>> 8));
        }
        bytes.putInt(72, hash);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(bytes.array(), 0, 32);
        digest.update(bytes.array(), 64, bytes.capacity() - 64);
        System.arraycopy(digest.digest(), 0, bytes.array(), 32, 32);
        Files.write(path, bytes.array(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("m3-synexia-payload-");
        try {
            Path image = directory.resolve("one.m3lex");
            writeV2(image, "a");
            Files.writeString(directory.resolve("synexia.shards.tsv"),
                    "shard_id\tfile\tfirst_lexeme\tlast_lexeme\timage_records\tutf16_units\tsha256\n"
                            + "0\tone.m3lex\ta\ta\t1\t1\t" + sha256(image) + "\n");
            Files.writeString(directory.resolve("synexia.records.tsv"),
                    "source_id\tsource_path\tsource_kind\tlanguage_tag\trecord_id\tlexeme\tshard_id\timage_row\t"
                            + "mapping_id\tmapping_name\ttranslation_profile\tprecompute_profile\tprecompute_payload\n"
                            + "numbers\tvalues\tnumber\ten\t7\ta\t0\t0\tn-7\tNUMBER_7\t-\tnumbers\t"
                            + "{\"concept_ids\":[11,0,19],\"flags\":7}\n");
            Files.writeString(directory.resolve("synexia.precompute.tsv"),
                    "shard_id\timage_row\tutf16_units\tjava_hash\tcode_points\tunpaired_surrogates\t"
                            + "non_bmp_code_points\tascii\tlatin1\tcontains_whitespace\tprecompute_profile\n"
                            + "0\t0\t1\t97\t1\t0\t0\tTrue\tTrue\tFalse\tnumbers\n");
            Files.writeString(directory.resolve("synexia.precompute-index.tsv"),
                    "precompute_profile\tsource_records\timage_records\tsha256\n"
                            + "numbers\t1\t1\t" + profileHash("numbers", 1, 1) + "\n");
            SharedLexiconCatalog catalog = SharedLexiconCatalog.openLazy(directory);
            SynexiaPrecomputePayload payload = catalog.findPrecomputePayload("numbers", "7").orElseThrow();
            check(payload.requireInt("flags") == 7);
            check(payload.requireLongArray("concept_ids")[2] == 19L);
            check(catalog.findPrecomputePayload("numbers", "missing").isEmpty());
            check(catalog.findPrecomputePayload("numbers", "7").orElseThrow() == payload);
            catalog.close();
            System.out.println("M3JDK_SYNXIA_RECEIVER_PASS checks=" + checks);
        } finally {
            try (var paths = Files.list(directory)) { for (Path path : paths.toList()) Files.delete(path); }
            Files.delete(directory);
        }
    }
}
