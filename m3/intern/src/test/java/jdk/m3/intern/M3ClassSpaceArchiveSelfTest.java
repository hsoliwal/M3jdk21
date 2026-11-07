// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** No external dependencies: bounded correctness, corruption, mmap and checked-JNI oracle. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-classspace-immutable-archive-20261030/src/test/java/com/synexia/m3/classspace/M3ClassSpaceArchiveSelfTest.java blob e8a8f2c771af; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
public final class M3ClassSpaceArchiveSelfTest {
    private static long assertions;
    private static final LongAdder threadAssertions = new LongAdder();

    private M3ClassSpaceArchiveSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        emptyAndBasic();
        immutableSnapshotAndRestore();
        deterministicRandomReplay();
        invalidEnvelopes();
        canonicalDuplicateRejection();
        realClassFileAndMappedGeometry();
        concurrentReadOnlyViews();
        strictCanonicalOpen();
        nativeOracle();
        System.out.println("M3_CLASSSPACE_ARCHIVE_PASS assertions=" + (assertions + threadAssertions.sum()));
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void invalid(Class<? extends Throwable> exception, Runnable action) {
        assertions++;
        try {
            action.run();
        } catch (Throwable ex) {
            if (exception.isInstance(ex)) return;
            throw new AssertionError("Unexpected exception: " + ex, ex);
        }
        throw new AssertionError("Expected " + exception.getSimpleName());
    }

    private static void emptyAndBasic() {
        M3ClassSpace owner = new M3ClassSpace(16);
        byte[] empty = M3ClassSpaceArchive.freeze(owner);
        check(empty.length == 56, "empty envelope size");
        M3ClassSpaceArchive.View v = M3ClassSpaceArchive.openCopy(empty);
        check(v.count() == 0 && v.payloadBytes() == 0, "empty values");
        check(v.restore().imageCount() == 0, "empty restoration");
        invalid(IndexOutOfBoundsException.class, () -> v.viewAt(0));
        int emptyId = owner.intern(new byte[0]);
        check(emptyId == 0, "zero-length atom ID");
        int oneId = owner.intern(new byte[] {0, (byte)255, 1});
        check(oneId == 1, "second atom ID");
        byte[] archive = M3ClassSpaceArchive.freeze(owner);
        M3ClassSpaceArchive.View restored = M3ClassSpaceArchive.openCopy(archive);
        check(restored.viewAt(0).remaining() == 0, "zero length view");
        check(Arrays.equals(restored.copyAt(1), new byte[] {0, (byte)255, 1}), "exact unsigned bytes");
        invalid(IndexOutOfBoundsException.class, () -> restored.copyAt(-1));
        invalid(IndexOutOfBoundsException.class, () -> restored.copyAt(2));
    }

    private static void immutableSnapshotAndRestore() {
        M3ClassSpace owner = new M3ClassSpace(4096);
        byte[] source = new byte[] {1, 2, 3};
        int a = owner.intern(source);
        source[0] = 42;
        int b = owner.intern(new byte[] {1, 2, 3});
        check(a == b, "caller alias cannot mutate interner");
        owner.intern(new byte[] {(byte) 0xff, (byte) 0x80, 0, 8});
        byte[] saved = M3ClassSpaceArchive.freeze(owner);
        byte[] second = M3ClassSpaceArchive.freeze(owner);
        check(Arrays.equals(saved, second), "deterministic image");
        M3ClassSpaceArchive.View view = M3ClassSpaceArchive.openCopy(saved);
        saved[0] = 0;
        saved[saved.length-1] = 0;
        check(Arrays.equals(view.copyAt(0), new byte[] {1, 2, 3}), "defensive image intake");
        byte[] copy = view.copyAt(0);
        copy[0] = 10;
        check(view.copyAt(0)[0] == 1, "copy mutation isolation");
        ByteBuffer buf = view.viewAt(0);
        check(buf.isReadOnly(), "read-only immutable value");
        invalid(java.nio.ReadOnlyBufferException.class, () -> buf.put((byte) 9));
        M3ClassSpace roundtrip = view.restore();
        check(roundtrip.imageCount() == owner.imageCount(), "restored count");
        check(roundtrip.retainedByteCount() == owner.retainedByteCount(), "restored bytes");
        check(Arrays.equals(second, M3ClassSpaceArchive.freeze(roundtrip)), "byte-identical round trip");
    }

    private static void deterministicRandomReplay() {
        M3ClassSpace owner = new M3ClassSpace(400_000);
        Random random = new Random(0x3ca5510L);
        byte[][] previously = new byte[257][];
        for (int i = 0; i < 1500; i++) {
            byte[] bytes;
            if (i % 5 == 0 && previously[i % previously.length] != null) {
                bytes = previously[i % previously.length].clone();
            } else {
                bytes = new byte[random.nextInt(129)];
                random.nextBytes(bytes);
            }
            previously[i % previously.length] = bytes.clone();
            int id = owner.intern(bytes);
            check(id >= 0 && id < owner.imageCount(), "stable ordinal");
        }
        byte[] saved = M3ClassSpaceArchive.freeze(owner);
        M3ClassSpaceArchive.View v = M3ClassSpaceArchive.openCopy(saved);
        check(v.count() == owner.imageCount(), "random counts");
        for (int i = 0; i < v.count(); i++) {
            check(Arrays.equals(v.copyAt(i), owner.copy(i)), "random ordinal image");
        }
        M3ClassSpace reconstructed = v.restore();
        check(reconstructed.imageCount() == owner.imageCount(), "random restored count");
        check(Arrays.equals(saved, M3ClassSpaceArchive.freeze(reconstructed)), "random idempotent archive");
    }

    private static void invalidEnvelopes() {
        M3ClassSpace owner = new M3ClassSpace(256);
        owner.intern(new byte[] {1, 2, 3, 4});
        owner.intern(new byte[] {5, 6});
        byte[] original = M3ClassSpaceArchive.freeze(owner);
        invalid(NullPointerException.class, () -> M3ClassSpaceArchive.openCopy(null));
        invalid(IllegalArgumentException.class, () -> M3ClassSpaceArchive.openCopy(new byte[0]));
        for (int n = 0; n < original.length; n++) {
            final int length = n;
            invalid(IllegalArgumentException.class,
                    () -> M3ClassSpaceArchive.openCopy(Arrays.copyOf(original, length)));
        }
        // Every byte belongs to a hash-protected field or to a structurally checked span.
        for (int i = 0; i < original.length; i++) {
            byte[] corrupted = original.clone();
            corrupted[i] ^= (byte) 0x80;
            invalid(IllegalArgumentException.class, () -> M3ClassSpaceArchive.openCopy(corrupted));
        }
        byte[] outOfBounds = original.clone();
        ByteBuffer.wrap(outOfBounds).order(ByteOrder.BIG_ENDIAN).putInt(24, Integer.MAX_VALUE);
        byte[] badFirst = outOfBounds;
        invalid(IllegalArgumentException.class,
                () -> M3ClassSpaceArchive.openCopy(badFirst));
        outOfBounds = original.clone();
        ByteBuffer.wrap(outOfBounds).order(ByteOrder.BIG_ENDIAN).putInt(16, -1);
        byte[] rejected = outOfBounds;
        invalid(IllegalArgumentException.class, () -> M3ClassSpaceArchive.openCopy(rejected));
        invalid(IllegalArgumentException.class,
                () -> M3ClassSpaceArchive.openTrustedMapped(ByteBuffer.wrap(original).asReadOnlyBuffer()));
        invalid(IllegalArgumentException.class,
                () -> M3ClassSpaceArchive.openTrustedMapped(ByteBuffer.allocateDirect(original.length)));
    }

    private static void canonicalDuplicateRejection() {
        M3ClassSpace owner = new M3ClassSpace(256);
        owner.intern(new byte[] {11, 12});
        owner.intern(new byte[] {23, 24});
        byte[] invalid = M3ClassSpaceArchive.freeze(owner);
        // Forge an archive with two equal entries and valid SHA roots; reading can succeed,
        // but rebuilding canonical owner-local ordinal IDs MUST reject it.
        int payloadBase = 24 + 2 * 40;
        invalid[payloadBase + 2] = 11;
        invalid[payloadBase + 3] = 12;
        byte[] digest = sha256(new byte[] {11, 12});
        System.arraycopy(digest, 0, invalid, 24 + 40 + 8, digest.length);
        byte[] root = sha256(Arrays.copyOf(invalid, invalid.length - 32));
        System.arraycopy(root, 0, invalid, invalid.length - 32, 32);
        M3ClassSpaceArchive.View v = M3ClassSpaceArchive.openCopy(invalid);
        check(v.count() == 2, "structurally legitimate duplicate envelope");
        invalid(IllegalArgumentException.class, v::restore);
    }

    private static void realClassFileAndMappedGeometry() throws IOException {
        byte[] classBytes;
        try (InputStream input = M3ClassSpaceArchiveSelfTest.class.getResourceAsStream(
                "/jdk/m3/intern/M3ClassSpaceArchiveSelfTest.class")) {
            if (input == null) throw new IOException("Missing compiled real classfile");
            classBytes = input.readAllBytes();
        }
        M3ClassSpace owner = new M3ClassSpace(classBytes.length * 2);
        owner.intern(classBytes);
        owner.intern(new byte[] {0, 1, 2, (byte)0xff});
        byte[] encoded = M3ClassSpaceArchive.freeze(owner);
        Path temp = Files.createTempFile("m3classspace-archive-", ".img");
        try {
            Files.write(temp, encoded);
            try (FileChannel ch = FileChannel.open(temp, StandardOpenOption.READ)) {
                ByteBuffer mapped = ch.map(FileChannel.MapMode.READ_ONLY, 0, encoded.length);
                check(mapped.isDirect() && mapped.isReadOnly(), "real OS mapped read-only view");
                M3ClassSpaceArchive.View view = M3ClassSpaceArchive.openTrustedMapped(mapped);
                check(view.count() == 2, "mapped counts");
                check(Arrays.equals(view.copyAt(0), classBytes), "exact real classfile bytes");
                check(Arrays.equals(view.copyAt(1), new byte[] {0, 1, 2, (byte)0xff}),
                        "mapped second atom");
                check(view.readOnlyImage().isDirect(), "no mapped-image heap copy");
                check(Arrays.equals(M3ClassSpaceArchive.freeze(view.restore()), encoded),
                        "mapped image canonical replay");
                Class<?> one = new ClassByteLoader().define(classBytes);
                Class<?> two = new ClassByteLoader().define(classBytes);
                check(one != two && one.getClassLoader() != two.getClassLoader(),
                        "Equal classfile bytes retain loader-specific Class identities");
                check(one.getName().equals(two.getName()), "Symbolic name equality is not type identity");
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static final class ClassByteLoader extends ClassLoader {
        private ClassByteLoader() { super(null); }
        private Class<?> define(byte[] image) {
            return defineClass(null, image, 0, image.length);
        }
    }

    private static void concurrentReadOnlyViews() throws InterruptedException {
        M3ClassSpace owner = new M3ClassSpace(1024);
        for (int i = 0; i < 100; i++) owner.intern(new byte[] {(byte)i, 0, (byte)0xff});
        M3ClassSpaceArchive.View view = M3ClassSpaceArchive.openCopy(M3ClassSpaceArchive.freeze(owner));
        int workers = 6;
        Thread[] threads = new Thread[workers];
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failed = new AtomicReference<>();
        for (int t = 0; t < workers; t++) {
            threads[t] = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 500; i++) {
                        int id = i % 100;
                        byte[] image = view.copyAt(id);
                        if (image.length != 3 || image[0] != (byte)id || image[2] != (byte)0xff) {
                            throw new AssertionError("Concurrent view lost bytes");
                        }
                        threadAssertions.increment();
                    }
                } catch (Throwable ex) {
                    failed.compareAndSet(null, ex);
                }
            }, "m3-image-read-" + t);
            threads[t].start();
        }
        start.countDown();
        for (Thread t : threads) t.join();
        if (failed.get() != null) throw new AssertionError("Parallel view corrupt", failed.get());
        check(view.count() == 100, "concurrent read-only view count");
    }

    /** Verify optional strict intake without changing existing structural intake. */
    private static void strictCanonicalOpen() {
        M3ClassSpace owner = new M3ClassSpace(100_000);
        byte[] empty = M3ClassSpaceArchive.freeze(owner);
        check(M3ClassSpaceArchive.openCanonicalCopy(empty).count() == 0, "empty strict");
        for (int i = 0; i < 2048; i++) {
            byte[] bytes = new byte[] {(byte) i, (byte) (i >>> 8),
                    (byte) (i * 31), (byte) (i >>> 4)};
            check(owner.intern(bytes) == i, "distinct archive content ID");
        }
        byte[] valid = M3ClassSpaceArchive.freeze(owner);
        M3ClassSpaceArchive.View strict = M3ClassSpaceArchive.openCanonicalCopy(valid);
        check(strict.count() == 2048, "canonical intake preserves order");
        check(Arrays.equals(valid, M3ClassSpaceArchive.freeze(strict.restore())),
                "strict independent owner byte replay");
        strict.verifyCanonicalIds();
        for (int i = 0; i < strict.count(); i++) {
            check(Arrays.equals(strict.copyAt(i), owner.copy(i)), "original ID payload");
        }
        ByteBuffer direct = ByteBuffer.allocateDirect(valid.length + 7);
        direct.position(3);
        direct.put(valid);
        direct.flip();
        direct.position(3);
        M3ClassSpaceArchive.View mapped =
                M3ClassSpaceArchive.openCanonicalTrustedMapped(direct.asReadOnlyBuffer());
        check(mapped.count() == 2048 && mapped.readOnlyImage().isDirect(),
                "strict direct-buffer view with nonzero position");

        M3ClassSpace twins = new M3ClassSpace(100);
        twins.intern(new byte[] {11, 22, 33});
        twins.intern(new byte[] {44, 55, 66});
        byte[] forged = M3ClassSpaceArchive.freeze(twins);
        int payloadBase = 24 + 2 * 40;
        System.arraycopy(forged, payloadBase, forged, payloadBase + 3, 3);
        byte[] digest = sha256(new byte[] {11, 22, 33});
        System.arraycopy(digest, 0, forged, 24 + 40 + 8, digest.length);
        byte[] root = sha256(Arrays.copyOf(forged, forged.length - 32));
        System.arraycopy(root, 0, forged, forged.length - 32, root.length);
        check(M3ClassSpaceArchive.openCopy(forged).count() == 2,
                "legacy structural intake remains permissive");
        invalid(IllegalArgumentException.class,
                () -> M3ClassSpaceArchive.openCanonicalCopy(forged));
        ByteBuffer forgedMapped = ByteBuffer.allocateDirect(forged.length);
        forgedMapped.put(forged).flip();
        check(M3ClassSpaceArchive.openTrustedMapped(
                forgedMapped.asReadOnlyBuffer()).count() == 2, "legacy mapped contract");
        invalid(IllegalArgumentException.class,
                () -> M3ClassSpaceArchive.openCanonicalTrustedMapped(
                        forgedMapped.asReadOnlyBuffer()));
    }

    private static void nativeOracle() {
        String library = System.getProperty("m3.archive.native.library");
        if (library == null || library.isBlank()) return;
        System.load(library);
        M3ClassSpace owner = new M3ClassSpace(4096);
        for (int i = 0; i < 64; i++) owner.intern(new byte[] {(byte)i, (byte)-i, 0});
        byte[] bytes = M3ClassSpaceArchive.freeze(owner);
        ByteBuffer direct = ByteBuffer.allocateDirect(bytes.length + 8);
        direct.position(4);
        direct.put(bytes);
        direct.position(4);
        direct.limit(bytes.length + 4);
        M3ClassSpaceArchive.View view = M3ClassSpaceArchive.openTrustedMapped(direct.slice().asReadOnlyBuffer());
        ByteBuffer nativeBytes = view.readOnlyImage();
        for (int start = 0; start < bytes.length; start += 31) {
            int len = Math.min(63, bytes.length - start);
            check(M3ClassSpaceArchiveNative.fnv64Region(nativeBytes, start, len)
                    == javaFnv(bytes, start, len), "JNI direct-buffer parity");
        }
        check(M3ClassSpaceArchiveNative.fnv64Region(nativeBytes, 0, 0)
                == javaFnv(bytes, 0, 0), "JNI empty region");
        invalid(IllegalArgumentException.class,
                () -> M3ClassSpaceArchiveNative.fnv64Region(ByteBuffer.wrap(bytes), 0, 10));
        invalid(IllegalArgumentException.class,
                () -> M3ClassSpaceArchiveNative.fnv64Region(nativeBytes, -1, 1));
        invalid(IllegalArgumentException.class,
                () -> M3ClassSpaceArchiveNative.fnv64Region(nativeBytes, 0, bytes.length + 1));
        invalid(IllegalArgumentException.class,
                () -> M3ClassSpaceArchiveNative.fnv64Region(null, 0, 0));
    }

    private static long javaFnv(byte[] bytes, int offset, int length) {
        long value = 0xcbf29ce484222325L;
        for (int i = 0; i < length; i++) {
            value ^= (bytes[offset + i] & 0xffL);
            value *= 0x100000001b3L;
        }
        return value;
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(ex);
        }
    }
}
