<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Canonical Synexia recipe: M3 five-fork prepared selection (2026-10-18)

M3JDK21 is the canonical **product** and Java/JNI SDK owner. Synexia remains the Maven/OpenRewrite recipe and donor-convergence owner. The five source forks are **hsoliwal/M3jdk21, hsoliwal/guava, hsoliwal/eclipse-collections, hsoliwal/commons-collections, hsoliwal/spring-framework**; `FORK_DONORS.tsv` pins exact revisions, source blobs, namespaces and rights. Never rebase Synexia `develop`.

## Actual additive implementation
- New `M3FiveFamilyPreparedSelection` operates over the **existing** `M3FiveLibraryIndexViews` M3 arrays, physical source-index masks and immutable snapshots. It never introduces a second value owner, per-row Entry/Node or JNI kernel.
- Select each logical *occurrence* once by Java `BiPredicate` or unsigned state masks, retain a dynamically sized `BitSet`; combine via AND/OR/XOR/AND-NOT/NOT and refine only previously accepted occurrences.
- Memoize Java projection as primitive `int[]` original ordinals. Sorted projection uses stable mergesort of ordinal IDs; half-open slice shares the index and value owner.
- Independent **typed** output materialization reuses existing M3 factories: `java.util.List/Map`; Guava `ListMultimap`; Eclipse Collections `MutableListMultimap`; Apache Commons `ListValuedMap`; Spring `MultiValueMap`. Their cardinality, duplicate and absent-key semantics remain separate.
- Explicit native projection reuses `M3IndexMaskCompactor.select` with int[] ordinals + byte[] masks, without callbacks or Java object pointers. The full fork-relocated Eclipse/Apache implementation classes are **not yet** the M3JDK21 runtime dependencies: their `com.m3.collections.eclipse/apache` namespaces differ from upstream interface jars. No binary equivalence claim.
- Original JDK/Guava/Spring names stay original. JDK source GPLv2 with Classpath Exception and Eclipse EPL/EDL rights are **not** relicensed by this Apache-2.0 first-party code.

## Recipe-first custody
Maven-bound `M3FiveForkPreparedRecipe` installs exactly THREE new Java source/test files from SHA-256-sealed templates and preflights source identity, symlinks and occupied/drift targets before any write. It reaches a fixed point and refuses changes to existing user-written Java. The initial task has no existing Java AST replacements; qualified OpenRewrite must own any later semantic modifications to existing classes.

Generated source SHA-256: `8b9ba8edf03de5ccce15b1be104ba846b53a5e39fcda15bdb22a741d9575db04`.
Generated selftest SHA-256: `9ceb788e6c6cdca0596879eff680cb6fb4591663a7379f868d4f1b0afd5bbd52`.
Generated Jupiter test SHA-256: `2113f5a2dc439f59e653127900c01ac55ae547d05a93436ed46c7ca269220bf7`.

## Actual local evidence and qualifications
- Java 21 `javac --release 21 -proc:none -Xlint:all -Werror`: PASS selected implementation, oracle, JUnit test syntax, with **temporary standalone M3/third-party signature stand-ins**, not actual live provider binaries.
- Seeded logical-index differential `M3FiveFamilyPreparedSelectionSelfTest`: PASS **34,932 assertions** over 500 seeds. This is Java-only stand-in evidence; no real fork binary compile or native mode was executed locally.
- **Exact recipe implementation** direct Java `--selftest`: PASS missing/drift refusal, twice apply, fixed point, 3 exact emitted bytes.
- Local Maven is not installed. Full M3JDK21 core+optional interop Maven/JUnit, checked real JNI, five fork Git blob readback and fork source builds, full JDK/JCK/jtreg, donor API closure, licensing per output, performance & installed-image smoke are **PENDING / HOLD**.
- The user-requested complete five-provider SDK superset is a continuing acceptance goal, not a result of this one add-on. No automatic merge or String→arrays→collections stage promotion.

References: LeetCode, HackerRank, GeeksforGeeks are **category evidence only**, no challenge solution copied. New recipe/templates ©2026 Hitesh Soliwal and contributors, Apache-2.0. Preserve all donor notices in future actual source intake.
