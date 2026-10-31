/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.naming;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Scans the tracked files of a repository for names the rules would still rewrite: the conformance census. Scope,
 * lineage exclusions and the recipe's own files mirror {@code jdk_naming.py} (in_scope, is_lineage, SELF).
 */
public final class NamingScan {

    /** One non-conformant name: line 0 is the path itself. */
    public record Violation(String path, int line, String rules, String excerpt) {
        @Override
        public String toString() {
            return path + ":" + line + " " + rules + " " + excerpt;
        }
    }

    static final Set<String> TEXT_SUFFIXES = Set.of(".java", ".yml", ".yaml", ".xml", ".sh", ".py", ".mjs", ".js",
            ".properties", ".gradle", ".toml", ".cfg", ".ini", ".c", ".h", ".cpp", ".json", ".txt");
    static final Set<String> DOC_SUFFIXES = Set.of(".md", ".tsv", ".rst", ".adoc", ".html", ".log", ".patch");
    static final Set<String> DOC_NAMES = Set.of("NOTICE", "LICENSE", "COPYRIGHT", "AUTHORS", "THIRD_PARTY_NOTICES");
    static final List<String> LINEAGE_HINTS = List.of("provenance", "evidence", "receipt", "manifest",
            "source_sha256", "final_", "readback");
    static final String SELF_PREFIX = "m3/tooling/jdk-naming/";
    static final Set<String> SELF = Set.of("m3/release/check_standalone.py", "m3/release/test_check_standalone.py");
    /** Every rule contains one of these literals; a line without them is conformant (the recipe's QUICK). */
    static final Pattern QUICK = Pattern.compile("(?i)synexia|com[./_]{1,2}m3[./_]|m3([-_./])m3\\1");
    private static final int EXCERPT = 160;

    private final NamingRules rules;
    private final Path root;

    public NamingScan(NamingRules rules, Path root) {
        this.rules = rules;
        this.root = root;
    }

    static boolean isSelf(String path) {
        return path.startsWith(SELF_PREFIX) || SELF.contains(path);
    }

    static boolean isLineage(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        String lower = path.toLowerCase(Locale.ROOT);
        String suffix = name.lastIndexOf('.') < 0 ? "" : lower.substring(lower.lastIndexOf('.'));
        String[] parts = name.toLowerCase(Locale.ROOT).split("\\.");
        boolean docSuffix = Arrays.stream(parts).skip(1).anyMatch(part -> DOC_SUFFIXES.contains("." + part));
        return docSuffix || DOC_NAMES.contains(name)
                || path.startsWith("m3/docs/") || path.startsWith("m3/release/audit-")
                || path.contains("/evidence/") || path.contains("/receipts/") || path.contains("/surefire-reports/")
                || name.startsWith("TEST-") && suffix.equals(".xml")
                || suffix.equals(".json") && LINEAGE_HINTS.stream().anyMatch(lower::contains);
    }

    static boolean inScope(String path) {
        if (isSelf(path) || isLineage(path)) {
            return false;
        }
        String name = path.substring(path.lastIndexOf('/') + 1);
        String suffix = name.lastIndexOf('.') < 0 ? "" : name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        return TEXT_SUFFIXES.contains(suffix) || name.equals("pom.xml");
    }

    /** The tracked paths of the working tree ({@code rev == null}) or of a revision. */
    public List<String> tracked(String rev) throws IOException, InterruptedException {
        List<String> command = rev == null ? List.of("git", "-C", root.toString(), "ls-files", "-z")
                : List.of("git", "-C", root.toString(), "ls-tree", "-r", "-z", "--name-only", "--full-tree", rev);
        Process git = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        byte[] out = git.getInputStream().readAllBytes();
        if (git.waitFor() != 0) {
            throw new IOException(String.join(" ", command) + " failed");
        }
        List<String> paths = new ArrayList<>(Arrays.asList(new String(out, StandardCharsets.UTF_8).split("\0")));
        paths.removeIf(String::isEmpty);
        return paths;
    }

    /** Every violation in the tracked files that are checked out, paths first. */
    public List<Violation> scan() throws IOException, InterruptedException {
        List<String> tracked = tracked(null);
        List<Violation> violations = pathViolations(tracked);
        for (String path : tracked) {
            Path file = root.resolve(path);
            if (inScope(path) && Files.isRegularFile(file)) {
                scanFile(path, Files.readAllBytes(file), violations);
            }
        }
        return violations;
    }

    /**
     * Every violation in the files of a revision, read from their blobs with one {@code git cat-file --batch} (no
     * lazy fetch), so a sparse or partial checkout is scanned completely.
     */
    public List<Violation> scan(String rev) throws IOException, InterruptedException {
        List<String> tracked = tracked(rev);
        List<Violation> violations = pathViolations(tracked);
        List<String> scoped = tracked.stream().filter(NamingScan::inScope).toList();
        BlobReader.read(root, rev, scoped, (path, bytes) -> scanFile(path, bytes, violations));
        return violations;
    }

    private List<Violation> pathViolations(List<String> tracked) {
        Map<String, Integer> claims = new HashMap<>();
        for (String path : tracked) {
            if (!isSelf(path)) {
                claims.merge(targetPath(path), 1, Integer::sum);
            }
        }
        Set<String> existing = Set.copyOf(tracked);
        List<Violation> violations = new ArrayList<>();
        for (String path : tracked) {
            if (!pathConformant(path, existing, claims)) {
                violations.add(new Violation(path, 0, "PATH", rules.renamedPath(path)));
            }
        }
        return violations;
    }

    /**
     * Mirrors the recipe's plan_moves: a path is conformant when its target (target_path) is itself, or when it is a
     * lineage file whose target collides with another tracked path and it has kept its name under its renamed
     * directory (KEEP_LINEAGE_NAME), or stays where it is because that name is taken too (KEEP_LINEAGE_PATH).
     */
    boolean pathConformant(String path, Set<String> existing, Map<String, Integer> claims) {
        if (isSelf(path)) {
            return true;
        }
        String target = targetPath(path);
        if (target.equals(path)) {
            return true;
        }
        boolean collides = existing.contains(target) || claims.getOrDefault(target, 0) > 1;
        String kept = keepsName(path);
        return collides && isLineage(path) && (path.equals(kept) || existing.contains(kept));
    }

    /** A legal or attribution file keeps its name and follows its renamed directory; any other path is renamed. */
    String targetPath(String path) {
        return rules.keepsPath(path) ? keepsName(path) : rules.renamedPath(path);
    }

    String keepsName(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : rules.renamedPath(path.substring(0, slash)) + path.substring(slash);
    }

    void scanFile(String path, byte[] bytes, List<Violation> violations) {
        String text = new String(bytes, StandardCharsets.ISO_8859_1); // byte-transparent; the rules are ASCII
        if (!QUICK.matcher(text).find()) {
            return;
        }
        String[] lines = text.split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            if (QUICK.matcher(lines[index]).find()) {
                scanLine(path, index + 1, lines[index], violations);
            }
        }
    }

    private void scanLine(String path, int number, String line, List<Violation> violations) {
        Map<String, Integer> counts = new HashMap<>();
        if (!rules.rewrite(line, counts).equals(line)) {
            String excerpt = line.strip();
            excerpt = excerpt.length() > EXCERPT ? excerpt.substring(0, EXCERPT) : excerpt;
            violations.add(new Violation(path, number, String.join(",", counts.keySet().stream().sorted().toList()),
                    excerpt));
        }
    }
}
