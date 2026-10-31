// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast.partial;

/** Evidence requested from a partial AST. */
public enum MIndexPartialAstMode {
    /** Syntax only; dependency/binding environment is deliberately excluded from cache identity. */
    SYNTAX,
    /** Syntax plus compiler/parser attribution/bindings. */
    ATTRIBUTED,
    /** Projection intended to authorize a guarded source rewrite. */
    REWRITE
}
