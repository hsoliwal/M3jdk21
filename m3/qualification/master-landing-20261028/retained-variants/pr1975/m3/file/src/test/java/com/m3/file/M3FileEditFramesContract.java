// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Differential reference oracle for a provider-independent TSInputEdit-shaped packet. */
public final class M3FileEditFramesContract {
    private M3FileEditFramesContract() { }

    private static int checks;
    private static void ok(boolean yes, String reason) {
        if (!yes) throw new AssertionError(reason);
        checks++;
    }
    private static void refuse(Class<? extends Throwable> exception, Runnable action) {
        try { action.run(); }
        catch (Throwable t) {
            if (!exception.isInstance(t)) throw new AssertionError("wrong rejection", t);
            checks++; return;
        }
        throw new AssertionError("accepted invalid edit frame");
    }
    private static byte[] utf8(String s) { return s.getBytes(StandardCharsets.UTF_8); }
    /** Independent line scanner. An offset splitting CRLF is still on the preceding line. */
    private static M3FileEditFrames.Point point(byte[] bytes, int offset) {
        int row = 0, lineStart = 0;
        for (int i = 0; i < offset; ) {
            int c = bytes[i] & 0xff;
            int newline = c == '\r' && i + 1 < bytes.length && bytes[i + 1] == '\n' ? 2
                    : c == '\r' || c == '\n' ? 1 : 0;
            if (newline > 0 && i + newline <= offset) {
                i += newline;
                lineStart = i;
                row++;
            } else i++;
        }
        return new M3FileEditFrames.Point(row, offset - lineStart);
    }
    private static byte[] splice(byte[] source, int from, int to, byte[] inserted) {
        byte[] result = new byte[source.length - (to - from) + inserted.length];
        System.arraycopy(source, 0, result, 0, from);
        System.arraycopy(inserted, 0, result, from, inserted.length);
        System.arraycopy(source, to, result, from + inserted.length, source.length - to);
        return result;
    }
    private static void compare(M3FileSource old, M3FileEditBatch.Applied changed) {
        var frames = M3FileEditFrames.plan(old, changed);
        ok(frames.size() == changed.map().changes()
                && frames.originalSha256().equals(old.sha256())
                && frames.finalSha256().equals(changed.source().sha256()), "generation provenance");
        byte[] intermediate = old.bytes();
        byte[] finalBytes = changed.source().bytes();
        for (int i = changed.map().changes() - 1, k = 0; i >= 0; i--, k++) {
            var frame = frames.frame(k);
            int start = changed.map().oldStart(i), end = changed.map().oldEnd(i);
            byte[] replacement = Arrays.copyOfRange(finalBytes, changed.map().newStart(i),
                    changed.map().newEnd(i));
            byte[] updated = splice(intermediate, start, end, replacement);
            ok(frame.originalEditIndex() == i && frame.startByte() == start
                    && frame.oldEndByte() == end && frame.newEndByte() == start + replacement.length,
                    "reverse-order edit geometry");
            ok(frame.startPoint().equals(point(intermediate, start)), "independent start byte point");
            ok(frame.oldEndPoint().equals(point(intermediate, end)), "independent old end point");
            ok(frame.newEndPoint().equals(point(updated, start + replacement.length)),
                    "independent new end point");
            intermediate = updated;
        }
        ok(Arrays.equals(intermediate, finalBytes), "sequential frames reconstruct final text");
    }

    /** Differential proof of the bounded LF-only lane against stage-by-stage replay. */
    private static void linearLfOracle() {
        Random rng = new Random(0x3a53e7cL);
        String[] atoms = {"a", "x", "β", "😀", "\n", "_"};
        String[] inserts = {"", "A", "Ω", "😀", "\n", "a\nb", "\n\n"};
        for (int repetition = 0; repetition < 500; repetition++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < 35 + rng.nextInt(90); j++)
                code.append(atoms[rng.nextInt(atoms.length)]);
            byte[] raw = utf8(code.toString());
            List<Integer> valid = new ArrayList<>();
            for (int i = 0; i < raw.length; i++)
                if ((raw[i] & 0xc0) != 0x80) valid.add(i);
            valid.add(raw.length);
            M3FileSource original = M3FileSource.fromBytes(raw);
            M3FileEditBatch batch = M3FileEditBatch.on(original);
            int previous = -1;
            for (int k = 0; k < 2 + rng.nextInt(6); k++) {
                int startIdx = Math.min(valid.size() - 1,
                        previous + 2 + rng.nextInt(3));
                if (startIdx == valid.size() - 1) break;
                int endIdx = Math.min(valid.size() - 1,
                        startIdx + rng.nextInt(3));
                batch.replaceBytes(valid.get(startIdx), valid.get(endIdx),
                        utf8(inserts[rng.nextInt(inserts.length)]));
                previous = endIdx;
            }
            M3FileEditBatch.Applied applied = batch.apply();
            M3FileEditFrames baseline = M3FileEditFrames.plan(original, applied, Long.MAX_VALUE);
            M3FileEditFrames fast = M3FileEditFrames.planLinearLf(original, applied,
                    Long.MAX_VALUE);
            ok(baseline.size() == fast.size(), "LF-only edit count");
            ok(baseline.originalSha256().equals(fast.originalSha256())
                    && baseline.finalSha256().equals(fast.finalSha256()),
                    "LF-only generation identity");
            for (int i = 0; i < baseline.size(); i++)
                ok(baseline.frame(i).equals(fast.frame(i)), "byte and row/column geometry");
            if (baseline.size() >= 3)
                ok(fast.materializedByteWork() < baseline.materializedByteWork(),
                        "linear plan uses less byte materialization");
        }
        M3FileSource crlf = M3FileSource.fromBytes(utf8("one\r\ntwo"));
        var changed = M3FileEditBatch.on(crlf).replaceBytes(0, 3, utf8("ONE")).apply();
        refuse(IllegalArgumentException.class,
                () -> M3FileEditFrames.planLinearLf(crlf, changed));
        M3FileSource clean = M3FileSource.fromBytes(utf8("a😀b\nc"));
        var fractured = M3FileEditBatch.on(clean).replaceBytes(2, 3, utf8("z")).apply();
        refuse(IllegalArgumentException.class,
                () -> M3FileEditFrames.planLinearLf(clean, fractured));
        var edited = M3FileEditBatch.on(clean).replaceBytes(0, 1, utf8("x")).apply();
        refuse(IllegalArgumentException.class,
                () -> M3FileEditFrames.planLinearLf(clean, edited, 1));
        var invalidInserted = M3FileEditBatch.on(clean).replaceBytes(1, 1,
                new byte[] {(byte) 0x80}).apply();
        refuse(IllegalArgumentException.class,
                () -> M3FileEditFrames.planLinearLf(clean, invalidInserted));
        var noOp = M3FileEditBatch.on(clean).apply();
        ok(M3FileEditFrames.planLinearLf(clean, noOp, 0).size() == 0,
                "no-edit image uses no buffer budget");
        byte[] large = new byte[1 << 20];
        Arrays.fill(large, (byte) 'a');
        var largeSource = M3FileSource.fromBytes(large);
        var largePlan = M3FileEditBatch.on(largeSource);
        for (int i = 1; i <= 16; i++) {
            int offset = i * 65536 - 1;
            largePlan.replaceBytes(offset, offset, utf8("x\ny"));
        }
        var largeApplied = largePlan.apply();
        var bounded = M3FileEditFrames.planLinearLf(largeSource, largeApplied, 3L << 20);
        ok(bounded.size() == 16 && bounded.materializedByteWork() < (3L << 20),
                "large file uses bounded source work, not edits times file bytes");
        refuse(IllegalArgumentException.class,
                () -> M3FileEditFrames.plan(largeSource, largeApplied, 3L << 20));
        var conservative = M3FileEditFrames.plan(largeSource, largeApplied, Long.MAX_VALUE);
        for (int i = 0; i < bounded.size(); i++)
            ok(bounded.frame(i).equals(conservative.frame(i)),
                    "large-file incremental coordinates equal full replay");
    }

    public static void main(String[] args) {
        var original = M3FileSource.fromBytes(utf8("A\r\nβ\n😀\rZ"));
        var edit = M3FileEditBatch.on(original)
                .replaceUtf16(0, 1, "B\n")
                .replaceUtf16(4, 5, "Ω\r\n")
                .replaceUtf16(original.utf16Length(), original.utf16Length(), "!\n")
                .apply();
        compare(original, edit);
        var unchanged = M3FileEditBatch.on(original).apply();
        compare(original, unchanged);
        ok(M3FileEditFrames.plan(original, unchanged).materializedByteWork() == 0,
                "empty frame stream has no intermediate copy");
        var seam = M3FileSource.fromBytes(utf8("A\rB\nC"));
        compare(seam, M3FileEditBatch.on(seam).replaceUtf16(2, 2, "\n").apply());
        var seam2 = M3FileSource.fromBytes(utf8("A\r\nB"));
        compare(seam2, M3FileEditBatch.on(seam2).replaceUtf16(2, 3, "").apply());
        refuse(IllegalArgumentException.class, () -> M3FileEditFrames.plan(original, edit, 1));
        refuse(IllegalStateException.class, () -> M3FileEditFrames.plan(seam, edit));
        var codepoint = M3FileSource.fromBytes(utf8("a😀b"));
        var split = M3FileEditBatch.on(codepoint).replaceBytes(3, 4, utf8("x")).apply();
        refuse(IllegalArgumentException.class, () -> M3FileEditFrames.plan(codepoint, split));
        Random random = new Random(0x53a17ecL);
        String[] inserts = {"", "Q", "Ω", "😀", "\n", "\r", "\r\n", "\nX"};
        for (int iteration = 0; iteration < 120; iteration++) {
            StringBuilder phrase = new StringBuilder();
            String[] atoms = {"a", "β", "😀", "\n", "\r", "z"};
            for (int i = 0; i < 20 + random.nextInt(20); i++)
                phrase.append(atoms[random.nextInt(atoms.length)]);
            byte[] raw = utf8(phrase.toString());
            List<Integer> boundaries = new ArrayList<>();
            for (int i = 0; i < raw.length; i++)
                if ((raw[i] & 0xc0) != 0x80) boundaries.add(i);
            boundaries.add(raw.length);
            M3FileSource image = M3FileSource.fromBytes(raw);
            M3FileEditBatch edits = M3FileEditBatch.on(image);
            int prevBoundary = -1;
            for (int j = 0; j < 5; j++) {
                int startIndex = Math.min(boundaries.size() - 1, prevBoundary + 2 + random.nextInt(2));
                if (startIndex == boundaries.size() - 1) break;
                int endIndex = Math.min(boundaries.size() - 1, startIndex + random.nextInt(2));
                edits.replaceBytes(boundaries.get(startIndex), boundaries.get(endIndex),
                        utf8(inserts[random.nextInt(inserts.length)]));
                prevBoundary = endIndex;
            }
            compare(image, edits.apply());
        }
        linearLfOracle();
        System.out.println("M3_FILE_EDIT_FRAMES_PASS checks=" + checks);
    }
}
