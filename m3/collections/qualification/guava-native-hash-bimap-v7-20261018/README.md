<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native Guava HashBiMap qualification companion

This receiving increment records the qualified native concrete Guava HashBiMap
and composes the earlier native ArrayListMultimap/M3 core variant. It changes
qualification metadata only. The 107-row FAMILY_CATALOG.tsv is unchanged;
the HashBiMap row remains owner `-`, disposition `PENDING`. No M3BiIndexMap
alias or java.base implementation is installed.

Authoritative mapping storage uses parallel payload, hash, order, link, index,
state, free, generation, retirement and pin arrays with primitive offsets and
mask. Structural Entry/Node objects are absent. Entry APIs alone create the
views managed by a bounded 128-slot M3LFUCache adaptation. Slot generation and
creator-iterator lease preserve detached snapshots and creator-only healing;
escaped objects are not retargeted or shared across incompatible iterators.

The full report records 10,601 sole-provider tests (4,533 HashBiMap, 6,064
ArrayListMultimap and four storage tests), strict Java 21 source compilation,
public/protected ABI, UIDs, callback/reentry traces, independent-JVM streams,
classpath/named-module checks, LFU12,000 and callback churn10,000. The final
complete artifact keeps `com.google.common@33.4.8-jre`, the original module
requirements/exports/opens/services and all unrelated Guava payloads.

Report SHA-256: `1a3c4cb47de723d59fd64c756b9a1ea14158847455a4521b7dd992c077ce0a4f`.
Artifact SHA-256: `0d1f77f56a15a9c6c91e3faca6b598e8bff5b17ecb76c323feff9c427351539a`.
Canonical recipe/proof crate: https://github.com/hsoliwal/com.synexia/tree/03de1b62d58a98972e42eb09fae7635ff007c1e0/synexia-openrewrite-recipes/recipe-crates/guava-native-hash-bimap-v7-20261018.
Canonical draft: https://github.com/hsoliwal/com.synexia/pull/12348.

This companion stacks on M3 PR #1307, branch
`codex/class-instance-registration-20261018-3bebc12f`, commit
`cde88a7a565ce1701038974199896c05dda7f04e`; previous BiMap and
ClassToInstanceMap registrations remain unchanged. The Guava companion builds
on merged PR #4 and the current compatible baseline, not its closed topic branch.

SOURCE_PROVENANCE.tsv and two recipe manifests bind the actual emitted native
Guava source and POM. The source is received by the separate Guava fork;
this repository receives custody and qualification records only.
Configured autoreview remains NO_VERDICT after its mandatory no-tools refusal;
hosted CI and draft review remain pending. No performance, universal resource-OOM
or whole-superset completion claim is made.

The native Guava receiver is now published as draft [Guava PR #7](https://github.com/hsoliwal/guava/pull/7)
at commit `5f4ae26694bbccaa956d16bda69ed87d6b4fa694`. The source and POM are directly reviewable:

- [HashBiMap.java](https://github.com/hsoliwal/guava/blob/5f4ae26694bbccaa956d16bda69ed87d6b4fa694/m3-jre21/src/main/java/com/google/common/collect/HashBiMap.java).
- [Qualified module POM](https://github.com/hsoliwal/guava/blob/5f4ae26694bbccaa956d16bda69ed87d6b4fa694/m3-jre21/pom.xml).

Their exact Git blobs and SHA-256 values are recorded in SOURCE_PROVENANCE.tsv
and the native-fork record. Publication/readback does not imply completed hosted CI,
configured review or merge.
