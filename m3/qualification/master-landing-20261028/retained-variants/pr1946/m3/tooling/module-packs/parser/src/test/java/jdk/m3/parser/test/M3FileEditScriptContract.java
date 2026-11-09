/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import jdk.m3.parser.M3File;

/** Byte-exact, point-exact validation of sequential source-bound parser edits. */
public final class M3FileEditScriptContract {
    private static final URI SOURCE = URI.create("memory:/original-utf8-edit-script.json");
    private static int checks;
    private static int trials;

    private M3FileEditScriptContract() { }

    public static void main(String[] args) throws Exception {
        independentNonAsciiEdits();
        unchangedAndCoarse();
        strictSourceAndResourceRefusal();
        seededByteAndPointReconstruction();
        System.out.println("M3_FILE_EDIT_SCRIPT_PASS checks=" + checks + " trials=" + trials);
    }

    private static M3File read(String text) throws Exception {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        return M3File.read(SOURCE, new ByteArrayInputStream(bytes),
                StandardCharsets.UTF_8, bytes.length);
    }

    private static void independentNonAsciiEdits() throws Exception {
        String before = "{\n  \"fixedα\": 100,\n  \"a\": \"old😀\",\n  \"fixedβ\": 200,\n"
                + "  \"b\": \"old\",\n  \"fixedγ\": 300\n}\n";
        String after = "{\n  \"fixedα\": 100,\n  \"a\": \"new🐱\",\n  \"fixedβ\": 200,\n"
                + "  \"b\": \"more\",\n  \"fixedγ\": 300\n}\n";
        M3File old = read(before), now = read(after);
        M3File.Utf8EditScript script = now.utf8InputEditScriptSince(old, 4096, 100, 20);
        check(script.stepCount() == 2 && !script.coarse(), "two edit operations in reverse source order");
        check(script.step(0).inputEdit().startByte() > script.step(1).inputEdit().startByte(),
                "highest byte edit must run first");
        check(script.primitiveBytes() == 2L * 11 * Integer.BYTES,
                "edit script retains only primitive lanes, not per-edit heap objects");
        check(script.uri().equals(SOURCE)
                && script.previousByteSha256().equals(old.byteSha256().orElseThrow())
                && script.currentByteSha256().equals(now.byteSha256().orElseThrow()),
                "immutable source identity binds both byte observations");
        check(script.rawDigestComparison() == M3File.RawDigestComparison.SHA256_DIFFERENT,
                "original UTF-8 byte hashes remain independent of parser claims");
        reconstruct(old, now, script);
    }

    private static void unchangedAndCoarse() throws Exception {
        M3File old = read("😀\r\nα\n"), same = read("😀\r\nα\n");
        M3File.Utf8EditScript unchanged = same.utf8InputEditScriptSince(old, 100, 10, 1);
        check(unchanged.stepCount() == 0 && unchanged.primitiveBytes() == 0
                && !unchanged.coarse(), "identical strict UTF-8 observations emit no input edits");
        check(unchanged.rawDigestComparison() == M3File.RawDigestComparison.SHA256_SAME,
                "identical byte observations have exact SHA status");
        expect(IndexOutOfBoundsException.class, () -> unchanged.step(0));
        M3File before = read("front\nunique-A\nold1\nunique-B\nold2\nunique-C\ntail\n");
        M3File after = read("front\nunique-A\nnew1\nunique-B\nnew2\nunique-C\ntail\n");
        M3File.Utf8EditScript coarse = after.utf8InputEditScriptSince(before, 200, 30, 1);
        check(coarse.stepCount() == 1 && coarse.coarse(),
                "exhausted hunk budget preserves one explicit conservative envelope");
        reconstruct(before, after, coarse);
    }

    private static void strictSourceAndResourceRefusal() throws Exception {
        M3File old = read("first\nretain A\nold\nretain B\nlast\n");
        M3File now = read("first\nretain A\nnew\nretain B\nlast\n");
        expect(IllegalArgumentException.class, () -> now.utf8InputEditScriptSince(old, 1, 20, 5));
        expect(IllegalArgumentException.class, () -> now.utf8InputEditScriptSince(old, 1024, 0, 5));
        expect(IllegalArgumentException.class, () -> now.utf8InputEditScriptSince(old, 1024, 20, 0));
        expect(IllegalArgumentException.class, () -> now.utf8InputEditScriptSince(old, 1024, 20, Integer.MAX_VALUE));
        expect(IllegalArgumentException.class, () -> now.utf8InputEditScriptSince(old, 1024, 1, 5));
        expect(IllegalArgumentException.class, () -> now.utf8InputEditScriptSince(
                M3File.of(URI.create("memory:/other.json"), old.text()), 1024, 20, 5));
        expect(IllegalArgumentException.class, () -> now.utf8InputEditScriptSince(
                M3File.of(SOURCE, old.text()), 1024, 20, 5));
        expect(IllegalArgumentException.class, () -> M3File.of(SOURCE, now.text())
                .utf8InputEditScriptSince(old, 1024, 20, 5));

        M3File original = read("head\nkeep-1\nremove-a\nremove-b\nremove-c\n"
                + "keep-2\nshort\nkeep-3\nend");
        M3File target = read("head\nkeep-1\nreplacement\nkeep-2\n"
                + "big-1\nbig-2\nbig-3\nbig-4\nkeep-3\nend");
        check(original.utf8Offsets().orElseThrow().lfLineCount(10) <= 10
                && target.utf8Offsets().orElseThrow().lfLineCount(10) <= 10,
                "both immutable endpoints fit the limited line budget");
        M3File.Utf8Hunks hunks = target.utf8ChangeHunksSince(original, 4096, 10, 10);
        check(hunks.hunkCount() == 2 && !hunks.coarse(),
                "resource fixture genuinely contains two exact separated hunks");
        expect(IllegalArgumentException.class,
                () -> target.utf8InputEditScriptSince(original, 4096, 10, 10));
        reconstruct(original, target, target.utf8InputEditScriptSince(original, 4096, 20, 10));
    }

    private static void seededByteAndPointReconstruction() throws Exception {
        Random random = new Random(0x7ee511abL);
        String[] tokens = {"", "A", "b", "α", "猫", "🐱", "🧪", "\r", "\n", "\r\n",
                "\t", "one", "fixed-1\n", "fixed-2\r\n", "longer-string"};
        for (int trial = 0; trial < 3500; trial++) {
            StringBuilder old = new StringBuilder(), current = new StringBuilder();
            int parts = random.nextInt(32);
            for (int i = 0; i < parts; i++) {
                String token = tokens[random.nextInt(tokens.length)];
                old.append(token);
                int choice = random.nextInt(8);
                if (choice == 0) current.append(tokens[random.nextInt(tokens.length)]);
                else if (choice == 1) current.append(token).append(tokens[random.nextInt(tokens.length)]);
                else if (choice != 2) current.append(token);
            }
            M3File a = read(old.toString()), b = read(current.toString());
            M3File.Utf8EditScript script = b.utf8InputEditScriptSince(a, 4096, 4096, 20);
            check(script.stepCount() <= 20, "bounded script retains no more than maxHunks");
            reconstruct(a, b, script);
            trials++;
        }
    }

    /** Reference splice oracle independent of the source correspondence builder. */
    private static void reconstruct(M3File old, M3File now, M3File.Utf8EditScript script) throws Exception {
        byte[] intermediate = old.text().getBytes(StandardCharsets.UTF_8);
        byte[] target = now.text().getBytes(StandardCharsets.UTF_8);
        int lastStart = Integer.MAX_VALUE;
        for (int i = 0; i < script.stepCount(); i++) {
            M3File.Utf8EditStep step = script.step(i);
            M3File.Utf8InputEdit edit = step.inputEdit();
            check(edit.startByte() <= edit.oldEndByte()
                    && edit.oldEndByte() <= intermediate.length,
                    "sequential old positions are inside current intermediate byte source");
            check(edit.startByte() < lastStart, "steps strictly descend source byte offsets");
            lastStart = edit.startByte();
            check(step.currentStartByte() >= 0 && step.currentEndByte() <= target.length,
                    "insertion comes from exact target generation byte range");
            M3File intermediateSource = M3File.read(SOURCE, new ByteArrayInputStream(intermediate),
                    StandardCharsets.UTF_8, intermediate.length);
            M3File.Utf8Offsets coordinates = intermediateSource.utf8Offsets().orElseThrow();
            check(coordinates.pointAt(edit.startByte(), 4096).equals(edit.startPoint()),
                    "start point is valid in the intermediate source receiving this edit");
            check(coordinates.pointAt(edit.oldEndByte(), 4096).equals(edit.oldEndPoint()),
                    "old end point is valid in the intermediate source receiving this edit");

            int removed = edit.oldEndByte() - edit.startByte();
            int inserted = step.currentEndByte() - step.currentStartByte();
            byte[] updated = new byte[intermediate.length - removed + inserted];
            System.arraycopy(intermediate, 0, updated, 0, edit.startByte());
            System.arraycopy(target, step.currentStartByte(), updated, edit.startByte(), inserted);
            System.arraycopy(intermediate, edit.oldEndByte(), updated,
                    edit.startByte() + inserted, intermediate.length - edit.oldEndByte());
            check(edit.newEndByte() == edit.startByte() + inserted,
                    "new end offset is intermediate-relative, not final-target absolute");
            M3File updatedSource = M3File.read(SOURCE, new ByteArrayInputStream(updated),
                    StandardCharsets.UTF_8, updated.length);
            check(updatedSource.utf8Offsets().orElseThrow().pointAt(edit.newEndByte(), 4096)
                    .equals(edit.newEndPoint()),
                    "new end point is valid in the actual intermediate result");
            intermediate = updated;
        }
        check(Arrays.equals(intermediate, target),
                "sequential edits produce exact target UTF-8 bytes, not merely equal decoded text");
    }

    private static void check(boolean value, String explanation) {
        checks++;
        if (!value) throw new AssertionError(explanation);
    }

    @FunctionalInterface private interface Action { void run() throws Exception; }

    private static void expect(Class<? extends Throwable> expected, Action action) throws Exception {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("unexpected refusal type: " + failure, failure);
        }
        throw new AssertionError("expected refusal: " + expected.getSimpleName());
    }
}
