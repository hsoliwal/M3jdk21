// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Exact adaptive trigram containment across sparse/dense sets and warm payload fences
 * @modules java.base/jdk.internal.mindex:open
 * @run main M3TQSparseContainmentTest
 */
import java.lang.reflect.Constructor;
import java.util.Random;
import java.util.TreeSet;
import jdk.internal.mindex.M3TQ;

public final class M3TQSparseContainmentTest {
    private static long checks;
    private static final Constructor<M3TQ.Facts> FACTORY;
    static {
        try {
            FACTORY = M3TQ.Facts.class.getDeclaredConstructor(int.class, int.class, int.class, long[].class);
            FACTORY.setAccessible(true);
        } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }
    public static void main(String[] args) throws Exception {
        Random random = new Random(0x4d335451L);
        for (int trial = 0; trial < 10000; trial++) {
            TreeSet<Long> source = new TreeSet<>(), required = new TreeSet<>();
            int size = trial % 17 == 0 ? 4096 : random.nextInt(257);
            for (int i = 0; i < size; i++) source.add(random.nextLong() & 0xffffffffffffL);
            int count = trial % 3 == 0 ? random.nextInt(4) : random.nextInt(257);
            Long[] members = source.toArray(Long[]::new);
            for (int i = 0; i < count; i++) {
                required.add(members.length != 0 && (trial % 2 == 0 || random.nextBoolean())
                        ? members[random.nextInt(members.length)] : random.nextLong() & 0xffffffffffffL);
            }
            M3TQ.Facts a = facts(source), b = facts(required);
            check(a.containsAll(b) == source.containsAll(required), "set oracle trial " + trial);
            check(a.containsAll(a), "identity");
            check(b.containsAll(a) == required.containsAll(source), "reverse set oracle");
        }
        TreeSet<Long> large = new TreeSet<>();
        for (long i = 0; i < 32766; i++) large.add(i);
        M3TQ.Facts prepared = facts(large);
        for (long key : new long[] {0, 1, 16383, 32765, 32766, 0xffffffffffffL}) {
            check(prepared.containsAll(facts(new TreeSet<>(java.util.List.of(key))))
                    == large.contains(key), "sparse extreme " + key);
        }
        check(facts(new TreeSet<>()).containsAll(facts(new TreeSet<>())), "empty identity");
        try { prepared.containsAll(null); throw new AssertionError("null accepted"); }
        catch (NullPointerException expected) { checks++; }

        Fence input = new Fence("abc-bcd-\u0000\uD800x\uDC00-\uFFFF".repeat(100));
        M3TQ.Facts source = M3TQ.precompute(input, input.length());
        check(input.reads == input.length(), "single preparation scan");
        input.closed = true;
        M3TQ.Facts needed = M3TQ.precompute("abcd", 4);
        for (int i = 0; i < 1000; i++) check(source.containsAll(needed), "necessary-condition false positive retained");
        check(input.reads == input.length(), "zero warm payload reads");
        check(!"abc-bcd".contains("abcd"), "containment is not exact substring proof");
        System.out.println("TQ_SPARSE_CONTAINMENT_PASS checks=" + checks + " warmPayloadReads=0");
    }
    private static M3TQ.Facts facts(TreeSet<Long> keys) throws Exception {
        return FACTORY.newInstance(keys.size() + 2, 0, 0, keys.stream().mapToLong(Long::longValue).toArray());
    }
    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label);
    }
    private static final class Fence implements CharSequence {
        private final String text;
        int reads;
        boolean closed;
        Fence(String text) { this.text = text; }
        public int length() { return text.length(); }
        public char charAt(int at) {
            if (closed) throw new AssertionError("warm containment read payload");
            reads++;
            return text.charAt(at);
        }
        public CharSequence subSequence(int from, int to) { throw new AssertionError("slice materialization"); }
    }
}
