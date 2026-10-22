// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

class NebulaM3UpstreamSyncIgnoreRecipeTest {

    private static final String BEFORE =
            "#Ignore all bin folders\n"
            + "bin/\n"
            + "target/\n"
            + "#Ignore all .settings folders\n"
            + "*/*/.settings/\n"
            + "#Ignore vi temporary files\n"
            + "*~\n"
            + "*.orig\n"
            + "*.patch\n"
            + "class/\n"
            + "*.class\n"
            + ".metadata/\n"
            + "releng/org.eclipse.nebula.site/updates/\n"
            + "build.log\n"
            + ".tycho.*";

    private static final String AFTER = BEFORE + "\n.polyglot*";

    @Test
    void exactUpstreamIgnoreLeafReplaysAndReachesFixedPoint() {
        var recipe = new NebulaM3UpstreamSyncIgnoreRecipe();
        assertEquals(recipe.expectedBeforeSha256(), NebulaM3ExactTextSnapshotRecipe.sha256(BEFORE));
        assertEquals(recipe.expectedAfterSha256(), NebulaM3ExactTextSnapshotRecipe.sha256(AFTER));

        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        PlainText input = PlainText.builder().sourcePath(Path.of(".gitignore")).text(BEFORE).build();

        var first = recipe.run(new InMemoryLargeSourceSet(List.of(input)), context);
        assertTrue(errors.isEmpty(), errors.toString());
        var changes = first.getChangeset().getAllResults();
        assertEquals(1, changes.size());
        SourceFile after = changes.getFirst().getAfter();
        assertNotNull(after);
        assertEquals(AFTER, after.printAll());

        var second = recipe.run(new InMemoryLargeSourceSet(List.of(after)), context);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void sourceDriftRefusesAndPromotionAuthorityRemainsClosed() {
        var recipe = new NebulaM3UpstreamSyncIgnoreRecipe();
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        PlainText drift = PlainText.builder()
                .sourcePath(Path.of(".gitignore"))
                .text(BEFORE + "\n# drift")
                .build();

        var run = recipe.run(new InMemoryLargeSourceSet(List.of(drift)), context);
        assertTrue(run.getChangeset().getAllResults().isEmpty());
        assertFalse(errors.isEmpty());
        assertTrue(recipe.sourceMutationAuthority());
        assertFalse(recipe.promotionAuthority());
        assertTrue(recipe.matches(Path.of(".gitignore")));
        assertTrue(recipe.matches(Path.of("../../.gitignore")));
    }
}
