// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.jackson.tests;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import jdk.m3.jackson.M3AST;
import jdk.m3.jackson.M3ASTTokenPattern;
import jdk.m3.jackson.M3JsonLimits;
import jdk.m3.jackson.M3ObjectMapper;

/**
 * Opt-in semantic token regex acceptance on an actual >=100 MB JSON file.
 * Suitable for the structured corpus produced by bench/generate_json_100m.py.
 *
 * <p>The measured times are observations, not acceptance thresholds.
 * Independent whole-document token round-trip belongs to the companion
 * M3Json100MiBAcceptance oracle and must also pass.
 */
public final class M3ASTTokenRegex100MiB {
    private M3ASTTokenRegex100MiB() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("expected JSON fixture path");
        Path input = Path.of(args[0]).toAbsolutePath().normalize();
        if (!Files.isRegularFile(input) || Files.size(input) < 100_000_000L) {
            throw new IllegalArgumentException("regular JSON file of at least 100 MB required");
        }

        M3ObjectMapper mapper = M3ObjectMapper.withLimits(M3JsonLimits.for100MiB());
        long start = System.nanoTime();
        M3AST ast = mapper.readASTStreaming(input, StandardCharsets.UTF_8, Files.size(input));
        long parsed = System.nanoTime();
        int root = ast.rootAtomIndex();
        if (!"ARRAY".equals(ast.kindAt(root))) {
            throw new AssertionError("corpus root must be JSON array");
        }

        M3ASTTokenPattern elements = M3ASTTokenPattern.compile("[{kind:OBJECT}]+");
        if (!elements.matches(ast, root)) {
            throw new AssertionError("every fixture array element must be an object token");
        }
        long firstObject = M3ASTTokenPattern.compile("[{kind:OBJECT}]").find(ast, root);
        if (M3ASTTokenPattern.startOrdinal(firstObject) != 0
                || M3ASTTokenPattern.endOrdinal(firstObject) != 1) {
            throw new AssertionError("incorrect first indexed element span");
        }

        int rows = ast.childCountAt(root);
        if (rows < 2) throw new AssertionError("fixture must contain many object occurrences");
        int stride = Math.max(1, rows / 4096);
        M3ASTTokenPattern duplicateFields = M3ASTTokenPattern.compile(
                "[{kind:FIELD}&{name:dup}&{value:first}] "
                + "[{kind:FIELD}&{name:dup}&{value:second}]");
        M3ASTTokenPattern nestedScalars = M3ASTTokenPattern.compile(
                "[{kind:NUMBER_INT}]+ [{kind:NUMBER_FLOAT}]+ "
                + "[{kind:NULL}] [{kind:TRUE}] [{kind:FALSE}]");
        long visited = 0;
        for (int index = 0; index < rows - 1; index += stride) {
            int child = ast.arrayElementAt(root, index);
            long pair = duplicateFields.find(ast, child);
            if (M3ASTTokenPattern.startOrdinal(pair) != 1
                    || M3ASTTokenPattern.endOrdinal(pair) != 3) {
                throw new AssertionError("ordered duplicate fields lost at object " + index);
            }
            int nested = ast.objectFieldValueAt(child, "n", 0);
            if (!nestedScalars.matches(ast, nested)) {
                throw new AssertionError("nested scalar token sequence changed at " + index);
            }
            visited++;
        }

        long checked = System.nanoTime();
        Runtime runtime = Runtime.getRuntime();
        System.out.format(Locale.ROOT,
                "M3_AST_TOKEN_100M_PASS bytes=%d objects=%d atoms=%d childRefs=%d "
                + "sampledObjects=%d parseSeconds=%.3f tokenQuerySeconds=%.3f heapUsedBytes=%d%n",
                Files.size(input), rows, ast.atomCount(), ast.childReferenceCount(),
                visited, (parsed - start) / 1_000_000_000.0,
                (checked - parsed) / 1_000_000_000.0,
                runtime.totalMemory() - runtime.freeMemory());
    }
}
