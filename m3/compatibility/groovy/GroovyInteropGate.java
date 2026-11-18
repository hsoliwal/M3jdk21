// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0

import java.io.IOException;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Differential Groovy/JDK compatibility oracle. The product Java 21 runtime and
 * Apache Groovy are NOT linked at compile time. The same pinned Groovy jar set
 * and unchanged fixtures execute under two independent Java launchers.
 *
 * This is a test tool, not an M3 runtime or a Groovy parser.
 */
public final class GroovyInteropGate {
    private static final Duration TIMEOUT = Duration.ofMinutes(2);
    private static final String PREFIX = "M3G\t";

    private GroovyInteropGate() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("selftest")) {
            selfTest();
        } else if (args.length == 3 && args[0].equals("probe")) {
            probe(Path.of(args[1]), Path.of(args[2]));
        } else if (args.length == 7 && args[0].equals("compare")) {
            compare(Path.of(args[1]), Path.of(args[2]), args[3],
                    Path.of(args[4]), Path.of(args[5]), Path.of(args[6]));
        } else {
            System.err.println("Usage: GroovyInteropGate selftest");
            System.err.println("   or: GroovyInteropGate probe <fixtures-dir> <collisions.tsv>");
            System.err.println("   or: GroovyInteropGate compare <baseline-java> <candidate-java>"
                    + " <groovy-classpath> <fixtures-dir> <collisions.tsv> <output-dir>");
            System.exit(2);
        }
    }

    private static void probe(Path fixtures, Path collisions) throws Exception {
        for (Collision item : collisionEntries(collisions)) {
            Class<?> owner = Class.forName(item.owner());
            List<String> matches = new ArrayList<>();
            for (Method method : owner.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()
                        && method.getName().equals(item.name())
                        && method.getParameterCount() == item.arity()) {
                    matches.add(descriptor(method));
                }
            }
            Collections.sort(matches);
            emit("C:" + item.owner() + "#" + item.name() + "/" + item.arity(),
                    matches.isEmpty() ? "<absent>" : String.join(";", matches));
        }
        Class<?> shellType = Class.forName("groovy.lang.GroovyShell");
        Object shell = shellType.getConstructor().newInstance();
        Method evaluate = shellType.getMethod("evaluate", String.class);
        List<Path> scripts;
        try (Stream<Path> entries = Files.list(fixtures)) {
            scripts = entries.filter(path -> path.getFileName().toString().endsWith(".groovy"))
                    .sorted().toList();
        }
        if (scripts.isEmpty()) {
            throw new IllegalArgumentException("No .groovy fixtures in " + fixtures);
        }
        for (Path script : scripts) {
            if (!Files.isRegularFile(script) || Files.isSymbolicLink(script)) {
                throw new IllegalArgumentException("Non-regular fixture: " + script);
            }
            try {
                Object value = evaluate.invoke(shell, Files.readString(script, StandardCharsets.UTF_8));
                emit("F:" + script.getFileName(), Objects.toString(value, "<null>"));
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                throw new IllegalStateException("Groovy fixture " + script.getFileName()
                        + " failed: " + cause, cause);
            }
        }
        Object groovyVersion = Class.forName("groovy.lang.GroovySystem")
                .getMethod("getVersion").invoke(null);
        emit("R:groovy-version", String.valueOf(groovyVersion));
        System.out.println("M3G_META\tjava=" + System.getProperty("java.runtime.version"));
    }

    private static void compare(Path baselineJava, Path candidateJava, String groovyClasspath,
            Path fixtures, Path collisions, Path outputDir) throws Exception {
        if (!Files.isRegularFile(baselineJava) || !Files.isRegularFile(candidateJava)) {
            throw new IllegalArgumentException("Both Java launchers must exist: "
                    + baselineJava + " and " + candidateJava);
        }
        if (groovyClasspath.isBlank()) {
            throw new IllegalArgumentException("Supply a pinned Groovy jar classpath");
        }
        Files.createDirectories(outputDir);
        Map<String, String> inputSeals = seals(fixtures, collisions, groovyClasspath);
        String baseline = launch(baselineJava, groovyClasspath, fixtures, collisions);
        check(inputSeals.equals(seals(fixtures, collisions, groovyClasspath)),
                "fixture or jar drift after baseline");
        String candidate = launch(candidateJava, groovyClasspath, fixtures, collisions);
        check(inputSeals.equals(seals(fixtures, collisions, groovyClasspath)),
                "fixture or jar drift after candidate");
        var pins = new StringBuilder();
        inputSeals.forEach((name, sha) -> pins.append(sha).append("  ").append(name).append("\n"));
        Files.writeString(outputDir.resolve("input-sha256.txt"), pins, StandardCharsets.UTF_8);
        Files.writeString(outputDir.resolve("baseline.tsv"), baseline, StandardCharsets.UTF_8);
        Files.writeString(outputDir.resolve("candidate.tsv"), candidate, StandardCharsets.UTF_8);
        Map<String, String> before = parse(baseline), after = parse(candidate);
        List<String> findings = differences(before, after);
        String report = findings.isEmpty() ? "PASS: no observable Groovy/JDK drift\n"
                : "FAIL: " + findings.size() + " Groovy/JDK differences\n"
                + String.join("\n", findings) + "\n";
        Files.writeString(outputDir.resolve("differences.txt"), report, StandardCharsets.UTF_8);
        System.out.print(report);
        if (!findings.isEmpty()) System.exit(1);
    }

    private static Map<String, String> seals(Path fixtures, Path collisions,
            String groovyClasspath) throws IOException {
        Map<String, String> sealed = new TreeMap<>();
        if (!Files.isDirectory(fixtures) || Files.isSymbolicLink(fixtures)) {
            throw new IllegalArgumentException("Missing or symlinked fixture directory: " + fixtures);
        }
        putSeal(sealed, "collisions.tsv", collisions);
        try (Stream<Path> stream = Files.list(fixtures)) {
            for (Path file : stream.filter(x -> x.getFileName().toString().endsWith(".groovy"))
                    .sorted().toList()) {
                putSeal(sealed, "fixtures/" + file.getFileName(), file);
            }
        }
        for (String jar : groovyClasspath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (jar.isBlank()) throw new IllegalArgumentException("Empty Groovy classpath entry");
            Path file = Path.of(jar);
            if (!file.getFileName().toString().endsWith(".jar")) {
                throw new IllegalArgumentException("Expected Groovy jar, not: " + jar);
            }
            putSeal(sealed, "groovy/" + file.toAbsolutePath(), file);
        }
        if (sealed.keySet().stream().noneMatch(x -> x.startsWith("fixtures/"))) {
            throw new IllegalArgumentException("No Groovy fixtures");
        }
        return sealed;
    }

    private static void putSeal(Map<String, String> result, String key, Path path)
            throws IOException {
        if (!Files.isRegularFile(path) || Files.isSymbolicLink(path)) {
            throw new IllegalArgumentException("Missing or symlinked input: " + path);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var in = Files.newInputStream(path)) {
                byte[] bytes = new byte[65536];
                for (int n; (n = in.read(bytes)) != -1;) {
                    digest.update(bytes, 0, n);
                }
            }
            result.put(key, HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static String launch(Path executable, String groovyClasspath,
            Path fixtures, Path collisions) throws Exception {
        // Do not inherit arbitrary launcher dependencies; they could shadow the
        // version-pinned Groovy jars and invalidate a differential comparison.
        var ownCodeSource = GroovyInteropGate.class.getProtectionDomain().getCodeSource();
        if (ownCodeSource == null || ownCodeSource.getLocation() == null) {
            throw new IllegalStateException("Groovy oracle code source is unavailable");
        }
        String classpath = Path.of(ownCodeSource.getLocation().toURI())
                + System.getProperty("path.separator") + groovyClasspath;
        List<String> command = List.of(executable.toAbsolutePath().toString(), "-cp", classpath,
                GroovyInteropGate.class.getName(), "probe",
                fixtures.toAbsolutePath().toString(), collisions.toAbsolutePath().toString());
        Path captured = Files.createTempFile("m3-groovy-oracle-", ".log");
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(captured.toFile()).start();
            if (!process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor();
                throw new IllegalStateException("Timed out running: " + executable);
            }
            String output = Files.readString(captured, StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Groovy/JDK probe failed with " + executable
                        + ", exit=" + process.exitValue() + ":\n" + output);
            }
            return output;
        } finally {
            Files.deleteIfExists(captured);
        }
    }

    private static List<Collision> collisionEntries(Path file) throws IOException {
        List<Collision> entries = new ArrayList<>();
        var seen = new java.util.HashSet<String>();
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (raw.isBlank() || raw.startsWith("#")) continue;
            String[] parts = raw.split("\t", -1);
            if (parts.length != 4 || !parts[2].matches("[0-9]+")) {
                throw new IllegalArgumentException("Invalid collision row: " + raw);
            }
            Collision row = new Collision(parts[0], parts[1], Integer.parseInt(parts[2]), parts[3]);
            if (!seen.add(row.owner() + "#" + row.name() + "/" + row.arity())) {
                throw new IllegalArgumentException("Duplicate collision key: " + raw);
            }
            if (row.owner().isBlank() || row.name().isBlank() || row.donor().isBlank()) {
                throw new IllegalArgumentException("Empty collision field: " + raw);
            }
            entries.add(row);
        }
        if (entries.isEmpty()) throw new IllegalArgumentException("Empty collision manifest");
        return entries;
    }

    private static String descriptor(Method method) {
        return method.getName() + "(" + String.join(",",
                Arrays.stream(method.getParameterTypes()).map(Class::getTypeName).toList())
                + "):" + method.getReturnType().getTypeName();
    }

    private static void emit(String key, String value) {
        System.out.println(PREFIX + key + "\t" + escaped(value));
    }

    private static String escaped(String text) {
        return text.replace("\\", "\\\\").replace("\t", "\\t")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    private static Map<String, String> parse(String input) {
        Map<String, String> rows = new TreeMap<>();
        for (String raw : input.split("\n")) {
            if (!raw.startsWith(PREFIX)) continue;
            String[] parts = raw.split("\t", 3);
            if (parts.length != 3 || parts[1].isBlank()
                    || rows.putIfAbsent(parts[1], parts[2]) != null) {
                throw new IllegalArgumentException("Malformed/duplicate oracle row: " + raw);
            }
        }
        if (rows.isEmpty()) throw new IllegalArgumentException("No valid oracle rows");
        return rows;
    }

    private static List<String> differences(Map<String, String> before,
            Map<String, String> after) {
        var all = new java.util.TreeSet<String>(before.keySet());
        all.addAll(after.keySet());
        List<String> changed = new ArrayList<>();
        for (String key : all) {
            if (!Objects.equals(before.get(key), after.get(key))) {
                changed.add(key + " baseline=" + Objects.toString(before.get(key), "<missing>")
                        + " candidate=" + Objects.toString(after.get(key), "<missing>"));
            }
        }
        return changed;
    }

    private static void selfTest() throws Exception {
        Map<String, String> parsed = parse("noise\nM3G\tF:one\thello\nM3G_META\tignored\n");
        check(parsed.size() == 1 && "hello".equals(parsed.get("F:one")), "row parser");
        check(differences(parsed, parsed).isEmpty(), "fixed point");
        check(differences(parsed, Map.of("F:one", "changed")).size() == 1,
                "semantic drift rejection");
        check(differences(parsed, Map.of("F:other", "hello")).size() == 2,
                "missing/added fixture rejection");
        expectInvalid(() -> parse("M3G\tF:x\ta\nM3G\tF:x\tb\n"));
        expectInvalid(() -> parse("M3G\tF:x\n"));
        check(escaped("a\\b\tc\nd").equals("a\\\\b\\tc\\nd"), "escaping");
        Path fixture = Files.createTempFile("m3-groovy-collision-selftest", ".tsv");
        try {
            Files.writeString(fixture, "java.lang.String\tcapitalize\t0\tGDK\n");
            check(collisionEntries(fixture).size() == 1, "manifest parser");
            Files.writeString(fixture, "java.lang.String\tcapitalize\t0\tGDK\n"
                    + "java.lang.String\tcapitalize\t0\tGDK\n");
            expectInvalid(() -> collisionEntries(fixture));
        } finally {
            Files.deleteIfExists(fixture);
        }
        System.out.println("M3_GROOVY_GATE_SELFTEST_PASS checks=9");
    }

    private static void check(boolean success, String label) {
        if (!success) throw new AssertionError("gate: " + label);
    }

    private static void expectInvalid(ThrowingRunnable action) throws Exception {
        try {
            action.run();
            throw new AssertionError("expected invalid input to be refused");
        } catch (IllegalArgumentException expected) {
            // Expected refusal.
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private record Collision(String owner, String name, int arity, String donor) { }
}
