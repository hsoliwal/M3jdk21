<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3 package responsibilities and contracts

[Guide](README.md) · [Projects](projects.md) · [Inventory](packages/README.md)

These 44 physical source roots have a source-declaration sample and a component-specific explanation. They include independent libraries, tooling, compatibility/custody and one unnamed source-gate root. Repeated namespace names at different roots are kept separate. This is partial semantic coverage, not 44 completed packages or runtime verification. Native and nonstandard source layouts also appear in the inventory.

| Namespace | Source root | Type files | Evidence / coverage |
| --- | --- | ---: | --- |
| [com.m3.algorithm](#package-1) | [m3/algorithms/src/com/m3/algorithm](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/algorithms/src/com/m3/algorithm) | 1 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.arrays](#package-2) | [m3/arrays/src/main/java/com/m3/arrays](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays/src/main/java/com/m3/arrays) | 7 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.collections](#package-3) | [m3/collections/src/main/java/com/m3/collections](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/src/main/java/com/m3/collections) | 76 | OBSERVED_SAMPLE / PARTIAL |
| [com.synexia.indexstring](#package-4) | [m3/compatibility/synexia-reference/src/com/synexia/indexstring](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/compatibility/synexia-reference/src/com/synexia/indexstring) | 4 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.text](#package-5) | [m3/core/src/com/m3/text](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/core/src/com/m3/text) | 17 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.indexdb](#package-6) | [m3/indexdb/src/main/java/com/m3/indexdb](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/main/java/com/m3/indexdb) | 8 | OBSERVED_SAMPLE / PARTIAL |
| [com.synexia.mindex.db](#package-7) | [m3/indexdb/src/main/java/com/synexia/mindex/db](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/main/java/com/synexia/mindex/db) | 1 | OBSERVED_SAMPLE / PARTIAL |
| [org.m3.runtime.lazy](#package-8) | [m3/lazyobjects/src/main/java/org/m3/runtime/lazy](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/lazyobjects/src/main/java/org/m3/runtime/lazy) | 1 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.migration](#package-9) | [m3/migration/src/com/m3/migration](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/migration/src/com/m3/migration) | 1 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.ds](#package-10) | [m3/ports/collections/src/main/java/com/m3/ds](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/main/java/com/m3/ds) | 1 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.util](#package-11) | [m3/ports/collections/src/main/java/com/m3/util](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/main/java/com/m3/util) | 6 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.compiletime](#package-12) | [m3/ports/compiletime/src/main/java/com/m3/compiletime](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/main/java/com/m3/compiletime) | 19 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.residency](#package-13) | [m3/ports/compiletime/src/main/java/com/m3/residency](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/main/java/com/m3/residency) | 3 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.text.compat](#package-14) | [m3/ports/indexstring/src/main/java/com/m3/text/compat](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/main/java/com/m3/text/compat) | 1 | OBSERVED_SAMPLE / PARTIAL |
| [com.synexia.indexstring](#package-15) | [m3/ports/indexstring/src/main/java/com/synexia/indexstring](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/main/java/com/synexia/indexstring) | 6 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.parallel.bulk](#package-16) | [m3/ports/parallel-bulk/src/main/java/com/m3/parallel/bulk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel-bulk/src/main/java/com/m3/parallel/bulk) | 2 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.parallel](#package-17) | [m3/ports/parallel/src/main/java/com/m3/parallel](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel) | 16 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.parallel.spi](#package-18) | [m3/ports/parallel/src/main/java/com/m3/parallel/spi](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel/spi) | 3 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.precompute](#package-19) | [m3/ports/precompute/src/main/java/com/m3/precompute](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/main/java/com/m3/precompute) | 23 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.swing.viewport](#package-20) | [m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport) | 21 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.swing.viewport.engine](#package-21) | [m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine) | 1 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.swing.viewport.engine.residency](#package-22) | [m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/residency](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/residency) | 10 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.swing.viewport.engine.swap](#package-23) | [m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/swap](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/swap) | 9 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.tornado](#package-24) | [m3/ports/tornadovm-bulk/src/main/java/com/m3/tornado](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/tornadovm-bulk/src/main/java/com/m3/tornado) | 1 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.vcpu](#package-25) | [m3/ports/vcpu/src/main/java/com/m3/vcpu](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/main/java/com/m3/vcpu) | 9 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.vcpu.internal](#package-26) | [m3/ports/vcpu/src/main/java/com/m3/vcpu/internal](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/main/java/com/m3/vcpu/internal) | 1 | OBSERVED_SAMPLE / PARTIAL |
| [(unnamed)](#package-27) | [m3/runtime-integration/src/main/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/runtime-integration/src/main/java) | 2 | OBSERVED_SAMPLE / PARTIAL |
| [com.synexia.rewrite](#package-28) | [m3/synexia-import/pure-int-recipe-custody/src/main/java/com/synexia/rewrite](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/pure-int-recipe-custody/src/main/java/com/synexia/rewrite) | 16 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.synexia.importer](#package-29) | [m3/synexia-import/src/main/java/com/m3/synexia/importer](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/src/main/java/com/m3/synexia/importer) | 10 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.a3](#package-30) | [m3/tooling/a3/src/main/java/com/m3/a3](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/a3/src/main/java/com/m3/a3) | 15 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.tooling.dag](#package-31) | [m3/tooling/backport-dag/src/main/java/com/m3/tooling/dag](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/backport-dag/src/main/java/com/m3/tooling/dag) | 25 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.rewrite.jmod](#package-32) | [m3/tooling/jmod-recipes/src/main/java/com/m3/rewrite/jmod](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/jmod-recipes/src/main/java/com/m3/rewrite/jmod) | 2 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.tooling.priority](#package-33) | [m3/tooling/lazy-priority-frontier/src/main/java/com/m3/tooling/priority](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/lazy-priority-frontier/src/main/java/com/m3/tooling/priority) | 1 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.rewrite](#package-34) | [m3/tooling/migration-recipes/src/main/java/com/m3/rewrite](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite) | 3 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.rewrite.a3](#package-35) | [m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/a3](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/a3) | 2 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.rewrite.backport](#package-36) | [m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport) | 60 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.rewrite.scope](#package-37) | [m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope) | 5 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.rewrite.semantic](#package-38) | [m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic) | 3 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.rewrite.synexia](#package-39) | [m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/synexia](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/synexia) | 1 | OBSERVED_SAMPLE / PARTIAL |
| [com.synexia.rewrite](#package-40) | [m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite) | 3 | OBSERVED_SAMPLE / PARTIAL |
| [com.synexia.rewrite.semantic](#package-41) | [m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/semantic](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/semantic) | 3 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.tools.modulepack](#package-42) | [m3/tooling/module-packs/kernel/src/main/java/com/m3/tools/modulepack](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/kernel/src/main/java/com/m3/tools/modulepack) | 4 | OBSERVED_SAMPLE / PARTIAL |
| [com.m3.rewrite.packs](#package-43) | [m3/tooling/module-packs/recipes/src/main/java/com/m3/rewrite/packs](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/recipes/src/main/java/com/m3/rewrite/packs) | 2 | OBSERVED_SAMPLE / PARTIAL |
| [com.synexia.rewrite.sealed](#package-44) | [m3/tooling/src/main/java/com/synexia/rewrite/sealed](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/src/main/java/com/synexia/rewrite/sealed) | 9 | OBSERVED_SAMPLE / PARTIAL |

## Deeper API reading routes

- [Text/storage API contracts](#textstorage-api-contracts): ownership, offsets, encoding, image I/O, failure and usage
- [Precompute API contracts](#precompute-api-contracts): metrics/signals, exact distance, workspace lifetime and progress

## Package contracts

<a id="package-1"></a>
### com.m3.algorithm

Exact prefix/Z facts over an existing M3StringPiece, without becoming another text owner.

- Ownership/source: [m3/algorithms/src/com/m3/algorithm](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/algorithms/src/com/m3/algorithm)
- Main navigation types (not all verified public entry points): [M3PrefixZ](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/algorithms/src/com/m3/algorithm/M3PrefixZ.java)
- Sampled contract: [M3PrefixZ.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/algorithms/src/com/m3/algorithm/M3PrefixZ.java); blob `65f259e23d818b9b35052feff37198f5ddf567b4`
- Direct-import evidence from that sample: com.m3.text, java.util.Objects, java.util.concurrent, java.util.function; full transitive/call graph remains unreviewed
- Build/project guide: [m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/README.md)
- Tests: no package-specific test root mapped in this pass; inspect the owning project/harness
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Exact UTF-16 prefix facts projected from an existing immutable M3 view. This class is not a text owner, interner, regex engine or String replacement. It does not flatten, copy, cache or retain the input's character payload. The Z-box algorithm performs O(n) character comparisons and uses one retained int[n] lane. Reads delegate to the existing owner; with its flat directory of s segments, the worst-case time is O(n log(s + 1)). The budget counts the int lane's payload only: 4*n bytes. It does NOT count JVM headers/alignment, this result object, or existing input storage. No claims of throughput improvement or allocation-free execution are made. Code units, including NUL and unpaired or cross-segment surrogate halves, are compared without normalization. All fields are immutable; there are no global caches. The input must be an existing sealed M3StringPiece value.

<a id="package-2"></a>
### com.m3.arrays

Immutable byte, UTF-16, int and long snapshot/range/composition values with explicit mutable-array export and optional JNI comparison/materialization.

- Ownership/source: [m3/arrays/src/main/java/com/m3/arrays](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays/src/main/java/com/m3/arrays)
- Main navigation types (not all verified public entry points): [M3ArrayNative](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays/src/main/java/com/m3/arrays/M3ArrayNative.java), [M3Arrays](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays/src/main/java/com/m3/arrays/M3Arrays.java), [M3ByteArrayView](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays/src/main/java/com/m3/arrays/M3ByteArrayView.java), [M3IntArrayView](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays/src/main/java/com/m3/arrays/M3IntArrayView.java), [M3LongArrayView](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays/src/main/java/com/m3/arrays/M3LongArrayView.java), [M3Utf16ArrayView](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays/src/main/java/com/m3/arrays/M3Utf16ArrayView.java) and 1 more source files
- Sampled contract: [M3ArrayNative.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays/src/main/java/com/m3/arrays/M3ArrayNative.java); blob `440694e15ba340e0e98d669269beed32f09c31da`
- Direct-import evidence from that sample: java.nio.file, java.util.Objects; full transitive/call graph remains unreviewed
- Build/project guide: [m3/arrays/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays/README.md)
- Tests: [m3/arrays/src/test/java/com/m3/arrays](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays/src/test/java/com/m3/arrays) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Optional JNI materialization/compare boundary for ordinary Java UTF-16 arrays. No native pointer is retained after a call. Absence of the library falls back to exact Java behavior; native acceleration never owns array identity or correctness.

<a id="package-3"></a>
### com.m3.collections

Indexed collection and storage proving ground: primitive lanes, sparse slots, maps/sets, queues, progress and parent-owned residency. Each concrete owner has its own order, null, lifetime and concurrency contract.

- Ownership/source: [m3/collections/src/main/java/com/m3/collections](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/src/main/java/com/m3/collections)
- Main navigation types (not all verified public entry points): [M3AdaptiveEnumLongMap](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/src/main/java/com/m3/collections/M3AdaptiveEnumLongMap.java), [M3Address28](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/src/main/java/com/m3/collections/M3Address28.java), [M3BitLane28](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/src/main/java/com/m3/collections/M3BitLane28.java), [M3BlockingDeque](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/src/main/java/com/m3/collections/M3BlockingDeque.java), [M3ChildProgressMonitor](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/src/main/java/com/m3/collections/M3ChildProgressMonitor.java), [M3Collections](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/src/main/java/com/m3/collections/M3Collections.java) and 70 more source files
- Sampled contract: [M3AdaptiveEnumLongMap.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/src/main/java/com/m3/collections/M3AdaptiveEnumLongMap.java); blob `67a1bf136c21ab2b347f0a33ec4784e17c0eb245`
- Direct-import evidence from that sample: java.util.NoSuchElementException, java.util.Objects; full transitive/call graph remains unreviewed
- Build/project guide: [m3/collections/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/README.md)
- Tests: [m3/collections/src/test/java/com/m3/collections](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/src/test/java/com/m3/collections) (located, not executed)
- Inline package documentation: [m3/collections/src/main/java/com/m3/collections/package-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/src/main/java/com/m3/collections/package-info.java)
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Enum-keyed primitive-long map that hashes ordinals while sparse and switches to the dense ordinal-indexed M3EnumLongMap once the fixed domain is the cheaper representation. This is the adaptive variant deferred from M3EnumLongMap, adapted from the first-party Synexia donor com.synexia.primitives.AdaptiveEnumPrimitiveMap onto the single long value lane of the proving ground. The backing is chosen by retained primitive bytes: n mappings stay sparse in an M3IntLongHashMap while payloadBytesFor(n) is below #directPayloadBytes(), and the insertion that reaches #directSwitchSize() migrates every mapping to the dense lane. Removal never migrates implicitly; #compact() re-applies the rule to the current size and shrinks a sparse table. Neither mode retains a key reference or a boxed value per mapping. The external contract is that of M3EnumLongMap: ordinal iteration order in both modes (sparse traversal walks the ordinal domain and probes each constant, so it allocates nothing and costs O(keyCapacity)), ClassCastException for a key of another enum type and NullPointerException for a null key on put, and absence for both on reads and removals. Not a java.util.EnumMap replacement; java.util.EnumMap remains the public contract owner.

<a id="package-4"></a>
### com.synexia.indexstring

Pinned immutable heap/mapped storage and joined-view compatibility owners. Preserve original names/provenance and avoid duplicate class definitions on a single loader path.

- Ownership/source: [m3/compatibility/synexia-reference/src/com/synexia/indexstring](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/compatibility/synexia-reference/src/com/synexia/indexstring)
- Main navigation types (not all verified public entry points): [FrozenBytes](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/compatibility/synexia-reference/src/com/synexia/indexstring/FrozenBytes.java), [FrozenChars](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/compatibility/synexia-reference/src/com/synexia/indexstring/FrozenChars.java), [MIndexAddressedView](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/compatibility/synexia-reference/src/com/synexia/indexstring/MIndexAddressedView.java), [SynexiaMappedStorageTest](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/compatibility/synexia-reference/src/com/synexia/indexstring/SynexiaMappedStorageTest.java)
- Sampled contract: [FrozenBytes.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/compatibility/synexia-reference/src/com/synexia/indexstring/FrozenBytes.java); blob `0e249745d0e96a1d9a8754a075f33cdf42e8fc0b`
- Direct-import evidence from that sample: java.nio.ByteBuffer, java.util.Arrays, java.util.Objects; full transitive/call graph remains unreviewed
- Build/project guide: [m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/README.md)
- Tests: no package-specific test root mapped in this pass; inspect the owning project/harness
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Immutable heap or mapped byte storage. Mutable callers enter through copyOf.

<a id="package-5"></a>
### com.m3.text

Independent immutable text foundation: local pieces, joined leaf directories, storage identities and lexicon/precompute image readers. This is a separately built module, not java.lang.String.

- Ownership/source: [m3/core/src/com/m3/text](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/core/src/com/m3/text)
- Main navigation types (not all verified public entry points): [JoinedM3StringPiece](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/core/src/com/m3/text/JoinedM3StringPiece.java), [LexiconTool](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/core/src/com/m3/text/LexiconTool.java), [LocalM3Arena](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/core/src/com/m3/text/LocalM3Arena.java), [LocalM3StringPiece](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/core/src/com/m3/text/LocalM3StringPiece.java), [M3InstanceIndexPrecompute](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/core/src/com/m3/text/M3InstanceIndexPrecompute.java), [M3LanguageGrammarSupport](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/core/src/com/m3/text/M3LanguageGrammarSupport.java) and 11 more source files
- Sampled contract: [JoinedM3StringPiece.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/core/src/com/m3/text/JoinedM3StringPiece.java); blob `cc3c84f5ea0230e2f57e3d3ae0f5d29b9241e597`
- Direct-import evidence from that sample: java.util.Objects; full transitive/call graph remains unreviewed
- Build/project guide: [m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/README.md)
- Tests: no package-specific test root mapped in this pass; inspect the owning project/harness
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Flat leaf directory avoids recursive traversal or rope-depth amplification.

<a id="package-6"></a>
### com.m3.indexdb

Content-addressed artifact storage and named references. The sampled M3IndexDB owner writes filesystem blobs and references; it is not evidence of complete mapped coordinate-table storage.

- Ownership/source: [m3/indexdb/src/main/java/com/m3/indexdb](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/main/java/com/m3/indexdb)
- Main navigation types (not all verified public entry points): [M3IndexDB](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDB.java), [M3IndexDbArtifact](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbArtifact.java), [M3IndexDbSemanticCodec](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbSemanticCodec.java), [M3IndexDbSemanticEdge](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbSemanticEdge.java), [M3IndexDbSemanticFingerprint](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbSemanticFingerprint.java), [M3IndexDbSemanticIndex](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbSemanticIndex.java) and 2 more source files
- Sampled contract: [M3IndexDB.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDB.java); blob `61e913815a3dc8d94280a765cc050f2e75c9c040`
- Direct-import evidence from that sample: java.io.IOException, java.nio.charset, java.nio.file, java.security.MessageDigest, java.security.NoSuchAlgorithmException, java.util.HexFormat, java.util.List, java.util.Objects, java.util.stream; full transitive/call graph remains unreviewed
- Build/project guide: [m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/README.md)
- Tests: [m3/indexdb/src/test/java/com/m3/indexdb](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/test/java/com/m3/indexdb) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> M3-owned native content-addressed artifact store. Payloads are stored once by SHA-256 under blobs/; named artifacts are tiny immutable refs under refs/. This is the first inlined M3IndexDB storage atom. Coordinate-table mmap storage is a later donor-inline pass and does not require changing this API.

<a id="package-7"></a>
### com.synexia.mindex.db

Historical structural/logic fingerprint compatibility algebra retained by the independent index database receiver. Similarity hashes do not prove semantic equivalence.

- Ownership/source: [m3/indexdb/src/main/java/com/synexia/mindex/db](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/main/java/com/synexia/mindex/db)
- Main navigation types (not all verified public entry points): [M3IndexDbFingerprintV1](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/main/java/com/synexia/mindex/db/M3IndexDbFingerprintV1.java)
- Sampled contract: [M3IndexDbFingerprintV1.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/main/java/com/synexia/mindex/db/M3IndexDbFingerprintV1.java); blob `a51449e205016cfc01fb0ccbdb70e0038539b276`
- Direct-import evidence from that sample: java.nio.charset, java.security.MessageDigest, java.security.NoSuchAlgorithmException, java.util.ArrayList, java.util.HexFormat, java.util.List, java.util.Objects; full transitive/call graph remains unreviewed
- Build/project guide: [m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/README.md)
- Tests: [m3/indexdb/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/test/java) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Historical M3IndexDB V1 structural/logic/SimHash fingerprint compatibility algebra. Structural identity is deliberately separate from normalized logic identity. Role and child order participate in parent composition. SimHash is a similarity signal only and never proves equivalence.

<a id="package-8"></a>
### org.m3.runtime.lazy

Opt-in parent-owned logical handles, lazy recreation, pin leases and optional scratch-file swapping. The adapter does not preserve raw Java object identity through eviction.

- Ownership/source: [m3/lazyobjects/src/main/java/org/m3/runtime/lazy](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/lazyobjects/src/main/java/org/m3/runtime/lazy)
- Main navigation types (not all verified public entry points): [LazyObjectParent](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/lazyobjects/src/main/java/org/m3/runtime/lazy/LazyObjectParent.java)
- Sampled contract: [LazyObjectParent.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/lazyobjects/src/main/java/org/m3/runtime/lazy/LazyObjectParent.java); blob `6d00b8d4fa4b404fd26e50dd82617a19c23a1bbd`
- Direct-import evidence from that sample: java.io.IOException, java.nio.file, java.util.HashMap, java.util.Map, java.util.Objects, java.util.function; full transitive/call graph remains unreviewed
- Build/project guide: [m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/README.md)
- Tests: no package-specific test root mapped in this pass; inspect the owning project/harness
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Parent-owned, logical handles for transient state. All operations are serialized by the parent monitor. Handles, rather than raw objects, must escape the parent. This is an opt-in research adapter, not a replacement for JVM object identity.

<a id="package-9"></a>
### com.m3.migration

Source-bound additive file recipes and rollback receipts. Hash guards and cooperating locks do not make the multi-file process crash-atomic.

- Ownership/source: [m3/migration/src/com/m3/migration](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/migration/src/com/m3/migration)
- Main navigation types (not all verified public entry points): [ExactFileRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/migration/src/com/m3/migration/ExactFileRecipe.java)
- Sampled contract: [ExactFileRecipe.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/migration/src/com/m3/migration/ExactFileRecipe.java); blob `918f41259c0c623bb88287c44c817070a58fd9fe`
- Direct-import evidence from that sample: java.io.ByteArrayOutputStream, java.io.IOException, java.nio.ByteBuffer, java.nio.channels, java.nio.charset, java.nio.file, java.security.MessageDigest, java.security.NoSuchAlgorithmException, java.util.ArrayList, java.util.HashSet, java.util.HexFormat, java.util.List, java.util.Set, java.util.TreeSet; full transitive/call graph remains unreviewed
- Build/project guide: [m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/README.md)
- Tests: [m3/migration/test](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/migration/test) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Dependency-free, Maven-executable source-bound additive recipe. Reviewed postimages, not blind renames, define the transformation. Every source, target guard, snapshot and destination is checked before writes. No existing destination is overwritten. Cooperating invocations use a lock. This is NOT a cross-file filesystem transaction. A crash may leave completed files or a partial new file: complete equal files replay safely; partial/drifted files require review. Rollback removes only receipt-owned, still-equal additions. A crash after file creation but before recording ownership may leave an unowned file, which rollback conservatively preserves. Non-cooperating concurrent filesystem mutation is unsupported. No provenance signature is implied.

<a id="package-10"></a>
### com.m3.ds

Packed primitive Fenwick prefix/range calculations with exact arithmetic and failure-atomic point mutation in the sampled owner.

- Ownership/source: [m3/ports/collections/src/main/java/com/m3/ds](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/main/java/com/m3/ds)
- Main navigation types (not all verified public entry points): [M3LongFenwickTree](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/main/java/com/m3/ds/M3LongFenwickTree.java)
- Sampled contract: [M3LongFenwickTree.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/main/java/com/m3/ds/M3LongFenwickTree.java); blob `31a1d9314560e2f48b3160e8fb797d6ddc156f92`
- Direct-import evidence from that sample: java.util.Arrays, java.util.Objects; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/collections/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/README.md)
- Tests: [m3/ports/collections/src/test/java/com/m3/ds](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/test/java/com/m3/ds) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Primitive Fenwick tree with exact arithmetic and failure-atomic point mutation. Retained state is two long lanes plus fixed O(log n) scratch. Construction from values is O(n). Prefix/range/add/set are O(log n). No stream, wrapper or per-operation array allocation is required after construction.

<a id="package-11"></a>
### com.m3.util

Portable primitive collection facades and their dense-hash implementation support. These explicit M3 APIs are not blanket java.util replacements.

- Ownership/source: [m3/ports/collections/src/main/java/com/m3/util](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/main/java/com/m3/util)
- Main navigation types (not all verified public entry points): [M3DenseHash](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/main/java/com/m3/util/M3DenseHash.java), [M3IntArrayDeque](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java), [M3PrimitiveArrayDeque](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java), [M3PrimitiveArrayList](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java), [M3PrimitiveKind](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveKind.java), [M3PrimitiveMinMaxQueue](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java)
- Sampled contract: [M3DenseHash.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/main/java/com/m3/util/M3DenseHash.java); blob `6fc4b0aedb977799b1e751416b6eb51c2fa977b6`
- Direct-import evidence from that sample: No explicit imports in the sampled source; implicit/same-package dependencies remain; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/collections/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/README.md)
- Tests: [m3/ports/collections/src/test/java/com/m3/util](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/test/java/com/m3/util) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled internal source has no leading type Javadoc. Its name is not evidence of a complete package contract.

<a id="package-12"></a>
### com.m3.compiletime

Bytecode/constant String inventory and bounded regex-image preparation. ASM analysis and image tooling live outside the java.base bootstrap path.

- Ownership/source: [m3/ports/compiletime/src/main/java/com/m3/compiletime](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/main/java/com/m3/compiletime)
- Main navigation types (not all verified public entry points): [M3BytecodeOffsets](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3BytecodeOffsets.java), [M3CallSiteCatalog](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3CallSiteCatalog.java), [M3ClassScan](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ClassScan.java), [M3CompileTimeStringInventory](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3CompileTimeStringInventory.java), [M3ConstantRegexDifferential](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexDifferential.java), [M3ConstantRegexGate](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexGate.java) and 13 more source files
- Sampled contract: [M3BytecodeOffsets.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3BytecodeOffsets.java); blob `4cd6f079f22c1800571054aefb6481a478758b36`
- Direct-import evidence from that sample: java.util.Arrays, java.util.HashMap, java.util.Map, org.objectweb.asm; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/compiletime/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/README.md)
- Tests: [m3/ports/compiletime/src/test/java/com/m3/compiletime](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/test/java/com/m3/compiletime) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Exact bytecode offset of every instruction, per method (name + desc), decoded from the raw Code attribute (the ASM tree API keeps instruction order but not offsets). The k-th real ASM instruction of a method is the k-th decoded instruction.

<a id="package-13"></a>
### com.m3.residency

CLASS-retained annotations used by build-time transient-ownership analysis; annotation presence alone does not establish a runtime lifetime proof.

- Ownership/source: [m3/ports/compiletime/src/main/java/com/m3/residency](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/main/java/com/m3/residency)
- Main navigation types (not all verified public entry points): [M3BehaviourOnly](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/main/java/com/m3/residency/M3BehaviourOnly.java), [M3Retains](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/main/java/com/m3/residency/M3Retains.java), [M3Transient](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/main/java/com/m3/residency/M3Transient.java)
- Sampled contract: [M3BehaviourOnly.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/main/java/com/m3/residency/M3BehaviourOnly.java); blob `415620e147bcf00f92592f5c5aec52040e01b5f6`
- Direct-import evidence from that sample: java.lang.annotation; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/compiletime/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/README.md)
- Tests: [m3/ports/compiletime/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/test/java) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> The annotated method (or parameter) uses its transient argument only during the call, so passing a transient handle to it does not escape. CLASS retention: visible to the build-time ASM pass (m3/docs/m3-item-residency-metadata.md), absent at run time.

<a id="package-14"></a>
### com.m3.text.compat

M3Text is a stock-JVM retained-view facade over the isolated IndexString owners. It intentionally has different cross-type equality from String and materializes at explicit export boundaries.

- Ownership/source: [m3/ports/indexstring/src/main/java/com/m3/text/compat](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/main/java/com/m3/text/compat)
- Main navigation types (not all verified public entry points): [M3Text](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/main/java/com/m3/text/compat/M3Text.java)
- Sampled contract: [M3Text.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/main/java/com/m3/text/compat/M3Text.java); blob `46aecc2fb0b1e5f516c1db8b5e93476eb70444f2`
- Direct-import evidence from that sample: com.synexia.indexstring, java.io.IOException, java.io.Reader, java.nio.ByteBuffer, java.nio.CharBuffer, java.nio.charset, java.util.NoSuchElementException, java.util.Objects, java.util.PrimitiveIterator, java.util.Spliterator, java.util.Spliterators, java.util.function, java.util.regex, java.util.stream; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/indexstring/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/README.md)
- Tests: [m3/ports/indexstring/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/test/java) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Immutable M3-named facade over the existing IndexString descriptor owner. This class owns no character/byte payload, dictionary, interner, or native arena. It is an explicit stock-JVM view, not java.lang.String or String.intern(). Equal M3Text values have String-compatible UTF-16 hashes; equality with String is intentionally false in both directions. Use contentEquals at that boundary.

<a id="package-15"></a>
### com.synexia.indexstring

Pinned immutable heap/mapped storage and joined-view compatibility owners. Preserve original names/provenance and avoid duplicate class definitions on a single loader path.

- Ownership/source: [m3/ports/indexstring/src/main/java/com/synexia/indexstring](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/main/java/com/synexia/indexstring)
- Main navigation types (not all verified public entry points): [FrozenBytes](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/main/java/com/synexia/indexstring/FrozenBytes.java), [FrozenChars](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/main/java/com/synexia/indexstring/FrozenChars.java), [MIndexJoinedBytes](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/main/java/com/synexia/indexstring/MIndexJoinedBytes.java), [MIndexJoinedChars](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/main/java/com/synexia/indexstring/MIndexJoinedChars.java), [MIndexJoinedStorageIntern](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/main/java/com/synexia/indexstring/MIndexJoinedStorageIntern.java), [MIndexJoinedStreams](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/main/java/com/synexia/indexstring/MIndexJoinedStreams.java)
- Sampled contract: [FrozenBytes.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/main/java/com/synexia/indexstring/FrozenBytes.java); blob `b624dc8dbb23eac6b3147ea888ac69df60f971e1`
- Direct-import evidence from that sample: java.nio.ByteBuffer, java.util.Arrays, java.util.Objects; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/indexstring/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/README.md)
- Tests: [m3/ports/indexstring/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/test/java) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Immutable heap or mapped byte storage. Mutable callers enter through copyOf.

<a id="package-16"></a>
### com.m3.parallel.bulk

Optional adapters from JDK-internal bulk tasks to the independent parallel fabric. Source contains both integer-add and bit-count providers; the older README describes only the initial integer-add slice.

- Ownership/source: [m3/ports/parallel-bulk/src/main/java/com/m3/parallel/bulk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel-bulk/src/main/java/com/m3/parallel/bulk)
- Main navigation types (not all verified public entry points): [M3ParallelBitCountProvider](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel-bulk/src/main/java/com/m3/parallel/bulk/M3ParallelBitCountProvider.java), [M3ParallelIntAddProvider](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel-bulk/src/main/java/com/m3/parallel/bulk/M3ParallelIntAddProvider.java)
- Sampled contract: [M3ParallelBitCountProvider.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel-bulk/src/main/java/com/m3/parallel/bulk/M3ParallelBitCountProvider.java); blob `58438ee52696b5435292e5afb361b4feb1768e94`
- Direct-import evidence from that sample: com.m3.parallel, java.util.List, java.util.Map, java.util.Objects, java.util.Set, jdk.internal.vm; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/parallel-bulk/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel-bulk/README.md)
- Tests: [m3/ports/parallel-bulk/src/test/java/com/m3/parallel/bulk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel-bulk/src/test/java/com/m3/parallel/bulk) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Bounded one-kernel bridge for independent 32-bit population counts. The operation computes Integer#bitCount(int) for every signed int input, including negative values interpreted as 32-bit two's-complement patterns. It neither mutates the input nor infers source semantics from a bitmap count. The CPU reference remains the exact oracle, and explicit accelerator requests cannot silently fall back.

<a id="package-17"></a>
### com.m3.parallel

Named one-dimensional kernel plans over off-heap int buffers, with a Java reference backend and a full-buffer comparison path. Availability and accelerated performance require separate provider evidence.

- Ownership/source: [m3/ports/parallel/src/main/java/com/m3/parallel](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel)
- Main navigation types (not all verified public entry points): [Backend](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel/Backend.java), [BackendRegistry](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel/BackendRegistry.java), [Digests](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel/Digests.java), [Grid1D](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel/Grid1D.java), [IntBuffer](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel/IntBuffer.java), [JavaKernel](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel/JavaKernel.java) and 10 more source files
- Sampled contract: [Backend.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel/Backend.java); blob `04b3bdce69d7ac5afe68482509b4e8843c092dde`
- Direct-import evidence from that sample: com.m3.parallel, java.util.Collections, java.util.EnumSet, java.util.Map, java.util.Objects, java.util.Set; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/parallel/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/README.md)
- Tests: [m3/ports/parallel/src/test/java/com/m3/parallel](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/test/java/com/m3/parallel) (located, not executed)
- Inline package documentation: [m3/ports/parallel/src/main/java/com/m3/parallel/package-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel/package-info.java)
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Handle of an execution backend. The pure-Java CPU reference ({@value #REFERENCE_ID}) is always present. Other backends are discovered once per process through java.util.ServiceLoader and BackendProvider; a provider whose probe fails, throws, or whose id is invalid, reserved or ambiguous is recorded as unavailable and never selected. Backends are compared by identity; there is one instance per id.

<a id="package-18"></a>
### com.m3.parallel.spi

ServiceLoader backend-provider boundary for probe, allocation/preparation, upload, launch, download and close. The interface is the provider contract, not proof that every backend exists.

- Ownership/source: [m3/ports/parallel/src/main/java/com/m3/parallel/spi](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel/spi)
- Main navigation types (not all verified public entry points): [BackendProvider](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel/spi/BackendProvider.java), [DeviceProgram](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel/spi/DeviceProgram.java), [Probe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel/spi/Probe.java)
- Sampled contract: [BackendProvider.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel/spi/BackendProvider.java); blob `880ab9dad7783bd6ea5964fbe68f437ac83108dd`
- Direct-import evidence from that sample: com.m3.parallel, java.util.Set; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/parallel/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/README.md)
- Tests: [m3/ports/parallel/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/test/java) (located, not executed)
- Inline package documentation: [m3/ports/parallel/src/main/java/com/m3/parallel/spi/package-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/main/java/com/m3/parallel/spi/package-info.java)
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Service-provider interface of a backend, discovered with java.util.ServiceLoader. A provider is a stateless factory. #probe() must never crash the caller: a missing driver or runtime is reported as Probe#unavailable(String). Ids are lower-case ([a-z0-9][a-z0-9._-]{0,63}); the id {@value com.m3.parallel.Backend#REFERENCE_ID} is reserved for the built-in CPU reference.

<a id="package-19"></a>
### com.m3.precompute

Broader text geometry, lexical signals, regex shape, bounded edit-distance and Java/optional-native batch providers. Approximate signals nominate candidates; they do not decide exact String or regex semantics.

- Ownership/source: [m3/ports/precompute/src/main/java/com/m3/precompute](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/main/java/com/m3/precompute)
- Main navigation types (not all verified public entry points): [M3CodeTextSignalBatch](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignalBatch.java), [M3CodeTextSignalBatchJava](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignalBatchJava.java), [M3CodeTextSignalBatchNative](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignalBatchNative.java), [M3CodeTextSignals](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignals.java), [M3CpuToolImage](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/main/java/com/m3/precompute/M3CpuToolImage.java), [M3CpuToolImageCompiler](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/main/java/com/m3/precompute/M3CpuToolImageCompiler.java) and 17 more source files
- Sampled contract: [M3CodeTextSignalBatch.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignalBatch.java); blob `b10bd333c74d9c0ae7ded451831aaab645e1117a`
- Direct-import evidence from that sample: No explicit imports in the sampled source; implicit/same-package dependencies remain; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/precompute/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/README.md)
- Tests: [m3/ports/precompute/src/test/java/com/m3/precompute](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/test/java/com/m3/precompute) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Batch UTF-16 lexical signal extractor for code-looking and regex-looking text.

<a id="package-20"></a>
### com.m3.swing.viewport

Packed Swing list/table/tree models, selection and row proxies with explicit viewport binding. UI-thread and identity behavior remain receiver-specific contracts.

- Ownership/source: [m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport)
- Main navigation types (not all verified public entry points): [M3Chars](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/M3Chars.java), [M3PackedCellRenderer](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/M3PackedCellRenderer.java), [M3PackedCells](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/M3PackedCells.java), [M3PackedColumn](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/M3PackedColumn.java), [M3PackedList](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/M3PackedList.java), [M3PackedListModel](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/M3PackedListModel.java) and 15 more source files
- Sampled contract: [M3Chars.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/M3Chars.java); blob `4c24810b3fb91abb3ee74a32c3c1a8c50e667c56`
- Direct-import evidence from that sample: No explicit imports in the sampled source; implicit/same-package dependencies remain; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/swing-viewport/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/README.md)
- Tests: [m3/ports/swing-viewport/src/test/java/com/m3/swing/viewport](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/test/java/com/m3/swing/viewport) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Allocation-free text into a caller's reusable char[]. Every method returns the length written, or -(needed) - 1 when the buffer is too small (the caller grows and retries).

<a id="package-21"></a>
### com.m3.swing.viewport.engine

Shared primitive capacity-growth policy for packed arrays. The sampled owner considers heap headroom only when growing; it does not reject a required allocation or force GC.

- Ownership/source: [m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine)
- Main navigation types (not all verified public entry points): [PackedHeapGrowth](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/PackedHeapGrowth.java)
- Sampled contract: [PackedHeapGrowth.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/PackedHeapGrowth.java); blob `00d1896409671284fb7a56ba045409b6cd3d2724`
- Direct-import evidence from that sample: No explicit imports in the sampled source; implicit/same-package dependencies remain; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/swing-viewport/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/README.md)
- Tests: [m3/ports/swing-viewport/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/test/java) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Heap-aware capacity choice for replacement primitive arrays and packed row tables (any positive row width; the Synexia original admits 1, 2, 4 or 8). Call it only on a capacity miss: it samples Runtime.maxMemory() (Xmx) and used heap once per growth, never per element. Under headroom pressure it returns the exact required length instead of geometric slack; it never rejects a required allocation, changes logical size or forces GC. Callers keep their add/append APIs; nothing is asked of the programmer. Web (GWT/RAP) builds have no Runtime.maxMemory(); a super-source variant returns the geometric capacity there.

<a id="package-22"></a>
### com.m3.swing.viewport.engine.residency

Toolkit-neutral item directories, bit state, sparse tables, requests and slot/residency bookkeeping used by the Swing receiver.

- Ownership/source: [m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/residency](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/residency)
- Main navigation types (not all verified public entry points): [ItemBitSet](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/residency/ItemBitSet.java), [ItemDirectory](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/residency/ItemDirectory.java), [ItemLanes](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/residency/ItemLanes.java), [ItemRequests](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/residency/ItemRequests.java), [ItemResidency](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/residency/ItemResidency.java), [ItemSlots](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/residency/ItemSlots.java) and 4 more source files
- Sampled contract: [ItemBitSet.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/residency/ItemBitSet.java); blob `4ff039c92d6707a1b932e0bdf46443d0c76f6cad`
- Direct-import evidence from that sample: com.m3.swing, java.util.Arrays; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/swing-viewport/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/README.md)
- Tests: [m3/ports/swing-viewport/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/test/java) (located, not executed)
- Inline package documentation: [m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/residency/package-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/residency/package-info.java)
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Sparse bit set over non-negative int positions in RoaringBitmap-style containers (container idea adapted from RoaringBitmap, Apache-2.0): sorted 65536-position chunks, each an ARRAY of sparse positions, a RUN list (long runs such as select-all) or a BITMAP (dense pages). Memory is O(set state), not O(positions). int only (GWT).

<a id="package-23"></a>
### com.m3.swing.viewport.engine.swap

Session-scoped swap storage and binary-operation providers. File-backed records are scratch residency, not a durable database; stale IDs and conflicting writes have refusal semantics.

- Ownership/source: [m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/swap](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/swap)
- Main navigation types (not all verified public entry points): [FileSwapStore](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/swap/FileSwapStore.java), [JavaSwapPageBinaryOps](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/swap/JavaSwapPageBinaryOps.java), [ParityCheckedSwapPageBinaryOps](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/swap/ParityCheckedSwapPageBinaryOps.java), [SwapBudgetPlane](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/swap/SwapBudgetPlane.java), [SwapPageBinaryOps](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/swap/SwapPageBinaryOps.java), [SwapPageBinaryOpsSelector](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/swap/SwapPageBinaryOpsSelector.java) and 3 more source files
- Sampled contract: [FileSwapStore.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/swap/FileSwapStore.java); blob `e7721e143a967d47a8960658e4c9f76f475e48b4`
- Direct-import evidence from that sample: com.m3.swing, java.io., java.nio., java.nio.channels, java.nio.file, java.util.; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/swing-viewport/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/README.md)
- Tests: [m3/ports/swing-viewport/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/test/java) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Java SwapStore for desktop SWT: the fail-closed backend when the Synexia JNI SQLite store is absent (SWT ships no native swap library). Session-scoped: one delete-on-close temporary file written with positional FileChannel I/O; the OS page cache is the second-level cache. Record bytes move through SwapPageBinaryOps#copy (Java ops, or a native candidate under Java parity checking, see SwapPageBinaryOpsSelector). Ids come from #reserve(int): (sequence

<a id="package-24"></a>
### com.m3.tornado

Optional RV32IM bulk adapter to the separately supplied TornadoVM provider. This dependency is outside java.base and is not enabled by the default control reactor.

- Ownership/source: [m3/ports/tornadovm-bulk/src/main/java/com/m3/tornado](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/tornadovm-bulk/src/main/java/com/m3/tornado)
- Main navigation types (not all verified public entry points): [Rv32iBulkExecutorAdapter](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/tornadovm-bulk/src/main/java/com/m3/tornado/Rv32iBulkExecutorAdapter.java)
- Sampled contract: [Rv32iBulkExecutorAdapter.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/tornadovm-bulk/src/main/java/com/m3/tornado/Rv32iBulkExecutorAdapter.java); blob `47b4977901041d6d76c92a34104318f591189ed6`
- Direct-import evidence from that sample: com.synexia.m3, java.util.List, java.util.Map, java.util.Objects, java.util.Set, jdk.internal.vm; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/tornadovm-bulk/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/tornadovm-bulk/README.md)
- Tests: [m3/ports/tornadovm-bulk/src/test/java/com/m3/tornado](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/tornadovm-bulk/src/test/java/com/m3/tornado) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Optional M3JDK adapter from the internal bulk scheduler contract to the verified TornadoVM RV32IM provider. The input batch is never mutated; execution happens on a private copy.

<a id="package-25"></a>
### com.m3.vcpu

RV32IM virtual CPU state, sequential oracle, kernel, session and receipt API over m3.parallel. Device-specific qualification is separate from the existence of the Java reference.

- Ownership/source: [m3/ports/vcpu/src/main/java/com/m3/vcpu](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/main/java/com/m3/vcpu)
- Main navigation types (not all verified public entry points): [CpuRv32iEngine](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/main/java/com/m3/vcpu/CpuRv32iEngine.java), [Elf32RiscVLoader](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/main/java/com/m3/vcpu/Elf32RiscVLoader.java), [Rv32i](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/main/java/com/m3/vcpu/Rv32i.java), [Rv32iAssembler](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/main/java/com/m3/vcpu/Rv32iAssembler.java), [Rv32iBatch](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/main/java/com/m3/vcpu/Rv32iBatch.java), [Rv32iBulkExecutor](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/main/java/com/m3/vcpu/Rv32iBulkExecutor.java) and 3 more source files
- Sampled contract: [CpuRv32iEngine.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/main/java/com/m3/vcpu/CpuRv32iEngine.java); blob `f969ebc89b9d5334a13a9916f55d34e8f1afe521`
- Direct-import evidence from that sample: No explicit imports in the sampled source; implicit/same-package dependencies remain; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/vcpu/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/README.md)
- Tests: [m3/ports/vcpu/src/test/java/com/m3/vcpu](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/test/java/com/m3/vcpu) (located, not executed)
- Inline package documentation: [m3/ports/vcpu/src/main/java/com/m3/vcpu/package-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/main/java/com/m3/vcpu/package-info.java)
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Independent heap-based RV32IM interpreter used as the correctness oracle for GPU execution.

<a id="package-26"></a>
### com.m3.vcpu.internal

JNI bindings for the native virtual-CPU HAL. A native session owns its buffers; loader configuration and mapped failure exceptions are part of this internal boundary.

- Ownership/source: [m3/ports/vcpu/src/main/java/com/m3/vcpu/internal](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/main/java/com/m3/vcpu/internal)
- Main navigation types (not all verified public entry points): [NativeVcpu](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/main/java/com/m3/vcpu/internal/NativeVcpu.java)
- Sampled contract: [NativeVcpu.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/main/java/com/m3/vcpu/internal/NativeVcpu.java); blob `19dc36c73b74ef932fa48244baa34fdf6a194692`
- Direct-import evidence from that sample: java.io.IOException, java.nio.file; full transitive/call graph remains unreviewed
- Build/project guide: [m3/ports/vcpu/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/README.md)
- Tests: [m3/ports/vcpu/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/test/java) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> JNI binding of the m3vcpu native HAL (m3vcpu_jni over m3vcpu). Six entry points, long handles, no policy. A session handle owns its buffer handles; closing a session releases their device storage. Every handle may be closed more than once (later closes are no-ops), so a Cleaner can own it. Native status codes map to IllegalArgumentException (invalid argument), IndexOutOfBoundsException (range / geometry), OutOfMemoryError, UnsupportedOperationException (backend, device or kernel format unavailable) and IllegalStateException (stale handle, driver, build or launch failure). Loader order: system property {@value #PATH_PROPERTY} (absolute library path), then the file named by {@value #PATH_FILE_PROPERTY} (whose content is that path), then System.loadLibrary("m3vcpu_jni"). The m3vcpu core next to the JNI library is loaded first so its dependency resolves on every platform.

<a id="package-27"></a>
### (unnamed)

Target-specific String source-layout gate using javac trees. Source checks do not establish VM layout compatibility or a passing matched-runtime test.

- Ownership/source: [m3/runtime-integration/src/main/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/runtime-integration/src/main/java)
- Main navigation types (not all verified public entry points): [M3StringLayout](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/runtime-integration/src/main/java/M3StringLayout.java), [M3StringReplacementRoutes](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/runtime-integration/src/main/java/M3StringReplacementRoutes.java)
- Sampled contract: [M3StringLayout.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/runtime-integration/src/main/java/M3StringLayout.java); blob `b33bd9b79958cf13ebcdec641441ca6edc2585b3`
- Direct-import evidence from that sample: com.sun.source, java.io.IOException, java.net.URI, java.nio.ByteBuffer, java.nio.charset, java.nio.file, java.util.ArrayList, java.util.List, java.util.Locale, java.util.Set, javax.lang.model, javax.tools.Diagnostic, javax.tools.DiagnosticCollector, javax.tools.JavaFileObject, javax.tools.SimpleJavaFileObject, javax.tools.ToolProvider; full transitive/call graph remains unreviewed
- Build/project guide: [m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/README.md)
- Tests: no package-specific test root mapped in this pass; inspect the owning project/harness
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Target-specific source gate; compilation, VM layout and jtreg remain separate gates.

<a id="package-28"></a>
### com.synexia.rewrite

Retained reusable-recipe custody or compatibility implementation. Its exact path and provenance matter; a local copy does not create a second canonical recipe authority.

- Ownership/source: [m3/synexia-import/pure-int-recipe-custody/src/main/java/com/synexia/rewrite](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/pure-int-recipe-custody/src/main/java/com/synexia/rewrite)
- Main navigation types (not all verified public entry points): [M3CanonicalMultiPassPlan](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/pure-int-recipe-custody/src/main/java/com/synexia/rewrite/M3CanonicalMultiPassPlan.java), [M3ContractMode](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/pure-int-recipe-custody/src/main/java/com/synexia/rewrite/M3ContractMode.java), [M3EditScope](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/pure-int-recipe-custody/src/main/java/com/synexia/rewrite/M3EditScope.java), [M3Jdk21A3AlgorithmCatalogueRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/pure-int-recipe-custody/src/main/java/com/synexia/rewrite/M3Jdk21A3AlgorithmCatalogueRecipe.java), [M3Jdk21A3SerialFileWorkRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/pure-int-recipe-custody/src/main/java/com/synexia/rewrite/M3Jdk21A3SerialFileWorkRecipe.java), [M3Jdk21A3SourceSealedRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/pure-int-recipe-custody/src/main/java/com/synexia/rewrite/M3Jdk21A3SourceSealedRecipe.java) and 10 more source files
- Sampled contract: [M3CanonicalMultiPassPlan.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/pure-int-recipe-custody/src/main/java/com/synexia/rewrite/M3CanonicalMultiPassPlan.java); blob `897a218c5489c851c1906fd91a2cd64174d19d70`
- Direct-import evidence from that sample: java.nio.charset, java.security.MessageDigest, java.security.NoSuchAlgorithmException, java.util.HexFormat, java.util.List, java.util.Objects; full transitive/call graph remains unreviewed
- Build/project guide: [m3/synexia-import/pure-int-recipe-custody/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/pure-int-recipe-custody/README.md)
- Tests: [m3/synexia-import/pure-int-recipe-custody/src/test/java/com/synexia/rewrite](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/pure-int-recipe-custody/src/test/java/com/synexia/rewrite) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Canonical scope-aware M3 convergence order. Independent FILE work may fan out. Within FILE scope, semantic convergence is explicitly atomization -> patternization/IOP -> derivable documentation -> fixed point. Scope authority grows only after that FILE frontier is sealed. Candidate production may parallelize; canonical promotion remains serial/proof-gated.

<a id="package-29"></a>
### com.m3.synexia.importer

Source-pinned receiver/admission helpers. A donor or Nebula proof may be an input to admission, but it does not itself authorize OpenJDK runtime promotion.

- Ownership/source: [m3/synexia-import/src/main/java/com/m3/synexia/importer](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/src/main/java/com/m3/synexia/importer)
- Main navigation types (not all verified public entry points): [NebulaMasteredDagReceiver](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/src/main/java/com/m3/synexia/importer/NebulaMasteredDagReceiver.java), [SynexiaCanonicalFamilyPolicy](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/src/main/java/com/m3/synexia/importer/SynexiaCanonicalFamilyPolicy.java), [SynexiaCanonicalRecipeHandoff](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/src/main/java/com/m3/synexia/importer/SynexiaCanonicalRecipeHandoff.java), [SynexiaGitCheckout](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/src/main/java/com/m3/synexia/importer/SynexiaGitCheckout.java), [SynexiaImportCli](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/src/main/java/com/m3/synexia/importer/SynexiaImportCli.java), [SynexiaImportManifest](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/src/main/java/com/m3/synexia/importer/SynexiaImportManifest.java) and 4 more source files
- Sampled contract: [NebulaMasteredDagReceiver.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/src/main/java/com/m3/synexia/importer/NebulaMasteredDagReceiver.java); blob `d45d01d81c17bc15d50baea57958c543ce874de5`
- Direct-import evidence from that sample: java.util.List, java.util.Objects; full transitive/call graph remains unreviewed
- Build/project guide: [m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/README.md)
- Tests: [m3/synexia-import/src/test/java/com/m3/synexia/importer](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/src/test/java/com/m3/synexia/importer) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Fail-closed receiver for the Nebula-proven portable M3 recipe DAG. Nebula is a proving target, not an authority to mutate OpenJDK. This receiver admits mechanical reuse only after one exact content-bound proof records green recipe-first, 99-percent semantic coverage, fixed-point and original-build gates.

<a id="package-30"></a>
### com.m3.a3

A3 command-line tooling for atomize, patternize and absorb operations. This is authoring/proof tooling rather than a runtime dependency.

- Ownership/source: [m3/tooling/a3/src/main/java/com/m3/a3](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/a3/src/main/java/com/m3/a3)
- Main navigation types (not all verified public entry points): [A3](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/a3/src/main/java/com/m3/a3/A3.java), [A3Alg](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/a3/src/main/java/com/m3/a3/A3Alg.java), [A3Apply](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/a3/src/main/java/com/m3/a3/A3Apply.java), [A3Cases](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/a3/src/main/java/com/m3/a3/A3Cases.java), [A3Fs](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/a3/src/main/java/com/m3/a3/A3Fs.java), [A3Inv](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/a3/src/main/java/com/m3/a3/A3Inv.java) and 9 more source files
- Sampled contract: [A3.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/a3/src/main/java/com/m3/a3/A3.java); blob `bd3d36d8eaa1ead5248b287ced403fd74ca0fe62`
- Direct-import evidence from that sample: java.nio.charset, java.nio.file, java.util.ArrayList, java.util.List; full transitive/call graph remains unreviewed
- Build/project guide: [m3/tooling/a3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/a3/README.md)
- Tests: [m3/tooling/a3/src/test/java/com/m3/a3](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/a3/src/test/java/com/m3/a3) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Compact CLI for A3 = Atomize -> Patternize -> Absorb.

<a id="package-31"></a>
### com.m3.tooling.dag

Scope-aware backport preparation and execution-plan tooling. Java source preparation uses existing A3 owners; non-Java work remains on the native/text lane.

- Ownership/source: [m3/tooling/backport-dag/src/main/java/com/m3/tooling/dag](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/backport-dag/src/main/java/com/m3/tooling/dag)
- Main navigation types (not all verified public entry points): [M3A3BackportPreparation](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/backport-dag/src/main/java/com/m3/tooling/dag/M3A3BackportPreparation.java), [M3AdmissionFact](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/backport-dag/src/main/java/com/m3/tooling/dag/M3AdmissionFact.java), [M3AtomEvidenceRoot](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/backport-dag/src/main/java/com/m3/tooling/dag/M3AtomEvidenceRoot.java), [M3BackportDagLoader](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/backport-dag/src/main/java/com/m3/tooling/dag/M3BackportDagLoader.java), [M3BackportDagMain](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/backport-dag/src/main/java/com/m3/tooling/dag/M3BackportDagMain.java), [M3BackportPacket](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/backport-dag/src/main/java/com/m3/tooling/dag/M3BackportPacket.java) and 19 more source files
- Sampled contract: [M3A3BackportPreparation.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/backport-dag/src/main/java/com/m3/tooling/dag/M3A3BackportPreparation.java); blob `19548080f59680e72ba3b3c2d67d50c8d86f34c3`
- Direct-import evidence from that sample: com.m3.a3, java.io.IOException, java.nio.charset, java.nio.file, java.util.ArrayList, java.util.Comparator, java.util.HashMap, java.util.List, java.util.Map, java.util.Objects; full transitive/call graph remains unreviewed
- Build/project guide: [m3/tooling/backport-dag/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/backport-dag/README.md)
- Tests: [m3/tooling/backport-dag/src/test/java/com/m3/tooling/dag](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/backport-dag/src/test/java/com/m3/tooling/dag) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Packet-front preparation gate between exact file-delta inventory and source-changing backport recipes. Java source/test files are converged through the existing A3 FILE recipe laboratory before a backport recipe crate may run. Non-Java files are classified explicitly and remain owned by the source-sealed native/text recipe lane; they are never passed through a Java parser.

<a id="package-32"></a>
### com.m3.rewrite.jmod

OpenRewrite-backed module descriptor materialization and fixed-point verification with drift refusal and receipt output.

- Ownership/source: [m3/tooling/jmod-recipes/src/main/java/com/m3/rewrite/jmod](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/jmod-recipes/src/main/java/com/m3/rewrite/jmod)
- Main navigation types (not all verified public entry points): [M3ModuleDescriptorApply](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/jmod-recipes/src/main/java/com/m3/rewrite/jmod/M3ModuleDescriptorApply.java), [M3ModuleDescriptorRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/jmod-recipes/src/main/java/com/m3/rewrite/jmod/M3ModuleDescriptorRecipe.java)
- Sampled contract: [M3ModuleDescriptorApply.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/jmod-recipes/src/main/java/com/m3/rewrite/jmod/M3ModuleDescriptorApply.java); blob `0c35f76c65cb009bfda0a7594a79005b457767b4`
- Direct-import evidence from that sample: java.io.IOException, java.io.UncheckedIOException, java.nio.charset, java.nio.file, java.util.ArrayList, java.util.List, org.openrewrite.InMemoryExecutionContext, org.openrewrite.Result, org.openrewrite.SourceFile, org.openrewrite.internal, org.openrewrite.marker, org.openrewrite.text; full transitive/call graph remains unreviewed
- Build/project guide: [m3/tooling/jmod-recipes/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/jmod-recipes/README.md)
- Tests: [m3/tooling/jmod-recipes/src/test/java/com/m3/rewrite/jmod](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/jmod-recipes/src/test/java/com/m3/rewrite/jmod) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Applies M3ModuleDescriptorRecipe to a repository root: parses the descriptor targets that already exist, runs the recipe, writes generated files, then runs it again and requires zero changes (fixed point). Exit codes: 0 applied or already applied, 3 drift refusal, 4 not a fixed point. Writes a receipt TSV (target, before_sha256 / ABSENT, after_sha256, change). usage: M3ModuleDescriptorApply <repo-root> <DESCRIPTOR_MANIFEST.tsv> <receipt.tsv>

<a id="package-33"></a>
### com.m3.tooling.priority

Primitive-long lazy priority frontier: defensive-copy heap construction, peek and one-path repair on poll, without retaining an object per entry.

- Ownership/source: [m3/tooling/lazy-priority-frontier/src/main/java/com/m3/tooling/priority](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/lazy-priority-frontier/src/main/java/com/m3/tooling/priority)
- Main navigation types (not all verified public entry points): [M3LazyLongPriorityFrontier](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/lazy-priority-frontier/src/main/java/com/m3/tooling/priority/M3LazyLongPriorityFrontier.java)
- Sampled contract: [M3LazyLongPriorityFrontier.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/lazy-priority-frontier/src/main/java/com/m3/tooling/priority/M3LazyLongPriorityFrontier.java); blob `b90aecd8c7232c526f1cab8244b490062ab7b196`
- Direct-import evidence from that sample: java.util.Arrays, java.util.NoSuchElementException, java.util.Objects; full transitive/call graph remains unreviewed
- Build/project guide: [m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/README.md)
- Tests: no package-specific test root mapped in this pass; inspect the owning project/harness
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Primitive-long lazy priority frontier. Construction heapifies a defensive copy; peek observes the current top without removal and poll removes only that top, repairing one heap path. No per-entry object is retained and no fully sorted representation is materialized.

<a id="package-34"></a>
### com.m3.rewrite

Target-specific migration recipe entry points. The sampled compatibility installer is additive and does not lower arbitrary callers or replace java.base.

- Ownership/source: [m3/tooling/migration-recipes/src/main/java/com/m3/rewrite](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite)
- Main navigation types (not all verified public entry points): [InstallIndexStringCompatibility](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/InstallIndexStringCompatibility.java), [M3Java21ConvergenceCatalog](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/M3Java21ConvergenceCatalog.java), [M3Java21ConvergenceRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/M3Java21ConvergenceRecipe.java)
- Sampled contract: [InstallIndexStringCompatibility.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/InstallIndexStringCompatibility.java); blob `a5a1745d543de4b043580e39891f7fec31b541b1`
- Direct-import evidence from that sample: java.io.IOException, java.io.InputStream, java.nio.charset, java.nio.file, java.security.MessageDigest, java.security.NoSuchAlgorithmException, java.util.ArrayList, java.util.Collection, java.util.HashMap, java.util.HexFormat, java.util.List, java.util.Map, java.util.Properties, org.openrewrite.ExecutionContext, org.openrewrite.ScanningRecipe, org.openrewrite.SourceFile, org.openrewrite.Tree, org.openrewrite.TreeVisitor, org.openrewrite.java; full transitive/call graph remains unreviewed
- Build/project guide: [m3/tooling/migration-recipes/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/README.md)
- Tests: [m3/tooling/migration-recipes/src/test/java/com/m3/rewrite](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/test/java/com/m3/rewrite) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Explicit additive compatibility installation, not java.base or automatic caller lowering.

<a id="package-35"></a>
### com.m3.rewrite.a3

M3JDK21 compatibility facades over the canonical Synexia A3 catalogue recipes; reusable semantics stay with the donor owner.

- Ownership/source: [m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/a3](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/a3)
- Main navigation types (not all verified public entry points): [M3A3AlgorithmCatalogueRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/a3/M3A3AlgorithmCatalogueRecipe.java), [M3A3SerialFileWorkRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/a3/M3A3SerialFileWorkRecipe.java)
- Sampled contract: [M3A3AlgorithmCatalogueRecipe.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/a3/M3A3AlgorithmCatalogueRecipe.java); blob `313d3f64844f869c282b411369d2cf4d4385bf93`
- Direct-import evidence from that sample: java.util.List, java.util.Set, org.openrewrite.Recipe; full transitive/call graph remains unreviewed
- Build/project guide: [m3/tooling/migration-recipes/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/README.md)
- Tests: [m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/a3](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/a3) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> M3JDK21 compatibility facade for the canonical Synexia A3 catalogue packet.

<a id="package-36"></a>
### com.m3.rewrite.backport

Source-sealed receivers for the backport planning/proof control plane. The sampled receiver does not itself edit or promote JDK product source.

- Ownership/source: [m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport)
- Main navigation types (not all verified public entry points): [M3BackportExecutionWaveReceiverRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3BackportExecutionWaveReceiverRecipe.java), [M3BulkExecutorGateRepairRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3BulkExecutorGateRepairRecipe.java), [M3BulkExecutorNameMappingRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3BulkExecutorNameMappingRecipe.java), [M3BulkExecutorPortRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3BulkExecutorPortRecipe.java), [M3BulkFreezeSourceBootstrapRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3BulkFreezeSourceBootstrapRecipe.java), [M3BulkInputBindingReceiverRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3BulkInputBindingReceiverRecipe.java) and 54 more source files
- Sampled contract: [M3BackportExecutionWaveReceiverRecipe.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3BackportExecutionWaveReceiverRecipe.java); blob `534193b61988f49a812ba12088a22c770902d559`
- Direct-import evidence from that sample: java.util.List, java.util.Set, org.openrewrite.Recipe; full transitive/call graph remains unreviewed
- Build/project guide: [m3/tooling/migration-recipes/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/README.md)
- Tests: [m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Source-sealed receiver for the M3JDK21 backport execution-wave control plane. The received Python/Markdown/POM files are planning/proof substrate only. They schedule already-admitted backport recipe atoms; they do not edit JDK product source, approve scope promotion, or promote a backport.

<a id="package-37"></a>
### com.m3.rewrite.scope

Target compatibility vocabulary adapting canonical Synexia scope/contract modes.

- Ownership/source: [m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope)
- Main navigation types (not all verified public entry points): [M3ContractMode](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/M3ContractMode.java), [M3EditScope](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/M3EditScope.java), [M3RecipeScopePolicy](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/M3RecipeScopePolicy.java), [M3RecipeScopeRegistry](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/M3RecipeScopeRegistry.java), [M3ScopeInference](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/M3ScopeInference.java)
- Sampled contract: [M3ContractMode.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/M3ContractMode.java); blob `3ee8dd184d5c382391f6f9590fb9c773044bfaa5`
- Direct-import evidence from that sample: java.util.Objects; full transitive/call graph remains unreviewed
- Build/project guide: [m3/tooling/migration-recipes/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/README.md)
- Tests: [m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/scope](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/scope) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> M3JDK21 compatibility vocabulary for Synexia's canonical contract modes. This target enum is a thin adapter only; reusable meaning is owned by com.synexia.rewrite.M3ContractMode.

<a id="package-38"></a>
### com.m3.rewrite.semantic

Thin target recipe adapters delegating semantic-source-hash work to the canonical child recipe.

- Ownership/source: [m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic)
- Main navigation types (not all verified public entry points): [M3SemanticHashRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic/M3SemanticHashRecipe.java), [M3SemanticHashTable](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic/M3SemanticHashTable.java), [M3SemanticHasher](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic/M3SemanticHasher.java)
- Sampled contract: [M3SemanticHashRecipe.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic/M3SemanticHashRecipe.java); blob `daa47582dc3d2d80f25727df5d1cc9be417de6cb`
- Direct-import evidence from that sample: java.util.List, java.util.Set, org.openrewrite.Recipe; full transitive/call graph remains unreviewed
- Build/project guide: [m3/tooling/migration-recipes/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/README.md)
- Tests: [m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/semantic](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/semantic) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> M3JDK21 compatibility receiver for Synexia's canonical semantic-hash recipe. The reusable recipe implementation is owned by hsoliwal/com.synexia. M3JDK21 retains this class name only as a thin target adapter and delegates execution to the canonical child recipe.

<a id="package-39"></a>
### com.m3.rewrite.synexia

Read-only OpenRewrite inventory projections for source-pinned Synexia delivery manifests.

- Ownership/source: [m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/synexia](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/synexia)
- Main navigation types (not all verified public entry points): [M3SynexiaImportInventoryRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/synexia/M3SynexiaImportInventoryRecipe.java)
- Sampled contract: [M3SynexiaImportInventoryRecipe.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/synexia/M3SynexiaImportInventoryRecipe.java); blob `f23cad1939ff2fb71d6916d89e35caefdb06583e`
- Direct-import evidence from that sample: java.util.ArrayList, java.util.Comparator, java.util.List, java.util.Set, org.openrewrite.Column, org.openrewrite.DataTable, org.openrewrite.ExecutionContext, org.openrewrite.ScanningRecipe, org.openrewrite.SourceFile, org.openrewrite.Tree, org.openrewrite.TreeVisitor, org.openrewrite.text; full transitive/call graph remains unreviewed
- Build/project guide: [m3/tooling/migration-recipes/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/README.md)
- Tests: [m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/synexia](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/synexia) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Read-only OpenRewrite inventory for the pinned Synexia -> M3JDK21 Apache delivery manifest.

<a id="package-40"></a>
### com.synexia.rewrite

Retained reusable-recipe custody or compatibility implementation. Its exact path and provenance matter; a local copy does not create a second canonical recipe authority.

- Ownership/source: [m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite)
- Main navigation types (not all verified public entry points): [M3HashPinnedJavaSnapshotRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java), [M3MIndexJoinedCharsViewRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/M3MIndexJoinedCharsViewRecipe.java), [M3SegmentedLaneNativeRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/M3SegmentedLaneNativeRecipe.java)
- Sampled contract: [M3HashPinnedJavaSnapshotRecipe.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java); blob `d58ca50dfaeb60f581b64f43545e688aca5c6530`
- Direct-import evidence from that sample: com.fasterxml.jackson, java.io.IOException, java.nio.ByteBuffer, java.nio.charset, java.nio.file, java.security.MessageDigest, java.security.NoSuchAlgorithmException, java.util.ArrayList, java.util.Collection, java.util.HashMap, java.util.HashSet, java.util.HexFormat, java.util.List, java.util.Map, java.util.Set, org.openrewrite.ExecutionContext, org.openrewrite.Option, org.openrewrite.Parser, org.openrewrite.ScanningRecipe, org.openrewrite.SourceFile, org.openrewrite.Tree, org.openrewrite.TreeVisitor, org.openrewrite.java, org.openrewrite.text; full transitive/call graph remains unreviewed
- Build/project guide: [m3/tooling/migration-recipes/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/README.md)
- Tests: [m3/tooling/migration-recipes/src/test/java/com/synexia/rewrite](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/test/java/com/synexia/rewrite) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Reusable, candidate-only Java migration from an exact source snapshot to reviewed Java LSTs. Each crate supplies a manifest and UTF-8 templates as classpath resources. All target preimages and all output templates are admitted before any output is produced. Java LST mode is the default. Explicit PlainText mode is a source-custody fallback for parser-incompatible exact JDK owners and does not claim Java semantic/type validation. Hashes cover rendered Java text encoded as UTF-8, not the original on-disk byte encoding. Replacements preserve the input charset and BOM flag and discard its obsolete checksum. This recipe checks source identity and syntax; the sealed M3 contract, coverage, compiler, test and runtime gates remain authoritative.

<a id="package-41"></a>
### com.synexia.rewrite.semantic

Non-mutating Java semantic-source-hash evidence generation. A fingerprint is evidence to inspect, not a behavioral equivalence verdict.

- Ownership/source: [m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/semantic](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/semantic)
- Main navigation types (not all verified public entry points): [M3SemanticHashRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/semantic/M3SemanticHashRecipe.java), [M3SemanticHashTable](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/semantic/M3SemanticHashTable.java), [M3SemanticHasher](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/semantic/M3SemanticHasher.java)
- Sampled contract: [M3SemanticHashRecipe.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/semantic/M3SemanticHashRecipe.java); blob `e315710ba16b4c9b276b53e86b47c81ab582caf5`
- Direct-import evidence from that sample: java.util.Set, org.openrewrite.ExecutionContext, org.openrewrite.Recipe, org.openrewrite.TreeVisitor, org.openrewrite.java; full transitive/call graph remains unreviewed
- Build/project guide: [m3/tooling/migration-recipes/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/README.md)
- Tests: [m3/tooling/migration-recipes/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/src/test/java) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Non-mutating OpenRewrite semantic-source-hash evidence generator for Java 21 source.

<a id="package-42"></a>
### com.m3.tools.modulepack

JAR/JMOD descriptor, class-version, resource, native-entry and checksum inventory plus module-graph validation. Checksums do not certify license or ABI compatibility.

- Ownership/source: [m3/tooling/module-packs/kernel/src/main/java/com/m3/tools/modulepack](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/kernel/src/main/java/com/m3/tools/modulepack)
- Main navigation types (not all verified public entry points): [M3ModuleArtifact](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/kernel/src/main/java/com/m3/tools/modulepack/M3ModuleArtifact.java), [M3ModuleGraph](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/kernel/src/main/java/com/m3/tools/modulepack/M3ModuleGraph.java), [M3ModuleInspector](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/kernel/src/main/java/com/m3/tools/modulepack/M3ModuleInspector.java), [M3ModuleInventory](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/kernel/src/main/java/com/m3/tools/modulepack/M3ModuleInventory.java)
- Sampled contract: [M3ModuleArtifact.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/kernel/src/main/java/com/m3/tools/modulepack/M3ModuleArtifact.java); blob `10b92709f0eac4aef0608b5e6a66ee5c4bd3f848`
- Direct-import evidence from that sample: java.lang.module, java.nio.file, java.util.List, java.util.Objects; full transitive/call graph remains unreviewed
- Build/project guide: [m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/README.md)
- Tests: [m3/tooling/module-packs/kernel/src/test/java/com/m3/tools/modulepack](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/kernel/src/test/java/com/m3/tools/modulepack) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Immutable archive inventory. Pattern/IOP role: InventoryRecord. A checksum records bytes; it does not certify license, ABI or compatibility.

<a id="package-43"></a>
### com.m3.rewrite.packs

Read-only OpenRewrite DataTable adapter over the module-pack inventory kernel, without a competing module registry.

- Ownership/source: [m3/tooling/module-packs/recipes/src/main/java/com/m3/rewrite/packs](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/recipes/src/main/java/com/m3/rewrite/packs)
- Main navigation types (not all verified public entry points): [M3InventoryModulePackRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/recipes/src/main/java/com/m3/rewrite/packs/M3InventoryModulePackRecipe.java), [M3RegisterModulePacksRecipe](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/recipes/src/main/java/com/m3/rewrite/packs/M3RegisterModulePacksRecipe.java)
- Sampled contract: [M3InventoryModulePackRecipe.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/recipes/src/main/java/com/m3/rewrite/packs/M3InventoryModulePackRecipe.java); blob `a9657667d9304c622742d15c30160ece29b943be`
- Direct-import evidence from that sample: com.fasterxml.jackson, com.m3.tools, java.io.IOException, java.io.UncheckedIOException, java.nio.file, java.util.ArrayList, java.util.Collection, java.util.List, org.openrewrite.Column, org.openrewrite.DataTable, org.openrewrite.ExecutionContext, org.openrewrite.Option, org.openrewrite.ScanningRecipe, org.openrewrite.SourceFile, org.openrewrite.TreeVisitor; full transitive/call graph remains unreviewed
- Build/project guide: [m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/README.md)
- Tests: [m3/tooling/module-packs/recipes/src/test/java/com/m3/rewrite/packs](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/recipes/src/test/java/com/m3/rewrite/packs) (located, not executed)
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Read-only OpenRewrite projection of the shared inventory kernel. Pattern/IOP role: Adapter. Scope: READ_ONLY external artifacts, no source mutation.

<a id="package-44"></a>
### com.synexia.rewrite.sealed

Implementation-identity proof helpers based on loaded class bytes and declared closure, rather than version labels alone.

- Ownership/source: [m3/tooling/src/main/java/com/synexia/rewrite/sealed](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/src/main/java/com/synexia/rewrite/sealed)
- Main navigation types (not all verified public entry points): [CodeIdentity](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/src/main/java/com/synexia/rewrite/sealed/CodeIdentity.java), [ExtractPrivateIntHelper](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/src/main/java/com/synexia/rewrite/sealed/ExtractPrivateIntHelper.java), [JavaContractSurface](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/src/main/java/com/synexia/rewrite/sealed/JavaContractSurface.java), [JavaSourceUnit](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/src/main/java/com/synexia/rewrite/sealed/JavaSourceUnit.java), [M3ExactSourceAtom](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/src/main/java/com/synexia/rewrite/sealed/M3ExactSourceAtom.java), [SealHash](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/src/main/java/com/synexia/rewrite/sealed/SealHash.java) and 3 more source files
- Sampled contract: [CodeIdentity.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/src/main/java/com/synexia/rewrite/sealed/CodeIdentity.java); blob `33b941ffe94226e86256fa5e9e58e1c41c4332db`
- Direct-import evidence from that sample: java.io.IOException, java.util.Arrays, java.util.Comparator; full transitive/call graph remains unreviewed
- Build/project guide: [m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/README.md)
- Tests: no package-specific test root mapped in this pass; inspect the owning project/harness
- Inline package documentation: no package-info.java at this physical directory; source comments and the guide do not close that gap
- Status: source-sampled, partial documentation; complete API/error/concurrency/effects review, caller mapping and exact-head test evidence remain open

The sampled source documents the following bounded contract (quoted from that public source; it is not a fresh test result):

> Actual loaded class bytes for an explicitly declared implementation closure, never a version label alone.

## Text/storage API contracts

This bounded second pass read the complete bodies of M3StringPiece, LocalM3Arena,
LocalM3StringPiece, JoinedM3StringPiece, StorageIdentity and SharedLexiconImage,
plus FoundationTest. It closes the explanation gaps below for those owners; it does
not cover every lexicon/catalog/precompute class in com.m3.text. Source remains at
`278b221e8365ebeb6095a3a5643b38a0564258b5`. All test references below are **read, NOT_RUN**.

### Text units, ownership and identity

[M3StringPiece](../../m3/core/src/com/m3/text/M3StringPiece.java) is a sealed
CharSequence whose permitted implementations are LocalM3StringPiece and the
package-private JoinedM3StringPiece. Length, charAt indexes, subsequence bounds and
local range offsets are **UTF-16 code units**, not bytes, Unicode code points or
user-perceived characters. Bounds use start-inclusive/end-exclusive ranges. A
subsequence may split a surrogate pair; no normalization or repair occurs.

[LocalM3Arena](../../m3/core/src/com/m3/text/LocalM3Arena.java) is an explicit
allocator, not an interner. copyUtf16(char[]) writes each code unit into privately
owned little-endian bytes. copyLatin1(byte[]) clones the input and interprets each
byte as an unsigned 0–255 code unit. Neither method performs charset guessing.
Callers must keep mutable input stable during the copy; the method does not lock
or snapshot an array atomically against concurrent external mutation.

The arena retains its namespace and atomic record counter, not the allocated
backing arrays. Each published piece owns its backing through a final reference.
Dropping the arena does not invalidate pieces. A fresh arena has a new UUID;
records start at one, generation is one in this implementation, and counter
exhaustion throws IllegalStateException rather than wrapping. Atomic record
allocation is not a general guarantee about concurrent caller-owned input.

[StorageIdentity](../../m3/core/src/com/m3/text/StorageIdentity.java) validates a
nonnull owner and positive generation/record. It describes a backing, not a
particular range, content equality or access capability. Its public record
constructor does not authenticate an identity. Equal text copied twice receives
distinct allocation identities. Local piece equality is ordinary object identity;
this API does not override equals/hashCode to implement String content equality.

### Slices, joins and materialization

| API | Ownership / result | Failure and cost boundary |
| --- | --- | --- |
| LocalM3Arena.copyUtf16 | New local owned bytes preserving every char, including unpaired surrogates | Null input: NullPointerException; length × 2 uses multiplyExact and may throw ArithmeticException; allocation failure propagates |
| LocalM3Arena.copyLatin1 | New cloned byte backing; unsigned Latin-1 units | Null input: NullPointerException; allocation failure propagates |
| LocalM3StringPiece.subSequence | New range object sharing the same entire backing and StorageIdentity | Invalid half-open bounds: IndexOutOfBoundsException; a tiny slice may retain a large backing |
| LocalM3StringPiece.retainedBackingBytes | Byte length of the entire retained payload array | Excludes object/array headers, the piece itself, and other owners; not an all-in heap size |
| M3StringPiece.join | Clones the caller's directory, omits empty leaves, flattens nested joined directories and stores cumulative ends | Null directory/element: NullPointerException; leaf/length sums use addExact; directory metadata allocates even though character bytes are not copied |
| JoinedM3StringPiece.charAt | Binary search over cumulative ends, then local-leaf lookup | Invalid index: IndexOutOfBoundsException; lookup is not constant-time independent of leaf count |
| JoinedM3StringPiece.subSequence | New local leaf ranges and a new flat joined directory | Invalid bounds refused; scans the existing leaf directory and allocates metadata |
| flatten / toString | New char[] projection followed by ordinary String construction | Explicit allocating/export boundary; no cached flat String is retained by these implementations |

Sources: [local piece](../../m3/core/src/com/m3/text/LocalM3StringPiece.java),
[joined piece](../../m3/core/src/com/m3/text/JoinedM3StringPiece.java).
Published piece state is immutable through this API; sharing readers does not
require a mutable cursor or a cache update. This source observation is not a
stress-test result. No close, pool-return or explicit free operation exists for
these local backing objects; reachability controls their lifetime.

### Local usage example

This example is checked against the declarations and source logic, but was not compiled or run in this pass:

```java
import com.m3.text.LocalM3Arena;
import com.m3.text.LocalM3StringPiece;
import com.m3.text.M3StringPiece;

LocalM3Arena arena = new LocalM3Arena();
char[] input = {'A', '\uD83D', '\uDE42'};
LocalM3StringPiece whole = arena.copyUtf16(input);
input[0] = 'X';                     // Does not mutate the published piece
M3StringPiece face = whole.subSequence(1, 3); // Two UTF-16 units
M3StringPiece joined = M3StringPiece.join(
        face, arena.copyLatin1(new byte[] {'!'}));
String exported = joined.flatten(); // Expected: the smiling face followed by !
```

The example does not claim zero allocation: input copying, range objects, joined
directories and final String projection are deliberate boundaries.

### SharedLexiconImage file and lifetime contract

[SharedLexiconImage](../../m3/core/src/com/m3/text/SharedLexiconImage.java) maps an
explicit local path read-only. It does not fetch a network resource or attach to
String constructors. The file channel is closed after mapping. The image is **not
AutoCloseable** and has no close/unmap method; mapping lifetime follows
reachability and the VM. warm() explicitly requests mapping load, without a
portable latency or residency guarantee.

- Maximum mapped file: 64 MiB; maximum record count: 65,536
- A 64-byte header holds magic/version/count/dimensions/checksum. Header and directory integers are big-endian; text payload code units are UTF16_LE
- open accepts versions 1 and 2; create emits version 1. Directory entries are 8 bytes in v1 and 12 bytes in v2; v2 additionally validates the stored Java String hash
- Record offsets and lengths are measured in UTF-16 units within the payload. Records must be strictly increasing in Java UTF-16 String ordering, so duplicates are refused
- open checks dimensions, whole-image SHA-256, record ranges, ordering and per-record snapshots; it checks the image digest again after capture
- copyRecord(index, arena) checks the arena before the index, copies the selected bytes, compares their SHA-256 with the captured record digest, then gives the owned copy a fresh local-arena identity
- A published local piece does not retain mapped storage and is not changed by later file edits. A later copy may throw IOException if the mapped record has changed
- read-only mapping is not OS sealing. The caller must publish/manage immutable files. Concurrent external modification/truncation is not a supported hostile-file sandbox

open/create/copyRecord propagate filesystem IOException failures. Unsupported
headers/versions, corrupt dimensions/checksums/ranges/order and changed records
are explicitly rejected with IOException. Invalid record indexes produce
IndexOutOfBoundsException; null arena produces NullPointerException. create
snapshots the List, rejects null elements through List.copyOf, requires sorted
unique words, and opens the destination with CREATE_NEW, so it never deliberately
truncates an existing file. It is not an atomic publication transaction: an I/O
failure after creating a new file can leave a partial output requiring caller
cleanup. The source does not provide a cross-process writer lock or a signed
provenance check.

The image retains two int arrays and 32 digest bytes per record, or 40 × count
primitive metadata bytes, plus headers, mapping/VM metadata and other fields.
copyRecord and opening/hash validation allocate temporary storage; this formula
is not a whole-process memory bound. Multiple independently opened images can
retain additional mappings. Source readers use per-call duplicate/sliced buffers;
no explicit concurrent-read qualification was performed here.

### Foundation test linkage and remaining gaps

[FoundationTest.pieces](../../m3/core/test/FoundationTest.java) contains checks for
mutable-input isolation, range identity and offsets, caller-directory mutation,
all 256 Latin-1 values, malformed UTF-16, deep repeated joins, bounds/null refusal
and concurrent arena record identities. FoundationTest.images includes v1/v2
images, malformed headers, existing-destination refusal, missing shards,
post-publication local-piece isolation and changed-record rejection.

Those tests were read, not executed. Remaining gaps include identity-counter
exhaustion and allocation-failure injection, interrupted file creation/recovery,
hostile concurrent file changes, platform mapping lifetime, and full contracts for
SharedLexiconCatalog and the other package owners. Some catalogue tests live in
the same test file, but their presence does not extend this six-owner review to
all catalogue APIs.

## Precompute API contracts

This bounded second pass read the complete bodies of M3TextMetrics,
M3EditDistancePlan, M3Progress, M3TextSignals and its package-private M3Histogram
dependency, plus the two test files linked below. These are independent library
APIs under `m3/ports/precompute`, not java.lang.String state or a new bootstrap
precompute framework. No test or benchmark was run.

### M3TextMetrics: units, encoding and seam composition

[M3TextMetrics](../../m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java)
is an immutable record of UTF-16 length, UTF-8 encoded byte length, code-point
count, unpaired-surrogate count, Java hash, 31-power and first/last UTF-16 units.
It retains no spelling payload.

of(CharSequence) scans the input's current charAt/length view. Use an immutable
or externally stabilized input: it does not copy or synchronize a mutable
CharSequence. A valid surrogate pair contributes two UTF-16 units, one code
point and four UTF-8 bytes. An unpaired surrogate contributes one code point and
the Java UTF-8 encoder's replacement-byte length; it is not silently treated as
three bytes of well-formed UTF-8. Empty boundary units are -1, and EMPTY has
hash31Power 1.

combine(left, right) uses stored facts without rereading text. A high-surrogate
left boundary followed by a low-surrogate right boundary joins into one code
point: the implementation subtracts one code point and two unpaired-surrogate
counts and adjusts the UTF-8 byte count. Length/count additions and selected
subtractions use exact arithmetic and can throw ArithmeticException. Java hash
and hash31Power intentionally use ordinary int overflow, matching the String
hash recurrence. Null operands/input are rejected with NullPointerException.

The public record constructor validates nonnegative counters and boundary-unit
shape, **not full semantic consistency or authenticity**. Prefer of/combine over
hand-authored metric values; a caller can construct facts whose hash/counts do
not describe any supplied text. isWellFormedUtf16 only checks that the stored
unpaired count is zero. Facts and matching hashes never prove text equality.

### M3TextSignals: exact facts versus candidate signals

[M3TextSignals](../../m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java)
compiles String input directly; for another CharSequence it uses toString(). The
result stores derived metrics, sorted unigram/bigram/trigram histograms, boundary
units and sketches, not the whole original spelling. For a custom CharSequence,
toString must represent the same characters used by the caller. No concurrent
mutation contract is enforced by the compiler.

- Histogram characters and q-grams use UTF-16 units; letter/digit/whitespace/case classification uses Character code-point operations
- characterCount, bigramCount and trigramCount are exact counts in those derived histograms; isAnagramOf compares UTF-16-unit multisets and does not imply order, code-point or normalization equivalence
- mayContain uses length and a 64-bit presence filter; mayEqual uses length and two hashes. Positive answers require original-text verification
- editLowerBound takes the maximum of length difference and rounded-up histogram L1 differences divided by 2, 4 and 6 for unigrams, bigrams and trigrams. Its documented domain is unit-cost UTF-16 Levenshtein
- SimHash distance and 16-lane estimated Jaccard are ranking signals only. minHash() returns a defensive clone; labels() constructs an immutable list
- combine joins histograms and adds the cross-boundary bigram/up to two trigrams. It uses boundary units to repair supplementary-code-point classification without reading child spellings
- Empty text has vacuous ALL_LETTERS, ALL_DIGITS and BLANK flags, alongside ASCII, LATIN1 and WELL_FORMED_UTF16; HAS_LETTER/HAS_DIGIT remain false

[M3Histogram](../../m3/ports/precompute/src/main/java/com/m3/precompute/M3Histogram.java)
is package-private: of sorts an owned work array into distinct keys/counts;
plus constructs merged arrays and uses exact additions for duplicate counts.
Public signal methods reject null operands. Arithmetic and allocation failures
propagate. histogramPayloadBytes counts only retained key/count primitive arrays
(12 bytes per distinct key across the three histograms), not object headers,
metrics, sketches, temporary arrays or total heap.

Compilation allocates and sorts temporary q-gram arrays; combination allocates
histogram/metadata results. These APIs are reusable facts, not an allocation-free
path or a blanket constant-memory guarantee. The public result exposes no
mutable histogram array; this does not replace a concurrent publication/runtime
qualification.

### M3EditDistancePlan: exact query and workspace contract

[M3EditDistancePlan](../../m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java)
computes unit-cost Levenshtein over **UTF-16 code units**. A supplementary
character can therefore participate as two edit units. compile(CharSequence)
copies pattern chars; it does not retain caller-mutable pattern storage. The
caller must still keep the input stable during that copy.

| Entry point | Result and effect | Refusal / constraint |
| --- | --- | --- |
| compile(pattern) | Immutable char snapshot and, for lengths 1–63, sorted unit masks for single-word Myers | Null: NullPointerException; more than 1,048,576 units: IllegalArgumentException |
| workspace() | New mutable two-row int workspace and compiled pattern signals, bound to this exact plan instance | Allocates even when the plan uses Myers; identical pattern contents do not make another plan's workspace valid |
| distance(value) | Exact int distance using a freshly created workspace and no-op monitor | Convenience call allocates; source must be stable during the call |
| distance(value, workspace, monitor) | Myers for 1–63-unit patterns, two-row DP for longer patterns; empty pattern returns source length | Null value/workspace refused; foreign workspace: IllegalArgumentException; null monitor means no-op |
| within(value, maxDistance) | Exact threshold decision after length/histogram negative filters, with a fresh workspace | Negative threshold: IllegalArgumentException; positive filters do not themselves establish acceptance |
| within(value, workspace, maxDistance, monitor) | Reuses caller rows, compiles input signals, then uses the selected exact Myers/banded path if needed | Workspace remains bound by object identity; mutable inputs are not copied for the entire operation |
| primitivePayloadBytes() | Counts the plan's retained primitive pattern/mask arrays | Excludes workspace rows, pattern/input signals, object headers, temporary arrays and caller storage |
| charAt / subSequence | Pattern code-unit access; subsequence returns a new ordinary String | Bounds checked with Objects; subSequence and toString materialize |

Both query APIs reject source lengths above 1,048,576 units. Full DP rejects
sourceLength × max(1, patternLength) greater than 64,000,000. The banded guard
uses sourceLength × max(1, min(patternLength, 2 × threshold + 1)) with the same
limit. The guards use exact long arithmetic. These numbers bound the selected
DP-cell calculation, **not all CPU work or allocation**: within compiles/sorts
source histograms, and bandedWithin fills a full pattern-length row on each
iteration even when the recurrence visits only a narrow band. No speedup or
strict whole-operation work bound is implied.

A Workspace contains mutable arrays with no lock. Reuse it sequentially; do not
share it across concurrent/reentrant calls. Separate workspaces are needed for
independent calls sharing an immutable plan, and callers must also stabilize
source input and monitor state. previousRowCopy() clones the stored row; it is a
diagnostic copy, not a promise that every early-refusal path populated a fresh
complete DP matrix. This source-based usage guidance has not been stress-tested
in this pass.

### Progress, cancellation and early returns

[M3Progress](../../m3/ports/precompute/src/main/java/com/m3/precompute/M3Progress.java)
provides no-op defaults. Cancellation is caller-defined through checkCanceled;
there is no built-in cancellation token or implicit thread-interrupt handling.
Myers checks every 1,024 input iterations (including index zero) and once at the
end; dynamic/banded paths check every 1,024 rows and at normal completion.
Callbacks may throw and their exceptions propagate.

The algorithms call begin before entering their try/finally and done in that
finally. Thus done is reached after an entered algorithm body, including its
exceptions/early exit, but is not guaranteed if begin itself throws. Argument,
budget, empty-pattern, length-gap or histogram rejection can return/refuse before
an algorithm begins, with **no balanced begin/done callback pair**. A banded
row-minimum rejection returns before the final cancellation check. Do not use
these callbacks as a universal resource-acquire/release protocol. A throwing done
callback can replace an in-flight exception under ordinary Java finally rules.

### Precompute usage and test linkage

Source-checked example, not compiled or executed here:

```java
import com.m3.precompute.M3EditDistancePlan;
import com.m3.precompute.M3TextMetrics;

M3TextMetrics left = M3TextMetrics.of("\uD83D");
M3TextMetrics right = M3TextMetrics.of("\uDE42");
M3TextMetrics joined = M3TextMetrics.combine(left, right);
// Expected from the implementation: 2 UTF-16 units, 1 code point, 4 UTF-8 bytes

M3EditDistancePlan plan = M3EditDistancePlan.compile("kitten");
M3EditDistancePlan.Workspace work = plan.workspace();
int distance = plan.distance("sitting", work, null);       // Expected: 3
boolean close = plan.within("sitting", work, 2, null);    // Expected: false
// Reuse work only sequentially, and only with this same plan instance
```

[M3EditDistancePlanTest](../../m3/ports/precompute/src/test/java/com/m3/precompute/M3EditDistancePlanTest.java)
contains deterministic/empty examples, seeded Myers/DP/threshold comparisons with
an independent recurrence, wrong-workspace refusal and pattern/source/full-DP
budget checks. [M3TextSignalsTest](../../m3/ports/precompute/src/test/java/com/m3/precompute/M3TextSignalsTest.java)
contains flat-versus-composed metrics/histogram/sketch comparisons including
surrogate seams, finite lower-bound comparisons and candidate-signal checks.
These tests were read, **not run**, and their finite loops are not exhaustive
semantic proof.

Remaining targeted gaps: concurrent workspace misuse, mutable/custom CharSequence
inconsistency, monitor throwing/cancellation paths, negative-threshold and banded
budget edge cases, manually inconsistent metrics, allocation failure and measured
cold/warm/full-operation resource costs. The rest of com.m3.precompute, including
native providers and regex/image owners, retains its earlier partial review.


## Portable collection package contract continuation, 15 October 2026

For the six-file com.m3.util physical package under m3/ports/collections/src/main/java, the [portable primitive collection reference](portable-primitive-collection-reference.md) and [complete source/member routes](portable-primitive-collection-source-coverage.md) now supplement the earlier [package-11 sample](m3-packages.md#package-11). They cover all 97 explicit public callables and package-private support at c842fab8cf5b799257e3c9ea07f8baddde80db09. The earlier sampled inventory remains unchanged. Inline package docs, exhaustive callers and executed target qualification remain open; this is separate from com.m3.collections and from the sibling Fenwick owner already documented in indexed-algorithm-reference.md.

## Frozen-text fixture continuation for package-4

Source review: 15 October 2026 at `c842fab8cf5b799257e3c9ea07f8baddde80db09`.

The [frozen-text compatibility reference](frozen-text-compatibility-reference.md) and [complete four-source/member map](frozen-text-compatibility-source-coverage.md) supersede the earlier [package-4](#package-4) sample-only contract routing for this exact fixture root at `c842fab8cf5b799257e3c9ea07f8baddde80db09`. Its contents are two frozen-storage snapshots, a throwing `MIndexAddressedView` shim and `SynexiaMappedStorageTest`; there are no joined-view owners here. The separate [retained-port bridge](indexed-string-reference.md) keeps its own physical root and source pin.

The [actual test map](frozen-text-compatibility-source-coverage.md#actual-test-to-contract-map) resolves the previously unmapped route: the package-local direct-memory test and unnamed-package `SynexiaContractTest` are compiled by `m3/compatibility/check-synexia.py` into separate reference-test classes alongside the `com.m3.text` product module path. Full source/member contracts and test assertions are reviewed, while execution is NOT_RUN; package Javadoc, broader consumer/alias/error coverage and runtime qualification remain open. This is additive current-source guidance, not a change to the earlier sample's evidence state.


## Module-pack kernel contract supplement (2026-10-15)

The [package-42](#package-42) historical artifact sample is supplemented by a [complete four-owner kernel reference](module-pack-kernel-reference.md) and [source/member/test ledger](module-pack-kernel-source-coverage.md). At `510dfa27b0679ce33348140afc57b558b8853802`, all 335 lines of `M3ModuleArtifact`, `M3ModuleInspector`, `M3ModuleGraph` and `M3ModuleInventory`, their declared/generated surfaces, seven test/helper bodies and the bounded direct adapter route were read. The supplement records exact errors, effects, resource/ownership limits and test assertion strength without changing the earlier sample's historical pin.

No package-info file was added; recipe-package completion, whole-project caller closure and exact-head execution remain separate. Builds, tests, recipes and runtime/native qualification were NOT_RUN in this documentation authoring/review pass.

Hosted CI is separate exact-commit/workflow evidence and does not change these execution or admission limits.
