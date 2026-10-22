// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.util.List;
import java.util.Objects;

/**
 * Canonical semantic authority ladder for M3 refactoring.
 *
 * <p>Repository size never widens authority. A recipe promotes only when its transformation
 * actually crosses the next contract/dependency boundary.</p>
 */
public enum NebulaM3RefactorScope {
    FILE,
    VISIBILITY,
    PACKAGE,
    MODULE,
    MULTI_MODULE,
    LIBRARY_API;

    private static final List<NebulaM3RefactorScope> ORDER = List.of(values());

    public static List<NebulaM3RefactorScope> canonicalOrder() {
        return ORDER;
    }

    public boolean canContain(NebulaM3RefactorScope required) {
        return ordinal() >= Objects.requireNonNull(required, "required").ordinal();
    }

    public static NebulaM3RefactorScope max(
            NebulaM3RefactorScope left,
            NebulaM3RefactorScope right) {
        NebulaM3RefactorScope checkedLeft = Objects.requireNonNull(left, "left");
        NebulaM3RefactorScope checkedRight = Objects.requireNonNull(right, "right");
        return checkedLeft.ordinal() >= checkedRight.ordinal()
                ? checkedLeft
                : checkedRight;
    }
}
