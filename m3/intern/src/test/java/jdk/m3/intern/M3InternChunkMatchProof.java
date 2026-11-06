// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/** Differential private-arena oracle: equal content, mutated edges, page crossings. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/fd9a233680b43197eb8c5745a729c342ca56c4e2/synexia-common/src/test/java/com/synexia/common/intern/M3InternChunkMatchProof.java blob ef88c73810e4; recipe m3/intern/recipe/intern-space-20261106/relocate.py v4
public final class M3InternChunkMatchProof {
    private static int checks;
    private M3InternChunkMatchProof() { }
    public static void main(String[] args) throws Exception {
        run();
        System.out.println("M3InternChunkMatchProof PASS checks=" + checks);
    }
    public static void run() throws Exception {
        checks = 0;
        Random random = new Random(0x47a6bce0L);
        Class<?> bytes = Class.forName("jdk.m3.intern.M3InternSpace$ByteArena");
        Constructor<?> constructor = bytes.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object byteArena = constructor.newInstance();
        Method byteAdd = bytes.getDeclaredMethod("add", byte[].class);
        Method byteMatch = bytes.getDeclaredMethod("matches", long.class, byte[].class);
        byteAdd.setAccessible(true);
        byteMatch.setAccessible(true);
        for (int i = 0; i < 170; i++) {
            int n = switch (i % 13) {
                case 0 -> 0;
                case 1 -> 1;
                case 2 -> 65535;
                case 3 -> 65536;
                case 4 -> 65537;
                case 5 -> 131071;
                default -> random.nextInt(8193);
            };
            byte[] data = new byte[n];
            random.nextBytes(data);
            long start = (long) byteAdd.invoke(byteArena, data);
            compare(byteMatch, byteArena, start, data, true);
            for (int position : changedPositions(n)) {
                byte[] different = data.clone();
                different[position] ^= 1;
                compare(byteMatch, byteArena, start, different, false);
            }
        }
        Class<?> longs = Class.forName("jdk.m3.intern.M3InternSpace$LongArena");
        Constructor<?> longConstructor = longs.getDeclaredConstructor();
        longConstructor.setAccessible(true);
        Object longArena = longConstructor.newInstance();
        Method longAdd = longs.getDeclaredMethod("add", long[].class);
        Method longMatch = longs.getDeclaredMethod("matches", long.class, long[].class);
        longAdd.setAccessible(true);
        longMatch.setAccessible(true);
        for (int i = 0; i < 150; i++) {
            int n = switch (i % 12) {
                case 0 -> 0;
                case 1 -> 1;
                case 2 -> 8191;
                case 3 -> 8192;
                case 4 -> 8193;
                case 5 -> 16385;
                default -> random.nextInt(4097);
            };
            long[] data = new long[n];
            for (int j = 0; j < n; j++) data[j] = random.nextLong();
            long start = (long) longAdd.invoke(longArena, (Object) data);
            compare(longMatch, longArena, start, data, true);
            for (int position : changedPositions(n)) {
                long[] different = data.clone();
                different[position] ^= 1;
                compare(longMatch, longArena, start, different, false);
            }
        }
    }
    private static int[] changedPositions(int length) {
        if (length == 0) return new int[0];
        return Arrays.stream(new int[] {0, length/2, length-1}).distinct().toArray();
    }
    private static void compare(Method match, Object arena, long start, Object value, boolean expected)
            throws Exception {
        Object result = match.invoke(arena, start, value);
        if (!(result instanceof Boolean found) || found != expected) {
            throw new AssertionError("arena mismatch; expected " + expected);
        }
        checks++;
    }
}
