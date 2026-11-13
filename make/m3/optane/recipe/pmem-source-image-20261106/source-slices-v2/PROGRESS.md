<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/recipes/source-slices-v2/PROGRESS.md blob 72b49c504e79; recipe make/m3/optane/recipe/pmem-source-image-20261106/relocate.py v3 -->
# SDD ledger — plan: m3-pmem-sdk/recipes/source-slices-v2/PLAN.md

## Frozen input and scope

- Canonical input: hsoliwal/com.synexia PR15204, f75ddfc10fc161af646dea9660a650545ded7661. All 45 module blobs were fetched and independently checked against their Git object hashes.
- Baseline actual Java21/POSIX JNI, 23 SDK assertions, 25 transfer assertions, 17 source CPU assertions, old sparse/sampled native check, JMOD and jlink passed before modification.
- The source host connection is absent from the inspected integration code. This change improves the existing source-read boundary and preserves its external M3File ownership. No new scheduler, durable store, source identity owner or LLM invocation authority was invented.

## Executed work

- Reproduced the empty-file-after-close defect against real JNI: original readSealed(0) returned after mapping.close(); the retained regression now passes.
- Compiled caller-buffer JUnit contracts against the baseline; all 16 new-overload call sites failed as expected. Added the overload through the existing source template and recipe; retained the owning-array API and all existing JNI signatures.
- Extended the same recipe to eight targets with seven exact existing preimages and explicit absent-before JUnit admission. Replayed from fresh source images; proved create, exact upgrade, repeat fixed point, source/template/baseline drift refusal, late-target preflight and symlink refusal.
- Corrected the Maven JUnit lint configuration after an initial build failure on the unchanged module-name warning. The final build uses the original module lint policy, -Xlint:all,-module, with -Werror.
- Final Maven verify passed: 23 SDK, 25 transfer and 22 source CPU assertions on regular and linked runtimes; each runtime verified all 20,000 distinct synthetic 50,000-byte source entries (1,000,000,000 bytes); all 10 real JUnit tests passed without skips.
- Same 10 JUnit tests passed under interpreted JVM / checked JNI and C17 UBSan / checked JNI. A complete scale traversal also passed with a 32MiB Java heap cap. This cap excludes native mappings and OS page cache.
- Public API comparison is exactly one additive overload; all 11 native JNI exports are unchanged.

## Independent review and corrections

A separate read-only reviewer inspected the whole change and new files. Two minor findings were corrected: atomic upgrades now preserve POSIX permissions (RED retained, 0755/0640 GREEN checks), and original postimage-manifest SPDX lines are retained. The final review reports no unresolved code findings. README wording was corrected from sparse-only coverage through an eighth sealed target and its retained preimage.

The review's eight explicit exclusions were accepted as integration/receiving gates, with reasons and concrete next actions in evidence/README.md. They cover canonical source identity/lease, PMEM-to-quality wiring, durable inference, source packet budget, physical residency/performance, other platforms/hardware, whole-repository acceptance and concurrent recipe writers. None is counted as a local test pass.

## Evidence and publication

- evidence/README.md records commands, toolchain, red/green failures, scope limits, reviewer dispositions and next actions.
- evidence/baseline-inputs.tsv records the 45 exact baseline files; evidence/qualified-inputs.tsv records all 33 qualification-relevant source/build/recipe/template hashes.
- Publication completed as implementation commit b1767eddf851fc2e1c18fee8712593bd27940b9a, parent f75ddfc10fc161af646dea9660a650545ded7661, tree 739b5f83e1cbffbdf23692917d0a816b456fa2ed. The existing branch advanced with expected-head protection and force=false. All 49 changed blobs were fetched at that exact commit and matched the reviewed local bytes and Git blob IDs. This receipt-only follow-up changes no qualified source/build/template input. No full-repository, live Chrome or hardware qualification is asserted.
