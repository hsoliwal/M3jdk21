// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Independent M3JDK21 verifier for a concrete Synexia delivery-export TSV. */
public record SynexiaImportManifest(
        String sourceRevision,
        String targetId,
        List<Entry> entries,
        String root) {

    public static final String HEADER =
            "source_revision\ttarget_id\tcategory\tsource_path\ttarget_path\tsha256\tlicense\tmode";

    public SynexiaImportManifest {
        sourceRevision = commit(sourceRevision);
        targetId = token(targetId, "targetId");
        if (!"m3jdk21".equals(targetId)) {
            throw new IllegalArgumentException("M3JDK21 receiver requires target_id=m3jdk21");
        }
        entries = Objects.requireNonNull(entries, "entries").stream()
                .map(entry -> Objects.requireNonNull(entry, "entry"))
                .sorted(Comparator.comparing(Entry::targetPath))
                .toList();
        if (entries.isEmpty()) throw new IllegalArgumentException("empty Synexia import manifest");
        for (int index = 1; index < entries.size(); index++) {
            if (entries.get(index - 1).targetPath().equals(entries.get(index).targetPath())) {
                throw new IllegalArgumentException(
                        "duplicate target path: " + entries.get(index).targetPath());
            }
        }
        String expected = root(sourceRevision, targetId, entries);
        root = root == null || root.isBlank() ? expected : sha(root, "root");
        if (!root.equals(expected)) {
            throw new IllegalArgumentException("Synexia import manifest root mismatch");
        }
    }

    public static SynexiaImportManifest parse(String tsv) {
        List<String> lines = Objects.requireNonNull(tsv, "tsv").lines().toList();
        if (lines.isEmpty() || !HEADER.equals(lines.getFirst())) {
            throw new IllegalArgumentException("invalid Synexia import header");
        }

        ArrayList<Entry> entries = new ArrayList<>();
        String revision = null;
        String target = null;
        String declaredRoot = null;
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) continue;
            if (line.startsWith("# root\t")) {
                if (declaredRoot != null) throw new IllegalArgumentException("duplicate root row");
                declaredRoot = line.substring("# root\t".length());
                continue;
            }
            if (line.startsWith("#")) continue;

            String[] cells = line.split("\t", -1);
            if (cells.length != 8) {
                throw new IllegalArgumentException("invalid Synexia import row " + (index + 1));
            }
            if (revision == null) revision = cells[0];
            if (target == null) target = cells[1];
            if (!revision.equals(cells[0]) || !target.equals(cells[1])) {
                throw new IllegalArgumentException("manifest row revision/target drift");
            }
            entries.add(new Entry(
                    cells[2],
                    cells[3],
                    cells[4],
                    cells[5],
                    cells[6],
                    Mode.valueOf(cells[7])));
        }
        if (revision == null || target == null || declaredRoot == null) {
            throw new IllegalArgumentException("incomplete Synexia import manifest");
        }
        return new SynexiaImportManifest(revision, target, entries, declaredRoot);
    }

    public String toTsv() {
        StringBuilder out = new StringBuilder(HEADER).append('\n');
        for (Entry entry : entries) {
            out.append(sourceRevision).append('\t')
                    .append(targetId).append('\t')
                    .append(entry.category()).append('\t')
                    .append(entry.sourcePath()).append('\t')
                    .append(entry.targetPath()).append('\t')
                    .append(entry.sha256()).append('\t')
                    .append(entry.license()).append('\t')
                    .append(entry.mode()).append('\n');
        }
        return out.append("# root\t").append(root).append('\n').toString();
    }

    public enum Mode {
        APACHE_SOURCE,
        APACHE_RECIPE_RESOURCE
    }

    public record Entry(
            String category,
            String sourcePath,
            String targetPath,
            String sha256,
            String license,
            Mode mode) {
        public Entry {
            category = token(category, "category");
            sourcePath = relative(sourcePath, "sourcePath");
            targetPath = relative(targetPath, "targetPath");
            if (!targetPath.startsWith("m3/vendor/synexia/")) {
                throw new IllegalArgumentException(
                        "automatic Synexia import must remain below m3/vendor/synexia");
            }
            sha256 = sha(sha256, "sha256");
            license = token(license, "license");
            if (!"Apache-2.0".equals(license)) {
                throw new IllegalArgumentException("automatic Synexia import requires Apache-2.0");
            }
            mode = Objects.requireNonNull(mode, "mode");
            SynexiaCanonicalFamilyPolicy.requireMirrorTarget(sourcePath, targetPath);
            SynexiaCanonicalFamilyPolicy.requireCategoryFamily(category, sourcePath);
        }
    }

    private static String root(String revision, String targetId, List<Entry> entries) {
        MessageDigest digest = digest();
        frame(digest, "M3_DELIVERY_EXPORT_V1");
        frame(digest, revision);
        frame(digest, targetId);
        for (Entry entry : entries) {
            frame(digest, entry.category());
            frame(digest, entry.sourcePath());
            frame(digest, entry.targetPath());
            frame(digest, entry.sha256());
            frame(digest, entry.license());
            frame(digest, entry.mode().name());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static String commit(String value) {
        String checked = token(value, "sourceRevision").toLowerCase(Locale.ROOT);
        if (!(checked.matches("[0-9a-f]{40}") || checked.matches("[0-9a-f]{64}"))) {
            throw new IllegalArgumentException("sourceRevision");
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = token(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String token(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0 || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String relative(String value, String field) {
        String checked = token(value, field).replace('\\', '/');
        if (checked.startsWith("/")
                || checked.equals("..")
                || checked.startsWith("../")
                || checked.contains("/../")
                || checked.endsWith("/..")
                || checked.contains("//")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
