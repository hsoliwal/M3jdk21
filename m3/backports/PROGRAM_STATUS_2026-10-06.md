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

The current repair restores the released-feature authority that was partially lost by a later
merge. The authoritative JDK 22–27 set includes:

- **JEP 404** — Generational Shenandoah (Experimental) — JDK 24
  - disposition: `candidate-high-risk`
  - reason: base generational Shenandoah implementation; GC/runtime dependency closure and
    platform proof are mandatory
- **JEP 483** — Ahead-of-Time Class Loading & Linking — JDK 24
  - disposition: `candidate-high-risk`
  - reason: runtime/CDS optimization, no Java grammar change, but dependency closure and
    startup/class-loading parity are mandatory
- **JEP 521** — Generational Shenandoah — JDK 25
  - disposition: `candidate-high-risk`
  - reason: productizes the JEP 404 lineage and therefore depends on the complete JEP 404 collector
    implementation

**JEP 401 is not part of the released JDK 22–27 authority set** and is removed from this denominator
rather than being used to keep the total artificially at 85.

Per-release authority counts:

- JDK 22: 12
- JDK 23: 12
- JDK 24: 24
- JDK 25: 18
- JDK 26: 10
- JDK 27: 9

Current disposition counts:

- **42** pending proof or implementation
  - 11 `candidate`
  - 18 `candidate-high-risk`
  - 5 `hold-compat`
  - 5 `hold-jit`
  - 3 `hold-preview`
- **43** decided no direct default backport
  - 18 `reject-language`
  - 5 `reject-compat`
  - 17 `superseded`
  - 2 `superseded-high-risk`
  - 1 `superseded-jit`

A pending row is **not** an acceptance decision. It remains pending until its dependency,
compatibility, implementation and proof packet closes.

## Priority-matrix closure

`POST21_PRIORITY_COMPATIBILITY.tsv` now contains **34 rows**, and every priority row must also
exist in the full released-JEP catalogue.

`program_status.py` now fails closed when:

- a JEP number is duplicated;
- a priority row is missing from the full catalogue;
- a released row lies outside JDK 22..27;
- catalogue ordering is not deterministic by release then JEP.

## Deterministic JEP residue queue

`JEP_RESIDUE_QUEUE.tsv` materializes all 42 pending rows.

For each row it records:

- release/JEP/title/domain/disposition;
- proof priority;
- current recipe evidence state;
- current recipe evidence paths;
- next mechanical action;
- priority-matrix Java21 default/classification when available.

Current JEP packet evidence recognized by the regenerated queue:

- JEP 423 — `PACKET_EVIDENCE`
  - short/long inventory/candidate packet directories exist; no current-tree receipt;
- JEP 458 — `MATERIALIZED_PACKET`
  - current-tree receipt: `REVIEWED_POSTIMAGES_ALREADY_PRESENT`;
- JEP 467 — `MATERIALIZED_PACKET`
  - receipt-backed packet evidence, promotion still `NOT_AUTHORIZED`;
- JEP 474 — `MATERIALIZED_PACKET`
  - receipt state: `EQUIVALENCE_PROOF_PENDING`;
- JEP 484 — `PACKET_EVIDENCE`
  - class-file API lineage/inventory packet exists; no current-tree receipt;
- JEP 485 — `PACKET_EVIDENCE`
  - implementation/recovery directories and recipe class exist, but current master has no retained
    current-tree receipt, so the queue does not infer product materialization from directory presence;
- JEP 491 — `PACKET_EVIDENCE`
  - high-risk monitor-unpinning inventory packet exists;
- JEP 493 — `MATERIALIZED_PACKET`
  - receipt state: `PACKET_READY`;
- JEP 510 — `PACKET_EVIDENCE`
  - KDF inventory/dependency packet exists with `product_materialization=false`.

`PACKET_EVIDENCE` means repository-owned review/dependency/inventory material exists but does not
prove that product source is present. `MATERIALIZED_PACKET` is reserved for receipt-backed evidence;
the receipt still controls promotion and next proof.

The remaining pending rows stay visible even when no recipe exists yet. A missing recipe means
**author/improve a reusable recipe first**, not hand-edit the affected JDK files.

## Backport packet evidence estate

Packet directories are evidence-bearing work units, not automatic materialization claims.

Pending-JEP packet directories currently include:

- `j423`, `jep-423-region-pinning`;
- `j491`;
- `j510`;
- `jep-458-current`;
- `jep-467-markdown`;
- `jep-474-generational-zgc`;
- `jep-484-classfile-api`;
- `j485`, `jep-485-gatherers`, `jep-485-gatherers-recovery`;
- `jep-493-runtime-image`.

Compatible non-JEP/JBS packets also remain under the same recipe estate, including
`jdk-8347112`, `jdk-8364182`, `jdk-8367584`, `jdk-8368692`, and `jdk-8374808`.

A packet directory alone means `PACKET_EVIDENCE`. Only a valid `CURRENT_TREE_RECEIPT.tsv`
upgrades the evidence state to `MATERIALIZED_PACKET`, and even then explicit promotion authority,
build/jtreg/runtime proof, and canonical readback remain separate gates.

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
