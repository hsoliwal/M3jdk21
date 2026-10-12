// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3BulkRegistrationCiBootstrapRecipeTest {
    private static final String TARGET = ".github/workflows/m3-bulk-registration-live-recovery.yml";
    private static final String RESOURCE =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/"
                    + M3BulkRegistrationCiBootstrapRecipe.CRATE + "/";

    @Test
    void installsPinnedDependencyBeforeMavenAndConverges() {
        var wrapper = new M3BulkRegistrationCiBootstrapRecipe();
        assertFalse(wrapper.promotionAuthority());
        assertEquals(1, wrapper.getRecipeList().size());
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe(M3BulkRegistrationCiBootstrapRecipe.CRATE);
        assertEquals(List.of(TARGET), recipe.targetPaths());
        String before = text("before.yml.txt");
        String after = text("post.yml.txt");
        assertTrue(after.contains("m3/synexia-import/pure-int-recipe-custody/install.sh"));
        assertTrue(after.indexOf("Install pinned Synexia recipe custody")
                < after.indexOf("-Dtest=M3BulkLiveRegistrationRecoveryRecipeTest"));
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        SourceFile original = PlainText.builder().sourcePath(Path.of(TARGET)).text(before).build();
        var first = recipe.run(new InMemoryLargeSourceSet(List.of(original)), context, 1);
        assertEquals(1, first.getChangeset().getAllResults().size());
        SourceFile changed = Objects.requireNonNull(first.getChangeset().getAllResults().getFirst().getAfter());
        assertEquals(after, changed.printAll());
        var replay = new M3Jdk21HashPinnedTextSnapshotRecipe(M3BulkRegistrationCiBootstrapRecipe.CRATE)
                .run(new InMemoryLargeSourceSet(List.of(changed)), context, 1);
        assertTrue(replay.getChangeset().getAllResults().isEmpty());
        SourceFile drift = PlainText.builder().sourcePath(Path.of(TARGET)).text(before + "# drift\n").build();
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(List.of(drift)),
                new InMemoryExecutionContext(), 1));
    }

    private static String text(String name) {
        try (var stream =
                M3BulkRegistrationCiBootstrapRecipeTest.class.getResourceAsStream(RESOURCE + name)) {
            return new String(Objects.requireNonNull(stream, name).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
