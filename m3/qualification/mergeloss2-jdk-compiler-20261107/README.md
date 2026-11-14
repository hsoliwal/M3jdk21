<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss audit phase 2, leaf 103: scoped M3AST source worksets (2026-11-07)

Leaf 103 of the phase-2 merge-loss audit (`CLEAN_0E.tsv`, lane com-synexia-0e). Origin `6f151d7ad12`, "Reuse
unchanged binary dependencies in scoped original M3AST compilations" (canonical recipe
`com.synexia.m3.AstScope20261020`), binds an explicit file/package/module/project source workset to an unstarted
javac task:

- `M3AST.Scope`, `M3AST.ScopeKind` and `M3AST.open(task, retainSyntax, scope)`;
- the `scope=` plugin option and the scoped `open` in `M3ASTPC`;
- `M3ASTPC.beforeSourceRead`, which refuses an unselected source before javac reads it;
- the declaration check in `requireDeclaration`;
- `JavaCompiler.beforeSourceProcessing` at the four source-read sites, which is a no-op while no scope is bound.

The retain merge `4c9dc30c47e4` (Superset: retain PR #1559) kept master's three owners and parked the PR's copies
under `m3/qualification/master-landing-20261028/retained-variants/pr1559/`.

Decision: RESTORE. `apply.py` runs the 3-way merge itself: ours = pinned master blob, base = origin parent,
theirs = origin = retained copy. The `M3AST` and `JavaCompiler` merges are clean. `M3ASTPC` has five conflict hunks
of declared shape. Four have an empty base and resolve as ours, then theirs. The fifth is the session-open line:
master's `retainSyntax || relations != null` is kept, inside the origin's scoped-open choice. Any other hunk count,
shape or postimage hash is refused.

The parser-pack half of the origin (its scope contracts and `M3ASTIndex` changes under `m3/tooling`) is leaf 116,
lane com-synexia-4d, and is not part of this PR. The origin's `M3ASTScopeContract` was run against the result as a
gate (GATES.tsv).

| file | purpose |
|---|---|
| `apply.py` | recipe MERGELOSS2_RESTORE_3WAY v1 |
| `EDITS.tsv` | before/after sha256, origin, dropping merge, 3-way record per file |
| `DECISION.tsv` | the decision with evidence |
| `GATES.tsv` | executed gates with numbers |
| `gate103.sh` | the compiler-patch / parser-module / consumers / contracts gate (after `m3astpc-pr2357/parser_pack_gate.sh`) |

```text
python -I m3/qualification/mergeloss2-jdk-compiler-20261107/apply.py [--check]
bash m3/qualification/mergeloss2-jdk-compiler-20261107/gate103.sh <tree archive> <dir with the 3 restored owners> <out>
```
