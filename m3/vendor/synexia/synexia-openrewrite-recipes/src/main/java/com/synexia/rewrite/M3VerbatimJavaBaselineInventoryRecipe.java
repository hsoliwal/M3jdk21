// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;

/**
 * Read-only exact Java baseline inventory for source-sealed recipe planning.
 *
 * <p>Each Java compilation unit is identified by its normalized repository-relative path and
 * SHA-256 of exact OpenRewrite printer text encoded as UTF-8, matching the existing
 * {@link M3HashPinnedJavaSnapshotRecipe} preimage mechanism. Rows are deterministically sharded by
 * Maven module into bounded <=256-target candidate recipe crates. No row grants mutation,
 * source-copy, replacement or promotion authority.</p>
 */
public final class M3VerbatimJavaBaselineInventoryRecipe
        extends ScanningRecipe<M3VerbatimJavaBaselineInventoryRecipe.Inventory> {

    @Option(
            displayName = "Repository",
            description = "Stable repository identity for the baseline.",
            example = "openjdk/jdk21u")
    private final String repository;

    @Option(
            displayName = "Exact source revision",
            description = "Exact lowercase 40-hex Git commit for the baseline source tree.",
            example = "0123456789abcdef0123456789abcdef01234567")
    private final String sourceRevision;

    @Option(
            displayName = "Maximum targets per crate",
            description = "Maximum number of exact Java targets in one planned hash-pinned crate.",
            example = "256",
            required = false)
    private final Integer maxTargetsPerCrate;

    private final transient BaselineTable baseline = new BaselineTable(this);
    private final transient CrateTable crates = new CrateTable(this);

    public M3VerbatimJavaBaselineInventoryRecipe() {
        this("hsoliwal/com.synexia", "0".repeat(40), 256);
    }

    @JsonCreator
    public M3VerbatimJavaBaselineInventoryRecipe(
            String repository,
            String sourceRevision,
            Integer maxTargetsPerCrate) {
        this.repository = required(repository, "repository");
        String revision = required(sourceRevision, "sourceRevision").toLowerCase(java.util.Locale.ROOT);
        if (!revision.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("sourceRevision must be lowercase 40-hex Git commit");
        }
        this.sourceRevision = revision;
        int bounded = maxTargetsPerCrate == null ? 256 : maxTargetsPerCrate;
        if (bounded < 1 || bounded > 256) {
            throw new IllegalArgumentException("maxTargetsPerCrate");
        }
        this.maxTargetsPerCrate = bounded;
    }

    @Override
    public String getDisplayName() {
        return "M3 verbatim Java baseline inventory";
    }

    @Override
    public String getDescription() {
        return "Inventories every parsed Java source by exact path and rendered UTF-8 SHA-256, then "
                + "plans deterministic bounded hash-pinned recipe crates by Maven module.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "recipe-first",
                "verbatim",
                "preimage",
                "sha256",
                "java",
                "inventory",
                "candidate-only",
                "read-only");
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
    public Inventory getInitialValue(ExecutionContext context) {
        return new Inventory();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (tree instanceof SourceFile source) {
                    stopAfterPreVisit();
                    if (!(source instanceof J.CompilationUnit)) return tree;
                    String path = normalized(source);
                    M3ScopeInference.JavaLocation location =
                            M3ScopeInference.describeJavaTarget(path);
                    String text = source.printAll();
                    Entry entry =
                            new Entry(
                                    path,
                                    location.module(),
                                    location.packageName(),
                                    sha256(text),
                                    text.getBytes(StandardCharsets.UTF_8).length);
                    inventory.observe(entry);
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            Inventory inventory,
            ExecutionContext context) {
        List<Entry> entries = inventory.entries();
        for (int ordinal = 0; ordinal < entries.size(); ordinal++) {
            baseline.insertRow(context, new BaselineRow(ordinal, repository, sourceRevision, entries.get(ordinal)));
        }

        int crateOrdinal = 0;
        TreeMap<String, List<Entry>> byModule = new TreeMap<>();
        for (Entry entry : entries) {
            byModule.computeIfAbsent(entry.module(), ignored -> new ArrayList<>()).add(entry);
        }
        for (Map.Entry<String, List<Entry>> module : byModule.entrySet()) {
            List<Entry> moduleEntries = module.getValue().stream()
                    .sorted(Comparator.comparing(Entry::path))
                    .toList();
            for (int from = 0; from < moduleEntries.size(); from += maxTargetsPerCrate) {
                int to = Math.min(moduleEntries.size(), from + maxTargetsPerCrate);
                List<Entry> shard = moduleEntries.subList(from, to);
                List<String> paths = shard.stream().map(Entry::path).toList();
                crates.insertRow(
                        context,
                        new CrateRow(
                                crateOrdinal++,
                                module.getKey(),
                                from / maxTargetsPerCrate,
                                shard.size(),
                                paths.getFirst(),
                                paths.getLast(),
                                M3ScopeInference.inferJavaTargets(paths).name(),
                                crateRoot(repository, sourceRevision, shard),
                                false,
                                false,
                                false));
            }
        }
        return List.of();
    }

    public String getRepository() {
        return repository;
    }

    public String getSourceRevision() {
        return sourceRevision;
    }

    public int getMaxTargetsPerCrate() {
        return maxTargetsPerCrate;
    }

    public boolean replacementAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    private static String normalized(SourceFile source) {
        String value = source.getSourcePath().normalize().toString().replace('\\', '/');
        if (value.startsWith("/") || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("repository-relative source path required");
        }
        return value;
    }

    private static String crateRoot(
            String repository,
            String sourceRevision,
            List<Entry> entries) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            frame(digest, "M3_VERBATIM_JAVA_BASELINE_CRATE_V1");
            frame(digest, repository);
            frame(digest, sourceRevision);
            for (Entry entry : entries) {
                frame(digest, entry.path());
                frame(digest, entry.sha256());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
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

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private static String required(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private record Entry(
            String path,
            String module,
            String packageName,
            String sha256,
            int utf8Bytes) {}

    public static final class Inventory {
        private final Map<String, Entry> entries = new LinkedHashMap<>();

        synchronized void observe(Entry entry) {
            Entry prior = entries.putIfAbsent(entry.path(), entry);
            if (prior != null) {
                if (!prior.equals(entry)) {
                    throw new IllegalStateException("conflicting duplicate baseline path: " + entry.path());
                }
                throw new IllegalStateException("duplicate baseline path: " + entry.path());
            }
        }

        synchronized List<Entry> entries() {
            return entries.values().stream()
                    .sorted(Comparator.comparing(Entry::path))
                    .toList();
        }
    }

    public static final class BaselineTable extends DataTable<BaselineRow> {
        BaselineTable(org.openrewrite.Recipe recipe) {
            super(
                    recipe,
                    "M3 verbatim Java baseline",
                    "Exact rendered-Java path/SHA rows used to author hash-pinned recipe crates.");
        }
    }

    public static final class BaselineRow {
        @Column(displayName = "Ordinal", description = "Stable path-sorted ordinal.")
        private final int ordinal;
        @Column(displayName = "Repository", description = "Repository identity.")
        private final String repository;
        @Column(displayName = "Source revision", description = "Exact 40-hex Git baseline revision.")
        private final String sourceRevision;
        @Column(displayName = "Path", description = "Normalized repository-relative Java path.")
        private final String path;
        @Column(displayName = "Module", description = "Maven module inferred from the source path.")
        private final String module;
        @Column(displayName = "Package", description = "Java package inferred from the source path.")
        private final String packageName;
        @Column(displayName = "SHA-256", description = "Exact rendered UTF-8 SHA-256 preimage.")
        private final String sha256;
        @Column(displayName = "UTF-8 bytes", description = "Rendered UTF-8 byte count.")
        private final int utf8Bytes;

        BaselineRow(int ordinal, String repository, String sourceRevision, Entry entry) {
            this.ordinal = ordinal;
            this.repository = repository;
            this.sourceRevision = sourceRevision;
            this.path = entry.path();
            this.module = entry.module();
            this.packageName = entry.packageName();
            this.sha256 = entry.sha256();
            this.utf8Bytes = entry.utf8Bytes();
        }

        public int getOrdinal() { return ordinal; }
        public String getRepository() { return repository; }
        public String getSourceRevision() { return sourceRevision; }
        public String getPath() { return path; }
        public String getModule() { return module; }
        public String getPackageName() { return packageName; }
        public String getSha256() { return sha256; }
        public int getUtf8Bytes() { return utf8Bytes; }
    }

    public static final class CrateTable extends DataTable<CrateRow> {
        CrateTable(org.openrewrite.Recipe recipe) {
            super(
                    recipe,
                    "M3 verbatim recipe crate plan",
                    "Deterministic <=256-target module-local crate shards. Planning evidence only.");
        }
    }

    public static final class CrateRow {
        @Column(displayName = "Ordinal", description = "Stable global crate ordinal.")
        private final int ordinal;
        @Column(displayName = "Module", description = "Owning Maven module.")
        private final String module;
        @Column(displayName = "Module shard", description = "Zero-based shard within the module.")
        private final int moduleShard;
        @Column(displayName = "Target count", description = "Number of exact Java targets.")
        private final int targetCount;
        @Column(displayName = "First path", description = "First path in sorted shard.")
        private final String firstPath;
        @Column(displayName = "Last path", description = "Last path in sorted shard.")
        private final String lastPath;
        @Column(displayName = "Required scope", description = "Narrowest physical edit scope.")
        private final String requiredScope;
        @Column(displayName = "Crate root", description = "SHA-256 of repository/revision/path/preimage pairs.")
        private final String crateRoot;
        @Column(displayName = "Replacement authority", description = "Always false.")
        private final boolean replacementAuthority;
        @Column(displayName = "Promotion authority", description = "Always false.")
        private final boolean promotionAuthority;
        @Column(displayName = "Donor source copy authority", description = "Always false.")
        private final boolean donorSourceCopyAuthority;

        CrateRow(
                int ordinal,
                String module,
                int moduleShard,
                int targetCount,
                String firstPath,
                String lastPath,
                String requiredScope,
                String crateRoot,
                boolean replacementAuthority,
                boolean promotionAuthority,
                boolean donorSourceCopyAuthority) {
            this.ordinal = ordinal;
            this.module = module;
            this.moduleShard = moduleShard;
            this.targetCount = targetCount;
            this.firstPath = firstPath;
            this.lastPath = lastPath;
            this.requiredScope = requiredScope;
            this.crateRoot = crateRoot;
            this.replacementAuthority = replacementAuthority;
            this.promotionAuthority = promotionAuthority;
            this.donorSourceCopyAuthority = donorSourceCopyAuthority;
        }

        public int getOrdinal() { return ordinal; }
        public String getModule() { return module; }
        public int getModuleShard() { return moduleShard; }
        public int getTargetCount() { return targetCount; }
        public String getFirstPath() { return firstPath; }
        public String getLastPath() { return lastPath; }
        public String getRequiredScope() { return requiredScope; }
        public String getCrateRoot() { return crateRoot; }
        public boolean isReplacementAuthority() { return replacementAuthority; }
        public boolean isPromotionAuthority() { return promotionAuthority; }
        public boolean isDonorSourceCopyAuthority() { return donorSourceCopyAuthority; }
    }
}
