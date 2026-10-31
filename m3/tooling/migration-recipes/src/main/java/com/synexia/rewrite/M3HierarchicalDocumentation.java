// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/** Pure deterministic FLLDCIM documentation projection; no source edits and no filesystem IO. */
public final class M3HierarchicalDocumentation {
    public static final String OUTPUT = ".m3/generated-docs/";
    public static final String MARKER = "<!-- M3-FLLDCIM-GENERATED v1 -->\n";

    private M3HierarchicalDocumentation() { }

    /** sha256 describes OpenRewrite's printed UTF-8 text, not necessarily the original Git blob. */
    public record Source(String path, String sha256, long printedBytes, String sourceKind,
                         String packageName, List<String> declarations) {
        public Source {
            path = path.replace('\\', '/');
            if (path.isEmpty() || path.startsWith("/") || path.contains("//")
                    || path.equals("..") || path.startsWith("../") || path.contains("/../")
                    || path.equals(".") || path.startsWith("./") || path.indexOf('\n') >= 0
                    || path.indexOf('\r') >= 0 || path.indexOf('\0') >= 0
                    || path.indexOf(':') >= 0 || path.indexOf('\t') >= 0
                    || !sha256.matches("[0-9a-f]{64}") || printedBytes < 0) {
                throw new IllegalArgumentException("invalid source evidence path/hash/size");
            }
            sourceKind = Objects.requireNonNull(sourceKind);
            packageName = Objects.requireNonNull(packageName);
            declarations = List.copyOf(Objects.requireNonNull(declarations));
        }
    }

    public static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }

    /** Every input SourceFile yields L0 evidence; parent levels list verified child identity, not invented semantics. */
    public static SortedMap<String, String> render(Collection<Source> input, String project, String repository) {
        Objects.requireNonNull(input);
        project = validatedLabel(project);
        repository = validatedLabel(repository);
        SortedMap<String, Source> sources = new TreeMap<>();
        for (Source item : input) {
            if (item.path().startsWith(OUTPUT)) continue; // Never recursively document generated documentation.
            if (sources.putIfAbsent(item.path(), item) != null) {
                throw new IllegalArgumentException("duplicate source owner: " + item.path());
            }
        }
        SortedMap<String, String> output = new TreeMap<>();
        Set<String> moduleRoots = new TreeSet<>();
        for (String path : sources.keySet()) {
            if (path.equals("pom.xml")) moduleRoots.add("");
            else if (path.endsWith("/pom.xml")) moduleRoots.add(path.substring(0, path.length() - "pom.xml".length()));
        }
        Map<String, Set<String>> packages = new TreeMap<>();
        Map<String, Set<String>> components = new TreeMap<>();
        Map<String, Set<String>> modules = new TreeMap<>();
        Set<String> projects = new TreeSet<>();
        for (Source item : sources.values()) {
            String path = item.path();
            String module = owningModule(path, moduleRoots);
            String component = component(path.substring(module.length()));
            String pkg = item.packageName().isBlank() ? "folder/" + parent(path.substring(module.length()))
                    : "java/" + item.packageName().replace('.', '/');
            String fileDoc = OUTPUT + "L0/" + path + ".md";
            String packageDoc = OUTPUT + "L1/" + module + component + "/" + pkg + ".md";
            String componentDoc = OUTPUT + "L2/" + module + component + ".md";
            String moduleDoc = OUTPUT + "L3/" + (module.isEmpty() ? "root/" : module) + "MODULE.md";
            String projectDoc = OUTPUT + "L4/PROJECT.md";
            output.put(fileDoc, fileDocument(item));
            add(packages, packageDoc, fileDoc);
            add(components, componentDoc, packageDoc);
            add(modules, moduleDoc, componentDoc);
            projects.add(moduleDoc);
        }
        // Packages might contribute to different components. Use the child identity only once per parent.
        for (var entry : packages.entrySet()) output.put(entry.getKey(), rollup(1, "Package", entry.getKey(), entry.getValue(), output));
        for (var entry : components.entrySet()) output.put(entry.getKey(), rollup(2, "Component", entry.getKey(), entry.getValue(), output));
        for (var entry : modules.entrySet()) output.put(entry.getKey(), rollup(3, "Module", entry.getKey(), entry.getValue(), output));
        output.put(OUTPUT + "L4/PROJECT.md", rollup(4, "Project: " + project, OUTPUT + "L4/PROJECT.md", projects, output));
        output.put(OUTPUT + "L5/REPOSITORY.md", rollup(5, "Repository: " + repository,
                OUTPUT + "L5/REPOSITORY.md", Set.of(OUTPUT + "L4/PROJECT.md"), output));
        return output;
    }

    private static String validatedLabel(String value) {
        if (value == null || value.isBlank() || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("project/repository name is required");
        }
        return value;
    }

    private static void add(Map<String, Set<String>> groups, String owner, String child) {
        groups.computeIfAbsent(owner, ignored -> new TreeSet<>()).add(child);
    }

    private static String parent(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "root" : path.substring(0, slash);
    }

    private static String owningModule(String path, Set<String> moduleRoots) {
        String best = "";
        for (String module : moduleRoots) {
            if (path.startsWith(module) && module.length() > best.length()) best = module;
        }
        return best;
    }

    private static String component(String path) {
        if (path.startsWith("src/main/java/")) return "production";
        if (path.startsWith("src/test/")) return "tests";
        if (path.startsWith("src/main/cpp/") || path.startsWith("src/main/native/")) return "native";
        if (path.startsWith("docs/") || path.startsWith("doc/")) return "documentation";
        if (path.startsWith("src/main/")) return "main-resources";
        return "other";
    }

    private static String fileDocument(Source item) {
        StringBuilder out = new StringBuilder(MARKER)
                .append("# L0 — File `").append(item.path()).append("`\n\n")
                .append("- Source path: `").append(item.path()).append("`\n")
                .append("- Printed UTF-8 SHA-256: `").append(item.sha256()).append("`\n")
                .append("- Printed UTF-8 bytes: ").append(item.printedBytes()).append("\n")
                .append("- Parser classification: `").append(item.sourceKind()).append("`\n")
                .append("- Declared package: `").append(item.packageName().isBlank() ? "UNRESOLVED" : item.packageName()).append("`\n")
                .append("- Authority: `STRUCTURAL_OBSERVATION_ONLY`\n\n")
                .append("## Recognized declarations\n\n");
        if (item.declarations().isEmpty()) out.append("- UNRESOLVED — no recognized typed declarations in this source input\n");
        else item.declarations().stream().distinct().sorted().forEach(s -> out.append("- `").append(s).append("`\n"));
        return out.append("\n## Proof obligations\n\nPublic behavior, effects, Javadoc accuracy, "
                + "tests and runtime contracts: `UNVERIFIED` until separately proven.\n").toString();
    }

    private static String rollup(int level, String label, String owner, Set<String> children,
                                 Map<String, String> generated) {
        StringBuilder data = new StringBuilder();
        StringBuilder refs = new StringBuilder();
        for (String child : children) {
            String content = generated.get(child);
            if (content == null) throw new IllegalStateException("missing child: " + child);
            String digest = sha256(content);
            data.append(child).append('\t').append(digest).append('\n');
            String link = Path.of(owner).getParent().relativize(Path.of(child)).toString()
                    .replace('\\', '/').replace(" ", "%20");
            refs.append("- [").append(child.replace("[", "\\[").replace("]", "\\]"))
                    .append("](").append(link).append(") — SHA-256 `").append(digest).append("`\n");
        }
        return MARKER + "# L" + level + " — " + label + "\n\n"
                + "- Scope: `" + owner + "`\n"
                + "- Direct children: " + children.size() + "\n"
                + "- Child-evidence root (SHA-256): `" + sha256(data.toString()) + "`\n"
                + "- Claim: `DERIVED_STRUCTURAL_ROLLUP; NOT_SEMANTICALLY_VERIFIED`\n\n"
                + "## Child documentation\n\n" + refs
                + "\nHigher-level contracts and dependency semantics remain `UNRESOLVED` until source-linked proof is attached.\n";
    }
}
