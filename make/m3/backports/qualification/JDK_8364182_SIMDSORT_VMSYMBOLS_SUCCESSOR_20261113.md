<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# JDK-8364182 exact HotSpot vmSymbols source successor — 2026-11-13

**Draft source-identity repair / NOT a JDK runtime backport / NOT a release approval.**

This product receiver contains only two exact **generated** script/test postimages
and this passive qualification receipt. The recipe engine, Maven/OpenRewrite
authoring crate, source templates, and improvement memory stay in private
[Synexia PR #17272](https://github.com/hsoliwal/com.synexia/pull/17272).
The original JDK-8364182 seven-row adaptation ledger and all original HotSpot,
Java/JNI symbols, test sources and GPLv2+Classpath Exception headers are unchanged.

## Immutable lineage

Product preimage: `hsoliwal/M3jdk21 master@0b7bb3ab0e0e1fec6c5818428dad81ae30e86fbb`.
Prior historical JBS-8364182 adapted `vmSymbols.hpp` SHA-256:
`eeffc686c1eef000be5e3be5eb02e13257c744a63ea926762e9d4749fadecc67`.
Historical M3 String superseded SHA-256:
`4f752ab79abf854d144e53118a2cc57a4b3cc2432140e9297079cca86dd2abab`.

- JDK21u-merged parent: `vmSymbols.hpp` Git blob
  `07a4452d5fabdf6f8f520421c3c668ae390bf666`, SHA-256
  `691bd90288f30d9d25504526f234abc7db862e577544a7733d3fe0d09e585ff2`.
- Later additive SIMD sort donor: [product commit cb0cc9a](https://github.com/hsoliwal/M3jdk21/commit/cb0cc9a88456795e90be97250f2462b01de59670)
  compared with direct parent `16e35bee827e585cfb6fa38bc4a78671972ddb80`
  adds **exactly one** `java_util_DualPivotQuicksort` declaration;
  source Git blob `65860effc9f00df74c1dd0dcea1e2c1605f54cfa`,
  SHA-256 `db65f0fdf1b3eba49a7819ab59a2c01f04b02c6e82daf8b55774d3c2404990aa`.

The current `vmSymbols.hpp` retains the JDK-8364182
`serializeSecurityPropertiesToByteArray_name` and the M3 String symbol family.
Removing only the one SIMD declaration from the exact current source reconstructs
the entire JDK21u source **byte-for-byte**. The repaired verifier requires exact
SHA-256, Git object identity, original target path and historical adaptation pin,
source markers, and one-line inverse; mutation, duplication and unrelated paths
still fail closed.

## Source-receiving identity

| Product path | Source Git blob before | Exact Synexia-generated product Git blob | SHA-256 postimage |
| --- | --- | --- | --- |
| `m3/backports/verify.py` | `e4ad2b11da3cd8352997c57686f24d70502bdbe8` | `9e2441357202a06e6631267c4dc4897209df351c` | `44281f429b240ed8604ca4eb9e7061bf8e1500f0cc721ddf9a8dc0d63bf8620c` |
| `m3/backports/test_jdk8364182_source_lineage.py` | `f9036900225b4327501b07e850dea6fd84fd0652` | `8ef2ef4743be947253ed647c6874e476cb43879e` | `5eb5c4ddc29f165d2eb7bf0c53c03f36a9ef81a7712d404a12a3e2cbaf0a045f` |

The existing source-owned `M3HashPinnedTextSnapshotRecipe` manifest is Git blob
`972e1618d1f62ff43c437abb0d97a94373fe3c67`. Both public postimage Git
blobs and full text bytes were read back and matched the canonical private
source postimages, not rewritten by hand.

## Executable gates (not replaced with source hashes)

- Python: `python3 -m unittest -v m3.backports.test_jdk8364182_source_lineage`,
  including original five tests and four additional positive/negative metamorphic tests.
- Existing original full source admission: `python3 m3/backports/verify.py --root .`.
- Existing `m3/backports/pom.xml` and product Maven control reactor.
- Canonical Java21 Maven/OpenRewrite/Jupiter first pass, fixed point and
  per-file drift refusal in the existing Synexia crate — **NOT YET QUALIFIED**.
- Modified source-built M3JDK21 image and serviceability `SecurityPropertiesTest`,
  `SystemPropertiesTest`, JMX/native/JNI/API/ABI, JIT and jtreg — independent
  mandatory product acceptance; **NOT YET QUALIFIED**.

This report does not claim that JDK-8364182 runtime behavior, all JEPs, or
the complete JBS backlog are accepted. Keep this receiver in **draft/HOLD** until
the required actual CI and source-built product gates pass. No historical
adaption or release receipt is rewritten to force a pass.


## Source correction successor (2026-11-13)

The original proposed source hashes were reviewed and retained in private
Synexia `qualifications/CORRECTION_20261113.tsv`, not silently lost.
An independent test check found an incomplete negative baseline-alias guard.
The canonical source-owned recipe was therefore resealed to the **current**
postimages shown above. The corrected verifier now refuses `baseline_sha256`
aliasing **either** the exact JDK21u source hash or the exact SIMD successor,
in addition to its original historical successor refusal. The test source now
defines **11 test methods**, adding independent refusal of the older JDK21u
and historical M3 String baseline aliases. Both current product Git blobs
were independently read back identical to current canonical source resources.

Source-exact byte matching is not actual OpenRewrite/JUnit execution or
source-built M3JDK21 serviceability/JNI/JTReg qualification. No source code
merge or release has been authorized by the correction.
