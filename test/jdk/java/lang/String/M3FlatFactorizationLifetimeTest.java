/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only
 */
/*
 * @test
 * @summary Verify flat-factor metadata does not retain needle or payload and tolerates discarded entries
 * @requires vm.gc.Serial
 * @modules java.base/java.lang:+open
 * @run main/othervm -XX:+UseSerialGC -XX:-DisableExplicitGC -Xmx256m M3FlatFactorizationLifetimeTest
 * @run main/othervm -XX:+UseSerialGC -XX:-DisableExplicitGC -Xmx256m -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3FlatFactorizationLifetimeTest
 * @run main/othervm -XX:+UseSerialGC -XX:-DisableExplicitGC -Xmx256m -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3FlatFactorizationLifetimeTest
 */

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReferenceArray;

/** Actual collection and exact-search checks for the optional weak metadata. */
public class M3FlatFactorizationLifetimeTest {
    private static final int SLOTS = 64;
    private static final int WITNESSES = 8;
    private static long checks;
    private static ThreadLocal<Boolean> admitting;
    private static Field value, coder, m3;
    private static Method cached, fresh, search;
    private static AtomicReferenceArray<?> entries;
    private static final ReferenceQueue<Object> QUEUE = new ReferenceQueue<>();

    private record Witness(int slot, Reference<?> forward, Reference<?> reverse,
                           WeakReference<String> key, WeakReference<byte[]> payload) {}

    public static void main(String[] args) throws Exception {
        bind();
        // The optional modes are used only by a separate qualification harness which
        // compiles source-level failure injections into a local java.base patch.
        String mode = args.length == 0 ? "normal" : args[0];
        if (mode.equals("normal")) {
            check(entries != null && entries.length() == 2 * SLOTS, "bounded table");
            verifyLifetime();
        } else if (mode.equals("table-unavailable")) {
            check(entries == null, "table-allocation injection not reached");
        } else if (mode.equals("entry-unavailable")) {
            check(entries != null, "entry-allocation test requires a table");
        } else {
            throw new AssertionError("unknown qualification mode");
        }
        verifySearch(mode);
        System.out.println("M3_FLAT_FACTOR_LIFETIME_PASS mode=" + mode + " checks=" + checks);
    }

    private static void bind() throws Exception {
        Class<?> owner = Class.forName("java.lang.M3StringSearchPrecompute");
        Class<?> storage = Class.forName("java.lang.M3String");
        Class<?> lane = Class.forName("java.lang.M3StringBuilderLane");
        Field admission = storage.getDeclaredField("ADMITTING");
        admission.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Boolean> state = (ThreadLocal<Boolean>) admission.get(null);
        admitting = state;
        value = String.class.getDeclaredField("value"); value.setAccessible(true);
        coder = String.class.getDeclaredField("coder"); coder.setAccessible(true);
        m3 = String.class.getDeclaredField("m3"); m3.setAccessible(true);
        cached = owner.getDeclaredMethod("flatFactorization", CharSequence.class,
                byte[].class, byte.class, int.class, boolean.class);
        fresh = owner.getDeclaredMethod("factorization", CharSequence.class,
                byte[].class, byte.class, int.class, boolean.class);
        search = owner.getDeclaredMethod("twoWay", byte[].class, byte.class, int.class,
                lane, CharSequence.class, int.class, boolean.class);
        for (Method method : new Method[] {cached, fresh, search}) method.setAccessible(true);
        Field cache = Class.forName(owner.getName() + "$FlatFactors").getDeclaredField("ENTRIES");
        cache.setAccessible(true);
        entries = (AtomicReferenceArray<?>) cache.get(null);
    }

    private static String flat(char[] units) throws Exception {
        Boolean previous = admitting.get();
        admitting.set(Boolean.TRUE);
        try {
            String text = new String(units);
            check(m3.get(text) == null, "physical flat fixture");
            return text;
        } finally {
            if (previous == null) admitting.remove();
            else admitting.set(previous);
        }
    }

    private static int slot(String text, boolean backwards) {
        return (System.identityHashCode(text) & (SLOTS - 1)) + (backwards ? SLOTS : 0);
    }

    private static long plan(Method method, String text, boolean backwards) throws Exception {
        return (long) method.invoke(null, text, value.get(text), coder.getByte(text),
                text.length(), backwards);
    }

    // Only weak observations and the actual cache-entry objects escape this frame.
    // Holding those entries strongly also tests reclamation if another search evicts a slot.
    private static Witness[] populate() throws Exception {
        Witness[] witnesses = new Witness[WITNESSES];
        boolean[] occupied = new boolean[SLOTS];
        Random random = new Random(0x5745414b46414354L);
        int count = 0;
        for (int attempt = 0; attempt < 2048 && count < witnesses.length; attempt++) {
            char[] units = new char[128 + count];
            for (int i = 0; i < units.length; i++) units[i] = (char) random.nextInt(65536);
            String key = flat(units);
            int index = slot(key, false);
            if (occupied[index]) continue;
            occupied[index] = true;
            check(plan(cached, key, false) == plan(fresh, key, false), "forward plan");
            check(plan(cached, key, true) == plan(fresh, key, true), "reverse plan");
            Reference<?> forward = (Reference<?>) entries.get(index);
            Reference<?> reverse = (Reference<?>) entries.get(index + SLOTS);
            check(forward != null && forward.get() == key, "live forward referent");
            check(reverse != null && reverse.get() == key, "live reverse referent");
            byte[] payload = (byte[]) value.get(key);
            check(payload.length > 0, "physical payload");
            witnesses[count++] = new Witness(index, forward, reverse,
                    new WeakReference<>(key, QUEUE), new WeakReference<>(payload, QUEUE));
            Reference.reachabilityFence(key);
            Reference.reachabilityFence(payload);
        }
        check(count == witnesses.length, "eight distinct occupied slots");
        return witnesses;
    }

    private static void verifyLifetime() throws Exception {
        Witness[] witnesses = populate();
        int collected = 0;
        // Serial GC with explicit GC enabled is selected by the test action.
        // Wait for queue delivery as well as cleared references, not a fixed sleep.
        for (int round = 0; round < 12 && collected < 2 * witnesses.length; round++) {
            System.gc();
            Reference<?> delivered = QUEUE.remove(500);
            while (delivered != null) {
                collected++;
                delivered = QUEUE.poll();
            }
        }
        check(collected == 2 * witnesses.length, "actual GC delivered every key and payload");
        int retainedSlots = 0;
        for (Witness witness : witnesses) {
            check(witness.key().refersTo(null), "needle reclaimed");
            check(witness.payload().refersTo(null), "byte payload reclaimed");
            check(witness.forward().refersTo(null), "forward cache reference cleared by GC");
            check(witness.reverse().refersTo(null), "reverse cache reference cleared by GC");
            if (entries.get(witness.slot()) == witness.forward()) retainedSlots++;
            if (entries.get(witness.slot() + SLOTS) == witness.reverse()) retainedSlots++;
        }
        check(retainedSlots > 0, "GC observed while actual entries remained resident");
        // Revisit a collected slot with a new key, not by clearing any reference manually.
        Random random = new Random(0x52454255494c444cL);
        int rebuilt = 0;
        for (int attempt = 0; attempt < 2048 && rebuilt == 0; attempt++) {
            char[] units = new char[64];
            for (int i = 0; i < units.length; i++) units[i] = (char) random.nextInt(65536);
            String replacement = flat(units);
            int index = slot(replacement, false);
            for (Witness witness : witnesses) {
                if (witness.slot() != index || entries.get(index) != witness.forward()) continue;
                check(plan(cached, replacement, false) == plan(fresh, replacement, false), "reclaimed slot recomputed");
                Reference<?> installed = (Reference<?>) entries.get(index);
                check(installed != witness.forward() && installed.get() == replacement, "new exact weak key");
                rebuilt++;
                Reference.reachabilityFence(replacement);
                break;
            }
        }
        check(rebuilt > 0, "collected resident slot rebuilt");
        System.out.println("GC_LIFETIME_PASS keys=" + witnesses.length + " payloads=" + witnesses.length
                + " queued=" + collected + " residentSlots=" + retainedSlots);
    }

    private static int reference(char[] hay, CharSequence needle, int first, boolean backwards) {
        for (int pos = first; pos <= hay.length - needle.length(); pos++) {
            int i = 0;
            while (i < needle.length() && hay[backwards ? hay.length - 1 - pos - i : pos + i]
                    == needle.charAt(backwards ? needle.length() - 1 - i : i)) i++;
            if (i == needle.length()) return pos;
        }
        return -1;
    }

    private static void verifySearch(String mode) throws Exception {
        Random random = new Random(0x4641494c55524553L);
        for (int trial = 0; trial < 64; trial++) {
            char[] hay = new char[96];
            for (int i = 0; i < hay.length; i++) hay[i] = (char) ((trial % 2 == 0 ? 0x60 : 0xd800) + random.nextInt(4));
            char[] units = Arrays.copyOfRange(hay, trial % 32, trial % 32 + 16);
            if (trial % 3 == 0) units[5] ^= 1;
            String text = flat(hay), needle = flat(units);
            for (boolean backwards : new boolean[] {false, true}) {
                Object previous = entries == null ? null : entries.get(slot(needle, backwards));
                check(plan(cached, needle, backwards) == plan(fresh, needle, backwards), "optional plan parity");
                for (int first : new int[] {0, 1, 31, 80}) {
                    int expected = reference(hay, needle, first, backwards);
                    for (int repeat = 0; repeat < 2; repeat++) {
                        int actual = (int) search.invoke(null, value.get(text), coder.getByte(text),
                                hay.length, null, needle, first, backwards);
                        check(actual == expected, "exact search despite optional metadata state");
                    }
                }
                if (mode.equals("entry-unavailable")) {
                    check(entries.get(slot(needle, backwards)) == previous, "failed entry publication leaves table unchanged");
                }
            }
        }
    }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError("check " + checks + ": " + detail);
    }
}
