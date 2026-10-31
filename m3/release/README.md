# M3JDK21 evidence-first release pack

[Visible release queue](queue/README.md) · [Release contract](release-contract.json)

The first public M3JDK21 release is admitted by evidence, not by repository size, PR count, donor
catalogue size, or marketing language.

## Release theorem

A claim is publishable only when the release proof pack contains the exact source revision,
upstream baseline, commands, environment, raw result or test receipt, and SHA-256 needed to
reproduce that claim.

The release therefore has two layers:

1. **What is implemented and actually proved.**
2. **What remains an explicitly named qualification target.**

A planned feature is not a failed feature. It is simply not a release claim yet.

## P0 order

The canonical release priority is:

```text
M3 String
  -> regex precompute
  -> broader reusable precompute
  -> arrays
  -> collections
  -> SWT / Eclipse IDE integration
```

Later layers may be inventoried in parallel, but they must not be advertised as qualified before
their predecessor gates and their own target-specific evidence are sealed.

## Current first-release claim boundary

The current public P0 foundation is intentionally narrower than the full M3 programme.

**Currently safe to claim, subject to the release-admission workflow passing:**

- an OpenJDK 21-derived M3JDK21 product/research runtime target exists;
- an independently authored immutable-text/precompute foundation exists under the explicit
  first-party licensing boundary;
- product verification machinery, Java precompute differential tests and JNI/native
  parity tests exist here; under the standalone rule (2026-10-31) the transformation
  recipes this repository carries and their control-reactor evidence are executed here
  as well, with donor lineage kept in ledgers (`m3/tooling/standalone/`);
- the project has a deterministic release contract, rights/provenance record, and proof-pack
  checksum process.

**Not yet a first-release claim unless later evidence upgrades the machine contract:**

- that `java.lang.String` is already fully backed by M3 String;
- complete Java String compatibility;
- complete static/dynamic regex precomputation;
- a full jtreg pass;
- a universal performance win over OpenJDK 21;
- completed array/collection/SWT/Eclipse migration;
- production readiness.

Current implementations and pending claims are evaluated independently.

## Mandatory proof pack

A release candidate must preserve:

- exact release commit and tree;
- exact OpenJDK upstream baseline;
- M3/Synexia rights and licensing notices;
- foundation build/test logs;
- Java precompute differential results;
- JNI/native parity results where JNI/native is used;
- Maven/OpenRewrite control-reactor execution and replay receipts produced in this repository;
- product compilation and applicable installed-image/module tests;
- release-contract validation;
- generated SHA-256 manifest;
- raw benchmark data before any performance claim.

The release workflow may produce artifacts and receipts. Those artifacts are evidence only; they do
not independently create a release or upgrade a claim.

## Performance claims

A performance claim must compare the exact M3JDK21 candidate against the exact OpenJDK baseline on
the same machine and configuration. Preserve raw output and environment metadata.

At minimum record:

- CPU model, cores/threads, RAM, OS/kernel;
- JDK build identity and flags;
- warmup and measurement policy;
- cold and warm runs separately;
- wall time and CPU time;
- allocations and heap;
- RSS/native memory where applicable;
- GC counts/time;
- startup impact;
- score/error or median/percentiles and run-to-run variance.

Cherry-picked best runs are not release evidence.

## String / regex claim promotion

Before the release text can say that M3 String backs `java.lang.String`, the product must prove
the relevant JDK-visible contracts under product builds and applicable jtreg, including UTF-16 code
units and unpaired surrogates, equals/hash/compare, substring/views, interning, exceptions,
interpreter/C1/C2, GC/lifetime behavior, and CDS where affected.

Before the release text can say regex is fully precomputed, the release must preserve a corpus of at
least 10,000 regex cases against the Java semantic oracle, separate cold preparation from warm reuse,
seal cache keys by owner/snapshot/options/version, and test invalidation/corruption refusal.

## Release immutability

Before the first public tag, unreleased history may be consolidated while preserving the current
tree and upstream ancestry. Once a release/tag is published, that released history is immutable.
Subsequent work starts after the release.

See:

- `release-contract.json`
- `BENCHMARK_AND_COMPATIBILITY_SPEC.md`
- `verify_release_admission.py`
- `../../M3-SYNEXIA-NOTICE.md`
- `../../M3-SYNEXIA-RIGHTS.md`

## Inspect readiness and publish the work queue

`policy_valid` reports static release-policy checks. `release_ready` requires
all mandatory execution evidence, a clean inspected source revision and no
blocking custody/qualification items. A development policy pass is insufficient
for release. The historical single-commit publication requirement remains
explicit; this preparation does not rewrite or consolidate repository history.

```sh
python3 -m unittest discover -s m3/release -p 'test_*.py' -v
python3 m3/release/verify_release_admission.py --root . \
  --json-out m3/release/queue/current/admission.json \
  --queue-out m3/release/queue/current/README.md \
  --sha256-out m3/release/queue/current/SHA256SUMS

# A real candidate requires an independently produced evidence manifest:
python3 m3/release/verify_release_admission.py --root . --release \
  --evidence /absolute/path/to/release-evidence.json \
  --json-out m3/release/queue/current/admission.json \
  --queue-out m3/release/queue/current/README.md
```

The `M3_RELEASE_EVIDENCE_V1` manifest binds the inspected commit and tree to
one receipt for every `mandatory_release_gates` entry. Each passing receipt
records the executed command, zero exit status, toolchain/platform, a safe
relative artifact path and the artifact's SHA-256. Missing, duplicate, stale,
failed or malformed evidence is refused. Artifact integrity is a review aid;
the reviewer must still establish that the command and evidence prove the
claimed gate. Synthetic verifier fixtures are never release receipts.

Historical recipe-bearing target POMs and scripts are custody debt. Do not run
them as new transformation entrypoints. Keep product tests and raw evidence
available, recover the canonical recipe and callers in Synexia, then perform
an executed, source-preserving cleanup. Moving those recipes to another folder
inside M3JDK21 does not satisfy this boundary.

Receipt field details: artifact paths resolve relative to the manifest directory.
Every gate includes `owner_repository` and an `environment` object with `os`,
`architecture` and `toolchain`. `M3_MAVEN_CONTROL_REACTOR` also requires
`canonical_recipe` with `repository`, `commit` and `recipe_id`; its repository
is `hsoliwal/M3jdk21` (standalone rule, 2026-10-31). The product verifier consumes these records as data
and never executes command text supplied by a receipt.

## Retained PR source and naming queues

The first release workflow exports three **read-only review artifacts** alongside
the release-admission receipt:

- `m3/release/queue/current/name-census.tsv`: the canonical mapping declaration/path
  census from existing `m3/tooling/name-contract/audit.py`; not rename authority.
- `m3/release/queue/current/retained-source-queue.tsv`: preserved source from
  `m3/qualification/master-landing-20261028/retained-variants/pr<N>/` compared
  against its **original path in the checked-out Git HEAD tree**.
  Each row has archived/active modes and Git blob OIDs.
- `m3/release/queue/current/retained-source-summary.json`: pinned HEAD,
  classification totals, SHA-256, and `merge_authorized: false`,
  `rename_authorized: false`, `runtime_qualified: false`.

Reproduce the queue on a full checkout without changing active source:

```sh
python3 -m unittest discover -s m3/release -p 'test_*.py' -v
python3 m3/tooling/name-contract/check.py
python3 m3/tooling/name-contract/audit.py --strict
python3 m3/release/retained_source_queue.py --root . \
  --tsv-out m3/release/queue/current/retained-source-queue.tsv \
  --summary-out m3/release/queue/current/retained-source-summary.json
```

The queue is deterministic for one Git HEAD. On `release/**` branches,
the first-release workflow adds `--strict` and blocks release proof if any
archived source path remains unresolved. Artifact upload uses `if: always()`
so the queue remains available on failure. On ordinary PRs, unresolved rows
are reported without assuming that archived candidates must be promoted.

| Path classification | Interpretation / required next action |
| --- | --- |
| `EXACT_ACTIVE_PATH` | Same bytes and mode at the original path; **not** behavior proof |
| `REVIEW_ABSENT_ACTIVE_PATH` | Investigate rename, replacement or intentional exclusion |
| `REVIEW_DIVERGENT_ACTIVE_PATH` | Compare evolved contracts; never overwrite current code |
| `REVIEW_MODE_DIFFERENCE` | Check file-mode change before any replay |
| `HOLD_ARCHIVE_MODE` | Refuse promotion of unreviewed source type/mode |

For example, [PR #2084](https://github.com/hsoliwal/M3jdk21/pull/2084)
has archived native `wc` sources with no original live path under
`m3/tooling/module-packs/text-tools/src/jdk.m3.text/jdk/m3/text/`.
This is a **path-level observation** only; it is not proof that the
functionality is entirely absent, nor permission to copy archived code.

For each unresolved row, reuse the **private Synexia** Maven/Java
`M3HistoricalCodeLossRecipe` to compare the exact merge base, donor parent,
historical merge result, and current delivered Git blobs. Reuse existing
`M3HunkResolver`, `ExactFileHunkReplay` and `m3-pr-stack-preservation`
compiler/JUnit gates to prepare a bounded candidate. The public queue is
a review input, **not** a competing source transformation implementation.
GitHub's merge action handles mergeable history, but even a clean merge
needs content no-loss review. Conflicts require an explicit reviewed patch
and compiler, JUnit and (where applicable) matched-image JNI/jtreg gates.

Public JDK class and method names stay those of Java 21. Internal class/method
renames, package moves, documentation changes, service/reflection/JNI symbols
and consumption must be registered under the stable capability ID in
`m3/docs/name-mapping.json` and processed with the existing Synexia typed
Maven/OpenRewrite recipe before target acceptance. Preserve ABI, serialization
and native entry-point behavior. Recipes and execution memory live in Synexia;
only qualified product results and evidence live in M3JDK21.

## Canonical recipe execution and migration HOLD

The release workflow consumes the mandatory `M3_MAVEN_CONTROL_REACTOR` receipt
from source-bound work executed in this repository (`hsoliwal/M3jdk21`). Standalone
rule (2026-10-31): no other repository is needed to build, test, gate or release;
donor lineage stays in `m3/tooling/standalone/` (`SYNEXIA_REFS.tsv`,
`LINEAGE_SOURCES.tsv`). Its control job
requires both `policy_valid` and an exact passing canonical Maven gate; absent,
stale, malformed or failed evidence keeps the job and release summary on HOLD.
Historical target-local recipe scripts and the target recipe reactor are no
longer execution entrypoints in this workflow. Their retained sources remain
visible custody migration debt (`legacy_recipe_custody` HOLD) and are preserved
for a separately qualified cleanup.

Foundation, precompute Java/JNI and full-family product checks remain active,
along with naming and retained-source queues. The dated
[validation snapshot](queue/2026-10-29/validation-summary.json) and
[collections failure catalogue](queue/2026-10-29/collections-failures.tsv) retain
the bounded runs and outstanding contract questions. These observations keep
their original tested source pins; they do not satisfy all current release gates.
