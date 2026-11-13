<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 Wave21: source-exact signed half-open interval and optional JNI receiver

## Frozen first-party source/recipe ownership

Existing original Synexia [#15287](https://github.com/hsoliwal/com.synexia/pull/15287)
`59058d0b8e4acae90b8ceed2240d39b38dbb24b3` owns Maven/OpenRewrite `com.synexia.m3.CpuStructuredWave21_20261102`
with exact `M3HashPinnedJavaSnapshotRecipe` and
`M3HashPinnedTextSnapshotRecipe` source images, JUnit SDK, fixed-point
and occupied/drift refusal. Do not create an alternate recipe engine.

Receiver base: [M3JDK21 #2301](https://github.com/hsoliwal/M3jdk21/pull/2301)
`69dd6b5543950987920cf8fb4efe2de5096854bb`. Its scoped Wave20 Git source, Java21, C17 and
JNI JIT/-Xint GitHub Actions jobs have actually passed, but
JMOD/jtreg/full reactor remains unverified.

The seven Wave21 receiver files were ABSENT at the exact base.
Seven canonical UTF-8 SHA-256 source snapshots were independently
compared to the original Maven manifest and Git blob IDs in Synexia.
`SOURCE_RECEIPT.tsv` binds each exact path, preimage, SHA256, Git blob,
algorithm atom and qualification status.

## Additive product contracts

`M3IntIntervalSet` joins/normalizes signed **half-open [start,end)**
intervals into one primitive int[] backing lane, with detached result
arrays, exact long cardinality, binary-search membership and bounded
coverage queries. Adjacent ranges merge; reversed and malformed
input refuses. No existing Java/JNI method, class or exported ABI changes.

`M3IntIntervalSetNative` is a separate opt-in JNI `contains0` bridge
requiring an absolute `m3.cpu.interval.native.path` property.
Native C17 applies a monotone cursor only to proven sorted large
batches and otherwise preserves per-query binary fallback.
Output fresh byte flags, all native input arrays released JNI_ABORT.
No JNI pointers survive return; normal Java-only use does not load JNI.

Original Java/JNI and C17 independent oracles are kept at their
original cpu-tools fixture paths. Wave21 adds exactly three columns
to the complete 91-category Wave20 ledger: the preceding eight
columns were verified byte-for-byte identical across all 91 rows.

Existing `make/m3/tooling/module-packs/cpu-tools/build.py`, `pom.xml`,
`src/com.m3.tools.cpu/module-info.java` and Wave20 Java/C source
are locked unchanged. The new JNI library is an explicit sidecar
for differential testing: no claim that the original M3JDK21 JMOD
packager has installed the new native symbol.

## Problem-category donor decision

LeetCode 56 Merge Intervals uses inclusive/closed assumptions;
HackerRank Gridland Metro is 1-based with inclusive endpoints;
GfG merging intervals shares a sort/merge shape only;
RoaringBitmap stores membership rather than this ordered signed
half-open interval representation. Sources are REFERENCE_ONLY
except original first-party Synexia; no challenge code copied.

## Mandatory stage order

Docs → original recipe/SHA and ABSENT preimages → per-file Java source
→ native C17 → unchanged Java/C differential oracles and catalogue
→ strict compiler/lint/header → C17 native kernel → JIT/-Xint
and -Xcheck:jni → actual Maven/OpenRewrite SDK/JUnit/fixed point
→ complete M3JDK21 JMOD/jtreg/ABI/HotSpot/platform → code-loss
history checks. No stage can be silently marked passed.

**DRAFT/HOLD** until executed mandatory release gates.
