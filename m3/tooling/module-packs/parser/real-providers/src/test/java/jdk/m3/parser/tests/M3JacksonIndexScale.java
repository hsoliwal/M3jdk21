// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.tests;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3ASTJacksonEvents;
import jdk.m3.parser.M3File;

/** Actual Jackson JsonParser -> source-index 100 MB acceptance; not a mock JSON tree. */
public final class M3JacksonIndexScale {
    private static final long DEFAULT_BYTES = 100_000_000L;
    private static final byte[] OPEN = "{\"rows\":[".getBytes(StandardCharsets.UTF_8);
    private static final byte[] END_ROWS = "],\"padding\":\"".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CLOSE = "\"}".getBytes(StandardCharsets.UTF_8);
    private static final String MESSAGE = "abcdefghijklmnop0123456789".repeat(7);
    private static final byte[] PADDING = new byte[64 * 1024];

    private M3JacksonIndexScale() { }

    private static int generate(Path path, long target) throws IOException {
        Arrays.fill(PADDING, (byte) 'x');
        long written = 0;
        int count = 0;
        try (OutputStream sink = new BufferedOutputStream(Files.newOutputStream(
                path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), 64 * 1024)) {
            sink.write(OPEN);
            written += OPEN.length;
            while (true) {
                String next = (count == 0 ? "" : ",") + "{\"id\":" + count
                        + ",\"tag\":\"first\",\"tag\":\"last\",\"message\":\"" + MESSAGE + "\"}";
                byte[] bytes = next.getBytes(StandardCharsets.UTF_8);
                if (written + bytes.length + END_ROWS.length + CLOSE.length + 1_024 > target) break;
                sink.write(bytes);
                written += bytes.length;
                count++;
            }
            if (count < 1) throw new IllegalArgumentException("target too small");
            sink.write(END_ROWS);
            written += END_ROWS.length;
            long pad = target - written - CLOSE.length;
            if (pad < 0) throw new IllegalArgumentException("invalid target size");
            while (pad > 0) {
                int n = (int) Math.min(pad, PADDING.length);
                sink.write(PADDING, 0, n);
                written += n;
                pad -= n;
            }
            sink.write(CLOSE);
            written += CLOSE.length;
        }
        if (written != target) throw new AssertionError("not the exact requested byte size");
        return count;
    }

    private static String digest(Path path) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.newInputStream(path)) {
            byte[] bytes = new byte[64 * 1024];
            for (int n; (n = stream.read(bytes)) >= 0;) {
                sha.update(bytes, 0, n);
            }
        }
        return HexFormat.of().formatHex(sha.digest());
    }

    private static long peakHeap() {
        long peak = 0;
        for (MemoryPoolMXBean bean : ManagementFactory.getMemoryPoolMXBeans()) {
            if (bean.getType() == MemoryType.HEAP && bean.getPeakUsage() != null) {
                peak += bean.getPeakUsage().getUsed();
            }
        }
        return peak;
    }

    private static void require(boolean condition, String reason) {
        if (!condition) throw new AssertionError("M3 Jackson indexed scale: " + reason);
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length < 1 || arguments.length > 2) {
            throw new IllegalArgumentException("NEW_DIRECTORY [exact_decimal_bytes]");
        }
        long bytes = arguments.length == 2 ? Long.parseLong(arguments[1]) : DEFAULT_BYTES;
        if (bytes < 16_384 || bytes > Integer.MAX_VALUE - 16L) {
            throw new IllegalArgumentException("size outside source array geometry");
        }
        Path work = Path.of(arguments[0]).toAbsolutePath().normalize();
        if (Files.exists(work)) throw new IOException("proof directory must not preexist");
        Files.createDirectory(work);
        Path path = work.resolve("source.json");
        int records = generate(path, bytes);
        require(Files.size(path) == bytes, "exact-size generator");
        String expectedSha = digest(path);
        M3File source = M3File.read(path, StandardCharsets.UTF_8, Math.toIntExact(bytes));
        require(source.byteSha256().orElseThrow().equals(expectedSha), "file byte SHA256 owner");
        M3ASTIngress.Limits limits = new M3ASTIngress.Limits(
                Math.toIntExact(bytes), 10_000_000, 512);
        long t0 = System.nanoTime();
        M3ASTIndex index;
        try (JsonParser parser = new JsonFactory().createParser(source.text())) {
            index = M3ASTJacksonEvents.fromJackson(source, parser, limits);
        }
        long elapsed = System.nanoTime() - t0;
        require(index.nodeCount() == 5 + (9 * records), "all ordered occurrence nodes");
        require("JSON_OBJECT".equals(index.kind(0)), "root JSON object");
        int rowField = index.firstChild(0);
        require("JSON_FIELD".equals(index.kind(rowField)), "rows field");
        int array = index.firstChild(rowField);
        require("JSON_ARRAY".equals(index.kind(array)), "rows array");
        require(index.childCount(array) == records, "all record occurrences");
        int record = index.firstChild(array);
        require("JSON_OBJECT".equals(index.kind(record))
                && index.childCount(record) == 4, "first row and four fields");
        require("JSON_FIELD".equals(index.kind(index.firstChild(record))), "id field");
        require(!index.hasExactRange(array), "do not invent source spans from JSON tokens");
        require(index.source() == source, "retain original immutable text owner");
        double seconds = elapsed / 1_000_000_000d;
        String result = "M3_AST_JACKSON_INDEX_SCALE_PASS bytes=" + bytes
                + " records=" + records + " nodes=" + index.nodeCount()
                + " sourceSha256=" + expectedSha
                + " parseIndexSeconds=" + seconds
                + " indexedMBps=" + (bytes / 1_000_000d / seconds)
                + " peakHeapBytes=" + peakHeap();
        Files.writeString(work.resolve("RESULT.txt"), result + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
        System.out.println(result);
    }
}
