// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Read side of the {@code M3CTRX01} constant-regex precompute image: little-endian packed int
 * lanes, no object graph, memory-mapped read-only. See {@link M3ConstantRegexImageWriter} for the
 * layout. Opening refuses (throws {@link Refusal}) on bad magic/version, truncated lanes,
 * payload SHA-256 drift, or a {@code java/util/regex/Pattern.class} whose SHA-256 differs from the
 * JDK that produced the image: a stale image is never trusted.
 */
public final class M3ConstantRegexImage {
    static final int MAGIC = 0x4d334354;      // "M3CT"
    static final int MAGIC2 = 0x52583031;     // "RX01"
    static final int VERSION = 1;
    static final int HEADER_INTS = 32;
    static final int ENTRY_INTS = 8;
    static final int ATOM_INTS = 4;
    static final int H_ENTRIES = 4;
    static final int H_SLOTS = 5;
    static final int H_PROGRAM = 6;
    static final int H_ATOMS = 7;
    static final int H_UNITS = 8;
    static final int H_TRIGRAMS = 9;
    static final int H_JDK_SHA = 11;
    static final int H_INPUT_SHA = 19;
    static final int OP_ATOM = 1;
    static final int OP_AND = 2;
    static final int OP_OR = 3;

    /** Thrown when an image must not be trusted. */
    public static final class Refusal extends IOException {
        private static final long serialVersionUID = 1L;

        Refusal(String reason) {
            super(reason);
        }
    }

    /** A located entry: kind, admission bits and its decoded requirement lanes. */
    public record Entry(M3RegexRequirement.Kind kind, boolean matcherStateAdmitted, int[] program,
            String[] atoms, long[][] atomTrigrams) {}

    private final IntBuffer ints;
    private final int slots;
    private final int tableBase;
    private final int entryBase;
    private final int programBase;
    private final int atomBase;
    private final int unitBase;
    private final int trigramBase;

    private M3ConstantRegexImage(IntBuffer ints) throws Refusal {
        this.ints = ints;
        slots = ints.get(H_SLOTS);
        tableBase = HEADER_INTS;
        entryBase = tableBase + slots;
        programBase = entryBase + ints.get(H_ENTRIES) * ENTRY_INTS;
        atomBase = programBase + ints.get(H_PROGRAM);
        unitBase = atomBase + ints.get(H_ATOMS) * ATOM_INTS;
        trigramBase = unitBase + ints.get(H_UNITS);
        int end = trigramBase + ints.get(H_TRIGRAMS) * 2 + 8;
        if (end != ints.limit() || Integer.bitCount(slots) != 1) {
            throw new Refusal("lane geometry " + end + " != " + ints.limit());
        }
    }

    /** Memory-maps and verifies an image file. */
    public static M3ConstantRegexImage open(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            return verify(channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()));
        }
    }

    /** Verifies an in-memory image (used by tests and the harvester self-check). */
    public static M3ConstantRegexImage verify(ByteBuffer bytes) throws Refusal {
        ByteBuffer b = bytes.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        if (b.remaining() < (HEADER_INTS + 8) * 4 || (b.remaining() & 3) != 0) {
            throw new Refusal("truncated");
        }
        IntBuffer ints = b.asIntBuffer();
        if (ints.get(0) != MAGIC || ints.get(1) != MAGIC2 || ints.get(2) != VERSION
                || ints.get(3) != HEADER_INTS) {
            throw new Refusal("magic/version");
        }
        int[] trailer = new int[8];
        ints.get(ints.limit() - 8, trailer);
        ByteBuffer payload = b.duplicate().limit(b.limit() - 32);
        if (!Arrays.equals(trailer, M3ConstantRegexImageWriter.sha256Ints(payload))) {
            throw new Refusal("payload sha256 drift");
        }
        int[] producer = new int[8];
        ints.get(H_JDK_SHA, producer);
        if (!Arrays.equals(producer, patternClassSha256())) {
            throw new Refusal("java.util.regex.Pattern.class differs from the producing JDK");
        }
        return new M3ConstantRegexImage(ints);
    }

    /** SHA-256 of this JVM's {@code java/util/regex/Pattern.class} as 8 big-endian ints. */
    static int[] patternClassSha256() throws Refusal {
        try (InputStream in = Pattern.class.getResourceAsStream("Pattern.class")) {
            if (in == null) {
                throw new Refusal("Pattern.class unreadable");
            }
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return M3ConstantRegexImageWriter.toInts(sha.digest(in.readAllBytes()));
        } catch (IOException | NoSuchAlgorithmException unavailable) {
            throw new Refusal("Pattern.class digest: " + unavailable);
        }
    }

    public int entryCount() {
        return ints.get(H_ENTRIES);
    }

    /** Input-set SHA-256 recorded by the harvester. */
    public int[] inputSetSha256() {
        int[] out = new int[8];
        ints.get(H_INPUT_SHA, out);
        return out;
    }

    /** Finds the entry for {@code pattern}/{@code flags}; empty on miss. */
    public Optional<Entry> lookup(String pattern, int flags) {
        M3ContentKey key = M3ContentKey.of(pattern, flags);
        int mask = slots - 1;
        for (int probe = 0; probe < slots; probe++) {
            int slot = (int) (key.h1() + probe) & mask;
            int ref = ints.get(tableBase + slot);
            if (ref == 0) {
                return Optional.empty();
            }
            int e = entryBase + (ref - 1) * ENTRY_INTS;
            if (matches(e, key)) {
                return Optional.of(decode(e));
            }
        }
        return Optional.empty();
    }

    private boolean matches(int e, M3ContentKey key) {
        return ints.get(e) == (int) key.h1() && ints.get(e + 1) == (int) (key.h1() >>> 32)
                && ints.get(e + 2) == (int) key.h2() && ints.get(e + 3) == (int) (key.h2() >>> 32)
                && ints.get(e + 4) == key.length() && ints.get(e + 5) == key.flags();
    }

    private Entry decode(int e) {
        int bits = ints.get(e + 6);
        int at = programBase + ints.get(e + 7);
        int[] program = new int[ints.get(at)];
        ints.get(at + 1, program);
        int atomCount = (int) Arrays.stream(program).filter(op -> op >>> 24 == OP_ATOM).count();
        String[] atoms = new String[atomCount];
        long[][] grams = new long[atomCount][];
        int k = 0;
        for (int i = 0; i < program.length; i++) {
            if (program[i] >>> 24 == OP_ATOM) {
                int atom = program[i] & 0xffffff;
                atoms[k] = atomText(atom);
                grams[k] = atomTrigrams(atom);
                program[i] = (OP_ATOM << 24) | k++;
            }
        }
        return new Entry(M3RegexRequirement.Kind.values()[bits & 0xf], (bits & 0x10) != 0,
                program, atoms, grams);
    }

    private String atomText(int atom) {
        int a = atomBase + atom * ATOM_INTS;
        int offset = ints.get(a);
        int length = ints.get(a + 1);
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            int word = ints.get(unitBase + ((offset + i) >>> 1));
            text[i] = (char) (((offset + i) & 1) == 0 ? word : word >>> 16);
        }
        return new String(text);
    }

    private long[] atomTrigrams(int atom) {
        int a = atomBase + atom * ATOM_INTS;
        int offset = ints.get(a + 2);
        long[] grams = new long[ints.get(a + 3)];
        for (int i = 0; i < grams.length; i++) {
            int t = trigramBase + (offset + i) * 2;
            grams[i] = (ints.get(t) & 0xffffffffL) | ((long) ints.get(t + 1) << 32);
        }
        return grams;
    }
}
