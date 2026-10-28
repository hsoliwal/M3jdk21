/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Check tuple comparison boundaries and report bounded per-thread allocation and CPU counters
 * @modules java.management jdk.management
 * @run main/othervm -Xmx128m --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringFirstUnitResourceProbe
 * @run main/othervm -Xmx128m --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringFirstUnitResourceProbe
 */

import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Resource diagnostic for matched, source-built M3 images. There is no timing or
 * allocation pass threshold. Counter support and raw per-batch values are printed;
 * allocation traffic does not measure retained heap or native/process memory.
 *
 * The Aa/BB collision bytes and padded eight-unit tuple segments are adapted from
 * M3StringKeyedTiming, retaining its original copyright and license above. This
 * bounded pair uses keys zero and one from its early-mismatch fixture. The same
 * compiled class and parameters should be used on every compared image.
 */
public final class M3StringFirstUnitResourceProbe {
    private static final int UNITS = 256;
    private static final int PIECE = 8;
    private static volatile long sink;
    private static long checks;

    public static void main(String[] arguments) throws Exception {
        String label = arguments.length == 0 ? "unspecified" : arguments[0];
        int operations = Integer.getInteger("m3.probe.operations", 2048);
        int warmupOperations = Integer.getInteger("m3.probe.warmupOperations", 1024);
        int samples = Integer.getInteger("m3.probe.samples", 3);
        if (operations < 1 || operations > 1_000_000 || warmupOperations < 0
                || warmupOperations > 1_000_000 || samples < 1 || samples > 100) {
            throw new IllegalArgumentException("invalid bounded probe dimensions");
        }

        Owners owners = new Owners();
        Fixture fixture = new Fixture(owners);
        verifyBoundaryCases(owners);
        Counters counters = new Counters();
        System.out.println("# label=" + label + " java=" + System.getProperty("java.runtime.version")
                + " vm=" + System.getProperty("java.vm.name")
                + " actualM3=true units=" + UNITS + " piece=" + PIECE
                + " operations=" + operations + " warmupOperations=" + warmupOperations
                + " samples=" + samples + " allocatedBytesSupported=" + counters.allocation
                + " threadCpuSupported=" + counters.cpu);
        System.out.println("label\tworkload\tsample\toperations\tallocatedBytes\tcpuNanoseconds\tchecksum");

        measure(label, "control-loop", counters, operations, warmupOperations, samples,
                M3StringFirstUnitResourceProbe::control, 1);
        measure(label, "equals-early-tuple-scalar", counters, operations, warmupOperations, samples,
                count -> equal(fixture.tupleLeft, fixture.scalarRight, count), 0);
        measure(label, "equals-early-scalar-tuple", counters, operations, warmupOperations, samples,
                count -> equal(fixture.scalarLeft, fixture.tupleRight, count), 0);
        measure(label, "equals-early-tuple-tuple", counters, operations, warmupOperations, samples,
                count -> equal(fixture.tupleLeft, fixture.tupleRight, count), 0);
        measure(label, "compare-early-tuple-scalar", counters, operations, warmupOperations, samples,
                count -> compare(fixture.tupleLeft, fixture.scalarRight, count), -1);
        measure(label, "compare-early-scalar-tuple", counters, operations, warmupOperations, samples,
                count -> compare(fixture.scalarLeft, fixture.tupleRight, count), -1);
        measure(label, "compare-early-tuple-tuple", counters, operations, warmupOperations, samples,
                count -> compare(fixture.tupleLeft, fixture.tupleRight, count), -1);
        measure(label, "equals-early-scalar-scalar", counters, operations, warmupOperations, samples,
                count -> equal(fixture.scalarLeft, fixture.scalarRight, count), 0);
        measure(label, "compare-early-scalar-scalar", counters, operations, warmupOperations, samples,
                count -> compare(fixture.scalarLeft, fixture.scalarRight, count), -1);
        measure(label, "equals-one-tuple-scalar", counters, operations, warmupOperations, samples,
                count -> equal(fixture.oneLeft, fixture.oneScalar, count), 1);
        measure(label, "equals-one-tuple-tuple", counters, operations, warmupOperations, samples,
                count -> equal(fixture.oneLeft, fixture.oneRight, count), 1);
        measure(label, "compare-one-tuple-scalar", counters, operations, warmupOperations, samples,
                count -> compare(fixture.oneLeft, fixture.oneScalar, count), 0);
        measure(label, "compare-one-tuple-tuple", counters, operations, warmupOperations, samples,
                count -> compare(fixture.oneLeft, fixture.oneRight, count), 0);
        measure(label, "compare-one-prefix-tuple-scalar", counters, operations, warmupOperations, samples,
                count -> compare(fixture.oneLeft, fixture.scalarLeft, count), 1 - UNITS);
        Reference.reachabilityFence(fixture);
        System.out.println("M3_STRING_FIRST_UNIT_RESOURCE_PROBE_PASS checks=" + checks + " sink=" + sink);
    }

    private static final class Fixture {
        final String tupleLeft;
        final String tupleRight;
        final String scalarLeft;
        final String scalarRight;
        final String oneLeft;
        final String oneRight;
        final String oneScalar;

        Fixture(Owners owners) throws Exception {
            char[] left = collisionUnits(0);
            char[] right = collisionUnits(1);
            tupleLeft = segmented(left, PIECE);
            tupleRight = segmented(right, PIECE);
            scalarLeft = new String(left.clone());
            scalarRight = new String(right.clone());
            oneLeft = tupleLeft.substring(0, 1);
            oneRight = tupleRight.substring(2, 3);
            oneScalar = new String(new char[] {'A'});
            owners.requireTuple(tupleLeft);
            owners.requireTuple(tupleRight);
            owners.requireTuple(oneLeft);
            owners.requireTuple(oneRight);
            owners.requireScalar(scalarLeft);
            owners.requireScalar(scalarRight);
            owners.requireScalar(oneScalar);
            check(owners.owner(tupleLeft) != owners.owner(tupleRight), "unequal tuple owners");
            check(owners.owner(tupleLeft) != owners.owner(scalarLeft), "different equal-text shapes");
            check(tupleLeft.hashCode() == hash(left) && scalarLeft.hashCode() == hash(left),
                    "left exact Java hash");
            check(tupleRight.hashCode() == hash(right) && scalarRight.hashCode() == hash(right),
                    "right exact Java hash");
            check(hash(left) == hash(right), "hash collision bypasses hash rejection");
            check(tupleLeft.charAt(0) == 'A' && tupleRight.charAt(0) == 'B', "first-unit mismatch");
            check(tupleLeft.equals(scalarLeft) && tupleRight.equals(scalarRight), "exact equal fixtures");
            check(oneLeft.equals(oneRight) && oneLeft.equals(oneScalar), "distinct equal one-unit views");
        }
    }

    private static char[] collisionUnits(int key) {
        char[] units = new char[UNITS];
        Arrays.fill(units, 'x');
        for (int bit = 0; bit < 6; bit++) {
            units[bit * 2] = (key & (1 << bit)) == 0 ? 'A' : 'B';
            units[bit * 2 + 1] = (key & (1 << bit)) == 0 ? 'a' : 'B';
        }
        return units;
    }

    private static String segmented(char[] units, int piece) {
        String[] parts = new String[(units.length + piece - 1) / piece];
        for (int start = 0; start < units.length; start += piece) {
            int count = Math.min(piece, units.length - start);
            char[] padded = new char[count + 2];
            padded[0] = '!';
            padded[count + 1] = '?';
            System.arraycopy(units, start, padded, 1, count);
            parts[start / piece] = new String(padded).substring(1, count + 1);
        }
        return String.join("", parts);
    }

    private static void verifyBoundaryCases(Owners owners) throws Exception {
        char[] units = {'A', 'A', '\u0000', '\uD800', '\uD801', '\uDC00', '\uDC01', '\uFFFF'};
        String tuple = segmented(units, 1);
        owners.requireTuple(tuple);
        for (int start = 0; start < units.length; start++) {
            String one = tuple.substring(start, start + 1);
            owners.requireTuple(one);
            check(one.compareTo("") == 1 && "".compareTo(one) == -1, "empty-prefix order");
            check(!one.equals("") && !"".equals(one), "empty-prefix equality");
            for (int other = 0; other < units.length; other++) {
                String otherOne = tuple.substring(other, other + 1);
                String scalar = new String(new char[] {units[other]});
                int expected = units[start] - units[other];
                check(one.compareTo(otherOne) == expected, "one-unit tuple UTF-16 order");
                check(one.compareTo(scalar) == expected, "one-unit mixed UTF-16 order");
                check(scalar.compareTo(one) == -expected, "reversed one-unit UTF-16 order");
                check(one.equals(otherOne) == (expected == 0), "one-unit tuple equality");
                check(one.equals(scalar) == (expected == 0), "one-unit mixed equality");
                check(scalar.equals(one) == (expected == 0), "reversed one-unit equality");
            }
            if (start + 1 < units.length) {
                String two = tuple.substring(start, start + 2);
                String same = new String(new char[] {units[start], units[start + 1]});
                char changed = (char) (units[start + 1] ^ 1);
                String different = new String(new char[] {units[start], changed});
                check(two.equals(same) && same.equals(two), "index-one equal continuation");
                check(two.compareTo(same) == 0, "index-one equal order");
                check(!two.equals(different) && !different.equals(two), "index-one mismatch");
                check(two.compareTo(different) == units[start + 1] - changed,
                        "index-one exact order across a scalar seam");
                check(one.compareTo(same) == -1 && same.compareTo(one) == 1,
                        "one-unit common prefix length difference");
            }
        }
        Reference.reachabilityFence(tuple);
    }

    private static final class Owners {
        private final Field storage;
        private final Field owner;
        private final Class<?> tuple;

        Owners() throws Exception {
            storage = String.class.getDeclaredField("m3");
            storage.setAccessible(true);
            owner = Class.forName("java.lang.M3String").getDeclaredField("owner");
            owner.setAccessible(true);
            tuple = Class.forName("java.lang.M3StringTuple");
        }

        Object owner(String value) throws IllegalAccessException {
            Object backing = storage.get(value);
            if (backing == null) throw new AssertionError("actual M3 admission required");
            return owner.get(backing);
        }

        void requireTuple(String value) throws IllegalAccessException {
            check(tuple.isInstance(owner(value)), "tuple owner required");
        }

        void requireScalar(String value) throws IllegalAccessException {
            check(!tuple.isInstance(owner(value)), "scalar owner required");
        }
    }

    private static final class Counters {
        private final java.lang.management.ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        private final com.sun.management.ThreadMXBean allocationBean;
        private final long threadId = Thread.currentThread().threadId();
        final boolean allocation;
        final boolean cpu;

        Counters() {
            allocationBean = threadBean instanceof com.sun.management.ThreadMXBean bean ? bean : null;
            allocation = allocationBean != null && allocationBean.isThreadAllocatedMemorySupported();
            if (allocation && !allocationBean.isThreadAllocatedMemoryEnabled()) {
                allocationBean.setThreadAllocatedMemoryEnabled(true);
            }
            cpu = threadBean.isCurrentThreadCpuTimeSupported();
            if (cpu && !threadBean.isThreadCpuTimeEnabled()) threadBean.setThreadCpuTimeEnabled(true);
            // Initialize the counter accessors outside every measured interval.
            for (int index = 0; index < 16; index++) {
                allocatedBytes();
                cpuNanoseconds();
            }
        }

        long allocatedBytes() {
            return allocation ? allocationBean.getThreadAllocatedBytes(threadId) : -1;
        }

        long cpuNanoseconds() {
            return cpu ? threadBean.getCurrentThreadCpuTime() : -1;
        }
    }

    @FunctionalInterface
    private interface Work {
        long run(int operations);
    }

    private static void measure(String label, String name, Counters counters, int operations,
            int warmupOperations, int samples, Work work, long expectedPerOperation) {
        sink = work.run(warmupOperations);
        check(sink == expectedPerOperation * warmupOperations, "warmup checksum");
        for (int sample = 0; sample < samples; sample++) {
            long startCpu = counters.cpuNanoseconds();
            long startBytes = counters.allocatedBytes();
            long result = work.run(operations);
            long endBytes = counters.allocatedBytes();
            long endCpu = counters.cpuNanoseconds();
            long allocated = counters.allocation ? endBytes - startBytes : -1;
            long cpu = counters.cpu ? endCpu - startCpu : -1;
            sink = result;
            check(result == expectedPerOperation * operations, "sample checksum");
            check(!counters.allocation || (startBytes >= 0 && allocated >= 0), "allocation counter validity");
            check(!counters.cpu || (startCpu >= 0 && cpu >= 0), "CPU counter validity");
            System.out.println(label + "\t" + name + "\t" + sample + "\t" + operations
                    + "\t" + allocated + "\t" + cpu + "\t" + result);
        }
    }

    private static long control(int operations) {
        long result = 0;
        for (int index = 0; index < operations; index++) result++;
        return result;
    }

    private static long equal(String left, String right, int operations) {
        long result = 0;
        for (int index = 0; index < operations; index++) result += left.equals(right) ? 1 : 0;
        return result;
    }

    private static long compare(String left, String right, int operations) {
        long result = 0;
        for (int index = 0; index < operations; index++) result += left.compareTo(right);
        return result;
    }

    private static int hash(char[] units) {
        int result = 0;
        for (char unit : units) result = 31 * result + unit;
        return result;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
