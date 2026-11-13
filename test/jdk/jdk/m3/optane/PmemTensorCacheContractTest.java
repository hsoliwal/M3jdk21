// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test id=file
 * @summary Typed tensor geometry, domain separation, recovery and malformed-frame refusal
 * @modules jdk.m3.io/jdk.m3.optane
 * @run main/othervm -Xmx96m test.PmemTensorCacheContractTest
 */
/*
 * @test id=posix
 * @summary Typed tensor cache contracts against the real POSIX JNI owner
 * @requires (os.family == "linux")
 * @modules jdk.m3.io/jdk.m3.optane
 * @run main/othervm -Xcheck:jni -Xmx96m test.PmemTensorCacheContractTest --require-posix
 */
package test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import jdk.m3.optane.PmemAccess;
import jdk.m3.optane.PmemBackend;
import jdk.m3.optane.PmemBlockStore;
import jdk.m3.optane.PmemPrefixCache;
import jdk.m3.optane.PmemPrefixKey;
import jdk.m3.optane.PmemRegion;
import jdk.m3.optane.PmemSdk;
import jdk.m3.optane.PmemTensorSpec;

/** Semantic round trips plus independent malformed version-one wire fixtures. */
public final class PmemTensorCacheContractTest {
    private static final int CAPACITY = 200;
    private static final int MAX_PAYLOAD = 512;
    private static final String SCHEMA = "a".repeat(64);
    private static final long[][] SHAPES = { {}, { 0 }, { 6 }, { 2, 3 } };
    private static final ByteOrder[] ORDERS = { ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN };
    private static int assertions;

    private PmemTensorCacheContractTest() { }

    public static void main(String[] args) throws Exception {
        if (args.length > 1 || args.length == 1 && !"--require-posix".equals(args[0])) {
            throw new IllegalArgumentException("unknown option");
        }
        geometry();
        roundTrip(PmemBackend.FILE);
        if (args.length != 0) {
            check(PmemSdk.capabilities().nativeAvailable(), "real native library required");
            roundTrip(PmemBackend.POSIX);
        }
        System.out.println("PMEM_TENSOR_CACHE_PASS assertions=" + assertions
                + " posix=" + (args.length != 0));
    }

    private static void geometry() throws Exception {
        long[] source = { 2, 3 };
        PmemTensorSpec spec = new PmemTensorSpec(PmemTensorSpec.Scalar.FLOAT32,
                ByteOrder.LITTLE_ENDIAN, source);
        source[0] = 9;
        long[] result = spec.shape();
        result[1] = 9;
        check(Arrays.equals(spec.shape(), new long[] { 2, 3 }), "shape is defensively owned");
        check(spec.elements() == 6 && spec.byteCount() == 24 && spec.rank() == 2,
                "matrix geometry");
        PmemTensorSpec same = new PmemTensorSpec(PmemTensorSpec.Scalar.FLOAT32,
                ByteOrder.LITTLE_ENDIAN, 2, 3);
        check(spec.equals(same) && same.hashCode() == spec.hashCode(), "exact descriptor equality");
        check(spec.sha256().equals(same.sha256()), "stable descriptor identity");
        check(new PmemTensorSpec(PmemTensorSpec.Scalar.FLOAT64,
                ByteOrder.BIG_ENDIAN).byteCount() == 8,
                "rank-zero scalar");
        check(new PmemTensorSpec(PmemTensorSpec.Scalar.FLOAT64, ByteOrder.BIG_ENDIAN,
                Long.MAX_VALUE, 0, Long.MAX_VALUE).byteCount() == 0,
                "zero axis prevents irrelevant overflow");
        check(new PmemTensorSpec(PmemTensorSpec.Scalar.UINT8, ByteOrder.LITTLE_ENDIAN,
                1L << 32).byteCount() == 1L << 32, "long tensor coordinates");
        expect(IllegalArgumentException.class, () -> new PmemTensorSpec(PmemTensorSpec.Scalar.UINT8,
                ByteOrder.LITTLE_ENDIAN, Long.MAX_VALUE, 2));
        expect(IllegalArgumentException.class,
                () -> new PmemTensorSpec(PmemTensorSpec.Scalar.FLOAT64,
                ByteOrder.LITTLE_ENDIAN, Long.MAX_VALUE));
        expect(IllegalArgumentException.class, () -> new PmemTensorSpec(PmemTensorSpec.Scalar.UINT8,
                ByteOrder.LITTLE_ENDIAN, -1));
        expect(IllegalArgumentException.class, () -> new PmemTensorSpec(PmemTensorSpec.Scalar.UINT8,
                ByteOrder.LITTLE_ENDIAN, new long[17]));
        expect(NullPointerException.class,
                () -> new PmemTensorSpec(null, ByteOrder.LITTLE_ENDIAN, 1));
        expect(NullPointerException.class,
                () -> new PmemTensorSpec(PmemTensorSpec.Scalar.UINT8, null, 1));
        expect(NullPointerException.class, () -> new PmemTensorSpec(PmemTensorSpec.Scalar.UINT8,
                ByteOrder.LITTLE_ENDIAN, (long[]) null));
    }

    private static void roundTrip(PmemBackend backend) throws Exception {
        long bytes = PmemBlockStore.requiredBytes(CAPACITY, MAX_PAYLOAD);
        Path file = Files.createTempFile("m3-tensor-cache-", ".bin");
        try {
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
                channel.position(bytes - 1);
                channel.write(ByteBuffer.wrap(new byte[] { 0 }));
            }
            PmemPrefixKey key = prefix(7);
            try (PmemRegion region = PmemSdk.open(file, 0, bytes, PmemAccess.READ_WRITE, backend)) {
                check(region.backend() == backend, "backend selection");
                PmemBlockStore store = PmemBlockStore.create(
                        region, 0, CAPACITY, MAX_PAYLOAD, SCHEMA);
                PmemPrefixCache cache = new PmemPrefixCache(store, 1);
                cache.put(key, new byte[] { 99, 88 }, 0, 2);
                for (PmemTensorSpec.Scalar scalar : PmemTensorSpec.Scalar.values()) {
                    for (ByteOrder order : ORDERS) {
                        for (long[] shape : SHAPES) {
                            PmemTensorSpec spec = new PmemTensorSpec(scalar, order, shape);
                            byte[] payload = payload(spec);
                            cache.put(key, spec, payload, 0, payload.length);
                            verify(cache, key, spec);
                        }
                    }
                }
                check(store.committedCount() == 73, "typed descriptors do not alias");
                byte[] raw = new byte[4];
                check(cache.get(key, raw, 1) == 2 && raw[1] == 99 && raw[2] == 88,
                        "legacy raw cache coexists with typed domain");
                PmemTensorSpec small = new PmemTensorSpec(PmemTensorSpec.Scalar.UINT8,
                        ByteOrder.LITTLE_ENDIAN, 2);
                long before = store.committedCount();
                expect(IllegalArgumentException.class,
                        () -> cache.put(key, small, new byte[1], 0, 1));
                check(store.committedCount() == before, "invalid geometry has no storage effect");
                byte[] untouched = new byte[8];
                Arrays.fill(untouched, (byte) 61);
                check(cache.get(prefix(8), small, untouched, 2) == -1, "unknown prefix misses");
                check(allEqual(untouched, (byte) 61), "miss preserves destination");
                expect(IndexOutOfBoundsException.class,
                        () -> cache.get(key, small, new byte[1], 0));
                PmemTensorSpec huge = new PmemTensorSpec(PmemTensorSpec.Scalar.UINT8,
                        ByteOrder.LITTLE_ENDIAN, 1L << 32);
                expect(IllegalArgumentException.class, () -> cache.get(key, huge, new byte[0], 0));
                cache.releaseIndex();
                check(cache.indexBytes() == 0, "index release frees optional primitive lanes");
                verify(cache, key, new PmemTensorSpec(PmemTensorSpec.Scalar.BFLOAT16,
                        ByteOrder.LITTLE_ENDIAN, 2, 3));
                malformedFrames(cache, store, key, small);
            }
            try (PmemRegion region = PmemSdk.open(file, 0, bytes, PmemAccess.READ_WRITE, backend)) {
                PmemBlockStore store = PmemBlockStore.open(
                        region, 0, CAPACITY, MAX_PAYLOAD, SCHEMA);
                PmemPrefixCache cache = new PmemPrefixCache(store, 0);
                for (PmemTensorSpec.Scalar scalar : PmemTensorSpec.Scalar.values()) {
                    for (ByteOrder order : ORDERS) {
                        for (long[] shape : SHAPES) {
                            verify(cache, key, new PmemTensorSpec(scalar, order, shape));
                        }
                    }
                }
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void malformedFrames(PmemPrefixCache cache, PmemBlockStore store,
                                        PmemPrefixKey key, PmemTensorSpec spec) throws Exception {
        // Independent v1 wire fixture: magic, version, UINT8, little-endian, rank one, extent two.
        byte[] descriptor = ByteBuffer.allocate(32).putLong(0x4d3354454e535031L)
                .putInt(1).putInt(1).putInt(1).putInt(1).putLong(2).array();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update("M3PMEM_TYPED_PREFIX_V1".getBytes(StandardCharsets.US_ASCII));
        digest.update(HexFormat.of().parseHex(key.sha256()));
        digest.update(ByteBuffer.allocate(4).putInt(descriptor.length).array());
        digest.update(descriptor);
        String typedKey = HexFormat.of().formatHex(digest.digest());
        byte[] valid = ByteBuffer.allocate(82).putLong(0x4d3354454e534f31L).putInt(32).putInt(2)
                .put(HexFormat.of().parseHex(key.sha256())).put(descriptor)
                .put(new byte[] { 42, 43 }).array();
        for (int at : new int[] { 0, 8, 12, 16, 48, 56, 60, 64, 68, 72 }) {
            byte[] bad = valid.clone();
            bad[at] ^= 1;
            store.append(typedKey, bad, 0, bad.length);
            byte[] untouched = { 17, 17, 17, 17 };
            expect(IOException.class, () -> cache.get(key, spec, untouched, 1));
            check(allEqual(untouched, (byte) 17), "malformed typed envelope preserves destination");
        }
        store.append(typedKey, new byte[1], 0, 1);
        byte[] untouched = { 19, 19, 19 };
        expect(IOException.class, () -> cache.get(key, spec, untouched, 0));
        check(allEqual(untouched, (byte) 19), "truncated typed envelope preserves destination");
        cache.put(key, spec, new byte[] { 42, 43 }, 0, 2);
        byte[] restored = new byte[2];
        check(cache.get(key, spec, restored, 0) == 2 && restored[0] == 42 && restored[1] == 43,
                "repair is a fresh append; prior invalid frame is not selected");
    }

    private static void verify(PmemPrefixCache cache, PmemPrefixKey key, PmemTensorSpec spec)
            throws Exception {
        byte[] expected = payload(spec);
        byte[] target = new byte[expected.length + 4];
        Arrays.fill(target, (byte) 93);
        check(cache.get(key, spec, target, 2) == expected.length, "typed length");
        check(Arrays.equals(expected, Arrays.copyOfRange(target, 2, 2 + expected.length)),
                "exact tensor bytes");
        check(target[0] == 93 && target[1] == 93 && target[target.length - 1] == 93
                && target[target.length - 2] == 93, "destination slice boundaries");
    }

    private static byte[] payload(PmemTensorSpec spec) {
        byte[] bytes = new byte[(int) spec.byteCount()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i * 17 + spec.scalar().ordinal() * 7
                    + (spec.byteOrder() == ByteOrder.LITTLE_ENDIAN ? 1 : 3) + spec.rank());
        }
        return bytes;
    }

    private static PmemPrefixKey prefix(int token) {
        PmemPrefixKey.Context context = new PmemPrefixKey.Context("tensor-test", "b".repeat(64),
                "c".repeat(64), "unquantized", "engine/attention-v1", "d".repeat(64));
        return PmemPrefixKey.contiguous(context, new int[] { 1, token }, 0, 2, 0);
    }

    private static boolean allEqual(byte[] bytes, byte value) {
        for (byte b : bytes) {
            if (b != value) {
                return false;
            }
        }
        return true;
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expect(Class<? extends Throwable> type, Throwing action) throws Exception {
        assertions++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) {
                return;
            }
            throw new AssertionError("expected " + type.getName(), failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    @FunctionalInterface
    private interface Throwing {
        void run() throws Exception;
    }
}
