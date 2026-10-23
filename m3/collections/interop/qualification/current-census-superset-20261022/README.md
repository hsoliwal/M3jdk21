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

One of the originally added 40 **test files** then received an independently
generated Java 21 grammar correction: the arrayification test's compound
`var` statement was split into two declarations. This later revision is
authorized by a separate, narrowly bound
`receipts/changed-added-01.tsv` entry containing the original SHA-256/Git
blob from `added-01.tsv`, the exact corrected SHA-256/Git blob, the canonical
Synexia recipe identity and **pending** product compatibility status.
The original `added-01.tsv` snapshot is immutable; no other source change
is accepted as a consequence of this one corrected test.
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
4. Verify the original 40 added source identities, then apply the single
   SHA-256/Git-blob-authorized Java21 test revision and check all 76 paths.
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

## Exact Commons Collections 4.5.0 Bag source/test successor (2026-10-22)

The immutable original 36-owner census and all 40 added source shards are
unchanged. A further, exact two-row historical successor is now stored at
`receipts/changed-apache-bag-retain450-02.tsv`. Its two permitted paths are
`M3ApacheBag.java` (original owner) and
`M3ApacheBagContractTest.java` (existing donor differential).

`verify_superset.py` checks each old SHA-256/Git blob against the original
census, then admits only the explicitly named new SHA-256/Git blob from the
new two-row receipt. Missing, altered, symlinked or unknown receipts fail
closed. The actual `verify_historical_revisions` path still runs the
unaltered historical verifier on its pinned original checkout, and still
enforces all 36 original and 40 additional source identities.

This transition fixes Commons 4.5.0 `AbstractMapBag.retainAll(Bag)` behavior:
a request greater than the owned multiplicity removes all copies of that
key. The receiving source retains M3 arrays/counts and public/JNI contracts.
The canonical mechanical authoring and donor catalogue remain in
`hsoliwal/com.synexia` #13114; this product only stores the resulting
Java source, tests and receiving proofs (#1854).

At this checkpoint real Maven/JUnit/JNI gates are **pending**. This is one
of the six independently reported remaining five-provider issues in #1852,
not a claim that all 158 original tests now pass.
