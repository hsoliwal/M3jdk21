// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * Opt-in javac plugin {@code -Xplugin:M3NullnessFacts}: conservative, parameter-local nullness candidate facts read
 * from javac's attributed trees and reported to stderr; the two-bit nullness lattice and the fail-closed Tree API
 * bridge it is built on. Read-only: no tree, source or bytecode mutation.
 */
module org.m3jdk.compiler.nullness {
  requires transitive java.compiler;
  requires transitive jdk.compiler;
  exports jdk.m3.compiler.nullness;
  provides com.sun.source.util.Plugin with jdk.m3.compiler.nullness.M3JavaNullnessCompilerPlugin;
}
