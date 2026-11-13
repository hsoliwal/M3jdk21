# JEP 491 monitor-unpinning materialization packet

Status: executable materialization substrate; no product/runtime acceptance claim.

## Source authority

- target: M3JDK21
- exact branch base: c0a14387009aefc7d62bd3268055d526e04f9074
- upstream implementation: OpenJDK commit 78b80150e009745b8f28d36c3836f18ad0ca921f (JDK-8338383)
- upstream parent: 8a2a75e56de4497da48f43b3be3eb71bf3ef75ab
- exact implementation denominator: 246 touched paths
- scope floor: MULTI_MODULE
- public Java API delta: none
- semantic plane: HotSpot monitors/continuations/JNI/JVMTI/JFR/SA + java.base virtual-thread support

## Invariant

Do not bulk-copy this change. Decompose the upstream commit to one source-sealed FILE atom per touched path, classify every atom by architecture/domain, and recombine through a deterministic DAG. Deletions/renames remain explicit typed operations; they are never inferred from absence.

## Required follow-up reconciliation

Before freezing product candidates, classify these already pinned follow-ups:
JDK-8344247, JDK-8346120, JDK-8345543, JDK-8346792, JDK-8349689.
Each must be ABSORB, PROVEN_IRRELEVANT, or SEPARATE_LATER_CHANGE.

## Mechanical proof stages

1. verify exact 246-path denominator from the upstream commit;
2. verify checked-in PATHS.txt is byte-for-byte the sorted upstream path set;
3. classify every path into shared-runtime, architecture, java-base, JNI/JVMTI, JFR/SA, or test domains;
4. generate crate-size=1 source-sealed recipe atoms using the existing M3 generator, including native/text files;
5. account for modified/added/removed/renamed paths explicitly;
6. emit deterministic ARCH_DAG.tsv and DOMAIN_COUNTS.tsv;
7. reconcile follow-up commit intersections against the 246-path graph;
8. verify existing M3 backport/recipe substrate;
9. only after green atom proof may a later packet materialize product source;
10. product acceptance still requires matched fastdebug/release builds, per-architecture compile lanes, virtual-thread/JVMTI/JFR/SA jtreg, JNI pinned-section controls, stress, and performance evidence.

No compiler/test/build gate may be weakened to admit the packet.
