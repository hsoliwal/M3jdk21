// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NebulaM3CompileBootstrapRecipeTest {
    @TempDir Path root;

    @Test void exactReplayAndSecondRunFixedPoint() throws Exception {
        populate("before");
        AtomicLong progress = new AtomicLong();
        assertEquals(3, NebulaM3CompileBootstrapApply.apply(root, () -> false, progress::addAndGet));
        assertEquals(3, progress.get());
        for (var snapshot : NebulaM3CompileBootstrapRecipe.snapshots()) {
            assertEquals(resource(snapshot, "after"), Files.readString(root.resolve(snapshot.repositoryPath())));
        }
        assertEquals(0, NebulaM3CompileBootstrapApply.apply(root, null, null));
    }

    @Test void mixedAlreadyAppliedInputIsIdempotent() throws Exception {
        populate("before");
        var first = NebulaM3CompileBootstrapRecipe.snapshots().getFirst();
        Files.writeString(root.resolve(first.repositoryPath()), resource(first, "after"));
        assertEquals(2, NebulaM3CompileBootstrapApply.apply(root, null, null));
        assertEquals(0, NebulaM3CompileBootstrapApply.apply(root, null, null));
    }

    @Test void driftInSecondSourceRefusesEntireBatchBeforeWrites() throws Exception {
        populate("before");
        var snapshots = NebulaM3CompileBootstrapRecipe.snapshots();
        Path drift = root.resolve(snapshots.getLast().repositoryPath());
        Files.writeString(drift, Files.readString(drift) + "// unrelated change\n");
        assertThrows(IllegalStateException.class, () -> NebulaM3CompileBootstrapApply.apply(root, null, null));
        assertEquals(resource(snapshots.getFirst(), "before"), Files.readString(root.resolve(snapshots.getFirst().repositoryPath())));
    }

    @Test void missingSourceCannotCountAsSuccessfulNoOp() throws Exception {
        populate("before");
        var snapshots = NebulaM3CompileBootstrapRecipe.snapshots();
        Files.delete(root.resolve(snapshots.getLast().repositoryPath()));
        assertThrows(IllegalStateException.class, () -> NebulaM3CompileBootstrapApply.apply(root, null, null));
        assertEquals(resource(snapshots.getFirst(), "before"), Files.readString(root.resolve(snapshots.getFirst().repositoryPath())));
    }

    @Test void cancellationLeavesInputsUnchanged() throws Exception {
        populate("before");
        assertThrows(CancellationException.class, () -> NebulaM3CompileBootstrapApply.apply(root, () -> true, null));
        for (var snapshot : NebulaM3CompileBootstrapRecipe.snapshots()) {
            assertEquals(resource(snapshot, "before"), Files.readString(root.resolve(snapshot.repositoryPath())));
        }
    }

    @Test void contractAndExistingReviewOrderArePreserved() throws Exception {
        var snapshots = NebulaM3CompileBootstrapRecipe.snapshots();
        String table = resource(snapshots.getFirst(), "after");
        String composition = resource(snapshots.get(1), "after");
        assertTrue(table.contains("this.passOrder = pass.passOrder();"));
        assertTrue(table.contains("this.authority = \"READ_ONLY_EVIDENCE\";"));
        int inventory = composition.indexOf("new NebulaM3InventoryRecipe()");
        int review = composition.indexOf("new NebulaM3FastSearchReviewRecipe()");
        int ui = composition.indexOf("new NebulaM3UiBehaviorReviewRecipe()");
        int jni = composition.indexOf("new NebulaM3JavaBeforeJniReviewRecipe()");
        assertTrue(inventory >= 0 && inventory < review && review < ui && ui < jni);
        assertEquals(1, new NebulaM3CompileBootstrapRecipe().maxCycles());
    }

    private void populate(String phase) throws Exception {
        for (var snapshot : NebulaM3CompileBootstrapRecipe.snapshots()) {
            Path source = root.resolve(snapshot.repositoryPath());
            Files.createDirectories(source.getParent());
            Files.writeString(source, resource(snapshot, phase), StandardCharsets.UTF_8);
        }
    }

    private static String resource(NebulaM3ExactJavaSnapshotRecipe snapshot, String phase) throws IOException {
        String name = NebulaM3CompileBootstrapRecipe.RESOURCE
                + Path.of(snapshot.repositoryPath()).getFileName() + "." + phase + ".txt";
        try (var input = NebulaM3CompileBootstrapRecipeTest.class.getResourceAsStream(name)) {
            if (input == null) throw new IOException("missing fixture: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
