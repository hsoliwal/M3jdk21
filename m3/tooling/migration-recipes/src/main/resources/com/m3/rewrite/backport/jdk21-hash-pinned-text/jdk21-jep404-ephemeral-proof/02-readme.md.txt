# JEP 404 — Generational Shenandoah (Experimental)

Status: integration/GA inventory plus ephemeral candidate materialization/build proof. No canonical
product mutation, compatibility, runtime-equivalence or promotion claim.

## Upstream authority

- JEP: 404
- JBS integration issue: 8337511
- exact OpenJDK integration commit:
  `28ae281b42cd00f471e275db544a5d23a42df59c`
- integration commit date: 2024-11-29
- exact integration denominator: **228 paths**
- final donor state for candidate recipe generation: `jdk-24+36`
- Java-21 baseline: `jdk-21+35`

The exact 228 paths are recorded in `INTEGRATION_PATHS.txt`. They were taken from the upstream
integration commit, not inferred from directory names.

## Why final-GA postimages are required

The integration commit is not treated as the final donor snapshot. Generational Shenandoah
continued to receive fixes after integration. The inventory therefore compares every integration
path against both:

1. the exact JEP-404 integration commit; and
2. JDK 24 GA.

The reusable `selected_path_inventory.py` join classifies each integration path as:

- `MATERIALIZE_GA_POSTIMAGE`;
- `ALREADY_EQUIVALENT`;
- `GA_REMOVED_HOLD`;
- `GA_REMOVAL_HOLD`.

No removed path is silently reintroduced or deleted.

## Recipe-first lane

Only after the selected-path inventory is internally consistent does CI invoke the existing
`generate_recipe_crates.py` with:

```text
--crate-size 1
--include-native
--include-text
```

This produces one physical FILE recipe atom for each admitted final-GA delta:

- Java -> typed/hash-pinned OpenRewrite Java snapshot recipe;
- C/C++/headers -> source-sealed native FILE atom;
- other strict UTF-8 source/resource files -> source-sealed text FILE atom.

The generated crates remain candidate evidence. CI now applies them only to the ephemeral GitHub
Actions checkout through `materialize_generated_crates.py`. Exact current-tree preimages are
verified before application, only generated FILE targets may change, the complete postimage is
verified, and a second application must leave the candidate diff hash unchanged.

This does **not** authorize committing the generated product-source postimages to `master`.

## Scope

File atoms are independently reviewable and mechanically parallel. JEP 404 itself is not FILE
scope. It spans:

- HotSpot Shenandoah runtime;
- CPU-specific barrier implementations;
- serviceability-agent/JFR integration;
- management/diagnostic surfaces;
- gtest/jtreg/JNI/JVMTI/JFR tests.

The honest feature scope is therefore `MULTI_MODULE`.

## Required proof before source mutation

1. exact 228-path integration denominator;
2. JDK24-GA classification for all 228 paths;
3. one-file recipe-crate generation with explicit exclusions;
4. dependency closure against the current Java21 HotSpot/Shenandoah baseline;
5. architecture-by-architecture barrier review;
6. exact source-sealed candidate materialization;
7. Java21 HotSpot/Shenandoah build;
8. legacy non-generational Shenandoah regression proof;
9. opt-in generational mode jtreg/runtime/management/JFR/JNI/JVMTI proof;
10. fixed point;
11. serial promotion receipt.

Until those gates close, JEP 404 stays `candidate-high-risk` / `NOT_AUTHORIZED`.


## Ephemeral candidate build proof

The dedicated workflow now continues after FILE recipe generation:

```text
generated one-file crates
 -> exact current-M3JDK21 preimage check
 -> ephemeral candidate apply
 -> exact postimage verify
 -> changed-path fence
 -> second-apply diff fixed point
 -> Maven/backport scope proof
 -> fastdebug JDK image build
 -> legacy Shenandoah smoke
 -> experimental generational Shenandoah smoke
 -> generational jtreg
 -> JNI + MXBean + JFR Shenandoah regressions
 -> proof artifact upload
```

The experimental JEP 404 mode is invoked with
`-XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC
-XX:ShenandoahGCMode=generational`. The ordinary `-XX:+UseShenandoahGC` smoke is retained as the
Java-21 non-generational compatibility guard.

A failure at preimage admission is dependency/current-tree drift evidence and must be resolved in
the recipe/dependency packet. A build or jtreg failure is implementation evidence. Neither is
converted into automatic compatibility or promotion authority.
