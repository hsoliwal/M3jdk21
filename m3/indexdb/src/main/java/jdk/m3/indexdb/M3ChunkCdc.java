// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * Content-defined chunking with a gear rolling hash (the FastCDC gear, plain mask).
 *
 * <p>Input: bytes and {@link Parameters} (min, avg = power of two, max). Output: {@link Chunks},
 * two int lanes (offsets, lengths) covering the input exactly and contiguously. A cut falls at the
 * first byte position {@code p >= start + min} where {@code (fp & (avg - 1)) == 0}, or at
 * {@code start + max}; {@code fp = (fp << 1) + GEAR[b]} restarts at every cut, so the hash sees
 * only the last 64 bytes and boundaries re-synchronise after an insertion (stability contract).
 * Effects: none. Failure: {@code min < 1}, {@code avg} not a power of two, or
 * {@code min > avg > max} order violated is an {@link IllegalArgumentException}. The 256-entry
 * gear table is derived from {@link #GEAR_SEED} through {@link SplittableRandom}, so the
 * boundaries are reproducible everywhere. No owner in the repository had a rolling-hash chunker
 * (grep over specgrep chunkers: 0 hits), so this is the family's one new atom.</p>
 */
//FLLDCIM: CDC — gear hash; offsets+lengths as int lanes; min/avg/max from the registry
//FLLDCIM: HPCPOP — one pass over bytes, two int lanes, no chunk objects
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/f7b9599a63da1c1264ecacdcb4c9286e055a0948/synexia-openrewrite-recipes/crates/m3-hash-family-20261030/src/main/java/com/synexia/rewrite/hash/M3ChunkCdc.java blob ba00158858e0; recipe m3/indexdb/recipe/hash-family-20261106/relocate.py v3
public final class M3ChunkCdc {
    public static final String ALGORITHM = "CHUNK_CDC_GEAR";
    public static final int VERSION = 1;
    public static final long GEAR_SEED = 0x4d3347454152L;
    public static final Parameters DEFAULT = new Parameters(2048, 8192, 65536);
    private static final long[] GEAR = gear(GEAR_SEED);

    /** Chunk sizes: minimum, average (power of two, the mask) and maximum. */
    public record Parameters(int min, int avg, int max) {
        public Parameters {
            if (min < 1 || Integer.bitCount(avg) != 1 || min > avg || avg > max) {
                throw new IllegalArgumentException("need 1 <= min <= avg (power of two) <= max");
            }
        }

        public String text() { return "min=" + min + ";avg=" + avg + ";max=" + max + ";window=64"; }
    }

    /** Chunk lanes: {@code offsets[i]}, {@code lengths[i]} for {@code i < count}. */
    public record Chunks(int[] offsets, int[] lengths, int count) { }

    private M3ChunkCdc() { }

    public static Chunks chunk(byte[] bytes, Parameters parameters) {
        int[] offsets = new int[16];
        int[] lengths = new int[16];
        int count = 0;
        int start = 0;
        while (start < bytes.length) {
            int end = cut(bytes, start, parameters);
            offsets = M3HashLanes.grow(offsets, count + 1);
            lengths = M3HashLanes.grow(lengths, count + 1);
            offsets[count] = start;
            lengths[count++] = end - start;
            start = end;
        }
        return new Chunks(offsets, lengths, count);
    }

    /** First cut position after {@code start}; the end of input when the rest is shorter than min. */
    static int cut(byte[] bytes, int start, Parameters parameters) {
        int limit = Math.min(bytes.length, start + parameters.max());
        long mask = parameters.avg() - 1L;
        long fp = 0;
        for (int i = start; i < limit; i++) {
            fp = (fp << 1) + GEAR[bytes[i] & 0xff];
            if (i + 1 - start >= parameters.min() && (fp & mask) == 0) return i + 1;
        }
        return limit;
    }

    static long[] gear(long seed) {
        SplittableRandom random = new SplittableRandom(seed);
        long[] table = new long[256];
        Arrays.setAll(table, i -> random.nextLong());
        return table;
    }
}
