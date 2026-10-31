/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary jdk.m3.logging.M3StructuredFormatter atom passes after atomize: JIT (each atom compiled by C2 in its hot
 *          caller, none refused for size) and patternize (each atom in exactly one pattern of a closed catalogue).
 * @requires vm.compiler2.enabled
 * @modules jdk.m3.logging
 * @build M3StructuredFormatterJitDriver
 * @run junit M3StructuredFormatterPassesTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.m3.logging.M3StructuredFormatter;
import org.junit.jupiter.api.Test;

class M3StructuredFormatterPassesTest {

    /** The closed pattern catalogue of the formatter's atoms: pattern -> catalogue reference. */
    private static final Map<String, String> PATTERNS = Map.of(
            "REVERSIBLE_ESCAPE_CODEC", "TSV/CSV field escaping (RFC 4180 family); LeetCode 271 Encode and Decode Strings",
            "FIXED_COLUMN_RECORD", "one record per line, fixed column order (TSV, BLLSE structured log records)");
    /** Each atom's pattern. */
    private static final Map<String, String> ATOM_PATTERNS = Map.of(
            "escape", "REVERSIBLE_ESCAPE_CODEC",
            "unescape", "REVERSIBLE_ESCAPE_CODEC",
            "row", "FIXED_COLUMN_RECORD");
    private static final Pattern DECISION = Pattern.compile(
            "M3StructuredFormatter::(\\w+) \\((\\d+) bytes\\)\\s+(.+?)\\s*$", Pattern.MULTILINE);
    private static final List<String> REFUSALS = List.of("too big", "too large", "fail");
    private static final String COMPILED_CALL = "already compiled into a big method";
    /** HotSpot's -XX:MaxInlineSize default: a method this small is inlined wherever it is called. */
    private static final int MAX_INLINE_SIZE = 35;

    private static Set<String> atoms() {
        Set<String> atoms = new TreeSet<>();
        for (Method method : M3StructuredFormatter.class.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
                atoms.add(method.getName());
            }
        }
        return atoms;
    }

    @Test
    void patternizeEveryAtomHasOnePatternOfTheCatalogue() {
        assertEquals(atoms(), ATOM_PATTERNS.keySet(), "every public static atom is classified, nothing else");
        for (Map.Entry<String, String> entry : ATOM_PATTERNS.entrySet()) {
            assertTrue(PATTERNS.containsKey(entry.getValue()), entry.getKey() + " UNKNOWN_PATTERN " + entry.getValue());
        }
        assertEquals(PATTERNS.keySet(), new TreeSet<>(ATOM_PATTERNS.values()), "every catalogue pattern is used");
    }

    @Test
    void jitEveryAtomIsCompiledByC2InItsHotCaller() throws Exception {
        assumeTrue(System.getProperty("java.vm.name", "").contains("HotSpot"), "PrintInlining is a HotSpot flag");
        List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining", "-XX:-TieredCompilation", "-Xbatch"));
        String modulePath = System.getProperty("jdk.module.path");
        if (modulePath != null) {
            command.addAll(List.of("-p", modulePath, "--add-modules", "jdk.m3.logging"));
        }
        command.addAll(List.of("-cp", System.getProperty("java.class.path"), M3StructuredFormatterJitDriver.class.getName()));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
        assertTrue(output.contains("JIT_DRIVER_DONE true"), "the driver ran to the end");
        Map<String, TreeSet<String>> decisions = new TreeMap<>();
        Map<String, Integer> bytes = new TreeMap<>();
        Matcher m = DECISION.matcher(output);
        while (m.find()) {
            decisions.computeIfAbsent(m.group(1), k -> new TreeSet<>()).add(m.group(3));
            bytes.put(m.group(1), Integer.valueOf(m.group(2)));
        }
        List<String> problems = new ArrayList<>();
        for (String atom : atoms()) {
            TreeSet<String> seen = decisions.getOrDefault(atom, new TreeSet<>());
            int size = bytes.getOrDefault(atom, -1);
            boolean inlined = seen.stream().anyMatch(d -> d.startsWith("inline"));
            boolean called = seen.contains(COMPILED_CALL);
            System.out.println("LOGJIT " + atom + "\t" + size + "\t" + (inlined ? "INLINED" : called ? "COMPILED_CALL"
                    : "NONE") + "\t" + String.join(" | ", seen));
            if (size <= MAX_INLINE_SIZE ? !inlined : !(inlined || called)) {
                problems.add(atom + " (" + size + " bytes) is not compiled into its hot caller: " + seen);
            }
            seen.stream().filter(d -> REFUSALS.stream().anyMatch(d::contains))
                    .forEach(d -> problems.add(atom + " refused: " + d));
        }
        assertEquals(List.of(), problems, "every atom is compiled by C2 in its hot caller, none refused");
    }
}
