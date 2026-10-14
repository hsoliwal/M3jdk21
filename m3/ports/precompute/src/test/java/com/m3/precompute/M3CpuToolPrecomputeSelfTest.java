// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Executable Java 21 oracle with no JUnit dependency; Maven invokes the JUnit adapter. */
public final class M3CpuToolPrecomputeSelfTest {
    private static int checks;
    private M3CpuToolPrecomputeSelfTest() {}

    public static void main(String[] args) {
        runAll();
        System.out.println("M3 CPU tool plan/image/runtime PASS checks=" + checks);
    }

    public static void runAll() {
        checks = 0;
        precomputedLiteral();
        digestAndOperations();
        immutableDagsAndImages();
        errorsAndBudget();
        compilerInput();
        compilerFileContract();
        if (!Boolean.getBoolean("m3.cpu.precompute.coreOnly")) lexicalNativeBoundary();
    }

    private static M3CpuToolPlan.Step node(String id, String op, String constant, String... deps) {
        return new M3CpuToolPlan.Step(id, op, List.of(deps), constant);
    }

    private static M3CpuToolPlan.Compiled plan(M3CpuToolPlan.Step... steps) {
        return M3CpuToolPlan.compile(List.of(steps), steps[steps.length - 1].id());
    }

    private static void check(boolean success, String reason) {
        checks++;
        if (!success) throw new AssertionError(reason);
    }

    private static void reject(Runnable runnable, String reason) {
        checks++;
        try { runnable.run(); }
        catch (IllegalArgumentException | IllegalStateException expected) { return; }
        throw new AssertionError("accepted: " + reason);
    }

    private static void precomputedLiteral() {
        for (String literal : List.of("a", "abab", "aaaa", "é", "😀", "Δelta")) {
            var compiled = plan(node("find", "search.literal", literal));
            byte[] pattern = literal.getBytes(StandardCharsets.UTF_8);
            Random random = new Random(0x5A17L + pattern.length);
            for (int iteration = 0; iteration < 2000; iteration++) {
                byte[] input = new byte[random.nextInt(192)];
                random.nextBytes(input);
                if (input.length >= pattern.length && iteration % 3 == 0) {
                    System.arraycopy(pattern, 0, input, random.nextInt(input.length - pattern.length + 1),
                            pattern.length);
                }
                byte[] expected = naivePositions(input, pattern);
                check(Arrays.equals(expected, M3CpuToolRuntime.execute(compiled, input).bytes()),
                        "seeded overlapping UTF-8 KMP differential");
            }
        }
        var overlapping = plan(node("match", "search.literal", "aa"));
        check(Arrays.equals(new int[]{0, 1, 2}, decodePositions(
                M3CpuToolRuntime.execute(overlapping, "aaaa".getBytes(StandardCharsets.UTF_8)).bytes())),
                "overlap preserves all positions");
    }

    private static byte[] naivePositions(byte[] input, byte[] pattern) {
        ByteBuffer buffer = ByteBuffer.allocate(input.length * Integer.BYTES);
        for (int i = 0; i <= input.length - pattern.length; i++) {
            boolean equal = true;
            for (int j = 0; j < pattern.length; j++) {
                if (input[i + j] != pattern[j]) { equal = false; break; }
            }
            if (equal) buffer.putInt(i);
        }
        return Arrays.copyOf(buffer.array(), buffer.position());
    }

    private static int[] decodePositions(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int[] positions = new int[bytes.length / Integer.BYTES];
        for (int i = 0; i < positions.length; i++) positions[i] = buffer.getInt();
        return positions;
    }

    private static void digestAndOperations() {
        byte[] input = {0, (byte) 255, 1, 1, 0, 42};
        var sha = M3CpuToolRuntime.execute(plan(node("sha", "hash.sha256", "")), input);
        check(Arrays.equals(sha.bytes(), M3CpuToolPlan.sha256().digest(input)), "binary SHA-256 oracle");
        check(sha.steps() == 1 && sha.byteCount() == 32, "receipt stage/bytes");
        byte[] visible = sha.bytes(); visible[0] ^= 127;
        check(Arrays.equals(sha.bytes(), M3CpuToolPlan.sha256().digest(input)), "defensive output");
        var count = M3CpuToolRuntime.execute(plan(node("length", "bytes.length", "")), input);
        check(ByteBuffer.wrap(count.bytes()).getLong() == input.length, "binary length");
        var hist = M3CpuToolRuntime.execute(plan(node("hist", "statistics.byte-histogram", "")), input);
        check(hist.byteCount() == 1024 && ByteBuffer.wrap(hist.bytes()).getInt(255 * 4) == 1,
                "256-bin unsigned histogram");
        var sort = M3CpuToolRuntime.execute(plan(node("sort", "ordering.bytes", "")), input);
        check(Arrays.equals(sort.bytes(), new byte[]{0, 0, 1, 1, 42, (byte) 255}), "unsigned sort");
        var unique = M3CpuToolRuntime.execute(plan(node("unique", "set.unique.bytes", "")), input);
        check(Arrays.equals(unique.bytes(), new byte[]{0, 1, 42, (byte) 255}), "unsigned distinct");
        var xor = M3CpuToolRuntime.execute(plan(node("xor", "bytes.xor", "ff")), input);
        for (int i = 0; i < input.length; i++) check((byte) (input[i] ^ (byte) 255) == xor.bytes()[i],
                "xor constant compiled once");
        var entropy = M3CpuToolRuntime.execute(plan(node("entropy", "information.entropy", "")),
                new byte[]{1, 1, 2, 2});
        check(StrictMath.abs(ByteBuffer.wrap(entropy.bytes()).getDouble() - 1.0) < 1e-12,
                "entropy 1 bit");
        byte[] uniform = M3CpuToolRuntime.execute(plan(node("zero", "information.entropy", "")),
                new byte[0]).bytes();
        check(ByteBuffer.wrap(uniform).getDouble() == 0.0, "empty entropy");
        var rle = plan(node("rle", "compression.rle.encode", ""));
        var unrle = plan(node("unrle", "compression.rle.decode", ""));
        for (int size : new int[]{0, 1, 2, 255, 256, 257, 1024}) {
            byte[] bytes = new byte[size];
            new Random(1).nextBytes(bytes);
            byte[] coded = M3CpuToolRuntime.execute(rle, bytes).bytes();
            check(Arrays.equals(bytes, M3CpuToolRuntime.execute(unrle, coded).bytes()),
                    "RLE roundtrip with unsigned counts");
        }
        byte[] longRun = new byte[511]; Arrays.fill(longRun, (byte) 39);
        byte[] encoded = M3CpuToolRuntime.execute(rle, longRun).bytes();
        check(encoded.length == 6, "RLE splits 255-byte runs");
        check(Arrays.equals(M3CpuToolRuntime.execute(unrle, encoded).bytes(), longRun),
                "RLE longest run exact");
    }

    private static void immutableDagsAndImages() {
        var left = node("left", "bytes.xor", "ff");
        var right = node("right", "bytes.identity", "");
        var concat = node("merged", "bytes.concat", "", "right", "left");
        var unique = node("unique", "set.unique.bytes", "", "merged");
        var built = M3CpuToolPlan.compile(List.of(concat, unique, left, right), "unique");
        var reordered = M3CpuToolPlan.compile(List.of(right, left, unique, concat), "unique");
        check(built.sha256().equals(reordered.sha256()), "input-order-independent plan fingerprint");
        byte[] input = {1, 3};
        var result = M3CpuToolRuntime.execute(built, input);
        check(Arrays.equals(new byte[]{1, 3, (byte) 252, (byte) 254}, result.bytes()),
                "dependency-ordered fan-in + unique");
        byte[] bytes = M3CpuToolImage.encode(built);
        var decoded = M3CpuToolImage.decode(bytes);
        check(Arrays.equals(bytes, M3CpuToolImage.encode(decoded)), "precomputed image fixed point");
        check(decoded.sha256().equals(built.sha256()), "plan SHA-256 survives image transport");
        check(Arrays.equals(result.bytes(), M3CpuToolRuntime.execute(decoded, input).bytes()),
                "image reuses exact prepared operations");
        byte[] corrupted = bytes.clone(); corrupted[9] ^= 1;
        final byte[] altered = corrupted;
        reject(() -> M3CpuToolImage.decode(altered), "image hash mutation refusal");
        corrupted = Arrays.copyOf(bytes, bytes.length - 1);
        final byte[] truncated = corrupted;
        reject(() -> M3CpuToolImage.decode(truncated), "truncated image refusal");
        byte[] extended = Arrays.copyOf(bytes, bytes.length + 1);
        reject(() -> M3CpuToolImage.decode(extended), "trailing bytes refuse");
        check(built.definitions().size() == 4, "stable source definitions");
        checks++;
        try { built.definitions().clear(); throw new AssertionError("mutable definitions"); }
        catch (UnsupportedOperationException expected) { /* immutable */ }
    }

    private static void errorsAndBudget() {
        reject(() -> M3CpuToolPlan.compile(List.of(), "none"), "empty DAG");
        reject(() -> plan(node("a", "unknown.operator", "")), "unimplemented tool refuses");
        reject(() -> plan(node("a", "search.literal", "")), "empty literal refuses");
        reject(() -> plan(node("a", "bytes.xor", "xyz")), "malformed hex refuses");
        reject(() -> plan(node("a", "bytes.identity", "extra")), "unused static argument refuses");
        reject(() -> plan(node("a", "bytes.identity", "", "missing")), "unknown dependency refuses");
        reject(() -> plan(node("a", "bytes.identity", ""), node("a", "bytes.identity", "")),
                "duplicate ID refuses");
        reject(() -> plan(node("a", "bytes.identity", "", "b"),
                node("b", "bytes.identity", "", "a")), "cycle refuses");
        reject(() -> M3CpuToolPlan.compile(List.of(node("a", "bytes.identity", ""),
                node("b", "bytes.identity", "")), "a"), "dead stage refuses");
        reject(() -> plan(node("bad/name", "bytes.identity", "")), "path-shaped ID refuses");
        reject(() -> plan(node("c", "bytes.concat", "")), "concat arity refuses");
        reject(() -> plan(node("a", "bytes.identity", "", "a")), "self cycle refuses");
        reject(() -> plan(node("a", "bytes.identity", "", "a", "b")), "extra arity refuses");
        reject(() -> plan(node("a", "search.literal", "\ud800")), "malformed surrogate refuses");
        reject(() -> M3CpuToolRuntime.execute(plan(node("a", "compression.rle.decode", "")),
                new byte[]{1}), "odd RLE bytes refuse");
        reject(() -> M3CpuToolRuntime.execute(plan(node("a", "compression.rle.decode", "")),
                new byte[]{0, 11}), "zero RLE count refuses");
        reject(() -> M3CpuToolRuntime.execute(plan(node("a", "bytes.identity", "")),
                new byte[M3CpuToolRuntime.automaticByteBudget() + 1]),
                "automatic Xmx residency refusal");
    }

    private static void compilerInput() {
        String header = M3CpuToolImageCompiler.HEADER + "\n";
        String literal = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "aba".getBytes(StandardCharsets.UTF_8));
        String text = header + "M3_CPU_TOOL_DAG_V1\tmatch\tmatch\tsearch.literal\t\t"
                + literal + "\n";
        var prepared = M3CpuToolImageCompiler.compileTsv(text);
        check(Arrays.equals(new int[]{0, 2}, decodePositions(
                M3CpuToolRuntime.execute(prepared, "ababa".getBytes(StandardCharsets.UTF_8)).bytes())),
                "build-time TSV precomputation and immutable KMP");
        check(prepared.sha256().equals(M3CpuToolImage.decode(M3CpuToolImage.encode(prepared))
                .sha256()), "compiled text/image identity");
        reject(() -> M3CpuToolImageCompiler.compileTsv("wrong\n"), "wrong tool compiler schema");
        reject(() -> M3CpuToolImageCompiler.compileTsv(header
                + "M3_CPU_TOOL_DAG_V1\tmatch\tmatch\tsearch.literal\t\t#\n"),
                "malformed base64 constant");
        reject(() -> M3CpuToolImageCompiler.compileTsv(header
                + "M3_CPU_TOOL_DAG_V1\tmatch\tmatch\tsearch.literal\t\t"
                + literal + "\n"
                + "M3_CPU_TOOL_DAG_V1\tother\tother\tbytes.length\tmatch\t\n"),
                "mixed output root");
    }

    private static void compilerFileContract() {
        java.nio.file.Path dir = null;
        try {
            dir = java.nio.file.Files.createTempDirectory("m3cpu-precompute-test-");
            java.nio.file.Path input = dir.resolve("plan.tsv");
            java.nio.file.Path output = dir.resolve("image.bin");
            String literal = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("a".getBytes(StandardCharsets.UTF_8));
            java.nio.file.Files.writeString(input, M3CpuToolImageCompiler.HEADER + "\n"
                    + "M3_CPU_TOOL_DAG_V1\tfind\tfind\tsearch.literal\t\t"
                    + literal + "\n");
            M3CpuToolImageCompiler.main(new String[]{input.toString(), output.toString()});
            check(java.nio.file.Files.exists(output), "CLI created one new output image");
            byte[] stored = java.nio.file.Files.readAllBytes(output);
            check(M3CpuToolImage.decode(stored).stages() == 1, "CLI image is readable");
            checks++;
            try { M3CpuToolImageCompiler.main(new String[]{input.toString(), output.toString()});
                throw new AssertionError("CLI overwrote previous output"); }
            catch (java.nio.file.FileAlreadyExistsException expected) { /* fail closed */ }
        } catch (java.io.IOException failure) {
            throw new AssertionError("image CLI file contract", failure);
        } finally {
            if (dir != null) {
                try (var files = java.nio.file.Files.list(dir)) {
                    files.forEach(path -> {
                        try { java.nio.file.Files.delete(path); }
                        catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
                    });
                } catch (java.io.IOException failure) { throw new AssertionError(failure); }
                try { java.nio.file.Files.delete(dir); }
                catch (java.io.IOException failure) { throw new AssertionError(failure); }
            }
        }
    }

    private static void lexicalNativeBoundary() {
        var plan = plan(node("signal", "text.code-signals", ""));
        byte[] plain = M3CpuToolRuntime.execute(plan,
                "class A { int x=1; }".getBytes(StandardCharsets.UTF_8)).bytes();
        check(plain.length == Long.BYTES, "reuse canonical Java lexical signal kernel");
        check(ByteBuffer.wrap(plain).getLong() != 0, "known Java code lexical features");
        reject(() -> M3CpuToolRuntime.execute(plan, new byte[]{(byte) 0xc3, (byte) 0x28}),
                "malformed UTF-8 refused before JNI");
        check(!Boolean.getBoolean("m3.cpu.tools.native.signals"),
                "default Java oracle requires no native device");
    }
}
