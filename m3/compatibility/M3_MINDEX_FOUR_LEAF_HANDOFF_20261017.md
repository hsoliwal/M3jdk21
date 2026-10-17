<!-- SPDX-License-Identifier: Apache-2.0 -->
# MIndex → M3: four-leaf candidate handoff and collection-wrapper superset

Status: **EVIDENCE-ONLY TARGET RECEIVER**. This packet does not admit
Synexia source into M3JDK21's JDK runtime or add `java.util` / `java.lang`
API members. It does not copy `com.synexia.*` classes or add runtime Maven
dependencies. M3JDK21 is the product/name/ABI authority; Synexia continues to
own reusable, tested Maven/OpenRewrite recipes.

## Exact Synexia proposal

Synexia draft [#11925](https://github.com/hsoliwal/com.synexia/pull/11925)
introduces four additive source-sealed M3 leaves alongside unchanged MIndex
donors. `m3-four-leaf-candidate-20261017.tsv` records each original and new
fully qualified name, exact Git blob and SHA-256, source path, candidate
proposed receiving plane and proof gate. No target class name or JDK module
owner is assigned by a syntactic prefix alone.

Source proposal's proven local slice: Java21 exact eight-class
compilation; 892 Java checks; C17 JNI verifier; 32 JNI checks.
OpenRewrite Maven/JUnit, full Synexia reactor and target OpenJDK product
verification were **not** executed and remain **HOLD**. The source PR is draft;
this receiver must not elevate its status.

## Existing collection work is not reinvented

The sibling `m3-filtered-wrapper-candidates-20261017.tsv` accounts for
Synexia [#11913](https://github.com/hsoliwal/com.synexia/pull/11913),
[#11920](https://github.com/hsoliwal/com.synexia/pull/11920),
[#11911](https://github.com/hsoliwal/com.synexia/pull/11911), and
[#11903](https://github.com/hsoliwal/com.synexia/pull/11903).
Do not infer provider equivalence between JDK, Guava, Eclipse Collections,
Apache Commons and Spring collections. Preserve each wrapper's specific
write-through, read-only, snapshot, comparator, equality, null, range,
concurrency, iterator and spliterator behaviors; improve only after source-
bound oracle qualification. Native predicate callbacks and array/entry
materialization are not assumed superior.

## Reusable recipe ownership and exact admission order

The canonical Synexia OpenRewrite recipe is
`com.synexia.rewrite.M3CopyMIndexDeclarationRecipe` (merged source PR #7827).
The four exact configurations, fixture tests and digest manifest belong to
Synexia #11925. At M3JDK21, this file and the small source-independent
verifier are **target-specific read-only receivers**, not a forked recipe.

1. Verify original donor file identities, license, compiled contract and
   source-bound behavioral atom identity; do not overwrite old public APIs.
2. Execute and fix the canonical Maven/OpenRewrite recipe in Synexia until
   postimages, fixed point and all refusal tests are proved on exact sources.
3. Review the correct JDK-specific receiving owner independently; never infer
   it only from `MIndex*` spelling or a Synexia package.
4. Port only compatible Java/JNI/HotSpot atoms with target licensing and
   `java.base`/module rules; use target-specific recipes for exact OpenJDK
   preimages. Preserve existing JDK public interfaces and observable semantics.
5. Prove JDK module/API/serialization/GC/concurrency/Java-JNI contracts,
   source-built image, jtreg/JCK and performance before any runtime admission.
6. Update this receiver through a new reviewed ledger revision, not an
   unconditional mass move. Retain all historical provenance and source digests.

This receiving contract extends, but does not override,
`m3/compatibility/m3-family-naming-target-disposition.tsv`,
`m3/docs/m3jdk21-porting-invariant.md` and the machine-readable
`m3/docs/name-mapping.json`.

The manifest and verifier themselves are independently authored first-party
Apache-2.0; target OpenJDK source retains its original license.
