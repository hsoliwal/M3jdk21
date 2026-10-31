<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Internal mindex source coverage

[Guide](README.md) · [Runtime boundaries](runtime-owners.md) · [Coverage](coverage.md)

This is a source-contract routing map for the physical `src/java.base/share/classes/jdk/internal/mindex` package in `java.base`. The complete, unfiltered public directory listing at `e7d21edf0f998c3c53ab0d0909972a4a97dfc471` contains **24 files, all 24 Java sources listed below**. It contains no subdirectories, non-M3-prefixed files, non-Java entries or `package-info.java` at that pin. That absence was checked from the full directory listing, not inferred from an M3-name filter. All 24 blobs match the earlier P03 path inventory at `278b221e8365ebeb6095a3a5643b38a0564258b5`.

[Inspect the exact package listing](https://github.com/hsoliwal/M3jdk21/tree/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/src/java.base/share/classes/jdk/internal/mindex). These are physical file identities, not a count of public APIs, nested classes, independently tested features or admitted runtime replacements. The package is not exported by the [java.base descriptor](../../src/java.base/share/classes/module-info.java).

## Exact file-to-contract map

The linked references account for each complete declared source body, including its nested types and internal helpers, with explicit behavior/error/lifetime limits. Source-body routing is complete for these 24 files; method-level executable coverage, inherited JDK compatibility, whole-module behavior and runtime qualification remain separate and incomplete. Tests were read as source evidence; this documentation pass did not execute repository builds, tests, verifiers or benchmarks.

| Physical Java source | Exact Git blob | Contract reference |
| --- | --- | --- |
| [M3Address28.java](../../src/java.base/share/classes/jdk/internal/mindex/M3Address28.java) | `14cdb999f5eab5be443044bff2d273fb4e4113fb` | [Logical address arithmetic](mapped-string-storage-reference.md#m3address28-and-m3bits-logical-arithmetic) |
| [M3BitLane28.java](../../src/java.base/share/classes/jdk/internal/mindex/M3BitLane28.java) | `5942b1f46eedf83ad8c4ef8edee91f75e7424e7a` | [Packed segmented bits and lazy rank/select](internal-metadata-lanes-reference.md#segmented-bit-lane) |
| [M3Bits.java](../../src/java.base/share/classes/jdk/internal/mindex/M3Bits.java) | `2ad46058a5325483647af4af16858c329ae24665` | [Unchecked bit/word arithmetic](mapped-string-storage-reference.md#m3address28-and-m3bits-logical-arithmetic) |
| [M3Boundary.java](../../src/java.base/share/classes/jdk/internal/mindex/M3Boundary.java) | `f10f815fc1a59a0d1ea7ed45158b89178f128a1a` | [Optional weak Class/loader boundary](mapped-string-storage-reference.md#m3boundary-optional-weak-jvm-metadata) |
| [M3CB.java](../../src/java.base/share/classes/jdk/internal/mindex/M3CB.java) | `0be655b7020f13f3df4265f68daf189bd039d86b` | [Explicit bridge modes, declarations and retirement](internal-metadata-lanes-reference.md#class-bridge-m3cb) |
| [M3CI.java](../../src/java.base/share/classes/jdk/internal/mindex/M3CI.java) | `2acac4ad654a8af823fa46b483a42dd2f9c894c4` | [Reflective closure and frozen metadata lanes](class-index-reference.md) |
| [M3Class.java](../../src/java.base/share/classes/jdk/internal/mindex/M3Class.java) | `b95eee3c820f6054b4f70507dee101c9f456302f` | [Class owner/row views and live boundaries](class-member-view-reference.md#m3class-metadata-and-hierarchy) |
| [M3ClassFlags.java](../../src/java.base/share/classes/jdk/internal/mindex/M3ClassFlags.java) | `fc2152f63f4c404bce630b043d9138eaf96cb93e` | [Class flag vocabulary](class-member-view-reference.md#flags-and-kind-codes) |
| [M3Constructor.java](../../src/java.base/share/classes/jdk/internal/mindex/M3Constructor.java) | `4d708115e139a4ab938a4f9abd856f7fc98c5c00` | [Typed constructor projection](class-member-view-reference.md#member-views) |
| [M3Descriptor.java](../../src/java.base/share/classes/jdk/internal/mindex/M3Descriptor.java) | `92f4d7da32a3b8265675a413311fd304c6dc2dd2` | [VM descriptor encoding](internal-metadata-lanes-reference.md#descriptor-writer-m3descriptor) |
| [M3Field.java](../../src/java.base/share/classes/jdk/internal/mindex/M3Field.java) | `fad031da70ce813f52e5fc06479542a224bcd81a` | [Typed field projection](class-member-view-reference.md#member-views) |
| [M3IntLane28.java](../../src/java.base/share/classes/jdk/internal/mindex/M3IntLane28.java) | `513a1e8f5d55c074250d3b2747f041c19a1128e7` | [Segmented integer lanes](internal-metadata-lanes-reference.md#segmented-int-and-long-lanes) |
| [M3LongLane28.java](../../src/java.base/share/classes/jdk/internal/mindex/M3LongLane28.java) | `325e8e90ace21e71069665b74585c522b2c627b4` | [Segmented long lanes](internal-metadata-lanes-reference.md#segmented-int-and-long-lanes) |
| [M3MappedStringBacking.java](../../src/java.base/share/classes/jdk/internal/mindex/M3MappedStringBacking.java) | `c2dc2371c730aa758ccc45e06ba4ab95661e5593` | [Mapped-image admission, views and lifetime](mapped-string-storage-reference.md#opening-a-mapped-textbyte-snapshot) |
| [M3Member.java](../../src/java.base/share/classes/jdk/internal/mindex/M3Member.java) | `d5f8e07afc304379d5165c9c5c1659855938b5b0` | [Generic declared-member owner/row view](class-member-view-reference.md#member-views) |
| [M3MemberFlags.java](../../src/java.base/share/classes/jdk/internal/mindex/M3MemberFlags.java) | `f68a952217d3eead4ed10a43de49c240d6b790f1` | [Member flag vocabulary](class-member-view-reference.md#flags-and-kind-codes) |
| [M3MemberKind.java](../../src/java.base/share/classes/jdk/internal/mindex/M3MemberKind.java) | `d211e6bc1e72ccc8e0ff821d26b3e30361750bc9` | [Four member-kind codes](class-member-view-reference.md#flags-and-kind-codes) |
| [M3Method.java](../../src/java.base/share/classes/jdk/internal/mindex/M3Method.java) | `6bc2fe9176bdd9c95d732ecad105f03b62f4385e` | [Typed method projection](class-member-view-reference.md#member-views) |
| [M3PC.java](../../src/java.base/share/classes/jdk/internal/mindex/M3PC.java) | `b63fd9e457f7ac16bca8bf07dcb564069fdfef79` | [Ordered VM-local candidate context key](internal-metadata-lanes-reference.md#candidate-context-key-m3pc) |
| [M3RecordComponent.java](../../src/java.base/share/classes/jdk/internal/mindex/M3RecordComponent.java) | `55f82a8486055d50e24d1a9b12b0636f9db876ed` | [Typed record-component projection](class-member-view-reference.md#member-views) |
| [M3Release.java](../../src/java.base/share/classes/jdk/internal/mindex/M3Release.java) | `1116f4cb0ffd361e764688640adb53af8cabe86d` | [Exact declared release provenance](mapped-string-storage-reference.md#m3release-exact-caller-declared-provenance) |
| [M3StringBacking.java](../../src/java.base/share/classes/jdk/internal/mindex/M3StringBacking.java) | `5daedbe16dfc9377783b52d932e548cc44f0b335` | [Backing interface/default contracts](mapped-string-storage-reference.md#m3stringbacking-transport-neutral-surface) |
| [M3TQ.java](../../src/java.base/share/classes/jdk/internal/mindex/M3TQ.java) | `533a07b77061e757d476627557413f6df1067d34` | [Necessary-condition queries and exact trigram sets](trigram-query-reference.md) |
| [M3VI.java](../../src/java.base/share/classes/jdk/internal/mindex/M3VI.java) | `087aa55bef9b57189d03f3388fb2e1f360320b02` | [Version parsing/order/range queries and Scheme limits](internal-metadata-lanes-reference.md#version-index-m3vi) |

## Why five references are needed

- [Mapped String storage](mapped-string-storage-reference.md) owns six backing/address/boundary/release bodies, actual image/header validation and selected native consumer boundaries
- [Class/member views](class-member-view-reference.md) owns nine class/member/flag/kind bodies and distinguishes frozen metadata from optional live reflection
- [Internal lanes and bridge helpers](internal-metadata-lanes-reference.md) owns seven segmented-lane, bridge, descriptor, context-key and version-index bodies
- [Class-index snapshots](class-index-reference.md) owns M3CI's live-root admission, immutable primitive projection, ordering, weak round trips and implementation-defined fingerprint
- [Trigram query/facts](trigram-query-reference.md) owns M3TQ's candidate-only Boolean queries and exact UTF-16 membership/composition; it has no persisted image decoder

The six-owner mapped-storage reference is an earlier documentation increment. The remaining eighteen owners are added by the class/member, lanes/helpers and index/query references. Their shared dependency reads are not counted again as new primary-body coverage. These references also link the separately reviewed bootstrap String and Pattern integration guides without claiming to audit every member of those much larger JDK classes.

## Boundaries that a file count cannot close

1. Preserve owner identity: mapped coordinates, snapshot rows/loader spaces, context keys, version rows and trigram keys have different meanings. Equal integers or hashes do not provide a universal identity or cross-image resolver
2. Preserve authority: public Class/reflection, String/Pattern/Matcher, JVM class loading/linking/initialization and array semantics remain their original owners. Explicit internal bridges do not imply universal replacement or automatic attachment
3. Preserve admission: mapped-image version checks, caller Scheme revision checks, Class snapshot construction and trigram fact budgets are separate mechanisms. There is no shared metadata-image format introduced by this package map
4. Preserve evidence: source-reading and finite historical receipts are not current matched-JDK build/jtreg/native/GC/CDS/JVMTI/JFR/platform passes or CPU/heap/native-memory measurements. Every linked guide states its execution and behavior gaps
5. Preserve policy: [AGENTS](../../AGENTS.md), the [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md), [name map](../../m3/docs/name-mapping.json), applicable licenses and full-family receiver remain authoritative. The ordered receiving phases are not advanced by this documentation

The earlier [runtime overview](runtime-owners.md) and [coverage ledger](coverage.md) retain their historical sampled-scope statements. This map is an additive update to this exact package's source-body documentation routing, not a rewrite of historical evidence or a declaration of repository completion. Newly added source files, changed blobs, new consumers or new lifecycle assumptions require a new review rather than inheriting this status automatically.
