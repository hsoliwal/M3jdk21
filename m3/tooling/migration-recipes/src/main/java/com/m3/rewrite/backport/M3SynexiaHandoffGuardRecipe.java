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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
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
 * Fail-closed receiver guard for content-addressed Synexia public-target handoff packets.
 *
 * <p>The guard mutates nothing. It validates the exact V2 provenance packet, artifact roles,
 * source/payload hashes, Java/text manifests and bridge properties before any following hash-pinned
 * snapshot recipe can mutate M3JDK21. A successful guard still grants no promotion authority.</p>
 */
public final class M3SynexiaHandoffGuardRecipe extends Recipe {
    static final String VERSION = "SYNEXIA_M3_HANDOFF_V2";
    static final String LICENSE_POLICY = "SYNEXIA_FIRST_PARTY_APACHE2_V1";
    static final String ARTIFACT_POLICY = "SYNEXIA_PUBLIC_TARGET_ARTIFACT_CLASS_V1";
    static final String FAST_LANE_LICENSE = "Apache-2.0";

    private static final String BRIDGE_ROOT =
            "/com/m3/rewrite/backport/synexia-bridge/";
    private static final String JAVA_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/";
    private static final String TEXT_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/";
    private static final int MAX_ROWS = 4096;
    private static final int MAX_RESOURCE_BYTES = 64 * 1024 * 1024;

    @Option(
            displayName = "Synexia crate name",
            description = "Content-addressed Synexia V2 handoff crate to validate.",
            example = "synexia-m3string-v1")
    private final String crateName;

    public record Receipt(
            String crateName,
            String sourceRevision,
            String packetRoot,
            int rows,
            long payloadBytes) {
        public Receipt {
            Objects.requireNonNull(crateName, "crateName");
            Objects.requireNonNull(sourceRevision, "sourceRevision");
            Objects.requireNonNull(packetRoot, "packetRoot");
            if (!crateName.matches("synexia-[a-z0-9][a-z0-9-]{0,63}")
                    || !sourceRevision.matches("[0-9a-f]{40}")
                    || !packetRoot.matches("[0-9a-f]{64}")
                    || rows < 1
                    || payloadBytes < 0) {
                throw new IllegalArgumentException("invalid Synexia receipt");
            }
        }
    }

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
        return "Validate Synexia public-target handoff";
    }

    @Override
    public String getDescription() {
        return "Validates one source-pinned Apache-2.0 Synexia V2 handoff crate before target mutation.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "synexia",
                "jdk21",
                "openrewrite",
                "apache-2.0",
                "artifact-class",
                "provenance",
                "hash-pinned",
                "fail-closed",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        validateCrate(crateName);
        return new TreeVisitor<Tree, ExecutionContext>() {};
    }

    static Receipt validateCrate(String crate) {
        if (crate == null || !crate.matches("synexia-[a-z0-9][a-z0-9-]{0,63}")) {
            throw new IllegalArgumentException("invalid Synexia handoff crate");
        }

        String bridge = BRIDGE_ROOT + crate + "/";
        String packet = resource(bridge + "packet.tsv");
        List<String> lines = packet.lines().toList();
        if (lines.isEmpty() || !("# " + VERSION).equals(lines.getFirst())) {
            throw new IllegalStateException("invalid Synexia handoff packet version");
        }

        Map<String, String> headers = new HashMap<>();
        List<Row> rows = new ArrayList<>();
        HashSet<String> targets = new HashSet<>();
        String previousTarget = "";
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) {
                continue;
            }
            String[] cells = line.split("\t", -1);
            if (line.startsWith("@")) {
                if (cells.length != 2 || headers.putIfAbsent(cells[0], cells[1]) != null) {
                    throw new IllegalStateException("invalid Synexia packet header");
                }
                continue;
            }
            if (cells.length != 14 || !"ROW".equals(cells[0])) {
                throw new IllegalStateException("invalid Synexia packet row");
            }
            Row row = Row.parse(cells);
            if (!targets.add(row.targetPath())
                    || previousTarget.compareTo(row.targetPath()) >= 0) {
                throw new IllegalStateException("duplicate/noncanonical Synexia target order");
            }
            previousTarget = row.targetPath();
            rows.add(row);
            if (rows.size() > MAX_ROWS) {
                throw new IllegalStateException("Synexia packet row budget");
            }
        }

        if (!headers.keySet().equals(
                Set.of("@crate", "@source-revision", "@license-policy", "@artifact-policy"))) {
            throw new IllegalStateException("Synexia packet header set");
        }
        String revision = headers.get("@source-revision");
        if (!crate.equals(headers.get("@crate"))
                || revision == null
                || !revision.matches("[0-9a-f]{40}")
                || !LICENSE_POLICY.equals(headers.get("@license-policy"))
                || !ARTIFACT_POLICY.equals(headers.get("@artifact-policy"))
                || rows.isEmpty()) {
            throw new IllegalStateException("Synexia packet policy/provenance");
        }

        ArrayList<String> javaManifest = new ArrayList<>();
        ArrayList<String> textManifest = new ArrayList<>();
        long payloadBytes = 0L;
        for (Row row : rows) {
            String root = "JAVA".equals(row.kind()) ? JAVA_ROOT : TEXT_ROOT;
            String payload = resource(root + crate + "/" + row.payload());
            if (!sha256(payload).equals(row.sourceSha())) {
                throw new IllegalStateException("Synexia payload hash drift: " + row.targetPath());
            }
            payloadBytes = Math.addExact(
                    payloadBytes, payload.getBytes(StandardCharsets.UTF_8).length);
            String manifest =
                    row.targetPath()
                            + "\t"
                            + row.targetBefore()
                            + "\t"
                            + row.sourceSha()
                            + "\t"
                            + row.payload();
            ("JAVA".equals(row.kind()) ? javaManifest : textManifest).add(manifest);
        }

        verifyManifest(JAVA_ROOT + crate + "/manifest.tsv", javaManifest);
        verifyManifest(TEXT_ROOT + crate + "/manifest.tsv", textManifest);

        String packetRoot = sha256(packet);
        Properties properties = properties(resource(bridge + "bridge.properties"));
        requireEquals(VERSION, properties.getProperty("version"), "version");
        requireEquals(crate, properties.getProperty("crate"), "crate");
        requireEquals(revision, properties.getProperty("sourceRevision"), "sourceRevision");
        requireEquals(packetRoot, properties.getProperty("packetRoot"), "packetRoot");
        requireEquals(LICENSE_POLICY, properties.getProperty("licensePolicy"), "licensePolicy");
        requireEquals(ARTIFACT_POLICY, properties.getProperty("artifactPolicy"), "artifactPolicy");
        requireEquals(Integer.toString(rows.size()), properties.getProperty("rows"), "rows");
        requireEquals(Long.toString(payloadBytes), properties.getProperty("payloadBytes"), "payloadBytes");

        return new Receipt(crate, revision, packetRoot, rows.size(), payloadBytes);
    }

    static boolean m3OwnedTargetPath(String value) {
        return value != null && (value.startsWith("m3/") || value.startsWith(".m3/"));
    }

    static boolean acceptsTargetClass(String kind, String targetClass, String targetPath) {
        if (!Set.of("JAVA", "TEXT", "NATIVE").contains(kind)
                || targetClass == null
                || targetPath == null
                || !safePath(targetPath)) {
            return false;
        }
        boolean m3 = m3OwnedTargetPath(targetPath);
        boolean recipe =
                m3
                        && (targetPath.contains("/recipe")
                                || targetPath.contains("/rewrite")
                                || targetPath.startsWith(".m3/openrewrite-recipes/"));
        boolean test = targetPath.startsWith("test/") || targetPath.contains("/src/test/");
        boolean docs = targetPath.startsWith("m3/docs/") || targetPath.startsWith("doc/");
        boolean manifest =
                m3
                        && (targetPath.endsWith(".tsv")
                                || targetPath.endsWith(".json")
                                || targetPath.endsWith(".sha256")
                                || targetPath.endsWith(".properties"));
        boolean config =
                (m3 || targetPath.startsWith(".github/"))
                        && (targetPath.endsWith(".yml")
                                || targetPath.endsWith(".yaml")
                                || targetPath.endsWith(".xml")
                                || targetPath.endsWith(".properties")
                                || targetPath.endsWith(".sh"));
        boolean product = targetPath.startsWith("src/");
        return switch (targetClass) {
            case "M3_TOOLING" -> m3;
            case "M3_RECIPE" -> recipe;
            case "M3_TEST" -> test;
            case "M3_DOCUMENTATION" -> "TEXT".equals(kind) && docs;
            case "M3_MANIFEST" -> "TEXT".equals(kind) && manifest;
            case "M3_CONFIGURATION" -> "TEXT".equals(kind) && config;
            case "JDK_SEPARATE_COMPONENT" -> product;
            default -> false;
        };
    }

    private record Row(
            String kind,
            String targetClass,
            String sourceSha,
            String targetPath,
            String targetBefore,
            String payload) {

        static Row parse(String[] cells) {
            String kind = cells[1];
            String targetClass = cells[2];
            String sourceSha = cells[5];
            String targetPath = cells[6];
            String targetBefore = cells[7];
            String repeatedSha = cells[8];
            String payload = cells[9];
            String license = cells[10];
            String recipeId = cells[11];
            String contractRoot = cells[12];
            String gateRoot = cells[13];

            if (!Set.of("JAVA", "TEXT", "NATIVE").contains(kind)
                    || !acceptsTargetClass(kind, targetClass, targetPath)
                    || !sha(sourceSha)
                    || !sourceSha.equals(repeatedSha)
                    || !("ABSENT".equals(targetBefore) || sha(targetBefore))
                    || !payload.matches("[A-Za-z0-9_.-]+")
                    || !FAST_LANE_LICENSE.equals(license)
                    || !recipeId.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,159}")
                    || !sha(contractRoot)
                    || !sha(gateRoot)) {
                throw new IllegalStateException("invalid Synexia packet row fields");
            }
            if ("JAVA".equals(kind)
                    && !M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(targetPath)) {
                throw new IllegalStateException("invalid Synexia Java target");
            }
            if (!"JAVA".equals(kind) && targetPath.endsWith(".java")) {
                throw new IllegalStateException("non-Java Synexia row targets Java");
            }
            if ("NATIVE".equals(kind) && !nativeLeaf(targetPath)) {
                throw new IllegalStateException("invalid Synexia native target");
            }
            return new Row(kind, targetClass, sourceSha, targetPath, targetBefore, payload);
        }
    }

    private static void verifyManifest(String resourceName, List<String> expected) {
        if (expected.isEmpty()) {
            if (resourceExists(resourceName)) {
                throw new IllegalStateException("unexpected empty Synexia manifest");
            }
            return;
        }
        String actual = resource(resourceName);
        List<String> lines = actual.lines().filter(line -> !line.isBlank()).toList();
        if (!lines.equals(expected)) {
            throw new IllegalStateException("Synexia manifest drift: " + resourceName);
        }
    }

    private static Properties properties(String text) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(text));
            return properties;
        } catch (IOException impossible) {
            throw new IllegalStateException("cannot parse Synexia bridge properties", impossible);
        }
    }

    private static void requireEquals(String expected, String actual, String field) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException("invalid Synexia " + field);
        }
    }

    private static boolean nativeLeaf(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.endsWith(".c")
                || lower.endsWith(".cc")
                || lower.endsWith(".cpp")
                || lower.endsWith(".cxx")
                || lower.endsWith(".h")
                || lower.endsWith(".hh")
                || lower.endsWith(".hpp")
                || lower.endsWith(".s");
    }

    private static boolean safePath(String value) {
        if (value.isBlank()
                || value.startsWith("/")
                || value.matches("^[A-Za-z]:.*")
                || value.indexOf('\\') >= 0
                || value.length() > 4096
                || value.chars().anyMatch(Character::isISOControl)) {
            return false;
        }
        for (String part : value.split("/", -1)) {
            if (part.isEmpty()
                    || ".".equals(part)
                    || "..".equals(part)
                    || ".git".equalsIgnoreCase(part)) {
                return false;
            }
        }
        return true;
    }

    private static boolean sha(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    private static boolean resourceExists(String name) {
        return M3SynexiaHandoffGuardRecipe.class.getResource(name) != null;
    }

    private static String resource(String name) {
        try (var stream = M3SynexiaHandoffGuardRecipe.class.getResourceAsStream(name)) {
            if (stream == null) {
                throw new IllegalStateException("missing Synexia handoff resource: " + name);
            }
            byte[] bytes = stream.readNBytes(MAX_RESOURCE_BYTES + 1);
            if (bytes.length > MAX_RESOURCE_BYTES) {
                throw new IllegalStateException("Synexia handoff resource too large: " + name);
            }
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read Synexia handoff resource: " + name, failure);
        }
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
