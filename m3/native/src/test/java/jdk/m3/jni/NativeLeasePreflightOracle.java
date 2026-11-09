// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

/** Executable native/Java parity oracle. Baseline mode NEVER invokes unsafe direct malformed inputs. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/test/java/com/synexia/primitives/NativeLeasePreflightOracle.java blob 7aab35439eff; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
public final class NativeLeasePreflightOracle {
    private static long checks;
    private static MessageDigest digest;
    private static Method direct;
    private NativeLeasePreflightOracle() { }

    private static void verify(boolean pass, String label) {
        checks++;
        if (!pass) throw new AssertionError(label);
    }

    private static void record(String line) {
        digest.update(line.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) '\n');
    }

    private static int direct(long[] ids, int[] leases, int[] pins,
            byte[] states, int count, long[] output) throws Exception {
        return ((Integer)direct.invoke(null, ids, leases, pins, states, count, output)).intValue();
    }

    private static void validRandom() throws Exception {
        Random random = new Random(0x4d33534c45415345L);
        for (int round = 0; round < 650; round++) {
            int n = 1 + random.nextInt(700);
            long[] ids = new long[n + (round % 4)];
            int[] leases = new int[n + (round % 7)];
            int[] pins = new int[n + (round % 5)];
            byte[] states = new byte[n + (round % 3)];
            for (int i = 0; i < n; i++) {
                ids[i] = i + 1L + (round * 1000L);
                leases[i] = random.nextInt(4);
                pins[i] = random.nextInt(4);
                states[i] = (byte)random.nextInt(3);
            }
            long[] javaOut = new long[n + (round % 2)];
            long[] nativeOut = javaOut.clone();
            Arrays.fill(javaOut, Long.MIN_VALUE);
            Arrays.fill(nativeOut, Long.MIN_VALUE);
            long[] beforeIds = ids.clone();
            int[] beforeLeases = leases.clone();
            int[] beforePins = pins.clone();
            byte[] beforeStates = states.clone();
            int expected = NativeLeaseScan.collect(ids, leases, pins, states, n, javaOut, false);
            int actual = NativeLeaseScan.collect(ids, leases, pins, states, n, nativeOut, true);
            verify(expected == actual, "count parity " + round);
            verify(Arrays.equals(javaOut, nativeOut), "output and suffix parity " + round);
            verify(Arrays.equals(ids, beforeIds), "ids unchanged " + round);
            verify(Arrays.equals(leases, beforeLeases), "leases unchanged " + round);
            verify(Arrays.equals(pins, beforePins), "pins unchanged " + round);
            verify(Arrays.equals(states, beforeStates), "states unchanged " + round);
            record("round=" + round + " count=" + actual + " output=" + Arrays.hashCode(nativeOut));
            if ((round & 31) == 0) {
                int directWritten = direct(ids, leases, pins, states, n, new long[n]);
                verify(directWritten == expected, "direct valid parity " + round);
            }
            if ((round & 15) == 0) {
                int first = n >>> 2;
                long[] limitedJava = {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
                long[] limitedNative = limitedJava.clone();
                long j = NativeLeaseScan.collectWindow(ids, leases, pins, states,
                        n, first, limitedJava, false);
                long k = NativeLeaseScan.collectWindow(ids, leases, pins, states,
                        n, first, limitedNative, true);
                verify(j == k, "window cursor parity " + round);
                verify(Arrays.equals(limitedJava, limitedNative), "window output parity " + round);
                record("window=" + j + " output=" + Arrays.hashCode(limitedNative));
            }
        }
    }

    private static void expectRefusal(long[] ids, int[] leases, int[] pins,
            byte[] states, int count, long[] output, String label) throws Exception {
        long[] beforeOut = output == null ? null : output.clone();
        long[] beforeIds = ids == null ? null : ids.clone();
        verify(direct(ids, leases, pins, states, count, output) == -1, label + " refused");
        verify(output == null || Arrays.equals(output, beforeOut), label + " output unchanged");
        verify(ids == null || Arrays.equals(ids, beforeIds), label + " source unchanged");
        record("REFUSED " + label);
    }

    private static void malformedDirectCandidateOnly() throws Exception {
        long[] ids = {11, 12, 13, 14, 15};
        int[] leases = {0, 0, 0, 0, 0};
        int[] pins = {0, 0, 0, 0, 0};
        byte[] states = {0, 0, 0, 0, 0};
        long[] output = {99, 98, 97, 96, 95};
        expectRefusal(ids, leases, pins, states, -1, output, "negative count");
        expectRefusal(ids, leases, pins, states, Integer.MAX_VALUE, output, "huge count");
        expectRefusal(new long[2], leases, pins, states, 5, output, "short ids");
        expectRefusal(ids, new int[2], pins, states, 5, output, "short leases");
        expectRefusal(ids, leases, new int[2], states, 5, output, "short pins");
        expectRefusal(ids, leases, pins, new byte[2], 5, output, "short states");
        expectRefusal(ids, leases, pins, states, 5, new long[2], "short output");
        expectRefusal(ids, leases, pins, states, 5, ids, "aliased ids/output");
        expectRefusal(null, leases, pins, states, 5, output, "null ids");
        expectRefusal(ids, null, pins, states, 5, output, "null leases");
        expectRefusal(ids, leases, null, states, 5, output, "null pins");
        expectRefusal(ids, leases, pins, null, 5, output, "null states");
        expectRefusal(ids, leases, pins, states, 5, null, "null output");
        verify(direct(new long[0], new int[0], new int[0], new byte[0], 0,
                new long[0]) == 0, "empty direct scan is successful zero");
        verify(NativeLeaseScan.collect(new long[0], new int[0], new int[0],
                new byte[0], 0, new long[0], true) == 0, "empty public native scan");
        record("empty count=0");
    }

    private static void publicValidation() {
        long[] ids = {1, 2, 3};
        int[] counts = {0, 0, 0};
        byte[] states = {0, 1, 2};
        for (boolean preferNative : new boolean[]{false, true}) {
            boolean failed = false;
            try { NativeLeaseScan.collect(ids, counts, counts, states, 4,
                    new long[3], preferNative); }
            catch (IllegalArgumentException expected) { failed = true; }
            verify(failed, "public rejects count overrun " + preferNative);
            failed = false;
            try { NativeLeaseScan.collect(ids, counts, counts, states, 3,
                    ids, preferNative); }
            catch (IllegalArgumentException expected) { failed = true; }
            verify(failed, "public rejects alias " + preferNative);
        }
    }

    private static void tableIntegration() {
        try (ScopedLeaseTable table = new ScopedLeaseTable()) {
            for (int i = 0; i < 180; i++) {
                long id = table.allocate();
                if (i % 6 == 0) table.acquire(id);
                if (i % 7 == 0) table.pin(id);
                if (i % 9 == 0) table.markDirty(id);
            }
            long[] j = new long[table.size()];
            long[] n = new long[table.size()];
            int a = table.collectEligible(j, false);
            int b = table.collectEligible(n, true);
            verify(a == b && Arrays.equals(j, n), "table collect Java/JNI parity");
            record("table count=" + a + " hash=" + Arrays.hashCode(n));
            for (long id = 1; id <= table.size(); id++) {
                if (table.leaseCount(id) != 0) table.release(id);
                if (table.pinCount(id) != 0) table.unpin(id);
                table.markCommitted(id);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !(args[0].equals("baseline") || args[0].equals("candidate"))) {
            throw new IllegalArgumentException("baseline|candidate");
        }
        checks = 0;
        digest = MessageDigest.getInstance("SHA-256");
        verify(NativeLeaseScan.nativeAvailable(), "required native library must load");
        direct = NativeLeaseScan.class.getDeclaredMethod("collectNative",
                long[].class, int[].class, int[].class, byte[].class,
                int.class, long[].class);
        direct.setAccessible(true);
        validRandom();
        publicValidation();
        tableIntegration();
        if (args[0].equals("candidate")) malformedDirectCandidateOnly();
        System.out.println("M3_LEASE_JNI_PREFLIGHT_PASS mode=" + args[0] + " checks=" + checks
                + " sha256=" + HexFormat.of().formatHex(digest.digest()));
    }
}
