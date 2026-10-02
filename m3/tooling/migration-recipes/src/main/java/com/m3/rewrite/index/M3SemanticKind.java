// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

/**
 * Stable semantic hierarchy used by the M3 atom database.
 *
 * <p>Atoms are the reusable leaves. Fields and methods compose atoms; Java types compose fields and
 * methods; files compose types and documentation; broader nodes aggregate deterministically
 * through package, module, library, project and repository.
 */
public enum M3SemanticKind {
    ATOM,
    FIELD,
    METHOD,
    FILE,
    INTERFACE,
    IMPLEMENTATION,
    DOCUMENTATION,
    PACKAGE,
    MODULE,
    LIBRARY,
    PROJECT,
    REPOSITORY
}
