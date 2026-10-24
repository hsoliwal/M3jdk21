<!--
Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0, or (at your option) the Eclipse
Public License 2.0, which are available at
https://www.apache.org/licenses/LICENSE-2.0 and
https://www.eclipse.org/legal/epl-2.0/

SPDX-License-Identifier: Apache-2.0 OR EPL-2.0

Contributors:
    Hitesh Soliwal (Synexia) - initial API and implementation
-->

# M3 collections naming

These conventions apply to code that is added to or received into `m3/collections`. New names
follow rules R1 to R12. Existing names that differ keep their paths; they are listed under
Aliases and Exceptions.

## Provider tokens and packages

| Token | Upstream package root | M3 package root |
|---|---|---|
| `jdk` | `java.util` | `com.m3.collections.jdk.util` |
| `guava` | `com.google.common` | `com.m3.collections.guava` |
| `apache` | `org.apache.commons.collections4` | `com.m3.collections.apache` |
| `spring` | `org.springframework.util` | `com.m3.collections.spring` |
| `eclipse` | `org.eclipse.collections` | `com.m3.collections.eclipse` |
| `jackson` (interop only) | `com.fasterxml.jackson.datatype` | `com.m3.collections.jackson` |

The upstream remainder is kept: `com.google.common.collect` becomes `com.m3.collections.guava.collect`,
and `org.apache.commons.collections4.bag` becomes `com.m3.collections.apache.bag`.

## Rules

| Rule | Convention |
|---|---|
| R1 | Provider tokens are the closed list `jdk`, `guava`, `apache`, `spring`, `eclipse`. `jackson` is an interop-only token. The Apache token stays `apache` and its packages are not renamed; `commons-collections` is an alias of it. |
| R2 | A received source dir is `implementations/<upstream-project>-<X.Y.Z>[-<slice>]`, where X.Y.Z is the full release version of the received tree. Only release tags are received, never snapshots. A flavour qualifier such as `-jre` is dropped only when every flavour is received. The evidence dir is `receiving/<same name>-<yyyymmdd>`. |
| R3 | Each provider has one package prefix substitution, as in the table above. The `com.m3.sdk.*` scheme is retired: [`relocation/five-provider-20261018`](relocation/five-provider-20261018/README.md) is kept as history, and the commons-collections fork branch `m3/package-relocation-20261018` is superseded. |
| R4 | A module name is the R3 substitution applied to the upstream module name or automatic module name. The artifact suffix is used only when upstream declares no name. |
| R5 | One provider package is one module. Owned and scaffold shapes ship inside their provider's module, never as named per-shape modules. Every POM that gains a `module-info` adds `-Xlint:-module`. |
| R6 | The OSGi Bundle-SymbolicName is the module name, and Export-Package lists the relocated roots. The donor GAV stays as Maven provenance, with install and deploy skipped, until a target-owned build exists. |
| R7 | A target-owned build is `com.m3.collections:m3-<upstream-artifactId>:<upstream-version>-m3-source-<n>`. Shape artifacts keep `m3-owned-<dir>` and `m3-scaffold-<dir>`. New artifacts use groupId `com.m3.collections`. The existing `com.m3:*` coordinates stay, because consumers use them. |
| R8 | First-party classes carry the `M3` prefix everywhere, including inside relocated donor trees. Received classes keep their upstream names. Interop classes are `M3<Token><Shape>`. A provider-package class is `M3<UpstreamSimpleName>`. Each M3 simple name has one owner across `com.m3.collections.**`. |
| R9 | A new shape dir is `<token>-<kebab(UpstreamSimpleName)>`, and its recipe crate uses the same dir name. Acronyms form one token (`lru`); compounds are split (`bi-map`, `multi-value`, `min-max`). |
| R10 | The fork branch for a receive is `m3/<upstream-project>-<X.Y.Z>-namespace-source-<yyyymmdd>`, cut from the release tag. |
| R11 | The provenance class is a field in the inventory and the README, not part of the artifact name. `owned` is a first-party body (the shape is borrowed, the code is ours), `adapted` means the NOTICE lists donor code, and `scaffold` is surface only. The `<token>-` prefix names where the shape comes from, not where the code comes from. |
| R12 | Challenge references use `LC<n>` and `HR_<SLUG>` ids in catalogue rows, with the technique in its own column. Challenge ids never appear in public class names. New rows go under [`algorithm-catalogue/`](algorithm-catalogue); [`algorithm-catalog/`](algorithm-catalog) is frozen. |

## Names at a glance

| Item | Pattern | Example |
|---|---|---|
| Received source dir | `implementations/<upstream-project>-<X.Y.Z>[-<slice>]` | [`implementations/guava-33.4.8`](implementations/guava-33.4.8), [`implementations/spring-core-6.2.0-collections`](implementations/spring-core-6.2.0-collections) |
| Receive evidence dir | `receiving/<received dir>-<yyyymmdd>` | [`receiving/guava-33.4.8-20261018`](receiving/guava-33.4.8-20261018) |
| Fork branch of a receive | `m3/<upstream-project>-<X.Y.Z>-namespace-source-<yyyymmdd>` | `m3/guava-33.4.8-namespace-source-20261018` in the Guava fork |
| Shape dir | `implementations/<token>-<kebab(UpstreamSimpleName)>` | [`implementations/apache-hash-bag`](implementations/apache-hash-bag) holds `M3HashBag` |
| Shape artifact | `com.m3.collections:m3-owned-<dir>` or `com.m3.collections:m3-scaffold-<dir>` | `m3-owned-guava-enum-multiset` |
| Owned-body recipe crate | `recipe/owned-<dir>-<yyyymmdd>` | [`recipe/owned-guava-enum-multiset-20261019`](recipe/owned-guava-enum-multiset-20261019) |
| Technique recipe crate | `recipe/<technique>-<yyyymmdd>` | [`recipe/keyed-ordered-ring-20261019`](recipe/keyed-ordered-ring-20261019) |
| First-party class | `com.m3.collections.<token>.M3<UpstreamSimpleName>` | `com.m3.collections.guava.M3EnumMultiset` |
| Interop class | `com.m3.collections.interop.M3<Token><Shape>` | `com.m3.collections.interop.M3ApacheBag` |
| Challenge catalogue dir | `algorithm-catalogue/challenge-patterns-<yyyymmdd>/` | [`algorithm-catalogue/challenge-patterns-20261018`](algorithm-catalogue/challenge-patterns-20261018) |
| Anchor on a refined method | `//FLLDCIM: ALG — <id> <technique>` | `//FLLDCIM: ALG — LC189 rotate` |

A receive evidence dir holds `FOSS_REUSE_DECISION.tsv`, `FULL_SOURCE_BINDINGS.tsv`,
`NAMESPACE_EXCEPTION.json`, `README.md`, `SOURCE_QUALIFICATION.json`, `TARGET_DISPOSITION.json`,
`THIRD_PARTY_NOTICES.md` and `TRANSFORMATION_CATALOGUE.json`.

New challenge catalogue rows have the columns id, technique, incumbent M3 owner
(`com.m3.collections.<token>.M3<Shape>`), decision and proof gate. A challenge never changes a
class name.

## Aliases

Existing paths keep their names. The canonical name is the one that a re-receive or a new
revision of the item uses.

| Current name | Canonical name | Holds |
|---|---|---|
| [`implementations/apache-circular-fifo`](implementations/apache-circular-fifo) | `apache-circular-fifo-queue` | `M3CircularFifoQueue` |
| [`implementations/guava-minmax-priority-queue`](implementations/guava-minmax-priority-queue) | `guava-min-max-priority-queue` | `M3MinMaxPriorityQueue` |
| [`implementations/spring-case-insensitive-map`](implementations/spring-case-insensitive-map) | `spring-linked-case-insensitive-map` | `M3LinkedCaseInsensitiveMap` |
| [`implementations/jdk21-package-renamed`](implementations/jdk21-package-renamed) | `jdk-21-collections` | `java.util` sources in `com.m3.collections.jdk.util` |
| token `commons-collections` | `apache` | [`implementations/commons-collections-4.5.0`](implementations/commons-collections-4.5.0), packages `com.m3.collections.apache` |
| token `jackson` | interop-only token, not a provider | packages under `com.m3.collections.jackson` |

Challenge ids:

| Id in existing files | Canonical id | Technique column |
|---|---|---|
| `LC0189_ROTATE` | `LC189` | `rotate` |
| `LC0307_FENWICK` | `LC307` | `fenwick` |
| `HR-AVL` | `HR_AVL` | - |

Sealed files are not rewritten; join them to new rows through this table. `LC0189_ROTATE` and
`LC0307_FENWICK` occur in [`algorithm-catalog/challenge-forks-20261018`](algorithm-catalog/challenge-forks-20261018),
[`algorithm-catalog/netbeans-collection-atoms-20261019.tsv`](algorithm-catalog/netbeans-collection-atoms-20261019.tsv)
and [`qualification/netbeans-collection-atoms-20261019`](qualification/netbeans-collection-atoms-20261019).
`HR-AVL` occurs in
[`algorithm-catalogue/challenge-patterns-20261018/PRIMARY_SOURCES.tsv`](algorithm-catalogue/challenge-patterns-20261018/PRIMARY_SOURCES.tsv).

## Exceptions

| Name | Rule | Handling |
|---|---|---|
| The 24 `M3Apache*` classes in [`implementations/apache`](implementations/apache), for example `M3ApacheCollectionUtils` | R8: a provider-package class is `M3<UpstreamSimpleName>` | Kept as they are. Recipe crates pin these files by hash. |
| `M3ConcurrentReferenceHashMap` and `M3MultiValueMap`, each in both `com.m3.collections.apache` and `com.m3.collections.spring` | R8: one owner per M3 simple name | Not renamed; the packages keep them apart. Import them with explicit single-type imports, never with wildcard imports of both packages. |
| Root `com.m3.collections.M3EnumBiMap`, a superset scaffold | R8: one owner per M3 simple name | Superseded by the owned body `com.m3.collections.guava.M3EnumBiMap` in [`implementations/guava-enum-bi-map`](implementations/guava-enum-bi-map). Neither class is renamed. |
| [`algorithm-catalog/`](algorithm-catalog) beside [`algorithm-catalogue/`](algorithm-catalogue) | one directory per area | `algorithm-catalog/` is frozen, because existing files and workflows read its paths. New rows go under `algorithm-catalogue/`. |
| [`recipe/`](recipe) beside [`recipes/`](recipes) | one directory per area | Both are kept. |

## Checklist

| Adding | Names to settle |
|---|---|
| An owned or scaffold shape | dir (R9), class (R8), artifact (R7), provenance class (R11). Check that the simple name is free: `git ls-files -- 'm3/collections/*/M3<Name>.java'` must print nothing. |
| A full-source receive | fork branch (R10), `implementations/` dir and `receiving/` dir (R2), packages (R3), module (R4), OSGi and Maven identity (R6, R7) |
| A challenge-driven refinement | catalogue row under `algorithm-catalogue/` (R12), recipe crate `recipe/<technique>-<yyyymmdd>` or `recipe/owned-<dir>-<yyyymmdd>`, anchor on the method |
