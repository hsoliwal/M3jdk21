<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK 21 release preparation

[Release preparation: names and tests](RELEASE_PREPARATION.md) · [Visible release queue](queue/README.md) · [Release contract](release-contract.json) · [Naming, documentation and coverage](NAMING_DOCUMENTATION_COVERAGE.md) · [Fixed-point refusal evidence](NAMING_FIXED_POINT_EVIDENCE.md) · [Candidate notes](RELEASE_NOTES.md) · [Historical checklist](RELEASE_CHECKLIST.md) · [Current candidate checklist](RELEASE_CHECKLIST_CANDIDATE_2756.md)

M3JDK21 is one integrated Java/HotSpot/JNI SDK. Release claims require evidence
for the exact candidate commit and tree. A naming pass, finite component test,
recipe replay or historical CI run does not establish installed-JDK acceptance.

## Product and version names

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
  parity tests exist here; under the 2026-10-31 standalone rule the
  `M3_MAVEN_CONTROL_REACTOR` receipt is produced inside `hsoliwal/M3jdk21`, and
  M3JDK21 independently executes product acceptance;
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

## Release names and naming gate scope

The machine product name is **M3JDK21**; the display name is **M3JDK 21**.
The current preview tag is `m3jdk-21.0.14-m3.1-preview`, as specified by
`release-contract.json` and checked against `make/conf/version-numbers.conf`.
Use the same tag in the release title,
archive names and evidence pack. A preview qualifier describes release maturity;
it does not qualify an implementation or satisfy a test gate.

| Surface | Naming rule | Evidence required before release |
| --- | --- | --- |
| Standard Java API | Preserve `java.*`, `javax.*` and the actual owning JDK module; preserve JVM/JNI descriptors | API/binary and behavioral checks on the candidate image |
| Additive public M3 companion | Preserve the source-declared `jdk.m3.*` owner and separately reviewed public module export | Exact source hashes, matching package/path/type and module ownership, plus independent compatibility review |
| Internal M3 implementation | Keep the existing mapped internal owner and visibility | Canonical map and exact source declaration checks; internal implementation is not a public replacement API |
| Compatibility alias | Preserve the existing `toAdpater` spelling where the compatibility contract requires it | Caller and behavioral regression checks before any future deprecation |
| Migration queue | Keep stable mapping IDs, lineage and `QUEUED` state until the required proofs exist | Exact `name-mapping.json` Git blob in `name-plan.json`; no status promotion from test counts |

The naming receiver ignores comments, strings and text blocks when checking
mapped declarations. A nested type or two conflicting top-level declarations
cannot establish the top-level source owner. Package-private internal owners
remain valid; public companions have separate public/export checks. These are
lexical and mapping checks, not a substitute for Java compilation or jtreg.

Run the complete naming suite as well as the live receiver and census:

```sh
python3 -m unittest discover -s m3/tooling/name-contract -p 'test_*.py' -v
python3 m3/tooling/name-contract/check.py
python3 m3/tooling/name-contract/audit.py --strict
```

The naming workflow installs `m3/migration/requirements.txt`, checks out the
schema and every concrete mapped target, and runs its refusal tests even when
the live authority gate fails. A successful synthetic fixture suite does not
make a stale authority pin, missing lineage, schema violation or source hash
mismatch acceptable. Repair the canonical source/mapping owner and replay its
sealed recipe; do not merely replace a digest to manufacture readiness.

## Test coverage evidence

Keep three results separate in a release proof pack: **test outcome**, **measured
tooling coverage**, and **candidate-image compatibility**. Include the exact
source commit/tree, command, dependency versions, raw logs and hashes for each.
The release suite covers the admission consumer, standalone dependency checker
and retained-source queue. The isolated naming receiver suite covers its own
refusal/declaration logic. Neither suite reports coverage of the JDK, HotSpot,
JNI implementations or SWT.

For a reproducible local tooling measurement, install `coverage==7.13.4` in an
isolated Python 3.12 environment and use separate data files:

```sh
python3 -m coverage run --branch --source=m3/release --omit='*/test_*.py' \
  --data-file=/tmp/m3-release.coverage \
  -m unittest discover -s m3/release -p 'test_*.py' -v
python3 -m coverage report --data-file=/tmp/m3-release.coverage --show-missing
python3 -m coverage json --data-file=/tmp/m3-release.coverage \
  -o /tmp/m3-release-coverage.json

python3 -m coverage run --branch --include='*/m3/tooling/name-contract/check.py' \
  --data-file=/tmp/m3-naming-receiver.coverage \
  -m unittest discover -s m3/tooling/name-contract -p 'test_check.py' -v
python3 -m coverage report --data-file=/tmp/m3-naming-receiver.coverage --show-missing
python3 -m coverage json --data-file=/tmp/m3-naming-receiver.coverage \
  -o /tmp/m3-naming-receiver-coverage.json
```

These commands measure the instrumented Python process; child-process execution
is not included. Preserve statement and branch totals, missing lines and test
failures, rather than quoting a percentage without its denominator. Run the
complete naming suite above independently: a scoped coverage measurement cannot
replace its current-authority and source-successor regressions. No percentage
here is a release threshold or a performance claim. The eleven mandatory gates in
`release-contract.json` still require current execution receipts, including the
OpenJDK platform matrix and the relevant String/JNI contracts.

## Mandatory proof pack

A release candidate must preserve:

- exact release commit and tree;
- exact OpenJDK upstream baseline;
- M3/Synexia rights and licensing notices;
- foundation build/test logs;
- Java precompute differential results;
- **changed-atom instrumented line/branch coverage**: reviewed TSV inventory,
  raw JaCoCo XML where applicable, each source SHA-256, >=90% per measured
  contract atom (99% objective for critical atoms), and explicit exclusions/HOLD;
- JNI/native parity results where JNI/native is used;
- Maven/OpenRewrite control-reactor execution receipt **produced inside M3jdk21**
  from M3jdk21 sources, with its exact repository, commit and recipe ID;
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

The existing single-fork-commit history requirement remains an explicit release gate.
Resolve any incompatible ancestry through separate reviewed policy; this preparation does not
consolidate, backdate or rewrite source history. Published release history and tags are immutable.
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
| Field | Value and authority |
| --- | --- |
| Display name | **M3JDK 21** |
| Machine product ID | `M3JDK21` |
| Version | `21.0.14`, from `make/conf/version-numbers.conf` |
| Preview tag | `m3jdk-21.0.14-m3.1-preview` |
| Release branch | `release/m3jdk-21.0.14-m3.1-preview` |
| Tier | `P0_TECHNICAL_PREVIEW` |

`verify_release_admission.py` checks the display name, numeric version, preview
tag and branch together against the configured JDK version. Public Java SE
owners retain their original names. Current additive companion packages use the
source-declared `jdk.m3.*` namespace and separately reviewed module exports.
Stable mapping IDs and historical donor symbols remain lineage data.
Both `Object.toAdapter(Class<T>)` and the exact compatibility spelling
`Object.toAdpater(Class<T>)` remain part of the current source contract.

## Recipe ownership and product acceptance

[AGENTS.md](../../AGENTS.md) and the
[porting invariant](../docs/m3jdk21-porting-invariant.md) place all recipe
creation, execution, qualification and improvement memory in canonical Synexia.
This target receives executed product postimages, target tests and passive
provenance receipts. It does not run a recipe command supplied in a receipt.

Under the 2026-10-31 standalone rule the existing mandatory gate
`M3_MAVEN_CONTROL_REACTOR` requires an execution receipt produced inside
`hsoliwal/M3jdk21`, including its exact commit and recipe ID. Every mandatory
gate is M3JDK21-owned. The
standalone dependency check exempts only the structured canonical gate-owner
metadata; actual external checkout/build/runtime references remain blockers.
Historical target-local recipe roots remain custody debt. Preserve their source
and dependencies until a separately reviewed, content-preserving migration has
been qualified in Synexia; do not introduce another receiver-side recipe copy.

## Claims and receiving order

The current contract records an immutable-text foundation with a repository test
surface. It does not claim a complete String backend, full regex precomputation,
full jtreg compatibility, performance superiority, qualified arrays/collections,
or SWT/Eclipse integration. Source presence and published tests describe what
can be checked; a passing current-source receipt establishes what was checked.

Runtime receiving follows **String → arrays → collections → AST/compiler →
remaining families**. The release contract further prioritizes regex and general
precompute within the String work before later layers. Inventory work may
proceed independently; only exact target evidence can advance a phase.

## Checks available during preparation

Run these from the target root after installing the pinned schema dependency:

```sh
python3 -m pip install -r m3/migration/requirements.txt
python3 m3/tooling/name-contract/check.py
python3 m3/tooling/name-contract/audit.py --strict
python3 -m unittest discover -s m3/tooling/name-contract -p 'test_*.py' -v
python3 -m unittest discover -s m3/release -p 'test_*.py' -v
```

These checks cover queue/map identity, current package/type/path declarations,
module boundaries, schema and stable-ID preservation, evidence admission,
release naming, artifact-path integrity, dependency and packaging refusals.
They are **release-tooling regression coverage**. They do not measure Java,
HotSpot, native, jtreg or line/branch coverage of the product.

The [naming source receipt](../tooling/name-contract/receipts/release-preparation/source-identity.json)
records the exact inspected baseline and original stable IDs. The strict census
keeps missing pending paths and present-but-unverified native paths visible;
it does not promote those capabilities. The four proposed renames stay `QUEUED`.

## Exact-source execution evidence

On a full checkout, write inspection output outside the source tree so the
report itself does not dirty the candidate:

```sh
python3 m3/release/verify_release_admission.py --root . \
  --json-out /absolute/path/to/proof/admission-development.json \
  --queue-out /absolute/path/to/proof/README.md \
  --sha256-out /absolute/path/to/proof/SHA256SUMS
python3 m3/release/verify_release_admission.py --root . --release \
  --evidence /absolute/path/to/proof/evidence.json \
  --json-out /absolute/path/to/proof/admission-release.json
```

`policy_valid` describes static policy checks. `release_ready` also requires a
clean exact source, all eleven mandatory execution receipts, no blocking custody
or dependency items, and the existing history gate. A development-policy pass
is insufficient. Missing, duplicate, stale, failed or malformed receipts keep
release admission on HOLD.

The `M3_RELEASE_EVIDENCE_V1` manifest binds `source_commit` and `source_tree`.
Each gate includes its owner, executed command, zero exit status, OS,
architecture, toolchain, and safe manifest-relative artifact paths with SHA-256.
The canonical recipe gate additionally records `canonical_recipe.repository`,
`commit` and `recipe_id`. Artifact bytes and structure can be verified locally;
review must still establish that the actual execution covers the claimed
contract. Synthetic verifier fixtures are never execution receipts.

## Product coverage required for release

The [candidate notes](RELEASE_NOTES.md) distinguish current-source preparation
checks from outstanding product gates. The proof pack must retain foundation,
Java differential, JNI/native parity, UTF-16 contract and OpenJDK sanity results,
plus rights, source/history, claims, canonical recipe and checksum receipts.
`STRING_UTF16_CONTRACT` remains mandatory even though the first-release workflow
has no dedicated producer bound to that ID. A source-built image and matching
jtreg/native/platform evidence must resolve it; do not infer it from a Python
suite or stock-JDK synthetic fixture.

A String backend claim additionally needs UTF-16/unpaired-surrogate semantics,
equals/hash/compare, substring/views, interning, exceptions, interpreter/C1/C2,
GC/lifetime and CDS where affected. Full regex precompute needs the contract's
10,000-case oracle and cache/invalidation/corruption proof. Performance claims
require exact same-host M3JDK/OpenJDK comparisons with raw CPU, allocation,
heap, native/RSS, GC, startup, cold/warm and variance evidence. See
[BENCHMARK_AND_COMPATIBILITY_SPEC.md](BENCHMARK_AND_COMPATIBILITY_SPEC.md).

## Retained source and history

The workflow exports the existing canonical `name-census.tsv`, retained-source
TSV and source summary as review artifacts, including on failure. They describe
exact paths/blobs and do not authorize a rename or archived-source promotion.
`retained_source_queue.py` resolves Git HEAD once and compares retained variants
to their original paths using that same immutable commit for both tree reads.
A later branch movement does not change the census or its recorded source.
Unresolved absent, divergent or mode-changing rows require current owner and
contract review. Release branches retain its strict refusal; exact path/blob
equality alone does not qualify behavior, history or runtime admission.

Resolve source loss or conflicts through the existing canonical Synexia recipe,
then receive and test its sealed postimage. Keep active and historical source,
license/NOTICE and receipts. A clean Git merge does not prove content or behavior
preservation. The dated [2026-10-29 validation snapshot](queue/2026-10-29/validation-summary.json)
retains its own tested pins and does not qualify a later candidate.

Preparation is append-only. The contract's historical
`single_fork_commit_required: true` requirement remains unchanged; a candidate
that does not satisfy it stays on HOLD. This preparation does not delete/rebuild
a branch, backdate authorship, rewrite ancestry or bypass the gate. Published
release history remains immutable. See the [current candidate checklist](RELEASE_CHECKLIST_CANDIDATE_2756.md) for
the evidence required before a tag or release can be admitted.
