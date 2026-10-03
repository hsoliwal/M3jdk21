// SPDX-License-Identifier: Apache-2.0
package com.m3.pack.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.m3.pack.M3ModuleArtifact;
import com.m3.pack.M3PackTool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

/** Read-only inventory recipe: real artifacts, exact source fence, no implicit downloads. */
public final class M3InventoryModulePackRecipe extends Recipe {
    public static final String OBSERVATIONS = "com.m3.pack.observations.v1";

    @Option(displayName = "Lock file", description = "Explicit local artifact lock path.", example = "/work/LOCK.tsv")
    private final String lockFile;
    @Option(displayName = "Source path", description = "Exact OpenRewrite source path to observe.", example = "packs/LOCK.tsv")
    private final String sourcePath;
    @JsonIgnore
    private final transient M3ModuleInventoryTable table = new M3ModuleInventoryTable(this);

    /** Declares one readable lock boundary. It confers no source-edit or promotion authority. */
    @JsonCreator
    public M3InventoryModulePackRecipe(@JsonProperty("lockFile") String lockFile,
            @JsonProperty("sourcePath") String sourcePath) {
        this.lockFile = Objects.requireNonNull(lockFile, "lockFile");
        this.sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
    }

    public String getLockFile() { return lockFile; }
    public String getSourcePath() { return sourcePath; }
    @Override public String getDisplayName() { return "Inventory a pinned M3 module pack"; }
    @Override public String getDescription() { return "Inspects actual Java 21 JAR/JMOD descriptors without changing source or admitting a distribution."; }
    @Override public Set<String> getTags() { return Set.of("m3", "read-only", "inventory", "module-pack", "iop-adapter"); }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<ExecutionContext>() {
            @Override public PlainText visitText(PlainText text, ExecutionContext context) {
                if (!text.getSourcePath().toString().replace('\\', '/').equals(sourcePath)) {
                    return text;
                }
                try {
                    Path lock = Path.of(lockFile);
                    if (!Files.readString(lock).equals(text.getText())) {
                        throw new IllegalStateException("LOCK_SOURCE_DRIFT: " + sourcePath);
                    }
                    List<M3ModuleArtifact> artifacts = new M3PackTool().verify(lock);
                    context.putMessage(OBSERVATIONS, artifacts);
                    for (M3ModuleArtifact artifact : artifacts) {
                        table.insertRow(context, new M3ModuleInventoryTable.Row(
                                artifact.descriptor().name(), artifact.descriptor().rawVersion().orElse("UNVERSIONED"),
                                artifact.sha256(), artifact.maximumClassVersion(), String.join(",", artifact.nativeEntries())));
                    }
                    return text;
                } catch (IOException failure) {
                    throw new IllegalStateException("MODULE_INVENTORY_IO: " + sourcePath, failure);
                }
            }
        };
    }
}
