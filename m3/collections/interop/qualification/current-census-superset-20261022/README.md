<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 five-library interop: 76-source current successor

This is a **product-side source-identity and behavior-gate receiver**, not a second
transformation workspace or a replacement collection framework.

The original 36-source/5-recipe predecessor under
`qualification/current-census-20261017` remains untouched. This successor
executes that original verifier on the *historical* M3JDK21 commit
`a720780b1a5ab41ea27182b9b7ad873efdf533b4`, preserving its complete
36-source SHA-256/Git blob and historical license checks.

Current product source is a distinct generation: **29 historical source
blobs remain identical, seven have exact source-pinned successor revisions,
and 40 additional first-party Java files exist** (13 production and 27 tests).
The seven revisions are checked against the historical preimage AND their
current postimage, newest source-history commit and Apache-2.0 header.
A revision source receipt is not proof that its API or behavior is equivalent.

Every additional row seals its path, source SHA-256/Git blob, family,
PRODUCTION/TEST physical role and observed Apache-2.0 header. The
`receipts/changed-07.tsv` ledger independently records the prior and current
identity of every historical revision, including its history commit. Both
historical and current verifier/manifest bytes remain immutably pinned.
The complete tracked set must equal **all 76 exact source/test paths**, with
no unknown files. Symlink aliases, drifted old/new images, duplicate paths,
forged hashes and new unadmitted sources fail closed.

The algorithm/storage owners remain the existing **M3JDK21** dynamic arrays,
logical-to-physical offsets and masks; sorted/filtered wrappers keep existing
Java collection contracts. No Entry-object graph, new JNI ABI, renamed public
JDK API or alternative M3 runtime layer is introduced.

Source-sealed Maven/OpenRewrite recipe:
`com.synexia.m3.M3Jdk21Interop76Receiver20261022` in
`hsoliwal/com.synexia`. M3JDK21 receives **only** these verifier/test/source
receipts and adjusted product CI; no recipe source or fixture templates.

Proof order:
1. Reverify immutable historical 36 owners and five-atom recipe receipt on
   the pinned historical product checkout, not against newer overwritten files.
2. Compare the current 29 unchanged and seven revised historical owners
   against an exact old→current revision receipt.
3. Run six adversarial successor verifier tests.
4. Verify all 40 added source identities and the full 76-path tracked set.
5. Continue unchanged original Maven recipe proofs, full collection-module
   compilation, API/ABI/serialization, Guava/Apache/Eclipse/Spring/JDK
   differential tests and available JNI mask-compaction parity.
6. Only label a result qualified if the exact-head product and canonical
   Synexia workflows have completed successfully.

This source-census repair is **not** behavioral compatibility evidence by
itself. Source bytes have been independently confirmed against the current
M3JDK21 master and prior 40-row Synexia receipts.

Historical origin: M3JDK21 failed four-library workflow on October 22, 2026;
canonical Synexia source successor PR #12999, which preserved the 36-file
baseline and catalogued 40 additional Java files.

## Three compiler-targeted source revisions (October 22, 2026)

A later full Maven interop run progressed past the original 36+7+40 census,
the existing recipe proofs and core collection compile. Its Java 21 test
compiler then exposed three bounded, independent Java problems:

- `M3ApacheCollectionsTest`: an intentional Apache Commons Collections 4.5
  `Predicate` field triggers a deprecation warning under `-Werror`. Apply
  only a field-scoped deprecation suppression, retaining the Apache predicate
  and all its JUnit assertions.
- `M3FiveLibraryKeyRangesTest`: missing `java.util.Map` import for
  the existing `Map.of` test oracle. Admit that import and nothing else.
- `M3FiveProviderFlagSelectionTest`: a test refers to the nonexistent
  `M3FiveProviderDirectSdk` instead of the already-existing canonical
  `M3LibraryCollections.springMultiValueMap()` factory, which returns
  the original M3-backed `M3SpringArrayMultiValueMap`. Reuse that owner,
  with no new adapter layer or JNI code.

The exact three generated Java source changes are mastered only in Synexia
under `com.synexia.m3.M3Jdk21InteropThreeCompilerAtoms20261022`.
A separate source-sealed `changed-compiler-03.tsv` records **one**
historically admitted source and **two** source-addition revisions with
old/new SHA-256, Git blob, recipe and pending behavior status. No old 36,
7, 40 or compound-var receipt row is rewritten or renamed.

The current immutable original source epoch is still 36 entries. The
historical old-to-current lineage now consists of eight pinned revisions,
including one Apache predicate test; the 40 separately discovered source
paths still exist, and two of them have additional exact source-pinned
compiler revisions. Source identity or Java grammar success alone is not
vendor behavior qualification. The full interop Maven, JDK, Guava,
Eclipse Collections, Apache and Spring tests, plus applicable JNI gates,
must pass before promotion.
