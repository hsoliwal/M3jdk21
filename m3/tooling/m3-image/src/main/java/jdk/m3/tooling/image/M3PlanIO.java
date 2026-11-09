/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Deterministic image-plan serialization. */
public final class M3PlanIO {
    private static final String INPUT_HEADER = "input_id\tlogical_name\tkind\tsha256";
    private static final String CLASS_HEADER = "class_name\tinput_id\tentry\trole\thot_count\tstartup_rank\tsha256\tfeatures";
    private static final String FEATURE_HEADER = "owner_class\tfeature\tdetail\tresolved";
    private static final Set<String> SEMANTIC_EXCLUDED = Set.of(
            "bindings.properties", "semantic-manifest.tsv", "PLAN_SHA256",
            "jdeps.stdout.log", "jdeps.stderr.log");

    private M3PlanIO() {
    }

    /** Writes and returns a sealed plan. */
    public static M3ImagePlan write(Path output, String mainClass, M3Mode mode, M3JdkTools tools,
                                    M3ClassPathScanner.Result scan, M3ReachabilityAnalyzer.Result reachability,
                                    M3ModuleResolver.Result modules, M3Profile profile) throws IOException {
        Path directory = M3Files.freshDirectory(output);
        M3Files.writeUtf8(directory.resolve("plan.properties"),
                "schema=1\n"
                        + "javaRelease=21\n"
                        + "mainClass=" + propertyEscape(mainClass) + "\n"
                        + "mode=" + mode.name() + "\n"
                        + "javaRuntimeIdentity=" + tools.runtimeIdentity() + "\n"
                        + "moduleFallback=" + modules.fallback() + "\n");

        StringBuilder inputs = new StringBuilder(INPUT_HEADER).append('\n');
        StringBuilder bindings = new StringBuilder();
        for (M3ClassPathScanner.Input input : scan.inputs()) {
            inputs.append(input.id()).append('\t').append(tsvEscape(input.logicalName())).append('\t')
                    .append(input.kind()).append('\t').append(input.sha256()).append('\n');
            bindings.append(input.id()).append('=').append(propertyEscape(input.path().toString())).append('\n');
        }
        M3Files.writeUtf8(directory.resolve("inputs.tsv"), inputs.toString());
        M3Files.writeUtf8(directory.resolve("bindings.properties"), bindings.toString());

        StringBuilder classRows = new StringBuilder(CLASS_HEADER).append('\n');
        for (Map.Entry<String, M3ClassInfo> entry : scan.classes().entrySet()) {
            M3ClassInfo info = entry.getValue();
            String featureNames = info.features().stream().map(Enum::name)
                    .collect(java.util.stream.Collectors.joining(","));
            classRows.append(info.binaryName()).append('\t').append(info.inputId()).append('\t')
                    .append(tsvEscape(info.entryName())).append('\t')
                    .append(reachability.roles().get(info.binaryName()).name()).append('\t')
                    .append(profile.hotCount(info.binaryName())).append('\t')
                    .append(profile.startupRank(info.binaryName())).append('\t')
                    .append(info.sha256()).append('\t').append(featureNames).append('\n');
        }
        M3Files.writeUtf8(directory.resolve("classes.tsv"), classRows.toString());

        StringBuilder featureRows = new StringBuilder(FEATURE_HEADER).append('\n');
        for (M3ReachabilityAnalyzer.Feature feature : reachability.features()) {
            featureRows.append(feature.ownerClass()).append('\t').append(feature.kind().name()).append('\t')
                    .append(tsvEscape(feature.detail())).append('\t').append(feature.resolved()).append('\n');
        }
        M3Files.writeUtf8(directory.resolve("features.tsv"), featureRows.toString());
        M3Files.writeUtf8(directory.resolve("roots.txt"), lines(reachability.roots()));
        M3Files.writeUtf8(directory.resolve("modules.txt"), lines(modules.modules()));
        M3ProfileIO.write(directory.resolve("profile.tsv"), profile);
        M3ProfileIO.writeLoadedClasses(directory.resolve("loaded-classes.txt"), profile);
        M3Files.writeUtf8(directory.resolve("jdeps.stdout.log"), modules.stdout());
        M3Files.writeUtf8(directory.resolve("jdeps.stderr.log"), modules.stderr());

        List<String> semanticRows = M3Files.manifest(directory, SEMANTIC_EXCLUDED);
        M3Files.writeUtf8(directory.resolve("semantic-manifest.tsv"), String.join("", semanticRows));
        String planHash = M3Hashing.hashRows(semanticRows);
        M3Files.writeUtf8(directory.resolve("PLAN_SHA256"), planHash + "\n");
        return new M3ImagePlan(directory, planHash, mainClass, mode, tools.runtimeIdentity(), scan.inputs(),
                scan.classes(), reachability.roles(), reachability.features(), reachability.roots(),
                modules.modules(), profile, modules.fallback());
    }

    /** Reads and verifies a plan. */
    public static M3ImagePlan read(Path directory) throws IOException {
        Path root = directory.toAbsolutePath().normalize();
        verifySemanticManifest(root);
        Properties properties = readProperties(root.resolve("plan.properties"));
        String planHash = Files.readString(root.resolve("PLAN_SHA256"), StandardCharsets.UTF_8).trim();
        M3Hashing.requireSha256(planHash, "PLAN_SHA256");
        List<M3ClassPathScanner.Input> inputs = readInputs(root);
        Map<String, M3ClassInfo> classes = new TreeMap<>();
        Map<String, M3ClassRole> roles = new TreeMap<>();
        readClasses(root, inputs, classes, roles);
        Set<M3ReachabilityAnalyzer.Feature> features = readFeatures(root);
        Set<String> roots = readLines(root.resolve("roots.txt"));
        Set<String> modules = readLines(root.resolve("modules.txt"));
        M3Profile profile = M3ProfileIO.read(root.resolve("profile.tsv"));
        return new M3ImagePlan(root, planHash, properties.getProperty("mainClass"),
                M3Mode.valueOf(properties.getProperty("mode")), properties.getProperty("javaRuntimeIdentity"),
                inputs, classes, roles, features, roots, modules, profile,
                Boolean.parseBoolean(properties.getProperty("moduleFallback", "false")));
    }

    /** Verifies the semantic manifest and plan root. */
    public static void verifySemanticManifest(Path directory) throws IOException {
        Path root = directory.toAbsolutePath().normalize();
        List<String> expected = Files.readAllLines(root.resolve("semantic-manifest.tsv"), StandardCharsets.UTF_8);
        List<String> normalizedExpected = expected.stream().filter(line -> !line.isEmpty())
                .map(line -> line + "\n").toList();
        Set<String> paths = new TreeSet<>();
        for (String row : normalizedExpected) {
            String[] fields = row.substring(0, row.length() - 1).split("\t", -1);
            if (fields.length != 2 || !fields[0].matches("[0-9a-f]{64}")) {
                throw new IOException("Invalid semantic manifest row: " + row);
            }
            if (!paths.add(fields[1])) {
                throw new IOException("Duplicate semantic manifest path: " + fields[1]);
            }
            Path file = safeResolve(root, fields[1]);
            if (!Files.isRegularFile(file)) {
                throw new IOException("Missing semantic plan file: " + fields[1]);
            }
            String actual = M3Hashing.sha256(file);
            if (!actual.equals(fields[0])) {
                throw new IOException("Plan file hash mismatch: " + fields[1]);
            }
        }
        List<String> actualRows = M3Files.manifest(root, SEMANTIC_EXCLUDED);
        if (!actualRows.equals(normalizedExpected)) {
            throw new IOException("Semantic manifest file set differs from current plan");
        }
        String actualPlan = M3Hashing.hashRows(actualRows);
        String expectedPlan = Files.readString(root.resolve("PLAN_SHA256"), StandardCharsets.UTF_8).trim();
        M3Hashing.requireSha256(expectedPlan, "PLAN_SHA256");
        if (!actualPlan.equals(expectedPlan)) {
            throw new IOException("PLAN_SHA256 mismatch: expected " + expectedPlan + " actual " + actualPlan);
        }
    }

    private static List<M3ClassPathScanner.Input> readInputs(Path root) throws IOException {
        Properties bindings = readProperties(root.resolve("bindings.properties"));
        List<String> rows = Files.readAllLines(root.resolve("inputs.tsv"), StandardCharsets.UTF_8);
        requireHeader(rows, INPUT_HEADER, "inputs.tsv");
        List<M3ClassPathScanner.Input> inputs = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            if (rows.get(index).isEmpty()) {
                continue;
            }
            String[] fields = rows.get(index).split("\t", -1);
            if (fields.length != 4) {
                throw new IOException("Invalid inputs.tsv row " + (index + 1));
            }
            String expectedId = "cp" + String.format(java.util.Locale.ROOT, "%04d", inputs.size());
            if (!fields[0].equals(expectedId)) {
                throw new IOException("Input ids must be contiguous plan order; expected " + expectedId
                        + " at row " + (index + 1));
            }
            String bound = bindings.getProperty(fields[0]);
            if (bound == null) {
                throw new IOException("Missing binding for " + fields[0]);
            }
            Path path = Path.of(bound).toAbsolutePath().normalize();
            if (!M3Hashing.hashPath(path).equals(fields[3])) {
                throw new IOException("Input drift for " + fields[0] + ": " + path);
            }
            inputs.add(new M3ClassPathScanner.Input(fields[0], path, tsvUnescape(fields[1]), fields[2], fields[3]));
        }
        return inputs;
    }

    private static void readClasses(Path root, List<M3ClassPathScanner.Input> inputs,
                                    Map<String, M3ClassInfo> classes, Map<String, M3ClassRole> roles)
            throws IOException {
        M3ClassPathScanner.Result rescan = M3ClassPathScanner.scan(inputs.stream().map(M3ClassPathScanner.Input::path).toList());
        List<String> rows = Files.readAllLines(root.resolve("classes.tsv"), StandardCharsets.UTF_8);
        requireHeader(rows, CLASS_HEADER, "classes.tsv");
        for (int index = 1; index < rows.size(); index++) {
            if (rows.get(index).isEmpty()) {
                continue;
            }
            String[] fields = rows.get(index).split("\t", -1);
            if (fields.length != 8) {
                throw new IOException("Invalid classes.tsv row " + (index + 1));
            }
            M3ClassInfo info = rescan.classes().get(fields[0]);
            if (info == null || !info.inputId().equals(fields[1]) || !info.entryName().equals(tsvUnescape(fields[2]))
                    || !info.sha256().equals(fields[6])) {
                throw new IOException("Class input drift for " + fields[0]);
            }
            if (classes.putIfAbsent(fields[0], info) != null) {
                throw new IOException("Duplicate class row for " + fields[0]);
            }
            roles.put(fields[0], M3ClassRole.valueOf(fields[3]));
        }
        if (!classes.keySet().equals(rescan.classes().keySet())) {
            Set<String> missing = new TreeSet<>(rescan.classes().keySet());
            missing.removeAll(classes.keySet());
            Set<String> unexpected = new TreeSet<>(classes.keySet());
            unexpected.removeAll(rescan.classes().keySet());
            throw new IOException("Class inventory mismatch; missing=" + missing + ", unexpected=" + unexpected);
        }
    }

    private static Set<M3ReachabilityAnalyzer.Feature> readFeatures(Path root) throws IOException {
        List<String> rows = Files.readAllLines(root.resolve("features.tsv"), StandardCharsets.UTF_8);
        requireHeader(rows, FEATURE_HEADER, "features.tsv");
        Set<M3ReachabilityAnalyzer.Feature> features = new TreeSet<>(
                java.util.Comparator.comparing(M3ReachabilityAnalyzer.Feature::ownerClass)
                        .thenComparing(feature -> feature.kind().name())
                        .thenComparing(M3ReachabilityAnalyzer.Feature::detail));
        for (int index = 1; index < rows.size(); index++) {
            if (rows.get(index).isEmpty()) {
                continue;
            }
            String[] fields = rows.get(index).split("\t", -1);
            if (fields.length != 4) {
                throw new IOException("Invalid features.tsv row " + (index + 1));
            }
            features.add(new M3ReachabilityAnalyzer.Feature(fields[0], M3FeatureKind.valueOf(fields[1]),
                    tsvUnescape(fields[2]), Boolean.parseBoolean(fields[3])));
        }
        return features;
    }

    private static Properties readProperties(Path file) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static void requireHeader(List<String> rows, String expected, String file) throws IOException {
        if (rows.isEmpty() || !expected.equals(rows.get(0))) {
            throw new IOException("Invalid " + file + " header");
        }
    }

    private static Set<String> readLines(Path file) throws IOException {
        Set<String> values = new TreeSet<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) {
                values.add(line.trim());
            }
        }
        return values;
    }

    private static String lines(Set<String> values) {
        StringBuilder builder = new StringBuilder();
        values.forEach(value -> builder.append(value).append('\n'));
        return builder.toString();
    }

    private static Path safeResolve(Path root, String relative) throws IOException {
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root) || relative.startsWith("/") || relative.contains("\\")) {
            throw new IOException("Unsafe plan path: " + relative);
        }
        return resolved;
    }

    private static String propertyEscape(String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String tsvEscape(String value) {
        return value.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String tsvUnescape(String value) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character != '\\') {
                builder.append(character);
                continue;
            }
            if (++index >= value.length()) {
                throw new IOException("Trailing TSV escape");
            }
            char escaped = value.charAt(index);
            builder.append(switch (escaped) {
                case '\\' -> '\\';
                case 't' -> '\t';
                case 'n' -> '\n';
                case 'r' -> '\r';
                default -> throw new IOException("Unknown TSV escape: \\" + escaped);
            });
        }
        return builder.toString();
    }
}
