// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.intern;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Differential driver of the canonical-literal pool relocation (recipe canonical-literals-20261106, gate 3;
 * M3JDK-authored, test scope). One seeded literal stream of all nine kinds (UTF-16 strings with unpaired surrogates
 * and supplementary code points, booleans, chars with lone surrogates, byte / short / int / long extremes, raw float
 * and double bits with +0.0 / -0.0, infinities, subnormals and 1,000 distinct NaN payloads each, re-admitted in a
 * shuffled order), null strings and a null space, same-value-different-type groups (including equal payload bytes
 * under different kinds) and same-value-different-domain groups (TYPE, PATH and int-sequence entries of the same
 * space) is admitted through the pool and the space. The dump prints the equivalence classes of the handles (each
 * class named by the index of its first operation, never by a handle value), so the donor pool over its own atom
 * store and the relocated pool over InternSpace must print byte-identical dumps. It also checks the handle relation
 * against a reference key (Float.floatToRawIntBits, Double.doubleToRawLongBits, the String's UTF-16 code units, the
 * scope and the kind) and every payload read back from the space against the same references.
 *
 * <p>differential.sh compiles it a second time for the donor with its package line and the seven lines that name
 * the space API replaced (declared in differential.sh, each refused unless it occurs exactly once). Lines end with
 * a single LF on every platform.</p>
 *
 * <p>usage: java CanonicalLiteralPoolDump [random operations, default 100000]</p>
 */
public final class CanonicalLiteralPoolDump {
    /** Seed of the stream (named so a rerun replays the same inputs). */
    static final long SEED = 0x2026_1106_434C_4954L;
    static final int NAN_PATTERNS = 1_000;
    static final int DEFAULT_RANDOM = 100_000;
    static final int STRING_POOL = 5_000;
    static final long NPE = Long.MIN_VALUE;
    static final char STRING = 'S';
    static final char NULL_STRING = 'Z';
    static final char BOOLEAN = 'B';
    static final char CHAR = 'C';
    static final char BYTE = 'Y';
    static final char SHORT = 'H';
    static final char INT = 'I';
    static final char LONG = 'L';
    static final char FLOAT = 'F';
    static final char DOUBLE = 'D';
    static final char TYPE = 'T';
    static final char PATH = 'P';
    static final char INTS = 'N';
    static final char[] RANDOM_KINDS = {STRING, BOOLEAN, CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, TYPE, PATH, INTS};
    static final String[] SPECIAL_STRINGS = {
        "", "\u0000", "\u0000\u0000", "word", "Word", "\ud800", "\udbff", "\udc00", "\udfff", "\ud800\u0000\udc00",
        "�\u0000�", "😀", "\ude00\ud83d", "a\ud83d", "\ud83db", "􏿿", "𐀀",
        "￿", "﻿", "Å", "Å", "\u0001\u0000", "\u0000\u0001", "a/b", "java/lang/Object"};
    static final char[] SPECIAL_CHARS = {
        '\u0000', 'A', '\ud800', '\udbff', '\udc00', '\udfff', '�', '￿', 'ÿ', 'Ā'};
    static final long[] SPECIAL_BYTES = {Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE};
    static final long[] SPECIAL_SHORTS = {Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE, 0xff, 0x100};
    static final long[] SPECIAL_INTS = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE, Short.MIN_VALUE,
        Short.MAX_VALUE, 0x7fc0_0000, 0x0001_0000, 0xd800};
    static final long[] SPECIAL_LONGS = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE, Integer.MIN_VALUE,
        Integer.MAX_VALUE, 0xffff_ffffL, 0x7ff8_0000_0000_0000L, 0x0001_0000L};
    static final int[] SPECIAL_FLOAT_BITS = {0, 0x8000_0000, 0x7f80_0000, 0xff80_0000, 1, 0x8000_0001, 0x7f7f_ffff,
        0x3f80_0000, 0xbf80_0000, 0x7fc0_0000, 0x7fc0_0001, 0x7fc0_0002, 0xffc0_0000, 0x7f80_0001, 0x7fbf_ffff,
        0xffff_ffff, 0x0080_0000};
    static final long[] SPECIAL_DOUBLE_BITS = {0L, 0x8000_0000_0000_0000L, 0x7ff0_0000_0000_0000L,
        0xfff0_0000_0000_0000L, 1L, 0x8000_0000_0000_0001L, 0x7fef_ffff_ffff_ffffL, 0x3ff0_0000_0000_0000L,
        0x7ff8_0000_0000_0000L, 0x7ff8_0000_0000_0001L, 0x7ff8_0000_0000_0002L, 0xfff8_0000_0000_0000L,
        0x7ff0_0000_0000_0001L, 0x7ff7_ffff_ffff_ffffL, 0xffff_ffff_ffff_ffffL, 0x0010_0000_0000_0000L};
    static final long[] TYPE_GROUP_VALUES = {0, 1, -1, 0x41, 0xd800, 0x7fc0_0000, Integer.MIN_VALUE};
    static final long[] DOMAIN_GROUP_INTS = {0, 1, -1, Integer.MIN_VALUE};

    /** One admission: kind letter, text (strings, TYPE, PATH), raw value bits and scope (TYPE, PATH). */
    record Op(char kind, String text, long bits, int scope) { }

    private final PrintStream out;
    private final SplittableRandom random = new SplittableRandom(SEED);
    private final List<Op> ops = new ArrayList<>();
    private final List<String> strings = new ArrayList<>();
    private final List<int[]> typeGroups = new ArrayList<>();
    private final List<int[]> byteGroups = new ArrayList<>();
    private final List<int[]> domainGroups = new ArrayList<>();
    private final int[] floatNaNs = new int[NAN_PATTERNS];
    private final long[] doubleNaNs = new long[NAN_PATTERNS];
    private int floatNaNStart;
    private int doubleNaNStart;
    private int floatNaNRepeat;
    private int doubleNaNRepeat;
    private long[] handles;
    private String nullStringMessage = "-";
    private long checks;
    private long failures;

    private CanonicalLiteralPoolDump(PrintStream out) {
        this.out = out;
    }

    public static void main(String[] args) {
        int randomOps = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_RANDOM;
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(FileDescriptor.out), 1 << 16),
                false, StandardCharsets.UTF_8);
        CanonicalLiteralPoolDump dump = new CanonicalLiteralPoolDump(out);
        boolean pass = dump.run(randomOps);
        out.flush();
        if (!pass) {
            throw new AssertionError("CanonicalLiteralPoolDump FAIL");
        }
    }

    private boolean run(int randomOps) {
        specials();
        groups();
        nans();
        randomStream(randomOps);
        repeatNaNs();
        line("CanonicalLiteralPoolDump seed=0x" + Long.toHexString(SEED) + " ops=" + ops.size()
                + " random=" + randomOps);
        replay();
        line("TOTAL checks=" + checks + " failures=" + failures);
        line("CanonicalLiteralPoolDump " + (failures == 0 ? "PASS" : "FAIL") + " checks=" + checks);
        return failures == 0;
    }

    private void line(String text) {
        out.print(text);
        out.print('\n');
    }

    private void add(char kind, String text, long bits, int scope) {
        ops.add(new Op(kind, text, bits, scope));
    }

    private void add(char kind, long bits) {
        add(kind, "", bits, 0);
    }

    private void specials() {
        add(NULL_STRING, 0);
        for (int pass = 0; pass < 2; pass++) {
            for (String text : SPECIAL_STRINGS) {
                add(STRING, new String(text.toCharArray()), 0, 0);
            }
            for (char c : SPECIAL_CHARS) {
                add(CHAR, c);
            }
            add(BOOLEAN, 1);
            add(BOOLEAN, 0);
            addAll(BYTE, SPECIAL_BYTES);
            addAll(SHORT, SPECIAL_SHORTS);
            addAll(INT, SPECIAL_INTS);
            addAll(LONG, SPECIAL_LONGS);
            for (int bits : SPECIAL_FLOAT_BITS) {
                add(FLOAT, bits);
            }
            addAll(DOUBLE, SPECIAL_DOUBLE_BITS);
        }
        strings.addAll(Arrays.asList(SPECIAL_STRINGS));
    }

    private void addAll(char kind, long[] values) {
        for (long value : values) {
            add(kind, value);
        }
    }

    /** Same value under every kind; equal payload bytes under different kinds; one value in several domains. */
    private void groups() {
        for (long v : TYPE_GROUP_VALUES) {
            int start = ops.size();
            add(BOOLEAN, v & 1);
            add(CHAR, (char) v);
            add(BYTE, (byte) v);
            add(SHORT, (short) v);
            add(INT, (int) v);
            add(LONG, v);
            add(FLOAT, (int) v);
            add(DOUBLE, v);
            add(STRING, String.valueOf((char) v), 0, 0);
            typeGroups.add(new int[] {start, ops.size()});
        }
        byteGroup(new Op(BYTE, "", 0, 0), new Op(BOOLEAN, "", 0, 0));
        byteGroup(new Op(BYTE, "", 1, 0), new Op(BOOLEAN, "", 1, 0));
        byteGroup(new Op(SHORT, "", 0, 0), new Op(CHAR, "", 0, 0), new Op(STRING, "\u0000", 0, 0));
        byteGroup(new Op(INT, "", 0, 0), new Op(FLOAT, "", 0, 0), new Op(STRING, "\u0000\u0000", 0, 0));
        byteGroup(new Op(INT, "", 0x0001_0000, 0), new Op(FLOAT, "", 0x0001_0000, 0),
                new Op(STRING, "\u0000\u0001", 0, 0));
        byteGroup(new Op(LONG, "", 0, 0), new Op(DOUBLE, "", 0, 0), new Op(STRING, "\u0000\u0000\u0000\u0000", 0, 0));
        for (String text : new String[] {"word", "", "\ud800", "😀", "a/b", "java/lang/Object"}) {
            int start = ops.size();
            add(STRING, text, 0, 0);
            add(TYPE, text, 0, 0);
            add(TYPE, text, 0, 1);
            add(PATH, text, 0, 0);
            add(PATH, text, 0, 1);
            add(TYPE, new String(text.toCharArray()), 0, 0);
            add(PATH, new String(text.toCharArray()), 0, 0);
            domainGroups.add(new int[] {start, ops.size()});
        }
        for (long v : DOMAIN_GROUP_INTS) {
            int start = ops.size();
            add(INT, v);
            add(INTS, v);
            add(INTS, v);
            domainGroups.add(new int[] {start, ops.size()});
        }
        add(NULL_STRING, 0);
    }

    private void byteGroup(Op... members) {
        int start = ops.size();
        ops.addAll(Arrays.asList(members));
        byteGroups.add(new int[] {start, ops.size()});
    }

    /** 1,000 distinct float and 1,000 distinct double NaN payloads (both signs, quiet and signaling). */
    private void nans() {
        HashSet<Integer> floats = new HashSet<>();
        HashSet<Long> doubles = new HashSet<>();
        floatNaNStart = ops.size();
        for (int i = 0; i < NAN_PATTERNS; ) {
            int bits = (random.nextBoolean() ? 0x8000_0000 : 0) | 0x7f80_0000 | random.nextInt(1, 0x80_0000);
            if (floats.add(bits)) {
                floatNaNs[i++] = bits;
                add(FLOAT, bits);
            }
        }
        doubleNaNStart = ops.size();
        for (int i = 0; i < NAN_PATTERNS; ) {
            long bits = (random.nextBoolean() ? Long.MIN_VALUE : 0L) | 0x7ff0_0000_0000_0000L
                    | random.nextLong(1L, 1L << 52);
            if (doubles.add(bits)) {
                doubleNaNs[i++] = bits;
                add(DOUBLE, bits);
            }
        }
    }

    private void repeatNaNs() {
        int[] floatOrder = shuffled();
        floatNaNRepeat = ops.size();
        for (int at : floatOrder) {
            add(FLOAT, floatNaNs[at]);
        }
        int[] doubleOrder = shuffled();
        doubleNaNRepeat = ops.size();
        for (int at : doubleOrder) {
            add(DOUBLE, doubleNaNs[at]);
        }
        add(NULL_STRING, 0);
    }

    private int[] shuffled() {
        int[] order = new int[NAN_PATTERNS];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        for (int i = order.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int t = order[i];
            order[i] = order[j];
            order[j] = t;
        }
        return order;
    }

    private void randomStream(int count) {
        for (int i = 0; i < count; i++) {
            if (random.nextInt(1_000) == 0) {
                add(NULL_STRING, 0);
                continue;
            }
            char kind = RANDOM_KINDS[random.nextInt(RANDOM_KINDS.length)];
            boolean small = random.nextBoolean();
            switch (kind) {
                case STRING -> add(STRING, someString(), 0, 0);
                case BOOLEAN -> add(BOOLEAN, random.nextBoolean() ? 1 : 0);
                case CHAR -> add(CHAR, small ? 'a' + random.nextInt(8) : random.nextInt(0x1_0000));
                case BYTE -> add(BYTE, (byte) random.nextInt(256));
                case SHORT -> add(SHORT, small ? random.nextInt(-8, 8) : (short) random.nextInt(0x1_0000));
                case INT, INTS -> add(kind, small ? random.nextInt(-16, 16) : random.nextInt());
                case LONG -> add(LONG, small ? random.nextLong(-16, 16) : random.nextLong());
                case FLOAT -> add(FLOAT, floatBits());
                case DOUBLE -> add(DOUBLE, doubleBits());
                default -> add(kind, someString(), 0, random.nextInt(3));
            }
        }
    }

    private int floatBits() {
        return switch (random.nextInt(4)) {
            case 0 -> SPECIAL_FLOAT_BITS[random.nextInt(SPECIAL_FLOAT_BITS.length)];
            case 1 -> floatNaNs[random.nextInt(NAN_PATTERNS)];
            case 2 -> random.nextInt();
            default -> Float.floatToRawIntBits(random.nextInt(-16, 16));
        };
    }

    private long doubleBits() {
        return switch (random.nextInt(4)) {
            case 0 -> SPECIAL_DOUBLE_BITS[random.nextInt(SPECIAL_DOUBLE_BITS.length)];
            case 1 -> doubleNaNs[random.nextInt(NAN_PATTERNS)];
            case 2 -> random.nextLong();
            default -> Double.doubleToRawLongBits(random.nextInt(-16, 16));
        };
    }

    /** A fresh String object: half the time a copy of an earlier text, else new text with surrogates and NULs. */
    private String someString() {
        if (random.nextBoolean()) {
            return new String(strings.get(random.nextInt(strings.size())).toCharArray());
        }
        StringBuilder text = new StringBuilder();
        int length = random.nextInt(9);
        for (int i = 0; i < length; i++) {
            switch (random.nextInt(10)) {
                case 0 -> text.append((char) (0xd800 + random.nextInt(0x400)));
                case 1 -> text.append((char) (0xdc00 + random.nextInt(0x400)));
                case 2 -> text.appendCodePoint(random.nextInt(0x1_0000, 0x11_0000));
                case 3 -> text.append('\u0000');
                case 4, 5, 6 -> text.append((char) ('a' + random.nextInt(4)));
                default -> text.append((char) random.nextInt(0x1_0000));
            }
        }
        String made = text.toString();
        if (strings.size() < STRING_POOL) {
            strings.add(made);
        }
        return made;
    }

    private void replay() {
        handles = new long[ops.size()];
        InternSpace space = new InternSpace();
        CanonicalLiteralPool pool = new CanonicalLiteralPool(space);
        for (int i = 0; i < handles.length; i++) {
            handles[i] = admit(space, pool, ops.get(i));
        }
        Map<Long, List<Integer>> classes = classes();
        space.freeze();
        Map<Character, long[]> payloads = new LinkedHashMap<>();
        for (List<Integer> members : classes.values()) {
            Op op = ops.get(members.get(0));
            long handle = handles[members.get(0)];
            if (isLiteral(op.kind())) {
                byte[] payload = space.blobs().copy(space.shapes().members(handle)[1]);
                long[] counts = payloads.computeIfAbsent(op.kind(), k -> new long[2]);
                counts[0]++;
                counts[1] += check(payloadMatches(op, payload)) ? 0 : 1;
            }
        }
        report(classes, payloads);
    }

    private static long admit(InternSpace space, CanonicalLiteralPool pool, Op op) {
        return switch (op.kind()) {
            case STRING -> pool.stringLiteral(op.text());
            case NULL_STRING -> nullString(pool);
            case BOOLEAN -> pool.booleanLiteral(op.bits() != 0);
            case CHAR -> pool.charLiteral((char) op.bits());
            case BYTE -> pool.byteLiteral((byte) op.bits());
            case SHORT -> pool.shortLiteral((short) op.bits());
            case INT -> pool.intLiteral((int) op.bits());
            case LONG -> pool.longLiteral(op.bits());
            case FLOAT -> pool.floatLiteral(Float.intBitsToFloat((int) op.bits()));
            case DOUBLE -> pool.doubleLiteral(Double.longBitsToDouble(op.bits()));
            case TYPE -> space.internType(op.scope(), op.text());
            case PATH -> space.internPath(op.scope(), op.text());
            case INTS -> space.internFrozenInts(new int[] {(int) op.bits()});
            default -> throw new IllegalStateException("kind " + op.kind());
        };
    }

    private static long nullString(CanonicalLiteralPool pool) {
        try {
            return pool.stringLiteral(null);
        } catch (NullPointerException expected) {
            return NPE;
        }
    }

    private static String nullSpace() {
        try {
            return "admitted " + new CanonicalLiteralPool(null);
        } catch (NullPointerException expected) {
            return "NullPointerException:" + expected.getMessage();
        }
    }

    private static boolean isLiteral(char kind) {
        return kind != TYPE && kind != PATH && kind != INTS && kind != NULL_STRING;
    }

    /** Handle classes in first-occurrence order; the handle relation must equal the reference-key relation. */
    private Map<Long, List<Integer>> classes() {
        Map<Long, List<Integer>> classes = new LinkedHashMap<>();
        Map<String, Long> handleOfKey = new HashMap<>();
        Map<Long, String> keyOfHandle = new HashMap<>();
        long violations = 0;
        for (int i = 0; i < handles.length; i++) {
            Op op = ops.get(i);
            if (op.kind() == NULL_STRING) {
                nullStringMessage = handles[i] == NPE ? "NullPointerException" : "admitted";
                continue;
            }
            classes.computeIfAbsent(handles[i], h -> new ArrayList<>()).add(i);
            String key = key(op);
            Long knownHandle = handleOfKey.putIfAbsent(key, handles[i]);
            String knownKey = keyOfHandle.putIfAbsent(handles[i], key);
            boolean same = (knownHandle == null || knownHandle == handles[i]) && (knownKey == null || knownKey.equals(key));
            violations += check(same) ? 0 : 1;
        }
        line("RELATION ops=" + (handles.length - nulls()) + " classes=" + classes.size() + " keys=" + handleOfKey.size()
                + " violations=" + violations);
        return classes;
    }

    private long nulls() {
        return ops.stream().filter(op -> op.kind() == NULL_STRING).count();
    }

    /** Reference identity: kind, scope and raw bits / UTF-16 code units through the JDK reference functions. */
    private static String key(Op op) {
        return switch (op.kind()) {
            case STRING -> "S" + units(op.text());
            case TYPE, PATH -> op.kind() + Integer.toString(op.scope()) + ":" + units(op.text());
            case FLOAT -> "F" + Integer.toHexString(Float.floatToRawIntBits(Float.intBitsToFloat((int) op.bits())));
            case DOUBLE -> "D" + Long.toHexString(Double.doubleToRawLongBits(Double.longBitsToDouble(op.bits())));
            case BOOLEAN -> "B" + (op.bits() != 0);
            case CHAR -> "C" + (int) (char) op.bits();
            case BYTE -> "Y" + (byte) op.bits();
            case SHORT -> "H" + (short) op.bits();
            case INT, INTS -> op.kind() + Integer.toString((int) op.bits());
            default -> "L" + op.bits();
        };
    }

    private static String units(String text) {
        StringBuilder hex = new StringBuilder(text.length() * 4);
        for (int i = 0; i < text.length(); i++) {
            hex.append(HexFormat.of().toHexDigits(text.charAt(i)));
        }
        return hex.toString();
    }

    private static boolean payloadMatches(Op op, byte[] p) {
        return switch (op.kind()) {
            case STRING -> p.length % 2 == 0 && Arrays.equals(decodeUnits(p), op.text().toCharArray());
            case FLOAT -> p.length == 4
                    && (int) littleEndian(p) == Float.floatToRawIntBits(Float.intBitsToFloat((int) op.bits()));
            case DOUBLE -> p.length == 8
                    && littleEndian(p) == Double.doubleToRawLongBits(Double.longBitsToDouble(op.bits()));
            case LONG -> p.length == 8 && littleEndian(p) == op.bits();
            case INT -> p.length == 4 && (int) littleEndian(p) == (int) op.bits();
            case SHORT -> p.length == 2 && (short) littleEndian(p) == (short) op.bits();
            case CHAR -> p.length == 2 && (char) littleEndian(p) == (char) op.bits();
            case BYTE -> p.length == 1 && p[0] == (byte) op.bits();
            default -> p.length == 1 && p[0] == (op.bits() != 0 ? 1 : 0);
        };
    }

    private static long littleEndian(byte[] p) {
        long value = 0;
        for (int i = p.length - 1; i >= 0; i--) {
            value = (value << 8) | (p[i] & 0xff);
        }
        return value;
    }

    private static char[] decodeUnits(byte[] p) {
        char[] units = new char[p.length / 2];
        for (int i = 0; i < units.length; i++) {
            units[i] = (char) ((p[2 * i] & 0xff) | ((p[2 * i + 1] & 0xff) << 8));
        }
        return units;
    }

    private boolean check(boolean ok) {
        checks++;
        if (!ok) {
            failures++;
        }
        return ok;
    }

    private void report(Map<Long, List<Integer>> classes, Map<Character, long[]> payloads) {
        kinds(classes);
        line("NULL string ops=" + nulls() + " result=" + nullStringMessage + " space=" + nullSpace());
        nanLines();
        line("CROSS type " + crossGroups(typeGroups));
        line("CROSS bytes " + crossGroups(byteGroups));
        line("CROSS domain " + crossGroups(domainGroups));
        payloads.forEach((kind, counts) -> line("PAYLOAD " + kind + " classes=" + counts[0] + " mismatches="
                + counts[1]));
        classes.values().forEach(members -> line(classLine(members)));
    }

    private void kinds(Map<Long, List<Integer>> classes) {
        Map<Character, long[]> perKind = new LinkedHashMap<>();
        for (char kind : RANDOM_KINDS) {
            perKind.put(kind, new long[2]);
        }
        for (int i = 0; i < handles.length; i++) {
            if (ops.get(i).kind() != NULL_STRING) {
                perKind.get(ops.get(i).kind())[0]++;
            }
        }
        classes.values().forEach(members -> perKind.get(ops.get(members.get(0)).kind())[1]++);
        perKind.forEach((kind, counts) -> line("K " + kind + " ops=" + counts[0] + " classes=" + counts[1]));
    }

    private void nanLines() {
        int floatExact = 0;
        int doubleExact = 0;
        int floatSame = 0;
        int doubleSame = 0;
        Set<Long> floatHandles = new HashSet<>();
        Set<Long> doubleHandles = new HashSet<>();
        for (int i = 0; i < NAN_PATTERNS; i++) {
            floatExact += Float.floatToRawIntBits(Float.intBitsToFloat(floatNaNs[i])) == floatNaNs[i] ? 1 : 0;
            doubleExact += Double.doubleToRawLongBits(Double.longBitsToDouble(doubleNaNs[i])) == doubleNaNs[i] ? 1 : 0;
            floatHandles.add(handles[floatNaNStart + i]);
            doubleHandles.add(handles[doubleNaNStart + i]);
        }
        for (int i = 0; i < NAN_PATTERNS; i++) {
            Op repeat = ops.get(floatNaNRepeat + i);
            int first = indexOf(floatNaNs, (int) repeat.bits());
            floatSame += check(handles[floatNaNRepeat + i] == handles[floatNaNStart + first]) ? 1 : 0;
            Op repeatD = ops.get(doubleNaNRepeat + i);
            int firstD = indexOf(doubleNaNs, repeatD.bits());
            doubleSame += check(handles[doubleNaNRepeat + i] == handles[doubleNaNStart + firstD]) ? 1 : 0;
        }
        check(floatHandles.size() == NAN_PATTERNS);
        check(doubleHandles.size() == NAN_PATTERNS);
        line("NAN float patterns=" + NAN_PATTERNS + " handles=" + floatHandles.size() + " repeat_same=" + floatSame
                + " roundtrip_exact=" + floatExact);
        line("NAN double patterns=" + NAN_PATTERNS + " handles=" + doubleHandles.size() + " repeat_same=" + doubleSame
                + " roundtrip_exact=" + doubleExact);
    }

    private static int indexOf(int[] values, int value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOf(long[] values, long value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    /** Pairs inside each group: equal reference keys must share the handle, different keys must not (merged). */
    private String crossGroups(List<int[]> groups) {
        long pairs = 0;
        long merged = 0;
        long repeats = 0;
        long repeatsEqual = 0;
        for (int[] group : groups) {
            for (int i = group[0]; i < group[1]; i++) {
                for (int j = i + 1; j < group[1]; j++) {
                    boolean sameKey = key(ops.get(i)).equals(key(ops.get(j)));
                    boolean sameHandle = handles[i] == handles[j];
                    pairs++;
                    repeats += sameKey ? 1 : 0;
                    repeatsEqual += sameKey && check(sameHandle) ? 1 : 0;
                    merged += !sameKey && !check(!sameHandle) ? 1 : 0;
                }
            }
        }
        return "groups=" + groups.size() + " pairs=" + pairs + " merged=" + merged + " repeats_equal=" + repeatsEqual
                + "/" + repeats;
    }

    private String classLine(List<Integer> members) {
        StringBuilder text = new StringBuilder("E ").append(members.get(0)).append(' ')
                .append(ops.get(members.get(0)).kind()).append(" n=").append(members.size());
        char separator = ' ';
        for (int member : members) {
            text.append(separator).append(member);
            separator = ',';
        }
        return text.toString();
    }
}
