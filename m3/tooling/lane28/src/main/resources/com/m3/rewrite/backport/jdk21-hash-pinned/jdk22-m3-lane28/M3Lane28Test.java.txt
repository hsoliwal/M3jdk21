// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Differential primitive lanes, maintained rank/select and production JNI peer
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm --add-opens=java.base/jdk.internal.mindex=ALL-UNNAMED M3Lane28Test
 */
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.TreeSet;
import jdk.internal.mindex.M3Address28;
import jdk.internal.mindex.M3BitLane28;
import jdk.internal.mindex.M3IntLane28;
import jdk.internal.mindex.M3LongLane28;

public class M3Lane28Test {
    private static final int LIMIT = M3Address28.MAX_SLOTS;
    private static long checks;
    private static long nativeCalls;
    private static long operations;

    public static void main(String[] args) throws Exception {
        String library = System.getProperty("lane28.native");
        if (library != null) {
            // Stock-JDK proof only: the loader is patched into java.base, never the product.
            Class.forName("jdk.internal.mindex.Lane28Loader").getMethod("load", String.class)
                    .invoke(null, library);
        }
        check(M3BitLane28.class.getModule().getName().equals("java.base"), "java.base owner");
        check(M3BitLane28.class.getClassLoader() == null, "bootstrap owner");
        addresses();
        primitiveLanes();
        sparseBoundaries();
        bitmap();
        nativeRefusals();
        System.out.printf("LANE28 checks=%d operations=%d nativeCalls=%d stablePages=true maintainedCounts=true%n",
                checks, operations, nativeCalls);
    }

    private static void addresses() {
        Random random = new Random(28);
        for (int i = 0; i < 4096; i++) {
            int slot = random.nextInt(LIMIT);
            check(M3Address28.compose(M3Address28.region(slot), M3Address28.block(slot),
                    M3Address28.offset(slot)) == slot, "address round trip");
        }
        M3Address28.checkRange(LIMIT, LIMIT);
        M3Address28.checkRangeSize(LIMIT, 0);
        rejects(IndexOutOfBoundsException.class, () -> M3Address28.checkSlot(-1));
        rejects(IndexOutOfBoundsException.class, () -> M3Address28.checkSlot(LIMIT));
        rejects(IndexOutOfBoundsException.class, () -> M3Address28.compose(256, 0, 0));
        rejects(IndexOutOfBoundsException.class, () -> M3Address28.compose(0, -1, 0));
        rejects(IndexOutOfBoundsException.class, () -> M3Address28.compose(0, 0, 4096));
        rejects(IndexOutOfBoundsException.class, () -> M3Address28.checkRange(1, 0));
        rejects(IndexOutOfBoundsException.class, () -> M3Address28.checkRangeSize(1, Integer.MAX_VALUE));
    }

    private static void primitiveLanes() throws Exception {
        M3IntLane28 ints = new M3IntLane28();
        M3LongLane28 longs = new M3LongLane28();
        int[] intOracle = new int[24576];
        long[] longOracle = new long[intOracle.length];
        int[] intOut = new int[intOracle.length];
        long[] longOut = new long[intOracle.length];
        Random random = new Random(284096);
        for (int step = 0; step < 12000; step++) {
            int index = random.nextInt(intOracle.length);
            int length = random.nextInt(Math.min(513, intOracle.length - index + 1));
            int iv = random.nextInt();
            long lv = random.nextLong();
            switch (step % 6) {
                case 0 -> { ints.set(index, iv); longs.set(index, lv); intOracle[index] = iv; longOracle[index] = lv; }
                case 1 -> {
                    check(ints.getAndSet(index, iv) == intOracle[index], "int exchange");
                    check(longs.getAndSet(index, lv) == longOracle[index], "long exchange");
                    intOracle[index] = iv; longOracle[index] = lv;
                }
                case 2 -> {
                    ints.fill(index, index + length, iv); longs.fill(index, index + length, lv);
                    Arrays.fill(intOracle, index, index + length, iv);
                    Arrays.fill(longOracle, index, index + length, lv);
                }
                case 3 -> {
                    int destination = Math.max(0, Math.min(intOracle.length - length, index + random.nextInt(301) - 150));
                    ints.copyFrom(ints, index, destination, length); longs.copyFrom(longs, index, destination, length);
                    System.arraycopy(intOracle, index, intOracle, destination, length);
                    System.arraycopy(longOracle, index, longOracle, destination, length);
                }
                case 4 -> {
                    int[] sourceI = {iv, 0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE};
                    long[] sourceL = {lv, 0, -1, Long.MIN_VALUE, Long.MAX_VALUE};
                    int to = Math.min(index, intOracle.length - 5);
                    ints.copyFrom(sourceI, 0, to, 5); longs.copyFrom(sourceL, 0, to, 5);
                    System.arraycopy(sourceI, 0, intOracle, to, 5);
                    System.arraycopy(sourceL, 0, longOracle, to, 5);
                    sourceI[0] = 123; sourceL[0] = 456;
                    check(ints.get(to) == iv && longs.get(to) == lv, "no retained caller arrays");
                }
                default -> { ints.clear(index); longs.clear(index); intOracle[index] = 0; longOracle[index] = 0; }
            }
            operations++;
            check(ints.get(index) == intOracle[index] && longs.get(index) == longOracle[index], "scalar parity");
            if (step % 97 == 0) {
                ints.copyTo(0, intOut, 0, intOut.length); longs.copyTo(0, longOut, 0, longOut.length);
                check(Arrays.equals(intOracle, intOut), "int range and overlap parity");
                check(Arrays.equals(longOracle, longOut), "long range and overlap parity");
            }
        }
        int[] beforeI = intOracle.clone(); long[] beforeL = longOracle.clone();
        rejects(IndexOutOfBoundsException.class, () -> ints.copyFrom(ints, -1, 0, 2));
        rejects(IndexOutOfBoundsException.class, () -> longs.copyFrom(longs, 0, LIMIT - 1, 2));
        rejects(IndexOutOfBoundsException.class, () -> ints.copyFrom(new int[1], 0, 0, 2));
        rejects(IndexOutOfBoundsException.class, () -> longs.copyTo(0, new long[1], 1, 1));
        rejects(NullPointerException.class, () -> ints.copyFrom((M3IntLane28) null, 0, 0, 0));
        rejects(NullPointerException.class, () -> longs.copyFrom((long[]) null, 0, 0, 0));
        ints.copyTo(0, intOut, 0, intOut.length); longs.copyTo(0, longOut, 0, longOut.length);
        check(Arrays.equals(beforeI, intOut) && Arrays.equals(beforeL, longOut), "bounds failure has no writes");
        ints.set(0, Integer.MAX_VALUE); longs.set(0, Long.MAX_VALUE);
        rejects(ArithmeticException.class, () -> ints.incrementAndGet(0));
        rejects(ArithmeticException.class, () -> longs.addAndGet(0, 1));
        check(ints.get(0) == Integer.MAX_VALUE && longs.get(0) == Long.MAX_VALUE, "overflow has no writes");
        ints.set(0, 7); longs.set(0, 7);
        Object intPage = ((int[][][]) field(ints, "regions"))[0][0];
        Object longPage = ((long[][][]) field(longs, "regions"))[0][0];
        for (int i = 0; i < 20000; i++) { ints.incrementAndGet(0); longs.addAndGet(0, 1); }
        check(intPage == ((int[][][]) field(ints, "regions"))[0][0], "stable int page");
        check(longPage == ((long[][][]) field(longs, "regions"))[0][0], "stable long page");
        check(ints.get(0) == 20007 && longs.get(0) == 20007, "prepared increments");
    }

    private static void sparseBoundaries() {
        M3IntLane28 ints = new M3IntLane28(); M3LongLane28 longs = new M3LongLane28();
        ints.fill(0, LIMIT, 0); longs.fill(0, LIMIT, 0);
        ints.copyFrom(new int[8194], 0, 4095, 8194); longs.copyFrom(new long[8194], 0, 4095, 8194);
        check(ints.allocatedBlockCount() == 0 && longs.allocatedBlockCount() == 0, "zero does not allocate pages");
        int[] points = {0, 63, 64, 4095, 4096, (1 << 20) - 1, 1 << 20, LIMIT - 4097, LIMIT - 1};
        for (int point : points) { ints.set(point, point + 1); longs.set(point, -(long) point - 1); }
        for (int point : points) {
            check(ints.get(point) == point + 1 && longs.get(point) == -(long) point - 1, "sparse boundary value");
            check(ints.isBlockAllocated(point) && longs.isBlockAllocated(point), "page allocation visible");
        }
        check(ints.allocatedRegionCount() == 3 && longs.allocatedRegionCount() == 3, "sparse region count");
        check(ints.payloadBytes() == ints.allocatedBlockCount() * 4096L * 4, "int payload accounting");
        check(longs.payloadBytes() == longs.allocatedBlockCount() * 4096L * 8, "long payload accounting");
        M3IntLane28 copyI = new M3IntLane28(); M3LongLane28 copyL = new M3LongLane28();
        copyI.copyFrom(ints, LIMIT - 8192, 0, 8192); copyL.copyFrom(longs, LIMIT - 8192, 0, 8192);
        for (int i = 0; i < 8192; i++) check(copyI.get(i) == ints.get(LIMIT - 8192 + i)
                && copyL.get(i) == longs.get(LIMIT - 8192 + i), "cross-owner page parity");
        copyI.copyFrom(new M3IntLane28(), 0, 0, 8192); copyL.copyFrom(new M3LongLane28(), 0, 0, 8192);
        check(copyI.get(8191) == 0 && copyL.get(8191) == 0, "absent source clears destination");
        ints.copyTo(LIMIT, new int[0], 0, 0); longs.copyTo(LIMIT, new long[0], 0, 0);
    }

    private static void bitmap() throws Exception {
        M3BitLane28 bits = new M3BitLane28(); TreeSet<Integer> oracle = new TreeSet<>();
        check(bits.rank(0) == 0 && bits.rank(LIMIT) == 0 && bits.select(0) == -1, "empty rank select");
        check(field(bits, "regionCounts") == null, "empty queries avoid metadata");
        for (int point : new int[]{0, 1, 63, 64, 4095, 4096, 1048575, 1048576, LIMIT - 1}) {
            bits.set(point); oracle.add(point);
        }
        check(field(bits, "regionCounts") == null, "writes do not prepare metadata");
        check(bits.rank(65) == 4, "first rank");
        Object counts = field(bits, "regionCounts"), pages = field(bits, "pageCounts");
        check(counts != null && pages != null, "lazy counts published");
        Random random = new Random(280064);
        for (int step = 0; step < 16000; step++) {
            int region = switch (step % 4) { case 0 -> 0; case 1 -> 1 << 20; case 2 -> LIMIT - 16384; default -> 4 << 20; };
            int slot = region + random.nextInt(16384);
            switch (step % 3) {
                case 0 -> { bits.set(slot); oracle.add(slot); }
                case 1 -> { bits.clear(slot); oracle.remove(slot); }
                default -> { bits.flip(slot); if (!oracle.remove(slot)) oracle.add(slot); }
            }
            operations++;
            check(bits.get(slot) == oracle.contains(slot), "bit scalar parity");
            check(bits.cardinality() == oracle.size(), "maintained total");
            if (step % 61 == 0) {
                check(bits.rank(slot) == oracle.headSet(slot).size(), "maintained rank");
                int end = Math.min(LIMIT, slot + random.nextInt(6000));
                int expected = oracle.subSet(slot, end).size();
                check(bits.cardinality(slot, end) == expected, "range count");
                nativeParity(bits, slot, end, expected);
                Integer next = oracle.ceiling(slot), previous = oracle.floor(slot);
                check(bits.nextSetBit(slot) == (next == null ? -1 : next), "next set bit");
                check(bits.previousSetBit(slot) == (previous == null ? -1 : previous), "previous set bit");
                int ordinal = random.nextInt(oracle.size());
                check(bits.select(ordinal) == oracle.stream().skip(ordinal).findFirst().orElseThrow(), "maintained select");
                check(bits.cardinality(0, 1 << 20) == oracle.headSet(1 << 20).size(), "complete region count");
                check(bits.cardinality(0, 4096) == oracle.headSet(4096).size(), "complete page count");
            }
        }
        check(counts == field(bits, "regionCounts") && pages == field(bits, "pageCounts"), "metadata directories retained");
        check(bits.payloadBytes() == bits.allocatedBlockCount() * 512L, "bitmap payload accounting");
        nativeParity(bits, 0, LIMIT, oracle.size());
        nativeParity(bits, LIMIT, LIMIT, 0);
        check(bits.nextSetBit(LIMIT) == -1 && bits.previousSetBit(-1) == -1, "traversal endpoints");
        check(bits.select(-1) == -1 && bits.select(oracle.size()) == -1, "select outside ordinals");
        rejects(IndexOutOfBoundsException.class, () -> bits.rank(LIMIT + 1));
        rejects(IndexOutOfBoundsException.class, () -> bits.previousSetBit(LIMIT));
        rejects(IndexOutOfBoundsException.class, () -> bits.cardinalityNative(1, 0));

        // Dense and word-edge differential oracle, independent of the sparse TreeSet.
        M3BitLane28 dense = new M3BitLane28(); BitSet expected = new BitSet();
        for (int i = 0; i < 8192; i++) { dense.set(i); expected.set(i); }
        check(dense.rank(8191) == 8191 && dense.select(8191) == 8191, "full page short count 4096");
        for (int from : new int[]{0, 1, 62, 63, 64, 65, 4095, 4096, 8191, 8192}) {
            for (int to : new int[]{0, 1, 63, 64, 65, 4096, 4097, 8192}) if (to >= from) {
                int count = expected.get(from, to).cardinality();
                check(dense.cardinality(from, to) == count, "dense BitSet parity");
                nativeParity(dense, from, to, count);
            }
        }
        for (int i = 0; i < 8192; i++) dense.clear(i);
        check(dense.rank(4096) == 0 && dense.select(0) == -1 && dense.cardinality(0, 8192) == 0, "counts after clearing prepared pages");
        nativeParity(dense, 0, LIMIT, 0);
    }

    private static void nativeParity(M3BitLane28 bits, int from, int to, int expected) {
        check(bits.cardinalityNative(from, to) == expected, "native parity"); nativeCalls++;
    }

    private static void nativeRefusals() throws Exception {
        Method method = M3BitLane28.class.getDeclaredMethod("cardinality0", long[][][].class, int.class, int.class);
        method.setAccessible(true);
        rejects(IllegalArgumentException.class, () -> method.invoke(null, null, 0, 0));
        rejects(IllegalArgumentException.class, () -> method.invoke(null, new long[1][][], 0, 1));
        rejects(IllegalArgumentException.class, () -> method.invoke(null, new long[256][][], -1, 1));
        rejects(IllegalArgumentException.class, () -> method.invoke(null, new long[256][][], 1, 0));
        rejects(IllegalArgumentException.class, () -> method.invoke(null, new long[256][][], 0, LIMIT + 1));
        long[][][] malformed = new long[256][][];
        malformed[0] = new long[1][];
        rejects(IllegalArgumentException.class, () -> method.invoke(null, malformed, 0, 1));
        malformed[0] = new long[256][]; malformed[0][0] = new long[63];
        rejects(IllegalArgumentException.class, () -> method.invoke(null, malformed, 0, 1));
        nativeCalls += 7;
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }

    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void rejects(Class<? extends Throwable> expected, Action action) {
        try { action.run(); } catch (Throwable failure) {
            if (failure instanceof InvocationTargetException invocation) failure = invocation.getCause();
            check(expected.isInstance(failure), "wrong exception: " + failure); return;
        }
        throw new AssertionError("missing " + expected.getName());
    }
    private static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
}
