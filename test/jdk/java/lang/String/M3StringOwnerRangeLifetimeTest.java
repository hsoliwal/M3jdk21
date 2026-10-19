/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 *
 * @test
 * @summary M3 String ranges retain owners without a second Java spelling payload and local native atoms are weakly reclaimable
 * @library /test/lib
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringOwnerRangeLifetimeTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringOwnerRangeLifetimeTest
 * @run main/othervm/timeout=90 --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xmx128m -Xint M3StringOwnerRangeLifetimeTest
 * @run main/othervm/timeout=90 --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings -Xmx128m -Xint M3StringOwnerRangeLifetimeTest
 * @run main/othervm/timeout=90 --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xmx128m -XX:TieredStopAtLevel=1 -XX:CompileThreshold=100 -Xbatch M3StringOwnerRangeLifetimeTest
 * @run main/othervm/timeout=90 --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings -Xmx128m -XX:TieredStopAtLevel=1 -XX:CompileThreshold=100 -Xbatch M3StringOwnerRangeLifetimeTest
 * @run main/othervm/timeout=90 --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xmx128m -XX:-TieredCompilation -XX:CompileThreshold=100 -Xbatch M3StringOwnerRangeLifetimeTest
 * @run main/othervm/timeout=90 --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings -Xmx128m -XX:-TieredCompilation -XX:CompileThreshold=100 -Xbatch M3StringOwnerRangeLifetimeTest
 */

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jdk.test.lib.Utils;

public class M3StringOwnerRangeLifetimeTest {
    private static final Field STRING_M3;
    private static final Field M3_OWNER;
    private static final Field M3_VALUE;
    private static final Field TUPLE_LEFT;
    private static final Field TUPLE_RIGHT;
    private static final Method LOCAL_NATIVE_BYTES;
    private static final Method EXPUNGE_LOCALS;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> m3 = Class.forName("java.lang.M3String");
            M3_OWNER = m3.getDeclaredField("owner");
            M3_OWNER.setAccessible(true);
            M3_VALUE = m3.getDeclaredField("value");
            M3_VALUE.setAccessible(true);
            Class<?> tuple = Class.forName("java.lang.M3StringTuple");
            TUPLE_LEFT = tuple.getDeclaredField("left");
            TUPLE_LEFT.setAccessible(true);
            TUPLE_RIGHT = tuple.getDeclaredField("right");
            TUPLE_RIGHT.setAccessible(true);
            Class<?> pool = Class.forName("java.lang.M3StringPool");
            LOCAL_NATIVE_BYTES = pool.getDeclaredMethod("localNativeBytes");
            LOCAL_NATIVE_BYTES.setAccessible(true);
            EXPUNGE_LOCALS = pool.getDeclaredMethod("expungeLocals");
            EXPUNGE_LOCALS.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        structureHasNoSecondSpellingPayload();
        slicesRetainOneOwner();
        tupleRetainsChildCoordinates();
        localNativeAtomsAreWeaklyReclaimable();
        lastUseNativeReadsSurviveConcurrentReclamation();
        System.out.println("M3_STRING_OWNER_RANGE_LIFETIME_PASS|checks=" + checks);
    }

    private static void structureHasNoSecondSpellingPayload() throws Exception {
        Class<?> m3 = Class.forName("java.lang.M3String");
        List<Field> m3Fields = instanceFields(m3);
        check(m3Fields.size() == 2, "M3String must have exactly owner+coordinate instance fields");
        check(m3Fields.stream().anyMatch(field -> field.getName().equals("owner")),
                "M3String owner field");
        check(m3Fields.stream().anyMatch(field -> field.getName().equals("value")
                        && field.getType() == long.class),
                "M3String packed coordinate field");
        for (String className : List.of(
                "java.lang.M3String",
                "java.lang.M3StringOwner",
                "java.lang.M3StringAtom",
                "java.lang.M3StringTuple",
                "java.lang.M3StringFacts")) {
            for (Field field : instanceFields(Class.forName(className))) {
                check(!field.getType().isArray(),
                        className + " must not retain array payload field " + field.getName());
            }
        }
    }

    private static void slicesRetainOneOwner() throws Exception {
        String whole = new String("0123456789-\u0100-\uD83D\uDE42-tail".toCharArray());
        String middle = whole.substring(3, whole.length() - 4);
        String nested = middle.substring(2, middle.length() - 2);
        Object wholeM3 = requireM3(whole);
        Object middleM3 = requireM3(middle);
        Object nestedM3 = requireM3(nested);
        Object owner = M3_OWNER.get(wholeM3);
        same(owner, M3_OWNER.get(middleM3), "substring owner");
        same(owner, M3_OWNER.get(nestedM3), "nested substring owner");
        check(M3_VALUE.getLong(wholeM3) != M3_VALUE.getLong(middleM3),
                "substring changes only packed coordinate");
        check(M3_VALUE.getLong(middleM3) != M3_VALUE.getLong(nestedM3),
                "nested substring changes only packed coordinate");
        check(middle.equals("3456789-\u0100-\uD83D\uDE42-"), "middle content");
        check(nested.equals("56789-\u0100-\uD83D"), "nested content");
    }

    private static void tupleRetainsChildCoordinates() throws Exception {
        String left = new String("left-\u0100".toCharArray());
        String right = new String("\uD83D\uDE42-right".toCharArray());
        Object leftM3 = requireM3(left);
        Object rightM3 = requireM3(right);
        Object leftOwner = M3_OWNER.get(leftM3);
        Object rightOwner = M3_OWNER.get(rightM3);
        String joined = left.concat(right);
        Object joinedM3 = requireM3(joined);
        Object tupleOwner = M3_OWNER.get(joinedM3);
        check(tupleOwner.getClass().getName().equals("java.lang.M3StringTuple"),
                "concat must retain tuple owner");
        Object tupleLeft = TUPLE_LEFT.get(tupleOwner);
        Object tupleRight = TUPLE_RIGHT.get(tupleOwner);
        same(leftOwner, M3_OWNER.get(tupleLeft), "tuple left retains canonical owner");
        same(rightOwner, M3_OWNER.get(tupleRight), "tuple right retains canonical owner");
        WeakReference<Object> leftOwnerRef = new WeakReference<>(leftOwner);
        WeakReference<Object> rightOwnerRef = new WeakReference<>(rightOwner);
        left = null;
        right = null;
        leftM3 = null;
        rightM3 = null;
        leftOwner = null;
        rightOwner = null;
        forceGcCycles(3);
        check(leftOwnerRef.get() != null, "live tuple must strongly retain left owner");
        check(rightOwnerRef.get() != null, "live tuple must strongly retain right owner");
        check(joined.equals("left-\u0100\uD83D\uDE42-right"),
                "tuple content survives source-wrapper collection pressure");
    }

    private static void localNativeAtomsAreWeaklyReclaimable() throws Exception {
        long before = localNativeBytes();
        List<WeakReference<Object>> owners = allocateEphemeralOwners();
        long peak = localNativeBytes();
        check(peak > before, "ephemeral local atoms must increase native retained-byte accounting");
        int cleared = 0;
        long after = peak;
        for (int attempt = 0; attempt < 40 && (cleared == 0 || after >= peak); attempt++) {
            forceGcCycles(1);
            EXPUNGE_LOCALS.invoke(null);
            cleared = 0;
            for (WeakReference<Object> owner : owners) {
                if (owner.get() == null) cleared++;
            }
            after = localNativeBytes();
        }
        check(cleared > 0, "pool weak references must allow local atom owner collection");
        check(after < peak, "expunge must release at least some local native payload");
    }

    private static List<WeakReference<Object>> allocateEphemeralOwners() throws Exception {
        ArrayList<WeakReference<Object>> owners = new ArrayList<>();
        for (int item = 0; item < 96; item++) {
            char[] units = new char[257 + (item % 17)];
            for (int index = 0; index < units.length; index++) {
                units[index] = (char) (0x0100 + ((item * 131 + index * 17) & 0x3fff));
            }
            units[0] = (char) (0x4000 + item);
            String value = new String(units);
            Object m3 = requireM3(value);
            Object owner = M3_OWNER.get(m3);
            check(owner.getClass().getName().equals("java.lang.M3StringAtom"),
                    "ephemeral scalar owner");
            owners.add(new WeakReference<>(owner));
        }
        return owners;
    }

    private static volatile int lastUsePressureSink;
    private static final int LAST_USE_EPOCHS = 56;
    private static final int LAST_USE_CASES_PER_EPOCH = 64;
    private static final int LAST_USE_WARMUP_ROUNDS = 64;

    /*
     * Regression pressure on the real pool, not a deterministic premature-reclamation
     * oracle. No input String/owner is saved by the hot helpers or by an expected String.
     * Primitive arrays are spelling oracles. Distinct probe/warmup/pressure prefixes
     * avoid canonical aliases retaining a measured owner.
     *
     * Warmup is finite work, not an observation that a selected JIT tier compiled it.
     * Each measured epoch joins one GC-request/admission/expunge sequence with a batch
     * of 64 cases, including the final batch. This does not certify any individual
     * read/GC race. System.gc() is advisory; actual compilation and collector evidence
     * remain independent runtime gates. A passing stress run is regression evidence.
     */
    private static void lastUseNativeReadsSurviveConcurrentReclamation() throws Exception {
        int admissionProbes = verifyLastUseScalarAdmission();
        int warmupCases = warmUpLastUseReads();
        // Retire warmup-only owners before measured pressure; no hot-path reflection.
        System.gc();
        EXPUNGE_LOCALS.invoke(null);
        AtomicBoolean stop = new AtomicBoolean();
        AtomicReference<Throwable> backgroundFailure = new AtomicReference<>();
        AtomicInteger completedPressureEpochs = new AtomicInteger();
        Phaser phases = new Phaser(2);
        long measuredDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Utils.adjustTimeout(35_000L));
        Thread pressure = new Thread(() -> {
            try {
                for (int epoch = 0; epoch < LAST_USE_EPOCHS && !stop.get(); epoch++) {
                    awaitLastUsePhase(phases, measuredDeadline);
                    admitLastUsePressure(epoch);
                    System.gc();
                    // Reflection stays on this background path during measured reads.
                    EXPUNGE_LOCALS.invoke(null);
                    completedPressureEpochs.incrementAndGet();
                    awaitLastUsePhase(phases, measuredDeadline);
                }
            } catch (InterruptedException interrupted) {
                if (!stop.get()) backgroundFailure.compareAndSet(null, interrupted);
                Thread.currentThread().interrupt();
            } catch (Throwable failure) {
                backgroundFailure.compareAndSet(null, failure);
            } finally {
                if (stop.get() || backgroundFailure.get() != null) phases.forceTermination();
            }
        }, "M3-native-last-use-pressure");
        pressure.setDaemon(true);
        int cases = 0;
        int completedReaderEpochs = 0;
        boolean measuredComplete = false;
        int[] lengths = {0, 1, 2, 7, 33, 257, 4097};
        try {
            pressure.start();
            for (int epoch = 0; epoch < LAST_USE_EPOCHS; epoch++) {
                rethrowLastUseBackgroundFailure(backgroundFailure);
                awaitLastUsePhase(phases, measuredDeadline);
                for (int offset = 0; offset < LAST_USE_CASES_PER_EPOCH; offset++) {
                    check(System.nanoTime() < measuredDeadline, "last-use measured budget");
                    int ordinal = epoch * LAST_USE_CASES_PER_EPOCH + offset;
                    int round = ordinal / 14;
                    int laneAndLength = ordinal % 14;
                    int length = lengths[laneAndLength / 2];
                    char[] units = lastUseUnits(length, (laneAndLength & 1) != 0, round);
                    exerciseLastUseReads(units, (round & 1) != 0);
                    if (round == 0) exerciseLastUseBounds(units);
                    cases++;
                }
                awaitLastUsePhase(phases, measuredDeadline);
                completedReaderEpochs++;
            }
            measuredComplete = true;
        } catch (Throwable foregroundFailure) {
            Throwable workerFailure = backgroundFailure.get();
            if (workerFailure != null && workerFailure != foregroundFailure) {
                foregroundFailure.addSuppressed(workerFailure);
            }
            throw foregroundFailure;
        } finally {
            stop.set(true);
            if (!measuredComplete) phases.forceTermination();
            pressure.interrupt();
            pressure.join(Utils.adjustTimeout(5000L));
            phases.forceTermination();
            check(!pressure.isAlive(), "owned pressure thread must finish within join budget");
        }
        rethrowLastUseBackgroundFailure(backgroundFailure);
        check(cases == 3584, "all bounded last-use cases completed");
        check(warmupCases == 896, "all finite warmup cases completed");
        check(completedReaderEpochs == LAST_USE_EPOCHS
                        && completedPressureEpochs.get() == LAST_USE_EPOCHS,
                "every reader batch, including the final batch, joins completed pressure");
        System.out.println("M3_STRING_NATIVE_LAST_USE_STRESS_PASS|cases=" + cases
                + "|warmupCases=" + warmupCases
                + "|admissionProbes=" + admissionProbes
                + "|completedReaderEpochs=" + completedReaderEpochs
                + "|completedPressureEpochs=" + completedPressureEpochs.get()
                + "|regressionOnly=true");
    }

    private static void awaitLastUsePhase(Phaser phases, long deadline)
            throws InterruptedException, java.util.concurrent.TimeoutException {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0L) throw new AssertionError("last-use phase deadline");
        int phase = phases.arrive();
        if (phase < 0 || phases.awaitAdvanceInterruptibly(phase, remaining,
                TimeUnit.NANOSECONDS) < 0) {
            throw new AssertionError("last-use phase terminated before completion");
        }
    }

    private static void rethrowLastUseBackgroundFailure(
            AtomicReference<Throwable> backgroundFailure) {
        Throwable failure = backgroundFailure.get();
        if (failure != null) throw new AssertionError("last-use background pressure failed", failure);
    }

    /*
     * Admission is inspected only before warmup/pressure. Require real local native
     * owners for the large foreground sizes and all pressure sizes in both coder
     * lanes. These distinct probe spellings do not pin any measured/pressure owner.
     * This is an eligibility gate, not an unobserved claim about every later call.
     */
    private static int verifyLastUseScalarAdmission() throws Exception {
        Class<?> atom = Class.forName("java.lang.M3StringAtom");
        Field payloadOwner = atom.getDeclaredField("payloadOwner");
        Field address = atom.getDeclaredField("address");
        Field storageWidth = atom.getDeclaredField("storageWidth");
        Field coder = atom.getSuperclass().getDeclaredField("coder");
        Field compactStrings = String.class.getDeclaredField("COMPACT_STRINGS");
        payloadOwner.setAccessible(true);
        address.setAccessible(true);
        storageWidth.setAccessible(true);
        coder.setAccessible(true);
        compactStrings.setAccessible(true);
        boolean compact = compactStrings.getBoolean(null);
        List<WeakReference<Object>> probes = new ArrayList<>();
        for (int length : new int[] {257, 4097}) {
            for (int lane = 0; lane < 2; lane++) {
                char[] units = lastUseUnits(length, lane != 0, 257 + lane);
                units[0] = lane != 0 ? '\u6303' : 'P';
                probes.add(probeLastUseNativeAdmission(units, payloadOwner, address,
                        storageWidth, coder, compact));
            }
        }
        for (int geometry = 0; geometry < 32; geometry++) {
            for (int lane = 0; lane < 2; lane++) {
                char[] units = lastUsePressureUnits(geometry, lane);
                units[0] = lane != 0 ? '\u6303' : 'P';
                probes.add(probeLastUseNativeAdmission(units, payloadOwner, address,
                        storageWidth, coder, compact));
            }
        }
        check(probes.size() == 68, "all large native-admission geometries inspected");
        retireLastUseAdmissionProbes(probes);
        return probes.size();
    }

    private static WeakReference<Object> probeLastUseNativeAdmission(
            char[] units, Field payloadOwner, Field address, Field storageWidth,
            Field coder, boolean compactStrings) throws Exception {
        String value = new String(units);
        Object owner = M3_OWNER.get(requireM3(value));
        check(owner.getClass() == payloadOwner.getDeclaringClass(),
                "large admission probe requires actual scalar Atom");
        check(payloadOwner.get(owner) == null, "large admission probe must be VM-local");
        check(address.getLong(owner) != 0L, "large admission probe requires native address");
        int width = storageWidth.getByte(owner);
        boolean wide = false;
        for (char unit : units) if (unit > 0xff) wide = true;
        int expectedCoder = wide || !compactStrings ? 1 : 0;
        check(coder.getByte(owner) == expectedCoder, "admission probe requested backing coder");
        check(width == expectedCoder + 1, "admission probe requested native unit width");
        WeakReference<Object> result = new WeakReference<>(owner);
        owner = null;
        value = null;
        return result;
    }

    private static void retireLastUseAdmissionProbes(List<WeakReference<Object>> probes)
            throws Exception {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Utils.adjustTimeout(5_000L));
        for (int attempt = 0; attempt < 40 && System.nanoTime() < deadline; attempt++) {
            System.gc();
            EXPUNGE_LOCALS.invoke(null);
            boolean retired = true;
            for (WeakReference<Object> probe : probes) {
                if (!probe.refersTo(null)) retired = false;
            }
            if (retired) {
                EXPUNGE_LOCALS.invoke(null);
                return;
            }
            Thread.sleep(10L);
        }
        throw new AssertionError("distinct admission probe roots must retire before warmup");
    }

    private static int warmUpLastUseReads() {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Utils.adjustTimeout(15_000L));
        int[] lengths = {0, 1, 2, 7, 33, 257, 4097};
        int cases = 0;
        for (int round = 0; round < LAST_USE_WARMUP_ROUNDS; round++) {
            for (int length : lengths) {
                for (int lane = 0; lane < 2; lane++) {
                    check(System.nanoTime() < deadline, "last-use warmup budget");
                    char[] units = lastUseUnits(length, lane != 0, round + 1024);
                    if (length != 0) units[0] = lane != 0 ? '\u6404' : 'W';
                    exerciseLastUseReads(units, (round & 1) != 0);
                    cases++;
                }
            }
        }
        return cases;
    }

    private static char[] lastUsePressureUnits(int epoch, int item) {
        boolean wide = (item & 1) != 0;
        char[] units = new char[2048 + ((epoch + item) & 31)];
        for (int index = 0; index < units.length; index++) {
            int value = epoch * 37 + item * 131 + index;
            units[index] = wide ? (char) (0x0200 + (value & 0x3fff)) : (char) (value & 0xff);
        }
        // Measured F/0x6101, probes P/0x6303 and warmup W/0x6404 are disjoint.
        units[0] = wide ? '\u6202' : 'B';
        units[1] = 'B';
        return units;
    }

    private static void admitLastUsePressure(int epoch) {
        int observed = 0;
        for (int item = 0; item < 8; item++) {
            char[] units = lastUsePressureUnits(epoch, item);
            observed ^= new String(units).charAt(units.length - 1);
        }
        lastUsePressureSink = observed;
    }

    private static char[] lastUseUnits(int length, boolean wide, int round) {
        char[] units = new char[length];
        for (int index = 0; index < units.length; index++) {
            units[index] = wide
                    ? (char) (0x0100 + ((round * 193 + index * 29) & 0x7fff))
                    : (char) ((round * 193 + index * 29) & 0xff);
        }
        if (length != 0) units[0] = wide ? '\u6101' : 'F';
        if (wide && length > 1) units[1] = '\uD83D';
        if (wide && length > 2) units[2] = '\uDE42';
        if (wide && length > 4) units[4] = '\uD800';
        if (wide && length > 5) units[5] = '\uDC00';
        return units;
    }

    private static void exerciseLastUseReads(char[] units, boolean wideBuilder) {
        int length = units.length;
        if (length != 0) {
            check(lastUseCharAt(units, 0) == units[0], "last-use first UTF-16 unit");
            check(lastUseCharAt(units, length / 2) == units[length / 2],
                    "last-use middle UTF-16 unit");
            check(lastUseCharAt(units, length - 1) == units[length - 1],
                    "last-use final UTF-16 unit");
        }
        char[] whole = new char[length + 4];
        java.util.Arrays.fill(whole, '\uFFFF');
        lastUseChars(units, 0, length, whole, 2);
        checkLastUseChars(units, 0, length, whole, 2, "last-use whole getChars");

        int from = length / 3;
        int to = length - length / 4;
        char[] range = new char[to - from + 4];
        java.util.Arrays.fill(range, '\uFFFF');
        lastUseRangeChars(units, from, to, range, 2);
        checkLastUseChars(units, from, to, range, 2, "last-use range getChars");
        if (from < to) {
            check(lastUseRangeCharAt(units, from, to) == units[to - 1],
                    "last-use range charAt");
        }

        byte[] bytes = new byte[length + 4];
        java.util.Arrays.fill(bytes, (byte) 0x5a);
        lastUseBytes(units, 0, length, bytes, 2);
        checkLastUseBytes(units, 0, length, bytes, 2, "last-use truncating getBytes");
        byte[] rangeBytes = new byte[to - from + 4];
        java.util.Arrays.fill(rangeBytes, (byte) 0x5a);
        lastUseBytes(units, from, to, rangeBytes, 2);
        checkLastUseBytes(units, from, to, rangeBytes, 2, "last-use byte range");

        char[] builderCopy = new char[length + 4];
        java.util.Arrays.fill(builderCopy, '\uFFFF');
        lastUseBuilderCopy(units, builderCopy, 2, wideBuilder);
        checkLastUseChars(units, 0, length, builderCopy, 2, "last-use builder copy");

        char[] emptyChars = {'\uFFFF', '\uFFFF', '\uFFFF'};
        lastUseChars(units, length, length, emptyChars, 1);
        checkLastUseChars(units, length, length, emptyChars, 1, "empty char range");
        byte[] emptyBytes = {(byte) 0x5a, (byte) 0x5a, (byte) 0x5a};
        lastUseBytes(units, length, length, emptyBytes, 1);
        checkLastUseBytes(units, length, length, emptyBytes, 1, "empty byte range");
    }

    private static char lastUseCharAt(char[] units, int index) {
        return new String(units).charAt(index);
    }

    private static char lastUseRangeCharAt(char[] units, int from, int to) {
        return new String(units).substring(from, to).charAt(to - from - 1);
    }

    private static void lastUseChars(
            char[] units, int from, int to, char[] destination, int destinationStart) {
        new String(units).getChars(from, to, destination, destinationStart);
    }

    private static void lastUseRangeChars(
            char[] units, int from, int to, char[] destination, int destinationStart) {
        new String(units).substring(from, to).getChars(0, to - from,
                destination, destinationStart);
    }

    @SuppressWarnings("deprecation")
    private static void lastUseBytes(
            char[] units, int from, int to, byte[] destination, int destinationStart) {
        new String(units).getBytes(from, to, destination, destinationStart);
    }

    private static void lastUseBuilderCopy(
            char[] units, char[] destination, int destinationStart, boolean wideDestination) {
        StringBuilder builder = new StringBuilder(units.length + 2);
        builder.append(wideDestination ? '\u0400' : '[');
        builder.append(new String(units));
        builder.append(']');
        builder.getChars(1, units.length + 1, destination, destinationStart);
    }

    private static void checkLastUseChars(
            char[] units, int from, int to, char[] actual, int start, String label) {
        for (int index = 0; index < actual.length; index++) {
            char expected = index >= start && index - start < to - from
                    ? units[from + index - start] : '\uFFFF';
            check(actual[index] == expected, label);
        }
    }

    private static void checkLastUseBytes(
            char[] units, int from, int to, byte[] actual, int start, String label) {
        for (int index = 0; index < actual.length; index++) {
            byte expected = index >= start && index - start < to - from
                    ? (byte) units[from + index - start] : (byte) 0x5a;
            check(actual[index] == expected, label);
        }
    }

    private static void exerciseLastUseBounds(char[] units) {
        expectLastUseBounds(() -> lastUseCharAt(units, -1));
        expectLastUseBounds(() -> lastUseCharAt(units, units.length));
        expectLastUseBounds(() -> lastUseChars(units, -1, units.length,
                new char[units.length + 4], 2));
        expectLastUseBounds(() -> lastUseChars(units, 0, units.length,
                new char[units.length + 4], 5));
        expectLastUseBounds(() -> lastUseBytes(units, -1, units.length,
                new byte[units.length + 4], 2));
        expectLastUseBounds(() -> lastUseBytes(units, 0, units.length,
                new byte[units.length + 4], 5));
    }

    private static void expectLastUseBounds(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("last-use bounds operation must refuse");
        } catch (IndexOutOfBoundsException expected) {
            check(true, "last-use bounds exception preserved");
        }
    }

    private static long localNativeBytes() throws Exception {
        return (long) LOCAL_NATIVE_BYTES.invoke(null);
    }

    private static void forceGcCycles(int cycles) throws InterruptedException {
        for (int cycle = 0; cycle < cycles; cycle++) {
            System.gc();
            byte[][] pressure = new byte[4][];
            for (int i = 0; i < pressure.length; i++) {
                pressure[i] = new byte[1 << 20];
            }
            Thread.sleep(20L);
        }
    }

    private static Object requireM3(String value) throws IllegalAccessException {
        value.length();
        Object m3 = STRING_M3.get(value);
        check(m3 != null, "String must be M3-backed");
        return m3;
    }

    private static List<Field> instanceFields(Class<?> type) {
        ArrayList<Field> fields = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) fields.add(field);
        }
        return fields;
    }

    private static void same(Object expected, Object actual, String label) {
        check(expected == actual, label);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
