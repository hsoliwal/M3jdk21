/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3PrecomputePayload;
import jdk.m3.text.SharedLexiconCatalog;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/**
 * {@link SharedLexiconCatalog#close()} releases the decoded payload cache (2e5af99316b).
 *
 * <p>Observed through reachability only, without reflection: every cached payload is weakly
 * tracked while the catalog itself stays strongly reachable. While the catalog is open, the
 * cache must keep each payload alive and return the same instance. After {@code close()}, a
 * bounded explicit-GC loop must clear and enqueue every reference. If {@code close()} kept its
 * cache, the payloads would stay strongly reachable from the live catalog and the loop would
 * run out.</p>
 */
public final class SharedLexiconCatalogPayloadReleaseTest {
    private static final String SOURCE_ID = "numbers";
    private static final String[] RECORD_IDS = {"7", "8"};
    private static final int CONTROL_GC_ROUNDS = 3;
    private static final int RELEASE_GC_ROUNDS = 40;
    private static final long RELEASE_GC_WAIT_MILLIS = 50L;
    private static int checks;
    private static int maxReleaseRounds;

    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("m3-payload-release-");
        try {
            writeFixture(directory);
            proveRelease(SharedLexiconCatalog.open(directory));
            proveRelease(SharedLexiconCatalog.openLazy(directory));
            proveEscapedPayloadSurvivesClose(SharedLexiconCatalog.openLazy(directory));
            System.out.println("M3JDK_LEXICON_PAYLOAD_RELEASE_PASS checks=" + checks
                    + " max_release_gc_rounds=" + maxReleaseRounds);
        } finally {
            try (var paths = Files.list(directory)) { for (Path path : paths.toList()) Files.delete(path); }
            Files.delete(directory);
        }
    }

    private static void proveRelease(SharedLexiconCatalog catalog) throws Exception {
        ReferenceQueue<M3PrecomputePayload> queue = new ReferenceQueue<>();
        List<WeakReference<M3PrecomputePayload>> payloads = track(catalog, queue);
        for (int round = 0; round < CONTROL_GC_ROUNDS; round++) System.gc();
        check(payloads.stream().noneMatch(payload -> payload.refersTo(null)));
        check(cachedInstancesReturned(catalog, payloads));
        catalog.close();
        check(released(payloads, queue));
        expectClosed(catalog);
        Reference.reachabilityFence(catalog);
    }

    /** close() drops the catalog's references only; a payload the caller kept stays intact. */
    private static void proveEscapedPayloadSurvivesClose(SharedLexiconCatalog catalog) {
        M3PrecomputePayload escaped = catalog.findPrecomputePayload(SOURCE_ID, RECORD_IDS[0]).orElseThrow();
        String json = escaped.canonicalJson();
        catalog.close();
        check(escaped.requireInt("flags") == Integer.parseInt(RECORD_IDS[0]));
        check(escaped.canonicalJson().equals(json));
        expectClosed(catalog);
    }

    /** Decodes each payload once; only weak references leave this frame. */
    private static List<WeakReference<M3PrecomputePayload>> track(
            SharedLexiconCatalog catalog, ReferenceQueue<M3PrecomputePayload> queue) {
        return Arrays.stream(RECORD_IDS).map(recordId -> {
            M3PrecomputePayload payload = catalog.findPrecomputePayload(SOURCE_ID, recordId).orElseThrow();
            check(payload.requireInt("flags") == Integer.parseInt(recordId));
            return new WeakReference<>(payload, queue);
        }).toList();
    }

    private static boolean cachedInstancesReturned(
            SharedLexiconCatalog catalog, List<WeakReference<M3PrecomputePayload>> payloads) {
        for (int index = 0; index < RECORD_IDS.length; index++) {
            M3PrecomputePayload warm = catalog.findPrecomputePayload(SOURCE_ID, RECORD_IDS[index]).orElseThrow();
            if (!payloads.get(index).refersTo(warm)) return false;
        }
        return true;
    }

    /** Bounded: at most RELEASE_GC_ROUNDS explicit GCs, each followed by a timed queue wait. */
    private static boolean released(List<WeakReference<M3PrecomputePayload>> payloads,
                                    ReferenceQueue<M3PrecomputePayload> queue) throws InterruptedException {
        int enqueued = 0, round = 0;
        while (round < RELEASE_GC_ROUNDS && enqueued < payloads.size()) {
            round++;
            System.gc();
            for (Reference<?> cleared = queue.remove(RELEASE_GC_WAIT_MILLIS); cleared != null; cleared = queue.poll())
                enqueued++;
        }
        maxReleaseRounds = Math.max(maxReleaseRounds, round);
        return enqueued == payloads.size() && payloads.stream().allMatch(payload -> payload.refersTo(null));
    }

    private static void expectClosed(SharedLexiconCatalog catalog) {
        try {
            catalog.findPrecomputePayload(SOURCE_ID, RECORD_IDS[0]);
            throw new AssertionError("closed catalog decoded a payload");
        } catch (IllegalStateException expected) {
            check(true);
        }
    }

    /** One shard holding "a"; two source identities map to it with distinct payloads. */
    private static void writeFixture(Path directory) throws Exception {
        Path image = directory.resolve("one.m3lex");
        writeV2(image, "a");
        Files.writeString(directory.resolve("m3.shards.tsv"),
                "shard_id\tfile\tfirst_lexeme\tlast_lexeme\timage_records\tutf16_units\tsha256\n"
                        + "0\tone.m3lex\ta\ta\t1\t1\t" + sha256(image) + "\n");
        Files.writeString(directory.resolve("m3.records.tsv"),
                "source_id\tsource_path\tsource_kind\tlanguage_tag\trecord_id\tlexeme\tshard_id\timage_row\t"
                        + "mapping_id\tmapping_name\ttranslation_profile\tprecompute_profile\tprecompute_payload\n"
                        + "numbers\tvalues\tnumber\ten\t7\ta\t0\t0\tn-7\tNUMBER_7\t-\tnumbers\t{\"flags\":7}\n"
                        + "numbers\tvalues\tnumber\ten\t8\ta\t0\t0\tn-8\tNUMBER_8\t-\tnumbers\t{\"flags\":8}\n");
        Files.writeString(directory.resolve("m3.precompute.tsv"),
                "shard_id\timage_row\tutf16_units\tjava_hash\tcode_points\tunpaired_surrogates\t"
                        + "non_bmp_code_points\tascii\tlatin1\tcontains_whitespace\tprecompute_profile\n"
                        + "0\t0\t1\t97\t1\t0\t0\tTrue\tTrue\tFalse\tnumbers\n");
        Files.writeString(directory.resolve("m3.precompute-index.tsv"),
                "precompute_profile\tsource_records\timage_records\tsha256\n"
                        + "numbers\t2\t1\t" + profileHash("numbers", 2, 1) + "\n");
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String profileHash(String profile, long sourceRecords, long imageRecords) throws Exception {
        byte[] value = (profile + "\t" + sourceRecords + "\t" + imageRecords + "\n")
                .getBytes(StandardCharsets.UTF_8);
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
