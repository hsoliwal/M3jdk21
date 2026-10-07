// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.packs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.m3.tools.modulepack.M3ModuleArtifact;
import com.m3.tools.modulepack.M3ModuleGraph;
import com.m3.tools.modulepack.M3ModuleInspector;
import com.m3.tools.modulepack.M3ModuleInventory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;

/**
 * Read-only OpenRewrite projection of the shared inventory kernel.
 * Pattern/IOP role: Adapter. Scope: READ_ONLY external artifacts, no source mutation.
 */
public final class M3InventoryModulePackRecipe extends ScanningRecipe<List<M3ModuleArtifact>> {
    @Option(displayName = "Artifact paths", description = "Explicit JAR/JMOD files or directories, including the selected JDK JMODs.", example = "/opt/jdk21/jmods")
    private final List<String> artifactPaths;
    @Option(displayName = "Root modules", description = "Explicit runtime roots; service providers must be selected deliberately.", example = "jdk.jfr")
    private final List<String> roots;
    private final transient DataTable<Row> inventory = new DataTable<>(this, Row.class,
            "com.m3.rewrite.packs.ModuleInventory", "M3 module inventory",
            "Observed module facts; not product acceptance.");

    /** Immutable observed data-table row. */
    public record Row(
            @Column(displayName = "Module", description = "Explicit module identity.") String module,
            @Column(displayName = "SHA-256", description = "Inspected archive byte digest.") String sha256,
            @Column(displayName = "Path", description = "Canonical archive path.") String path,
            @Column(displayName = "Requires", description = "Descriptor dependency declarations.") String requires,
            @Column(displayName = "Native entries", description = "Observed native names; no ABI claim.") String nativeEntries) {}

    /** Configure only explicit read authority. */
    @JsonCreator
    public M3InventoryModulePackRecipe(@JsonProperty("artifactPaths") List<String> artifactPaths,
            @JsonProperty("roots") List<String> roots) {
        this.artifactPaths = List.copyOf(artifactPaths);
        this.roots = List.copyOf(roots);
    }

    public List<String> getArtifactPaths() { return artifactPaths; }
    public List<String> getRoots() { return roots; }

    @Override public String getDisplayName() { return "Inventory M3 module-pack artifacts"; }
    @Override public String getDescription() {
        return "Inspect real Java 21 module archives and validate selected dependency closure without mutating sources.";
    }

    @Override
    public List<M3ModuleArtifact> getInitialValue(ExecutionContext context) {
        ArrayList<M3ModuleArtifact> artifacts = new ArrayList<>();
        M3ModuleInspector inspector = new M3ModuleInspector();
        try {
            for (String path : artifactPaths) {
                for (Path archive : M3ModuleInventory.paths(Path.of(path))) {
                    artifacts.add(inspector.inspect(archive));
                }
            }
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        return new M3ModuleGraph().resolve(artifacts, roots);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(List<M3ModuleArtifact> artifacts) {
        return TreeVisitor.noop();
    }

    @Override
    public Collection<? extends SourceFile> generate(List<M3ModuleArtifact> artifacts,
            ExecutionContext context) {
        for (M3ModuleArtifact artifact : artifacts) {
            inventory.insertRow(context, new Row(artifact.descriptor().name(), artifact.sha256(),
                    artifact.path().toString(),
                    artifact.descriptor().requires().stream().map(Object::toString).sorted().toList().toString(),
                    artifact.nativeEntries().toString()));
        }
        return List.of();
    }
}
