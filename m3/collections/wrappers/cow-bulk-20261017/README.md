<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# M3JDK21 copy-on-write filtered bulk removal receiver

**Target:** `hsoliwal/M3jdk21`, stacked on [filtered batch admission PR #1051](https://github.com/hsoliwal/M3jdk21/pull/1051), branch `m3/filtered-batch-admission-20261017`. **Donor:** [Synexia PR #11831](https://github.com/hsoliwal/com.synexia/pull/11831), canonical M3_COW_BULK_REMOVE_V1 (Apache-2.0). Existing generic filtered/sorted/navigable classes and public APIs remain the sole owners. Do not copy this product receiver back into Synexia or create another hierarchy.

## Before, after and contract

The exact old `M3CollectionViews.FilteredCollection.removeMatching` invokes `backing.iterator().remove()`. `CopyOnWriteArrayList` and `CopyOnWriteArraySet` support `removeIf` but forbid iterator removal, so removing a visible member via `clear`, `removeIf`, `removeAll` or `retainAll` throws `UnsupportedOperationException`.

The already reviewed Synexia atom dispatches those **two owner types only** to the JDK `backing.removeIf(value -> visible(value) && condition.test(value))`; the original iterator-removal loop remains byte-for-byte unchanged for all other collections. The short circuit protects hidden values, predicate evaluation order, source ownership and retained reference identity. There is no per-element persistent wrapper/index, no JDK method override, no java.base/HotSpot modification. The changed COW behavior intentionally repairs a defect; do not claim identical behavior to its former exception.

This increment does not reconcile sibling draft PRs that change the same owner (e.g. the stored-representative and deeper list branches) and does not repair single-object `remove(Object)`. No thread-safety/linearizability guarantee is inferred for dynamic user predicates.

## Recipe-first delivery

This branch **first** commits the source-sealed Maven-bound receipt, immutable original Java preimage, read-only donor catalogue, Java21 red/green oracle, and native C17 checked JNI consumer. Only a subsequent commit installs the exact deterministic recipe emissions. The canonical transformation choice comes from Synexia rather than newly invented code; the target recipe is a narrow, exact-source receiver.

- Source preimage SHA-256: `115f16592a41ce78a63b4892904cb4fe934f5ee45974ffb3305ec39a7c0b9f6e`.
- Generated postimage SHA-256: `5d2a4c9197ac3391d13d5a25c0f77a12261255bbab076e9e454e89df2dcffc66`.
- Java proof template SHA-256: `af51caf6ddc8030a2b52c64c323835981890fa6dbc39e68188b96b8e696bea6a`.
- Native C fixture SHA-256: `82c12d4442b44f2131acb42317d0c2a894e154f143aab7e898c3a672c25ac7f1`.

`recipe.py` refuses changed preimages, missing/multiple anchors, duplicate atom, foreign test occupant, symlinks and generated output mismatches. It independently compiles original and generated sources, proves the original failing COW case, checks public descriptors, runs the generated receiver in tiered and interpreted modes, then verifies a no-change fixed point. The optional C17 test-only JNI consumer traverses actual Java filtered/sorted iterators under `-Xcheck:jni` and UBSan. The JNI path does not move Java predicates/native pointers into production and provides no performance result. The portable Java oracle is always mandatory.

This is **Maven-bound exact-source replay, not an executed OpenRewrite SDK/LST recipe**. The canonical project separately maintains class-backed OpenRewrite recipes and compiler/contract gates. If this fix is widened beyond this exact source pin, qualify a new OpenRewrite source-sealed recipe in Synexia before applying any further product changes.

## Reproduce

On a clean checkout of this PR with **Java 21, Python 3, Maven**, and on Linux for **GCC C17 / JNI**:

```bash
python3 m3/collections/wrappers/cow-bulk-20261017/recipe.py self-test
mvn -B -ntp -f m3/collections/wrappers/cow-bulk-20261017/pom.xml verify
python3 m3/collections/wrappers/cow-bulk-20261017/recipe.py verify --jni
python3 m3/collections/wrappers/cow-bulk-20261017/search_catalogue.py copy-on-write
```

In a deliberately isolated, pinned **preimage** candidate only, `mvn ... -Papply verify` installs two source-sealed files; its generated SHA-256 hashes must then equal the above. Do not apply onto parent, develop, or master. `-Papply` is intentionally not run by default or CI.

## Atom logic, algorithms, JNI and scope

The category ledger separately records canonical Synexia AtomLogic SHA-256/SimHash and portable V2 catalogue/call-graph owners in [#11810](https://github.com/hsoliwal/com.synexia/pull/11810), [#11823](https://github.com/hsoliwal/com.synexia/pull/11823), [#11864](https://github.com/hsoliwal/com.synexia/pull/11864) and [#11944](https://github.com/hsoliwal/com.synexia/pull/11944). Those supply **candidate discovery**, not equivalence proof. This wrapper change reuses the existing admitted donor atom and does not add a parallel fingerprint backend. The selected algorithm and other candidates are searchable by category using `search_catalogue.py`.

The wider project still needs whole-reactor/JDK-from-source builds, jtreg/JCK, independently qualified OpenRewrite SDK replay, cross-platform and concurrency testing, plus performance and allocation analysis. Keep the PR in **DRAFT** until those gates pass. No claim of completing all JDK bugs, all collections, all JNI kernels, or the entire Synexia convergence programme.
