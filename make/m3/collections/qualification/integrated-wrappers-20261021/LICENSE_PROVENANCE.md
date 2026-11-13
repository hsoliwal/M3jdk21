<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Source, license and disclosure custody

The recipe tooling and this document are Synexia first-party Apache-2.0 work.
That license does not relicense an embedded template, before fixture, generated
product file or historical source record. Each such file retains its existing
copyright, license and modification notices. JSONL source-history containers
retain those exact bytes and are classified separately as mixed-license evidence.

## Exact source owners

The reviewed M3JDK21 source is commit
`33d2e6ae8048c436bd55b951d9f7dbfab2f3efcb`. The canonical shared recipe engine is
Synexia commit `408373f4101fdca2fb3625bec8af1234e5fd3e26`; neither engine source is
changed by this packet. The prior array-index implementation and its three
fixtures reuse the source published in Synexia commit
`2aa8dec54c00b121f25bab7e888f5c2afa79f7a0` and the corresponding M3JDK21 PR #1760.
The current cohort independently requalifies the applicable source and fixtures.

| Material | Rights and custody |
| --- | --- |
| Existing `M3IndexedArray`, `M3IndexedView`, `M3IntAlgorithms` and their derived source templates | Preserve GPL-2.0-only WITH Classpath-exception-2.0 notices and original authorship. |
| New core contract and resource fixtures | Preserve the GPL-2.0-only WITH Classpath-exception-2.0 headers already frozen in their templates; do not stamp the recipe's Apache notice over them. |
| Existing collection SDK Java/JNI owners and their fixtures | Preserve each existing Apache-2.0 notice, authorship and source lineage. |
| Existing `java.util.CollectionViews` owner | Preserve its actual Apache-2.0 header; the separately recorded `java.base` integration license review remains a product promotion gate. |
| New recipe binding, SDK helpers, task POM and authored documentation | Synexia first-party Apache-2.0, with canonical SPDX attribution. |
| Measurement logs and receipts | First-party evidence of the recorded finite executions, with source/fixture/toolchain hashes and explicit limitations. |
| Bundled original, rejected or superseded source | Exact historical bytes, with per-record source and license identities; the container does not convert their rights. |

## Existing license texts

The root Synexia `LICENSE` is the Apache-2.0 tooling license. The following
already-existing canonical copies were fetched at the pinned Synexia revision
and compared byte-for-byte with the pinned M3JDK21 license texts:

- `synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017/enhancements/20261021-eclipse-capabilities/licenses/M3JDK21-LICENSE`,
  SHA256 `4b9abebc4338048a7c2dc184e9f800deb349366bdf28eb23c2677a77b4c87726`.
- `synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017/enhancements/20261021-eclipse-capabilities/licenses/M3JDK21-ADDITIONAL_LICENSE_INFO`,
  SHA256 `a69bce275ba7a3570af6579cb0f55682cd75fedfcd49e0e8e9022270c447c916`.

The source-verification receipt retains the exact URLs, revisions, byte counts
and comparison results. Reusing those existing texts does not override any
per-file notice or close the separate target license-review gate.

## Donor decisions

The catalogue compares the existing M3/Synexia atoms first. Guava
`669a232b4e20ae6e8bc6efc62caaf2f830d56327` and RoaringBitmap
`dda30fae2bce83f3e7680e768024ed86594d6022` are revision-pinned Apache-2.0 references.
No new Guava or RoaringBitmap implementation body is transplanted in this pass.
LeetCode, HackerRank and GeeksforGeeks provide problem-category evidence only;
no challenge statement, editorial or submission body is copied. A reference
without an established compatible code license is not an implementation donor.

The catalogue preserves the distinction between abstract algorithm ideas,
independently authored adaptations and reused source. It records complexity,
observable callback/exception/ownership constraints, measured outcomes and
rejected candidates so later passes do not repeat failed comparisons.

## Disclosure disposition

This packet refines already-public M3JDK21 collection owners using established
array-copy, stable-compaction, iterator and bit-mask patterns. Synexia supplies
the reusable transformation recipes and validation evidence; M3JDK21 owns the
integrated runtime implementation. The packet introduces no new private payload
service, proprietary donor body, novel storage architecture or patent-status
claim. Its publication scope is ordinary maintenance of those disclosed owners
and the explicitly requested reviewable source/evidence handoff. Existing grants
and authorship are preserved; no legal novelty or patentability claim is made.
