<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 five-provider source successor (36 → 76)

This is the existing integrated SDK's exact installed-source census, not another M3+JDK layer. The original five-file historical OpenRewrite receipt remains unchanged. The preceding 36-file census is preserved verbatim as `predecessor-source-census-20261017.tsv`, SHA-256 `5ebaf6ef12f2f954029f588e38e36c844cf97b550626a4f16e35c9219d725620`. The current 76-file receipt is SHA-256 `bc80523d994a68c8f87bfc5fb2a1cc75ad0559b31f84f317c70a40290552603e` and pins both SHA-256 and Git blob identities.

At product commit `7f657d6ca05760af7bee7d4d2d9e14ae91ddd942`, 40 Java source/tests were added after the last pinned census and seven previously admitted files changed. All 36 predecessor identities remain retained. The verifier requires the exact seven replacement paths, checks their previous output SHA as the successor preimage, and refuses missing/additional tracked Java, symlinks, unapproved file changes and drift. The original five-file historical contract is checked against the predecessor rather than silently rewritten. Separate `source-provenance-20261021.tsv` identifies exact source changes and commit roots.

The seven changed files implement and/or test shared M3 counted ownership, extended five-provider index/array/mask paths, Guava random-access list contracts and Spring casefold binding expansion; their latest source commits were inspected. These source receipts do **not** certify the five provider's full Java/JNI runtime compatibility or performance.

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
Existing GitHub Actions run `39608772037` failed on the stale tracked-file census before compilation. The successor contains no production Java or C changes; it is a fail-closed gate repair. Independent Python successor-lineage oracle passed eight hostile cases, but target exact-head Maven/OpenRewrite, Python verifier, JUnit, JNI and installed SDK gates remain **PENDING**.

## Java 21 syntax repair — verified CI failure

Exact run [39623768546](https://github.com/hsoliwal/M3jdk21/actions/runs/39623768546) passed source admissions, six recipe proofs, original five-file source replay and original M3 core compile. Maven then rejected `M3FiveLibraryArrayificationTest.java:56`: Java 21 does not permit compound `var` declarations. The corrected original test keeps left then right evaluation order with two legal `var` declarations. Production Java, public APIs and JNI symbols are unchanged.

Original test SHA-256 `43d670c2278b4db3cb39e8c6d63eff56827c7ef8359e7c85aa8f72b892bba51d` (Git blob `42d78b4fae7525467f0a5d4f34baf45b511e3b55`), corrected SHA-256 `6d31a53eb94af7c3cfe08ce126ce1be1f8d7da1b09150bd72a12c602d3ab3565` (Git blob `e220622405432368d2c46b32f9f1c197913e7c0a`). The old 76-row census SHA-256 `aa4c050c26a33e6a0d539e8c8bb9d24139681c746a8633ded8eb5f8502e435fe` is retained in the canonical recipe history; corrected 76-row census SHA-256 `eb77198310b1b2fe66fb800a5a38870bf4bd913ba9a6e1c0b4a70f90a1c412f6`. Its 36-row predecessor and original five-file recipe remain unmodified. A tiny isolated `javac --release 21 -Xlint:all -Werror` test passed. Exact target Maven/OpenRewrite JUnit and native tests remain pending.

## Compiler corrective pass #002 — historical source preserved

Workflow [39625854548](https://github.com/hsoliwal/M3jdk21/actions/runs/39625854548) passed original source admission, six recipe tests and production Java compile, then real five-library JUnit compilation exposed Apache's deprecated `Predicate` with `-Werror`, absent `java.util.Map` import, and a stale unmerged direct-SDK test factory. The first fix suppresses only the intentional API deprecation at its field; the second adds the standard import; the third reuses original `M3LibraryCollections.springMultiValueMap()`, which returns the same M3-backed concrete class as the historical `M3FiveProviderDirectSdk` factory found in branch `m3/five-provider-direct-sdk-receiver-20261017`. Zero production Java/JNI changes.

Prior 76-row receipt `eb77198310b1b2fe66fb800a5a38870bf4bd913ba9a6e1c0b4a70f90a1c412f6`; next 76-row `bc80523d994a68c8f87bfc5fb2a1cc75ad0559b31f84f317c70a40290552603e`. Apache test is the eighth changed original predecessor with its old SHA carried forward; two later-added test sources retain ABSENT predecessor. Frozen original 36-source and five-source receipts unchanged. Actual Maven JUnit/native gates still pending.
=======
Original sources are reference values from the pinned M3JDK21 tree, not 'all five libraries implemented'. Real Maven/OpenRewrite JUnit scheduler and package/ABI/interop/HotSpot/multi-platform gates remain to be actually observed. The historic receipt SHA-256 is retained as exact predecessor, not reused as the current receipt.

## 2026-10-21 sealed successor for 76 owners

The original 36-row `current-source-census.tsv` was retained verbatim as
`previous-source-census-20261021.tsv`, with SHA-256 bound in `verify.py`.
At original M3JDK21 source head `712fda47e962631eddebe5e214b820796c755016`, exactly **40 new tracked files**
were separately reviewed and admitted and **7 existing files changed**.
The seven source predecessors and successors are recorded in
`source-successor-ledger-20261021.tsv` using before/after SHA-256 and Git blobs.
The immutable historical five-source recipe remains untouched; the changed
`M3LibraryCollectionsTest.java` historical successor is explicitly pinned to
its old and new hashes instead of disabling the historical protection.
Current verifier rejects incomplete tracked membership, predecessor/ledger drift,
new unexpected source files, mismatched Git blobs, SHA-256 drift, and any
unapproved change to the remaining historical four owners. There are 32
production and 44 test first-party interop files in this exact receipt.

This is an inventory/provenance correction, **not** proof all five donor-provider
contracts work or that 76 sources compiled successfully. The actual original
Java/JNI and full reactor gates must run after source admission; failed or
unstarted gates remain open. Canonical Synexia recipe dataset:
`m3-interop-census-20261021`; no manual edits to production source files.
>>>>>>> 9a6848a1564ca42bf7bbde31eafd1c2556090695
