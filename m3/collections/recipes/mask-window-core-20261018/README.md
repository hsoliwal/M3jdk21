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
