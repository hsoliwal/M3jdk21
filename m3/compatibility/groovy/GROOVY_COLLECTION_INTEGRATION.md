<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->

# Current merged-count GINQ and provider-receiver integration — draft

This is a deliberately combined source-built **test candidate**, not a request to replace or merge the individual algorithm or provider PRs without review. The original `countByMany` Java API/contract is already on `master` (product PR #3386); the current `ObjectStream.java`, `ObjectStreamLoopKernels.java`, and `ObjectStreamLanguageKernels.java` also include separately merged Java21 host compatibility changes (#3388). **No existing product Java method or JNI implementation is rewritten here.**

## Explicitly integrated product and verification inputs

- GINQ Java kernel and test: byte-identical to the reviewed, source-pinned Synexia recipe #18928 and original product draft #3389; four join modes INNER/LEFT/RIGHT/FULL with bounded Java equality semantics and nullable presence bits. This is **not** full Groovy GINQ, dynamic coercive `==`, GINQ AST/Queryable ABI, or MetaClass integration.
- Full/linked native `countByMany` and GINQ tests share **one** existing `m3_arrays` CMake/JNI library and run with `-Xcheck:jni`. Original CTest, `jlink`, installed M3 image and JMOD checks are mandatory.
- 54 original Apache/Eclipse/Guava/Vavr JUnit/test-helper Java files moved byte-for-byte to the established `providers` root (repair candidate #3402). The JDK full-module provider runner requires 14 exact original JUnit classes and retains all assertions; their original source hashes are sealed in the provider manifest. This does **not** exclude those tests without a replacement: a provider failure is fatal.
- The original Maven `m3/collections/pom.xml` remains unchanged. The broad collections `testCompile` and JUnit execution stay fatal after the selected jtreg and provider qualification lanes; no `-DskipTests`, `-Dmaven.test.skip`, or `continue-on-error` waiver.
- The current eight-file stream receipt `verify.py` updates only three stale postimage SHA-256 values and the two host-Java21 kernel delegation conditions, from the exact canonical text-recipe candidate in Synexia #19083. Its remaining five file receipts, API uniqueness, drift/missing-refusal tests, and JDK public semantics are retained.

## Fixed original source receipts

| Current original owner | SHA-256 required in the successor guard |
| --- | --- |
| `ObjectStream.java` | `40497e161e2e8d8c9ec7a6524b63d68bf4e2b775845b9f58bf193d1068d041d2` |
| `ObjectStreamLoopKernels.java` | `02b56913bb682f90a3f7d2d04689c259a6e8c6ee68748ad920cfd1082937c024` |
| `ObjectStreamLanguageKernels.java` | `c74cc7b797660e2a57548ca8a64931077e544ddf042c90ea015ceab4a5994e14` |

The historic countByMany canonical ObjectStream **postimage** hash identifies an earlier, approved commit and must not be substituted for the current master source identity. No historical Synexia template or original merge receipt is retroactively changed.

## Real acceptance and HOLD

Before release or merge, obtain the exact branch's source-identity/Python source refusal results, a source-built full JDK image, original CMake CTest, `countByMany` and GINQ full/linked receipts, installed-image fingerprints, actual jtreg, all 14 provider JUnit classes, and the original broad Maven JUnit results. Verify the new stream-receipt OpenRewrite SDK emission and fixed-point replay from Synexia #19083 independently; its successful CI run has **not yet been observed**. Current unrelated superset failures are not waived. The known Groovy 4/5/6 `String.capitalize`, `uncapitalize`, and padding precedence differences remain unresolved and require donor-runtime comparison.

Keep this integration PR **DRAFT/HOLD** until independent exact-head outputs show those finite receiving gates completed. This document is custody/requirements evidence only, not a declaration of a release-ready merged fork.
