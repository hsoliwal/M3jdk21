<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Static range API: negative source compatibility oracle

**Disposition: SOURCE-COMPATIBILITY HOLD; this is not a release pass.**

M3JDK21 now contains the static range methods in its existing final
jdk.m3.collections.CollectionViews class (merged product PR #3137).
That avoids the inherited default/superclass method conflicts documented in
Synexia #17926. It does not guarantee that every original Java source file
recompiles unchanged.

A Java 21 negative fixture has two wildcard static imports: CollectionViews.*
and a preexisting legacy RangeOps.* utility exposing the same forEachRange
signature. The call compiles against the original API. After adding the static
method to CollectionViews, javac reports an ambiguous static import.
The old compiled client continues to link and run unchanged.

The new script verifies the actual product method signature, then builds
proof-only old/new APIs and the unchanged client with strict Java 21 javac.
It checks that baseline compilation and runtime succeed, unchanged classfiles
run against the new API shape, and unchanged-source recompilation fails with
the expected ambiguity diagnostic. This is a narrow method-resolution witness,
not a full-module or full-JDK execution receipt.

**Release decision required:** analyze actual consumers and decide whether
static-import source ambiguity is acceptable for the additive M3-specific
API. Fully qualified calls can be migrated explicitly, but this is not
transparent backward compatibility. Never silently remove published
methods or claim universal source compatibility from binary-linkage results.
