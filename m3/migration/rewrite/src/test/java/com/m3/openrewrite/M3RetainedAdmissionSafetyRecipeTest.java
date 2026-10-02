/* Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0 */
package com.m3.openrewrite;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;
import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.SourceSpecs.text;

final class M3RetainedAdmissionSafetyRecipeTest implements RewriteTest {
    @Override public void defaults(RecipeSpec spec) { spec.recipe(new M3RetainedAdmissionSafetyRecipe()); }
    private static SourceSpecs[] sources(boolean after) {
        List<SourceSpecs> files = new ArrayList<>();
        for (var atom : M3RetainedAdmissionSafetyRecipe.ATOMS) {
            String post = M3RetainedAdmissionSafetyRecipe.resource(atom, false);
            String pre = after ? post : atom.before() == null ? null : M3RetainedAdmissionSafetyRecipe.resource(atom, true);
            files.add(atom.path().endsWith(".java")
                    ? (after ? java(post, spec -> spec.path(atom.path()))
                             : java(pre, post, spec -> spec.path(atom.path())))
                    : (after ? text(post, spec -> spec.path(atom.path()))
                             : text(pre, post, spec -> spec.path(atom.path()))));
        }
        files.add(java("class Unrelated { int value() { return 7; } }", spec -> spec.path("other/Unrelated.java")));
        return files.toArray(SourceSpecs[]::new);
    }
    @Test void exactReplayPreservesUnrelatedOutput() { rewriteRun(spec -> spec.cycles(1).expectedCyclesThatMakeChanges(1), sources(false)); }
    @Test void idempotent() { rewriteRun(sources(true)); }
    @Test void missingOwnersRefused() { assertThrows(IllegalStateException.class, () -> M3RetainedAdmissionSafetyRecipe.needsApply(new M3RetainedAdmissionSafetyRecipe.Scan())); }
    @Test void mixedStatesRefused() {
        var scan = new M3RetainedAdmissionSafetyRecipe.Scan();
        java.util.Arrays.fill(scan.states, 2); scan.states[0] = 1;
        assertThrows(IllegalStateException.class, () -> M3RetainedAdmissionSafetyRecipe.needsApply(scan));
    }
    @Test void driftRefused() {
        var scan = new M3RetainedAdmissionSafetyRecipe.Scan();
        java.util.Arrays.fill(scan.states, 2); scan.states[0] = 3;
        assertThrows(IllegalStateException.class, () -> M3RetainedAdmissionSafetyRecipe.needsApply(scan));
    }
    @Test void completePostimageAndResourceHashes() {
        var recipe = new M3RetainedAdmissionSafetyRecipe();
        var scan = recipe.getInitialValue(new InMemoryExecutionContext());
        java.util.Arrays.fill(scan.states, 2);
        assertFalse(M3RetainedAdmissionSafetyRecipe.needsApply(scan));
    }
}
