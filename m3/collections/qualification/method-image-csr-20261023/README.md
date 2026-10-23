<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# Direct M3JDK21 method-level source signal image receiving

M3JDK21 **is the modified JDK itself**, not M3 + JDK, and this primitive-only
receiver is inside its existing first-party `m3/collections` source owner.
Canonical Maven/OpenRewrite transformations, source custody, comparison tests and donor
selection belong in the Synexia convergence workspace.

## Inventory
The existing `M3SourceSignalPlugin` javac plugin and its `M3MethodSignals` methodOut
producer already extract method-attributed source facts. Their `M3JDK21-METHOD-SIGNALS/1`
contract emits `M` rows (fourteen static counters), `C` rows (direct/dispatch-candidate
callsite records) and a SHA-256 trailer. They remain **unchanged**.

The existing `M3SignalColumns` retains typed `int[]`/`long[]` primitive lanes,
validity bits and provenance; `M3SignalRelations` already owns CSR int[] offsets, target
ordinals and edge multiplicities. No second parser, sorter, JNI export, AST plugin or
per-method Entry/Node graph store is introduced.

## Received atom
`M3MethodSignalImage` validates the original compiler's exact M/C v1 schema and
complete SHA-256 trailer, source ordering and uniqueness, canonical escaped method
identities, 14 nonnegative compiler counters, candidate edge ordering/classification,
positive multiplicities and bounded image size. It creates 17 typed primitive columns:
14 source counts and three known-incoming, known-outgoing and external-site long lanes.
Only **known source methods** become CSR graph nodes; nonlocal/unknown targets remain
separate **incomplete evidence**.

Method identities are packed into UTF-8 bytes plus int[] offsets. No Java object payloads
or native handles are retained per graph node. The output image includes the full
source SHA-256. Its long prefix is only an index hint; neither hash matches, source
call expression counts nor candidate dispatch certify semantic equality, dynamic call
frequency, purity, escape behavior, JIT benefit, or a complete call graph.

## Acceptance
The three product Java files are exact Git blob/SHA256 matches to the canonical Synexia
recipe. Strict local Java21 component compilation/tests have passed **96,014 assertions**
including randomized call graph and SHA256 refusal, but **not** full modified JDK acceptance.

Required independent gates: original javac compiler Plugin loaded under ServiceLoader and
real `methodOut`, unchanged type-level source image when `methodOut` is enabled, compiler
source/JUnit, original collections JUnit with source-vendored test/tool prerequisites,
actual canonical OpenRewrite first apply/zero-diff replay/drift refusal, javac/jtreg/JCK,
source-built modified JDK/HotSpot/JNI/GC/CDS, ABI/rights/platform/performance checks.

The global String → arrays → collections → AST/compiler receiving sequence remains locked.
This incremental source receiver alone does not close the full source-family inventory.
