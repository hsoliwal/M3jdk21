import com.m3.text.M3LexiconPrecompute;
import com.m3.text.SharedLexiconCatalog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Executable proof for source-gated SI-unit payload decoding. */
public final class SynexiaSiUnitDecoderTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("m3-synexia-si-");
        try {
            Path image = directory.resolve("one.m3lex");
            writeV2(image, "ampere");
            Files.writeString(directory.resolve("synexia.shards.tsv"),
                    "shard_id\tfile\tfirst_lexeme\tlast_lexeme\timage_records\tutf16_units\tsha256\n"
                            + "0\tone.m3lex\tampere\tampere\t1\t6\t" + sha256(image) + "\n");
            Files.writeString(directory.resolve("synexia.records.tsv"),
                    "source_id\tsource_path\tsource_kind\tlanguage_tag\trecord_id\tlexeme\tshard_id\timage_row\t"
                            + "mapping_id\tmapping_name\ttranslation_profile\tprecompute_profile\tprecompute_payload\n"
                            + "dictlang.si-units\tunits\tsi-unit\tund\tA\tampere\t0\t0\tu-A\tAMPERE\t-\tsi\t"
                            + "{\"si_decimal_exponent\":-6,\"si_dimension_packed\":42,\"si_offset\":0.0,\"si_prefixable\":true}\n"
                            + "dictlang.acronyms\tacronyms\tacronym\tund\tA\tampere\t0\t0\ta-A\tAMPERE\t-\tacronyms\t"
                            + "{\"flags\":1}\n");
            Files.writeString(directory.resolve("synexia.precompute.tsv"),
                    "shard_id\timage_row\tutf16_units\tjava_hash\tcode_points\tunpaired_surrogates\t"
                            + "non_bmp_code_points\tascii\tlatin1\tcontains_whitespace\tprecompute_profile\n"
                            + "0\t0\t6\t2881128724\t6\t0\t0\tTrue\tTrue\tFalse\tsi + acronyms\n");
            Files.writeString(directory.resolve("synexia.precompute-index.tsv"),
                    "precompute_profile\tsource_records\timage_records\tsha256\n"
                            + "acronyms\t1\t1\t" + profileHash("acronyms", 1, 1) + "\n"
                            + "si\t1\t1\t" + profileHash("si", 1, 1) + "\n");
            SharedLexiconCatalog catalog = SharedLexiconCatalog.openLazy(directory);
            M3LexiconPrecompute.SiUnitPrecompute unit =
                    catalog.findSiUnitPrecompute("dictlang.si-units", "A").orElseThrow();
            check(unit.decimalExponent() == -6, "decimal exponent");
            check(unit.dimensionPacked() == 42L, "packed dimension");
            check(unit.offset() == 0.0d && unit.prefixable(), "offset and prefixability");
            check(catalog.findSiUnitPrecompute("dictlang.acronyms", "A").isEmpty(),
                    "source family isolation");
            check(catalog.findSiUnitPrecompute("dictlang.si-units", "missing").isEmpty(),
                    "record identity isolation");
            expectIllegalArgument(() -> new M3LexiconPrecompute.SiUnitPrecompute(101, 0L, 0.0, false),
                    "exponent range");
            expectIllegalArgument(() -> new M3LexiconPrecompute.SiUnitPrecompute(0, 0L, Double.NaN, false),
                    "finite offset");
            catalog.close();
            expectIllegalState(() -> catalog.findSiUnitPrecompute("dictlang.si-units", "A"),
                    "close fences decoder");
            System.out.println("M3JDK_SI_UNIT_PASS checks=" + checks);
        } finally {
            try (var paths = Files.walk(directory)) {
                paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                    try { Files.deleteIfExists(path); }
                    catch (java.io.IOException failure) { throw new RuntimeException(failure); }
                });
            }
        }
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
        Files.write(path, bytes.array());
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }

    private static String profileHash(String profile, long sourceRecords, long imageRecords) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest((profile + "\t" + sourceRecords + "\t" + imageRecords + "\n")
                        .getBytes(StandardCharsets.UTF_8)));
    }

    private static void expectIllegalArgument(ThrowingRunnable action, String message) throws Exception {
        try { action.run(); throw new AssertionError(message); }
        catch (IllegalArgumentException expected) { checks++; }
    }

    private static void expectIllegalState(ThrowingRunnable action, String message) throws Exception {
        try { action.run(); throw new AssertionError(message); }
        catch (IllegalStateException expected) { checks++; }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }
}
