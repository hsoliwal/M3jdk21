<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3 counted collection SDK — actual Java implementations, 2026-10-17

Canonical convergence authority: hsoliwal/com.synexia, `synexia-m3-recipe/recipes/m3-counted-sdk-20261017`.
Canonical public product/owner: hsoliwal/M3jdk21, `m3/collections` plus the separate optional `m3/collections/interop` module.

This package **adds actual runtime Java owners**, not additional names-only catalogue rows:

* `com.m3.collections.M3CountedBag<E>` — distinct-key counted storage delegated to existing first-party `M3LinkedHashMap<E,Integer>` (parallel Java reference arrays plus primitive hash/order/index lanes). One stored positive int count per distinct key, no entry object or boxed occurrence per repetition. Exact `long` total; `size()` saturates at `Integer.MAX_VALUE`; null keys, fail-fast occurrence/unique iterators, backed unique-set removal, explicit negative/overflow refusal, state-aligned optional existing JNI mask selection.
* `M3CountedBag.CountSnapshot<E>` — detached copy of distinct keys/counts with stable primitive-index sorting, comparator-equivalent ties, half-open offset slices, and a one-time cached filtered projection. Sorting reorders only `int[]` indexes, not the owned M3 map's values. No predicate crossing JNI.
* `com.m3.collections.interop.M3GuavaMultiset<E>` — implements the **real** `com.google.common.collect.Multiset<E>` API under the existing optional Guava dependency, with backed elementSet and entrySet, multiplicity-sensitive equality and hash, add/remove/setCount and count-producing iteration.
* `com.m3.collections.interop.M3ApacheMultiSet<E>` — extends Apache's public `AbstractMultiSet<E>` skeletal implementation and exposes the **real** `org.apache.commons.collections4.MultiSet<E>` contract, including Apache entrySet and uniqueSet semantics. Only one M3 counted storage owner is retained; temporary API entries are materialized at boundaries.

It does **not** pretend these API types are the same. Guava `Multiset`, Apache `MultiSet` and Eclipse `Bag` are separate contracts. Eclipse Bag and sorted/multimap variants remain explicitly deferred. Existing M3 filtered Lists, navigable sorted Sets/Maps, multi-value maps, primitives and SDK family catalogue remain unchanged. No added provider dependencies are needed: the optional interop POM already owns Guava and Commons Collections.

Source delivery is template-first: the six generated Java output files are SHA-256 pinned by Maven-bound `M3CountedSdkRecipe`. It admits ABSENT or byte-identical source; altered destination, symlink, missing template, or drift is refused. All outputs preflight before one is written. The actual Java templates remain canonical in Synexia; target mirror is proof custody. This is an additive generator; existing-source future changes must use a reviewed OpenRewrite AST rule, not hand-patch generated outputs.

## Local execution scope

* Java 21 direct compile: PASS `javac --release 21 -Xlint:all -Werror` for the core owner and both provider adapter source files against **explicitly marked local compile-only substitutes** for unavailable external jars and existing target dependencies.
* Core randomized/contract oracle: PASS **165184 checks**, including stable sorting, one-evaluation filtering, duplicate/null counts, count overflow, iterator mutations, and mask selection in JAVA mode. This isolated check uses test-only `M3LinkedHashMap` and `M3IndexMaskCompactor` stand-ins. It is NOT proof of real M3 owner or provider JAR integration.
* Maven-bound generator direct Java `--selftest`: PASS 6 exact sources, `--apply`, `--check`, idempotence, missing/drift refusal. Maven itself unavailable in the local runner.
* **NOT_RUN/PENDING:** actual Guava 33.4.8-jre/Apache Commons Collections 4.5.0 Maven dependency compilation and their differential JUnit tests, actual M3 map/JNI installed backend, full M3 module, JDK/jtreg/JCK, cross-platform memory/throughput. The target head workflow requires these gates before promotion.

## Algorithm catalogue and rights

`CATEGORY_REVIEW.tsv` compares existing M3 index-mask and linked map owners against Guava/Apache vendor semantics and LeetCode/HackerRank/GFG problem categories. Existing M3 production source is reused by dependency; no upstream source solution, provider implementation, JDK class, or third-party binary has been copied into these templates. All authored code Copyright 2026 Hitesh Soliwal and contributors, Apache-2.0. Other providers retain their own licenses and module dependencies; recipe license never relicenses upstream source.

The product receiving phase stays `WAITING_FOR_STRING`; no `java.util` rewrite is claimed, no rebase of Synexia `develop`, and no default-branch write.
