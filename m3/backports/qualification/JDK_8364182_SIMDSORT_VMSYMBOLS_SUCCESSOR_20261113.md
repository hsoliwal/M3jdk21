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
| `m3/backports/verify.py` | `e4ad2b11da3cd8352997c57686f24d70502bdbe8` | `fb3487eb4f83fd6fdf0e4af86014d9765458e8b7` | `be14df5623df961fe8f5a35b373b30574203e76a2dcd9baf007c7d3b4dd4771d` |
| `m3/backports/test_jdk8364182_source_lineage.py` | `f9036900225b4327501b07e850dea6fd84fd0652` | `336f938ca29824901e0bab57934f40d9ae101895` | `a4f28414d189a6158f9cb331acb319b0da0d6b5b2af8c715432d2c273180ac18` |

The existing source-owned `M3HashPinnedTextSnapshotRecipe` manifest is Git blob
`cde7c3c3a8c865a0c2f0b4f19ab996fc5162ea37`. Both public postimage Git
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
