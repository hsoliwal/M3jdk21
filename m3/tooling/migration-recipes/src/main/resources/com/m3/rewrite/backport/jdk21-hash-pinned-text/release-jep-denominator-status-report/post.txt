# M3JDK21 backport programme status — 2026-10-04

This is a current execution snapshot, not a claim that all 14,948 released upstream changes have
been proven compatible or backported.

Regenerate repository-owned counts with:

`python3 m3/backports/program_status.py --root .`

## Released JEP denominator

The JDK 22–27 catalogue contains **85 JEP rows**. The catalogue is an admission/review denominator,
not implementation evidence.

A release-authority cross-check on 2026-10-04 found and restored three omitted released JEPs: JEP 404 and JEP 483 from JDK 24, plus JEP 521 from JDK 25. `RELEASE_JEP_AUTHORITY.tsv` is now the fail-closed release-feature denominator for JDK 22–26; JDK 27 remains an explicitly mutable in-development snapshot.

Language/source-semantics removals and incompatible API/runtime removals remain explicit rejects or
holds. Final/additive tooling, library, serviceability, runtime and VM changes remain candidates
until dependency closure and Java-21 compatibility proof are complete.

## Complete released-change denominator

The pinned released intervals contain **14,948 upstream commits** across JDK 22–27. The complete
inventory workflow, compatibility queue, whole-tree file-delta inventory and bounded OpenRewrite
crate generator remain the denominator authority.

No JEP-only or release-note subset is accepted as completion evidence.

## Materialized current-master packets

Current master contains source-bound/adapted implementation for:

- JDK-8357439 — jcmd bash completion;
- JDK-8347112 — recursive javadoc doc-files behavior with Java-21 compatibility preservation;
- JDK-8359706 — open-file-descriptor diagnostics recovery;
- JDK-8364182 — `VM.security_properties`;
- JDK-8367584 — `FlightRecorderOptions:help` compatibility leaf only, explicitly excluding JEP 536 redaction;
- JDK-8368692 — `jdk.security.password.allowSystemIn` with compatibility-preserving default;
- JDK-8374808 — KeyStore creation Instant additive API leaf.

The five OpenRewrite-owned adapted packets 8347112/8364182/8367584/8368692/8374808 are now
materialized in `BACKPORT_WORK_QUEUE.tsv` at pass **verify**. Their existence is not a green
verification receipt.

## Control-plane convergence on branch m3/jdk21-final-convergence-20261004

The current-master convergence branch restores and integrates:

- M3IndexDB as the sole semantic-index persistence/composition owner;
- OpenRewrite semantic atom/hash extraction from ATOM through REPOSITORY;
- exact structural hash, normalized logic hash and SimHash candidate signals;
- explicit pattern/IOP role state;
- bounded multi-pass plan and append-only pass receipt ledger;
- scope authority FILE -> VISIBILITY -> PACKAGE -> MODULE -> MULTI_MODULE -> LIBRARY_API;
- 99% line + branch JaCoCo gates for scope/atom/semantic/pass tooling and M3IndexDB;
- `BACKPORT_PASSES.tsv` and `BACKPORT_WORK_QUEUE.tsv`;
- fail-closed queue/catalogue verification;
- focused workflow coverage for all five current adapted backport recipes;
- reactor-correct Maven execution through `m3/pom.xml`.

Externally visible JDK/tool/API backports are registered as
`LIBRARY_API + EXPLICIT_CONTRACT_CHANGE`; physical module locality does not hide their contract
authority.

## JEP 458

Draft PR #59 is the current-tree, recipe-first JEP 458 implementation:

`m3/recover-jep458-current-master-v2-20261003`

It is based on current master, is mergeable, and owns the complete current-tree replay packet rather
than rebasing the historical implementation branch.

Two observed CI substrate failures were repaired on the PR branch:

1. removed nonexistent Maven dependency `org.openrewrite:rewrite-text:8.17.1`; in OpenRewrite
   v8.17.1 `org.openrewrite.text.PlainText` is supplied by `rewrite-core`;
2. added `libxrandr-dev`, required by OpenJDK configure on Ubuntu.

JEP 458 remains draft until its refreshed recipe/build/jtreg/runtime workflow passes the exact
repaired head. Do not infer source failure from the superseded infrastructure failures.

## Community/module packs

PR #56 — module-pack tooling and diagnostics-image proof — **merged 2026-10-03**. It is no longer
an open review lane. Optional external artifact packs such as JavaFX remain separate distribution
admission concerns and do not block the core JDK21 backport denominator.

## Current open completion boundary

M3JDK21 compatible-backport convergence is complete only when:

1. every released change in the 14,948-commit denominator has an explicit compatibility decision;
2. every proven-compatible change is implemented or proven already present/equivalent;
3. every accepted packet has exact source provenance and replay;
4. Maven/OpenRewrite/JUnit/JaCoCo proof is green for the recipe/control plane;
5. relevant OpenJDK build, jtreg and runtime gates are green;
6. JEP 458 and every other accepted product packet is promoted only after exact-head proof;
7. rerunning inventory/admission finds no compatible residue.

The current work substantially closes the control-plane and known materialized-packet proof gaps,
but it must not be described as all 14,948 upstream changes completed until the denominator itself
reaches that fixed point.
