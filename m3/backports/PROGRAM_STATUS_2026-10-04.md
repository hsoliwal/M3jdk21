# M3JDK21 backport programme status — 2026-10-04

This is a current execution snapshot, not a claim that all 14,948 released upstream changes have
been proven compatible or backported.

Regenerate repository-owned counts with:

`python3 m3/backports/program_status.py --root .`

## Released JEP denominator

The JDK 22–27 catalogue contains **85 JEP rows**. The catalogue is an admission/review denominator,
not implementation evidence.

A release-authority cross-check on 2026-10-04 found and restored three omitted released JEPs:
JEP 404 and JEP 483 from JDK 24, plus JEP 521 from JDK 25.
`RELEASE_JEP_AUTHORITY.tsv` is now the fail-closed release-feature denominator for JDK 22–26;
JDK 27 is now a released Java SE 27 line; its nine feature JEPs are part of the released denominator.

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

## File-atomic recipe generation

Draft PR #60 is the shared whole-JDK recipe-generation prerequisite:

`m3/file-atomic-recipe-generator-20261004`

It now preserves the historical Java-only default while adding two explicit opt-ins:

- `--crate-size 1` for true FILE replay atoms;
- `--include-text` for strict UTF-8 build/tool/resource targets.

Java files remain owned by `M3Jdk21HashPinnedSnapshotRecipe`. Non-Java UTF-8 targets use the
separate `M3Jdk21HashPinnedTextSnapshotRecipe`; the Java recipe contract was not weakened to
pretend build metadata is a Java compilation unit.

The text lane fails closed for removal, non-UTF-8 payloads and executable/file-mode changes.
Those states are typed exclusions because a PlainText replay cannot truthfully preserve filesystem
semantics.

The already-observed OpenRewrite v8.17.1 dependency repair was also applied on this branch:
`org.openrewrite:rewrite-text:8.17.1` was removed because PlainText is supplied by
`rewrite-core`. Exact-head recipe-crate workflow attempts are currently failing before GitHub
creates a job object, so no Python/Maven verdict is inferred from those red workflow cards.

## JDK-8359706 evidence recovery

The JDK-8359706 product implementation is already on current master from merged PR #53.

Draft PR #62:

`m3/backport-jdk-8359706-open-fd-count-20261004`

atomizes its provenance, recipe/DAG evidence and verification ownership. Its first executable DAG
and OpenRewrite failures were traced to the same nonexistent
`org.openrewrite:rewrite-text:8.17.1` dependency and repaired on the branch.

Before that repair, the branch produced green CI-probe and JDK21-verbatim-oracle results. Current
exact-head workflow attempts after the repair are failing before job creation, so the packet remains
at pass **verify** rather than being promoted.

## JEP 485 — Stream Gatherers

Draft PR #63 is the source-sealed Java-21 adaptation of final JEP 485:

`m3/backport-jep485-stream-gatherers-20261004`

The donor state is JDK 24 GA (`jdk-24+36`), including the final API graduation and follow-up
Gatherer fixes. The only explicit Java-21 adaptation identified by the dependency audit is the
post-21 `jdk.internal.invoke.MhUtil` VarHandle helper use in `GathererOp`; M3 replaces that leaf
with direct `MethodHandles.Lookup.findVarHandle(...)` behavior rather than importing the unrelated
utility.

The packet contains an exact 15-target OpenRewrite crate, recipe JUnit/fixed-point proof, dependency
audit, atom/DAG evidence, focused Gatherer jtreg family, existing stream regressions and a built-JDK
API smoke gate. Because this is additive public `java.base` API, authority is
`LIBRARY_API + EXPLICIT_CONTRACT_CHANGE`.

The exact pull-request workflow attempt for the current head failed before creating a job object;
the exact push workflow was still queued when this status was updated. JEP 485 therefore remains at
pass **verify**, not promoted.

## JEP 493 — Linking Run-Time Images without JMODs

Draft stacked PR #64 is the inventory/dependency/materialization lane for JEP 493:

`m3/jep493-link-runtime-images-20261004`

It is intentionally based on PR #60 because the upstream implementation mixes Java and non-Java JDK
files. Upstream implementation commit
`2ec358082f0896480bdbfcb289b4ba2bff0dd828` touches **47 paths**; all 47 are pinned in
`m3/backports/recipes/jep-493-runtime-image/PATHS.txt`.

The initial Java-21 dependency audit found the JDK-internal owners used by the JDK24 jlink code
already present in the target, including `OperatingSystem`, `ModuleBootstrap`, `ModulePath`,
`ModuleReferenceImpl`, `ModuleResolution`, `jdk.internal.opt.CommandLine`, and
`ExcludeJmodSectionPlugin`. New `JRTArchive`, `LinkableRuntimeImage` and
`runtimelink` classes are feature-owned additions.

PR #64 currently stops deliberately at inventory/file-atom materialization. Its workflow generates
one Java/text replay atom per selected path, accounts for every selected path as candidate, typed
exclusion or already equivalent, and does **not** apply the 47-file product packet yet. The next
required pass is review/composition followed by `jdk.jlink` build, runtime-image jtreg, existing
JMOD regression proof and fixed-point replay. Current exact-head workflow attempts failed before job
creation, so no compatibility/build verdict is claimed.

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
6. JEP 458, JEP 485, JEP 493 and every other accepted product packet are promoted only after
   exact-head proof;
7. generated FILE atoms are recomposed only at their honest PACKAGE/MODULE/MULTI_MODULE/LIBRARY_API
   boundary rather than being promoted independently;
8. rerunning inventory/admission finds no compatible residue.

The current work substantially closes the control-plane and known materialized-packet proof gaps,
but it must not be described as all 14,948 upstream changes completed until the denominator itself
reaches that fixed point.
