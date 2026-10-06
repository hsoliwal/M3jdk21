// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Plans and stages a current/full Synexia delivery without writing the vendored target tree.
 *
 * <p>The plan compares the sealed delivery manifest with the current
 * {@code m3/vendor/synexia} snapshot. Only ADD/REPLACE candidates may be copied, and only under
 * {@code m3/build}. KEEP rows prove reuse; STALE rows are evidence only and never authorize
 * deletion.</p>
 */
public final class SynexiaImportPlan {
    public static final String HEADER =
            "source_revision\tmanifest_root\tcategory\tsource_path\ttarget_path\tmode\t"
                    + "expected_sha256\tcurrent_sha256\taction\tlane";

    public enum Action {
        ADD,
        REPLACE,
        KEEP,
        STALE
    }

    public enum Lane {
        OPENREWRITE_RECIPE,
        OPENREWRITE_TEST,
        OPENREWRITE_RESOURCE,
        CONVERGENCE_JAVA,
        CONVERGENCE_TEST,
        CONVERGENCE_NATIVE,
        M3_CLONER,
        M3INDEX,
        M3_RECIPE,
        OTHER_APACHE
    }

    public record Row(
            String category,
            String sourcePath,
            String targetPath,
            String mode,
            String expectedSha256,
            String currentSha256,
            Action action,
            Lane lane) {
        public Row {
            category = token(category, "category");
            sourcePath = markerOrPath(sourcePath, "sourcePath", Set.of("NONE"));
            targetPath = relative(targetPath, "targetPath");
            if (!targetPath.startsWith("m3/vendor/synexia/")) {
                throw new IllegalArgumentException("targetPath");
            }
            mode = token(mode, "mode");
            expectedSha256 =
                    digestOrMarker(expectedSha256, "expectedSha256", Set.of("STALE"));
            currentSha256 =
                    digestOrMarker(currentSha256, "currentSha256", Set.of("ABSENT"));
            action = Objects.requireNonNull(action, "action");
            lane = Objects.requireNonNull(lane, "lane");
            if (action == Action.STALE) {
                if (!"stale".equals(category)
                        || !"NONE".equals(sourcePath)
                        || !"STALE".equals(mode)
                        || !"STALE".equals(expectedSha256)
                        || "ABSENT".equals(currentSha256)) {
                    throw new IllegalArgumentException("STALE row");
                }
            } else {
                if ("NONE".equals(sourcePath)
                        || "STALE".equals(expectedSha256)
                        || (!"APACHE_SOURCE".equals(mode)
                                && !"APACHE_RECIPE_RESOURCE".equals(mode))) {
                    throw new IllegalArgumentException("delivery row");
                }
                switch (action) {
                    case ADD -> {
                        if (!"ABSENT".equals(currentSha256)) {
                            throw new IllegalArgumentException("ADD currentSha256");
                        }
                    }
                    case KEEP -> {
                        if ("ABSENT".equals(currentSha256)
                                || !currentSha256.equals(expectedSha256)) {
                            throw new IllegalArgumentException("KEEP hash relation");
                        }
                    }
                    case REPLACE -> {
                        if ("ABSENT".equals(currentSha256)
                                || currentSha256.equals(expectedSha256)) {
                            throw new IllegalArgumentException("REPLACE hash relation");
                        }
                    }
                    case STALE -> throw new AssertionError();
                }
            }
        }
    }

    public record Plan(
            String sourceRevision,
            String manifestRoot,
            List<Row> rows,
            String root) {
        public Plan {
            sourceRevision = commit(sourceRevision);
            manifestRoot = sha(manifestRoot, "manifestRoot");
            rows = Objects.requireNonNull(rows, "rows").stream()
                    .map(row -> Objects.requireNonNull(row, "row"))
                    .sorted(Comparator.comparing(Row::targetPath))
                    .toList();
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("empty Synexia import plan");
            }
            String previous = null;
            for (Row row : rows) {
                if (row.targetPath().equals(previous)) {
                    throw new IllegalArgumentException(
                            "duplicate plan target: " + row.targetPath());
                }
                previous = row.targetPath();
            }
            String expected = root(sourceRevision, manifestRoot, rows);
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("Synexia import plan root mismatch");
            }
        }

        public long changedCount() {
            return rows.stream()
                    .filter(row -> row.action() == Action.ADD || row.action() == Action.REPLACE)
                    .count();
        }

        public long staleCount() {
            return rows.stream().filter(row -> row.action() == Action.STALE).count();
        }

        public boolean sourceWriteAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }

        public String toTsv() {
            StringBuilder out = new StringBuilder(HEADER).append('\n');
            for (Row row : rows) {
                out.append(sourceRevision).append('\t')
                        .append(manifestRoot).append('\t')
                        .append(row.category()).append('\t')
                        .append(row.sourcePath()).append('\t')
                        .append(row.targetPath()).append('\t')
                        .append(row.mode()).append('\t')
                        .append(row.expectedSha256()).append('\t')
                        .append(row.currentSha256()).append('\t')
                        .append(row.action()).append('\t')
                        .append(row.lane()).append('\n');
            }
            return out.append("# root\t").append(root).append('\n').toString();
        }
    }

    private SynexiaImportPlan() {}

    public static Plan plan(Path m3jdkRoot, SynexiaImportManifest manifest) throws IOException {
        Path root = existingRoot(m3jdkRoot, "m3jdkRoot");
        SynexiaImportManifest checked = Objects.requireNonNull(manifest, "manifest");
        ArrayList<Row> rows = new ArrayList<>();
        Set<String> manifestTargets = new HashSet<>();

        for (SynexiaImportManifest.Entry entry : checked.entries()) {
            manifestTargets.add(entry.targetPath());
            Path target = resolve(root, entry.targetPath());
            String current = "ABSENT";
            Action action = Action.ADD;
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                requireRegular(target, "M3JDK21 vendor target");
                current = sha256(Files.readAllBytes(target));
                action = current.equals(entry.sha256()) ? Action.KEEP : Action.REPLACE;
            }
            rows.add(new Row(
                    entry.category(),
                    entry.sourcePath(),
                    entry.targetPath(),
                    entry.mode().name(),
                    entry.sha256(),
                    current,
                    action,
                    lane(entry.category(), entry.targetPath())));
        }

        appendStale(root, manifestTargets, rows);
        return new Plan(checked.sourceRevision(), checked.root(), rows, "");
    }

    public static Plan stage(
            Path synexiaRoot,
            Path m3jdkRoot,
            SynexiaImportManifest manifest,
            Path output) throws IOException {
        SynexiaImportManifest checked = Objects.requireNonNull(manifest, "manifest");
        SynexiaImporter.verifySources(synexiaRoot, checked);
        Path m3Root = existingRoot(m3jdkRoot, "m3jdkRoot");
        Plan plan = plan(m3Root, checked);
        Path out = buildOutput(m3Root, output);
        Map<String, SynexiaImportManifest.Entry> entries = new HashMap<>();
        for (SynexiaImportManifest.Entry entry : checked.entries()) {
            entries.put(entry.targetPath(), entry);
        }

        for (Row row : plan.rows()) {
            if (row.action() != Action.ADD && row.action() != Action.REPLACE) {
                continue;
            }
            SynexiaImportManifest.Entry entry = entries.get(row.targetPath());
            if (entry == null) {
                throw new IllegalStateException("changed row lacks manifest entry");
            }
            Path source = resolve(existingRoot(synexiaRoot, "synexiaRoot"), entry.sourcePath());
            byte[] bytes = Files.readAllBytes(source);
            if (!sha256(bytes).equals(entry.sha256())) {
                throw new IllegalStateException("staged Synexia source hash drift");
            }
            writeOrVerify(
                    out,
                    out.resolve("candidate").resolve(row.targetPath()),
                    bytes);
        }

        writeOrVerify(
                out,
                out.resolve("PLAN.tsv"),
                plan.toTsv().getBytes(StandardCharsets.UTF_8));
        writeOrVerify(
                out,
                out.resolve("ROOT"),
                ("ROOT  " + plan.root() + "\n").getBytes(StandardCharsets.UTF_8));
        return plan;
    }

    public static void writePlan(Path m3jdkRoot, Path output, Plan plan) throws IOException {
        Path root = existingRoot(m3jdkRoot, "m3jdkRoot");
        Path out = buildOutput(root, output);
        Plan checked = Objects.requireNonNull(plan, "plan");
        writeOrVerify(
                out,
                out.resolve("PLAN.tsv"),
                checked.toTsv().getBytes(StandardCharsets.UTF_8));
        writeOrVerify(
                out,
                out.resolve("ROOT"),
                ("ROOT  " + checked.root() + "\n").getBytes(StandardCharsets.UTF_8));
    }

    static Lane lane(String category, String targetPath) {
        String value = Objects.toString(category, "");
        return switch (value) {
            case "openrewrite-java" -> Lane.OPENREWRITE_RECIPE;
            case "openrewrite-tests" -> Lane.OPENREWRITE_TEST;
            case "openrewrite-resources" -> Lane.OPENREWRITE_RESOURCE;
            case "convergence-java" -> Lane.CONVERGENCE_JAVA;
            case "convergence-tests" -> Lane.CONVERGENCE_TEST;
            case "convergence-native" -> Lane.CONVERGENCE_NATIVE;
            case "m3-cloner-java", "m3-cloner-tests" -> Lane.M3_CLONER;
            case "m3index-source" -> Lane.M3INDEX;
            case "m3-recipe-source" -> Lane.M3_RECIPE;
            default -> laneFromTarget(targetPath);
        };
    }

    private static Lane laneFromTarget(String targetPath) {
        String path = relative(targetPath, "targetPath");
        if (path.contains("/synexia-openrewrite-recipes/src/main/java/")) {
            return Lane.OPENREWRITE_RECIPE;
        }
        if (path.contains("/synexia-openrewrite-recipes/src/test/java/")) {
            return Lane.OPENREWRITE_TEST;
        }
        if (path.contains("/synexia-openrewrite-recipes/src/main/resources/")) {
            return Lane.OPENREWRITE_RESOURCE;
        }
        if (path.contains("/synexia-code-convergence/src/main/java/")) {
            return Lane.CONVERGENCE_JAVA;
        }
        if (path.contains("/synexia-code-convergence/src/test/java/")) {
            return Lane.CONVERGENCE_TEST;
        }
        if (path.contains("/synexia-code-convergence/src/main/native/")) {
            return Lane.CONVERGENCE_NATIVE;
        }
        if (path.contains("/synexia-m3-cloner/")) return Lane.M3_CLONER;
        if (path.contains("/synexia-m3index/")) return Lane.M3INDEX;
        if (path.contains("/synexia-m3-recipe/")) return Lane.M3_RECIPE;
        return Lane.OTHER_APACHE;
    }

    private static void appendStale(
            Path root,
            Set<String> manifestTargets,
            List<Row> rows) throws IOException {
        Path vendor = root.resolve("m3/vendor/synexia");
        if (!Files.exists(vendor, LinkOption.NOFOLLOW_LINKS)) return;
        if (Files.isSymbolicLink(vendor) || !Files.isDirectory(vendor, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Synexia vendor root is not a regular directory");
        }
        Files.walkFileTree(
                vendor,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(
                            Path dir, BasicFileAttributes attributes) {
                        if (Files.isSymbolicLink(dir)) {
                            throw new IllegalStateException(
                                    "Synexia vendor tree contains symlink directory");
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                            Path file, BasicFileAttributes attributes) throws IOException {
                        requireRegular(file, "Synexia vendor file");
                        String target = normalized(root.relativize(file));
                        if (!manifestTargets.contains(target)) {
                            rows.add(new Row(
                                    "stale",
                                    "NONE",
                                    target,
                                    "STALE",
                                    "STALE",
                                    sha256(Files.readAllBytes(file)),
                                    Action.STALE,
                                    lane("stale", target)));
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
    }

    private static Path buildOutput(Path root, Path output) throws IOException {
        Path m3 = root.resolve("m3").normalize();
        if (Files.isSymbolicLink(m3)
                || !Files.isDirectory(m3, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("M3 root is not a regular directory");
        }
        Path build = m3.resolve("build").normalize();
        Path candidate = Objects.requireNonNull(output, "output");
        Path resolved = candidate.isAbsolute()
                ? candidate.toAbsolutePath().normalize()
                : root.resolve(candidate).normalize();
        if (!resolved.startsWith(build) || resolved.equals(build)) {
            throw new IllegalArgumentException(
                    "Synexia import plan output must stay below m3/build");
        }
        requireDirectoryChain(m3, resolved);
        return resolved;
    }

    private static void requireDirectoryChain(Path trustedRoot, Path directory)
            throws IOException {
        Path root = trustedRoot.toAbsolutePath().normalize();
        Path target = directory.toAbsolutePath().normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("directory escaped trusted root");
        }
        if (Files.isSymbolicLink(root)
                || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("trusted output root is not a regular directory");
        }
        Path current = root;
        for (Path part : root.relativize(target)) {
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(current)
                        || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException(
                            "staged output ancestor is not a regular directory: " + current);
                }
            } else {
                Files.createDirectory(current);
            }
        }
    }

    private static Path existingRoot(Path value, String field) {
        Path root = Objects.requireNonNull(value, field).toAbsolutePath().normalize();
        if (Files.isSymbolicLink(root)
                || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException(field + " must be an existing directory");
        }
        return root;
    }

    private static Path resolve(Path root, String relative) {
        Path result = root.resolve(relative(relative, "relative")).normalize();
        if (!result.startsWith(root)) {
            throw new IllegalArgumentException("path escapes root");
        }
        return result;
    }

    private static void requireRegular(Path path, String label) {
        if (Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException(label + " is not a regular file: " + path);
        }
    }

    private static void writeOrVerify(
            Path outputRoot, Path path, byte[] bytes) throws IOException {
        Path root = outputRoot.toAbsolutePath().normalize();
        Path target = path.toAbsolutePath().normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new IllegalArgumentException("staged output escaped output root");
        }
        Path parent = target.getParent();
        if (parent == null) throw new IOException("output has no parent");
        requireDirectoryChain(root, parent);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            requireRegular(target, "staged output");
            if (!MessageDigest.isEqual(Files.readAllBytes(target), bytes)) {
                throw new IllegalStateException("staged output drift: " + target);
            }
            return;
        }
        Path temporary = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        boolean moved = false;
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target);
            }
            moved = true;
        } finally {
            if (!moved) Files.deleteIfExists(temporary);
        }
    }

    private static String root(String revision, String manifestRoot, List<Row> rows) {
        MessageDigest digest = digest();
        frame(digest, "M3_SYNEXIA_IMPORT_PLAN_V1");
        frame(digest, revision);
        frame(digest, manifestRoot);
        for (Row row : rows) {
            frame(digest, row.category());
            frame(digest, row.sourcePath());
            frame(digest, row.targetPath());
            frame(digest, row.mode());
            frame(digest, row.expectedSha256());
            frame(digest, row.currentSha256());
            frame(digest, row.action().name());
            frame(digest, row.lane().name());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private static String markerOrPath(
            String value, String field, Set<String> markers) {
        String checked = token(value, field);
        return markers.contains(checked) ? checked : relative(checked, field);
    }

    private static String digestOrMarker(
            String value, String field, Set<String> markers) {
        String checked = token(value, field);
        return markers.contains(checked) ? checked : sha(checked, field);
    }

    private static String relative(String value, String field) {
        String checked = token(value, field).replace('\\', '/');
        if (checked.startsWith("/") || checked.contains("//")) {
            throw new IllegalArgumentException(field);
        }
        for (String part : checked.split("/", -1)) {
            if (part.isEmpty() || ".".equals(part) || "..".equals(part)) {
                throw new IllegalArgumentException(field);
            }
        }
        return checked;
    }

    private static String commit(String value) {
        String checked = token(value, "sourceRevision").toLowerCase(java.util.Locale.ROOT);
        if (!(checked.matches("[0-9a-f]{40}") || checked.matches("[0-9a-f]{64}"))) {
            throw new IllegalArgumentException("sourceRevision");
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = token(value, field).toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String token(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\t') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha256(byte[] bytes) {
        return HexFormat.of().formatHex(digest().digest(bytes));
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
