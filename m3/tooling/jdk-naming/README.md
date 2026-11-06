<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# JDK naming for M3JDK21 (recipe JDK_NAMING_RENAME)

User decisions (2026-10-31): M3JDK21 is standalone and self-sufficient, and Synexia names are renamed to JDK naming.
The legacy recipe files are renamed and kept as M3JDK21-owned code, so they are not deleted and not restored from
another repository. #2185 and #2186 are closed as superseded.

## Scheme

| Before | After |
|---|---|
| `com.synexia.m3.*`, `com.synexia.*` packages | `jdk.internal.m3.*`; the exported API becomes `jdk.m3.<area>` |
| `com/synexia/...` source paths | `jdk/internal/m3/...` |
| `M3Synexia*`, `Synexia*` types | `M3*` |
| `SYNEXIA_*` constants and environment names | `M3_*` |
| `synexia-*` and `synexia_*` names and paths | `m3-*` and `m3_*` |
| modules (new) | `jdk.m3.<area>` |

Some lines and files are left alone:
- Copyright, SPDX and license lines.
- Links to the Synexia repository, `secrets.SYNEXIA_*` and Maven `<groupId>com.synexia...` lines. Those are dependencies, or Maven coordinates mapped in a later phase, which the standalone check counts and the ABSORB, MOVE or RETIRE leaves remove; renaming would only hide them.
- Lineage: Markdown, TSV ledgers, provenance and evidence JSON, `evidence/` and `receipts/` folders, NOTICE/LICENSE, `m3/docs` and release audits.
- A lineage path whose new name already exists stays where it is (KEEP_LINEAGE_PATH).

## Tool

`python -I jdk_naming.py plan|apply [--root DIR] [--tsv OUT] [--only PREFIX]...`

- `plan` lists every path move and the rewrites per rule for every in-scope file. It refuses a plan when two names would collide.
- `apply` runs `git mv` and the rewrites, then runs a second plan, which must be empty (fixed point). It refuses while any file it would change is not checked out.
- `test_jdk_naming.py` covers packages, types, constants and paths; untouched copyright lines, links and lineage; the collision refusal; `--only`; and the fixed point.

## Dry run on master `bb66a8c16642`

- 8,719 path moves.
- Rewrites in 2,702 checked-out files. Another 4,375 files need a wider checkout before `apply`.
- 0 refusals.
- Token counts:

| Rule | Tokens |
|---:|---:|
| KEBAB | 58,657 |
| PKG_PATH | 28,568 |
| PKG | 12,826 |
| PKG_M3 | 1,993 |
| PKG_PATH_M3 | 809 |
| SNAKE | 295 |
| CONST | 177 |
| TYPE | 95 |
| TYPE_M3 | 30 |

- Per area: `RENAME_PLAN_SUMMARY.tsv`.

## Phases

Each phase is one PR. Each is gated by the module compile and tests of the areas it touches, their `verify*.py`, re-pinning of any hash-pinned manifest whose files changed, and the standalone check, whose N-kind counts must drop and whose D-kinds must not grow.

1. **Configuration names:** `SYNEXIA_*` constants and environment names in workflows and scripts. A small set, checked by CI.
2. **Live M3 areas, one at a time, largest first** (see the summary): `m3/tooling/*`, `m3/collections`, `m3/arrays`, `m3/runtime-integration`, `m3/synexia-import` (becomes `m3/m3-import`), `m3/vendor/synexia`.
3. **Legacy recipe roots and retained variants:** these are renamed and kept, per the decision; their hash-pinned fixtures are re-pinned by their own recipes.
4. **`com.m3.*` → `jdk.m3.*` and `jdk.internal.m3.*`,** with the module declarations `jdk.m3.<area>`.
