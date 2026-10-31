/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary M3 immediate words use fixed parent-owned arrays; canonical live slot reuse,
 *          interning concurrency, tuple/slice parity, and native JNI character shadow.
 * @modules java.base/java.lang:+open
 * @run main/othervm M3StringFixedPackedArrayTest
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public final class M3StringFixedPackedArrayTest {
    private static long checks;
    private static final String IDENT =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_$";

    public static void main(String[] args) throws Throwable {
        Class<?> immediate = Class.forName("java.lang.M3StringImmediate");
        Class<?> ownerClass = Class.forName("java.lang.M3StringOwner");
        Class<?> valueClass = Class.forName("java.lang.M3String");
        Class<?> poolClass = Class.forName("java.lang.M3StringPool");
        MethodHandles.Lookup a = MethodHandles.privateLookupIn(immediate, MethodHandles.lookup());
        MethodHandles.Lookup v = MethodHandles.privateLookupIn(valueClass, MethodHandles.lookup());
        MethodHandles.Lookup p = MethodHandles.privateLookupIn(poolClass, MethodHandles.lookup());
        MethodHandle pack = a.findStatic(immediate, "packChars",
                MethodType.methodType(long.class, char[].class, int.class, int.class));
        MethodHandle of = a.findStatic(immediate, "of", MethodType.methodType(immediate, long.class));
        MethodHandle words = a.findGetter(immediate, "words", long[].class);
        MethodHandle slot = a.findGetter(immediate, "slot", int.class);
        MethodHandle whole = v.findStatic(valueClass, "whole",
                MethodType.methodType(valueClass, ownerClass));
        MethodHandle concat = p.findStatic(poolClass, "concat",
                MethodType.methodType(valueClass, valueClass, valueClass));
        MethodHandle slice = v.findVirtual(valueClass, "slice",
                MethodType.methodType(valueClass, int.class, int.class));
        MethodHandle at = v.findVirtual(valueClass, "charAt", MethodType.methodType(char.class, int.class));
        MethodHandle shadow = v.findVirtual(valueClass, "charShadow",
                MethodType.methodType(char[].class));

        Random random = new Random(0x53A77L);
        ArrayList<Object> live = new ArrayList<>();
        for (int iteration = 0; iteration < 50_000; iteration++) {
            int length = 1 + random.nextInt(10);
            char[] chars = new char[length];
            for (int j = 0; j < length; j++) {
                chars[j] = length <= 7
                        ? (char) random.nextInt(256)
                        : IDENT.charAt(random.nextInt(IDENT.length()));
            }
            String spelling = new String(chars);
            long word = (long) pack.invoke(chars, 0, length);
            require(word != 0L, "pack refusal: " + spelling);
            Object first = of.invoke(word);
            Object second = of.invoke(word);
            require(first == second, "interned owner identity");
            long[] array = (long[]) words.invoke(first);
            int index = (int) slot.invoke(first);
            require(array.length == 32, "fixed physical page size");
            require(index >= 0 && index < array.length, "slot bounds");
            require(array[index] == word, "canonical word in physical array");
            Object view = whole.invoke(first);
            for (int j = 0; j < length; j++) {
                require((char) at.invoke(view, j) == chars[j], "Java character parity");
            }
            if (iteration % 31 == 0) live.add(first);
        }

        long word = (long) pack.invoke("ABCDE".toCharArray(), 0, 5);
        Object anchor = of.invoke(word);
        CountDownLatch begin = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] workers = new Thread[8];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(() -> {
                try {
                    begin.await();
                    for (int j = 0; j < 12_000; j++) {
                        Object got = of.invoke(word);
                        if (got != anchor) throw new AssertionError("concurrent canonical identity");
                    }
                } catch (Throwable throwable) {
                    failure.compareAndSet(null, throwable);
                }
            }, "m3-immediate-page-" + i);
            workers[i].start();
        }
        begin.countDown();
        for (Thread worker : workers) worker.join();
        if (failure.get() != null) throw new AssertionError(failure.get());
        checks += 96_000;

        for (int i = 0; i < live.size(); i++) {
            Object kept = live.get(i);
            long[] array = (long[]) words.invoke(kept);
            int index = (int) slot.invoke(kept);
            require(of.invoke(array[index]) == kept, "live slot must not be recycled");
        }

        String leftText = "A\u0000B";
        String rightText = "0123456789";
        long leftWord = (long) pack.invoke(leftText.toCharArray(), 0, leftText.length());
        long rightWord = (long) pack.invoke(rightText.toCharArray(), 0, rightText.length());
        Object left = whole.invoke(of.invoke(leftWord));
        Object right = whole.invoke(of.invoke(rightWord));
        Object joined = concat.invoke(left, right);
        String expected = leftText + rightText;
        require(((char[]) shadow.invoke(joined)).length == expected.length(), "JNI joined size");
        require(expected.equals(new String((char[]) shadow.invoke(joined))), "JNI joined characters");
        Object sub = slice.invoke(joined, 2, expected.length() - 1);
        String expectedSub = expected.substring(2, expected.length() - 1);
        require(expectedSub.equals(new String((char[]) shadow.invoke(sub))),
                "JNI sliced tuple character parity");
        char[] invalid = "\u0141".toCharArray();
        require((long) pack.invoke(invalid, 0, 1) == 0L,
                "non-Latin1 must not be silently truncated");
        System.out.println("M3StringFixedPackedArrayTest PASS checks=" + checks);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
