<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Sparse StateRelation C11 proposal SDK

Canonical owner: hsoliwal/com.synexia. Borrow-first admission was committed before
this source authoring at 5d2d2c39120c7a779e2beb06f0af19cb7364e253, capability
native-state-relation-sparse64-frontier. This is a source-only proposal. Maven,
candidate C11/JNI execution and performance evaluation are NOT_RUN.

The SDK directly imports the two existing source-sealed OpenRewrite engine
providers and Maven POM from current StateRelation SDK tree
a52ab91c9d023e9bc30bb2396e7599a305caf5e8. Their bytes and dependency coordinates
are unchanged. Two thin class-backed recipes activate by exact YAML names:

- com.synexia.m3.StateRelationSparseC11
- com.synexia.m3.StateRelationSparseC11.Inverse

Each direction has one exact C CHANGE and four KEEP owners. Complete frozen
before/out fixtures preserve the original Java StateRelation, DotStar, C header
and 71,748-case proof byte-for-byte. The C baseline is Git blob
610ca8bab47065a49b9b58814d6392a287f4fd8b, 5,899 bytes. The candidate is Git blob
abb751e6056e34f25d0f09660d575f362610359e, 6,237 bytes, SHA256
5d40fe7c65d54e26c6964313186624675592703ea6e2e1cb2886840b2f15347e.
The inverse payload is the complete original baseline C source.

Only the inner scan of a changed invocation-local frontier is replaced. Under
GCC/Clang, the guarded nonzero uint64_t bits value supplies __builtin_ctzll through
an unsigned long long cast; clearing its least set bit visits the same right-hand
rows. M3_STATE_RELATION_FORCE_POSITIONAL_SCAN or an unsupported compiler selects
the complete original size_t column/value/positional loop byte-for-byte. This
uses C11 and the existing headers. No foreign implementation or header is copied.

All validation, ceiling computation, adjacent-frontier reuse, staged output
writes, alias behavior, repeat algorithm, JNI entry points and bit transfers,
exceptions, public interfaces, ownership and native-allocation behavior remain
outside the changed atom. Sparse row visits may decline; worst-case O(n^2) for
n <= 64 remains unchanged. Performance and retention are unmeasured.

Three actual JUnit methods are authored:

- bothPartialStatesRoundTripAndReachFixedPointsInBothOrders: both complete
  before/after states, both input orders, forward/inverse applications and
  second applications at fixed points: 16 successful real recipe applications.
- everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation: each of five owners
  in both directions refuses drift, missing and duplicate ownership: 30 refusals.
- historicalCRefusesBothDirectionsWithoutMutation: the existing historical C
  fixture is refused in both directions: two additional refusals.

Tests preserve unrelated input, original maps and SourceFile identities/bytes,
check exact named activation and unknown-name refusal, and verify both manifests
and all payload identities. Only after all three methods pass do they retain the
actual generated SourceFile postimages, one in target/emitted and one in
target/inverse-emitted, and CREATE_NEW counter evidence. Expected counters are
three tests, 16 successes, 32 refusals and two emitted C files.

There are 27 SDK files including 26 nonself pins. verify.py checks closed physical
source inventory, raw UTF8 SHA256, Git blob SHA and byte sizes, original provider
and POM identities, both directional manifests and four unchanged owners.
Authored self-tests include 21 XML-report, 14 counter/stdout and 62 closed-input
refusals. Synthetic verifier controls do not constitute actual JUnit execution.
The reports phase requires all three exact named cases, zero failures/errors/
skips, the unique counter marker, fresh counters, four real Java21 provider
classes and both exact authentic emitted C bodies.

Future qualification uses the existing bounded isolated hosted lane with a fresh
M3_QUALIFICATION_ID, exact EXPECTED_SOURCE_SHA and verified output/cache routes:

```sh
python3 -I -S -B verify.py --inputs --self-test
mvn -B -ntp -Dstyle.color=never -Dmaven.test.skip=false -DskipTests=false test
python3 -I -S -B verify.py --reports
```

The unchanged POM invokes the verifier self-test during validate. Temporary
self-test copies stay in SDK/target; reports creates no temporary tree. A fresh
target/sdk-receipt.json links actual XML, counter, compiled SDK provider and
emitted-output hashes. Receipt scope is SOURCE_ONLY_STATE_RELATION_SPARSE_C11_PROPOSAL.

Before applying the product C target, compile the freshly generated candidate in
both fast and forced-positional variants with C11 fatal UBSan and -Xcheck:jni,
the complete existing 71,748-case/149,658-native-check proof with zero skips,
unchanged ABI/export and missing-library negatives. Run a bounded matched
current/candidate native-kernel benchmark with paired inputs, compiler flags,
deadlines, checksum equivalence and actual timing evidence. Sparse/dense,
bit-63, ceiling and repeated-frontier families must be distinguished. Do not
adopt from static operation counts or noisy timings.

The previously executed baseline proof (run 39354057348, job 118404674933)
qualifies the different original C blob, not this candidate. Full java.base/
libjava/String integration, the original private model, Windows, whole-JDK
compatibility and matched performance/heap/native/mapped retention remain
separate gates. No product source, public workflow or repository ref is applied
by this SDK authoring step.
