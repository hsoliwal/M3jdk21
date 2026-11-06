// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Differential contract: the Java L-Hash equals the Python oracle ({@code cpu_lhash.py}) on every
 * corpus file for H1..H4, the H5 window count, the H5 FNV fold and the SHA-256 of the H5 hex lane.
 *
 * <p>{@code --root=DIR --oracle=TSV [--min=200]}. Rows whose file bytes changed since the oracle
 * ran are reported as drift and excluded; at least {@code --min} rows must match and zero may
 * mismatch. The six {@code cpu_lhash.py self_test} invariants run first.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/f7b9599a63da1c1264ecacdcb4c9286e055a0948/synexia-openrewrite-recipes/crates/m3-hash-family-20261030/src/test/java/com/synexia/rewrite/hash/M3LogicHashContract.java blob cb7686fb9be3; recipe m3/indexdb/recipe/hash-family-20261106/relocate.py v3
public final class M3LogicHashContract {
    private M3LogicHashContract() { }

    public static void main(String[] args) throws IOException {
        selfTest();
        Path root = Path.of(M3HashFamilyContractSupport.option(args, "--root=").orElseThrow());
        Path oracle = Path.of(M3HashFamilyContractSupport.option(args, "--oracle=").orElseThrow());
        int min = Integer.parseInt(M3HashFamilyContractSupport.option(args, "--min=").orElse("200"));
        List<String> rows = Files.readAllLines(oracle, StandardCharsets.UTF_8);
        if (!rows.get(0).equals("path\tsha256\th1\th2\th3\th4\tnTokens\th5Count\th5Fold\th5Sha256")) {
            throw new AssertionError("oracle header: " + rows.get(0));
        }
        int matched = 0;
        int drift = 0;
        int mismatches = 0;
        for (String row : rows.subList(1, rows.size())) {
            String[] cells = row.split("\t", -1);
            byte[] bytes = Files.readAllBytes(root.resolve(cells[0]));
            if (!M3ChunkSha.hex(bytes).equals(cells[1])) { drift++; continue; }
            String observed = observed(cells[0], bytes);
            if (observed.equals(row)) matched++;
            else { mismatches++; System.out.println("MISMATCH\n  oracle " + row + "\n  java   " + observed); }
        }
        System.out.printf(Locale.ROOT, "INFO lhash oracle rows=%d matched=%d mismatches=%d drift=%d%n",
                rows.size() - 1, matched, mismatches, drift);
        if (mismatches != 0 || matched < min) throw new AssertionError("oracle differential failed");
        System.out.println("M3LogicHashContract PASS");
    }

    /** The oracle row recomputed by the Java atoms. */
    static String observed(String path, byte[] bytes) {
        M3LHashTokens.Tokens tokens = M3LHashTokens.tokenize(M3LHashTokens.decode(bytes));
        long[] hash = M3LogicHash.of(tokens);
        long[] windows = M3LogicHash.windows(tokens);
        String joined = LongStream.of(windows).mapToObj(M3HashFamilyContractSupport::hex).collect(Collectors.joining(","));
        return String.join("\t", path, M3ChunkSha.hex(bytes), M3HashFamilyContractSupport.hex(hash[0]), M3HashFamilyContractSupport.hex(hash[1]),
                M3HashFamilyContractSupport.hex(hash[2]), M3HashFamilyContractSupport.hex(hash[3]), Integer.toString(tokens.size()),
                Integer.toString(windows.length), M3HashFamilyContractSupport.hex(hash[4]),
                M3ChunkSha.hex(joined.getBytes(StandardCharsets.UTF_8)));
    }

    static void selfTest() {
        check(key("def f(alpha): return alpha + 1").equals(key("def g(beta):  return beta + 1")), "rename-invariant");
        check(key("x = 1").equals(key("x = 99999")), "literal-invariant");
        check(key("class A { List<String> x; }").equals(key("class A { List<Integer> x; }")), "generics normalize");
        check(!key("const a = () => <div><Item/></div>").equals(key("const a = () => <div></div>")), "JSX structure");
        check(!key("def f(a): return a + 1").equals(key("def f(a): return a * 2")), "distinguishes logic");
        check(key("if (a) { b(); }").equals(key("if (a) { b(); }")), "deterministic");
        check(M3LogicHash.of("").length == 5 && M3LogicHash.windows(M3LHashTokens.tokenize("")).length == 0, "empty");
        System.out.println("INFO cpu_lhash.py self_test invariants hold in Java");
    }

    static String key(String source) {
        return M3HashFamilyContractSupport.hex(M3LogicHash.of(source)[0]);
    }

    static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
