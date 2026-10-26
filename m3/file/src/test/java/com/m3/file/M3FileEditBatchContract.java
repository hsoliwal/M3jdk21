// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

/** Source-sealed deterministic single-pass edit, mapping and rejection proof. */
public final class M3FileEditBatchContract {
    public M3FileEditBatchContract() { }
    private static int checks;
    private static void ok(boolean p, String why) {
        if (!p) throw new AssertionError(why);
        checks++;
    }
    private static void rejects(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable thrown) {
            if (!expected.isInstance(thrown)) throw new AssertionError("wrong refusal", thrown);
            checks++;
            return;
        }
        throw new AssertionError("accepted invalid edit");
    }
    private static byte[] bytes(String s) { return s.getBytes(StandardCharsets.UTF_8); }
    public static void main(String[] args) {
        M3FileSource base = M3FileSource.fromBytes(bytes("A😀B\nC"));
        var edits = M3FileEditBatch.on(base);
        byte[] insertion = bytes("!");
        edits.replaceUtf16(1,3,"Ω").replaceBytes(base.length(),base.length(),insertion);
        insertion[0] = (byte) '?';
        var applied = edits.apply();
        var map = applied.map();
        ok(Arrays.equals(applied.source().bytes(), bytes("AΩB\nC!")), "two edits, one copy, owned inserts");
        ok(Arrays.equals(base.bytes(), bytes("A😀B\nC")), "previous generation immutable");
        ok(map.changes() == 2 && map.oldLength() == 8 && map.newLength() == 7,
                "source geometry");
        ok(map.oldStart(0) == 1 && map.oldEnd(0) == 5
                && map.newStart(0) == 1 && map.newEnd(0) == 3, "replacement range");
        ok(map.oldToNew(1, M3FileEditBatch.Affinity.LEFT) == 1
                && map.oldToNew(1, M3FileEditBatch.Affinity.RIGHT) == 3,
                "left/right boundary affinity");
        ok(map.oldToNew(2, M3FileEditBatch.Affinity.LEFT) == -1
                && map.oldToNew(5, M3FileEditBatch.Affinity.RIGHT) == 3,
                "deleted interior refused; surviving boundary mapped");
        ok(map.oldToNew(8, M3FileEditBatch.Affinity.LEFT) == 6
                && map.oldToNew(8, M3FileEditBatch.Affinity.RIGHT) == 7,
                "insertion location affinity");
        ok(map.newToOld(2, M3FileEditBatch.Affinity.RIGHT) == -1
                && map.newToOld(3, M3FileEditBatch.Affinity.RIGHT) == 5,
                "inserted interior refused; suffix back mapping");
        ok(!map.touchesOldSpan(5,7) && map.touchesOldSpan(0,8)
                && map.unchangedOldSpan(5,7)[0] == 3, "conservative impacted ranges");
        map.requireOld(base); map.requireNew(applied.source());
        rejects(IllegalStateException.class, () -> map.requireOld(applied.source()));
        rejects(IllegalStateException.class, edits::apply);
        var empty = M3FileEditBatch.on(base).apply();
        ok(empty.source() == base && empty.map().changes() == 0, "no-op returns original generation");
        var bad = M3FileEditBatch.on(base);
        bad.replaceBytes(1,5,bytes("x"));
        rejects(IllegalArgumentException.class, () -> bad.replaceBytes(4,6,bytes("y")));
        rejects(IllegalArgumentException.class, () -> bad.replaceBytes(0,1,bytes("y")));
        var equalInsertions = M3FileEditBatch.on(base);
        equalInsertions.replaceBytes(1,1,bytes("x"));
        rejects(IllegalArgumentException.class, () -> equalInsertions.replaceBytes(1,1,bytes("y")));
        rejects(IllegalArgumentException.class, () -> M3FileEditBatch.on(base).replaceUtf16(2,3,"x"));
        rejects(IllegalArgumentException.class, () -> M3FileEditBatch.on(base).replaceUtf16(0,1,"\ud800"));
        // Fixed-seed differential oracle: 300 non-overlapping edits over immutable source coordinates.
        Random random = new Random(0x4d33464c);
        for (int iteration=0; iteration<300; iteration++) {
            byte[] original = new byte[32 + random.nextInt(128)];
            random.nextBytes(original);
            M3FileSource source = M3FileSource.fromBytes(original);
            var plan = M3FileEditBatch.on(source);
            var reference = new java.io.ByteArrayOutputStream();
            int cursor=0, last=-1;
            for (int position=0; position<original.length; ) {
                int gap = 1 + random.nextInt(7);
                int from = Math.min(original.length, position + gap);
                int to = Math.min(original.length, from + random.nextInt(5));
                if (from == original.length || from == last) break;
                reference.write(original, cursor, from-cursor);
                byte[] insert = new byte[random.nextInt(7)];
                random.nextBytes(insert);
                plan.replaceBytes(from,to,insert);
                reference.write(insert,0,insert.length);
                cursor=to;
                position=Math.max(to,from+1);
                last=from;
            }
            reference.write(original, cursor, original.length-cursor);
            var actual = plan.apply();
            ok(Arrays.equals(actual.source().bytes(),reference.toByteArray()), "differential byte output");
            ok(actual.map().oldSha256().equals(source.sha256())
                    && actual.map().newSha256().equals(actual.source().sha256()),
                    "old/new seals");
        }
        System.out.println("M3_FILE_EDIT_BATCH_PASS checks=" + checks);
    }
}
