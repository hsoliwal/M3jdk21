/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3PrecomputePayload;
import jdk.m3.text.SharedLexiconCatalog;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Probe for the b31bda6823f contract: a warm {@code findPrecomputePayload} must not re-enter
 * {@code ConcurrentHashMap.computeIfAbsent}. This is a qualification probe, not an m3/build.sh
 * test. It records why no m3/core test covers that contract.
 *
 * <p>Default route, through the public API only: it can show that warm lookups return the
 * instance the cold lookup published, and so never re-run the loader. That also holds without
 * the warm get, because computeIfAbsent never re-runs its mapping function for a key that is
 * present. So {@code observable_same=true} prints on master and on the reverted mutant
 * alike.</p>
 *
 * <p>{@code --reflective} route: swaps the private {@code payloads} map for a counting subclass,
 * as the com.synexia crate harness M3PayloadWarmLookupRedTest did. That needs deep reflection
 * into {@code jdk.m3.text}, which the module exports but does not open. On the module path
 * (m3/build.sh), it fails unless the JVM runs with
 * {@code --add-opens jdk.m3.text/jdk.m3.text=ALL-UNNAMED}.</p>
 */
public final class SharedLexiconCatalogWarmLookupProbe {
    private static final int WARM_LOOKUPS = 100;

    /** Counts atomic cache computations; the key and value types are erased in the catalog field. */
    static final class CountingMap extends ConcurrentHashMap<Object, Object> {
        private static final long serialVersionUID = 1L;
        private final transient AtomicInteger computations = new AtomicInteger();

        @Override
        public Object computeIfAbsent(Object key, Function<? super Object, ? extends Object> loader) {
            computations.incrementAndGet();
            return super.computeIfAbsent(key, loader);
        }
    }

    public static void main(String[] args) throws Exception {
        boolean reflective = args.length == 1 && args[0].equals("--reflective");
        Path directory = Files.createTempDirectory("m3-warm-probe-");
        try {
            writeFixture(directory);
            SharedLexiconCatalog catalog = SharedLexiconCatalog.openLazy(directory);
            CountingMap counter = reflective ? install(catalog) : null;
            M3PrecomputePayload cold = catalog.findPrecomputePayload("numbers", "7").orElseThrow();
            boolean same = IntStream.range(0, WARM_LOOKUPS).allMatch(
                    ignored -> catalog.findPrecomputePayload("numbers", "7").orElseThrow() == cold);
            System.out.println("M3JDK_WARM_LOOKUP_PROBE observable_same=" + same + " warm_lookups=" + WARM_LOOKUPS
                    + (counter == null ? "" : " compute_if_absent_calls=" + counter.computations.get()));
            catalog.close();
        } finally {
            try (var paths = Files.list(directory)) { for (Path path : paths.toList()) Files.delete(path); }
            Files.delete(directory);
        }
    }

    private static CountingMap install(SharedLexiconCatalog catalog) throws ReflectiveOperationException {
        Field field = SharedLexiconCatalog.class.getDeclaredField("payloads");
        field.setAccessible(true);
        CountingMap counter = new CountingMap();
        field.set(catalog, counter);
        return counter;
    }

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
