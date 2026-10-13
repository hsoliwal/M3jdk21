// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Write side of the {@code M3CTRX01} image. Layout (all little-endian int32):
 *
 * <pre>
 * header[32]  magic "M3CT","RX01", version, 32, entries, slots, programInts, atoms, unitInts,
 *             trigrams, 0, jdkPatternClassSha256[8], inputSetSha256[8], 0...
 * table[slots]            entry ordinal + 1, 0 = empty; linear probing on h1
 * entries[entries][8]     h1lo, h1hi, h2lo, h2hi, utf16Length, flags, kindBits, programOffset
 * program[programInts]    per entry: count, then postfix ops (op &lt;&lt; 24 | arg):
 *                         ATOM(atom), AND(n), OR(n); count 0 = no constraint
 * atoms[atoms][4]         unitOffset, unitLength, trigramOffset, trigramCount
 * units[unitInts]         derived literal UTF-16 units, two per int (low half first)
 * trigrams[trigrams][2]   M3TQ keys (c0 &lt;&lt; 32 | c1 &lt;&lt; 16 | c2), lo/hi, sorted unique per atom
 * trailer[8]              SHA-256 of every preceding byte
 * </pre>
 *
 * <p>The image never stores a key pattern: entries are found by {@link M3ContentKey}. Units hold
 * only derived required literals; a key's spelling stays in the class-file constant pool.</p>
 */
public final class M3ConstantRegexImageWriter {
    /** One harvested key. */
    public record Item(String pattern, int flags, M3RegexRequirement.Analysis analysis) {}

    private final List<Integer> program = new ArrayList<>();
    private final Map<String, Integer> atomIndex = new LinkedHashMap<>();

    private M3ConstantRegexImageWriter() {}

    /** Builds the image bytes; throws if any two distinct keys collide on h1 or h2. */
    public static byte[] write(List<Item> items, int[] inputSetSha256) {
        return new M3ConstantRegexImageWriter().build(items, inputSetSha256);
    }

    private byte[] build(List<Item> items, int[] inputSha) {
        int slots = Integer.highestOneBit(Math.max(2, items.size() * 2 - 1)) << 1;
        int[] table = new int[slots];
        int[] entries = new int[items.size() * M3ConstantRegexImage.ENTRY_INTS];
        Set<Long> h1s = new HashSet<>();
        Set<Long> h2s = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            M3ContentKey key = M3ContentKey.of(item.pattern(), item.flags());
            if (!h1s.add(key.h1()) || !h2s.add(key.h2())) {
                throw new IllegalStateException("content-key collision at " + i);
            }
            int slot = (int) key.h1() & (slots - 1);
            while (table[slot] != 0) {
                slot = (slot + 1) & (slots - 1);
            }
            table[slot] = i + 1;
            int e = i * M3ConstantRegexImage.ENTRY_INTS;
            entries[e] = (int) key.h1();
            entries[e + 1] = (int) (key.h1() >>> 32);
            entries[e + 2] = (int) key.h2();
            entries[e + 3] = (int) (key.h2() >>> 32);
            entries[e + 4] = key.length();
            entries[e + 5] = key.flags();
            entries[e + 6] = item.analysis().kind().ordinal()
                    | (item.analysis().matcherStateAdmitted() ? 0x10 : 0);
            entries[e + 7] = encode(item.analysis().requirement());
        }
        return assemble(table, entries, inputSha);
    }

    private int encode(M3RegexRequirement.Req req) {
        int offset = program.size();
        program.add(0);
        emit(req);
        program.set(offset, program.size() - offset - 1);
        return offset;
    }

    private void emit(M3RegexRequirement.Req req) {
        switch (req.op()) {
            case ALL -> { }
            case LIT -> program.add((M3ConstantRegexImage.OP_ATOM << 24)
                    | atomIndex.computeIfAbsent(req.literal(), t -> atomIndex.size()));
            case AND, OR -> {
                req.terms().forEach(this::emit);
                int op = req.op() == M3RegexRequirement.Req.Op.AND
                        ? M3ConstantRegexImage.OP_AND : M3ConstantRegexImage.OP_OR;
                program.add((op << 24) | req.terms().size());
            }
        }
    }

    private byte[] assemble(int[] table, int[] entries, int[] inputSha) {
        List<String> atoms = new ArrayList<>(atomIndex.keySet());
        int[] atomLane = new int[atoms.size() * M3ConstantRegexImage.ATOM_INTS];
        StringBuilder units = new StringBuilder();
        List<Long> grams = new ArrayList<>();
        for (int a = 0; a < atoms.size(); a++) {
            long[] g = uniqueTrigrams(atoms.get(a));
            atomLane[a * 4] = units.length();
            atomLane[a * 4 + 1] = atoms.get(a).length();
            atomLane[a * 4 + 2] = grams.size();
            atomLane[a * 4 + 3] = g.length;
            units.append(atoms.get(a));
            Arrays.stream(g).forEach(grams::add);
        }
        int[] unitLane = new int[(units.length() + 1) / 2];
        for (int i = 0; i < units.length(); i++) {
            unitLane[i >>> 1] |= units.charAt(i) << ((i & 1) * 16);
        }
        int[] header = header(table.length, entries.length / 8, atoms.size(), unitLane.length,
                grams.size(), inputSha);
        int total = header.length + table.length + entries.length + program.size()
                + atomLane.length + unitLane.length + grams.size() * 2 + 8;
        ByteBuffer out = ByteBuffer.allocate(total * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int[] lane : List.of(header, table, entries, program.stream().mapToInt(Integer::intValue).toArray(),
                atomLane, unitLane)) {
            Arrays.stream(lane).forEach(out::putInt);
        }
        grams.forEach(g -> out.putInt((int) (long) g).putInt((int) (g >>> 32)));
        int[] sha = sha256Ints(out.duplicate().flip());
        Arrays.stream(sha).forEach(out::putInt);
        return out.array();
    }

    private int[] header(int slots, int entries, int atoms, int unitInts, int trigrams, int[] inputSha) {
        int[] h = new int[M3ConstantRegexImage.HEADER_INTS];
        h[0] = M3ConstantRegexImage.MAGIC;
        h[1] = M3ConstantRegexImage.MAGIC2;
        h[2] = M3ConstantRegexImage.VERSION;
        h[3] = M3ConstantRegexImage.HEADER_INTS;
        h[M3ConstantRegexImage.H_ENTRIES] = entries;
        h[M3ConstantRegexImage.H_SLOTS] = slots;
        h[M3ConstantRegexImage.H_PROGRAM] = program.size();
        h[M3ConstantRegexImage.H_ATOMS] = atoms;
        h[M3ConstantRegexImage.H_UNITS] = unitInts;
        h[M3ConstantRegexImage.H_TRIGRAMS] = trigrams;
        try {
            System.arraycopy(M3ConstantRegexImage.patternClassSha256(), 0, h, M3ConstantRegexImage.H_JDK_SHA, 8);
        } catch (M3ConstantRegexImage.Refusal unreadable) {
            throw new IllegalStateException(unreadable);
        }
        System.arraycopy(inputSha, 0, h, M3ConstantRegexImage.H_INPUT_SHA, 8);
        return h;
    }

    /** Unique sorted M3TQ trigram keys of {@code text}; empty below three units. */
    static long[] uniqueTrigrams(String text) {
        if (text.length() < 3) {
            return new long[0];
        }
        long[] keys = new long[text.length() - 2];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = ((long) text.charAt(i) << 32) | ((long) text.charAt(i + 1) << 16) | text.charAt(i + 2);
        }
        return Arrays.stream(keys).sorted().distinct().toArray();
    }

    /** SHA-256 of the remaining bytes as 8 big-endian ints. */
    static int[] sha256Ints(ByteBuffer bytes) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(bytes.duplicate());
            return toInts(sha.digest());
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(missing);
        }
    }

    static int[] toInts(byte[] digest) {
        int[] out = new int[digest.length / 4];
        ByteBuffer.wrap(digest).asIntBuffer().get(out);
        return out;
    }

    /** SHA-256 of a UTF-8 manifest, as 8 ints. */
    public static int[] sha256Ints(String manifest) {
        return sha256Ints(ByteBuffer.wrap(manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
