<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Canonical Synexia → M3JDK21 optional Eclipse core JMOD

## Source authority and scope

This canonical eight-target, source-absent Java21 OpenRewrite snapshot recipe
owns the **generative templates** for the target
`m3/eclipse/optional-jmod` facade and `m3/file` narrow read boundary.
M3JDK21 remains the public product and its own accepted Java/JNI implementation owner;
Synexia remains donor and recipe convergence authority.

- **M3String**: pass through the existing `M3StringPiece` descriptor from
  `com.m3.text`; stock-JDK copies are explicit via `LocalM3Arena`.
  This stock compatibility piece does **not** claim to be the complete
  OS-backed canonical `java.lang.M3String` representation. That VM contract
  must be proven separately inside the configured M3JDK21 product image.
- **M3Index***: forward to the existing `M3LongIndexProjection`, shared
  primitive index/address/JNI owners, and `M3Arrays`; no alternate index engine.
- **M3AST**: forward to existing `M3ASTHierarchy` L0–L5 source-sealed atoms;
  no JDT AST parser, bytecode semantics or alternative compiler implementation.
- **M3File**: adapt the read-only entry semantics of existing
  `synexia-mindex/resource/.../MIndexResourceAccess.java`, blob
  `1b4eb18b102f52be2d1e13589195b47e37f3b090`, Apache-2.0.
  `M3File` retains the original NIO `Path`, follows no symlinks for reads,
  checks bounded lengths, closes channels and refreshes metadata. It does NOT
  port the full `MIndexResourceSnapshot`, `MIndexResourceWorkspace` or Eclipse
  markers/deltas and does not own an additional resource tree.
- **JPMS**: one optional `com.m3.eclipse.core` JMOD and five prerequisite
  named modules compiled from the **actual existing M3 source paths**, not
  vendored/duplicated runtime source. Descriptor-only overlays exist for the
  currently nonmodular M3AST and M3Collections artifacts. Existing named
  M3Text and M3Arrays modules keep their original descriptors.

The target's standalone `build.py` runs JDK21 javac/jmod/jlink on real owner
sources, checks reproducible JMOD postimages with fixed timestamps, verifies
ServiceLoader/NIO, UTF-16, AST and stable-index contracts, and proves a
`jlink` image resolves its complete module graph. No runtime Synexia dependency,
no added JNI symbol, no Eclipse public API rename.

## Canonical admission gates

1. New Java source exact ABSENT preimages, SHA-256 postimages, OpenRewrite
   Java-LST generation/fixed point and occupied-path refusal.
2. Java21 `-Xlint:all,-module -Werror` on **actual M3 owners**; synthetic
   service/API fixture compilation is diagnostic only.
3. Real JDK21 `jmod describe`, deterministic second build, `jlink`, and
   ServiceLoader+filesystem integration tests on the exact PR head.
4. Keep SWT/Workbench/Equinox/EMF/JDT/JNI compatibility and whole M3 runtime
   performance **pending** until dedicated target-side proof; JPMS module
   packaging alone never proves full Eclipse replacement.
5. Compare donor licensing and source ownership at every promotion pass;
   no imported LeetCode/HackerRank/GeeksforGeeks solution source.

## Canonical transform name

`com.synexia.rewrite.M3EclipseCoreOptionalJmod`, using the existing
`com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe`; the builder is an
**output packaging/proof tool, not a new source transformation engine**.

## Additional strict-warning receiver fix — separate source-sealed crate

The optional-JMOD actual-owner CI exposed a Java 21 `missing-explicit-ctor`
warning in the existing exported `com.m3.text.SharedLexiconPrecomputeCatalog.Builder`.
With the module's intentional `-Xlint:all,-module -Werror` gate, this is a build
failure before packaging. The contract-preserving fix makes the existing public
no-argument constructor explicit; it does not add another M3 owner or API behavior.

Canonical Synexia recipe PR: https://github.com/hsoliwal/com.synexia/pull/12337
Pinned target-receiver record: `qualification/SYNEXIA_SHARED_BUILDER_CTOR_RECIPE_PIN.tsv`.
It uses the existing `M3HashPinnedJavaSnapshotRecipe` engine with exact source and
postimage hashes. The exact preimage, output template, JUnit replay/fixed-point test,
named-recipe discovery test and drift refusal are mirrored in this receiver's existing
recipe proof host. Both canonical and receiver CI are **pending** at this point.

The generated product source must be applied only after the recipe execution and
preimage/postimage evidence are verified. This correction only addresses the strict
Java compiler gate; it does not qualify full Eclipse Platform/PDE integration.
