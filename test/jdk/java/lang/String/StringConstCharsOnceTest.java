/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others
 */

/*
 * @test
 * @summary M3JDK decision 51: chValue is filled once. Threads that race the first toCharArray
 *          of fresh Latin-1 and UTF-16 Strings all see one chValue identity afterwards, equal to
 *          the chars, and String.constChars publishes the fill by a compare-and-set
 * @modules java.base/java.lang:open
 * @run main/othervm StringConstCharsOnceTest
 * @run main/othervm -XX:-CompactStrings StringConstCharsOnceTest
 * @run main/othervm -Xcomp -XX:-TieredCompilation StringConstCharsOnceTest 100
 */

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class StringConstCharsOnceTest {
    private static final String UNSAFE = "jdk/internal/misc/Unsafe.";
    /** Long enough that inflating the chars keeps first callers in the race for a while. */
    private static final int UNITS = 1 << 16;
    private static final Field CH_VALUE;
    private static final List<String> FAILURES = new ArrayList<>();
    private static final AtomicInteger DONE = new AtomicInteger();
    private static volatile int round;
    private static volatile String current;
    private static long checks;

    static {
        try {
            CH_VALUE = String.class.getDeclaredField("chValue");
            CH_VALUE.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void main(String[] args) throws Exception {
        int rounds = args.length > 0 ? Integer.parseInt(args[0]) : 400;
        int threads = Math.max(4, Math.min(16, Runtime.getRuntime().availableProcessors()));
        structure();
        int races = race(rounds, threads);
        System.out.println("StringConstCharsOnceTest rounds=" + rounds + " threads=" + threads
                + " units=" + UNITS + " roundsWithMoreThanOneChValue=" + races);
        System.out.println("StringConstCharsOnceTest checks=" + checks
                + " failures=" + FAILURES.size());
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(10).forEach(System.out::println);
            throw new AssertionError(FAILURES.size() + " failed checks");
        }
    }

    /**
     * Each round: a fresh String with no chValue; the workers start together, each calls
     * toCharArray once and reads chValue right after. One filled-once array means every worker
     * read the same identity, the final one, and every copy equals the chars.
     */
    private static int race(int rounds, int threads) throws Exception {
        Object[][] seen = new Object[threads][rounds];
        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            int worker = t;
            workers.add(Thread.ofPlatform().daemon().start(() -> work(worker, rounds, seen)));
        }
        int races = 0;
        for (int r = 0; r < rounds; r++) {
            char[] expected = text(r);
            String fresh = new String(expected);
            check(CH_VALUE.get(fresh) == null, "round " + r + ": a fresh String has no chValue");
            current = fresh;
            DONE.set(0);
            round = r + 1;
            while (DONE.get() < threads) {
                Thread.onSpinWait();
            }
            Object last = CH_VALUE.get(fresh);
            check(last instanceof char[] chars && Arrays.equals(chars, expected),
                    "round " + r + ": chValue equals the chars");
            Set<Object> identities = new HashSet<>();
            for (int t = 0; t < threads; t++) {
                identities.add(new Identity(seen[t][r]));
            }
            if (identities.size() != 1 || seen[0][r] != last) {
                races++;
                check(false, "round " + r + ": " + identities.size()
                        + " chValue identities seen by " + threads + " first callers");
            } else {
                check(true, "one chValue identity");
            }
        }
        round = -1;
        for (Thread worker : workers) {
            worker.join();
        }
        return races;
    }

    private static void work(int worker, int rounds, Object[][] seen) {
        try {
            for (int r = 0; r < rounds; r++) {
                while (round != r + 1) {
                    Thread.onSpinWait();
                }
                String s = current;
                char[] chars = s.toCharArray();
                seen[worker][r] = CH_VALUE.get(s);
                if (!Arrays.equals(chars, text(r))) {
                    synchronized (FAILURES) {
                        FAILURES.add("round " + r + ": worker " + worker + " copy differs");
                    }
                }
                DONE.incrementAndGet();
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /** Latin-1 text in even rounds, UTF-16 text in odd rounds. */
    private static char[] text(int r) {
        char[] chars = new char[UNITS];
        char base = (r & 1) == 0 ? 'a' : (char) 0x3b1;
        for (int k = 0; k < UNITS; k++) {
            chars[k] = (char) (base + (k + r) % 23);
        }
        return chars;
    }

    /**
     * The structural check: String.constChars invokes Unsafe.compareAndSetReference to publish
     * chValue and no longer the store-store fence of a plain store. Read from the String.class
     * that java.base serves (the patched one under --patch-module).
     */
    private static void structure() throws IOException {
        Set<String> invoked = invokedBy("constChars");
        check(invoked.contains(UNSAFE + "compareAndSetReference"),
                "String.constChars publishes chValue by Unsafe.compareAndSetReference: " + invoked);
        check(!invoked.contains(UNSAFE + "storeStoreFence"),
                "String.constChars no longer publishes behind a store-store fence: " + invoked);
    }

    /** owner.name of every method the named String method invokes (its invoke instructions). */
    private static Set<String> invokedBy(String methodName) throws IOException {
        byte[] bytes;
        Module base = Object.class.getModule();
        try (InputStream in = base.getResourceAsStream("java/lang/String.class")) {
            bytes = in.readAllBytes();
        }
        DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes));
        data.skipNBytes(8);
        int count = data.readUnsignedShort();
        String[] utf8 = new String[count];
        int[][] refs = new int[count][];
        for (int i = 1; i < count; i++) {
            int tag = data.readUnsignedByte();
            switch (tag) {
                case 1 -> utf8[i] = data.readUTF();
                case 3, 4 -> data.skipNBytes(4);
                case 5, 6 -> {
                    data.skipNBytes(8);
                    i++;
                }
                case 7, 8, 16, 19, 20 -> refs[i] = new int[] {data.readUnsignedShort()};
                case 9, 10, 11, 12, 17, 18 ->
                        refs[i] = new int[] {data.readUnsignedShort(), data.readUnsignedShort()};
                case 15 -> data.skipNBytes(3);
                default -> throw new IOException("constant pool tag " + tag);
            }
        }
        data.skipNBytes(6);
        data.skipNBytes(2L * data.readUnsignedShort());
        skipMembers(data);
        Set<String> invoked = new HashSet<>();
        for (int methods = data.readUnsignedShort(); methods > 0; methods--) {
            data.skipNBytes(2);
            String name = utf8[data.readUnsignedShort()];
            data.skipNBytes(2);
            for (int attributes = data.readUnsignedShort(); attributes > 0; attributes--) {
                String attribute = utf8[data.readUnsignedShort()];
                byte[] body = data.readNBytes(data.readInt());
                if (name.equals(methodName) && attribute.equals("Code")) {
                    invoked.addAll(invokes(body, utf8, refs));
                }
            }
        }
        return invoked;
    }

    private static void skipMembers(DataInputStream data) throws IOException {
        for (int fields = data.readUnsignedShort(); fields > 0; fields--) {
            data.skipNBytes(6);
            for (int attributes = data.readUnsignedShort(); attributes > 0; attributes--) {
                data.skipNBytes(2);
                data.skipNBytes(data.readInt());
            }
        }
    }

    /** Scans the code bytes of a Code attribute for invokevirtual/special/static. */
    private static Set<String> invokes(byte[] body, String[] utf8, int[][] refs) {
        Set<String> invoked = new HashSet<>();
        int length = ((body[4] & 0xff) << 24) | ((body[5] & 0xff) << 16)
                | ((body[6] & 0xff) << 8) | (body[7] & 0xff);
        for (int at = 8; at + 2 < 8 + length; at++) {
            int opcode = body[at] & 0xff;
            if (opcode < 0xb6 || opcode > 0xb8) {
                continue;
            }
            int index = ((body[at + 1] & 0xff) << 8) | (body[at + 2] & 0xff);
            int[] ref = index < refs.length ? refs[index] : null;
            if (ref == null || ref.length != 2 || refs[ref[0]] == null || refs[ref[1]] == null
                    || refs[ref[0]].length != 1 || refs[ref[1]].length != 2) {
                continue;
            }
            invoked.add(utf8[refs[ref[0]][0]] + "." + utf8[refs[ref[1]][0]]);
        }
        return invoked;
    }

    /** Identity, not equality, of a chValue array (null included). */
    private record Identity(Object array) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Identity that && that.array == array;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(array);
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) {
            synchronized (FAILURES) {
                FAILURES.add(label);
            }
        }
    }
}
