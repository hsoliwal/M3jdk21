// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.jackson.tests;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import jdk.m3.jackson.M3AST;
import jdk.m3.jackson.M3ASTAtom;
import jdk.m3.jackson.M3JsonLimits;
import jdk.m3.jackson.M3ObjectMapper;

/**
 * Opt-in acceptance gate: exact 100,000,000-byte JSON, never committed as a fixture.
 *
 * <p>The parser under test is the existing M3ObjectMapper/M3AST, not a second JSON
 * implementation. Its representation is checked at real public API boundaries.
 * The generator uses a bounded output buffer rather than constructing a 100 MB String.
 * No timing or memory improvement is claimed until this program actually passes.
 */
public final class M3LargeJsonAcceptance {
    private static final long DEFAULT_BYTES = 100_000_000L;
    private static final byte[] PREFIX = "{\"records\":[".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PAD_PREFIX = "],\"padding\":\"".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SUFFIX = "\"}".getBytes(StandardCharsets.UTF_8);
    private static final String MESSAGE = "abcdefghijklmnopqrstuvwxyz0123456789".repeat(3);
    private static final byte[] PAD_BYTES = new byte[64 * 1024];

    private M3LargeJsonAcceptance() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("usage: M3LargeJsonAcceptance NEW_DIRECTORY [byte_count]");
        }
        long budget = args.length == 2 ? Long.parseLong(args[1]) : DEFAULT_BYTES;
        if (budget < 16_384L || budget > Integer.MAX_VALUE - 16L) {
            throw new IllegalArgumentException("byte count outside supported M3File.read range");
        }
        Path directory = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectory(directory); // requires a fresh output directory
        Path json = directory.resolve("json-scale.json");
        int records = generate(json, budget);
        require(Files.size(json) == budget, "generated input size");
        String sourceDigest = sha256(json);

        // Explicit test-only budgets, never changing library safety defaults.
        M3JsonLimits limits = new M3JsonLimits(40_000_000L, 12_000_000,
                30_000_000L, 250_000_000L, 512);
        M3ObjectMapper mapper = new M3ObjectMapper(limits);
        long start = System.nanoTime();
        M3AST ast = mapper.readAST(json, StandardCharsets.UTF_8, Math.toIntExact(budget));
        long parseNs = System.nanoTime() - start;
        M3ASTAtom root = ast.root();
        require("OBJECT".equals(root.kind()) && root.childCount() == 2, "root shape");
        M3ASTAtom recordsField = root.child(0);
        require("FIELD".equals(recordsField.kind()) && "records".equals(recordsField.name()),
                "records field");
        M3ASTAtom rows = recordsField.child(0);
        require("ARRAY".equals(rows.kind()) && rows.childCount() == records,
                "complete record count");
        checkRecord(rows, 0);
        checkRecord(rows, records >>> 1);
        checkRecord(rows, records - 1);
        require("padding".equals(root.child(1).name()), "padding field preserved");
        require(ast.atomCount() > 0 && ast.childReferenceCount() >= records,
                "indexed document retained");

        // A streamed digest verifies all emitted bytes, including field order,
        // repeated field names, integer lexemes, large padding and nested arrays.
        MessageDigest serializerDigest = MessageDigest.getInstance("SHA-256");
        long writeStart = System.nanoTime();
        try (OutputStream sink = new DigestOutputStream(OutputStream.nullOutputStream(), serializerDigest)) {
            mapper.writeValue(sink, ast);
        }
        long writeNs = System.nanoTime() - writeStart;
        String outputDigest = HexFormat.of().formatHex(serializerDigest.digest());
        require(sourceDigest.equals(outputDigest), "streaming round-trip byte digest");
        require(ast.structuralHash64() == ast.root().structuralHash64(), "structural hash consistency");

        double parseSeconds = parseNs / 1_000_000_000d;
        double writeSeconds = writeNs / 1_000_000_000d;
        String result = "M3_JSON_SCALE_PASS bytes=" + budget
                + " records=" + records
                + " atoms=" + ast.atomCount()
                + " childRefs=" + ast.childReferenceCount()
                + " sourceSha256=" + sourceDigest
                + " parseSeconds=" + parseSeconds
                + " writeSeconds=" + writeSeconds
                + " parseMBps=" + (budget / 1_000_000d / parseSeconds)
                + " heapPeakBytes=" + peakHeapBytes();
        Files.writeString(directory.resolve("RESULT.txt"), result + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
        System.out.println(result);
    }

    private static int generate(Path path, long targetBytes) throws IOException {
        Arrays.fill(PAD_BYTES, (byte) 'x');
        int count = 0;
        long written = 0;
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(path,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), 64 * 1024)) {
            out.write(PREFIX);
            written += PREFIX.length;
            while (true) {
                String record = (count == 0 ? "" : ",") + "{\"id\":" + count
                        + ",\"message\":\"" + MESSAGE
                        + "\",\"tag\":\"first\",\"tag\":\"last\",\"items\":[0,1,true,false,null]}";
                byte[] encoded = record.getBytes(StandardCharsets.UTF_8);
                if (written + encoded.length + PAD_PREFIX.length + SUFFIX.length + 1_024L
                        > targetBytes) break;
                out.write(encoded);
                written += encoded.length;
                count++;
            }
            require(count > 1, "generator row count");
            out.write(PAD_PREFIX);
            written += PAD_PREFIX.length;
            long remaining = targetBytes - written - SUFFIX.length;
            require(remaining >= 0, "generator padding geometry");
            while (remaining > 0) {
                int n = (int) Math.min(remaining, PAD_BYTES.length);
                out.write(PAD_BYTES, 0, n);
                remaining -= n;
                written += n;
            }
            out.write(SUFFIX);
            written += SUFFIX.length;
        }
        require(written == targetBytes, "exact byte geometry");
        return count;
    }

    private static void checkRecord(M3ASTAtom array, int index) {
        M3ASTAtom record = array.child(index);
        require("OBJECT".equals(record.kind()) && record.childCount() == 5,
                "record shape index=" + index);
        M3ASTAtom id = record.child(0);
        require("id".equals(id.name()) && "FIELD".equals(id.kind())
                        && Integer.toString(index).equals(id.child(0).value()),
                "record id index=" + index);
        require("message".equals(record.child(1).name())
                        && MESSAGE.equals(record.child(1).child(0).value()), "message index=" + index);
        require("tag".equals(record.child(2).name())
                        && "first".equals(record.child(2).child(0).value())
                        && "tag".equals(record.child(3).name())
                        && "last".equals(record.child(3).child(0).value()),
                "duplicate field order index=" + index);
        M3ASTAtom items = record.child(4).child(0);
        require("ARRAY".equals(items.kind()) && items.childCount() == 5,
                "nested item array index=" + index);
    }

    private static String sha256(Path input) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var bytes = Files.newInputStream(input)) {
            byte[] buffer = new byte[64 * 1024];
            for (int len; (len = bytes.read(buffer)) != -1;) {
                digest.update(buffer, 0, len);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static long peakHeapBytes() {
        long peak = 0;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP && pool.getPeakUsage() != null) {
                peak += pool.getPeakUsage().getUsed();
            }
        }
        return peak;
    }

    private static void require(boolean assertion, String detail) {
        if (!assertion) throw new AssertionError("M3 JSON 100MB acceptance: " + detail);
    }
}
