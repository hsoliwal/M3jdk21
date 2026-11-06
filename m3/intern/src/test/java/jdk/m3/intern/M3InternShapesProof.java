// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

/** Executable Java21 differential assertions independent of Maven dependencies. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/fd9a233680b43197eb8c5745a729c342ca56c4e2/synexia-common/src/test/java/com/synexia/common/intern/M3InternShapesProof.java blob d8c71200fb17; recipe m3/intern/recipe/intern-space-20261106/relocate.py v4
public final class M3InternShapesProof {
    private static int checks;
    private M3InternShapesProof() { }
    public static void main(String[] args) throws Exception {
        run();
        System.out.println("M3InternShapesProof PASS checks=" + checks);
    }
    public static void run() throws Exception {
        checks = 0;
        M3InternSpace space = new M3InternSpace();
        M3InternShapes shapes = new M3InternShapes(space);
        long t1 = space.internType(5, "I");
        long t2 = space.internType(5, "Ljava/lang/Object;");
        long foreign = space.internType(6, "I");
        long method = shapes.internMethod(5, "run", t2, t1, t1);
        equal(method, shapes.internMethod(5, new String("run"), t2, t1, t1));
        equal(false, method == shapes.internMethod(5, "other", t2, t1, t1));
        equal(false, method == shapes.internMethod(5, "run", t2, t1));
        rejects(IllegalArgumentException.class, () -> shapes.internMethod(5, "run", foreign));
        rejects(IllegalArgumentException.class, () -> shapes.internMethod(-1, "bad", t1));
        rejects(IllegalArgumentException.class, () -> shapes.internMethod(5, "run", t1, foreign));
        String[] names = {"a", "b"};
        long[] types = {t1, t2};
        long record = shapes.internRecord(5, t2, names, types);
        names[0] = "x";
        types[0] = foreign;
        equal(record, shapes.internRecord(5, t2, new String[] {"a", "b"}, new long[] {t1, t2}));
        equal(false, record == shapes.internRecord(5, t2, new String[] {"b", "a"}, new long[] {t2, t1}));
        rejects(IllegalArgumentException.class, () -> shapes.internRecord(5, t2, new String[2], new long[1]));
        rejects(IllegalArgumentException.class, () -> shapes.internRecord(5, t2, new String[] {"a"}, new long[] {foreign}));
        String[] tokens = {" ", "\u0000", "\ud800", "𐐀"};
        long seq = shapes.internTokens(9, tokens);
        tokens[0] = "tampered";
        equal(true, Arrays.equals(new String[] {" ", "\u0000", "\ud800", "𐐀"}, shapes.copyTokens(seq)));
        equal(seq, shapes.internTokens(9, " ", "\u0000", "\ud800", "𐐀"));
        equal(false, seq == shapes.internTokens(10, " ", "\u0000", "\ud800", "𐐀"));
        long[] values = {0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 123456789L};
        long immutable = shapes.internFrozenLongs(values);
        values[0] = 99;
        long[] copy = shapes.copyFrozenLongs(immutable);
        equal(true, Arrays.equals(new long[] {0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 123456789L}, copy));
        copy[0] = 123;
        equal(0L, shapes.copyFrozenLongs(immutable)[0]);
        equal(immutable, shapes.internFrozenLongs(new long[] {0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 123456789L}));
        rejects(IllegalArgumentException.class, () -> shapes.copyFrozenLongs(method));
        BitSet mask = new BitSet();
        mask.set(1); mask.set(4097);
        long maskId = shapes.internBitMask(mask);
        mask.clear();
        BitSet returned = shapes.copyBitMask(maskId);
        equal(true, returned.get(1) && returned.get(4097));
        returned.clear();
        equal(true, shapes.copyBitMask(maskId).get(4097));
        BitSet mask2 = new BitSet(); mask2.set(1); mask2.set(4097);
        equal(maskId, shapes.internBitMask(mask2));
        equal(false, shapes.internBitMask(new BitSet()) == maskId);
        rejects(IllegalArgumentException.class, () -> shapes.copyBitMask(seq));
        Random rnd = new Random(48814);
        for (int i = 0; i < 10_000; i++) {
            long[] x = new long[i % 13];
            for (int j = 0; j < x.length; j++) x[j] = rnd.nextLong();
            long id = shapes.internFrozenLongs(x);
            equal(true, Arrays.equals(x, shapes.copyFrozenLongs(id)));
            equal(id, shapes.internFrozenLongs(x));
        }
        space.freeze();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        space.writeTo(new DataOutputStream(bytes));
        M3InternSpace replay = M3InternSpace.readFrom(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        M3InternShapes restored = new M3InternShapes(replay);
        equal(method, restored.internMethod(5, "run", t2, t1, t1));
        equal(record, restored.internRecord(5, t2, new String[] {"a", "b"}, new long[] {t1, t2}));
        equal(maskId, restored.internBitMask(mask2));
        equal(true, Arrays.equals(shapes.copyTokens(seq), restored.copyTokens(seq)));
        equal(true, Arrays.equals(shapes.copyFrozenLongs(immutable), restored.copyFrozenLongs(immutable)));
        rejects(IllegalStateException.class, () -> restored.internTokens(7, "new"));
    }
    private static void equal(Object a, Object b) { if (!a.equals(b)) throw new AssertionError(a + " != " + b); checks++; }
    private static void rejects(Class<? extends Throwable> kind, Runnable action) {
        try { action.run(); } catch (Throwable t) { if (kind.isInstance(t)) { checks++; return; } throw t; }
        throw new AssertionError("did not reject " + kind.getName());
    }
}
