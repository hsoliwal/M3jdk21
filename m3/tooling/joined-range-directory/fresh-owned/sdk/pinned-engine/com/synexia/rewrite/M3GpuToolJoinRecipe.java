// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Source-sealed installer: validate manifest and scanned batch before producing candidates;
 * recheck each target at editing. OpenRewrite errors must hold the entire result from promotion.
 * This visitor does not make multi-file editing or other recipes' changes transactional.
 */
public class M3GpuToolJoinRecipe extends ScanningRecipe<M3GpuToolJoinRecipe.Inventory> {
    /** Optional Runnable progress/cancellation checkpoint supplied through ExecutionContext. */
    public static final String CHECKPOINT_MESSAGE = "com.synexia.m3.sourceSeal.checkpoint";

    /** Extension point for another checked-in, hash-pinned template set. */
    protected String resourceRoot() {
        return "/com/synexia/rewrite/gpu-tool-join/";
    }

    // Retain the existing package-level value declaration for source/binary compatibility.
    record Target(String path, String before, String after, String text) {}

    public static final class Inventory {
        private final M3GpuToolJoinManifest manifest;
        private final M3GpuToolJoinManifest.Scan scan;

        public Inventory() {
            this("/com/synexia/rewrite/gpu-tool-join/", M3GpuToolJoinManifest.NOOP);
        }

        private Inventory(String resourceRoot, Runnable checkpoint) {
            manifest = M3GpuToolJoinManifest.load(resourceRoot,
                    M3GpuToolJoinRecipe.class::getResourceAsStream, checkpoint);
            scan = manifest.scan();
        }
    }

    @Override
    public String getDisplayName() {
        return "Install bounded M3 GPU tool join";
    }

    @Override
    public String getDescription() {
        return "Installs an exact join composition and catalogue entry from hash-pinned templates, "
                + "rejecting the entire batch on drift or duplicate ownership.";
    }

    @Override
    public Inventory getInitialValue(ExecutionContext ctx) {
        Objects.requireNonNull(ctx, "ctx");
        Runnable checkpoint = ctx.getMessage(CHECKPOINT_MESSAGE);
        return new Inventory(resourceRoot(), checkpoint == null ? M3GpuToolJoinManifest.NOOP : checkpoint);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile source) {
                    stopAfterPreVisit();
                    inventory.manifest.checkpoint();
                    String path = M3GpuToolJoinManifest.normalized(source.getSourcePath());
                    if (inventory.manifest.target(path) != null) {
                        inventory.scan.observe(path, M3GpuToolJoinManifest.sha256(source.printAll()));
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Inventory inventory, ExecutionContext ctx) {
        inventory.scan.requireAdmissible();
        var generated = new ArrayList<SourceFile>();
        for (var target : inventory.manifest.targets()) {
            inventory.manifest.checkpoint();
            if (!target.removal() && inventory.scan.missing(target.path())) generated.add(source(target));
        }
        return generated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
        inventory.scan.requireAdmissible();
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile file) {
                    stopAfterPreVisit();
                    inventory.manifest.checkpoint();
                    String path = M3GpuToolJoinManifest.normalized(file.getSourcePath());
                    if (inventory.manifest.target(path) == null) return tree;
                    String hash = M3GpuToolJoinManifest.sha256(file.printAll());
                    var target = inventory.scan.editing(path, hash);
                    if (!hash.equals(target.after())) {
                        if (target.removal()) return null;
                        return source(target)
                                .withId(file.getId())
                                .withSourcePath(file.getSourcePath())
                                .withMarkers(file.getMarkers())
                                .withFileAttributes(file.getFileAttributes());
                    }
                }
                return tree;
            }
        };
    }

    private static PlainText source(M3GpuToolJoinManifest.Target target) {
        return PlainText.builder().sourcePath(Path.of(target.path())).text(target.text()).build();
    }
}
