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
    public M3FileEditFramesContract() { }
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
        System.out.println("M3_FILE_EDIT_FRAMES_PASS checks=" + checks);
    }
}
