// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

/**
 * Current-develop byte-for-byte wire oracle plus bounded reusable scratch proof.
 * No Maven/third-party/native runtime is needed for the Java contract checks.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/test/java/com/synexia/algorithms/compression/M3CompressionWorkspaceSelfTest.java blob 6b4588a82879; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class CompressionWorkspaceSelfTest {
    private static final String SNAPPY_SHA256 =
            "c2740cd29dbe4413dda1d309baaf1b85a71a6ffb3d51ba23c444b05d38e51140";
    private static final String LZ4_SHA256 =
            "96998fbf2be7572738ad84ea615e968716aa4afdcd0b90cecc86a70740ee73fd";
    private static final int[] SIZES = {0, 1, 2, 3, 4, 5, 11, 12, 15, 16, 31, 59,
            60, 61, 127, 128, 255, 256, 257, 1024, 4096, 65535, 65536, 70000};
    private static final int SAMPLES = 1600;
    private static int checks;

    private CompressionWorkspaceSelfTest() {}

    public static void main(String[] args) throws IOException {
        if (args.length > 1) throw new IllegalArgumentException("optional native corpus path only");
        run(args.length == 1 ? Path.of(args[0]) : null);
    }

    public static void runAll() throws IOException {
        run(null);
    }

    private static void run(Path corpusPath) throws IOException {
        checks = 0;
        MessageDigest snappyHash = sha();
        MessageDigest lz4Hash = sha();
        SnappyRawEncoder snappy = new SnappyRawEncoder(12);
        Lz4RawBlockEncoder lz4 = new Lz4RawBlockEncoder(12);
        Random random = new Random(0x4d33444f4f503232L);
        try (DataOutputStream corpus = corpusPath == null ? null
                : new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(corpusPath)))) {
            if (corpus != null) {
                corpus.writeInt(0x4d335752);
                corpus.writeInt(160 * 2);
            }
            for (int sample = 0; sample < SAMPLES; sample++) {
                int size = sample < SIZES.length * 4
                        ? SIZES[sample % SIZES.length] : random.nextInt(32768);
                byte[] data = new byte[size];
                random.nextBytes(data);
                switch (sample % 6) {
                    case 0 -> Arrays.fill(data, (byte) 0x5a);
                    case 1 -> {
                        for (int i = 0; i < data.length; i++) data[i] = (byte) i;
                    }
                    case 2 -> {
                        for (int i = 0; i < data.length; i++) data[i] = (byte) (i % 7);
                    }
                    case 3 -> {
                        int half = data.length / 2;
                        System.arraycopy(data, 0, data, data.length - half, half);
                    }
                    default -> { }
                }
                byte[] before = data.clone();
                int cap = Math.max(64, size * 2 + 128);
                byte[] packedSnappy = snappy.encode(data, cap);
                byte[] packedLz4 = lz4.encode(data, cap);
                check(Arrays.equals(before, data), "input immutability");
                update(snappyHash, packedSnappy);
                update(lz4Hash, packedLz4);
                check(Arrays.equals(packedSnappy, snappy.encode(data, packedSnappy.length)),
                        "Snappy exact cap and repeated call");
                check(Arrays.equals(packedLz4, lz4.encode(data, packedLz4.length)),
                        "LZ4 exact cap and repeated call");
                if (sample % 9 == 0) {
                    reject(() -> snappy.encode(data, packedSnappy.length - 1));
                    reject(() -> lz4.encode(data, packedLz4.length - 1));
                }
                if (corpus != null && sample < 160) {
                    record(corpus, 0, data, packedSnappy);
                    record(corpus, 1, data, packedLz4);
                }
            }
            byte[] large = new byte[250000];
            Arrays.fill(large, (byte) 5);
            update(snappyHash, snappy.encode(large, large.length * 2));
            update(lz4Hash, lz4.encode(large, large.length * 2));
            check(SNAPPY_SHA256.equals(HexFormat.of().formatHex(snappyHash.digest())),
                    "Snappy exact baseline wire SHA-256");
            check(LZ4_SHA256.equals(HexFormat.of().formatHex(lz4Hash.digest())),
                    "LZ4 exact baseline wire SHA-256");
        }
        workspaceChecks();
        System.out.println("M3 compression reusable-workspace oracle checks=" + checks);
    }

    private static void workspaceChecks() {
        CompressionByteSink sink = new CompressionByteSink(100, 4);
        sink.append(42);
        byte[] previous = sink.finish();
        sink.reset(100, 4);
        sink.append(13);
        check(Arrays.equals(previous, new byte[] {42}), "caller output never aliases scratch");
        check(Arrays.equals(sink.finish(), new byte[] {13}), "reset logical length");
        sink.reset(250000, 250000);
        byte[] large = new byte[200000];
        Arrays.fill(large, (byte) 0x5a);
        sink.append(large, 0, large.length);
        byte[] externallyOwned = sink.finish();
        check(sink.retainedCapacity() > 64 * 1024, "large grow before trim");
        sink.reset(100, 5);
        check(sink.retainedCapacity() <= 100, "memory-pressure excess scratch released");
        sink.append(99);
        check(externallyOwned.length == 200000 && externallyOwned[199999] == 0x5a,
                "shrink cannot mutate previously returned output");
        check(Arrays.equals(sink.finish(), new byte[] {99}), "reuse after shrink");
        reject(() -> sink.reset(-1, 2));
        reject(() -> sink.reset(2, -1));
        check(Arrays.equals(sink.finish(), new byte[] {99}), "invalid reset is atomic");
        sink.reset(1, 1);
        sink.append(7);
        reject(() -> sink.append(8));
        check(Arrays.equals(sink.finish(), new byte[] {7}), "cap failure cannot escape bytes");
        sink.reset(0, 0);
        check(sink.retainedCapacity() == 0, "zero-cap reset releases buffer");
    }

    private static void record(DataOutputStream out, int codec, byte[] raw, byte[] packed)
            throws IOException {
        out.writeByte(codec);
        out.writeInt(raw.length);
        out.writeInt(packed.length);
        out.write(raw);
        out.write(packed);
    }

    private static void update(MessageDigest digest, byte[] wire) {
        int length = wire.length;
        digest.update((byte) (length >>> 24));
        digest.update((byte) (length >>> 16));
        digest.update((byte) (length >>> 8));
        digest.update((byte) length);
        digest.update(wire);
    }

    private static MessageDigest sha() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void reject(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException accepted) {
            checks++;
            return;
        }
        throw new AssertionError("invalid compression request accepted");
    }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message + " #" + checks);
    }
}
