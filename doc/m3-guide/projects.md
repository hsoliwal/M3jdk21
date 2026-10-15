<!-- SPDX-License-Identifier: Apache-2.0 -->

# Repository and project architecture

[Guide](README.md) · [JDK modules](jdk-modules.md) · [M3 packages](m3-packages.md) · [Coverage](coverage.md)

## Artifact and dependency direction

- `src/java.*` and `src/jdk.*` are the 70 direct JDK named-module source roots; `src/hotspot` is the native VM
- `src/demo` and `src/utils` are demonstration/build-support areas, not two additional JDK modules
- `make` and `configure` own the upstream image build; `test` contains jtreg, native/unit, microbenchmark and harness sources
- `m3/core`, arrays, collections and ports are independent or opt-in source/build units. Their existence does not imply JDK promotion
- `m3/tooling`, recipes, backports and importers form the authoring/proof plane. `m3/vendor` and retained snapshots preserve donor/upstream identity
- Java/HotSpot/JNI runtime consumers do not gain a Synexia runtime dependency. The isolated IndexString/Tornado ports and authoring tools have different dependency boundaries

The 23 workstream descriptions below are a reading map, not a claim that there are exactly 23 build projects. The exhaustive [descriptor ledger](descriptors.md) records 266 POMs and 565 module descriptors, including historical/test/vendor copies whose active membership remains unresolved. Additional script/native-only units are explicitly retained in the coverage backlog.

## Control-reactor membership observed in m3/pom.xml

Default declared modules: indexdb; synexia-import; synexia-import/pure-int-recipe-custody; tooling/migration-recipes; tooling/synexia-jdk-a3-recipes; tooling/a3; tooling/backport-dag; backports; tooling/module-packs. `m3-extended` adds collections; `m3-tornado` adds ports/tornadovm-bulk. The two atom-pattern profiles configure authoring recipes, not a universal runtime dependency. Maven parent inheritance, effective profiles and dependency resolution were not executed.

| Workstream | Source | Boundary evidence | Documentation status |
| --- | --- | --- | --- |
| [OpenJDK runtime image](#project-1) | [src](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/Makefile) | OBSERVED / PARTIAL |
| [HotSpot native VM](#project-2) | [src/hotspot](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/hotspot) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/make/hotspot/HotspotCommon.gmk) | OBSERVED / PARTIAL |
| [M3 control reactor](#project-3) | [m3](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/pom.xml) | OBSERVED / PARTIAL |
| [Immutable text foundation](#project-4) | [m3/core](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/core) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/core/src/module-info.java) | OBSERVED / PARTIAL |
| [Immutable arrays](#project-5) | [m3/arrays](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays/pom.xml) | OBSERVED / PARTIAL |
| [Collection proving ground](#project-6) | [m3/collections](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/pom.xml) | OBSERVED / PARTIAL |
| [Index database receiver](#project-7) | [m3/indexdb](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/pom.xml) | OBSERVED / PARTIAL |
| [Migration recipe library](#project-8) | [m3/migration](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/migration) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/migration/pom.xml) | OBSERVED / PARTIAL |
| [Primitive collection ports](#project-9) | [m3/ports/collections](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/pom.xml) | OBSERVED / PARTIAL |
| [IndexString compatibility](#project-10) | [m3/ports/indexstring](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/pom.xml) | OBSERVED / PARTIAL |
| [Static precompute](#project-11) | [m3/ports/precompute](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/pom.xml) | OBSERVED / PARTIAL |
| [Compile-time inventory](#project-12) | [m3/ports/compiletime](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/pom.xml) | OBSERVED / PARTIAL |
| [Parallel kernel fabric](#project-13) | [m3/ports/parallel](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/pom.xml) | OBSERVED / PARTIAL |
| [Virtual CPU](#project-14) | [m3/ports/vcpu](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/pom.xml) | OBSERVED / PARTIAL |
| [Parallel bulk adapter](#project-15) | [m3/ports/parallel-bulk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel-bulk) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel-bulk/pom.xml) | OBSERVED / PARTIAL |
| [Tornado bulk adapter](#project-16) | [m3/ports/tornadovm-bulk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/tornadovm-bulk) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/tornadovm-bulk/pom.xml) | OBSERVED / PARTIAL |
| [Swing viewport receiver](#project-17) | [m3/ports/swing-viewport](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/pom.xml) | OBSERVED / PARTIAL |
| [Runtime integration gates](#project-18) | [m3/runtime-integration](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/runtime-integration) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/runtime-integration/README.md) | OBSERVED / PARTIAL |
| [Compatible backports](#project-19) | [m3/backports](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/backports) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/backports/pom.xml) | OBSERVED / PARTIAL |
| [Synexia receiver](#project-20) | [m3/synexia-import](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/pom.xml) | OBSERVED / PARTIAL |
| [Maven/OpenRewrite tooling](#project-21) | [m3/tooling](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/pom.xml) | OBSERVED / PARTIAL |
| [Opt-in text SDK modules](#project-22) | [m3/tooling/module-packs/text-tools](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/text-tools) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/text-tools/README.md) | OBSERVED / PARTIAL |
| [NLP/search module pack](#project-23) | [m3/tooling/module-packs/nlp-search](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/nlp-search) | [boundary evidence](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/nlp-search/README.md) | OBSERVED / PARTIAL |

## Workstream details

<a id="project-1"></a>
### OpenJDK runtime image

The Java platform modules, HotSpot VM, native libraries and tools form one matched JDK image. Java/native layouts, bootstrap, GC, JNI and serviceability must agree.

- Entry/build authority: [Makefile](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/Makefile)
- Build/run/output instructions: [doc/building.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/doc/building.md)
- Test/harness location: [test](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Inputs: boot JDK, platform toolchain and configure options. Outputs: selected build configuration, images and logs; see the contributor checklist. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-2"></a>
### HotSpot native VM

VM runtime, interpreter, JIT compilers, garbage collectors, class/object metadata and platform ports. These are native subsystems, not Java packages.

- Entry/build authority: [make/hotspot/HotspotCommon.gmk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/make/hotspot/HotspotCommon.gmk)
- Build/run/output instructions: [doc/hotspot-style.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/doc/hotspot-style.md)
- Test/harness location: [test/hotspot](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/hotspot) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Native ABI, safepoints, GC barriers, platform assumptions and matched Java layouts are acceptance boundaries. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-3"></a>
### M3 control reactor

Independent Maven admission, inventory and recipe/proof control plane. Its default modules and optional profiles do not define the OpenJDK runtime dependency graph.

- Entry/build authority: [m3/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/pom.xml)
- Build/run/output instructions: [m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/README.md)
- Test/harness location: [m3](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Java [21,22) and Maven >=3.9 are declared by the enforcer. Default reactor membership is enumerated below. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-4"></a>
### Immutable text foundation

The separately built com.m3.text module owns immutable text pieces and lexicon/precompute readers. It does not patch the installed JDK.

- Entry/build authority: [m3/core/src/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/core/src/module-info.java)
- Build/run/output instructions: [m3/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/README.md)
- Test/harness location: [m3/core/test](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/core/test) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: M3_JDK selects a private Java 21 installation for m3/build.sh; output and fixture-image scope are documented in the existing guide. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-5"></a>
### Immutable arrays

Immutable primitive snapshots, ranges and compositions; Java arrays retain normal identity/mutability and VM contracts.

- Entry/build authority: [m3/arrays/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays/pom.xml)
- Build/run/output instructions: [m3/arrays/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays/README.md)
- Test/harness location: [m3/arrays/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/arrays/src/test/java) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Optional native-linux profile adds a C11/CMake/JNI boundary. Presence of the fallback does not prove the native lane. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-6"></a>
### Collection proving ground

Independent primitive lanes and concrete collection APIs, including explicit concurrency, iteration and residency variants.

- Entry/build authority: [m3/collections/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/pom.xml)
- Build/run/output instructions: [m3/collections/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/README.md)
- Test/harness location: [m3/collections/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/collections/src/test/java) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: M3Collections is an explicit library API; this is not a universal java.util replacement. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-7"></a>
### Index database receiver

Filesystem artifact/ref storage and compatibility fingerprint owners. Reusable database mechanics remain Synexia-owned under the canonical porting policy.

- Entry/build authority: [m3/indexdb/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/pom.xml)
- Build/run/output instructions: [m3/docs/m3jdk21-porting-invariant.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/docs/m3jdk21-porting-invariant.md)
- Test/harness location: [m3/indexdb/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/indexdb/src/test/java) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Document file I/O, immutable reference semantics and partial storage-format scope before claiming DB completeness. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-8"></a>
### Migration recipe library

Source-bound additive materialization with hashes, locks and rollback receipts.

- Entry/build authority: [m3/migration/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/migration/pom.xml)
- Build/run/output instructions: [m3/migration/RESUME.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/migration/RESUME.md)
- Test/harness location: [m3/migration](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/migration) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Crash interruption is not a cross-file transaction; partially written or unowned outputs need explicit recovery review. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-9"></a>
### Primitive collection ports

Portable collection facades and exact Fenwick arithmetic, separated from JDK public contracts.

- Entry/build authority: [m3/ports/collections/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/pom.xml)
- Build/run/output instructions: [m3/ports/collections/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/README.md)
- Test/harness location: [m3/ports/collections/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/collections/src/test/java) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Original donor ownership and PROVENANCE remain binding; targets are isolated receivers. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-10"></a>
### IndexString compatibility

Retained-view M3Text facade and six original-name storage owners for stock-JVM compatibility experiments.

- Entry/build authority: [m3/ports/indexstring/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/pom.xml)
- Build/run/output instructions: [m3/ports/indexstring/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/README.md)
- Test/harness location: [m3/ports/indexstring/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/indexstring/src/test/java) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Do not place duplicate original/compatibility classes on the same loader path; materialization and equality boundaries are explicit. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-11"></a>
### Static precompute

Text geometry, exact bounded edit distance and candidate signals outside bootstrap String state.

- Entry/build authority: [m3/ports/precompute/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/pom.xml)
- Build/run/output instructions: [m3/ports/precompute/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/README.md)
- Test/harness location: [m3/ports/precompute/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/test/java) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Optional native parity profile; an assumed/skipped JNI test is not a passing native test. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-12"></a>
### Compile-time inventory

ASM-based constant/call-site inventory, regex requirement images and transient-ownership annotations.

- Entry/build authority: [m3/ports/compiletime/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/pom.xml)
- Build/run/output instructions: [m3/ports/compiletime/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/README.md)
- Test/harness location: [m3/ports/compiletime/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/compiletime/src/test/java) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Depends on ASM and m3-precompute; reflective Pattern access and CLI output directories are documented in the project guide. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-13"></a>
### Parallel kernel fabric

The m3.parallel module supplies backend selection, off-heap buffers, immutable kernel plans, transfer and parity receipts.

- Entry/build authority: [m3/ports/parallel/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/pom.xml)
- Build/run/output instructions: [m3/ports/parallel/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/README.md)
- Test/harness location: [m3/ports/parallel/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel/src/test/java) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Provider discovery is a service boundary. CPU-reference presence is not proof of accelerator availability or speedup. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-14"></a>
### Virtual CPU

m3.vcpu requires m3.parallel and provides RV32IM oracle/kernel/session machinery plus a separate native HAL/JNI tree.

- Entry/build authority: [m3/ports/vcpu/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/pom.xml)
- Build/run/output instructions: [m3/ports/vcpu/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/README.md)
- Test/harness location: [m3/ports/vcpu/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/vcpu/src/test/java) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: The README explicitly says a Maven native profile is not wired; native CMake/ctest instructions are a separate route. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-15"></a>
### Parallel bulk adapter

Optional providers bridge reviewed bounded operations into jdk.internal.vm.parallel without making java.base depend on the fabric.

- Entry/build authority: [m3/ports/parallel-bulk/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel-bulk/pom.xml)
- Build/run/output instructions: [m3/ports/parallel-bulk/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel-bulk/README.md)
- Test/harness location: [m3/ports/parallel-bulk/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/parallel-bulk/src/test/java) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Integer-add and bit-count sources are present; arbitrary Runnable offload is outside this contract. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-16"></a>
### Tornado bulk adapter

Optional RV32IM provider bridge with a separately supplied cpu-gpu-rv32im dependency.

- Entry/build authority: [m3/ports/tornadovm-bulk/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/tornadovm-bulk/pom.xml)
- Build/run/output instructions: [m3/ports/tornadovm-bulk/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/tornadovm-bulk/README.md)
- Test/harness location: [m3/ports/tornadovm-bulk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/tornadovm-bulk) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Enabled through m3-tornado after installing the exact provider; no default java.base dependency or universal GPU claim. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-17"></a>
### Swing viewport receiver

Packed model and proxy APIs over toolkit-neutral residency/swap components.

- Entry/build authority: [m3/ports/swing-viewport/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/pom.xml)
- Build/run/output instructions: [m3/ports/swing-viewport/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/README.md)
- Test/harness location: [m3/ports/swing-viewport/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/swing-viewport/src/test/java) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Headless tests, EDT behavior, native effects and real UI qualification are distinct. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-18"></a>
### Runtime integration gates

Source gates, recipe packets, harnesses and exact-runtime evidence for M3String integration.

- Entry/build authority: [m3/runtime-integration/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/runtime-integration/README.md)
- Build/run/output instructions: [m3/runtime-integration/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/runtime-integration/README.md)
- Test/harness location: [m3/runtime-integration/tests](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/runtime-integration/tests) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Historical stage1/segmented packets retain their original commits and are not installers for an arbitrary later checkout. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-19"></a>
### Compatible backports

Inventory/admission of later-JDK changes while locking Java 21 public semantics and target proof.

- Entry/build authority: [m3/backports/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/backports/pom.xml)
- Build/run/output instructions: [m3/backports/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/backports/README.md)
- Test/harness location: [m3/backports](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/backports) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: A pending backport candidate is not a supported Java language-version upgrade. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-20"></a>
### Synexia receiver

Source manifest and custody/admission tooling; canonical donor facts and target qualification remain distinct.

- Entry/build authority: [m3/synexia-import/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/pom.xml)
- Build/run/output instructions: [m3/synexia-import/current-full-borrow/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/current-full-borrow/README.md)
- Test/harness location: [m3/synexia-import/src/test/java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/synexia-import/src/test/java) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Original paths, source identities, per-file rights and pending target gates remain part of the receiver contract. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-21"></a>
### Maven/OpenRewrite tooling

Migration facades, A3, backport DAG, module inventory and other source-sealed authoring/proof tools.

- Entry/build authority: [m3/tooling/migration-recipes/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/migration-recipes/pom.xml)
- Build/run/output instructions: [m3/tooling/module-packs/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/README.md)
- Test/harness location: [m3/tooling](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Tool dependencies such as OpenRewrite belong to authoring/proof artifacts, not the java.base bootstrap graph. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-22"></a>
### Opt-in text SDK modules

Source-built jdk.m3.text and jdk.m3.io extension modules and their native/text I/O packaging route.

- Entry/build authority: [m3/tooling/module-packs/text-tools/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/text-tools/README.md)
- Build/run/output instructions: [m3/tooling/module-packs/text-tools/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/text-tools/README.md)
- Test/harness location: [m3/tooling/module-packs/text-tools/fixtures](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/text-tools/fixtures) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: These optional modules are outside Java SE --release 21. Refer to the exact source pack and coverage manifest; no ordered-runtime phase advancement. Full module/API/caller reconciliation and exact-head qualification remain open.

<a id="project-23"></a>
### NLP/search module pack

Source-build/packaging receiver for existing OpenNLP, Lucene and logging tool implementations.

- Entry/build authority: [m3/tooling/module-packs/nlp-search/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/nlp-search/README.md)
- Build/run/output instructions: [m3/tooling/module-packs/nlp-search/README.md](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/nlp-search/README.md)
- Test/harness location: [m3/tooling/module-packs/nlp-search](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/tooling/module-packs/nlp-search) (source location only; NOT_RUN)
- Packages: [complete source-directory ledger](packages/README.md), [M3 contracts](m3-packages.md) and [descriptor ledger](descriptors.md)
- Boundaries and gaps: Existing historical Linux receipts retain their scope; this pass did not rerun training, search, upstream suites or target JDK qualification. Full module/API/caller reconciliation and exact-head qualification remain open.


## Module-pack kernel workstream routing (2026-10-15)

Within [Maven/OpenRewrite tooling](#project-21), the [module-pack inspection kernel reference](module-pack-kernel-reference.md) now documents the four-owner artifact/inspector/graph/CLI contract, with [exact member, test-source and build/caller evidence](module-pack-kernel-source-coverage.md). The declared aggregation route is `m3/pom.xml` → `tooling/module-packs/pom.xml` → `kernel/pom.xml`; aggregation does not imply parent inheritance or an executed reactor.

This closes that bounded source-contract documentation gap only. It supplements the existing module-pack README and leaves recipe-family, assembler, [text SDK](#project-22), [NLP/search](#project-23), JavaFX and target image/runtime qualifications separate. Builds, tests and recipes were NOT_RUN in this documentation authoring/review pass.

Hosted CI is separate exact-commit/workflow evidence and does not change these execution or admission limits.
