/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/**
 * The M3 annotations owner: the nullness annotations M3JDK carries itself, absorbed from JSpecify
 * 1.0.1 (Apache-2.0; see legal/jspecify.md) with JSpecify semantics. The other annotation donors
 * (Checker Framework, JetBrains, Error Prone, JSR-305 and j2objc) converge onto this module through
 * the ledger in {@code m3/tooling/annotations-absorb}.
 *
 * @moduleGraph
 */
module jdk.m3.annotations {
    // compile time only: the absorbed Javadoc links javax.lang.model.element.Element
    requires static java.compiler;

    exports jdk.m3.annotations;
}
