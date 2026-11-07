// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

/** Contract oracle, not an independent map/storage owner; baseline source is byte-pinned. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-openrewrite-recipes/crates/m3jdk21-longhashset-single-probe-20261030/inputs/M3LongHashSetSingleProbeTest.java.txt blob 9867e9c15ee6; recipe m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
public final class M3LongHashSetSingleProbeTest {
    private static long checks;
    private static long operations;
    private static final Random RANDOM = new Random(0x51F7F00DL);

    private M3LongHashSetSingleProbeTest() { }

    private static void yes(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError("operation=" + operations + " " + label);
    }

    private static long value() {
        return switch (RANDOM.nextInt(9)) {
            case 0 -> 0L;
            case 1 -> Long.MIN_VALUE;
            case 2 -> Long.MAX_VALUE;
            case 3 -> RANDOM.nextInt(5);
            case 4 -> RANDOM.nextInt(4096) - 2048;
            case 5 -> (long) RANDOM.nextInt(1024) << 32;
            case 6 -> RANDOM.nextInt(17) - 8;
            case 7 -> RANDOM.nextLong();
            default -> Long.rotateLeft(RANDOM.nextLong(), RANDOM.nextInt(64));
        };
    }

    private static Object field(Object value, String name) {
        try {
            Field f = value.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(value);
        } catch (ReflectiveOperationException problem) { throw new AssertionError(problem); }
    }

    private static void snapshot(LongHashSet current, M3LongHashSetBaseline original, Set<Long> oracle) {
        yes(current.size() == oracle.size(), "candidate size");
        yes(original.size() == oracle.size(), "original size");
        yes(current.capacity() == original.capacity(), "exact capacity");
        yes(Arrays.equals(current.toArray(), original.toArray()), "exact physical-order iteration");
        long[] array = current.toArray();
        HashSet<Long> seen = new HashSet<>();
        for (long v : array) yes(seen.add(v), "duplicate in output");
        yes(seen.equals(oracle), "reference HashSet membership");
        yes(Arrays.equals((long[]) field(current,"keys"), (long[]) field(original,"keys")), "key plane parity");
        yes(Arrays.equals((byte[]) field(current,"states"), (byte[]) field(original,"states")), "state plane parity");
        yes(field(current,"occupied").equals(field(original,"occupied")), "occupied parity");
        yes(field(current,"modCount").equals(field(original,"modCount")), "epoch parity");
        int occupied = 0, live = 0;
        for (byte b : (byte[]) field(current,"states")) {
            if (b != 0) occupied++;
            if (b == 1) live++;
        }
        yes(occupied == ((int) field(current,"occupied")), "authoritative occupancy");
        yes(live == current.size(), "live slots");
        LongIterator ia=current.iterator(), ib=original.iterator();
        for (long v : array) {
            yes(ia.hasNext() && ib.hasNext(), "iterator hasNext");
            yes(ia.nextLong()==v && ib.nextLong()==v, "iterator order");
        }
        yes(!ia.hasNext() && !ib.hasNext(), "iterator completion");
        try { ia.nextLong(); throw new AssertionError("missing iterator exhaustion"); }
        catch (NoSuchElementException expected) { checks++; }
    }

    private static void exercise(LongHashSet current, M3LongHashSetBaseline original,
            Set<Long> oracle, int kind, long value) {
        operations++;
        switch (kind) {
            case 0,1,2,3,4 -> {
                boolean expected = oracle.add(value);
                yes(current.add(value) == expected, "add candidate");
                yes(original.add(value) == expected, "add original");
            }
            case 5,6,7 -> {
                boolean expected = oracle.remove(value);
                yes(current.remove(value) == expected, "remove candidate");
                yes(original.remove(value) == expected, "remove original");
            }
            case 8,9 -> {
                yes(current.contains(value) == oracle.contains(value), "contains candidate");
                yes(original.contains(value) == oracle.contains(value), "contains original");
            }
            case 10 -> {
                current.clear();original.clear();oracle.clear();
            }
            default -> throw new AssertionError("action out of range");
        }
    }

    private static void iteratorEpochProof() {
        LongHashSet subject = new LongHashSet();
        M3LongHashSetBaseline baseline = new M3LongHashSetBaseline();
        subject.add(9);baseline.add(9);
        LongIterator ci=subject.iterator(), bi=baseline.iterator();
        yes(!subject.add(9) && !baseline.add(9), "duplicate is no-op");
        yes(ci.hasNext() && bi.hasNext(), "duplicate preserves iterators");
        subject.add(10); baseline.add(10);
        try { ci.hasNext(); throw new AssertionError("candidate iterator must failfast"); }
        catch (ConcurrentModificationException expected) {checks++;}
        try { bi.hasNext(); throw new AssertionError("baseline iterator must failfast"); }
        catch (ConcurrentModificationException expected) {checks++;}
    }

    public static void main(String[] args) {
        LongHashSet current = new LongHashSet();
        M3LongHashSetBaseline original = new M3LongHashSetBaseline();
        Set<Long> oracle = new HashSet<>();
        // Growth, probe collisions and tombstone pressure at larger stable occupancies.
        for (int i=0;i<17000;i++) exercise(current,original,oracle,0,(long)i*0x9e3779b97f4a7c15L);
        snapshot(current,original,oracle);
        for (int i=0;i<17000;i+=2) exercise(current,original,oracle,5,(long)i*0x9e3779b97f4a7c15L);
        snapshot(current,original,oracle);
        for (int i=0;i<9000;i++) exercise(current,original,oracle,0,(long)i*0x9e3779b97f4a7c15L);
        snapshot(current,original,oracle);
        for(int i=0;i<250000;i++) {
            int action = RANDOM.nextInt(1000) == 0 ? 10 : RANDOM.nextInt(10);
            exercise(current,original,oracle,action,value());
            if ((i & 8191) == 0) snapshot(current,original,oracle);
        }
        snapshot(current,original,oracle);
        iteratorEpochProof();
        try {new LongHashSet(-1);throw new AssertionError("negative cap accepted");}
        catch(IllegalArgumentException expected){checks++;}
        System.out.println("PASS operations="+operations+" checks="+checks+" finalSize="+current.size());
    }
}
