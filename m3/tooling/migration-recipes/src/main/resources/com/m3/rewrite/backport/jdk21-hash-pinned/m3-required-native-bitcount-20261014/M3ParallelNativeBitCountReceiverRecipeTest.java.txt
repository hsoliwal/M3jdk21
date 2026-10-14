// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.InMemoryLargeSourceSet;

final class M3ParallelNativeBitCountReceiverRecipeTest {
    private static final String CRATE = "m3-required-native-bitcount-20261014";

    @Test
    void mirroredSourcesAreExactAndIdempotent() {
        var javaRecipe = new M3Jdk21HashPinnedSnapshotRecipe(CRATE);
        var textRecipe = new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
        assertEquals(List.of(
                "m3/ports/parallel-bulk/src/main/java/com/m3/parallel/bulk/M3ParallelNativeBitCountProvider.java",
                "m3/ports/parallel-bulk/src/test/java/com/m3/parallel/bulk/M3ParallelNativeBitCountProof.java",
                "m3/ports/parallel-bulk/src/test/java/com/m3/parallel/bulk/M3ParallelNativeBitCountProviderTest.java",
                "m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport/M3ParallelNativeBitCountReceiverRecipeTest.java"),
                javaRecipe.targetPaths());
        assertEquals(List.of(
                ".github/workflows/m3-parallel-native-bitcount.yml",
                "m3/ports/parallel-bulk/native/m3_native_bitcount_jni.c",
                "m3/ports/parallel-bulk/verification/NATIVE_BITCOUNT.md",
                "m3/ports/parallel-bulk/verification/verify-native-bitcount.sh",
                "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-parallel-native-bitcount.yml",
                "m3/tooling/migration-recipes/tasks/m3-parallel-native-bitcount-20261014/source-lock.json"),
                textRecipe.targetPaths());
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        var first = javaRecipe.run(new InMemoryLargeSourceSet(List.of()), context, 1);
        assertEquals(4, first.getChangeset().getAllResults().size());
        var postimages = first.getChangeset().getAllResults().stream()
                .map(change -> change.getAfter()).toList();
        var second = new M3Jdk21HashPinnedSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(postimages), context, 1);
        assertEquals(0, second.getChangeset().getAllResults().size());
    }
}
