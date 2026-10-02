// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import java.util.List;
import java.util.Objects;
import org.openrewrite.java.tree.J;

/** Java declaration visibility normalized for M3 scope promotion. */
public enum M3VisibilityLevel {
    PRIVATE,
    PACKAGE,
    PROTECTED,
    PUBLIC;

    public static M3VisibilityLevel of(List<J.Modifier> modifiers) {
        Objects.requireNonNull(modifiers, "modifiers");
        for (J.Modifier modifier : modifiers) {
            if (modifier.getType() == J.Modifier.Type.Public) return PUBLIC;
            if (modifier.getType() == J.Modifier.Type.Protected) return PROTECTED;
            if (modifier.getType() == J.Modifier.Type.Private) return PRIVATE;
        }
        return PACKAGE;
    }

    public boolean escapesFile() {
        return this != PRIVATE;
    }

    public boolean librarySurface() {
        return this == PUBLIC || this == PROTECTED;
    }
}
