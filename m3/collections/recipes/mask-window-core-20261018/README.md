<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 mask-window core: canonical recipe and qualified receiver
Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0

## Ownership and exact scope
Canonical recipe: `hsoliwal/com.synexia`, `synexia-openrewrite-recipes/recipe-crates/m3-mask-window-core-20261018`.
Product: `hsoliwal/M3jdk21`, existing `com.m3.collections.M3IndexMaskCompactor` and existing C17 JNI file.
Target baseline: `f29b0afe7ad253cc0562bc8dfdd48fc1acec710e`.
Inherited source-seal tooling: Synexia `98368e107f4a205d852ef958cd48c1ee633219b1`.

The unchanged Java `M3GpuToolJoinManifest` and `SealReplay` are the recipe engine and filesystem adapter. This is a Maven-addressable exact-source recipe, NOT a new OpenRewrite parser, attributed visitor or competing generic engine. Reviewed original/pre/post bytes and all source/template hashes remain explicit. Target custody mirrors never acquire canonical ownership or introduce a Synexia runtime dependency.

## Implemented private atoms
Java full-array and independent-offset selectors share one stable emission kernel per primitive width; full-array counting reuses the window counter. Existing validation order, masks, detached results, native-required behavior, error messages and public descriptors remain unchanged.

The existing C int kernel now accepts both row and state offsets; the unchanged full/range/window JNI exports delegate to it. Int and long selection reuse their input chunk as output-prefix scratch. Entirely rejected chunks skip row-payload Region reads, but retain the second state scan and changed-count checks. Zero-mask selection does not read states. No malloc, callback, critical-array pin, retained pointer or new export is added.

`M3LongIndexProjection` and `M3LongAddressKernel` are actual unchanged product consumers, not interface doubles. Their sorted/sliced/mask-filtered physical occurrence identities are tested. Live predicate/comparator wrapper families are NOT replaced by detached mask snapshots.

## Reproduce
Requires full JDK 21, GCC with UBSan, Python 3 and Linux. Every output directory must be fresh.

```sh
python3 author.py --check
python3 verify.py --out /absolute/owned/fresh-proof
# On the receiving checkout, additionally admit its exact five source bytes:
python3 verify.py --root /absolute/path/to/M3jdk21 --out /absolute/owned/fresh-target-proof
# Maven execution of the same class-backed recipe and direct qualification:
mvn -B -ntp -f pom.xml verify
```

`author.py` authors only reviewed templates. Change the recipe/templates, rerun qualification, then apply with the unchanged `com.synexia.rewrite.SealReplay apply ROOT RESOURCE_DIRECTORY`. Never handpatch target outputs. Its `check` command reports a change count; fixed point requires `changes=0`, not merely exit zero. Qualification tests all eight partial images, seven pre-write refusals, unchanged timestamps, and source/template identity.

## Verification status
The current-source candidate passed 105 recorded direct qualification commands: 18,744 Java assertions and 20,549 actual checked-JNI assertions per corpus, repeated after each source pass. Original/candidate traces, public descriptors and seven JNI exports match. Eight partial source states, seven pre-write refusals, two native mutation controls and three old/new native binary combinations passed. These are finite selected-source results, not whole SDK acceptance. Raw logs and source-stage hashes accompany the delivery.

The source-seal engine is Maven-addressable and directly executable. Maven is unavailable locally; archive retrieval failed DNS resolution. Maven lifecycle resolution, complete collection/custom-JUnit reactor, historical installed-source successor reconciliation, source-built JDK/jtreg/JCK, cross-platform JNI and performance benchmarks remain NOT_RUN until actual receipts exist. No admission rule or global phase is relaxed. This is an independently reviewable DRAFT.

Shared javac frontend and selected build packs already exist in #1086; this increment is not complete custom Maven/Gradle/Ant/OpenRewrite packaging, M3File/Jackson/XML, L0-L5 intake, or all live filtered/sorted families. See SCOPE.tsv and RECONCILIATION.md.

## Current-source reconciliation, 2026-10-21

The earlier baseline and verification section above is historical. Current seals are in
`CURRENT_SOURCE_PINS.json`, and the forward-qualified correction is recorded in
`memory/LESSONS.md` and event `1d82d949bef954c038da5e3f08726634940c65bee35569ac114e373fc100c84f`. The source-seal engine remains unchanged.
The new direct evidence preserves all nine current JNI exports; Maven resolution was
attempted and blocked before build, as recorded under `reconciliation-20261021/evidence/`.

## Canonical publication amendment

The original source PR #12260 is merged/closed. Preserve its historical crate. The
current-source successor is the distinct `current-master-20261021` dataset under that
same canonical owner, prepared for the already-open source PR #12712 at
`bad47f27aa06d011d297841eba11d935e0258be2`. No merged branch is resurrected and no new
PR is created. Receiver PR #1280 mirrors this exact dataset in its existing custody
path after the canonical source commit exists. This is a local prepared publication
plan; no remote write has been performed by the qualification worker.

Publication amendment event: `5ac55e2fa109d1c46914f09a18d1365848e5d1302607aee6b9814b9ad7e3e62c`.

## Final checked source epoch:55b44776

The final source comparison caught newly merged PR#1638's additive long required/excluded
selector. The earlier1e5056b proof above remains historical; it is not the publication
preimage. The current Java seal is `ec8b9d2683f5fdb99f6c3e9ad4c79dc60e8ad19cbc3fde2b09e9da2fe3a83f2b`, Git blob
`6359eda54aceb2c3b73beab1cbb4718091a9041a`. C is unchanged. The same existing author was regenerated with this
new exact preimage and preserves the new method byte-for-byte.

Fresh serial qualification again passed105 commands. The current supplement passed14
commands:133,140 existing Java contract checks,512 actual JNI cases,136,397 independent
long-overload checks, and current/legacy/missing native capability cases on both before
and generated sources. All22 retained method bodies and JNI headers match exactly.
Current evidence is under `reconciliation-20261021/evidence/serial-v2` and `current-v2`.
Event:`5e4a86e1a376e7855daf8a2d885765f1b284b930d6219ad5ce016871d82dd156`. Previous receipts and events remain immutable. No global phase advances.
