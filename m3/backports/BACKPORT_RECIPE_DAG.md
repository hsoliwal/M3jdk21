# M3JDK21 backport recipe DAG

Status: deterministic orchestration layer over the existing M3JDK21 backport authorities.

## Purpose

Backporting is not a separate hand-edit stream.

Every compatible JEP, compatible non-JEP upstream improvement, and admitted community capability
moves through the same M3 recipe-first structure used elsewhere:

```text
authority inventory
 -> compatibility disposition
 -> small source-pinned recipe atoms
 -> Java-21 compile
 -> jtreg/JUnit
 -> runtime/JNI/VM proof where required
 -> fixed point
 -> serial promotion
```

## Authority

This DAG compiler does **not** decide whether a JEP is compatible.

The existing authorities remain canonical:

- `JEP_CATALOGUE.tsv`;
- `UPSTREAM_CHANGE_SEEDS.tsv`;
- `COMMUNITY_CAPABILITY_CANDIDATES.tsv`;
- checked-in packets under `m3/backports/recipes/`.

`backport_recipe_dag.py` projects those decisions. It never upgrades a hold/reject/superseded row
to executable work.

## Work-item states

Each authority row is projected to one of:

- `MATERIALIZED_RECIPE` — one checked-in recipe packet already exists;
- `MATERIALIZED_RECIPE_SET` — multiple checked-in packets are intentionally composed;
- `AUTHOR_RECIPE` — compatible/admitted candidate has no packet yet;
- `HOLD`;
- `REJECT`;
- `SUPERSEDED`;
- evidence-only community state.

Only materialized/author-recipe candidates generate executable packet TSVs.

A generated `AUTHOR_RECIPE` packet does not authorize direct editing. Its recipe atom explicitly
means “author/test the source-pinned reusable recipe first”.

## Small packet atoms

Each executable JEP/JDK work item receives one bounded packet:

```text
inventory
 -> recipe
 -> compile
 -> test
 -> [runtime-parity]
 -> fixed-point
```

`runtime-parity` is included for VM/GC/HotSpot/native/JFR/network/AOT/high-risk work.

The packet ABI is the existing `M3BackportPacketLoader` format:

```text
packet_id  atom_id  scope  scope_promotion_approved  work_ref  depends_on
```

The work-item identity lives in `packet_id` / `work_ref`; atom IDs remain small local names.

## Scope

The generator projects the already-classified domain into the minimum orchestration floor:

- runtime/VM/GC/HotSpot/native/compiler/JFR/network/AOT -> `MULTI_MODULE`;
- library/security/tool/javadoc/javac/jlink/launcher -> `MODULE`;
- otherwise -> `FILE`.

A packet rooted above FILE sets explicit scope-promotion approval. Repository size alone never
changes scope.

The existing `M3RecipeDag` remains the enforcement oracle for silent scope escalation.

## Existing packet reuse

Checked-in recipe directories are reused and never duplicated.

Examples include:

- JEP 423 region pinning;
- JEP 458 launcher work;
- JEP 467 Markdown javadoc;
- JEP 474 generational ZGC;
- JEP 484 Class-File API;
- JEP 485 Stream Gatherers;
- JEP 491 virtual-thread unpinning;
- JEP 493 runtime-image work;
- JEP 496 ML-KEM;
- JEP 497 ML-DSA;
- JEP 523 G1 default;
- JDK-8347112;
- JDK-8357439;
- JDK-8364182;
- JDK-8367584;
- JDK-8368692;
- JDK-8374808.

Presence of a packet means recipe material exists. It does **not** mean the full JDK image,
jtreg/runtime, VM/JNI or performance acceptance gates have passed.

## Existing Java DAG engine

M3JDK21 already owns the generic execution/projection substrate under
`m3/tooling/backport-dag`.

The generated packet is parsed by `M3BackportPacketLoader`, composed by
`M3PacketDagComposer`, and rejoined into the canonical verification tail.

`M3BackportPacketBatchMain` validates every generated packet in one JVM.

The existing orchestration projector remains responsible for Camel/Airflow/Drools projections.
Those projections do not become semantic or promotion authority.

## Maven evidence

`mvn -f m3/backports/pom.xml verify` now emits:

- `target/backport-recipe-dag.tsv`;
- `target/backport-recipe-dag.json`;
- `target/backport-packets.tsv`;
- `target/backport-packets/*.tsv`;
- existing programme-status evidence.

The JSON root is deterministic for the exact authority/recipe tree.

## Completion

This closes the orchestration gap; it does not falsely declare the whole backport programme done.

Programme completion still requires each compatible candidate to move from `AUTHOR_RECIPE` or
materialized-but-unverified state through its actual source-pinned recipe, Java-21 build,
jtreg/JUnit, runtime/VM/JNI gates as applicable, fixed point, and serial promotion.

Language/spec-incompatible, compatibility-reducing, preview-only, or dependency-blocked JEPs remain
held/rejected according to the existing catalogue.
