<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 release preparation: names, documents and test coverage

Status: **preparation / not a release admission receipt** (2026-11-07).
Product: `M3JDK21`, Java 21 compatibility target.
Current preview identifier: `m3jdk-21.0.14-m3.1-preview`.
The release name is a candidate; no tag or product-image qualification is asserted here.

## Naming freeze

Keep public Java 21 owners in their original packages and names (`java.lang.String`,
`java.util.*`, `java.util.regex.*`), with source/binary/behavior compatibility
in the M3JDK21 direction. Internal M3 owner names are distinct from public Java
APIs. Never rename a public type into an `M3*` facade; never rename an existing
JNI symbol or serialized/reflected name mechanically.

The existing authority is [name-mapping.json](../docs/name-mapping.json) and
[the name contract](../docs/M3JDK21_NAME_AND_PACKAGE_CONTRACT.md); the existing
name-plan is a *queue*, not an admission receipt. Run:

```sh
python3 m3/tooling/name-contract/check.py
python3 m3/tooling/name-contract/audit.py --strict
python3 -m unittest discover -s m3/release -p 'test_*.py' -v
```

Do not bulk-reformat `src/**` during release hardening. Any approved internal
rename must have an exact mapping ID, donor/rights evidence, deterministic
recipe execution and binary/JNI/reflection/serialization compatibility proof.
Naming transformations and recipe custody follow `AGENTS.md` and the release
contract: Synexia owns canonical Maven/OpenRewrite authoring and execution.
`M3_MAVEN_CONTROL_REACTOR` consumes a source-bound, hash-verified execution receipt
as passive data; the product verifier never executes its recorded command.
M3JDK21 owns product build, runtime and target acceptance gates. Release still
requires zero executable dependencies on another repository. The canonical
receipt does not establish installed-image acceptance or complete runtime coverage.

## Documentation freeze

Review [the claims contract](release-contract.json), [release notes](RELEASE_NOTES.md),
[publication checklist](RELEASE_CHECKLIST.md), upstream OpenJDK version, licensing
and Synexia NOTICE on the **same release commit**. Avoid describing queued
adapters/companions as installed-JDK features. Separate P0 foundation tests
from proof of an integrated `java.lang.String` replacement.

Verify all branch/tag references against the contract; `test_release_metadata.py`
now guards this drift. Re-pin all examples, source links, generated reports and
SHA-256 evidence to the final product commit after the last merge.

## Coverage qualification: do not substitute test counts for code coverage

| Area | Existing test surface | Required release evidence |
| --- | --- | --- |
| Naming/receipts | `m3/release/test_*.py`, name-contract tests | executed Python checks, zero failures, source-bound logs |
| M3 text foundation | `m3/build.sh` | JDK 21 test executions including `-Xint`, C2 and UTF-16/unpaired surrogate cases |
| Java precompute | `m3/ports/precompute` Maven tests | differential result, corpus/case count, failures and raw logs |
| JNI/native | precompute `-Pnative`, checked JNI receivers | compiled native build, parity/lifetime tests on matched images and OS/arch |
| JDK public behavior | OpenJDK sanity workflows, jtreg tests | tested **M3JDK21 image**, jtreg group/case counts, fails, skips, regression diffs |
| Arrays and collections | module tests and independent provider-contract tests | contract-by-contract JUnit, mutations/views/concurrency/serialization and ABI |
| Swing/SWT coupling | separate SWT backend matrix | exact SWT revision, matched Java/native artifacts, GUI, resource and JNI evidence |

The machine-readable preview contract now requires
`M3_CHANGED_ATOM_COVERAGE` with at least 90% instrumented line and branch
coverage **for every inventoried changed M3 contract atom**. A source
SHA-256, complete inventory TSV and its SHA-256, raw report artifact, and
explicit zero-unmeasured count are required; branchless atoms need an
explanation. Produce the receipt fragment with
`build_changed_atom_coverage.py` from a real JaCoCo XML and reviewed
source/method inventory; a fragment without the original raw artifacts and
the current release manifest is not admissible. This does not
establish that every changed source atom was inventoried — reviewers must
compare the inventory against the actual release Git diff.

For each contract-bearing **atom**, record its source file/method, behavior
branches, matching tests, error/null/overflow/concurrency boundaries, baseline
oracle and raw result. A 90%+ line/branch **engineering target** is not an achieved
coverage statistic. Aim for 99% in safety-critical atoms, but first collect
instrumented line **and branch** reports (JCov/Jacoco as applicable), with total
covered/missed counts and explicit exclusions. Treat unavailable instrumentation
as `PENDING`, never as 0% or 100%. Count unique tests, skips and disabled tests
separately. For JDK modules, do not substitute library-JAR test coverage for
a source-built installed-image/jtreg run.

## Building the changed-atom coverage receipt (data only)

Once instrumented JaCoCo XML has been produced by the actual M3 product/test
run and the full set of *changed contract-bearing methods* has been reviewed,
build its pinned receipt fragment:

```sh
python3 m3/release/build_changed_atom_coverage.py \
  --root . \
  --report /absolute/evidence/jacoco.xml \
  --inventory /absolute/evidence/changed-atoms.tsv \
  --report-artifact jacoco.xml \
  --inventory-artifact changed-atoms.tsv \
  --tool 'JaCoCo instrumented M3JDK21 build; exact version in producer logs' \
  --reviewed-complete-inventory \
  --out /absolute/evidence/changed-atom-coverage-fragment.json
```

The tab-separated inventory contains columns
`id`, `class`, `method`, `descriptor`, `source_path` and optional
`branchless_explanation`. JaCoCo class names use JVM slashes,
and `descriptor` is the JVM method descriptor. The importer does
**not** infer inventory completeness from JaCoCo and without
`--reviewed-complete-inventory` sets `inventory_complete: false`.

Place the generated JSON object into the matching
`M3_CHANGED_ATOM_COVERAGE` evidence gate's `coverage` property.
The outer manifest must include the raw XML and original inventory TSV in the
gate's SHA-256-verified `artifacts`, and bind the exact product commit
and tree. The importer output alone never qualifies a release. Check actual
source diff versus inventory before setting review complete.

## Release refusal conditions

No publication-ready assertion until the independent product build, ABI/source
compatibility, platform/JNI/jtreg gates, no-loss history audit, legal notices
and source-bound artifact SHA-256 receipts pass. `policy_valid` does not mean
`release_ready`; use the existing verifier:

```sh
python3 m3/release/verify_release_admission.py --root . --release \
  --evidence /absolute/path/to/release-evidence.json \
  --json-out admission-release.json
```

A failing or absent gate is recorded as **PENDING/HOLD** and stays in the queue.
The [SWT release-readiness pack](https://github.com/hsoliwal/eclipse.platform.swt/tree/master/docs)
is a separate downstream qualification and never upgrades an M3JDK runtime claim.

## Reproduce validator regression coverage

The naming receiver and release-evidence verifier have their own measured Python
regression scope. Their malformed-input fixtures exercise controlled refusal of
ambiguous declarations, unsafe source paths and invalid receipt shapes. A legal
Java identifier suffix such as `$Extra` must not establish ownership of the
shorter mapped type. A duplicate owner or JSON key must not be resolved by input
order. Rejected evidence must produce a useful gate diagnostic.

With `coverage.py` installed in the development environment, measure each owner
separately from the repository root:

```sh
python3 -m coverage run --branch --data-file=.coverage-naming \
  --include='*/m3/tooling/name-contract/check.py' \
  -m unittest discover -s m3/tooling/name-contract -p test_check.py -v
python3 -m coverage report --data-file=.coverage-naming -m
python3 -m coverage json --data-file=.coverage-naming -o naming-tool-coverage.json

python3 -m coverage run --branch --data-file=.coverage-release \
  --include='*/m3/release/verify_release_admission.py' \
  -m unittest discover -s m3/release -p test_release_admission.py -v
python3 -m coverage report --data-file=.coverage-release -m
python3 -m coverage json --data-file=.coverage-release -o release-verifier-coverage.json
```

Keep the exact tested commit/tree, full test logs, covered/missed counters and
raw JSON together. The two explicit `--include` selections define tooling-only
measurements; compare percentages only when the source identity and denominator
also match. Child-process execution is not instrumented by these commands.
Neither synthetic receipts nor validator coverage satisfy the instrumented M3
contract-atom, installed-image, jtreg, JNI or SWT platform gates above. The full
live naming authority remains an independently executed gate.

## Preparation regression evidence — 2026-11-07

The source-bound continuation on receiving commit
`6540b35f8bb9cd2d8b2ee89aa3d5bbcbc642422a` preserves the existing release
contract and adds 17 release-admission regression tests. All 52 original
admission test methods and their helper/setup bodies remain unchanged. The
complete release suite passes **141 tests**. Its unchanged release verifier
executes **487/487 statements and 248/248 branches**, with zero implementation
exclusions. This includes actual script-entry-point execution in the measured
process; child processes are still outside the instrumented scope.

The new cases check rights boundaries, receiving-phase promotion, malformed
map/queue/evidence records, divergent or unavailable Git history, relative
evidence paths and an artifact-directory replacement between validation steps.
That filesystem case qualifies the tested interleaving only. A standalone
baseline is also tested in both modes: development acceptance does not waive
the release naming HOLD. Synthetic complete receipts remain validator fixtures,
not authenticated build, native or product execution evidence.

The public-target and full-family policy readers retain their original guards
while restoring protocol identities from the preserved source authorities.
`SYNEXIA_FIRST_PARTY`, `SYNEXIA_FIRST_PARTY_APACHE2_V1`, the original catalogue
path, stable porting-invariant ID and receipt-field names are provenance and
serialization contracts. They are not target package names. Their spelling
must not be changed by a general naming substitution. Source pins, catalogue
records, legal anchors and receiving-phase state are unchanged.

The complete existing public-target policy workflow passes locally, including
both live validators, legal anchors and **32 tests**. The existing 13 test
method IDs remain and 19 methods add source/rights, malformed-input and
premature-promotion refusals. Exact production coverage from these tests is:

| Tool | Statements | Branches |
| --- | ---: | ---: |
| Release verifier | 487/487 — 100% | 248/248 — 100% |
| Public-target policy reader | 122/127 — 96.06% | 61/66 — 92.42% |
| Full-family policy reader | 130/135 — 96.30% | 77/82 — 93.90% |

The release-verifier row is measured with the complete release suite:

```sh
python3 -m coverage run --branch --data-file=.coverage-release \
  --source=m3/release --omit='*/test_*.py' \
  -m unittest discover -s m3/release -p 'test_*.py' -v
python3 -m coverage json --data-file=.coverage-release -o release-tool-coverage.json
```

The canonical `m3-release-preparation-20261107` task retains the exact inputs,
executed recipe outputs, source hashes, raw reports and earlier failed
attempts in its continuation-4 evidence. The product publication binds the
exact canonical proof revision. These scoped results do not close the
repository-wide naming fixed point, legacy recipe custody, global provenance,
installed-image, jtreg/JNI, platform or authenticated-producer gates.
