/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Random;

/** Baseline + candidate compiled independently, exact shared-operation physical-state trace. */
//FLLDCIM: RELOCATE - donor blob c98b1cde67be at ca1aafc4d94f; recipe m3/collections/qualification/census-g6-20261106/relocate.py v4
public final class LongLongMapSharedTrace {
    private static long checks;
    private LongLongMapSharedTrace() { }
    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }
    private static Object field(LongLongHashMap map, String name) {
        try {
            Field f = LongLongHashMap.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(map);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static byte[] snapshot(PackedSlotStates states) {
        byte[] result = new byte[states.length()];
        for (int i = 0; i < result.length; i++) result[i] = states.get(i);
        return result;
    }
    private static void update(MessageDigest sha, LongLongHashMap map, int step) {
        String state = step + "|" + map.size() + "|" + map.capacity() + "|"
                + field(map,"modCount") + "|" + field(map,"occupied") + "|"
                + Arrays.toString((long[])field(map,"keys")) + "|"
                + Arrays.toString((long[])field(map,"values")) + "|"
                + Arrays.toString(snapshot((PackedSlotStates)field(map,"states")));
        sha.update(state.getBytes(StandardCharsets.UTF_8));
    }
    public static void main(String[] args) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        long[] keys = new long[97];
        for (int i=0;i<keys.length;i++) keys[i] = i % 3 == 0 ? i*71L : i-30L;
        keys[0] = Long.MIN_VALUE; keys[1] = Long.MAX_VALUE; keys[2] = 0L;
        for (int seed=0;seed<8;seed++) {
            Random random = new Random(0x561ffabcL + seed);
            LongLongHashMap map = new LongLongHashMap();
            HashMap<Long,Long> ref = new HashMap<>();
            for (int step=0;step<20000;step++) {
                long key = keys[random.nextInt(keys.length)];
                long value = random.nextLong();
                switch (random.nextInt(9)) {
                    case 0,1 -> {
                        boolean expected = !ref.containsKey(key);
                        check(map.put(key,value) == expected, "put"); ref.put(key,value);
                    }
                    case 2 -> check(map.remove(key) == (ref.remove(key)!=null), "remove");
                    case 3 -> check(map.containsKey(key) == ref.containsKey(key), "contains");
                    case 4 -> check(map.getOrDefault(key, -159L) == ref.getOrDefault(key,-159L), "get");
                    case 5 -> {
                        boolean expected = ref.containsKey(key) && ref.get(key)==value;
                        check(map.replace(key,value,value ^ 19L)==expected,"replace");
                        if(expected)ref.put(key,value ^ 19L);
                    }
                    case 6 -> {map.clear();ref.clear();}
                    case 7 -> check(map.isEmpty()==ref.isEmpty(),"empty");
                    case 8 -> check(map.keysToArray().length==ref.size(),"key count");
                    default -> throw new AssertionError();
                }
                check(map.size() == ref.size(),"size");
                update(sha,map,step);
            }
        }
        System.out.printf("M3_LONG_MAP_SHARED_TRACE checks=%d sha256=%s%n",checks,HexFormat.of().formatHex(sha.digest()));
    }
}
