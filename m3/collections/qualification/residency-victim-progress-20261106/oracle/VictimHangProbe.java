// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Hang probe of qualification/residency-victim-progress-20261106 (HANG.txt): the public API drives a nested admission from
// Codec.encode on a worker thread; on a 2000 ms timeout reflection only READS the private lanes (no monitor) and jstack
// dumps the worker. usage: java -cp OUT:CLASSES:M3_ARRAYS_JAR VictimHangProbe unrelated|borrowed|same JSTACK_OUT
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import jdk.m3.collections.M3ObjectResidency;

public final class VictimHangProbe {
    static M3ObjectResidency<String> arena;
    static final List<String> events = Collections.synchronizedList(new ArrayList<>());
    static volatile boolean nestedDone;

    public static void main(String[] args) throws Exception {
        String scenario = args.length > 0 ? args[0] : "unrelated";
        Map<Integer, byte[]> secondary = new HashMap<>();
        int slots = 4;
        var codec = new M3ObjectResidency.Codec<String>() {
            boolean fired;
            public byte[] encode(String v) {
                int slot = v.charAt(0) - 'a';
                events.add("encode " + slot);
                if (!fired) {
                    fired = true;
                    // nested public-API request from inside the eviction callback
                    try { arena.use(scenario.equals("same") ? 2 : 3, 8, String::length); events.add("nested ok"); }
                    catch (RuntimeException e) { events.add("nested " + e.getClass().getSimpleName()); }
                    nestedDone = true;
                }
                return v.getBytes(StandardCharsets.UTF_8);
            }
            public String decode(int slot, byte[] b) { events.add("decode " + slot); return new String(b, StandardCharsets.UTF_8); }
        };
        var store = new M3ObjectResidency.SpillStore() {
            public void write(int slot, byte[] b) { events.add("write " + slot); secondary.put(slot, b.clone()); }
            public byte[] read(int slot) { events.add("read " + slot); return secondary.get(slot).clone(); }
            public void delete(int slot) { events.add("delete " + slot); secondary.remove(slot); }
        };
        int[] creates = new int[slots];
        arena = new M3ObjectResidency<>(slots, M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.LRU
                | M3ObjectResidency.SWAP_SECONDARY, 16, 0,
                slot -> { events.add("create " + slot); return (char) ('a' + slot) + "-" + ++creates[slot]; }, codec, store);
        arena.use(0, 8, String::length);
        arena.use(1, 8, String::length);
        if (scenario.equals("borrowed")) arena.retainProjection(1, 8);
        events.clear();
        Thread worker = new Thread(() -> {
            try { arena.use(2, 8, String::length); events.add("outer ok"); }
            catch (Throwable t) { events.add("outer " + t); }
        }, "residency-worker");
        worker.setDaemon(true);
        worker.start();
        worker.join(2000);
        if (!worker.isAlive()) {
            System.out.println("RESULT=COMPLETED scenario=" + scenario + " events=" + events + " creates=" + Arrays.toString(creates)
                    + " residentBytes=" + arena.residentBytes() + " states=" + states());
            return;
        }
        long pid = ProcessHandle.current().pid();
        System.out.println("RESULT=TIMEOUT_2000MS scenario=" + scenario + " pid=" + pid + " events=" + events);
        for (int sample = 0; sample < 3; sample++) {
            System.out.println("SAMPLE " + sample + " t=" + System.nanoTime() + " " + snapshot() + " threadState=" + worker.getState());
            Thread.sleep(500);
        }
        new ProcessBuilder(System.getProperty("java.home") + "/bin/jstack", Long.toString(pid))
                .redirectOutput(new java.io.File(args[1])).redirectErrorStream(true).start().waitFor();
        Thread.sleep(500);
        new ProcessBuilder(System.getProperty("java.home") + "/bin/jstack", Long.toString(pid))
                .redirectOutput(new java.io.File(args[1] + ".2")).redirectErrorStream(true).start().waitFor();
        System.out.println("SAMPLE 3 " + snapshot() + " cpuMillis=" + java.lang.management.ManagementFactory.getThreadMXBean().getThreadCpuTime(worker.threadId()) / 1_000_000);
        Thread.sleep(1000);
        System.out.println("SAMPLE 4 " + snapshot() + " cpuMillis=" + java.lang.management.ManagementFactory.getThreadMXBean().getThreadCpuTime(worker.threadId()) / 1_000_000);
        System.exit(3);
    }

    static Object field(String name) throws Exception {
        Field f = M3ObjectResidency.class.getDeclaredField(name);
        f.setAccessible(true);
        Object v = f.get(arena);
        if (v instanceof byte[] b) return Arrays.toString(b);
        if (v instanceof long[] l) return Arrays.toString(l);
        if (v instanceof int[] i) return Arrays.toString(i);
        if (v instanceof Object[] o) return Arrays.toString(o);
        return v;
    }
    static String snapshot() throws Exception {
        return "residentBytes=" + field("residentBytes") + " states=" + field("states") + " resident=" + field("resident")
                + " lruFirst=" + field("lruFirstEncoded") + " lruNext=" + field("lruNextEncoded") + " active=" + field("active")
                + " borrowers=" + field("borrowers") + " tick=" + field("tick") + " events=" + events.size();
    }
    static String states() { StringBuilder s = new StringBuilder(); for (int i = 0; i < arena.slots(); i++) s.append(arena.state(i)).append(' '); return s.toString(); }
}
