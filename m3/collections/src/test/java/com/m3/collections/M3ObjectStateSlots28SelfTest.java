/*
 * Copyright 2026 Hitesh Soliwal and contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Seeded, contract-based mask/offset/generation and two-pointer compaction oracle. */
public final class M3ObjectStateSlots28SelfTest {
    private static long checks;
    private static long operations;
    private M3ObjectStateSlots28SelfTest() { }

    private static void test(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("M3 state check " + checks);
    }
    private static void equal(Object a, Object b) {
        checks++;
        if (!java.util.Objects.equals(a, b)) {
            throw new AssertionError("M3 state check " + checks + ": " + a + " != " + b);
        }
    }
    private static void equal(int[] a, int[] b) {
        checks++;
        if (!Arrays.equals(a, b)) throw new AssertionError("selected positions differ");
    }

    private static void equal(long[] a, long[] b) {
        checks++;
        if (!Arrays.equals(a, b)) {
            throw new AssertionError("array check " + checks + ": " + Arrays.toString(a)
                    + " != " + Arrays.toString(b));
        }
    }
    private static void refuse(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong refusal type: " + type, failure);
        }
        throw new AssertionError("missing refusal: " + type);
    }
    private static boolean matches(int state, int required, int excluded) {
        return (state & required) == required && (state & excluded) == 0;
    }

    private static void lifecycleAndMemory() throws Exception {
        var state = new M3ObjectStateSlots28();
        equal(state.size(), 0);equal(state.statePayloadBytes(), 0L);
        equal(state.allocatedStateBlocks(), 0);
        long first = state.allocate();
        equal(state.state(first), 0);equal(state.allocatedStateBlocks(), 0);
        equal(state.setState(first, 0), 0);
        equal(state.update(first, 0, M3ObjectStateSlots28.DIRTY), 0);
        equal(state.allocatedStateBlocks(), 0);
        equal(state.update(first, M3ObjectStateSlots28.VISIBLE | M3ObjectStateSlots28.DIRTY, 0), 0);
        equal(state.state(first), 3);equal(state.stateBitCount(first), 2);
        equal(state.allocatedStateBlocks(), 1);equal(state.statePayloadBytes(), 4096L);
        equal(state.update(first, 0, M3ObjectStateSlots28.DIRTY), 3);
        equal(state.state(first), 1);
        refuse(IllegalArgumentException.class, () -> state.update(first, 1, 1));
        refuse(IllegalArgumentException.class, () -> state.update(first, -1, 0));
        refuse(IllegalArgumentException.class, () -> state.setState(first, 256));
        equal(state.state(first), 1);
        state.release(first);test(!state.isLive(first));
        refuse(IllegalStateException.class, () -> state.state(first));
        refuse(IllegalStateException.class, () -> state.release(first));
        long next = state.allocate();
        equal(M3SlotAllocator28.slot(first), M3SlotAllocator28.slot(next));
        test(first != next);equal(state.state(next), 0);
        refuse(IllegalStateException.class, () -> state.update(first, 2, 0));
        equal(state.state(next), 0);
        state.release(next);
        long[] handles = new long[4097];
        for (int i = 0; i < handles.length; i++) handles[i] = state.allocate();
        equal(state.highWaterMark(), 4097);equal(state.size(), 4097);
        for (int i : new int[] {0, 4095, 4096}) {
            state.setState(handles[i], 0x80);
        }
        equal(state.allocatedStateBlocks(), 2);equal(state.statePayloadBytes(), 8192L);
        equal(state.countMatching(0, 4097, 0x80, 0), 3);
        equal(state.countMatching(4096, 4097, 0x80, 0), 1);
        equal(state.nextMatchingSlot(0, 0x80, 0), 0);
        equal(state.nextMatchingSlot(1, 0x80, 0), 4095);
        equal(state.nextMatchingSlot(4096, 0x80, 0), 4096);
        equal(state.nextMatchingSlot(4097, 0x80, 0), -1);
        equal(state.nextMatchingSlot(M3Address28.MAX_SLOTS, 0x80, 0), -1);
        equal(state.countMatching(0, M3Address28.MAX_SLOTS, 0, 0), 4097);
        equal(state.countMatching(0, M3Address28.MAX_SLOTS, 1, 1), 0);
        refuse(IndexOutOfBoundsException.class, () -> state.countMatching(-1, 1, 0, 0));
        refuse(IndexOutOfBoundsException.class, () -> state.countMatching(2, 1, 0, 0));
        refuse(IndexOutOfBoundsException.class, () -> state.nextMatchingSlot(M3Address28.MAX_SLOTS + 1, 0, 0));
        for (long handle : handles) state.release(handle);
        equal(state.size(), 0);equal(state.freeSlotCount(), state.highWaterMark());
        equal(state.nextMatchingSlot(0, 0, 0), -1);
        long recycled = state.allocate();equal(state.state(recycled), 0);
        equal(state.allocatedStateBlocks(), 2);state.release(recycled);
        // All state is primitive bytes; no per-slot Object[], Entry or payload array.
        for (Field field : M3ObjectStateSlots28.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            test(field.getType() != Object[].class);
            test(field.getType() != Object.class);
        }
    }

    private static void rangeAndRefusal() {
        var lane = new M3ObjectStateSlots28();
        long a = lane.allocate(1), b = lane.allocate(3), c = lane.allocate(2);
        long[] input = {-1L, a, b, c, b, a, -2L};
        long[] original = input.clone();
        long[] expected = {-1L, a, b, b, a, 0, -2L};
        equal(lane.compactHandles(input, 1, 6, 1, 0), 4);
        equal(input, expected);
        equal(lane.snapshotHandles(original, 1, 6, 1, 0), new long[] {a,b,b,a});
        equal(original, new long[] {-1L,a,b,c,b,a,-2L});
        byte[] states = {(byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
                (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f};
        lane.exportStates(original, 1, 5, states, 2);
        test(Arrays.equals(states, new byte[] {127,127,1,3,2,3,1,127}));
        long[] unchanged = original.clone();
        refuse(NullPointerException.class, () -> lane.compactHandles(null, 0, 0, 0, 0));
        refuse(IndexOutOfBoundsException.class, () -> lane.compactHandles(unchanged, -1, 3, 0, 0));
        refuse(IndexOutOfBoundsException.class, () -> lane.compactHandles(unchanged, 2, 1, 0, 0));
        refuse(IllegalArgumentException.class, () -> lane.compactHandles(unchanged, 1, 6, 256, 0));
        refuse(IllegalArgumentException.class, () -> lane.compactHandles(unchanged, 1, 6, 0, -1));
        long[] bad = {a, b, c};
        lane.release(b);
        long[] badBefore = bad.clone();
        refuse(IllegalStateException.class, () -> lane.compactHandles(bad, 0, 3, 0, 0));
        equal(bad, badBefore);
        refuse(IllegalStateException.class, () -> lane.snapshotHandles(bad, 0, 3, 0, 0));
        byte[] previous = states.clone();
        refuse(IllegalStateException.class, () -> lane.exportStates(bad, 0, 3, states, 1));
        test(Arrays.equals(states, previous));
        equal(unchanged, original);
        // Bit-0 required and excluded simultaneously selects nothing, but still validates.
        long[] x = {a, c};
        equal(lane.compactHandles(x, 0, 2, 1, 1), 0);
        equal(x, new long[] {0, 0});
        refuse(IllegalArgumentException.class, () -> lane.allocate(-1));
        refuse(IllegalArgumentException.class, () -> lane.allocate(256));
        lane.release(a);lane.release(c);
    }

    private static void randomized() {
        long seedBase = 0x4d33415252415953L;
        for (int seed = 0; seed < 48; seed++) {
            var lane = new M3ObjectStateSlots28();
            var oracle = new HashMap<Long, Integer>();
            var active = new ArrayList<Long>();
            Random rng = new Random(seedBase + seed);
            for (int step = 0; step < 2048; step++) {
                int action = rng.nextInt(8);
                if (active.isEmpty() || action < 3 && active.size() < 400) {
                    int flags = rng.nextInt(256);
                    long handle = lane.allocate(flags);
                    test(!oracle.containsKey(handle));
                    oracle.put(handle, flags);active.add(handle);
                } else if (action == 3) {
                    int idx = rng.nextInt(active.size());
                    long removed = active.remove(idx);
                    int slot = M3SlotAllocator28.slot(removed);
                    lane.release(removed);oracle.remove(removed);
                    test(!lane.isLive(removed));
                    if (rng.nextInt(3) == 0) {
                        long recycled = lane.allocate();
                        equal(M3SlotAllocator28.slot(recycled), slot);
                        equal(lane.state(recycled), 0);
                        active.add(recycled);oracle.put(recycled, 0);
                    }
                } else if (action < 7) {
                    long handle = active.get(rng.nextInt(active.size()));
                    int old = oracle.get(handle);
                    int set = rng.nextInt(256);
                    int clear = rng.nextInt(256) & ~set;
                    equal(lane.update(handle, set, clear), old);
                    int next = (old | set) & ~clear & 255;
                    oracle.put(handle, next);equal(lane.state(handle), next);
                } else {
                    long handle = active.get(rng.nextInt(active.size()));
                    int value = rng.nextInt(256);
                    equal(lane.setState(handle, value), oracle.put(handle, value));
                    equal(lane.stateBitCount(handle), Integer.bitCount(value));
                }
                operations++;
                if (step % 79 == 0 || step == 2047) {
                    equal(lane.size(), active.size());
                    for (long handle : active) equal(lane.state(handle), oracle.get(handle));
                    int required = rng.nextInt(256), excluded = rng.nextInt(256);
                    int[] expected = oracle.entrySet().stream()
                            .filter(e -> matches(e.getValue(), required, excluded))
                            .mapToInt(e -> M3SlotAllocator28.slot(e.getKey()))
                            .sorted().toArray();
                    equal(lane.countMatching(0, lane.highWaterMark(), required, excluded), expected.length);
                    int cursor = 0;
                    for (int value : expected) {
                        int next = lane.nextMatchingSlot(cursor, required, excluded);
                        equal(next, value);cursor = value + 1;
                    }
                    equal(lane.nextMatchingSlot(cursor, required, excluded), -1);
                    List<Long> index = new ArrayList<>(active);
                    if (!index.isEmpty()) {
                        index.add(index.get(rng.nextInt(index.size())));
                        Collections.shuffle(index, rng);
                    }
                    long[] inputs = new long[index.size()+4];
                    Arrays.fill(inputs, -17L);
                    for (int i=0;i<index.size();i++) inputs[i+2]=index.get(i);
                    long[] originals = inputs.clone();
                    long[] selected = index.stream()
                            .filter(h -> matches(oracle.get(h), required, excluded))
                            .mapToLong(Long::longValue).toArray();
                    equal(lane.snapshotHandles(inputs, 2, 2+index.size(), required, excluded),selected);
                    equal(inputs,originals);
                    byte[] flags = new byte[index.size()+3];
                    Arrays.fill(flags, (byte)0x7d);
                    lane.exportStates(inputs, 2, index.size(), flags, 1);
                    equal(flags[0]&255,125);equal(flags[index.size()+1]&255,125);
                    for(int i=0;i<index.size();i++)equal(flags[i+1]&255,oracle.get(index.get(i)));
                    equal(lane.compactHandles(inputs, 2, 2+index.size(), required, excluded),selected.length);
                    test(inputs[0] == -17L && inputs[1] == -17L);
                    for(int i=0;i<selected.length;i++)equal(inputs[i+2],selected[i]);
                    for(int i=selected.length+2;i<index.size()+2;i++)equal(inputs[i],0L);
                    test(inputs[inputs.length-2] == -17L && inputs[inputs.length-1] == -17L);
                }
            }
            for (long handle : active) lane.release(handle);
            equal(lane.size(), 0);
        }
    }

    private static void indexedMaskBridge(boolean nativeRun) {
        var states = new M3ObjectStateSlots28();
        long first = states.allocate(1);
        long second = states.allocate(3);
        long third = states.allocate(4);
        long[] handles = {-99L, first, second, third, first, second, -98L};
        long[] before = handles.clone();
        var javaBackend = M3IndexMaskCompactor.Backend.JAVA;
        var nativeBackend = M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
        equal(new int[]{1, 4}, states.matchingPositions(handles, 1, 6, 1, 2, javaBackend));
        equal(new int[]{1, 2, 3, 4, 5}, states.matchingPositions(handles, 1, 6, 0, 0, javaBackend));
        equal(new int[0], states.matchingPositions(handles, 2, 2, 0, 0, javaBackend));
        if (nativeRun) {
            equal(new int[]{1, 4}, states.matchingPositions(handles, 1, 6, 1, 2, nativeBackend));
            equal(new int[]{1, 2, 3, 4, 5},
                    states.matchingPositions(handles, 1, 6, 0, 0, nativeBackend));
        }
        equal(states.updateHandles(handles, 1, 6, 8, 1), 3);
        equal(states.state(first), 8);
        equal(states.state(second), 10);
        equal(states.state(third), 12);
        equal(states.updateHandles(handles, 1, 6, 8, 1), 0);
        equal(before, handles);
        refuse(IllegalArgumentException.class,
                () -> states.updateHandles(handles, 1, 6, 3, 3));
        refuse(IndexOutOfBoundsException.class,
                () -> states.matchingPositions(handles, -1, 6, 0, 0, javaBackend));
        refuse(NullPointerException.class,
                () -> states.matchingPositions(handles, 1, 6, 0, 0, null));
        states.release(second);
        int unchanged = states.state(first);
        refuse(IllegalStateException.class,
                () -> states.updateHandles(handles, 1, 6, 1, 0));
        equal(states.state(first), unchanged);
        refuse(IllegalStateException.class,
                () -> states.matchingPositions(handles, 1, 6, 0, 0, javaBackend));
        if (nativeRun) {
            refuse(IllegalStateException.class,
                    () -> states.matchingPositions(handles, 1, 6, 0, 0, nativeBackend));
        }
        long reused = states.allocate(1);
        equal(M3SlotAllocator28.slot(reused), M3SlotAllocator28.slot(second));
        refuse(IllegalStateException.class,
                () -> states.matchingPositions(handles, 1, 6, 0, 0, javaBackend));
        states.release(reused);
        states.release(first);
        states.release(third);

        Random rng = new Random(0x4d33534c4f544d4cL);
        for (int trial = 0; trial < 96; trial++) {
            var arena = new M3ObjectStateSlots28();
            long[] ids = new long[1 + rng.nextInt(96)];
            for (int i = 0; i < ids.length; i++) {
                ids[i] = i > 0 && i % 7 == 0
                        ? ids[i - 1] : arena.allocate(rng.nextInt(256));
            }
            for (int iteration = 0; iteration < 32; iteration++) {
                int from = rng.nextInt(ids.length + 1);
                int to = from + rng.nextInt(ids.length - from + 1);
                int required = rng.nextInt(256);
                int excluded = rng.nextInt(256);
                int[] expected = new int[to - from];
                int count = 0;
                for (int i = from; i < to; i++) {
                    if (matches(arena.state(ids[i]), required, excluded)) {
                        expected[count++] = i;
                    }
                }
                expected = Arrays.copyOf(expected, count);
                equal(arena.matchingPositions(ids, from, to,
                        required, excluded, javaBackend), expected);
                if (nativeRun) {
                    equal(arena.matchingPositions(ids, from, to,
                            required, excluded, nativeBackend), expected);
                }
                operations++;
            }
            for (long h : ids) if (arena.isLive(h)) arena.release(h);
        }
    }

    public static void main(String[] args) throws Exception {
        boolean nativeRun = args.length == 1 && "--native".equals(args[0]);
        if (args.length != 0 && !nativeRun) throw new IllegalArgumentException("unsupported args");
        if (nativeRun && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("JNI library required");
        }
        lifecycleAndMemory();
        rangeAndRefusal();
        randomized();
        indexedMaskBridge(nativeRun);
        System.out.println("PASS M3_OBJECT_STATE_SLOTS_28 checks=" + checks
                + " randomized_operations=" + operations + " primitive_state_bytes_per_touched_slot=1");
    }
}
