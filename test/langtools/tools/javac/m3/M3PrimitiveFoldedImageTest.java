/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only.
 */
/*
 * @test
 * @summary Opt-in attributed primitive image folding, v1 stability and output guards
 * @modules jdk.compiler/com.sun.tools.javac.m3
 * @run main/othervm M3PrimitiveFoldedImageTest
 */

import com.sun.tools.javac.m3.M3PrimitiveImageCompiler;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** No support stubs: executes the actual jdk.compiler emitter. */
public final class M3PrimitiveFoldedImageTest {
    private static int checks;
    private record Value(int kind, long bits) { }
    @FunctionalInterface private interface Action { void run() throws Exception; }

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label);
    }

    private static Set<Value> read(Path image, Path slots) throws Exception {
        byte[] data = Files.readAllBytes(image);
        ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        check(b.getLong(0) == 0x4d335052494d3031L && b.getInt(8) == 1, "v1 format unchanged");
        int count = b.getInt(12);
        check(data.length == 64 + 16 * count && b.getLong(16) == data.length, "geometry");
        check(b.getLong(56) == 0, "reserved header");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data, 0, 16);
        md.update(data, 64, data.length - 64);
        String digest = HexFormat.of().formatHex(md.digest());
        check(digest.equals(HexFormat.of().formatHex(data, 24, 56)), "namespace");
        List<String> rows = Files.readAllLines(slots);
        check(rows.size() == count + 2 && rows.getFirst().equals("# M3PRIM01 SHA-256 " + digest), "sidecar identity");
        Set<Value> result = new HashSet<>();
        int oldKind = 0;
        long oldBits = 0;
        for (int slot = 0; slot < count; slot++) {
            int at = 64 + 16 * slot;
            int kind = Byte.toUnsignedInt(b.get(at));
            long bits = b.getLong(at + 8);
            check(kind >= 1 && kind <= 8, "primitive kind");
            check(kind > oldKind || (kind == oldKind && Long.compareUnsigned(bits, oldBits) > 0), "canonical unsigned order");
            for (int p = 1; p < 8; p++) check(b.get(at + p) == 0, "reserved row byte");
            check(result.add(new Value(kind, bits)), "unique typed value");
            check(rows.get(slot + 2).equals(kind + "\t" + HexFormat.of().toHexDigits(bits)
                    + "\t" + slot + "\t" + at), "constant slot/offset");
            oldKind = kind;
            oldBits = bits;
        }
        return result;
    }

    private static void emit(Path out, Path slots, boolean fold, Path... sources) throws Exception {
        ArrayList<String> args = new ArrayList<>(List.of("--out", out.toString(), "--slots", slots.toString()));
        if (fold) args.add("--fold-expressions");
        for (Path source : sources) args.add(source.toString());
        M3PrimitiveImageCompiler.main(args.toArray(String[]::new));
    }

    private static void has(Set<Value> values, int kind, long bits, String label) {
        check(values.contains(new Value(kind, bits)), label + " " + kind + ":" + Long.toUnsignedString(bits));
    }

    private static void reject(Action action, Path out, Path slots, Path source) throws Exception {
        byte[] a = Files.readAllBytes(out), b = Files.readAllBytes(slots), c = Files.readAllBytes(source);
        boolean rejected = false;
        try { action.run(); }
        catch (IllegalArgumentException | IOException expected) { rejected = true; }
        check(rejected, "input refused");
        check(Arrays.equals(a, Files.readAllBytes(out)), "image unchanged on rejection");
        check(Arrays.equals(b, Files.readAllBytes(slots)), "sidecar unchanged on rejection");
        check(Arrays.equals(c, Files.readAllBytes(source)), "source unchanged on rejection");
    }

    public static void main(String[] args) throws Exception {
        Path work = args.length == 0 ? Files.createTempDirectory("m3-folded-test-") : Path.of(args[0]);
        Files.createDirectories(work);
        Path source = work.resolve("FoldInput.java");
        StringBuilder text = new StringBuilder("""
                class FoldInput {
                  static { System.setProperty("m3.fold.must.not.execute", "BAD"); }
                  static final int DECLARED = 98 + 3;
                  static Object[] values() {
                    return new Object[] {
                      (byte)(130 + 6), (short)(32760 + 15), (char)('A' + 2),
                      1000 + 37, 1234567890123L + 47L, 1.25f * 3.0f, 1.25d * 7.0d,
                      (5 < 3) || !false, -0.0f * 2, -0.0d / 2, 0.0f / 0.0f, 0.0d / 0.0d,
                      1.0d / 0.0d, -1.0f / 0.0f, Integer.MAX_VALUE + 1,
                      Long.MAX_VALUE + 1L, (int)Double.POSITIVE_INFINITY,
                      (int)Double.NaN, 1 << 33, -16 >>> 2, -17 / 3, -17 % 3,
                      true ? 700 + 13 : 902, ~1234, ('x' == 120) && (3L < 4L)
                    };
                  }
                  static int dynamic() { return Integer.parseInt("19283746"); }
                  static int throwing() { return 1 / 0; }
                """);
        Random random = new Random(0x4d33c0deL);
        List<Value> expected = new ArrayList<>();
        for (int i = 0; i < 96; i++) {
            int a = random.nextInt(), b = random.nextInt(), c = random.nextInt();
            int shift = random.nextInt(80) - 40;
            int value = ((a ^ b) + c) >>> shift;
            text.append("static int i").append(i).append("(){ return ((")
                    .append(a).append(" ^ ").append(b).append(") + ").append(c)
                    .append(") >>> ").append(shift).append("; }\n");
            expected.add(new Value(5, Integer.toUnsignedLong(value)));
        }
        for (int i = -128; i <= 128; i++) {
            text.append("static long l").append(i + 128).append("(){ return 10000L + (")
                    .append(i - 10000).append("L); }\n");
            expected.add(new Value(6, i));
        }
        text.append("}\n");
        Files.writeString(source, text);
        Path out = work.resolve("folded.m3prim"), slots = work.resolve("folded.tsv");
        Path legacy = work.resolve("legacy.m3prim"), legacySlots = work.resolve("legacy.tsv");
        emit(legacy, legacySlots, false, source);
        Set<Value> old = read(legacy, legacySlots);
        emit(out, slots, true, source);
        Set<Value> values = read(out, slots);
        check(values.containsAll(old) && values.size() > old.size(), "additive harvesting, not default slot changes");
        check(!old.contains(new Value(5, 1037)), "regression fixture needs expression folding");
        has(values, 1, 1, "boolean constant normalization");
        has(values, 2, 136, "byte narrowing");
        has(values, 3, 32775, "short narrowing");
        has(values, 4, 67, "char normalization");
        has(values, 5, 1037, "int folded");
        has(values, 6, 1234567890170L, "long folded");
        has(values, 7, Integer.toUnsignedLong(Float.floatToRawIntBits(3.75f)), "float folded");
        has(values, 8, Double.doubleToRawLongBits(8.75d), "double folded");
        has(values, 7, 0x80000000L, "negative float zero retained");
        has(values, 8, Long.MIN_VALUE, "negative double zero retained");
        check(values.stream().anyMatch(v -> v.kind == 7 && Float.isNaN(Float.intBitsToFloat((int)v.bits))), "float NaN value");
        check(values.stream().anyMatch(v -> v.kind == 8 && Double.isNaN(Double.longBitsToDouble(v.bits))), "double NaN value");
        has(values, 5, 0x80000000L, "int overflow");
        has(values, 6, Long.MIN_VALUE, "long overflow");
        has(values, 5, 0x7fffffffL, "saturating floating cast");
        has(values, 5, 0, "NaN-to-int cast");
        has(values, 5, 713, "conditional");
        has(values, 5, Integer.toUnsignedLong(-17 / 3), "division truncates toward zero");
        has(values, 5, Integer.toUnsignedLong(-17 % 3), "negative remainder");
        for (Value value : expected) has(values, value.kind, value.bits, "seeded folded result");
        check(!values.contains(new Value(5, 19283746)), "no method-call evaluation");
        check(System.getProperty("m3.fold.must.not.execute") == null, "no application class initialization");
        check(!Files.exists(work.resolve("FoldInput.class")), "no classfile output");
        byte[] image = Files.readAllBytes(out), sidecar = Files.readAllBytes(slots);
        emit(out, slots, true, source, source);
        check(Arrays.equals(image, Files.readAllBytes(out)) && Arrays.equals(sidecar, Files.readAllBytes(slots)), "duplicate input fixed point");
        Path empty = work.resolve("Other.java");
        Files.writeString(empty, "class Other {}\n");
        emit(out, slots, true, empty, source);
        check(Arrays.equals(image, Files.readAllBytes(out)), "source order independent image");
        emit(out, slots, true, source, empty);
        check(Arrays.equals(sidecar, Files.readAllBytes(slots)), "source order independent sidecar");

        reject(() -> emit(out, out.getParent().resolve("./folded.m3prim"), true, source), out, slots, source);
        reject(() -> emit(source, slots, true, source), out, slots, source);
        reject(() -> emit(out, source, true, source), out, slots, source);
        reject(() -> emit(out, work, true, source), out, slots, source);
        reject(() -> emit(out, out.resolve("child.tsv"), true, source), out, slots, source);
        reject(() -> M3PrimitiveImageCompiler.main(new String[] {"--out", out.toString(),
                "--slots", slots.toString(), "--fold-expressions", "--fold-expressions", source.toString()}), out, slots, source);
        Path bad = work.resolve("Bad.java");
        Files.writeString(bad, "class Bad { int x = ; }\n");
        reject(() -> emit(out, slots, true, bad), out, slots, source);
        Path link = work.resolve("hardlink.java");
        try {
            Files.createLink(link, source);
            reject(() -> emit(link, slots, true, source), out, slots, source);
        } catch (UnsupportedOperationException unsupported) {
            System.out.println("SKIP_HARDLINK_UNSUPPORTED");
        }
        Path symbolic = work.resolve("symbolic.tsv");
        try {
            Files.createSymbolicLink(symbolic, slots.toAbsolutePath());
            reject(() -> emit(out, symbolic, true, source), out, slots, source);
            Path parentLink = work.resolve("linked-parent");
            Files.createSymbolicLink(parentLink, work.toAbsolutePath());
            reject(() -> emit(out, parentLink.resolve("uncreated.tsv"), true, source), out, slots, source);
        } catch (UnsupportedOperationException unsupported) {
            System.out.println("SKIP_SYMLINK_UNSUPPORTED");
        }
        check(Arrays.equals(image, Files.readAllBytes(out)) && Arrays.equals(sidecar, Files.readAllBytes(slots)), "final image intact");
        System.out.println("M3_FOLDED_IMAGE_PASS checks=" + checks + " legacyRows=" + old.size()
                + " foldedRows=" + values.size() + " image=" + out);
    }
}
