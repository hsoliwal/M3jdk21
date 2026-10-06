// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Fail-closed admission of the portable Nebula M3 recipe/DAG transfer bundle.
 *
 * <p>The bundle is evidence only. Successful admission proves content integrity, Java/OpenRewrite
 * compatibility metadata, the intended M3JDK21 target lane, and authority-free scheduler views.
 * It never grants JDK source mutation, semantic equivalence, donor-copy, native execution, or
 * promotion authority.</p>
 */
public final class M3NebulaTransferAdmission {
    public static final String SCHEMA = "NEBULA_M3_RECIPE_TRANSFER_V1";
    public static final int JAVA_RELEASE = 21;
    public static final String OPENREWRITE_VERSION = "8.90.4";
    public static final String REFACTOR_SCOPE_ORDER =
            "FILE,VISIBILITY,PACKAGE,MODULE,MULTI_MODULE,LIBRARY_API";
    public static final String ENTRYPOINT =
            "org.eclipse.nebula.m3.rewrite.NebulaM3Java21ConvergenceRecipe";
    public static final String TARGET_REPOSITORY = "hsoliwal/M3jdk21";
    public static final String TARGET_LANE = "JAVA21_JDK_COMPATIBILITY_AND_BACKPORT_LANES";

    private static final Set<String> ORCHESTRATORS =
            Set.of("MAVEN_OPENREWRITE", "CAMEL", "AIRFLOW", "DROOLS");

    private M3NebulaTransferAdmission() {}

    public static Receipt read(Path directory) {
        Path root = Objects.requireNonNull(directory, "directory").normalize();
        String metadata = read(root.resolve("transfer-metadata.tsv"));
        String dag = read(root.resolve("transfer-dag.tsv"));
        String targets = read(root.resolve("transfer-targets.tsv"));
        String orchestrators = read(root.resolve("orchestrators.tsv"));
        String camel = read(root.resolve("camel-route.yaml"));
        String airflow = read(root.resolve("airflow-dag.py"));
        String drools = read(root.resolve("drools-agenda.drl"));
        String transferRoot = singleHash(read(root.resolve("transfer.sha256")), "transfer root");

        Map<String, String> fields = metadata(metadata);
        requireEquals(SCHEMA, fields.get("schema"), "schema");
        requireEquals(Integer.toString(JAVA_RELEASE), fields.get("javaRelease"), "javaRelease");
        requireEquals(OPENREWRITE_VERSION, fields.get("openRewriteVersion"), "openRewriteVersion");
        requireEquals(ENTRYPOINT, fields.get("entrypoint"), "entrypoint");
        requireEquals(
                REFACTOR_SCOPE_ORDER,
                fields.get("refactorScopeOrder"),
                "refactor scope order");
        String dagRoot = hash(fields.get("dagRoot"), "dagRoot");
        String orchestratorPlansRoot =
                hash(fields.get("orchestratorPlansRoot"), "orchestratorPlansRoot");
        requireFalse(fields.get("directSourceMutationAuthority"), "directSourceMutationAuthority");
        requireFalse(fields.get("promotionAuthority"), "promotionAuthority");

        validateTargets(targets);
        validateOrchestrators(orchestrators);
        requirePlanRoot(dagRoot, camel, "camel");
        requirePlanRoot(dagRoot, airflow, "airflow");
        requirePlanRoot(dagRoot, drools, "drools");

        String computedPlansRoot =
                sha256(framed(dagRoot) + framed(camel) + framed(airflow) + framed(drools));
        requireEquals(orchestratorPlansRoot, computedPlansRoot, "orchestrator plan root");

        Path plansRootFile = root.resolve("orchestrator-plans.sha256");
        if (Files.exists(plansRootFile)) {
            requireEquals(
                    orchestratorPlansRoot,
                    singleHash(read(plansRootFile), "orchestrator plans root file"),
                    "orchestrator plans root file");
        }

        String computedTransferRoot =
                sha256(
                        framed(metadata)
                                + framed(dag)
                                + framed(targets)
                                + framed(orchestrators)
                                + framed(camel)
                                + framed(airflow)
                                + framed(drools));
        requireEquals(transferRoot, computedTransferRoot, "transfer root");

        return new Receipt(
                transferRoot,
                dagRoot,
                orchestratorPlansRoot,
                sha256(metadata),
                sha256(dag),
                sha256(targets),
                sha256(orchestrators));
    }

    public static boolean sourceMutationAuthority() {
        return false;
    }

    public static boolean semanticEquivalenceAuthority() {
        return false;
    }

    public static boolean sourceCopyAuthority() {
        return false;
    }

    public static boolean nativeExecutionAuthority() {
        return false;
    }

    public static boolean promotionAuthority() {
        return false;
    }

    public record Receipt(
            String transferRoot,
            String dagRoot,
            String orchestratorPlansRoot,
            String metadataRoot,
            String dagDocumentRoot,
            String targetsRoot,
            String orchestratorsRoot) {
        public Receipt {
            transferRoot = hash(transferRoot, "transferRoot");
            dagRoot = hash(dagRoot, "dagRoot");
            orchestratorPlansRoot = hash(orchestratorPlansRoot, "orchestratorPlansRoot");
            metadataRoot = hash(metadataRoot, "metadataRoot");
            dagDocumentRoot = hash(dagDocumentRoot, "dagDocumentRoot");
            targetsRoot = hash(targetsRoot, "targetsRoot");
            orchestratorsRoot = hash(orchestratorsRoot, "orchestratorsRoot");
        }

        public String admissionTsv() {
            return """
                    key	value
                    schema	%s
                    targetRepository	%s
                    targetLane	%s
                    refactorScopeOrder	%s
                    transferRoot	%s
                    dagRoot	%s
                    orchestratorPlansRoot	%s
                    metadataRoot	%s
                    dagDocumentRoot	%s
                    targetsRoot	%s
                    orchestratorsRoot	%s
                    sourceMutationAuthority	false
                    semanticEquivalenceAuthority	false
                    sourceCopyAuthority	false
                    nativeExecutionAuthority	false
                    promotionAuthority	false
                    """
                    .formatted(
                            SCHEMA,
                            TARGET_REPOSITORY,
                            TARGET_LANE,
                            REFACTOR_SCOPE_ORDER,
                            transferRoot,
                            dagRoot,
                            orchestratorPlansRoot,
                            metadataRoot,
                            dagDocumentRoot,
                            targetsRoot,
                            orchestratorsRoot);
        }
    }

    private static Map<String, String> metadata(String tsv) {
        List<String> lines = lines(tsv);
        if (lines.isEmpty() || !"key	value".equals(lines.getFirst())) {
            throw new IllegalArgumentException("unexpected transfer metadata header");
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.split("\t", -1);
            if (cells.length != 2 || cells[0].isBlank() || cells[1].isBlank()) {
                throw new IllegalArgumentException("invalid transfer metadata row: " + line);
            }
            if (result.putIfAbsent(cells[0], cells[1]) != null) {
                throw new IllegalArgumentException("duplicate transfer metadata key: " + cells[0]);
            }
        }
        Set<String> required =
                Set.of(
                        "schema",
                        "javaRelease",
                        "openRewriteVersion",
                        "entrypoint",
                        "refactorScopeOrder",
                        "dagRoot",
                        "orchestratorPlansRoot",
                        "directSourceMutationAuthority",
                        "promotionAuthority");
        if (!result.keySet().equals(required)) {
            throw new IllegalArgumentException("unexpected transfer metadata keys: " + result.keySet());
        }
        return Map.copyOf(result);
    }

    private static void validateTargets(String tsv) {
        List<String> lines = lines(tsv);
        String header =
                "repository	adapterLane	directSourceMutationAuthority	promotionAuthority";
        if (lines.isEmpty() || !header.equals(lines.getFirst())) {
            throw new IllegalArgumentException("unexpected transfer targets header");
        }
        boolean targetFound = false;
        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.split("\t", -1);
            if (cells.length != 4) {
                throw new IllegalArgumentException("invalid transfer target row: " + line);
            }
            requireFalse(cells[2], "target source mutation");
            requireFalse(cells[3], "target promotion");
            if (TARGET_REPOSITORY.equals(cells[0])) {
                requireEquals(TARGET_LANE, cells[1], "M3JDK21 target lane");
                targetFound = true;
            }
        }
        if (!targetFound) {
            throw new IllegalArgumentException("M3JDK21 transfer target missing");
        }
    }

    private static void validateOrchestrators(String tsv) {
        List<String> lines = lines(tsv);
        String header = "orchestrator	mutationAuthority	promotionAuthority";
        if (lines.isEmpty() || !header.equals(lines.getFirst())) {
            throw new IllegalArgumentException("unexpected orchestrators header");
        }
        ArrayList<String> names = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.split("\t", -1);
            if (cells.length != 3) {
                throw new IllegalArgumentException("invalid orchestrator row: " + line);
            }
            names.add(cells[0]);
            requireFalse(cells[1], "orchestrator mutation");
            requireFalse(cells[2], "orchestrator promotion");
        }
        if (!Set.copyOf(names).equals(ORCHESTRATORS) || names.size() != ORCHESTRATORS.size()) {
            throw new IllegalArgumentException("unexpected orchestrator set: " + names);
        }
    }

    private static void requirePlanRoot(String dagRoot, String plan, String name) {
        if (!plan.contains(dagRoot)) {
            throw new IllegalArgumentException(name + " plan does not bind DAG root");
        }
    }

    private static List<String> lines(String value) {
        return Objects.requireNonNull(value, "value").lines()
                .filter(line -> !line.isBlank())
                .toList();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalArgumentException("cannot read transfer file: " + path, failure);
        }
    }

    private static String singleHash(String value, String field) {
        List<String> values = value.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
        if (values.size() != 1) {
            throw new IllegalArgumentException(field);
        }
        return hash(values.getFirst(), field);
    }

    private static void requireFalse(String value, String field) {
        requireEquals("false", value, field);
    }

    private static void requireEquals(String expected, String actual, String field) {
        if (!Objects.equals(expected, actual)) {
            throw new IllegalArgumentException(field + " mismatch: " + actual);
        }
    }

    private static String hash(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String framed(String value) {
        byte[] bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        return bytes.length + ":" + value;
    }

    static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
