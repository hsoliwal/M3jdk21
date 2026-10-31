<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Current SWT proof-POM runtime closure — 2026-10-21

This canonical task corrects one missing dependency in the **existing SWT scrollbar proof POM**. It uses the unchanged `M3HashPinnedTextSnapshotRecipe`, composed as `com.synexia.swt.CurrentOwnerRuntimeClosure20261021`. It adds `org.slf4j:slf4j-api:2.0.13`, the API version already used by the qualified integrated SDK proof. It changes no geometry implementation, test expectation, engine, workflow selector, JNI symbol or product dependency.

## Exact owner and history

The receiver is `hsoliwal/eclipse.platform.swt@c0ffa6d76338ed1b06cfe0158b0e04d8930df546`. Its existing `ViewportScrollBarGeometry` already contains the full-range arithmetic repair, including the widened final hit endpoint. SWT [#236](https://github.com/hsoliwal/eclipse.platform.swt/pull/236) received the proof and [#237](https://github.com/hsoliwal/eclipse.platform.swt/pull/237) received the geometry. The current owner is blob `7370ac91ca85784b7b72efabef4129ffbfd3cea3`, SHA-256 `df4f8498b1f633f2ffeae310f761ecaa0ea686e392557b8cfe2c684864a7f9b4`.

The original recipe is [Synexia #12544](https://github.com/hsoliwal/com.synexia/pull/12544), head `9372a2d57f1a05394172a7bd85dda3f0d6d83983`. That PR remains unmerged at the recorded readback. Its original canonical POM has the same bytes and missing dependency as the received SWT POM, but that old canonical module path is absent from this publication baseline. This task records the correction in the canonical recipe workspace and targets the existing SWT receiver path. It does not reconstruct an absent old canonical module or silently modify that separate PR branch. `provenance/intake/REMOTE_LINEAGE.json` retains the exact observations.

| Field | Exact value |
| --- | --- |
| Target | `docs/m3/scrollbar-full-range-20261020/recipes/swt-scrollbar-full-range-20261020/pom.xml` |
| Before SHA-256 | `7539cf81d2d7a7b30bff97ce2812a7e67f4e3593463a898aa38c8e6b0240dfa0` |
| After SHA-256 | `6553565c8dd14a857e796f741f3f8f8e7cbf3edda11df58b8dcd8a53e78e5600` |
| Active phase | `swt-current-owner-runtime-closure-20261021-001` |
| Source kind and authority | One hash-pinned Maven XML file; actual Maven model execution remains the build authority |
| Production geometry changes | Zero |

## Reproduced failure and correction

The unchanged owning POM compiled the three existing recipe-engine sources and its original test class. Its seven tests then reported four failures and three errors because `org.openrewrite.Recipe` could not load `org.slf4j.LoggerFactory`. Adding the explicit SLF4J API dependency supplies that missing runtime class. No logging backend is required by these tests; the API's ordinary no-provider behavior is retained.

Two separate infrastructure observations remain preserved. The first offline attempt stopped before tests because `maven-clean-plugin:3.2.0` was not yet in the local cache. A later workspace build stopped at discovery with zero tests after encountering a transient synchronized class path. Its copied XML was stale output from the earlier seven-test failure and is explicitly excluded from that attempt's test count. The successful candidate and canonical SDK ran in fresh temporary build directories. `provenance/BASELINE_FAILURE_CLASSIFICATION.json` records the exact classifications and hashes.

## Existing category comparison and rights

| Candidate | Decision and reason | Rights or identity |
| --- | --- | --- |
| Current proof POM | Preserve as frozen failure input; it omits the API class required by OpenRewrite | Existing first-party Apache-2.0, blob `420eabcfebc730b8d932d2db884908c1cd77840b` |
| Qualified integrated SDK's `slf4j-api:2.0.13` | Selected; supplies the missing class with one dependency and no backend | MIT; exact artifact and license hashes in `provenance/SOURCE_AND_RIGHTS.json` |
| Previously qualified scrollbar packet's `slf4j-simple:2.0.13` | Existing alternative, unnecessary backend for this proof's contract | Preserved as comparison evidence; no backend added |
| JDK logging or a replacement logging facade | Does not supply the required `org.slf4j.LoggerFactory` binary name | No alternative implementation introduced |
| Skip tests, change assertions, or inject a stand-in class | Rejected; would leave the owning build defect unresolved | No such change admitted |

The original POM, reusable recipe/harness and new configuration remain Apache-2.0. The SLF4J API's complete MIT license is retained under `licenses/`. Unchanged SWT source fixtures preserve their original Apache-2.0 OR EPL-2.0 notices. No challenge/editorial source or new algorithm is imported for this dependency-closure task. No Synexia runtime service is introduced into SWT or M3JDK21.

## Executed qualification and its limits

The authoring candidate passed the **unchanged seven-test SWT SDK** with zero failures, errors or skips. The existing `verify.py --receiver` then consumed the actual SDK-generated geometry and compared it with the exact current SWT owner. It compiled the historical preimage and current geometry with Java 21 warnings as errors, ran each in tiered and C1 modes with **5,723,783 checks per mode**, retained the historical extreme-range discrepancies, and observed zero candidate mismatches. Package/public descriptors were identical.

The separate canonical POM recipe passed its inherited inventory-driven JUnit harness offline. It emitted one exact POM, reached a zero-change fixed point, rejected missing/duplicate/drifted inputs, preserved unrelated Java/text/native-looking decoys, and agreed across order, repeat, mixed-state and two-worker replay. The actual current SWT geometry class supplied the attribution probe. All 18 source inputs and the compiled dependency identity read back unchanged. The proof-checked copier separately passed one first write, zero replay writes and four refusal trials. These are distinct authorities; their test counts are not combined.

The canonical source gate is `proof/sdk-001-offline/RESULT.json`; the candidate runtime receipt is `proof/pom-candidate-001/RESULT.json`; copier guards are `proof/copier-qualification-001/RESULT.json`. The actual receiving-tree application is recorded in `proof/receiver-application-001/RESULT.json`: one applied POM and zero replay writes. Its fresh final gate, `proof/receiver-runtime-001/RESULT.json`, passed all seven original SDK tests and the existing `verify.py --receiver` with the same per-mode arithmetic checks, zero current mismatches, identical descriptors and all 27 input readbacks unchanged. It consumed the canonical emitted POM; the current geometry bytes remained unchanged. `QUALIFICATION_SUMMARY.json` indexes the distinct source, application and runtime authorities.

The separate task-specific correction-memory gate records three linked observations with admission still pending. It passed 129 profile checks, including 32 control-character cases, and verified three event records against 25 unique artifact hashes. Its counts describe evidence integrity and do not add runtime tests or product admission.

No native Win32/GTK/Cocoa display, real drag, DPI, accessibility, SWT lifetime, screenshot, Tycho reactor, full M3JDK21, performance or memory acceptance is claimed. The existing general SWT suites also do not discover the separately named geometry fixtures through their explicit `AllTests` selections. This one-file dependency correction does not alter those selectors or claim a stronger all-widget corpus. A later bounded extension can explicitly select the existing owner tests and the independently qualified full-range oracle through existing build/workflow owners, preserving all native gates.

## Reproduce the canonical source gate

Compile the exact current `ViewportScrollBarGeometry.java` into a fresh class directory with Java 21. Put that absolute directory path on one line in `parser-classpath.txt`. Run:

```sh
python3 qualify.py --java-home /path/to/jdk21 \
  --maven /path/to/mvn --maven-repo /path/to/offline-cache \
  --parser-classpath /path/to/parser-classpath.txt --attempt fresh-offline
python3 apply_receiver.py /path/to/exact-swt-checkout
python3 apply_receiver.py /path/to/exact-swt-checkout --apply
python3 apply_receiver.py /path/to/exact-swt-checkout --apply
python3 verify_sources.py /path/to/exact-swt-checkout
```

The output copier installs only bytes emitted by the successful canonical scheduler and refuses source, manifest, output, proof or engine drift. Reapplication is a no-op. Then run the SWT receiver's original owning POM and `verify.py --receiver` at that exact receiving tree. The existing SWT path-scoped workflow already watches this POM, runs the SDK/verifier, and performs a separate exact receiver byte comparison; no workflow activation or selector change is part of this task. Hosted CI and all native/platform gates remain independent of these local receipts.
