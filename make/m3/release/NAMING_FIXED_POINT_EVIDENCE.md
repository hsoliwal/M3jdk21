<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Release naming fixed-point refusal: exact 2026-11-07 evidence

**Disposition: HOLD (historical run).** This document is a data-only
release-qualification receipt summary. It is not a naming recipe, a recipe
mirror, a product postimage, or a claim that today's `master` still has
the same 91 candidates.

## Exact observed run

| Source | Value |
| --- | --- |
| GitHub workflow | [M3 JDK naming standard run 42366163089](https://github.com/hsoliwal/M3jdk21/actions/runs/42366163089) |
| Pull request | [#2765](https://github.com/hsoliwal/M3jdk21/pull/2765), subsequently merged |
| CI synthetic merge commit | `d9a471468cf93a38f6ddd89e1d2fa737d2ab9b92` |
| Job | naming, source tree fixed-point plan |
| Captured evidence | GitHub Action artifact `naming-plan`, artifact ID `13571976323` |
| Data file | `naming-plan.tsv`, 19,201 UTF-8 bytes (header and 91 operations) |
| Data SHA-256 | `9dd460f701a163af33a0549e6c057d601624058c643de04b6a0830e8a6caed71` |
| Test phase before fixed-point | 24/24 naming source/guard tests and 6/6 reseal tests passed |
| Naming fixed-point result | **FAIL**: `moves=35 rewrites=56 refusals=0` |
| Product JVM/JNI/jtreg coverage | **Not established by this naming plan** |

The historical failure that assumed every recipe-memory `guard` cell contained
`file#testMethod` was fixed by #2765 without changing the 21 existing
append-only `RECIPE_MEMORY.tsv` entries. The separate fixed-point failure is
not solved by that correction.

## Candidate group census

The categories below are a lossless **row-count classification** by original
path prefix, not automatic rename authority.

| Original path category | MOVE | REWRITE | Rows | First action |
| --- | ---: | ---: | ---: | --- |
| `make/m3/runtime-integration/regex-rope-affix-effects/` | 34 | 13 | 47 | Seal historical recipe/code/fixtures and decide canonical Synexia custody before any move |
| `m3/ports/indexstring/recipe/` | 0 | 26 | 26 | Check stub/donor package identities; do not blindly rewrite historical fixtures |
| `m3/collections/` | 1 | 6 | 7 | Separate current tests from recipe/qualification evidence and register source map IDs |
| `m3/search/` | 0 | 4 | 4 | Reconcile exact historical search-recipe pins and their downstream consumers |
| `make/m3/tooling/name-contract/` | 0 | 3 | 3 | Check canonical map/plan authority and preserve intentional negative-case fixtures |
| Other workflow/compiler/jtreg paths | 0 | 4 | 4 | Qualify each name with its actual contract and owner |
| **Total** | **35** | **56** | **91** | **HOLD** until provenance and gates pass |

Some candidates are source-sealed fixture names or deliberate negative controls;
a textual preflight does not prove they should be renamed. Do not delete old
source, repair test fixtures by weakening expected assertions, overwrite donor
attribution, or assume a source history merge means no code was lost.

## Reproduction and release acceptance

For the **historical** artifact, download the `naming-plan` artifact from
the linked Actions run. Verify its SHA-256 above before inspecting rows. The
`path` field holds the original path and the third field holds the proposed
target or rule counts. Do not treat it as an executable migration ledger.

For the **current** candidate, on its own complete, clean source checkout:

```sh
git rev-parse HEAD 'HEAD^{tree}'
python3 -I make/m3/tooling/jdk-naming/test_jdk_naming.py
python3 -I make/m3/tooling/jdk-naming/test_reseal.py
python3 -I make/m3/tooling/jdk-naming/jdk_naming.py plan --tree HEAD \
  --tsv /absolute/evidence/current-naming-plan.tsv
python3 make/m3/tooling/name-contract/check.py
python3 make/m3/release/audit_release_identity.py --root . --release
```

The plan is **read-only**, a candidate preflight. A release naming fixed-point
pass requires `moves=0 rewrites=0 refusals=0` on the **exact release tree**,
plus source-sealed recipe/postimage provenance, matched JDK/API/JNI/ABI
compatibility, legal notices and existing public-name gates. Any required
recipe correction is authored and executed in the canonical Synexia workspace
under the repository's receiving invariant. Existing target-local recipe copies
are migration residue, not an invitation to run or mirror recipe authors here.

The release manifest still needs its eleven mandatory exact-commit execution
receipts, including changed-atom instrumented line/branch coverage.
Passing only the naming fixed point cannot make the product release-ready.
