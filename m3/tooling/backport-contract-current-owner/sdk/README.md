<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Current backport contract fixture SDK

Canonical owner: hsoliwal/com.synexia. Admission was committed before authoring at
ab8633cc645487bc69bcd6919f328a0e1b5e40a9. Status: SOURCE_ONLY_NOT_RUN.

This SDK reuses the unchanged 1c1b recipe engine, 38df manifest kernel and d798
Maven POM from the admitted StateRelation source SDK. It adds no policy, planner,
authority catalogue, parser, native code or execution controller.

The exact receiver baseline is hsoliwal/M3jdk21 commit
9a164c31b38c7013c179a3ff38b0bb60c199c7fd. Eight full original paths are owned:

| Role | Path |
| --- | --- |
| REPLACE | m3/backports/test_compatibility_policy.py |
| KEEP | m3/backports/compatibility_policy.py |
| KEEP | m3/backports/POST21_PRIORITY_COMPATIBILITY.tsv |
| KEEP | m3/backports/JEP_CATALOGUE.tsv |
| KEEP | m3/backports/RELEASE_JEP_AUTHORITY.tsv |
| REPLACE | m3/backports/test_backport_execution_waves.py |
| KEEP | m3/backports/backport_execution_waves.py |
| REPLACE | m3/backports/BACKPORT_EXECUTION_WAVES.md |

Only the two owning Python tests and explanatory document have reviewed output
bytes. The production policy, planner and three authority/compatibility tables
stay byte-exact. Existing test methods/assertions and receiver serialization are
retained as documented by the independent fixture author. This recipe does not
run those Python tests or claim the full control reactor has recovered.

One descriptor explicitly activates two class-backed named recipes:

- com.synexia.m3.BackportContractCurrentOwner
- com.synexia.m3.BackportContractCurrentOwnerInverse

The existing kernel admits an exact before or after source independently for
each changed owner. Three changed bits yield eight valid partial tuples; no
blanket mixed-tuple refusal is claimed. Each direction applies every tuple in
both source orders and applies a second actual fixed-point run: 32 successful
calls per direction, 64 total. Full inverse fixtures recover the exact original
baseline, including historical test failures, and are never promoted as a new
current contract. Five KEEP rows have null replacement payloads.

The actual named JUnit class is
com.synexia.rewrite.M3BackportContractCurrentOwnerRecipeTest. Its three methods
are allEightPartialStatesBothOrdersReachExactFixedPoint,
allEightInverseStatesBothOrdersRecoverExactBefore, and
eachOwnedInputRefusesDriftMissingAndDuplicateOwnership. The last method checks
eight owners times drift, missing and duplicate ownership in both directions:
48 refusals with no original SourceFile mutation. All calls preserve unrelated
input. Explicit descriptor activation also refuses an unknown recipe name.

Only three forward candidates are retained as actual recipe output files under
target/emitted/<full-original-owner-path>. Inverse scheduling proves rollback
without writing extra output files. A fresh target/recipe-test-counters.json and
one exact stdout marker are written only after all three methods pass.

The closed SDK has 46 files including 45 nonself input pins. verify.py requires
exact path, Git blob SHA, UTF8 byte count and SHA256 for every input. Authored
synthetic validator controls include 21 XML-report refusals, 14 counter/stdout
refusals and 100 input-closure refusals. These controls are not actual JUnit,
Python or filesystem integration execution. The reports phase requires three
actual named zero-skip tests, counters 64/48/3, four major-65 SDK provider classes
and exactly three emitted files equal to the pinned forward postimages.

Future qualification must use an admitted isolated hosted root with verified
toolchain, explicit cache/temp/M2 routing, fresh M3_QUALIFICATION_ID and exact
EXPECTED_SOURCE_SHA. From the SDK directory, the commands are:

```sh
python3 -I -S -B verify.py --inputs --self-test
mvn -B -ntp -Dstyle.color=never -Dmaven.test.skip=false -DskipTests=false test
python3 -I -S -B verify.py --reports
```

The unchanged Maven POM also invokes verify.py --self-test during validate.
Self-test scratch copies are confined to SDK/target and removed by the existing
TemporaryDirectory mechanism. The reports phase creates no temporary tree.
Counter and main receipt use fresh CREATE_NEW files; never reuse an old target.

Verifier API: inputs() returns (pins, rawPins); SUITES names the exact suite and
methods; RECEIVER_CUSTODY maps all eight original receiver paths to baseline
SHA256; RECEIVER_OUTPUTS maps the three candidate paths to after SHA256.
validate_report, validate_counters and validate_marker enforce actual evidence.
The receipt at target/sdk-receipt.json has schema
M3_BACKPORT_CONTRACT_CURRENT_OWNER_SDK_RECEIPT_V1 and scope
SOURCE_ONLY_BACKPORT_CONTRACT_CURRENT_OWNER_REPLAY. It links actual XML,
counter, compiled providers, baseline custody and flat emitted source hashes.

Maven/JUnit, actual full Python methods, filesystem integration, full control
reactor, original private model, matched JDK, JNI and performance/retention are
separate qualification gates. No product is applied, no runtime behavior is
qualified and no migration phase or overall goal is advanced by this SDK alone.
