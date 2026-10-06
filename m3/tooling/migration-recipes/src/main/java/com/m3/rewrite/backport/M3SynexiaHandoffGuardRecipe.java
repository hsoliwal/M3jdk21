// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

/**
 * Fail-closed receiver guard for content-addressed Synexia handoff packets.
 *
 * <p>Retained V1 packets remain readable. V2 adds target artifact classes and explicit public-target
 * license/artifact policies. V3 additionally requires an independently verified five-scope
 * FILE/PACKAGE/MODULE/PROJECT/REPOSITORY hierarchy qualification. The guard is mutation-free and
 * never grants target promotion; M3JDK21 build, jtreg, native and runtime proof remain separate.</p>
 */
public final class M3SynexiaHandoffGuardRecipe extends Recipe {
    static final String VERSION_V1 = "SYNEXIA_M3_HANDOFF_V1";
    static final String VERSION_V2 = "SYNEXIA_M3_HANDOFF_V2";
    static final String VERSION_V3 = "SYNEXIA_M3_HANDOFF_V3";
    static final String LICENSE_POLICY = "SYNEXIA_FIRST_PARTY_APACHE2_V1";
    static final String ARTIFACT_POLICY = "SYNEXIA_PUBLIC_TARGET_ARTIFACT_CLASS_V1";
    static final String QUALIFICATION_VERSION =
            "SYNEXIA_HIERARCHICAL_ATOM_PATTERN_EXPORT_V1";
    static final String QUALIFICATION_FILE = "hierarchy-qualification.tsv";

    private static final String RESOURCE_ROOT =
            "/com/m3/rewrite/backport/synexia-bridge/";
    private static final Set<String> VERSIONS =
            Set.of(VERSION_V1, VERSION_V2, VERSION_V3);
    private static final Set<String> KINDS =
            Set.of("JAVA", "TEXT", "NATIVE");
    private static final Set<String> TARGET_CLASSES =
            Set.of(
                    "M3_TOOLING",
                    "M3_RECIPE",
                    "M3_TEST",
                    "M3_DOCUMENTATION",
                    "M3_MANIFEST",
                    "M3_CONFIGURATION",
                    "JDK_SEPARATE_COMPONENT");

    @Option(
            displayName = "Synexia crate name",
            description = "Content-addressed Synexia handoff crate to validate.",
            example = "synexia-string-precompute-v1")
    private final String crateName;

    @JsonCreator
    public M3SynexiaHandoffGuardRecipe(@JsonProperty("crateName") String crateName) {
        if (crateName == null
                || !crateName.matches("synexia-[a-z0-9][a-z0-9-]{0,63}")) {
            throw new IllegalArgumentException("invalid Synexia handoff crate");
        }
        this.crateName = crateName;
    }

    public String getCrateName() {
        return crateName;
    }

    @Override
    public String getDisplayName() {
        return "Validate Synexia content-addressed handoff";
    }

    @Override
    public String getDescription() {
        return "Validates retained V1, target-class-aware V2, and hierarchy-qualified V3 Synexia "
                + "handoffs before target mutation.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "synexia",
                "jdk21",
                "openrewrite",
                "provenance",
                "hash-pinned",
                "hierarchical",
                "fail-closed",
                "candidate-only");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        validatePacket();
        return new TreeVisitor<Tree, ExecutionContext>() {};
    }

    private void validatePacket() {
        String root = RESOURCE_ROOT + crateName + "/";
        String packet = resource(root + "packet.tsv");
        Properties properties = properties(resource(root + "bridge.properties"));

        String version = properties.getProperty("version");
        if (!VERSIONS.contains(version)) {
            throw new IllegalStateException("unsupported Synexia handoff version");
        }
        requireEquals(crateName, properties.getProperty("crate"), "crate");
        requireSha(properties.getProperty("packetRoot"), "packetRoot");
        requireEquals(
                sha256(packet), properties.getProperty("packetRoot"), "packetRoot");

        List<String> lines = packet.lines().filter(line -> !line.isBlank()).toList();
        if (lines.size() < 4 || !("# " + version).equals(lines.getFirst())) {
            throw new IllegalStateException("invalid Synexia handoff packet header");
        }
        requireEquals("@crate\t" + crateName, lines.get(1), "packet crate");

        String sourceRevision = properties.getProperty("sourceRevision");
        if (sourceRevision == null
                || !sourceRevision.matches("[0-9a-f]{40}")) {
            throw new IllegalStateException("invalid Synexia source revision");
        }
        requireEquals(
                "@source-revision\t" + sourceRevision,
                lines.get(2),
                "source revision");

        int cursor = 3;
        if (!VERSION_V1.equals(version)) {
            requireEquals(
                    LICENSE_POLICY,
                    properties.getProperty("licensePolicy"),
                    "license policy");
            requireEquals(
                    ARTIFACT_POLICY,
                    properties.getProperty("artifactPolicy"),
                    "artifact policy");
            requireEquals(
                    "@license-policy\t" + LICENSE_POLICY,
                    lines.get(cursor++),
                    "packet license policy");
            requireEquals(
                    "@artifact-policy\t" + ARTIFACT_POLICY,
                    lines.get(cursor++),
                    "packet artifact policy");
        }

        if (VERSION_V3.equals(version)) {
            HierarchyQualification qualification =
                    HierarchyQualification.parse(
                            resource(root + QUALIFICATION_FILE));
            if (!sourceRevision.equals(qualification.sourceRevision())) {
                throw new IllegalStateException(
                        "Synexia hierarchy qualification source revision mismatch");
            }

            requireEquals(
                    qualification.root(),
                    properties.getProperty("hierarchyQualificationRoot"),
                    "hierarchy qualification root");
            requireEquals(
                    qualification.hierarchyRoot(),
                    properties.getProperty("hierarchyRoot"),
                    "hierarchy root");
            requireEquals(
                    qualification.atomPatternRoot(),
                    properties.getProperty("atomPatternRoot"),
                    "atom-pattern root");
            requireEquals(
                    qualification.portfolioRoot(),
                    properties.getProperty("portfolioRoot"),
                    "portfolio root");

            requireEquals(
                    "@hierarchy-qualification-root\t" + qualification.root(),
                    lines.get(cursor++),
                    "packet hierarchy qualification root");
            requireEquals(
                    "@hierarchy-root\t" + qualification.hierarchyRoot(),
                    lines.get(cursor++),
                    "packet hierarchy root");
            requireEquals(
                    "@atom-pattern-root\t" + qualification.atomPatternRoot(),
                    lines.get(cursor++),
                    "packet atom-pattern root");
            requireEquals(
                    "@portfolio-root\t" + qualification.portfolioRoot(),
                    lines.get(cursor++),
                    "packet portfolio root");
        }

        int rows = 0;
        Set<String> targets = new HashSet<>();
        for (; cursor < lines.size(); cursor++) {
            String[] cells = lines.get(cursor).split("\t", -1);
            if (VERSION_V1.equals(version)) {
                validateV1Row(cells, targets);
            } else {
                validateV2V3Row(cells, targets);
            }
            rows++;
        }

        int expectedRows;
        long payloadBytes;
        try {
            expectedRows = Integer.parseInt(properties.getProperty("rows"));
            payloadBytes = Long.parseLong(properties.getProperty("payloadBytes"));
        } catch (RuntimeException invalid) {
            throw new IllegalStateException(
                    "invalid Synexia handoff counters", invalid);
        }
        if (rows < 1 || rows != expectedRows || payloadBytes < 0) {
            throw new IllegalStateException("Synexia handoff row/count mismatch");
        }
    }

    private static void validateV1Row(String[] cells, Set<String> targets) {
        if (cells.length != 13 || !"ROW".equals(cells[0])) {
            throw new IllegalStateException("invalid Synexia V1 handoff row");
        }
        requireKind(cells[1]);
        token(cells[2], "capability");
        relative(cells[3]);
        requireSha(cells[4], "sourceSha");
        String target = relative(cells[5]);
        uniqueTarget(targets, target);
        requireBefore(cells[6]);
        requireSha(cells[7], "targetAfter");
        payload(cells[8]);
        if (m3OwnedTargetPath(target) && !"Apache-2.0".equals(cells[9])) {
            throw new IllegalStateException(
                    "M3-owned Synexia target requires Apache-2.0 direct bytes: "
                            + target);
        }
        token(cells[10], "recipeId");
        requireSha(cells[11], "contractRoot");
        requireSha(cells[12], "gateRoot");
    }

    private static void validateV2V3Row(String[] cells, Set<String> targets) {
        if (cells.length != 14 || !"ROW".equals(cells[0])) {
            throw new IllegalStateException("invalid Synexia V2/V3 handoff row");
        }
        String kind = requireKind(cells[1]);
        String targetClass = cells[2];
        if (!TARGET_CLASSES.contains(targetClass)) {
            throw new IllegalStateException("invalid Synexia target class");
        }
        token(cells[3], "capability");
        String source = relative(cells[4]);
        String sourceSha = cells[5];
        requireSha(sourceSha, "sourceSha");
        String target = relative(cells[6]);
        uniqueTarget(targets, target);
        requireBefore(cells[7]);
        requireSha(cells[8], "targetAfter");
        requireEquals(sourceSha, cells[8], "source/target direct-byte hash");
        payload(cells[9]);
        if (!"Apache-2.0".equals(cells[10])) {
            throw new IllegalStateException(
                    "qualified public-target handoff requires Apache-2.0 direct bytes");
        }
        token(cells[11], "recipeId");
        requireSha(cells[12], "contractRoot");
        requireSha(cells[13], "gateRoot");
        validateTargetClass(kind, targetClass, source, target);
    }

    private static void validateTargetClass(
            String kind, String targetClass, String source, String target) {
        if ("JAVA".equals(kind)) {
            if (!source.endsWith(".java") || !target.endsWith(".java")) {
                throw new IllegalStateException("JAVA handoff requires Java source/target");
            }
        } else if (target.endsWith(".java")) {
            throw new IllegalStateException(
                    "non-Java handoff cannot target Java compilation unit");
        }
        if ("NATIVE".equals(kind)
                && (!nativeLeaf(source) || !nativeLeaf(target))) {
            throw new IllegalStateException(
                    "NATIVE handoff requires native source/target");
        }

        boolean m3 = m3OwnedTargetPath(target);
        boolean recipe =
                m3
                        && (target.contains("/recipe")
                                || target.contains("/rewrite")
                                || target.startsWith(
                                        ".m3/openrewrite-recipes/"));
        boolean test =
                target.startsWith("test/") || target.contains("/src/test/");
        boolean docs =
                target.startsWith("m3/docs/") || target.startsWith("doc/");
        boolean manifest =
                m3
                        && (target.endsWith(".tsv")
                                || target.endsWith(".json")
                                || target.endsWith(".sha256")
                                || target.endsWith(".properties"));
        boolean config =
                (m3 || target.startsWith(".github/"))
                        && (target.endsWith(".yml")
                                || target.endsWith(".yaml")
                                || target.endsWith(".xml")
                                || target.endsWith(".properties")
                                || target.endsWith(".sh"));
        boolean product = target.startsWith("src/");

        boolean accepted =
                switch (targetClass) {
                    case "M3_TOOLING" -> m3;
                    case "M3_RECIPE" -> recipe;
                    case "M3_TEST" -> test;
                    case "M3_DOCUMENTATION" -> "TEXT".equals(kind) && docs;
                    case "M3_MANIFEST" -> "TEXT".equals(kind) && manifest;
                    case "M3_CONFIGURATION" -> "TEXT".equals(kind) && config;
                    case "JDK_SEPARATE_COMPONENT" -> product;
                    default -> false;
                };
        if (!accepted) {
            throw new IllegalStateException(
                    "Synexia target class/path mismatch: "
                            + targetClass
                            + " -> "
                            + target);
        }
    }

    static boolean m3OwnedTargetPath(String value) {
        return value != null
                && (value.startsWith("m3/") || value.startsWith(".m3/"));
    }

    private static String requireKind(String value) {
        if (!KINDS.contains(value)) {
            throw new IllegalStateException("invalid Synexia handoff kind");
        }
        return value;
    }

    private static void requireBefore(String value) {
        if (!"ABSENT".equals(value)) {
            requireSha(value, "targetBefore");
        }
    }

    private static void uniqueTarget(Set<String> targets, String target) {
        if (!targets.add(target)) {
            throw new IllegalStateException(
                    "duplicate Synexia handoff target: " + target);
        }
    }

    private static String relative(String value) {
        if (value == null
                || value.isBlank()
                || value.startsWith("/")
                || value.matches("^[A-Za-z]:/.*")
                || value.indexOf('\\') >= 0
                || value.length() > 4096
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalStateException("invalid Synexia path");
        }
        for (String part : value.split("/", -1)) {
            if (part.isEmpty()
                    || ".".equals(part)
                    || "..".equals(part)
                    || ".git".equalsIgnoreCase(part)) {
                throw new IllegalStateException("noncanonical Synexia path");
            }
        }
        return value;
    }

    private static void payload(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalStateException("invalid Synexia payload leaf");
        }
    }

    private static void token(String value, String field) {
        if (value == null
                || !value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,159}")) {
            throw new IllegalStateException("invalid Synexia " + field);
        }
    }

    private static boolean nativeLeaf(String value) {
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".c")
                || lower.endsWith(".cc")
                || lower.endsWith(".cpp")
                || lower.endsWith(".cxx")
                || lower.endsWith(".h")
                || lower.endsWith(".hh")
                || lower.endsWith(".hpp")
                || lower.endsWith(".s");
    }

    private static Properties properties(String text) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(text));
        } catch (IOException impossible) {
            throw new IllegalStateException(
                    "cannot parse Synexia bridge properties", impossible);
        }
        return properties;
    }

    private static void requireEquals(
            String expected, String actual, String field) {
        if (!Objects.equals(expected, actual)) {
            throw new IllegalStateException("invalid Synexia " + field);
        }
    }

    private static void requireSha(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalStateException("invalid Synexia " + field);
        }
    }

    private static String resource(String name) {
        try (var stream =
                M3SynexiaHandoffGuardRecipe.class.getResourceAsStream(name)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "missing Synexia handoff resource: " + name);
            }
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(stream.readAllBytes()))
                    .toString();
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read Synexia handoff resource", failure);
        }
    }

    private static String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private record HierarchyQualification(
            String sourceRevision,
            String hierarchyRoot,
            String atomPatternRoot,
            String portfolioRoot,
            String root) {
        static HierarchyQualification parse(String text) {
            Map<String, String> values = new LinkedHashMap<>();
            List<String> lines =
                    Objects.toString(text, "")
                            .lines()
                            .filter(line -> !line.isBlank())
                            .toList();
            if (lines.size() != 15) {
                throw new IllegalStateException(
                        "invalid Synexia hierarchy qualification row count");
            }
            for (String line : lines) {
                String[] cells = line.split("\t", -1);
                if (cells.length != 2
                        || cells[0].isBlank()
                        || values.putIfAbsent(cells[0], cells[1]) != null) {
                    throw new IllegalStateException(
                            "invalid Synexia hierarchy qualification row");
                }
            }
            requireEquals(
                    QUALIFICATION_VERSION,
                    values.get("version"),
                    "hierarchy qualification version");
            String revision = values.get("sourceRevision");
            if (revision == null || !revision.matches("[0-9a-f]{40}")) {
                throw new IllegalStateException(
                        "invalid Synexia hierarchy source revision");
            }

            String topology = requireQualificationSha(values, "topologyRoot");
            String file = requireQualificationSha(values, "FILE");
            String packageRoot = requireQualificationSha(values, "PACKAGE");
            String module = requireQualificationSha(values, "MODULE");
            String project = requireQualificationSha(values, "PROJECT");
            String repository = requireQualificationSha(values, "REPOSITORY");
            String hierarchy = requireQualificationSha(values, "hierarchyRoot");
            String atomPattern =
                    requireQualificationSha(values, "atomPatternRoot");
            String portfolio =
                    requireQualificationSha(values, "portfolioRoot");
            int sources = positive(values.get("sources"), "sources");
            int documentation =
                    nonNegative(
                            values.get("documentationSources"),
                            "documentationSources");
            if (documentation > sources) {
                throw new IllegalStateException(
                        "invalid Synexia hierarchy documentation count");
            }
            requireEquals("true", values.get("complete"), "hierarchy complete");
            String declared = requireQualificationSha(values, "root");

            String expected =
                    qualificationRoot(
                            revision,
                            topology,
                            file,
                            packageRoot,
                            module,
                            project,
                            repository,
                            hierarchy,
                            atomPattern,
                            portfolio,
                            sources,
                            documentation);
            requireEquals(
                    expected, declared, "hierarchy qualification root");
            return new HierarchyQualification(
                    revision, hierarchy, atomPattern, portfolio, declared);
        }

        private static String qualificationRoot(
                String revision,
                String topology,
                String file,
                String packageRoot,
                String module,
                String project,
                String repository,
                String hierarchy,
                String atomPattern,
                String portfolio,
                int sources,
                int documentation) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                frame(digest, QUALIFICATION_VERSION);
                for (String value :
                        List.of(
                                revision,
                                topology,
                                file,
                                packageRoot,
                                module,
                                project,
                                repository,
                                hierarchy,
                                atomPattern,
                                portfolio,
                                Integer.toString(sources),
                                Integer.toString(documentation),
                                "true")) {
                    frame(digest, value);
                }
                return HexFormat.of().formatHex(digest.digest());
            } catch (NoSuchAlgorithmException impossible) {
                throw new ExceptionInInitializerError(impossible);
            }
        }

        private static String requireQualificationSha(
                Map<String, String> values, String key) {
            String value = values.get(key);
            requireSha(value, "hierarchy " + key);
            return value;
        }

        private static int positive(String value, String field) {
            int parsed = nonNegative(value, field);
            if (parsed < 1) {
                throw new IllegalStateException("invalid Synexia " + field);
            }
            return parsed;
        }

        private static int nonNegative(String value, String field) {
            try {
                int parsed = Integer.parseInt(value);
                if (parsed < 0) throw new NumberFormatException();
                return parsed;
            } catch (RuntimeException invalid) {
                throw new IllegalStateException(
                        "invalid Synexia " + field, invalid);
            }
        }

        private static void frame(MessageDigest digest, String value) {
            byte[] bytes =
                    Objects.requireNonNull(value, "value")
                            .getBytes(StandardCharsets.UTF_8);
            digest.update((byte) (bytes.length >>> 24));
            digest.update((byte) (bytes.length >>> 16));
            digest.update((byte) (bytes.length >>> 8));
            digest.update((byte) bytes.length);
            digest.update(bytes);
        }
    }
}
