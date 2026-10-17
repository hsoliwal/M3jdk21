// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast.partial;

/**
 * Parser-neutral partial-AST scopes ordered from the narrowest local syntax request to the
 * broadest compilation-unit fallback. Adapters may skip unsupported scopes, but escalation is
 * always monotonic.
 */
public enum MIndexPartialAstScope {
    EXPRESSION(10),
    STATEMENTS(20),
    PROCEDURE(30),
    TYPE(40),
    FOCAL_UNIT(50),
    DECLARATIONS(60),
    FILE(70),
    COMPILATION_UNIT(80);

    private final int breadth;

    MIndexPartialAstScope(int breadth) {
        this.breadth = breadth;
    }

    public int breadth() {
        return breadth;
    }

    public boolean broaderThan(MIndexPartialAstScope other) {
        return other != null && breadth > other.breadth;
    }
}
