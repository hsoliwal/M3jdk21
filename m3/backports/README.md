# JDK 22–27 → M3JDK21 complete compatible-backport programme

Status: active downstream backport programme. Target baseline is OpenJDK 21 semantics.

The governing execution law is [COMPATIBILITY_ADMISSION.md](COMPATIBILITY_ADMISSION.md).

## Locked contract

The Java 21 language grammar, source acceptance, class-file compatibility and existing externally
observable behavior are locked unless a future change is explicitly unlocked and independently
accepted.

The donor universe is intentionally complete, not curated. Every OpenJDK change after JDK 21 GA
through the latest released JDK enters inventory: JEP implementation, non-JEP JBS fix, javac fix,
HotSpot/GC/runtime change, library/security change, tooling improvement, build/test change and
follow-up repair.

The default state for an upstream change is `PENDING_COMPATIBILITY_PROOF`, not ignored.

A change is required for M3JDK21 when it can be adapted while preserving the Java 21 contract.
Tooling improvements are explicitly in scope, including `javac`, `jcmd`, `jlink`, `jpackage`,
`jfr`, `jconsole`, `jdeps`, `javadoc`, `keytool`, launchers, build/test tooling and
serviceability.

HotSpot, GC, JFR, native, security, core-library and performance changes are likewise in scope when
they preserve the Java 21 external contract. HotSpot compiler/JIT changes remain a high-risk lane,
but they are not excluded merely because they touch compiler/runtime internals.

A javac touch is also not an automatic hold. A compatible compiler bug fix, diagnostic improvement,
annotation-processing repair, documentation parser fix or performance improvement remains eligible.
Only evidence that a patch fundamentally requires post-21 language/class-file/spec semantics excludes
it from the Java-21-compatible lane.

The following are not silently imported:

- Java grammar/type-system features whose purpose is to make Java 21 accept post-21 source syntax.
- Class-file requirements that would make the runtime cease to satisfy the locked Java 21 contract.
- API/platform removals that narrow the JDK 21 contract.
- Preview/incubator contracts as if they were final.
- Compatibility restrictions whose purpose is to remove behavior Java 21 must continue to provide.
- A commit merely because it exists in a newer JDK. Every accepted backport needs provenance,
  dependency closure, tests and an exact target diff.

If an upstream patch mixes compatible and incompatible material, split it and keep the compatible
leaf in the backport queue.

## Upstream denominator

The released OpenJDK GA tag intervals used for mechanical inventory are:

| Release | GA tag |
| --- | --- |
| 21 | `jdk-21+35` |
| 22 | `jdk-22+36` |
| 23 | `jdk-23+37` |
| 24 | `jdk-24+36` |
| 25 | `jdk-25+36` |
| 26 | `jdk-26+35` |
| 27 | `jdk-27+35` |

The pinned GA intervals contain 14,948 upstream commits:

- JDK 22 interval: 2,384
- JDK 23 interval: 2,355
- JDK 24 interval: 2,562
- JDK 25 interval: 2,678
- JDK 26 interval: 2,611
- JDK 27 interval: 2,358

`JEP_CATALOGUE.tsv` records all 82 JEPs delivered by JDK 22 through JDK 27 and gives their
initial downstream disposition. A disposition is an admission decision, not implementation evidence.

`UPSTREAM_CHANGE_SEEDS.tsv` records individually inspected non-JEP enhancements. It is intentionally
only a seed. `inventory.py` is the denominator builder: against a complete local `openjdk/jdk`
checkout it walks every commit in each GA interval, records JBS IDs and touched paths, and assigns a
review lane. Path classification never proves incompatibility.

The complete inventory must be used to generate the mechanical backport queue. Release-note curation
or a JEP-only list is never considered the complete denominator.

## Mechanical compatibility queue

The complete upstream denominator is converted into an ordered proof queue by
`compatibility_queue.py`.

The queue never admits or rejects a change from path heuristics. Every row remains:

`PENDING_COMPATIBILITY_PROOF`

but receives deterministic planning fields:

- risk class;
- physical scope floor;
- proof lane;
- recipe strategy;
- numeric priority;
- next mechanical action.

The early passes prioritize low-risk build/tool/test changes, followed by libraries, security,
runtime/HotSpot, javac, and finally language/compatibility-sensitive review. This is scheduling,
not semantic authority.

The upstream-inventory workflow exports:

- `UPSTREAM_CHANGES.tsv`;
- `UPSTREAM_CHANGES.summary.json`;
- `COMPATIBILITY_QUEUE.tsv`;
- `COMPATIBILITY_QUEUE.summary.json`.

## Exact JDK21 ↔ donor file deltas and recipe crates

`file_delta_inventory.py` compares the entire JDK 21 GA tree verbatim against each released donor
GA tree and records SAME / MODIFIED / ADDED / REMOVED for the full path union.

For changed Java source, `generate_recipe_crates.py` can create bounded <=256-target,
hash-pinned OpenRewrite candidate crates. The generator:

- reads exact baseline/donor bytes from Git;
- requires strict UTF-8 round-trip for Java source;
- records SHA-256 preimages and postimages;
- emits typed exclusions rather than silently deleting/removing;
- keeps generated crates `CANDIDATE_UNVERIFIED` until Java-21 compatibility proof succeeds.

This is the bridge from whole-release inventory to per-file mechanical recipe work.

## Backport packet

Each actual backport must record:

1. target baseline commit and preimage path hashes;
2. upstream repository, exact commit and JBS/JEP identity;
3. touched-path inventory and dependency closure;
4. Java 21 compatibility decision and explicit exclusions;
5. exact patch or source-bound recipe;
6. lint/build/test/runtime receipts in that order where applicable;
7. postimage hashes and target diff;
8. idempotent replay/rollback or a documented reason why the upstream patch itself is the canonical
   replay unit.

For recurring or structurally repeated Java-source adaptation, a tested OpenRewrite recipe is
mandatory. JUnit must prove the recipe or recipe DAG on Java 21 fixtures before broad application.
A one-off exact donor patch may remain the replay unit only when hash-pinned and independently
verified.

Existing `m3/docs/name-mapping.json` remains the M3 migration mapping authority. This directory is an
upstream-JDK backport catalogue and must not replace or fork that mapping authority.

## First admitted tool change

JDK-8357439, **Add bash autocompletion for jcmd**, is the first concrete tool backport. Upstream
commit:

`8549d1896054dd230ba3038c83bce23b10dcda22`

Its two paths are additive in the inspected JDK 21 target:

- `make/modules/jdk.jcmd/Copy.gmk`
- `src/jdk.jcmd/share/conf/bash-completion/jcmd`

The target preimage for both paths is absent. The change does not alter Java grammar, javac, class
files or JVM execution semantics. Build/runtime acceptance remains required before promotion.

## Next adapted tooling candidate

JDK-8347112, **Copy nested directories in doc-files by default**, is materialized as a
Java-21-compatible split candidate from upstream commit
`b221cb6ba138672802644f37eebf368521a0a6f4`.

The M3 adaptation imports recursive copying by default and wildcard exclusion, but deliberately
retains Java 21's accepted `-docfilessubdirs` option processing. The packet is owned by
`M3Jdk8347112BackportRecipe` and remains `candidate-adapted` until focused javadoc build/jtreg and
recipe fixed-point CI pass. See `recipes/jdk-8347112/`.

## Adapted cross-platform diagnostics candidate

JDK-8359706, **open file descriptor diagnostics for VM.info / error reports**, is materialized from

`openjdk/jdk@b0831572e2cd9dbff9ee2abcdf81a493ddcecc7e`

together with required follow-up JDK-8380236:

`openjdk/jdk@3a109f49feb19f313632be6a2aa24ba7d9b7269b`.

M3JDK21 treats them as one dependency-closed packet. Linux uses bounded `/proc/self/fd`
enumeration, macOS uses bounded `proc_pidinfo`, AIX/Windows retain stubs, and VM.info/fatal error
reporting gains the new diagnostic. The packet is owned by `M3Jdk8359706BackportRecipe` as one
Java test atom plus one seven-file HotSpot text atom.

Status remains `candidate-adapted` until Linux and macOS build/test evidence, recipe fixed point,
backport verification and platform compatibility gates pass. See `recipes/jdk-8359706/`.

## Completion boundary

M3JDK21 backport convergence is complete only when:

- the full 14,948-commit released denominator is inventoried;
- all 82 JEPs and all non-JEP changes have a compatibility decision or explicit pending proof;
- every proven-compatible change is implemented or proven already present by equivalence;
- every accepted backport has provenance and verification evidence;
- the full M3JDK21 build/test gates are green; and
- rerunning inventory/admission yields no unclassified compatible residue.

Do not report completion merely because the catalogue or queue exists.
