/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.m3.collections;

import java.lang.reflect.*;
import java.util.*;

/** Separate, deliberately heap-limited process; invokes the actual private rehash atom. */
public final class M3AllocationFailureProbe {
    private M3AllocationFailureProbe() { }
    static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    public static void main(String[] args) throws Exception {
        if (args[0].equals("remove")) { removeUnderPressure(); return; }
        Object owner = switch (args[0]) {
            case "map" -> new M3LongLongHashMap();
            case "set" -> new M3LongHashSet();
            case "linked" -> new M3LongLinkedHashSet();
            default -> throw new IllegalArgumentException(args[0]);
        };
        if (owner instanceof M3LongLongHashMap map) { map.put(7, 70); map.put(9, 90); }
        if (owner instanceof M3LongHashSet set) { set.add(7); set.add(9); }
        if (owner instanceof M3LongLinkedHashSet set) { set.add(7); set.add(9); }
        Object keys = field(owner, "keys"), states = field(owner, "states");
        Object values = owner instanceof M3LongLongHashMap ? field(owner, "values") : null;
        Object previous = owner instanceof M3LongLinkedHashSet ? field(owner, "previous") : null;
        Object next = owner instanceof M3LongLinkedHashSet ? field(owner, "next") : null;
        Method rehash = owner.getClass().getDeclaredMethod("rehash", int.class); rehash.setAccessible(true);
        // Force a later lane to fail, not a synthetic replacement allocator.
        byte[] pressure = args[0].equals("set") ? new byte[Integer.getInteger("m3.pressure.mib", 12) * 1024 * 1024] : null;
        boolean oom = false;
        try { rehash.invoke(owner, 1 << 21); }
        catch (InvocationTargetException e) { if (!(e.getCause() instanceof OutOfMemoryError)) throw e; oom = true; }
        java.lang.ref.Reference.reachabilityFence(pressure); pressure = null;
        if (!oom) throw new AssertionError("heap limit did not induce OOME");
        if (keys != field(owner, "keys") || states != field(owner, "states")) throw new AssertionError("partially published storage");
        if (values != null && values != field(owner, "values")) throw new AssertionError("changed values");
        if (previous != null && (previous != field(owner, "previous") || next != field(owner, "next"))) throw new AssertionError("changed links");
        if (owner instanceof M3LongLongHashMap map && (map.size() != 2 || map.getOrDefault(7, -1) != 70 || map.getOrDefault(9, -1) != 90)) throw new AssertionError("lost values");
        if (owner instanceof M3LongHashSet set && (set.size() != 2 || !set.contains(7) || !set.contains(9))) throw new AssertionError("lost keys");
        if (owner instanceof M3LongLinkedHashSet set && (set.size() != 2 || !set.contains(7) || !set.contains(9))) throw new AssertionError("lost ordered keys");
        System.out.println(args[0] + ": actual OOME; original storage identity and contents retained");
    }
    static void removeUnderPressure() {
        M3LongBag bag = new M3LongBag(50000);
        for (int i = 0; i < 50000; i++) bag.add(i);
        for (int i = 0; i < 25000; i++) bag.removeAll(i);
        List<byte[]> held = new ArrayList<>();
        try { while (true) held.add(new byte[65536]); } catch (OutOfMemoryError expected) { /* pressure retained */ }
        boolean failed = false; long removed = -1;
        try { removed = bag.removeAll(25000); } catch (OutOfMemoryError error) { failed = true; }
        held.clear();
        if (failed || removed != 1 || bag.size() != 24999 || bag.count(25000) != 0) throw new AssertionError("removal/aggregate split by optional compaction failure");
        long[] total = {0}; bag.forEachCount((key, n) -> total[0] += n);
        if (total[0] != bag.size()) throw new AssertionError("aggregate mismatch");
        System.out.println("remove: completed mutation and aggregate stay consistent under real heap pressure");
    }
}
