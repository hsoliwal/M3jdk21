<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK 21 release preparation

[Release contract](release-contract.json) · [Candidate notes](RELEASE_NOTES.md) ·
[Checklist](RELEASE_CHECKLIST.md) · [Visible queue](queue/README.md)

M3JDK21 is one integrated Java/HotSpot/JNI SDK. Release claims require evidence
for the exact candidate commit and tree. A naming pass, finite component test,
recipe replay or historical CI run does not establish installed-JDK acceptance.

## Product and version names

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

The existing mandatory gate `M3_MAVEN_CONTROL_REACTOR` therefore consumes an
execution receipt owned by `hsoliwal/com.synexia`, including its exact canonical
commit and recipe ID. Every other mandatory gate is M3JDK21-owned. The
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
clean exact source, all ten mandatory execution receipts, no blocking custody
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
`retained_source_queue.py` compares retained variants to their original paths in
Git HEAD; unresolved absent, divergent or mode-changing rows require current
owner and contract review. Release branches retain its strict refusal.

Resolve source loss or conflicts through the existing canonical Synexia recipe,
then receive and test its sealed postimage. Keep active and historical source,
license/NOTICE and receipts. A clean Git merge does not prove content or behavior
preservation. The dated [2026-10-29 validation snapshot](queue/2026-10-29/validation-summary.json)
retains its own tested pins and does not qualify a later candidate.

Preparation is append-only. The contract's historical
`single_fork_commit_required: true` requirement remains unchanged; a candidate
that does not satisfy it stays on HOLD. This preparation does not delete/rebuild
a branch, backdate authorship, rewrite ancestry or bypass the gate. Published
release history remains immutable. See the [checklist](RELEASE_CHECKLIST.md) for
the evidence required before a tag or release can be admitted.
