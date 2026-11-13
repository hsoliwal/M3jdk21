/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3PrecomputePayload;
import jdk.m3.text.SharedLexiconCatalog;
import jdk.m3.text.SharedLexiconCatalog.SourceIdentity;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * A warm {@link SharedLexiconCatalog#findPrecomputePayload} must not re-enter
 * {@code computeIfAbsent} (b31bda6823f).
 *
 * <p>No public observable separates this contract: {@code computeIfAbsent} never re-runs its
 * mapping function for a key that is present, so the returned instance is the same either way.
 * So the test replaces the catalog's private {@code payloads} map with a counting one by
 * reflection, as the com.synexia crate harness M3PayloadWarmLookupRedTest did. That needs
 * {@code --add-opens jdk.m3.text/jdk.m3.text=ALL-UNNAMED} on the module path. make/m3/build.sh
 * passes it on this test's run line only. One cold lookup and {@value #WARM_LOOKUPS} warm
 * lookups must enter {@code computeIfAbsent} exactly once, and run the loader exactly once.</p>
 */
public final class SharedLexiconCatalogWarmLookupTest {
    private static final String SOURCE_ID = "numbers";
    private static final String RECORD_ID = "7";
    private static final int WARM_LOOKUPS = 100;
    private static int checks;

    private static void check(boolean value, String detail) {
        checks++;
        if (!value) throw new AssertionError("check " + checks + ": " + detail);
    }

    /** Counts entries into computeIfAbsent and runs of the catalog's loader function. */
    static final class CountingPayloads extends ConcurrentHashMap<SourceIdentity, M3PrecomputePayload> {
        private static final long serialVersionUID = 1L;
        private final transient AtomicInteger computeCalls = new AtomicInteger();
        private final transient AtomicInteger loaderRuns = new AtomicInteger();

        @Override
        public M3PrecomputePayload computeIfAbsent(SourceIdentity key,
                Function<? super SourceIdentity, ? extends M3PrecomputePayload> loader) {
            computeCalls.incrementAndGet();
            return super.computeIfAbsent(key, identity -> {
                loaderRuns.incrementAndGet();
                return loader.apply(identity);
            });
        }
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("m3-warm-lookup-");
        try {
            writeFixture(directory);
            proveWarmLookups(SharedLexiconCatalog.open(directory));
            proveWarmLookups(SharedLexiconCatalog.openLazy(directory));
            System.out.println("M3JDK_LEXICON_WARM_LOOKUP_PASS checks=" + checks + " compute_if_absent_calls=1"
                    + " loader_runs=1 warm_lookups=" + WARM_LOOKUPS);
        } finally {
            try (var paths = Files.list(directory)) { for (Path path : paths.toList()) Files.delete(path); }
            Files.delete(directory);
        }
    }

    private static void proveWarmLookups(SharedLexiconCatalog catalog) throws ReflectiveOperationException {
        try {
            CountingPayloads payloads = install(catalog);
            M3PrecomputePayload cold = catalog.findPrecomputePayload(SOURCE_ID, RECORD_ID).orElseThrow();
            check(payloads.size() == 1 && payloads.containsValue(cold), "catalog caches through the swapped map");
            check(cold.requireInt("flags") == 7, "cold payload decoded");
            boolean same = IntStream.range(0, WARM_LOOKUPS).allMatch(
                    ignored -> catalog.findPrecomputePayload(SOURCE_ID, RECORD_ID).orElseThrow() == cold);
            check(same, "warm lookups return the cached instance");
            check(payloads.computeCalls.get() == 1, "compute_if_absent_calls=" + payloads.computeCalls.get());
            check(payloads.loaderRuns.get() == 1, "loader_runs=" + payloads.loaderRuns.get());
        } finally {
            catalog.close();
        }
    }

    /** Replaces the still-empty private cache before the first lookup. */
    private static CountingPayloads install(SharedLexiconCatalog catalog) throws ReflectiveOperationException {
        Field field = SharedLexiconCatalog.class.getDeclaredField("payloads");
        check(field.getType() == Map.class, "payloads field type " + field.getType());
        field.setAccessible(true);
        check(((Map<?, ?>) field.get(catalog)).isEmpty(), "fresh catalog has no cached payload");
        CountingPayloads payloads = new CountingPayloads();
        field.set(catalog, payloads);
        check(field.get(catalog) == payloads, "counting map installed");
        return payloads;
    }

    /** One shard holding "a", mapped by one source identity with one payload. */
    private static void writeFixture(Path directory) throws Exception {
        Path image = directory.resolve("one.m3lex");
        writeV2(image, "a");
        Files.writeString(directory.resolve("m3.shards.tsv"),
                "shard_id\tfile\tfirst_lexeme\tlast_lexeme\timage_records\tutf16_units\tsha256\n"
                        + "0\tone.m3lex\ta\ta\t1\t1\t" + sha256(Files.readAllBytes(image)) + "\n");
        Files.writeString(directory.resolve("m3.records.tsv"),
                "source_id\tsource_path\tsource_kind\tlanguage_tag\trecord_id\tlexeme\tshard_id\timage_row\t"
                        + "mapping_id\tmapping_name\ttranslation_profile\tprecompute_profile\tprecompute_payload\n"
                        + "numbers\tvalues\tnumber\ten\t7\ta\t0\t0\tn-7\tNUMBER_7\t-\tnumbers\t{\"flags\":7}\n");
        Files.writeString(directory.resolve("m3.precompute.tsv"),
                "shard_id\timage_row\tutf16_units\tjava_hash\tcode_points\tunpaired_surrogates\t"
                        + "non_bmp_code_points\tascii\tlatin1\tcontains_whitespace\tprecompute_profile\n"
                        + "0\t0\t1\t97\t1\t0\t0\tTrue\tTrue\tFalse\tnumbers\n");
        Files.writeString(directory.resolve("m3.precompute-index.tsv"),
                "precompute_profile\tsource_records\timage_records\tsha256\n"
                        + "numbers\t1\t1\t" + sha256("numbers\t1\t1\n".getBytes(StandardCharsets.UTF_8)) + "\n");
    }

    private static String sha256(byte[] value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }

    /** Writes a one-record M3LEX v2 image, as M3PrecomputeReceiverTest does. */
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
}
