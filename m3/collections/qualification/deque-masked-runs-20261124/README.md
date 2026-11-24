<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# In-place M3JDK21 generic deque packed-mask survivor-run receiver

Product master base: `7423e6ef1080e033d066d4f1e65b9bb40010c23d`. Original `M3ArrayDeque` Git blob `66bbf9dee3cd41e514259fd186958b0e1658eae6`, exact source SHA-256 `c70380825ab7c443404189350566a22e057c9680991284e0b1851cf14d834300`.
Canonical Synexia Maven/OpenRewrite source template: [PR #19235](https://github.com/hsoliwal/com.synexia/pull/19235), commit `5225de37215b0c772d7abf2ea7c57b27052f25f4`, named recipe `com.synexia.m3.DequeMaskedRuns20261124`.
Generated original owner `src/jdk.m3.collections/share/classes/jdk/m3/collections/M3ArrayDeque.java` SHA-256 `263d42478bd4e850eb5d94bf8c519d5f2be8d39bdee04be1920016733275d108`.
New `test/jdk/jdk/m3/collections/M3ArrayDequeMaskedRunTest.java` SHA-256 `1c43dadc330926706e5c154b8d672dc1dc64047119f8bb516c305421880554a1`, ABSENT-before.

## Exact original-owner refinement
Leave original predicate evaluation and lazy `long[]` removal bitmap preparation unchanged. Replace scalar per-survivor copying with a `Long.numberOfTrailingZeros` word scan, backed by a private paired `System.arraycopy` ring-run mover. Clear only vacated aligned `Object[]` and `byte[]` physical spans, with up to two `Arrays.fill` calls for each lane. This reuses already merged M3JDK21 first-party array-span work (#2985, #3372), but the actual product source is the single integrated `jdk.m3.collections.M3ArrayDeque`. No new runtime layer, new collection root, per-entry objects, native symbol, API or JNI callback.

Preserve capacity, head, physical coordinate, object identity, masks, stable encounter order, no-op revisions, successful single structural epoch, exception/reentrancy atomicity and existing Java/native selected-index admission.

## Qualification evidence and HOLD
- Independent bounded Java 21 reference-vs-candidate physical-ring differential: **191,422** scenarios / **14,352,893** reference/state checks with JIT; **4,480** scenarios / **1,648,515** checks with `-Xint`. Compares entire physical payload and state lanes including cold slots, sizes, head, revision, arbitrary rotations and density.
- Java 21 public-deque regression against a local behaviorally equivalent stub, not the actual installed M3JDK21 module: **9,636** scenarios / **3,526,968** checks.
- Product source and JUnit template source identity verified by Git object readback on publication.
- Source-built M3JDK21 module `javac`, real JUnit/Maven/OpenRewrite recipe, jtreg/JCK, `-Xcheck:jni` parity, complete Eclipse Tycho/Equinox/SWT tests, platform native matrix, installed-JDK packaging and performance benchmarking: **NOT RUN HERE**. Do not mark release-ready.
- No external donor source copied. Original first-party Apache-2.0 rights preserved.

The full Eclipse Platform integration remains a separate multi-repository contract and release acceptance programme. This PR is one validated candidate behavioral atom only.
