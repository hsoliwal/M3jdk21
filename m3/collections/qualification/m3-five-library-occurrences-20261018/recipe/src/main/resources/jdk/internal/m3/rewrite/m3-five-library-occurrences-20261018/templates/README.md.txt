<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3JDK21 integrated collection owner: per-occurrence multivalue mutation

This is the product **M3JDK21**, not a bolt-on `m3 + JDK` overlay. The canonical
Java21 owner is the existing `com.m3.collections.M3IndexedMultiValueMap` in
the product's source tree. Public Java SE names remain JDK names. Guava, Eclipse
Collections, Apache Commons Collections and Spring Framework keep their separate
API types, semantics and licensing. The current mapped owner is useful in
their adaptation process but is **not** an automatic drop-in implementation of
every provider interface.

## Contract atom implemented

A physical Object[] retains the one canonical GC-visible occurrence value;
int[] next/group free lists and byte[] masks hold all structural state.
New methods `removeAt(key, ordinal)`, `removeFirstValue(key, value)` and
`replaceAt(key, ordinal, value)` mutate that incumbent backing directly.
The shared unlink method removes the first requested occurrence (not all
equals-equal duplicates), updates group head/tail/count, clears the vacated
reference and byte state, recycles physical offsets, removes an empty group,
and increments the owner revision. Existing detached filtered/sorted/index
snapshots remain stable. No per-entry Node, Cell or Entry is retained. No JNI
object references or Java predicates cross the existing native mask selector.
The direct loop is O(ordinal) or O(group length), unlink O(1), extra storage O(1).

The new root AGENTS paragraph binds future work and LLM authors to the single
M3JDK21 product and existing canonical storage owner. No actual `java.util`
ABI or HotSpot/JNI C entrypoint changes in this bounded pass.

## Source authority / tests

Canonical Synexia Maven/OpenRewrite named recipe: `com.synexia.m3.IndexedMultivalueOccurrence20261018`,
which reuses the **existing** `M3GpuToolJoinRecipe` and
`M3GpuToolJoinManifest` source-sealed scanner, not an alternative framework.
The exact source preimage is pinned at M3JDK21 master
`2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.
Inputs must be admitted before any output. The original class and AGENTS
source hashes are checked; new JUnit + standalone tests, catalogue, README
and workflow are admitted only from source-hash-sealed templates.
Fix the Synexia recipe/template, rerun its OpenRewrite SDK test, then
materialize exact product postimages in a separate serial target commit.

The focused oracle runs deterministic 3000 mixed-operation traces against
a JDK LinkedHashMap-of-ArrayList *reference model*, with null keys/values,
duplicate occurrences, replacement, group and slot recycling, state-bit
selection, detached snapshots, and opt-in Java/JNI selection parity using the
unchanged M3IndexMaskCompactor. It uses the **real** M3LinkedHashMap owner
in hosted selected-source Java21/Maven tests; no stand-in owner is admitted.

Current local gate: unexecuted (GitHub connector allows source publication
but local workspace cannot fetch the complete target repository/tooling jars).
Hosted exact-head workflow is the required authority; do NOT call queued tests
PASS. Whole-JDK/jtreg/JCK/HotSpot/JNI multi-platform packaging,
all five upstream API suites, comparative heap/CPU performance, and
String->arrays->collections stage promotion remain OPEN.

## Rights and provenance

Original and new M3 Java/test/recipe expression Copyright 2026 Hitesh Soliwal
and contributors, Apache-2.0. Historical JDK and third-party licences and
notices remain independent. No Guava/Eclipse/Apache/Spring/challenge code copied.
