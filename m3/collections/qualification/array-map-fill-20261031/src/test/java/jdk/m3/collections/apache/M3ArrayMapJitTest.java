/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Pass 2 of the atom passes (atomize, JIT, patternize): every ARRAY_MAP atom is compiled by C2 in its hot caller.
 * M3ArrayMapJitDriver runs in a forked HotSpot JVM with C2 only, synchronous compilation and -XX:+PrintInlining.
 * An atom of at most MAX_INLINE_SIZE bytecode bytes (HotSpot's always-inline bound) must be inlined; a larger atom
 * must be inlined or called as compiled code ("already compiled into a big method": its own C2 code is larger than
 * InlineSmallCode); no atom may be refused ("too big", "too large", "failed"). The decisions, each atom's bytecode
 * size and its outcome are written to target/jit/ARRAY_MAP_JIT.tsv.
 */
class M3ArrayMapJitTest {

    private static final Pattern DECISION = Pattern.compile("Atoms::(\\w+) \\((\\d+) bytes\\)\\s+(.+?)\\s*$",
            Pattern.MULTILINE);
    private static final List<String> REFUSALS = List.of("too big", "too large", "fail");
    private static final String INLINED = "inline";
    private static final String COMPILED_CALL = "already compiled into a big method";
    /** HotSpot's -XX:MaxInlineSize default: a method this small is inlined wherever it is called. */
    private static final int MAX_INLINE_SIZE = 35;

    @Test
    void everyAtomIsCompiledAndInlinedIntoItsHotCaller() throws Exception {
        assumeTrue(System.getProperty("java.vm.name", "").contains("HotSpot"), "PrintInlining is a HotSpot flag");
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        List<String> command = List.of(java.toString(), "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining",
                "-XX:-TieredCompilation", "-Xbatch", "-cp", System.getProperty("java.class.path"),
                M3ArrayMapJitDriver.class.getName());
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output.length() > 2000 ? output.substring(output.length() - 2000) : output);
        assertTrue(output.contains("JIT_DRIVER_DONE true"), "the driver ran to the end");

        Map<String, TreeSet<String>> decisions = new TreeMap<>();
        Map<String, Integer> bytes = new TreeMap<>();
        Matcher m = DECISION.matcher(output);
        while (m.find()) {
            decisions.computeIfAbsent(m.group(1), k -> new TreeSet<>()).add(m.group(3));
            bytes.put(m.group(1), Integer.valueOf(m.group(2)));
        }
        List<String> report = new ArrayList<>();
        report.add("atom\tbytecode_bytes\toutcome\tdecisions");
        List<String> problems = new ArrayList<>();
        for (Map<String, String> row : M3ArrayMapAtomContractTest.ledger()) {
            String atom = row.get("atom");
            TreeSet<String> seen = decisions.getOrDefault(atom, new TreeSet<>());
            int size = bytes.getOrDefault(atom, -1);
            boolean inlined = seen.stream().anyMatch(d -> d.startsWith(INLINED));
            boolean called = seen.contains(COMPILED_CALL);
            report.add(atom + "\t" + size + "\t" + (inlined ? "INLINED" : called ? "COMPILED_CALL" : "NONE") + "\t"
                    + String.join(" | ", seen));
            if (size <= MAX_INLINE_SIZE ? !inlined : !(inlined || called)) {
                problems.add(atom + " (" + size + " bytes) is not compiled into its hot caller: " + seen);
            }
            seen.stream().filter(d -> REFUSALS.stream().anyMatch(d::contains))
                    .forEach(d -> problems.add(atom + " refused: " + d));
        }
        Path out = Path.of("target", "jit", "ARRAY_MAP_JIT.tsv");
        Files.createDirectories(out.getParent());
        Files.write(out, report, StandardCharsets.UTF_8);
        report.forEach(line -> System.out.println("ATOMJIT " + line));
        assertEquals(List.of(), problems, "every atom is compiled by C2 in its hot caller, none refused");
    }
}
