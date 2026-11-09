<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/recipes/source-slices-v2/PLAN.md blob 35ad473ee387; recipe m3/optane/recipe/pmem-source-image-20261106/relocate.py v3 -->
# PMEM source-slices v2 implementation plan

> **For agentic workers:** Use superpowers:executing-plans for inline implementation and one independent whole-change review. Steps are tracked below.

**Goal:** Extend the existing PMEMSDK source-image reader with verified reads into a reusable caller-owned buffer, repair empty-slice lifecycle checks, and qualify complete 20,000-file JNI traversal.

**Architecture:** Reuse M3PmemRegion and its native mapping/lease owner. Extend M3PmemSourceSlices and its existing Maven-backed exact-source recipe; retain readSealed(int). M3File remains the image/path/snapshot owner and the existing quality/Chrome workflow remains the inference owner.

**Tech stack:** Java 21, existing JNI/POSIX C, Maven, JUnit Jupiter; no runtime dependency additions.

**Spec:** ../../docs/SOURCE_SLICES_CONTRACT.md and the user's 1GB / 20,000 x 50KB serial source-processing proposal.

## Global constraints

- Canonical remote input: hsoliwal/com.synexia PR 15204 at f75ddfc10fc161af646dea9660a650545ded7661.
- Update that existing branch with forward-only commits; preserve all existing M3PmemRegion and JNI signatures.
- Edit recipe templates and the existing recipe first; materialize targets only through executed exact-preimage admission.
- One stable, caller-owned mapping. The source owner retains its immutable image lease; SDK reads do not grant model invocation authority.
- Preserve one generation attempt per file in the existing inference owner; these tests execute zero model calls.
- Distinguish CPU test doubles, actual JNI/POSIX, complete logical-image traversal, physical residency and Chrome inference.
- Preserve historical recipe/source preimages and permanent regression evidence.

## Review focus

- Zero-byte files must obey mapping close semantics.
- Invalid target offset/length must fail before a region read or destination modification.
- A short file after a longer file must hash only the selected destination extent.
- Mutation or close must fail before a successful read can enter a consumer.
- Recipe upgrade must refuse unknown drift and symlinked target ancestors before any target writes.

## Task 1: Capture regressions and baseline

Files: existing module plus recipes/source-slices-v2/{PLAN.md,PROGRESS.md}, JUnit contract test template.

- [x] Fetch all 45 module blobs at the exact remote revision and verify Git hashes.
- [x] Execute original build-and-test.sh against actual Java21 JNI/POSIX and linked JMOD.
- [x] Add an empty-file-after-close regression against the original source and observe its assertion failure.
- [x] Add caller-buffer contract tests: offset sentinels, shorter second read, invalid destination geometry, SHA drift and independent concurrent reads.

## Task 2: Extend the existing source recipe and materialize

Produces: public int M3PmemSourceSlices.readSealed(int fileIndex, byte[] target, int arrayOffset).

- [x] Freeze original generated source, recipe script, POM and source contract preimages.
- [x] Extend PmemSourceSlicesRecipe with exact reviewed before-to-after upgrades, fixed-point checks and ancestor-symlink refusal.
- [x] Keep byte[] readSealed(int) as a compatibility method delegating to verified buffer reading.
- [x] Always call M3PmemRegion.read, including zero-length reads.
- [x] Hash only the copied target extent; return the verified byte count; document destination state on failure.
- [x] Add explicit named constants for newly introduced budgets and digest identifiers.
- [x] Materialize templates through the recipe; run retained CPU and native tests.

## Task 3: Prove the complete source-image workload

Files: existing native smoke template, JUnit tests, Maven POM template and recipe refusal script template.

- [x] Generate and physically write 20,000 distinct 50,000-byte fixture entries with exact SHA-256 identities.
- [x] Read every entry via actual JNI using one caller-owned buffer; assert exactly 20,000 successful entries and 1,000,000,000 verified bytes.
- [x] Retain an out-of-band mutation rejection after the complete pass.
- [x] Execute tests through Maven with actual JUnit, ordinary JIT and -Xcheck:jni; run targeted interpreted and native UBSan checks.
- [x] Verify old public methods and JNI symbols remain present.
- [x] Execute recipe create/upgrade/repeat/drift/template/symlink refusal cases and record exact output hashes.

## Task 4: Review and update the existing PR

- [x] Obtain independent code review; address concrete defects and rerun relevant checks.
- [x] Record evidence and integration limitations, including existing Chrome 48KiB full-source packet limit and unwired PMEM-to-quality host seam.
- [x] Publish only reviewed module paths onto the latest expected head of PR 15204.
- [x] Read back every changed remote blob; update the existing PR description with executed checks and exact remaining gates.

Publication completed: implementation commit b1767eddf851fc2e1c18fee8712593bd27940b9a was fast-forwarded onto the existing PR15204 branch with expected-head protection. All 49 changed remote blobs were fetched and matched the reviewed local bytes. The existing PR records receiving-tree verification and remaining integration gates.
