<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Adaptive clearing of reusable M3 linked slots

This draft receives exact Java postimages from the canonical Synexia recipe
[`com.synexia.m3.LinkedClearSwitch20261019`](https://github.com/hsoliwal/com.synexia/blob/7f89f1c8e358afc4567b2003be5cbf91bac5ab0d/synexia-openrewrite-recipes/crates/m3-linked-clear-switch-20261019/README.md), commit
`7f89f1c8e358afc4567b2003be5cbf91bac5ab0d`. Receiving base:
`a3e88976ee8664d1ef5d0cade96aec1206b2133d`. `source-handoff.tsv` seals the
before/after bytes. Synexia owns future edits; rerun its recipe instead of
independently modifying receiver copies.

M3LinkedList and M3LinkedHashMap clear only live payload slots when occupancy
is at most 1/64 of retained capacity. Dense clearing keeps the original bulk
reset. M3LinkedHashSet benefits through the existing key-only map owner.
The complete map bucket reset remains essential for removed-key tombstones.
There are no new fields, payload arrays or per-entry objects. Capacity is
retained for reuse; this is a reduction in sparse clearing work, not a reduction
in retained backing storage. Interfaces and existing thread-safety contracts
remain unchanged.

The generated M3LinkedClearContract covers nullable payloads, scattered live
slots, threshold boundaries, reversed/shared views, free-list reuse, map
access ordering, tombstones and key-only sets. Baseline and candidate passed
1,472,788 standalone behavioral checks each; Java 21 strict lint and protected/
public API comparison passed. Five real SDK tests cover materialization,
fixed point, refusals and the published descriptor. The clear contract also
passed interpreter, C1 and 64 MB SerialGC modes. Four existing cooperative
borrow tests passed; no reference-count implementation changed.

`BENCHMARKS.md` preserves both the complete 108-process diagnostic sweep and
the focused 36-process same-CPU follow-up, including slower cases and variance.
Sparse clearing improves in those diagnostics; no universal speed, heap
reduction, lock-free, JNI or small-device result is claimed. The owning product
Maven suite, full JDK build/jtreg/JCK, JMH and fleet-wide qualification are still
pending. The existing String -> arrays -> collections promotion sequence is
not advanced by this separately built Apache-2.0 collection candidate.

`OWNERSHIP_AND_SWITCHING.md` explains existing slot reuse, explicit cooperative
borrow counts, valid retirement boundaries, DOOP observations and JNI limits.
It is a custody copy of [the canonical document](https://github.com/hsoliwal/com.synexia/blob/7f89f1c8e358afc4567b2003be5cbf91bac5ab0d/synexia-openrewrite-recipes/crates/m3-linked-clear-switch-20261019/OWNERSHIP_AND_SWITCHING.md).
[Reproduction commands](https://github.com/hsoliwal/com.synexia/blob/7f89f1c8e358afc4567b2003be5cbf91bac5ab0d/synexia-openrewrite-recipes/crates/m3-linked-clear-switch-20261019/REPRODUCE.md),
[exact evidence archive](https://github.com/hsoliwal/com.synexia/blob/7f89f1c8e358afc4567b2003be5cbf91bac5ab0d/synexia-openrewrite-recipes/crates/m3-linked-clear-switch-20261019/evidence.tar.gz.b64),
[challenge shape decisions](https://github.com/hsoliwal/com.synexia/blob/7f89f1c8e358afc4567b2003be5cbf91bac5ab0d/synexia-openrewrite-recipes/crates/m3-linked-clear-switch-20261019/CHALLENGE_SHAPES.md) and
[LMAX/JCTools source review](https://github.com/hsoliwal/com.synexia/blob/7f89f1c8e358afc4567b2003be5cbf91bac5ab0d/synexia-openrewrite-recipes/crates/m3-linked-clear-switch-20261019/CONCURRENT_DONORS.md) remain canonical there.
No donor concurrency implementation is introduced by this receiving patch.
