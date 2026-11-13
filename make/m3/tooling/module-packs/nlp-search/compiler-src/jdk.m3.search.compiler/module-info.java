// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
/** Explicit consumer of the M3JDK compiler's existing scoped AST service. */
module jdk.m3.search.compiler {
    requires transitive jdk.m3.search;
    requires transitive jdk.compiler;
    exports jdk.m3.search.compiler;
}
