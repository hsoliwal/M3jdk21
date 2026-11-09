// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Probe of qualification/residency-donor-semantics-20261106 (STUCK_STATE.txt), public API only: an Error thrown once
// by Codec.encode during an eviction, then an ordinary over-budget admission and a use of the evicted slot, each on a
// worker thread with a 2000 ms timeout. usage: java -cp OUT:CLASSES:M3_ARRAYS_JAR ErrorThenAdmitProbe
import java.nio.charset.StandardCharsets;
import jdk.m3.collections.M3ObjectResidency;

public final class ErrorThenAdmitProbe {
    private static boolean failOnce = true;

    public static void main(String[] args) throws Exception {
        var codec = new M3ObjectResidency.Codec<String>() {
            @Override public byte[] encode(String value) {
                if (failOnce) {
                    failOnce = false;
                    throw new AssertionError("encode error");
                }
                return value.getBytes(StandardCharsets.UTF_8);
            }
            @Override public String decode(int slot, byte[] bytes) { return new String(bytes, StandardCharsets.UTF_8); }
        };
        byte[][] store = new byte[2][];
        var spill = new M3ObjectResidency.SpillStore() {
            @Override public void write(int slot, byte[] bytes) { store[slot] = bytes.clone(); }
            @Override public byte[] read(int slot) { return store[slot].clone(); }
            @Override public void delete(int slot) { store[slot] = null; }
        };
        var arena = new M3ObjectResidency<String>(2, M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.LRU
                | M3ObjectResidency.SWAP_SECONDARY, 8, 0, slot -> "v" + slot, codec, spill);
        arena.use(0, 8, String::length);
        step("use(1) with the Error in encode", () -> arena.use(1, 8, String::length), arena);
        step("use(1) again", () -> arena.use(1, 8, String::length), arena);
        step("use(0)", () -> arena.use(0, 8, String::length), arena);
        System.exit(0);
    }

    private static void step(String label, Runnable call, M3ObjectResidency<String> arena) throws Exception {
        String[] outcome = {"TIMEOUT_2000MS (worker still running)"};
        Thread worker = new Thread(() -> {
            try {
                call.run();
                outcome[0] = "returned";
            } catch (RuntimeException | Error failure) {
                outcome[0] = "threw " + failure.getClass().getSimpleName() + ": " + failure.getMessage();
            }
        });
        worker.setDaemon(true);
        worker.start();
        worker.join(2000);
        boolean stuck = worker.isAlive();
        String states = stuck ? "(arena monitor held by the worker)"
                : "states=" + arena.state(0) + "," + arena.state(1) + " residentBytes=" + arena.residentBytes();
        System.out.println(label + ": " + outcome[0] + " " + states);
        if (stuck) {
            System.out.println("RESULT=LIVELOCK");
            System.exit(3);
        }
    }
}
