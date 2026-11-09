// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * CDC contract: chunks cover the input contiguously within [min, max] (the last may be shorter);
 * two runs are byte-identical; after inserting bytes at a random point every original chunk that
 * starts at least {@code 2 * max} past the edit re-appears with the same SHA-256; per-chunk digests
 * equal a fresh SHA-256 of the slice; bad parameters are refused.
 *
 * <p>{@code --root=DIR --list=FILE}: the corpus files are concatenated into one 1 MiB+ byte stream
 * so chunks are plentiful; a seeded random stream is tested too.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/f7b9599a63da1c1264ecacdcb4c9286e055a0948/synexia-openrewrite-recipes/crates/m3-hash-family-20261030/src/test/java/com/synexia/rewrite/hash/M3ChunkCdcContract.java blob 6d8e8dbf3edf; recipe m3/indexdb/recipe/hash-family-20261106/relocate.py v3
public final class M3ChunkCdcContract {
    private static final M3ChunkCdc.Parameters SMALL = new M3ChunkCdc.Parameters(256, 1024, 4096);

    private M3ChunkCdcContract() { }

    public static void main(String[] args) throws IOException {
        Path root = Path.of(M3HashFamilyContractSupport.option(args, "--root=").orElseThrow());
        List<String> files = Files.readAllLines(Path.of(M3HashFamilyContractSupport.option(args, "--list=").orElseThrow()));
        byte[] corpus = concat(root, files);
        SplittableRandom random = new SplittableRandom(0x4d334344434Cl);
        byte[] noise = new byte[1 << 20];
        for (int i = 0; i < noise.length; i++) noise[i] = (byte) random.nextInt(256);
        stable(corpus, "corpus", M3ChunkCdc.DEFAULT, random);
        stable(corpus, "corpus", SMALL, random);
        stable(noise, "random", SMALL, random);
        digests(corpus);
        refused();
        System.out.println("M3ChunkCdcContract PASS");
    }

    static void stable(byte[] bytes, String name, M3ChunkCdc.Parameters parameters, SplittableRandom random) {
        M3ChunkCdc.Chunks first = M3ChunkCdc.chunk(bytes, parameters);
        M3ChunkCdc.Chunks second = M3ChunkCdc.chunk(bytes, parameters);
        check(Arrays.equals(lanes(first), lanes(second)), "determinism " + name);
        coverage(bytes, first, parameters);
        int edit = bytes.length / 3 + random.nextInt(bytes.length / 3);
        byte[] insert = new byte[1 + random.nextInt(300)];
        for (int i = 0; i < insert.length; i++) insert[i] = (byte) random.nextInt(256);
        byte[] edited = new byte[bytes.length + insert.length];
        System.arraycopy(bytes, 0, edited, 0, edit);
        System.arraycopy(insert, 0, edited, edit, insert.length);
        System.arraycopy(bytes, edit, edited, edit + insert.length, bytes.length - edit);
        M3ChunkCdc.Chunks after = M3ChunkCdc.chunk(edited, parameters);
        Set<String> editedDigests = new HashSet<>();
        M3ChunkSha.Digests afterDigests = M3ChunkSha.of(edited, after);
        for (int i = 0; i < after.count(); i++) editedDigests.add(afterDigests.hex(i));
        M3ChunkSha.Digests before = M3ChunkSha.of(bytes, first);
        int expected = 0;
        int survived = 0;
        int untouched = 0;
        for (int i = 0; i < first.count(); i++) {
            int start = first.offsets()[i];
            int end = start + first.lengths()[i];
            if (end <= edit || start >= edit + 2 * parameters.max()) {
                expected++;
                if (editedDigests.contains(before.hex(i))) survived++;
            }
            if (end <= edit || start >= edit) untouched++;
        }
        System.out.printf(Locale.ROOT, "INFO cdc %s bytes=%d %s chunks=%d edit=%d insert=%d farChunks=%d survived=%d%n",
                name, bytes.length, parameters.text(), first.count(), edit, insert.length, expected, survived);
        check(survived == expected, "chunk boundaries did not re-synchronise after the insertion (" + name + ")");
        check(untouched >= first.count() - 1, "edit touched more than one chunk");
    }

    static void coverage(byte[] bytes, M3ChunkCdc.Chunks chunks, M3ChunkCdc.Parameters parameters) {
        int position = 0;
        for (int i = 0; i < chunks.count(); i++) {
            check(chunks.offsets()[i] == position, "offset gap at chunk " + i);
            check(chunks.lengths()[i] <= parameters.max(), "chunk above max at " + i);
            check(chunks.lengths()[i] >= parameters.min() || i == chunks.count() - 1, "chunk below min at " + i);
            position += chunks.lengths()[i];
        }
        check(position == bytes.length, "chunks do not cover the input");
    }

    static void digests(byte[] bytes) {
        M3ChunkCdc.Chunks chunks = M3ChunkCdc.chunk(bytes, SMALL);
        M3ChunkSha.Digests digests = M3ChunkSha.of(bytes, chunks);
        for (int i = 0; i < chunks.count(); i++) {
            byte[] slice = Arrays.copyOfRange(bytes, chunks.offsets()[i], chunks.offsets()[i] + chunks.lengths()[i]);
            check(digests.hex(i).equals(M3ChunkSha.hex(slice)), "digest differs at chunk " + i);
        }
        System.out.printf(Locale.ROOT, "INFO chunk sha256 lane chunks=%d laneBytes=%d%n", chunks.count(), digests.lane().length);
    }

    static void refused() {
        for (int[] bad : new int[][] {{0, 1024, 4096}, {256, 1000, 4096}, {2048, 1024, 4096}, {256, 8192, 4096}}) {
            try {
                new M3ChunkCdc.Parameters(bad[0], bad[1], bad[2]);
                throw new AssertionError("accepted " + Arrays.toString(bad));
            } catch (IllegalArgumentException expected) {
                // refused as specified
            }
        }
    }

    static byte[] concat(Path root, List<String> files) throws IOException {
        byte[] out = new byte[0];
        int size = 0;
        for (String file : files) {
            byte[] bytes = Files.readAllBytes(root.resolve(file));
            if (out.length < size + bytes.length) out = Arrays.copyOf(out, Math.max(size + bytes.length, out.length * 2));
            System.arraycopy(bytes, 0, out, size, bytes.length);
            size += bytes.length;
        }
        return Arrays.copyOf(out, size);
    }

    static int[] lanes(M3ChunkCdc.Chunks chunks) {
        int[] lane = new int[chunks.count() * 2];
        for (int i = 0; i < chunks.count(); i++) {
            lane[2 * i] = chunks.offsets()[i];
            lane[2 * i + 1] = chunks.lengths()[i];
        }
        return lane;
    }

    static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
