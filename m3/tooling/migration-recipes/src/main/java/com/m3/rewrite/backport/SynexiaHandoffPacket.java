// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.io.IOException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class SynexiaHandoffPacket {
    static final String VERSION = "SYNEXIA_M3_HANDOFF_V1";
    private static final String BRIDGE_ROOT =
            "/com/m3/rewrite/backport/synexia-bridge/";
    private static final String JAVA_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/";
    private static final String TEXT_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/";
    static final String RESOURCE_PREFIX = "m3/tooling/migration-recipes/src/main/resources/";
    private static final int MAX_ROWS = 4096;
    private static final long MAX_RESOURCE_BYTES = 64L * 1024L * 1024L;
    private static final long MAX_PAYLOAD_BYTES = 512L * 1024L * 1024L;

    enum Kind { JAVA, TEXT, NATIVE }

    record Verified(
            String crateName,
            String sourceRevision,
            String packetRoot,
            int rows,
            long payloadBytes) {}

    record Inspection(Verified verified, List<String> resourcePaths) {
        Inspection {
            verified = Objects.requireNonNull(verified, "verified");
            resourcePaths = List.copyOf(Objects.requireNonNull(resourcePaths, "resourcePaths"));
        }
    }

    private record Row(
            Kind kind,
            String capability,
            String sourcePath,
            String sourceSha,
            String targetPath,
            String targetBefore,
            String targetAfter,
            String payload,
            String license,
            String recipeId,
            String contractRoot,
            String gateRoot) {}

    private SynexiaHandoffPacket() {}

    static Verified verify(String crateName) {
        String crate = checkedCrate(crateName);
        String root = BRIDGE_ROOT + crate + "/";
        String packet = resource(root + "packet.tsv");
        Map<String, String> properties = properties(resource(root + "bridge.properties"));

        exact(properties, "version", VERSION);
        exact(properties, "crate", crate);
        String revision = requireRevision(required(properties, "sourceRevision"));
        String packetRoot = requireSha(required(properties, "packetRoot"), "packetRoot");
        if (!packetRoot.equals(sha256(packet))) {
            throw new IllegalStateException("Synexia handoff packet root drift");
        }

        int expectedRows = positiveInt(required(properties, "rows"), "rows", MAX_ROWS);
        long expectedBytes = boundedLong(required(properties, "payloadBytes"), "payloadBytes", MAX_PAYLOAD_BYTES);

        List<String> lines = packet.lines().toList();
        if (lines.size() < 4
                || !("# " + VERSION).equals(lines.get(0))
                || !("@crate\t" + crate).equals(lines.get(1))
                || !("@source-revision\t" + revision).equals(lines.get(2))) {
            throw new IllegalStateException("invalid Synexia handoff packet header");
        }

        List<Row> rows = new ArrayList<>();
        Set<String> targets = new HashSet<>();
        List<String> javaManifest = new ArrayList<>();
        List<String> textManifest = new ArrayList<>();
        long payloadBytes = 0L;

        for (int index = 3; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) continue;
            String[] cells = line.split("\t", -1);
            if (cells.length != 13 || !"ROW".equals(cells[0])) {
                throw new IllegalStateException("invalid Synexia handoff packet row");
            }
            Kind kind;
            try {
                kind = Kind.valueOf(cells[1]);
            } catch (IllegalArgumentException badKind) {
                throw new IllegalStateException("invalid Synexia handoff kind", badKind);
            }
            String capability = token(cells[2], "capability");
            String sourcePath = relative(cells[3], "sourcePath");
            String sourceSha = requireSha(cells[4], "sourceSha");
            String targetPath = relative(cells[5], "targetPath");
            String before = "ABSENT".equals(cells[6]) ? "ABSENT" : requireSha(cells[6], "targetBefore");
            String after = requireSha(cells[7], "targetAfter");
            String payload = leaf(cells[8], "payload");
            String license = scalar(cells[9], "license", 128);
            String recipeId = token(cells[10], "recipeId");
            String contractRoot = requireSha(cells[11], "contractRoot");
            String gateRoot = requireSha(cells[12], "gateRoot");

            if (!sourceSha.equals(after)) {
                throw new IllegalStateException("handoff target postimage differs from reviewed source bytes");
            }
            if (kind == Kind.JAVA && (!sourcePath.endsWith(".java") || !targetPath.endsWith(".java"))) {
                throw new IllegalStateException("JAVA handoff row must map .java to .java");
            }
            if (!targets.add(targetPath)) {
                throw new IllegalStateException("duplicate Synexia handoff target: " + targetPath);
            }

            String payloadRoot = kind == Kind.JAVA ? JAVA_ROOT : TEXT_ROOT;
            String payloadText = resource(payloadRoot + crate + "/" + payload);
            String payloadSha = sha256(payloadText);
            if (!after.equals(payloadSha)) {
                throw new IllegalStateException("Synexia handoff payload hash drift: " + targetPath);
            }
            payloadBytes = Math.addExact(payloadBytes, payloadText.getBytes(StandardCharsets.UTF_8).length);
            if (payloadBytes > MAX_PAYLOAD_BYTES) {
                throw new IllegalStateException("Synexia handoff payload budget");
            }

            String manifest = targetPath + "\t" + before + "\t" + after + "\t" + payload;
            if (kind == Kind.JAVA) javaManifest.add(manifest);
            else textManifest.add(manifest);

            rows.add(new Row(kind, capability, sourcePath, sourceSha, targetPath, before, after,
                    payload, license, recipeId, contractRoot, gateRoot));
            if (rows.size() > MAX_ROWS) {
                throw new IllegalStateException("Synexia handoff row budget");
            }
        }

        if (rows.size() != expectedRows || payloadBytes != expectedBytes) {
            throw new IllegalStateException("Synexia handoff packet accounting mismatch");
        }
        if (rows.isEmpty()) {
            throw new IllegalStateException("empty Synexia handoff packet");
        }

        verifyManifest(JAVA_ROOT, crate, javaManifest);
        verifyManifest(TEXT_ROOT, crate, textManifest);
        return new Verified(crate, revision, packetRoot, rows.size(), payloadBytes);
    }


    static Verified verify(Path exportRoot, String crateName) {
        return inspect(exportRoot, crateName).verified();
    }

    static Inspection inspect(Path exportRoot, String crateName) {
        String crate = checkedCrate(crateName);
        Path root = Objects.requireNonNull(exportRoot, "exportRoot").toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root)) {
            throw new IllegalArgumentException("exportRoot must be a real directory");
        }
        Path resources = root.resolve(RESOURCE_PREFIX).normalize();
        if (!resources.startsWith(root)
                || !Files.isDirectory(resources, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(resources)) {
            throw new IllegalStateException("missing handoff resource root");
        }

        String bridgeBase = BRIDGE_ROOT.substring(1) + crate + "/";
        String packetName = bridgeBase + "packet.tsv";
        String propertiesName = bridgeBase + "bridge.properties";
        String packet = external(resources, packetName);
        Map<String, String> values = properties(external(resources, propertiesName));
        exact(values, "version", VERSION);
        exact(values, "crate", crate);
        String revision = requireRevision(required(values, "sourceRevision"));
        String packetRoot = requireSha(required(values, "packetRoot"), "packetRoot");
        if (!packetRoot.equals(sha256(packet))) {
            throw new IllegalStateException("Synexia handoff packet root drift");
        }
        int expectedRows = positiveInt(required(values, "rows"), "rows", MAX_ROWS);
        long expectedBytes =
                boundedLong(required(values, "payloadBytes"), "payloadBytes", MAX_PAYLOAD_BYTES);

        List<String> lines = packet.lines().toList();
        if (lines.size() < 4
                || !("# " + VERSION).equals(lines.get(0))
                || !("@crate\t" + crate).equals(lines.get(1))
                || !("@source-revision\t" + revision).equals(lines.get(2))) {
            throw new IllegalStateException("invalid Synexia handoff packet header");
        }

        List<String> javaManifest = new ArrayList<>();
        List<String> textManifest = new ArrayList<>();
        List<String> expectedResources = new ArrayList<>();
        expectedResources.add(RESOURCE_PREFIX + propertiesName);
        expectedResources.add(RESOURCE_PREFIX + packetName);
        Set<String> targets = new HashSet<>();
        Set<String> payloadResources = new HashSet<>();
        long payloadBytes = 0L;
        int rows = 0;

        for (int index = 3; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) continue;
            String[] cells = line.split("\t", -1);
            if (cells.length != 13 || !"ROW".equals(cells[0])) {
                throw new IllegalStateException("invalid Synexia handoff packet row");
            }
            Kind kind;
            try {
                kind = Kind.valueOf(cells[1]);
            } catch (IllegalArgumentException badKind) {
                throw new IllegalStateException("invalid Synexia handoff kind", badKind);
            }
            token(cells[2], "capability");
            String sourcePath = relative(cells[3], "sourcePath");
            String sourceSha = requireSha(cells[4], "sourceSha");
            String targetPath = relative(cells[5], "targetPath");
            String before =
                    "ABSENT".equals(cells[6])
                            ? "ABSENT"
                            : requireSha(cells[6], "targetBefore");
            String after = requireSha(cells[7], "targetAfter");
            String payload = leaf(cells[8], "payload");
            scalar(cells[9], "license", 128);
            token(cells[10], "recipeId");
            requireSha(cells[11], "contractRoot");
            requireSha(cells[12], "gateRoot");

            if (!sourceSha.equals(after)) {
                throw new IllegalStateException(
                        "handoff target postimage differs from reviewed source bytes");
            }
            if (kind == Kind.JAVA
                    && (!sourcePath.endsWith(".java")
                            || !M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(targetPath))) {
                throw new IllegalStateException(
                        "JAVA handoff row is outside the typed receiver roots");
            }
            if (!targets.add(targetPath)) {
                throw new IllegalStateException(
                        "duplicate Synexia handoff target: " + targetPath);
            }

            String ownerRoot =
                    (kind == Kind.JAVA ? JAVA_ROOT : TEXT_ROOT).substring(1);
            String payloadName = ownerRoot + crate + "/" + payload;
            if (!payloadResources.add(payloadName)) {
                throw new IllegalStateException(
                        "duplicate Synexia handoff payload: " + payload);
            }
            String payloadText = external(resources, payloadName);
            if (!after.equals(sha256(payloadText))) {
                throw new IllegalStateException(
                        "Synexia handoff payload hash drift: " + targetPath);
            }
            payloadBytes =
                    Math.addExact(
                            payloadBytes,
                            payloadText.getBytes(StandardCharsets.UTF_8).length);
            if (payloadBytes > MAX_PAYLOAD_BYTES) {
                throw new IllegalStateException("Synexia handoff payload budget");
            }
            expectedResources.add(RESOURCE_PREFIX + payloadName);

            String manifest =
                    targetPath + "\t" + before + "\t" + after + "\t" + payload;
            if (kind == Kind.JAVA) javaManifest.add(manifest);
            else textManifest.add(manifest);
            if (++rows > MAX_ROWS) {
                throw new IllegalStateException("Synexia handoff row budget");
            }
        }

        if (rows != expectedRows || payloadBytes != expectedBytes) {
            throw new IllegalStateException("Synexia handoff packet accounting mismatch");
        }
        if (rows == 0) throw new IllegalStateException("empty Synexia handoff packet");

        externalManifest(resources, JAVA_ROOT, crate, javaManifest, expectedResources);
        externalManifest(resources, TEXT_ROOT, crate, textManifest, expectedResources);

        String aliasName = "META-INF/rewrite/m3-" + crate + ".yml";
        String alias = external(resources, aliasName);
        if (!canonicalAlias(crate, !javaManifest.isEmpty(), !textManifest.isEmpty())
                .equals(alias)) {
            throw new IllegalStateException("Synexia handoff recipe alias drift");
        }
        expectedResources.add(RESOURCE_PREFIX + aliasName);
        expectedResources.sort(String::compareTo);
        return new Inspection(
                new Verified(crate, revision, packetRoot, rows, payloadBytes),
                expectedResources);
    }

    private static void externalManifest(
            Path resources,
            String ownerRoot,
            String crate,
            List<String> expected,
            List<String> expectedResources) {
        String name = ownerRoot.substring(1) + crate + "/manifest.tsv";
        Path path = resources.resolve(name).normalize();
        boolean exists =
                path.startsWith(resources)
                        && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                        && !Files.isSymbolicLink(path);
        if (expected.isEmpty()) {
            if (exists) {
                throw new IllegalStateException(
                        "unexpected empty receiver manifest: " + name);
            }
            return;
        }
        if (!exists) {
            throw new IllegalStateException("missing receiver manifest: " + name);
        }
        String actual = external(resources, name);
        List<String> rows =
                actual.lines()
                        .filter(line -> !line.isBlank() && !line.startsWith("#"))
                        .toList();
        if (!rows.equals(expected)) {
            throw new IllegalStateException(
                    "receiver manifest differs from Synexia handoff packet: " + name);
        }
        expectedResources.add(RESOURCE_PREFIX + name);
    }

    private static String external(Path resources, String relativeName) {
        Path path = resources.resolve(relativeName).normalize();
        if (!path.startsWith(resources)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(path)) {
            throw new IllegalStateException(
                    "missing or unsafe Synexia bridge resource: " + relativeName);
        }
        try {
            long size = Files.size(path);
            if (size > MAX_RESOURCE_BYTES) {
                throw new IllegalStateException(
                        "Synexia handoff resource budget: " + relativeName);
            }
            byte[] bytes = Files.readAllBytes(path);
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes))
                    .toString();
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read Synexia bridge resource: " + relativeName, failure);
        }
    }

    private static String canonicalAlias(String crate, boolean java, boolean text) {
        StringBuilder yaml =
                new StringBuilder(
                        "# SPDX-License-Identifier: Apache-2.0\n---\n"
                                + "type: specs.openrewrite.org/v1beta/recipe\n"
                                + "name: com.m3.rewrite.backport.SynexiaBridge"
                                + camel(crate.substring("synexia-".length()))
                                + "\n"
                                + "displayName: Receive reviewed Synexia handoff "
                                + crate
                                + "\n"
                                + "description: Replays exact target-ready Synexia bytes through existing M3JDK21 hash-pinned owners.\n"
                                + "tags:\n  - m3\n  - synexia\n  - jdk21\n  - hash-pinned\n  - candidate-only\n"
                                + "recipeList:\n"
                                + "  - com.m3.rewrite.backport.M3SynexiaHandoffGuardRecipe:\n"
                                + "      crateName: "
                                + crate
                                + "\n");
        if (java) {
            yaml.append(
                            "  - com.m3.rewrite.backport.M3Jdk21HashPinnedSnapshotRecipe:\n")
                    .append("      crateName: ")
                    .append(crate)
                    .append('\n');
        }
        if (text) {
            yaml.append(
                            "  - com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe:\n")
                    .append("      crateName: ")
                    .append(crate)
                    .append('\n');
        }
        return yaml.toString();
    }

    private static String camel(String value) {
        StringBuilder out = new StringBuilder();
        for (String part : value.split("-")) {
            if (!part.isEmpty()) {
                out.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1));
            }
        }
        return out.toString();
    }

    private static void verifyManifest(String ownerRoot, String crate, List<String> expected) {
        String name = ownerRoot + crate + "/manifest.tsv";
        String actual = resourceOrNull(name);
        if (expected.isEmpty()) {
            if (actual != null) throw new IllegalStateException("unexpected empty receiver manifest: " + name);
            return;
        }
        if (actual == null) throw new IllegalStateException("missing receiver manifest: " + name);
        List<String> lines = actual.lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .toList();
        if (!lines.equals(expected)) {
            throw new IllegalStateException("receiver manifest differs from Synexia handoff packet: " + name);
        }
    }

    private static Map<String, String> properties(String text) {
        Map<String, String> values = new HashMap<>();
        for (String line : text.lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            int at = line.indexOf('=');
            if (at <= 0 || at == line.length() - 1) {
                throw new IllegalStateException("invalid Synexia bridge properties");
            }
            String key = line.substring(0, at);
            String value = line.substring(at + 1);
            if (values.putIfAbsent(key, value) != null) {
                throw new IllegalStateException("duplicate Synexia bridge property: " + key);
            }
        }
        Set<String> expected =
                Set.of("version", "crate", "sourceRevision", "packetRoot", "rows", "payloadBytes");
        if (!values.keySet().equals(expected)) {
            throw new IllegalStateException("unexpected Synexia bridge properties");
        }
        return Map.copyOf(values);
    }

    private static void exact(Map<String, String> values, String key, String expected) {
        if (!expected.equals(required(values, key))) {
            throw new IllegalStateException("Synexia bridge property mismatch: " + key);
        }
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null) throw new IllegalStateException("missing Synexia bridge property: " + key);
        return value;
    }

    static String checkedCrate(String value) {
        String checked = Objects.requireNonNull(value, "crateName");
        if (!checked.matches("synexia-[a-z0-9][a-z0-9-]{0,63}")) {
            throw new IllegalArgumentException("invalid Synexia handoff crate");
        }
        return checked;
    }

    private static String requireRevision(String value) {
        if (value == null || !value.matches("[0-9a-f]{40}")) {
            throw new IllegalStateException("invalid Synexia source revision");
        }
        return value;
    }

    private static String requireSha(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalStateException("invalid " + field);
        }
        return value;
    }

    private static String token(String value, String field) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,159}")) {
            throw new IllegalStateException("invalid " + field);
        }
        return value;
    }

    private static String scalar(String value, String field, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum
                || value.indexOf('\t') >= 0 || value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf((char) 0) >= 0) {
            throw new IllegalStateException("invalid " + field);
        }
        return value;
    }

    private static String relative(String value, String field) {
        String checked = scalar(value, field, 4096);
        if (checked.startsWith("/") || checked.indexOf('\\') >= 0 || checked.matches("^[A-Za-z]:.*")) {
            throw new IllegalStateException("invalid " + field);
        }
        for (String part : checked.split("/", -1)) {
            if (part.isEmpty() || ".".equals(part) || "..".equals(part) || ".git".equalsIgnoreCase(part)) {
                throw new IllegalStateException("noncanonical " + field);
            }
        }
        return checked;
    }

    private static String leaf(String value, String field) {
        String checked = scalar(value, field, 255);
        if (checked.indexOf('/') >= 0 || checked.indexOf('\\') >= 0
                || ".".equals(checked) || "..".equals(checked)) {
            throw new IllegalStateException("invalid " + field);
        }
        return checked;
    }

    private static int positiveInt(String value, String field, int maximum) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1 || parsed > maximum) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException invalid) {
            throw new IllegalStateException("invalid " + field, invalid);
        }
    }

    private static long boundedLong(String value, String field, long maximum) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 0 || parsed > maximum) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException invalid) {
            throw new IllegalStateException("invalid " + field, invalid);
        }
    }

    private static String resource(String name) {
        String value = resourceOrNull(name);
        if (value == null) throw new IllegalStateException("missing Synexia bridge resource: " + name);
        return value;
    }

    private static String resourceOrNull(String name) {
        try (var stream = SynexiaHandoffPacket.class.getResourceAsStream(name)) {
            if (stream == null) return null;
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(stream.readAllBytes()))
                    .toString();
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read Synexia bridge resource: " + name, failure);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
