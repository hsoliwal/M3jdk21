/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Original/current successful-operation physical-state trace, including JNI-free primitive lanes. */
public final class M3LongLongSharedTrace {
    private static final Field KEYS = field("keys");
    private static final Field VALUES = field("values");
    private static final Field STATES = field("states");
    private static final Field SIZE = field("size");
    private static final Field OCCUPIED = field("occupied");
    private static final Field MOD = field("modCount");
    private static int checks;
    private M3LongLongSharedTrace() { }

    private static Field field(String name) {
        try {
            Field value = M3LongLongHashMap.class.getDeclaredField(name);
            value.setAccessible(true);
            return value;
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private static long[] longs(Field field, M3LongLongHashMap map) {
        try { return (long[]) field.get(map); } catch (IllegalAccessException failure) { throw new AssertionError(failure); }
    }
    private static byte[] bytes(Field field, M3LongLongHashMap map) {
        try { return (byte[]) field.get(map); } catch (IllegalAccessException failure) { throw new AssertionError(failure); }
    }
    private static int integer(Field field, M3LongLongHashMap map) {
        try { return field.getInt(map); } catch (IllegalAccessException failure) { throw new AssertionError(failure); }
    }
    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason + " at check " + checks);
    }
    private static void number(MessageDigest digest, long n) {
        for (int shift = 56; shift >= 0; shift -= 8) digest.update((byte) (n >>> shift));
    }
    private static void physical(MessageDigest digest, M3LongLongHashMap map) {
        number(digest, integer(SIZE, map));
        number(digest, integer(OCCUPIED, map));
        number(digest, integer(MOD, map));
        long[] keys = longs(KEYS, map), values = longs(VALUES, map);
        byte[] states = bytes(STATES, map);
        check(keys.length == values.length && states.length == keys.length, "primitive lane lengths");
        number(digest, keys.length);
        for (int i = 0; i < keys.length; i++) {
            digest.update(states[i]);
            number(digest, keys[i]);
            number(digest, values[i]);
        }
    }
    private static long key(Random random, int step) {
        return switch (step % 29) {
            case 0 -> 0L;
            case 1 -> -1L;
            case 2 -> Long.MIN_VALUE;
            case 3 -> Long.MAX_VALUE;
            default -> random.nextInt(321) - 160L;
        };
    }
    public static void main(String[] args) throws NoSuchAlgorithmException {
        checks = 0;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int seed = 0; seed < 3; seed++) {
            Random random = new Random(0x4d33564d434f4d50L + seed * 13L);
            M3LongLongHashMap map = new M3LongLongHashMap(20);
            Map<Long, Long> model = new HashMap<>();
            for (int step = 0; step < 33_333; step++) {
                long key = key(random, step), value = random.nextLong();
                switch (random.nextInt(12)) {
                    case 0, 1, 2 -> {
                        boolean absent = !model.containsKey(key);
                        model.put(key, value);
                        check(map.put(key, value) == absent, "put");
                    }
                    case 3, 4 -> {
                        long next = model.getOrDefault(key, 0L) + value;
                        model.put(key, next);
                        check(map.addTo(key, value) == next, "addTo");
                    }
                    case 5 -> check(map.remove(key) == (model.remove(key) != null), "remove");
                    case 6 -> {
                        boolean willRemove = model.containsKey(key) && model.get(key) == value;
                        if (willRemove) model.remove(key);
                        check(map.remove(key, value) == willRemove, "conditional remove");
                    }
                    case 7 -> {
                        boolean replaced = model.containsKey(key) && model.get(key) == value;
                        if (replaced) model.put(key, value + 17);
                        check(map.replace(key, value, value + 17) == replaced, "replace");
                    }
                    case 8 -> check(map.containsKey(key) == model.containsKey(key), "contains");
                    case 9 -> check(map.getOrDefault(key, 77L) == model.getOrDefault(key, 77L), "lookup");
                    case 10 -> {
                        if (step % 197 == 0) { map.clear(); model.clear(); }
                        else check(map.containsValue(value) == model.containsValue(value), "containsValue");
                    }
                    case 11 -> {
                        check(map.isEmpty() == model.isEmpty(), "isEmpty");
                        check(map.capacity() >= 8, "capacity");
                    }
                    default -> throw new AssertionError();
                }
                check(map.size() == model.size(), "size");
                if (step % 67 == 0) {
                    long[] keys = map.keysToArray();
                    check(keys.length == model.size(), "keys array length");
                    Arrays.sort(keys);
                    long[] expected = model.keySet().stream().mapToLong(Long::longValue).sorted().toArray();
                    check(Arrays.equals(keys, expected), "model keys");
                    map.forEach((k,v) -> {
                        if (!model.containsKey(k) || model.get(k) != v) throw new AssertionError("forEach");
                    });
                    physical(digest, map);
                }
            }
            digest.update(("seed=" + seed).getBytes(StandardCharsets.UTF_8));
        }
        System.out.printf("M3_PHYSICAL_TRACE_PASS checks=%d sha256=%s%n", checks,
                java.util.HexFormat.of().formatHex(digest.digest()));
    }
}
