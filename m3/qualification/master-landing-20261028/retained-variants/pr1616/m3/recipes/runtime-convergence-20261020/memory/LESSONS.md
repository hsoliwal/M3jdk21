<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Current lessons for runtime convergence

This is a derived reading view. `events/<sha256>.json` retains immutable observations, scopes, atom/source pins, patterns, exclusions, commands and evidence. Read failed attempts and linked successors together. No target ADMITTED event existed when this view was created.

## RCM01_FILTERED_DONOR_LIMIT

**REJECTED** · [8a92d05b5d69](events/8a92d05b5d693816afb7eac69b31024f8bd61abc38cc3624d6cac09c7531cb74.json)

M3FilteredListProviderInvalidationTest must pass for end, empty and nested provider cursors before a cursor mutation rewrite is selected.

The donor fixed ordinary cursor writes but did not validate every exhausted/bounded/empty sublist generation before indexed mutation.

Retain the donor as a reusable lead and exact historical input; reject it as the final provider-safe postimage.

Evidence boundary: Original focused donor suite: 3 pass. Extended provider regression: 7 tests, 3 failures, 0 errors, 0 skips. Final admission refused.

Applicability: Existing filtered cursor delegates payload ownership to a List provider. Original provider iterator carries generation semantics.

Limits: No blanket fail-fast policy for CopyOnWriteArrayList or arbitrary snapshot providers.

## RCM02_FILTERED_PROVIDER_FIX

**VALIDATED** · [27a51ab0ba0c](events/27a51ab0ba0ce99429286bac8c679c540f2edca2ab09de504c39c4da7e2a3a15.json)

Validate original iterator generation before each cursor mutation; accept the fallback only for UnsupportedOperationException from an otherwise writable provider.

Indexed mutation must validate the original iterator, including the bounded end where a forward probe is unavailable; a disappeared delivered span is a concurrent modification.

Use original iterator forward/backward probes with restoration; refuse vanished spans; delegate genuine empty insertion to original iterator.add, with only UnsupportedOperationException falling back to the writable backing List.

Evidence boundary: Seven provider tests pass; ten selected tests total pass. Earlier complete candidate module: 475 tests, 0 failures/errors, 2 native-only skips. These historical counts predate receiving master updates.

Applicability: Pinned existing M3FilteredLists cursor representation. ArrayList, LinkedList, snapshot/writable and fixed-size contracts are checked separately.

Limits: Arbitrary custom provider effects outside retained fixtures. No asserted predicate-count or speed improvement.

## RCM03_READONLY_SORTED_ENDPOINTS

**VALIDATED** · [5f75789dfe70](events/5f75789dfe7019246ed573d2ba012410540a0f41a16103e51f8b98c0b8ce951d.json)

M3FilteredEndpointRestorationContract must pass empty refusal and backed reversal checks; declared reflection changes must be recorded separately from callable API equality.

Inherited endpoint defaults traversed before failing; generic sorted reversal could lose the live provider-backed contract.

Reuse exact PR1554 endpoint overrides and provider-backed sorted reversal.

Evidence boundary: Candidate endpoint contract reports 1843 checks. Public callable methods retained; declared reflection differs for explicit inherited endpoint overrides.

Applicability: Strict read-only owners only. Pinned PR1554 source 987f743658055252bdaf056610a3a903eba0bfee.

Limits: Mutable filtered siblings and donor families retain separate policies. Added removeFirst/removeLast declarations are observable through declared-method reflection; this is a disclosed bug correction, not universal observational equality.

## RCM04_STABLE_RUN_REVERSAL

**VALIDATED** · [798594cd28aa](events/798594cd28aa73f1fd22b50457895a0b12ed183d99bb2873c891393a32a5af9e.json)

Use linear run reversal only for a known opposite sorted direction; keep stable merge sorting for unknown encounter order.

An already sorted projection needs only order-of-runs reversal; arbitrary encounter order still requires its original stable sort.

Reuse PR1549 stable equal-value run reversal; keep the existing fallback sort for unknown order.

Evidence boundary: Order/window contract: 2990400 checks, 65536 mask pairs and 67914 windows. Known sorted reversal is O(n); arbitrary sorting remains O(n log n).

Applicability: Known ascending or descending snapshot. Duplicate occurrence indices and original payload identity remain authoritative.

Limits: Mutable/live provider views. No measured broad workload speedup or heap claim.

## RCM05_SHARED_JAVA_JNI_MASKS

**VALIDATED** · [a07dbfed3c4a](events/a07dbfed3c4a0a89a384ada50b030898945f3be8ae507b3e04a651f285d9fa69.json)

Retain count/emit state reads and drift checks when omitting payload reads; require all old/new Java/C ABI pairings and the work-count fixture.

Independent whole/window emission bodies duplicated checked primitive selection; native emitters read payload chunks even when no selected row required them.

Reuse existing private int/long window emitters and one native region kernel, omitting only payload reads for wholly rejected chunks.

Evidence boundary: Four pairings each pass 320006 checks with identical digest 8a4338e94c64996bde776a42c0de8a0eaa6857a31f1beb3eacb60586f01be012. Both native work variants pass 54 checks. Payload reads 4 to 0/1/4 for empty/sparse/all; state reads stay 8.

Applicability: Primitive int/long row and unsigned state masks. Stable input arrays during the two-pass operation. Exact existing native symbols and generation headers.

Limits: JNI object references or new native ABI. Concurrent input mutation beyond documented drift refusal. Native call-count reduction is not latency or memory measurement.

## RCM06_PROGRESS_NESTED_ROLLBACK_FAILURE

**REJECTED** · [87a5a359ef13](events/87a5a359ef131f5e03c92d22c020d606e3d8e36a51a5e4d2e569ebf421f7015e.json)

fullyFailedNestedDeliveryRestoresOuterFractionForRetry must pass together with successful nested update preservation.

The first callback fix rolled back the fraction but left the revision advanced after a failed nested delivery, preventing the outer failure from restoring the original fraction.

Retain the failed candidate observation and prohibit final selection until both fraction and revision restore consistently.

Evidence boundary: Reviewer fixture: 16 tests, 1 failure, 0 errors/skips; expected 1 unit but observed 0.

Applicability: Synchronous reentrant owner.worked callbacks. No owner work accepted by either failed nested delivery.

Limits: Rollback of arbitrary external owner side effects. Intermediate draft source seal is unknown, so that draft cannot be replay-admitted.

## RCM07_PROGRESS_REENTRANCY_FIX

**VALIDATED** · [adb6b3b90099](events/adb6b3b90099c8cfae087d1b6c01ed165d913a2e2a864467486e4673a04fdf71.json)

Run ordinary failure retry, successful nested work preservation and fully failed nested rollback fixtures as one progress contract gate.

Owner callbacks can reenter lifecycle and fractional delivery; committed state must distinguish in-flight callbacks from successful delivery.

Guard begin/done in flight, commit successful lifecycle afterward; publish fraction/revision before callback and restore both on failure only if no surviving successful nested update supersedes it.

Evidence boundary: Baseline 16 tests has 5 failures; final 16 tests pass with real Eclipse runtime and SubMonitor. Historical module preflight also passes 16 with zero failures/errors/skips.

Applicability: Existing M3EclipseProgressMonitor public surface. Synchronous owner callbacks and existing SubMonitor ownership semantics.

Limits: Transactional rollback of arbitrary owner side effects. No new scheduler or generalized callback transaction.

## RCM08_OSGI_SERVICE_LIFETIME

**VALIDATED** · [94204130d30f](events/94204130d30f027e62d74b5a93dcc62515e8262adf4bb86bcc89568221013176.json)

Keep framework callbacks outside the activator monitor and retain context through UNREGISTERING; verify deferred stop owns the exact returned registration.

Context publication and synchronized external framework calls allowed callback-visible invalid state and reentry/lifetime failures.

Use private STOPPED/STARTING/ACTIVE/STOPPING state; framework calls occur outside the activator monitor; keep context valid through callbacks; defer stop until the exact registration returns; clear in finally.

Evidence boundary: Baseline 21 tests has 5 failures and 3 errors; final candidate and matching real module each pass 21 tests, zero failures/errors/skips.

Applicability: Existing activator and OSGi BundleContext ownership. Real runtime 3.35.0 and Equinox 3.24.300 in current-module preflight.

Limits: New registry, classloader or service store. Earlier Equinox 3.24.200 standalone fixture does not substitute for matching real-module 3.24.300 evidence.

## RCM09_LAUNCHER_ROOT_AND_RECEIPT

**VALIDATED** · [11de8f4d6fea](events/11de8f4d6fea13593f0fbf6f6aef11c3f95ca1fdbfda52b18508a4d09241e25e.json)

Source-launcher receipts must validate all 88 provenance files, 80 production files and jar legal/source identity before the owning qualifier uses them.

The verifier traversed two ancestors where the repository root requires four, and tried to read a nonexistent revision key instead of coordinate/source-jar identity.

Resolve owner.parents[3], retain exact inventory comparison, and report coordinate plus sources_jar_sha256.

Evidence boundary: Source/JAR verifier executed exit 0: 88 exact files, 80 production files, source-jar digest and built artifact digest retained. Full owning qualification pending at capture.

Applicability: Exact vendored junit-platform-launcher-1.10.2 layout and provenance schema.

Limits: No substitution of a downloaded launcher for the source-built dependency gate. No claim that fixing this standalone verifier alone passes the full collection owner.

## RCM10_CACHED_COMPILER_FALSE_GREEN

**REJECTED** · [30d50af737f1](events/30d50af737f1d6283fae7b1c8f2ed60ae1df60552ac0c8975658857c5948a8a7.json)

Run clean before owning source/dependency compilation; cached test success must never establish current source qualification.

Maven incremental timestamps allowed an old compiler output to pass while the verifier described current source hashes; end-of-run hash collection cannot alone prove executed bytecode identity.

Reject a qualification chain that removes test reports but retains cached production/test classes.

Evidence boundary: Cached classes: 16 tests pass. Same broken source after clean: 16 tests, 8 failures, 2 errors. This falsifies the no-clean receipt.

Applicability: Maven source verification with potentially existing target directories.

Limits: This is a verifier counterexample; the deliberately broken progress source is never a production candidate.

## RCM11_FRESH_BUILD_AND_EARLY_REFUSAL

**VALIDATED** · [248b27ae9bd9](events/248b27ae9bd9bded6eeb07a26888272225a1f9a56d2484c46af6d3c386f3a2d8.json)

Invalidate PROOF before any environment refusal and clean all owning/source dependency outputs before compilation.

Cleanup must precede JAVA_HOME/HEAD checks; clean must precede compilation without deleting the newly created proof directory midway.

Delete the previous PROOF before resolving JAVA_HOME or validating EXPECTED_HEAD, clean the owning module before recreating OUT, clean-install all ten source dependencies, and clean-verify Eclipse modules in the contract driver.

Evidence boundary: Corrected source participates in exact SDK five-test pass. Independent stale-class counterexample is retained. Full receiving owner run pending. Earlier missing-JAVA_HOME/wrong-HEAD probes were reported as pass but their exact output log is unavailable in this packet, so no new executed gate is asserted here.

Applicability: Exclusive target source writer for the duration of verification. Exact receipt inputs and manifest postimages.

Limits: End-only closure hashes cannot establish arbitrary concurrent-writer safety. No full owner PASS until the complete corrected driver actually executes.

## RCM12_SDK_BINARY_INPUT_REFUSAL

**REJECTED** · [a2b6d13dc158](events/a2b6d13dc15855d1e86f5f23acc8321c7855b9e531daa6d47a09d76e3d1feb8c.json)

Do not import or py_compile the contract source under its bound directory; execute with PYTHONDONTWRITEBYTECODE=1 and refuse extra input files.

Python inspection created contract/__pycache__/verify.cpython-312.pyc under a directory included by exact SDK input binding; text decoding correctly refused that extra binary file.

Retain the refusal and preserve the strict binding; prepare a clean frozen input directory without generated binary cache.

Evidence boundary: Five tests executed, zero failures, one error: MalformedInputException. Four transformation contract tests passed; input-binding gate correctly refused.

Applicability: Current unchanged M3TaskRecipeTest binds all files under src and contract. Any generated input-side cache is a source binding change.

Limits: Do not weaken the binding to ignore unexpected files just to get a pass. Two initial contaminated attempts were reported; only the retained representative failed suite is asserted by this event.

## RCM13_FROZEN_SDK_INPUT_PASS

**VALIDATED** · [1232adc1d8d0](events/1232adc1d8d02cdd92c7b54a2d97de0ec1bd4f6a2b09c8137b40568f0eeda26f.json)

Before receiving, verify all five SDK tests and every bound source/output hash; reapply only an exact before or after target state.

Generated inspection caches were environmental contamination; the actual Java/text engines and exact unchanged receiver/test did not need relaxation.

Freeze the clean canonical input packet, pin sdk-owner to existing exact engine copies, execute Maven clean test, and retain SDK_INPUTS, SDK_RESULTS, emitted files and both patches.

Evidence boundary: Five SDK tests pass, zero failures/errors/skips. Actual generated Results bind all 29 changes. Receiving check reported 29 targets/29 pending changes before application; target acceptance remains a later event.

Applicability: Exact Java21/OpenRewrite 8.17.1 execution and engine identity in ENGINE_SOURCES.tsv. 29 manifest targets, 19 Java and 10 text/native.

Limits: Current evolving Synexia root engine is not substituted for the tested pinned copies. SDK success alone does not certify target module or runtime image behavior.

## RCM14_JDK_THRESHOLD_COST_REJECTION

**REJECTED** · [41c549612c20](events/41c549612c20b6eec1410e95cb2912ae8d27f9fef362baf68bdb9cef4ea12284.json)

A semantic pass cannot admit a runtime optimization whose retained cost gate is REJECT_COST_GATE.

Early run-count termination is semantically sound for tested cases, but five included paired forks over 24 workloads show only 1.00677x geometric-mean median ratio and four controls over 5% slower amid host noise.

Reject the runtime proposal and retain the exact current internal JDK compactor; archive candidate, workloads, measurements and correctness proof.

Evidence boundary: Both versions pass 199294 oracle cases, 13493171 existing differential checks and 3019650 current ArrayList/Vector checks. Cost gate rejects runtime replacement.

Applicability: Exact existing internal compactor and current source-patched ArrayList/Vector. Five included paired forks and all 24 documented workloads.

Limits: No claimed speedup from approximately 0.7 percent noisy aggregate. Stock source patch is not a source-built M3JDK21 image or jtreg run.

## RCM15_JDK_ORACLE_ONLY

**VALIDATED** · [b3fcb66cd50a](events/b3fcb66cd50a032149e1e19289ec3fcb824e7b179c526f9f07ef3853c88f23ea.json)

Receive M3MaskThresholdContracts as a test-only addition; exclude the rejected runtime postimage from every target manifest.

The rejected runtime experiment exposed reusable boundary/oracle coverage independent of the selected algorithm.

Receive only the new GPL/Classpath oracle and keep current ArrayList, Vector and compactor byte-identical.

Evidence boundary: 199294 oracle cases pass on both original and rejected candidate. Only the oracle is present in the 29 target manifests.

Applicability: Existing internal JDK compactor and actual source-patched callers. Lengths 0..16, all 65536 embedded seam masks, threshold neighbors and long random masks.

Limits: No exhaustive proof for all arbitrary-length inputs. No full-image or jtreg acceptance claim.

## RCM16_FULL_IMAGE_ENVIRONMENT_HOLD

**RECORDED** · [ce3036af044c](events/ce3036af044cdf11ff3435fa8b263dd7f9a4411467b32edd5993e9446c156d0e.json)

A missing image or configure prerequisite keeps installed-image acceptance blocked even when Java/JNI module witnesses pass.

Normal configure exits before compilation because Autoconf is absent; environment inventory also records other missing build tools/headers and limited disk.

Retain the actual blocked configure attempt and do not relabel module/source-patch proofs as an image build.

Evidence boundary: Configure exit 1, BLOCKED_CONFIGURE_AUTOCONF_MISSING, image_produced=false. No compiler warnings gate disabled.

Applicability: Historical checked-out source 73f59218dc4ef1a3ef79aad990e77273db2a2d60, production unchanged from initial baseline for this attempt.

Limits: No image produced. The preserved configuration attempt predates current upstream advances; it does not validate the new receiving parent.

