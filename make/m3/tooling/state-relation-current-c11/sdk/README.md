<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Current StateRelation C11 source-custody SDK

Canonical owner: hsoliwal/com.synexia. Admission was committed before authoring at
baa99d4a2ab1dd71fe05706bae4e7444ade70e30. This SDK is source-only and NOT_RUN.
It imports the existing source-sealed recipe engine, manifest kernel and Maven
POM unchanged. The descriptor activates
com.synexia.m3.StateRelationCurrentC11 through one thin class-backed adapter.

The complete current receiver closure is pinned at
adc10e96af417655a7f1fc61b07c0ca8789c83da:

| Owner | Git blob |
| --- | --- |
| M3StateRelation.java | 30359b57b7b553e78355d3672119c685051ec1da |
| M3DotStarRelations.java | d70a152bdedc380ed8b28861ea94ef4c2adbea90 |
| M3StateRelation.c | 89b3127f0f262de3e00ba28a01079a659013dce2 |
| m3_state_relation.h | 0e0c70e60b2cee61c3df31982a26f4251955cb61 |
| M3RelationPortProof.java | e9a6beee8916cf97d5d4aac2383e7ac2fa8843e0 |

Every manifest row is KEEP with equal before/after SHA256. Complete before/out
fixtures preserve exact bytes and notices; KEEP has no replacement payload, so
the tests explicitly require null row.text(). The historical C blob
e6a10286f64052cb948de1dbcc43aaaa1d33db1e is pinned only as an immutable audit
resource. It cannot be admitted as the current C owner.

The three real JUnit methods are:

- exactFiveOwnersReplayAsFixedPointInBothOrders: two input orders, each followed
  by another real named recipe application, for four zero-output successes.
- everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation: five owners times
  drift, missing and duplicate cases, for 15 refusals.
- historicalCRefusesCurrentOwnership: one historical-C refusal.

The tests also preserve unrelated input, require unchanged original SourceFiles,
verify exact named YAML activation and missing-name refusal, and write fresh
counter evidence only after all three methods pass. They do not compile or
execute the fixture Java/C owners.

The SDK has exactly 23 files, including 22 nonself pins. verify.py rejects missing,
changed, duplicate, redirected or unsealed inputs using raw UTF8 SHA256, Git blob
SHA and byte size. Its authored self-tests contain 21 XML-report refusal controls,
14 counter/stdout controls and 54 closed-input controls. Synthetic controls are
not JUnit execution. The actual reports phase requires three named tests with no
failure, error or skip, the unique counter stdout marker, a fresh counter JSON,
three real Java21 SDK provider class files and no emitted product files.

Future qualification uses the existing bounded isolated hosted lane. The lane
must provide fresh M3_QUALIFICATION_ID and exact EXPECTED_SOURCE_SHA, an admitted
output/cache root, verified toolchain and reserve. From this SDK directory:

```sh
python3 -I -S -B verify.py --inputs --self-test
mvn -B -ntp -Dstyle.color=never -Dmaven.test.skip=false -DskipTests=false test
python3 -I -S -B verify.py --reports
```

The unchanged Maven POM also invokes verify.py --self-test during validate.
Temporary self-test copies are confined to SDK/target; --reports creates no
temporary tree. Never rerun in an old output directory: counters and receipt
must be CREATE_NEW. The main receipt is target/sdk-receipt.json, schema
M3_STATE_RELATION_CURRENT_C11_SDK_RECEIPT_V1, scope
SOURCE_ONLY_STATE_RELATION_CURRENT_C11_CUSTODY. It links actual XML and counter
hashes, compiled SDK providers, five unchanged owner hashes and zero emissions.

Current C11, owner Java compilation, absolute-library JNI execution under
-Xcheck:jni, original private model, Windows, String/full-JDK integration and
performance/retention remain separate NOT_RUN gates. Historical C17 proof and
passing SDK replay cannot promote any of those. No production source, ABI,
native kernel, test corpus or proof body is changed by this SDK.
