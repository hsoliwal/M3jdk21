<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 Collections: receive the missing stack-safe filtered List history

**Canonical Synexia Maven/OpenRewrite recipe:**
[hsoliwal/com.synexia#12160](https://github.com/hsoliwal/com.synexia/pull/12160)
(commit `8008b8e6b4f0dc720e968a00f323fc29d327cd18`).

The current M3JDK21 master omitted behavioral atoms from already-merged PRs
[#1045](https://github.com/hsoliwal/M3jdk21/pull/1045)
and [#1069](https://github.com/hsoliwal/M3jdk21/pull/1069).
The restored original first-party class is exact Git blob
`e094fdde4a153eab2d1a125785bc55c68187bbd1`
(SHA-256 `b2dc49d3a9ec43b6e6bd34cf5c6dac53057cf90f42cc9ed506d54822a760fc2d`);
the target current-master source before this correction was
`e381d23e93702cea3ccd7d9c3f7ff5e0d56baaa6`
(SHA-256 `7a9d818d38beae4df178317f7913050b47d6442e85f1ef5d5873a0df89262101`).
The original four tests were absent on current master; all six generated
target files are source-sealed, including a new JNI consumer oracle.
The full preimage-to-postimage manifest is in
`recipe-mirror/manifest.tsv` and `SOURCE_RECEIPT.tsv`.

## Existing owners and JDK-compatible public boundary

- No `java.util`/HotSpot source replacement or public API signature change.
- The original `FilterChain` / `FilterTerm` implementation removes deep
  recursive evaluation with a lazily built, ordered flat Predicate reference array.
  Repeated private strict read-only wrappers fuse without exposing writes.
- The original late-bound spliterator observes permitted backing updates
  before first traversal, without claiming SIZED/SORTED/DISTINCT flags.
- The `List` remains a live view over the existing backing references;
  logical positions are mapped to source indices, never per-row Entry objects.
- No new JNI function: the mask bridge proof calls the already-owned
  `M3IndexMaskCompactor` Java/C17 selector with primitive `int[]` physical
  positions and precomputed `byte[]` state masks. Arbitrary predicates,
  comparators, user exceptions and Java heap identities never cross JNI.

## Source and recipe proof

`verify.py` checks exactly six generated postimages and the sole original
preimage against canonical recipe SHA-256, using byte identity rather than
file names or Git merge ancestry. `recipe-mirror/` is an exact *noncanonical*
source mirror for public review (original descriptor, manifest, templates,
test, preimage); it does not become a M3 runtime or recipe dependency.
Fix recipe defects in Synexia and re-emit every receiver postimage.

Focused selected-source Java 21 proof:
```sh
python3 m3/collections/qualification/filtered-list-history-restore-20261017/verify.py
javac --release 21 -proc:none -Xlint:all -Werror -d /tmp/m3-list \
  m3/collections/src/main/java/com/m3/collections/M3FilteredLists.java \
  m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java \
  m3/collections/src/test/java/com/m3/collections/M3FilteredListsDeepCompositionProof.java \
  m3/collections/src/test/java/com/m3/collections/M3FilteredListsReadonlyDepthProof.java \
  m3/collections/src/test/java/com/m3/collections/M3FilteredListsMaskBridgeProof.java
java -Xss256k -cp /tmp/m3-list com.m3.collections.M3FilteredListsDeepCompositionProof
java -Xss256k -cp /tmp/m3-list com.m3.collections.M3FilteredListsReadonlyDepthProof
java -cp /tmp/m3-list com.m3.collections.M3FilteredListsMaskBridgeProof
```
The focused workflow also checks the existing `m3_index_mask_compactor.c`
JNI source with `-Xcheck:jni`, UBSan and `--native` parity, and runs
the original unchanged M3FilteredLists tests. The canonical source recipe
SDK test must run in Synexia:
`mvn -B -ntp -f synexia-openrewrite-recipes/pom.xml
-Dtest=M3FilteredListHistoryRestore20261017RecipeTest test`.

**Actual admission:** source hash and Git blob identities verified; original
historical PRs were merged but newer master had missing code. Newly authored
CI is not a PASS until GitHub reports a completed green result. Whole
collection reactor, all providers, JDK image/jtreg, JNI on other platforms,
GC/CPU and heap are separate pending gates. STRING -> arrays ->
collections -> AST/compiler remains the mandated product promotion order.

Donor category reviews remain in the [Synexia catalogue](https://github.com/hsoliwal/com.synexia/blob/m3/filtered-list-history-restore-recipe-20261017/synexia-openrewrite-recipes/recipes/m3jdk21-filtered-list-history-restore-20261017/CATALOG.tsv).
LeetCode/HackerRank/GeeksforGeeks classifications do not copy solutions.
Original Synexia first-party Apache-2.0 copyright/NOTICE remains unchanged.
