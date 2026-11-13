<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# M3JDK21: direct primitive offset/mask viewport kernel — scope and acceptance

## Product ownership / no sidecar claim

`hsoliwal/M3jdk21` is the intended product owner. This is an additive **unpublished candidate** for the product's existing `m3/collections` source structure. It is not a new `M3 + JDK` adaptation layer. A new package-private kernel lives with the collection backend and operates directly on the already parent-owned primitive offset and state arrays. The Java and JNI owners can be built together without introducing permanent object nodes, Entry objects or wrapper copies. No JNI native handle stores Java object addresses.

This candidate **does not** change the OpenJDK `java.base` runtime, build a JDK image/jmod, integrate SWT/Nebula through their own public API, or implement all five provider-library contracts. Those claims require concrete source owners, verified current base commits, recipes, and full compatibility gates. Do not publish a claim of installed-runtime behavior based on this standalone proof.

## Operations (package-local)

- `count(states, first, length, requiredBits, forbiddenBits)` counts matching physical state slots.
- `project(logicalOffsets, states, first, length, requiredBits, forbiddenBits, target, targetOffset)` projects offsets in input order, preserving duplicates and caller destination boundaries. `logicalOffsets` may be sorted/permuted by the **existing** M3 signal-order owner; this kernel does not introduce another sort backend.
- `atRank(..., rank)` addresses the Nth matching offset in a bounded window.
- `M3MaskViewportNative.project` has the same array, mask, range and alias contract. JNI is optional for callers; it has no implicit library loading.

`requiredBits` can be zero to select every state not forbidden. Bit 31 is fully supported. Contradictory masks select no state; negative logical offsets are rejected. The caller provides the physical window `[first, first+length)` and must serialize concurrent writes to the underlying arrays. This is bounded in the **physical window**, not the total size of a logical collection. No global scan is required from this atom.

Complexity: Java `count`, `project`, and `atRank` cost O(window); Java `project` uses O(1) normal temporary storage and O(k) temporary int storage only for source/target aliasing. JNI uses O(k) native staging and possible JVM array copying, so it is not asserted to be faster on small windows. A true O(log n) rank/pixel query needs the existing Fenwick/index owner, not this filter kernel.

## Executed checks

- `javac -Xlint:all -Werror` with installed Java 21: PASS.
- 25,000 seeded windows against independent ArrayList reference: PASS.
- Native GCC C17 `-Wall -Wextra -Werror`, JNI `-Xcheck:jni`: PASS.
- Java reference checks: 94,442 PASS.
- Java+native checks: 144,449 PASS.
- Aliased input/output, duplicate offsets, full 32-bit state flags, negative-offset/capacity/overflow refusal: PASS.
- Deterministic additive source preflight: first pass 5 files, second pass 0 files, deliberate hash drift refused: PASS.

## Mandatory unfinished gates before any PR merge

1. Verify the exact current M3JDK21 `master` or selected receiving branch and check that all new package paths are absent; do not overwrite existing owner changes.
2. Materialize the real **canonical Synexia Maven/OpenRewrite** recipe with the same source fingerprints and fixtures, run selected Java21 SDK tests, refusal cases, and a fresh-parser fixed point. Local Java source-admission preflight is not a substitute for canonical OpenRewrite qualification.
3. Wire the package-local Java and JNI owners into the actual existing M3JDK21 build and module native-library mechanism without changing public contract descriptors.
4. Compare behavior of existing collection filtered/sorted wrapper APIs, including comparator aliases, nulls, snapshots, live views, exception propagation, iteration order, and mutability.
5. Cross-verify SWT Tree `TreeViewport` shrink selection (`#203/#213`), logical selectAll, native descendant handles, overflow; Nebula Grid G3/startup scroll/current source, Gallery all eight seeds and callback reentry.
6. Execute focused Maven/JUnit and native acceptance, OS-specific ABI and resource checks, full relevant JDK/java.base build and tests; inspect binary/API differences and benchmark a representative memory-pressure workload.
7. Publish new **draft PRs** only with valid receiving SHA, source-sealed recipe, reviewed donor ledger, actual test receipts and explicit HOLDs. Never amend/rebase/force-update `develop` or `master`.

## Current publication status

**DRAFT / HOLD.** This is a reviewable candidate only; do not merge before canonical Maven/OpenRewrite qualification, exact-head CI and platform acceptance. The original user-level programme remains much broader than this bounded atom.

## Source-preflight and review considerations

The standalone Java admission checks every manifest source hash and every existing target path before creating any file. It rejects duplicates, escaping relative paths and symbolic-link components; the second application has no changes. It does not guarantee a cross-file transactional installation under a simultaneous attacker or an I/O failure and therefore remains a separate *preflight* rather than a Git transaction or qualified OpenRewrite recipe. Git commit publication must operate on a verified tree with exact parents.
