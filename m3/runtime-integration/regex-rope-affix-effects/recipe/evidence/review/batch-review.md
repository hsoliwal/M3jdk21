# Independent prepared regex batch review

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

Reviewed on 2026-10-17 by the independent affix/cursor task, before canonical recipe emission. No product or candidate code was edited during this review.

## Exact reviewed inputs

| Input | SHA-256 |
| --- | --- |
| `batch-work/templates/src/java.base/share/classes/jdk/internal/mindex/M3RegexDpPlan.java` | `44e60be49cee95ec53efa9eb809d0a463fb3bd217e3e22e8a1a7046c2c0b0b4e` |
| `batch-work/templates/src/java.base/share/native/libjava/M3RegexDpPlan.c` | `b263d55dbcfa817c8b256d56995e389d6992b503f3a46bc5f1b075d82340605c` |
| `batch-work/templates/test/jdk/java/lang/String/M3StringRegexPreparedBatchTest.java` | `465e1002df44eb9453b3d6c45670af4b9a9de90e7fc746d9b82a2c85423be0ef` |
| `batch-work/optional-cuda/m3_regex_effects_cuda.cpp` | `696563e403619f9939c3722a6394f214651be7957cc90ea2d387c785b0d9f5a9` |
| `batch-work/optional-cuda/m3_regex_effects_cuda.h` | `79a0ea7c7a6693eae86119d7e18c976bd8029811ae89a04add18abfcc51a01d0` |

## Properties inspected

The prepared Java view snapshots caller coordinates and pins Effects from the exact same Plan. Immutable all-entry rows and UTF-16 boundary lanes remain intact. Budget refusal returns an unavailable candidate, and invalid coordinates, length overflow, cancellation, backend errors, and closure do not become a definitive false result. State masks include the 64th bit without shifting a 64-bit value by 64.

The fixed prefix is factored once into an open frontier and pending-high-surrogate flag. Two suffix acceptance preimages distinguish the incoming flag. The Java and C implementations agree for null and empty suffixes, omitted leading low surrogates after a pending high, a terminal high surrogate requiring complete-input flush, and identity fragments that preserve the pending flag. Prefix/suffix context does not narrow or replace the shared Effect rows.

Native creation derives its own context witnesses from the validated packed rows. It checks state masks, flags, empty-effect identities, CSR boundaries, tuple IDs, affix length overflow, row length overflow, and image geometry before publication. Java and C chunk limits charge tuple references consistently, including a maximum-size row with fixed affixes.

Java lifecycle locks protect active chunks while callbacks run outside those locks. Native handles use tagged, non-reused opaque IDs, and in-flight registry leases defer reclamation after retirement. JNI input arrays are copied through bounded region calls; no Java heap pointer is retained. Output publication follows lease release, and strict backend failures propagate. Only the explicitly preferred JNI policy admits the unavailable-provider fallback.

The optional CUDA code receives the same derived context and packed boundary lanes. Its per-instance workspace is serialized, the private stream is synchronized before copying results into the bounded JNI buffer, and disposal follows retirement of all native leases. No user text or independent regex parser is added to this provider.

## Conclusion and limits

No concrete correctness blocker was found in the reviewed seam, admission, lifecycle, or fixed-context factoring logic. This is an independent source review, not an independent execution receipt. Java/JNI execution evidence belongs to the source-bound contract run against actual SDK Results. The CUDA kernel was reviewed as source only: compilation with a real CUDA SDK, execution on a device, sanitizer coverage, and end-to-end throughput remain unqualified. This receipt does not claim a matched M3JDK/HotSpot build, full String-stage acceptance, or uniform CPU speedup.
