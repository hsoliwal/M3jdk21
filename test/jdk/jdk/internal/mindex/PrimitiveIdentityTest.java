/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 * Target-owned M3JDK implementation. See the primitive identity receiver receipt.
 */
/*
 * @test
 * @summary Primitive typed identity and immutable mapped catalogue contracts
 * @modules java.base/jdk.internal.mindex
 * @run main PrimitiveIdentityTest
 */
import jdk.internal.mindex.M3PrimitiveIdentity;
import jdk.internal.mindex.M3PrimitiveImage;
import static jdk.internal.mindex.M3PrimitiveIdentity.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PrimitiveIdentityTest {
    private static int assertions;
    private static native long readKey(long address);
    private static void check(boolean condition) {
        assertions++; if (!condition) throw new AssertionError("assertion " + assertions);
    }
    interface Action { void run() throws Exception; }
    private static void fails(Class<? extends Throwable> expected, Action action) throws Exception {
        try { action.run(); } catch (Throwable caught) {
            if (expected.isInstance(caught)) { assertions++; return; }
            throw new AssertionError("wrong exception", caught);
        }
        throw new AssertionError("expected " + expected.getName());
    }
    private static void redigest(byte[] bytes) throws Exception {
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        hash.update(bytes, 0, 64); hash.update(bytes, 96, bytes.length - 96);
        System.arraycopy(hash.digest(), 0, bytes, 64, 32);
    }
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("child")) {
            try (var image = M3PrimitiveImage.open(Path.of(args[1]))) {
                var handle = new M3PrimitiveImage.Handle(UUID.fromString(args[2]),
                        Long.parseLong(args[3]), Integer.parseInt(args[4]), Long.parseLong(args[5]));
                int slot = image.resolve(handle);
                check(slot >= 0 && image.handle(slot).equals(handle));
                image.withAddress(slot, address -> System.out.println("childAddress=" + address));
                System.out.println("childIdentity=PASS");
            }
            return;
        }
        boolean nativeTest = args.length > 0;
        if (nativeTest) System.load(Path.of(args[0]).toAbsolutePath().toString());
        check(ofBoolean(false) == 0 && ofBoolean(true) == 1);
        for (int i = -128; i <= 127; i++) check(ofByte((byte)i) == i);
        for (int i = 0; i <= 65535; i++) {
            check(ofChar((char)i) == i); check(ofShort((short)i) == (short)i);
        }
        long[] edges = {0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE};
        for (long a : edges) {
            check(ofLong(a) == a); check(ofInt((int)a) == (int)a);
            for (long b : edges) check(numericEquals(LONG, a, LONG, b) == (a == b));
        }
        check(!rawEquals(BYTE, 1, CHAR, 1));
        check(numericEquals(BYTE, 1, CHAR, 1));
        check(!boxedEquals(BYTE, 1, CHAR, 1));
        fails(IllegalArgumentException.class, () -> checkKey(BOOLEAN, 2));
        fails(IllegalArgumentException.class, () -> checkKey(FLOAT, -1));
        fails(IllegalArgumentException.class, () -> checkKey(CHAR, -1));
        fails(IllegalArgumentException.class, () -> checkKey(BYTE, 128));
        fails(IllegalArgumentException.class, () -> checkKey(8, 0));
        fails(IllegalArgumentException.class, () -> numericEquals(BOOLEAN, 1, INT, 1));
        check(!numericEquals(DOUBLE, ofDouble(Double.NaN), DOUBLE, ofDouble(Double.NaN)));
        check(numericEquals(DOUBLE, ofDouble(0.0), DOUBLE, ofDouble(-0.0)));
        check(!rawEquals(DOUBLE, ofDouble(0.0), DOUBLE, ofDouble(-0.0)));
        check(!boxedEquals(DOUBLE, ofDouble(0.0), DOUBLE, ofDouble(-0.0)));
        check(boxedEquals(DOUBLE, 0x7ff0000000000001L, DOUBLE, 0xfff8000000000013L));
        check(boxedEquals(FLOAT, 0x7f800001L, FLOAT, 0xffc00013L));
        check(ofFloatBits(0xff800001) == 0xff800001L);
        check(ofDoubleBits(0xfff0000000000001L) == 0xfff0000000000001L);
        Random random = new Random(0x4d335052494dL);
        for (int i = 0; i < 20000; i++) {
            int ai = random.nextInt(), bi = random.nextInt();
            long al = random.nextLong(), bl = random.nextLong();
            float af = Float.intBitsToFloat(ai), bf = Float.intBitsToFloat(bi);
            double ad = Double.longBitsToDouble(al), bd = Double.longBitsToDouble(bl);
            check(numericEquals(FLOAT, ofFloat(af), FLOAT, ofFloat(bf)) == (af == bf));
            check(numericEquals(DOUBLE, ofDouble(ad), DOUBLE, ofDouble(bd)) == (ad == bd));
            check(boxedEquals(FLOAT, ofFloat(af), FLOAT, ofFloat(bf)) == Float.valueOf(af).equals(bf));
            check(boxedEquals(DOUBLE, ofDouble(ad), DOUBLE, ofDouble(bd)) == Double.valueOf(ad).equals(bd));
            check(numericEquals(LONG, al, FLOAT, ofFloat(af)) == (al == af));
            check(numericEquals(INT, ai, DOUBLE, ofDouble(ad)) == (ai == ad));
            check(numericEquals(FLOAT, ofFloat(af), DOUBLE, ofDouble(ad)) == (af == ad));
            check(numericEquals(LONG, al, INT, bi) == (al == bi));
            check(numericEquals(CHAR, (char)ai, SHORT, (short)bi) == ((char)ai == (short)bi));
        }
        check(numericEquals(LONG, 16777217L, FLOAT, ofFloat(16777216f)));
        check(numericEquals(LONG, 9007199254740993L, DOUBLE, ofDouble(9007199254740992d)));

        Path dir = Files.createTempDirectory("m3-primitive-test-");
        UUID namespace = UUID.fromString("66684406-8739-53ee-7ffa-8ba9cd505833");
        int[] kinds = {BOOLEAN, BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE, INT, FLOAT, DOUBLE};
        long[] keys = {1, -128, -32768, 65535, 42, Long.MIN_VALUE, 0x7f800001L,
                0x7ff0000000000001L, 42, ofFloat(-0.0f), ofDouble(-0.0d)};
        Path path = dir.resolve("atoms.bin"), second = dir.resolve("same.bin");
        try {
            check(M3PrimitiveImage.publish(path, namespace, 1, kinds, keys) == 10);
            int[] reverseKinds = kinds.clone(); long[] reverseKeys = keys.clone();
            for (int i = 0, j = kinds.length - 1; i < j; i++, j--) {
                int k = reverseKinds[i]; reverseKinds[i] = reverseKinds[j]; reverseKinds[j] = k;
                long v = reverseKeys[i]; reverseKeys[i] = reverseKeys[j]; reverseKeys[j] = v;
            }
            M3PrimitiveImage.publish(second, namespace, 1, reverseKinds, reverseKeys);
            check(Arrays.equals(Files.readAllBytes(path), Files.readAllBytes(second)));
            byte[] original = Files.readAllBytes(path);
            fails(FileAlreadyExistsException.class, () -> M3PrimitiveImage.publish(path, namespace, 2, kinds, keys));
            check(Arrays.equals(original, Files.readAllBytes(path)));
            M3PrimitiveImage.Handle escaped;
            try (var a = M3PrimitiveImage.open(path); var b = M3PrimitiveImage.open(path)) {
                check(a.size() == 10);
                for (int i = 0; i < keys.length; i++) {
                    int slot = a.find(kinds[i], keys[i]);
                    check(slot >= 0 && a.key(slot) == keys[i] && a.kind(slot) == kinds[i]);
                    check(a.handle(slot).equals(b.handle(b.resolve(a.handle(slot)))));
                    long key = keys[i];
                    if (nativeTest) a.withAddress(slot, address -> check(readKey(address) == key));
                }
                check(a.find(INT, 43) == -1);
                int slot = a.find(INT, 42); escaped = a.handle(slot);
                final long[] addresses = new long[3];
                a.withAddress(slot, address -> addresses[0] = address);
                a.withAddress(slot, address -> addresses[1] = address);
                b.withAddress(b.resolve(escaped), address -> addresses[2] = address);
                check(addresses[0] == addresses[1]); check(addresses[0] != addresses[2]);
                System.out.println("sameIdentityDifferentMappingAddresses=PASS");
                fails(IllegalStateException.class, () -> a.withAddress(slot, address -> a.close()));
                fails(RuntimeException.class, () -> a.withAddress(slot, address -> { throw new RuntimeException("callback"); }));
                a.withAddress(slot, address -> check(address != 0));
                fails(IndexOutOfBoundsException.class, () -> a.withAddress(-1, address -> { }));
                fails(NullPointerException.class, () -> a.withAddress(slot, null));
                fails(IllegalArgumentException.class, () -> a.resolve(new M3PrimitiveImage.Handle(namespace, 2, INT, 42)));
                fails(IllegalArgumentException.class, () -> a.resolve(new M3PrimitiveImage.Handle(new UUID(0,0), 1, INT, 42)));
                CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
                try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                    Future<?> reader = executor.submit(() -> a.withAddress(slot, address -> {
                        entered.countDown();
                        try { if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("lease timeout"); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                        if (nativeTest) check(readKey(address) == 42);
                    }));
                    check(entered.await(10, TimeUnit.SECONDS));
                    try { fails(IllegalStateException.class, a::close); } finally { release.countDown(); }
                    reader.get(10, TimeUnit.SECONDS);
                }
                a.close(); a.close();
                fails(IllegalStateException.class, () -> a.find(INT, 42));
                fails(IllegalStateException.class, () -> a.withAddress(slot, address -> { }));
            }
            try (var reopened = M3PrimitiveImage.open(path)) { check(reopened.resolve(escaped) >= 0); }
            List<String> childCommand = new ArrayList<>();
            childCommand.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
            String patch = System.getProperty("m3.test.patch");
            if (patch != null) { childCommand.add("--patch-module"); childCommand.add("java.base=" + patch); }
            childCommand.addAll(List.of("--add-exports", "java.base/jdk.internal.mindex=ALL-UNNAMED", "-cp",
                    System.getProperty("java.class.path"), "PrimitiveIdentityTest", "child", path.toString(),
                    namespace.toString(), "1", "4", "42"));
            Process child = new ProcessBuilder(childCommand).redirectErrorStream(true).start();
            if (!child.waitFor(15, TimeUnit.SECONDS)) { child.destroyForcibly(); throw new AssertionError("child timeout"); }
            String output = new String(child.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            check(child.exitValue() == 0 && output.contains("childIdentity=PASS"));
            System.out.print(output);
            Path corrupt = dir.resolve("corrupt.bin");
            byte[] bad = original.clone(); bad[bad.length-1] ^= 1; Files.write(corrupt, bad);
            fails(IOException.class, () -> M3PrimitiveImage.open(corrupt));
            bad = original.clone(); ByteBuffer.wrap(bad).putLong(96, 9); redigest(bad); Files.write(corrupt, bad);
            fails(IOException.class, () -> M3PrimitiveImage.open(corrupt));
            bad = original.clone(); System.arraycopy(bad, 96, bad, 112, 16); redigest(bad); Files.write(corrupt, bad);
            fails(IOException.class, () -> M3PrimitiveImage.open(corrupt));
            Files.write(corrupt, Arrays.copyOf(original, 95)); fails(IOException.class, () -> M3PrimitiveImage.open(corrupt));
            Path empty = dir.resolve("empty.bin");
            check(M3PrimitiveImage.publish(empty, namespace, 1, new int[0], new long[0]) == 0);
            try (var image = M3PrimitiveImage.open(empty)) { check(image.size() == 0 && image.find(INT, 42) == -1); }
            Path raced = dir.resolve("race.bin"); AtomicInteger wins = new AtomicInteger();
            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                List<Future<?>> jobs = new ArrayList<>();
                for (int i=0; i<2; i++) jobs.add(executor.submit(() -> {
                    try { M3PrimitiveImage.publish(raced, namespace, 1, kinds, keys); wins.incrementAndGet(); }
                    catch (FileAlreadyExistsException expected) { }
                    catch (IOException e) { throw new java.io.UncheckedIOException(e); }
                }));
                for (Future<?> job : jobs) job.get(10, TimeUnit.SECONDS);
            }
            check(wins.get() == 1);
            try (var image = M3PrimitiveImage.open(raced)) { check(image.find(INT, 42) >= 0); }
        } finally {
            try (var files = Files.list(dir)) { for (Path file : files.toList()) Files.deleteIfExists(file); }
            Files.deleteIfExists(dir);
        }
        Object x = new Object(), y = new Object(); check(x != y);
        String s = new String("same"), t = new String("same"); check(s != t && s.equals(t));
        System.out.println("PrimitiveIdentityTest assertions=" + assertions + " JNI=" + nativeTest + " PASS");
    }
}
