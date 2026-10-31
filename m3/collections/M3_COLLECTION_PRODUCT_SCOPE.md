<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3JDK collection product scope

User requirement, 2026-10-12: all collection families and useful combinations belong in M3JDK, not only in the Synexia donor workspace. This document records the product destination and acceptance requirement. It is not evidence that every collection is implemented or shipped.

## Product ownership

**Synexia owns reusable convergence and recipes. M3JDK owns the delivered runtime.**

Use the existing porting/naming authority in `m3/docs/m3jdk21-porting-invariant.md`, the existing naming maps and collection owners. This supplements their collection scope; it does not replace them, create a second hierarchy, change licenses or advance the active STRING receiving phase.

There are two runtime delivery surfaces:

1. Existing public JDK collections keep their `java.util` / `java.util.concurrent` names and exact contracts. Qualified M3 backends may improve their implementation without changing their public identity.
2. Capabilities beyond the JDK surface belong in M3JDK-owned APIs and modules, reusing `com.m3.collections` and the target's established owners. A native M3 collection need not implement a JDK interface whose semantics do not fit it. Do not force every family into one universal mutable class or create another root merely because the name M3Collection is used for the programme.

No application should need a Synexia runtime jar, service, source checkout or build-tool dependency to use an admitted collection shipped by M3JDK. A separate package name does not mean a separate product destination. Packaging and module-resolution proof, however, are required before a separately built library is described as part of the installed JDK distribution.

## Open-ended capability scope

JDK, Apache, Eclipse Collections, Spring and Guava are donor/compatibility inputs, not the upper limit. The current family maps and 18-family work inventory are coverage floors, not closed enumerations.

Cover ordinary sequences, sets, bags and mappings; ordered/navigable and priority structures; multimaps, bidirectional mappings, tables and compound relations; graphs and dependency structures; range, prefix, spatial and temporal access; bounded queues and windows; immutable, persistent and versioned structures; compressed, dense, sparse, segmented, mapped and external-memory forms; and separately specified concurrent, reference-sensitive, approximate or replicated variants. These are illustrative families, not a claim that arbitrary combinations are already supported.

Extend the capability vocabulary when a new useful contract appears. Compose semantics, identity/equality, ordering, representation, mutation/concurrency, lifecycle, residency and result shape. Reject contradictory combinations explicitly; do not silently weaken a contract to make it fit an implementation. Approximate results must not masquerade as exact membership or counts.

## Combined and lightweight structures

Prefer one authoritative value store with selected indexes, typed views and reusable prepared facts. Examples include ID lookup plus priority/deadline order, forward/reverse relations, count bags plus rank queries, and canonical text matches combined with numeric/state selections. Preserve distinct record identities even when immutable values are shared.

Allocate only selected capabilities. Account separately for source payload, retained indexes, object overhead, mutation maintenance and peak workspace. An independent snapshot, concurrency/isolation boundary or persistence contract may require copies; document those copies rather than promise universal zero-copy behavior. No single structure, representation or JNI route is assumed fastest for every workload.

Precompute belongs inside the target runtime owner and must bind the relevant source generation and operation semantics. Evicting derived facts must not erase authoritative contents. Canonical M3 String/regex machinery remains its existing owner; collection integration must not flatten its representation, install another matcher/interner or retain unstable Java-heap addresses across JNI calls.

## Delivery is an installed-runtime property

A collection is delivered only when its exact target implementation, API/module entry points, dependencies, provenance and applicable native library are included in the intended M3JDK distribution and exercised from that installed image without a Synexia checkout or runtime dependency.

Track source candidate, target adaptation, build, contract tests, packaging, installed-image smoke tests, platform qualification and performance evidence separately. A catalogue row, copied vendor tree, passing donor test or merged PR alone proves none of the later stages. Preserve public APIs, ordering/views, exceptions, overflow, serialization, ownership and applicable concurrency guarantees. Keep cold preparation and warm reuse measurements separate.

Existing `m3/collections` is a separately built module exporting `com.m3.collections`; besides the implicit `java.base` dependency, its `module-info` requires `com.m3.arrays`. Its current documentation explicitly does not claim wholesale `java.base` replacement. This scope document does not change that status or certify packaging.

## Current handoff dispositions

- The established `m3/collections` module remains the existing target surface; preserve its owners, proofs, notices and naming map.
- Synexia combined prepared-index work at `53234675bd75a424c026224472881e919708fa5f` is a donor candidate. Its exact M3JDK owner mapping, target recipe, runtime/module tests and installed-image proof are **PENDING**; no class is mechanically copied or renamed by this document.
- The subsequent prepared-selection, bag, bitset and multimap work keeps its own recorded publication and qualification status. Unreferenced Git objects and local bundles are not promoted to received-runtime status.
- Full-family delivery is **PENDING**. The String -> arrays -> collections -> AST/compiler receiving order and existing admission gates remain unchanged.

Every collection continuation intended for M3JDK must end with a target PR and evidence, an explicit pending target mapping, or a justified higher-layer disposition. Do not leave qualified work only in Synexia and call M3JDK delivery complete.
