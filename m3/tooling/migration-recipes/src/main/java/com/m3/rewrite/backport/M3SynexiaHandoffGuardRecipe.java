// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

/** Fail-closed provenance gate for a source-generated Synexia -> M3JDK21 handoff crate. */
public final class M3SynexiaHandoffGuardRecipe extends Recipe {
    @Option(
            displayName = "Crate name",
            description = "Exact source-generated synexia-* handoff crate.",
            example = "synexia-indexstring-shared-v1")
    private final String crateName;

    @JsonCreator
    public M3SynexiaHandoffGuardRecipe(@JsonProperty("crateName") String crateName) {
        this.crateName = SynexiaHandoffPacket.checkedCrate(crateName);
    }

    public String getCrateName() {
        return crateName;
    }

    String packetRoot() {
        return SynexiaHandoffPacket.verify(crateName).packetRoot();
    }

    @Override
    public String getDisplayName() {
        return "Verify Synexia M3JDK21 source handoff";
    }

    @Override
    public String getDescription() {
        return "Validates the source revision, packet root, provenance rows, payload hashes and "
                + "typed receiver manifests before the existing hash-pinned M3JDK21 recipes run.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("m3", "synexia", "jdk21", "bridge", "hash-pinned", "fail-closed");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        packetRoot();
        return new TreeVisitor<Tree, ExecutionContext>() {};
    }
}
