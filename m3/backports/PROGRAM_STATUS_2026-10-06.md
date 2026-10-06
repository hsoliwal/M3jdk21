# M3JDK21 backport programme status — 2026-10-06

This is a dated evidence snapshot, not a completion claim.

Canonical baseline for this snapshot:

- M3JDK21 default branch: `master`
- master observed before this denominator-repair lane: `b71ee5bf88398fb80961c13db03e8c675fb445ce`
- released donor interval: JDK 22 GA through JDK 27 GA
- locked Java contract: Java 21 source/class-file/runtime compatibility unless an extension is
  explicitly opt-in and separately namespaced

Regenerate repository-owned counts with:

```bash
python3 m3/backports/program_status.py --root .
python3 m3/backports/jep_residue.py --root . --out m3/backports/JEP_RESIDUE_QUEUE.tsv
```

## Released JEP denominator

The corrected released catalogue contains **85 unique JEP rows** spanning JDK 22 through JDK 27.

This lane repairs three omissions from the older 82-row snapshot:

- **JEP 483** — Ahead-of-Time Class Loading & Linking — JDK 24
  - disposition: `candidate-high-risk`
  - reason: runtime/CDS optimization, no Java grammar change, but dependency closure and
    startup/class-loading parity are mandatory
- **JEP 521** — Generational Shenandoah — JDK 25
  - disposition: `candidate`
  - reason: makes an existing Shenandoah mode non-experimental; no Java grammar/API change, but
    GC/runtime option and regression proof are mandatory
- **JEP 401** — Value Classes and Objects (Preview) — JDK 26
  - disposition: `reject-language`
  - reason: preview language/JVMS/object-identity/class-file changes exceed the locked Java 21
    contract

Current disposition counts:

- **41** pending proof or implementation
  - 11 `candidate`
  - 17 `candidate-high-risk`
  - 5 `hold-compat`
  - 5 `hold-jit`
  - 3 `hold-preview`
- **44** decided no direct default backport
  - 19 `reject-language`
  - 5 `reject-compat`
  - 17 `superseded`
  - 2 `superseded-high-risk`
  - 1 `superseded-jit`

A pending row is **not** an acceptance decision. It remains pending until its dependency,
compatibility, implementation and proof packet closes.

## Priority-matrix closure

`POST21_PRIORITY_COMPATIBILITY.tsv` now contains **36 rows**, and every priority row must also
exist in the full released-JEP catalogue.

The current matrix also makes two previously implicit relationships explicit: JEP 404 is the opt-in runtime substrate required before JEP 521 can be considered, and JEP 510 is an opt-in post-21 SE API extension rather than default Java-21 behavior.

`program_status.py` now fails closed when:

- a JEP number is duplicated;
- a priority row is missing from the full catalogue;
- a released row lies outside JDK 22..27;
- catalogue ordering is not deterministic by release then JEP.

## Deterministic JEP residue queue

`JEP_RESIDUE_QUEUE.tsv` materializes all 41 pending rows.

For each row it records:

- release/JEP/title/domain/disposition;
- proof priority;
- current recipe evidence state;
- current recipe evidence paths;
- next mechanical action;
- priority-matrix Java21 default/classification when available.

Current JEP packet evidence recognized on live `master` after residue reconciliation:

- JEP 423 — `PACKET_EVIDENCE`
  - inventory/candidate packets exist; no current-tree receipt or product-completion claim;
- JEP 458 — `MATERIALIZED_PACKET`
  - reviewed current-tree receipt is present;
- JEP 467 — `MATERIALIZED_PACKET`
  - packet-ready current-tree receipt is present;
- JEP 474 — `MATERIALIZED_PACKET`
  - receipt state is `EQUIVALENCE_PROOF_PENDING`; the Java 21 ZGC default remains unchanged;
- JEP 484 — `PACKET_EVIDENCE`
  - cumulative class-file lineage/path-map evidence exists; no product-completion claim;
- JEP 485 — `MATERIALIZED_PACKET`
  - current tree contains the Gatherer/Gatherers/GathererOp product classes plus Stream and
    ReferencePipeline integration; promotion remains `NOT_AUTHORIZED` until Java 21 build/jtreg
    and fixed-point proof are complete;
- JEP 491 — `PACKET_EVIDENCE`
  - multi-architecture monitor-unpinning inventory exists; no product materialization claim;
- JEP 493 — `MATERIALIZED_PACKET`
  - packet-ready current-tree receipt is present.

`PACKET_EVIDENCE` means repository-owned inventory/review material exists but does not by itself
mean source has been materialized. `MATERIALIZED_PACKET` is reserved for receipt-backed packet
evidence; its receipt fields still control whether product source is present and whether promotion
is authorized.

The remaining pending rows stay visible even when no recipe exists yet. A missing recipe means
**author/improve a reusable recipe first**, not hand-edit the affected JDK files.

## Materialized backport packet estate

Eight packet directories currently have repository-owned README/evidence contracts:

- `jdk-8347112`
- `jdk-8364182`
- `jdk-8367584`
- `jdk-8368692`
- `jdk-8374808`
- `jep-458-current`
- `jep-467-markdown`
- `jep-493-runtime-image`

A packet directory is evidence of implementation/replay work. It is not itself proof that the
packet is accepted into current master.

## Verbatim JDK21 source oracle

The OpenJDK 21 GA oracle remains pinned to:

`890adb6410dab4606a4f26a942aed02fb2f55387`

The verbatim plane remains separate from semantic/contract evidence:

```text
OpenJDK21 exact bytes
    -> path/hash ledger
    -> changed-file frontier
    -> semantic/contract inventory
    -> scope-aware OpenRewrite recipe
    -> JUnit recipe proof
    -> dry-run diff
    -> Java21 compile/jtreg/runtime proof
    -> serial promotion
```

A byte difference is not semantic drift, and a semantic hash match is not automatic equivalence.

## Canonical multi-pass execution order

Every backport or refactor follows the narrowest required scope:

```text
INVENTORY
  -> FILE fixed point
  -> VISIBILITY
  -> PACKAGE
  -> MODULE
  -> MULTI_MODULE
  -> LIBRARY_API
  -> PROOF
```

Independent FILE work may fan out. Promotion remains serial and evidence-gated.

## Next mechanical work

The queue determines priority; conversation order does not.

The next passes are:

1. close already-materialized JEP packets against current master before authoring duplicates;
2. process low-risk compatible tooling/runtime rows before high-risk VM/JIT rows;
3. author exact source-pinned recipes for candidate rows with no current recipe evidence;
4. keep compatibility-policy/JIT/preview rows visible as holds rather than silently dropping them;
5. continue the complete released-commit denominator independently of JEP headline work;
6. rerun JEP and commit residue generation until no compatible residue remains.

## Completion boundary

Do not claim completion until:

1. every released JEP row is classified;
2. the complete released OpenJDK commit denominator is inventoried;
3. every compatible JEP/non-JEP/tooling change has an implementation, equivalence proof, or explicit
   compatibility rejection;
4. every accepted source-changing packet is source-pinned and replayable;
5. Java 21 build, jtreg and applicable runtime/JNI/JVMTI/JFR/security gates pass;
6. rerunning both JEP and commit residue queues yields no compatible unimplemented residue.
