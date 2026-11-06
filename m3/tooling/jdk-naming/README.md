<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# JDK naming for M3JDK21 (recipes JDK_NAMING_RENAME, JDK_NAMING_RESEAL)

The standard is [NAMING_STANDARD.md](NAMING_STANDARD.md); its executable form is [NAMING_RULES.tsv](NAMING_RULES.tsv).
User decisions (2026-10-31): M3JDK21 is standalone and self-sufficient, every M3 name follows JDK naming
(`jdk.m3.<area>` exported, `jdk.internal.m3.<area>` internal), and the legacy Synexia-named code is renamed and kept.

## Files

| File | Purpose |
|---|---|
| `NAMING_RULES.tsv` | the one rule table: ordered `TEXT` rules, `KEEP_LINE`, `KEEP_SPAN`, `KEEP_PATH` |
| `jdk_naming.py` | rename recipe: `plan` and `apply`, on the checkout or on a tree (`--tree REV --ref REF`) |
| `reseal.py` | carries sha256, blob and path pins in manifests and receipts to the renamed files |
| `conformance/` | Maven module; `JdkNamingConformanceTest` reads the same table and fails on any leftover |
| `RENAME_MAP.tsv` | every path move of the rename (`path_before`, `path_after`), written by the tree-mode apply |
| `RENAME_RESEAL.tsv` | every carried pin (`kind before after`) and every `AMBIGUOUS` value left alone |
| `test_jdk_naming.py`, `test_reseal.py` | real-Git fixtures; section 5 of the standard is itself a fixture |

## Stages

```text
plan --tree HEAD                       counts moves and rewrites per rule; refuses a name collision
apply --tree HEAD --ref refs/heads/X   one commit: moved paths, rewritten blobs (bytes kept, CRLF kept), RENAME_MAP.tsv
reseal.py --base B --tree X --ref X    one commit: pins carried round after round to a fixed point, RENAME_RESEAL.tsv
plan --tree X                          must print moves=0 rewrites=0 refusals=0
mvn -f conformance/pom.xml test        JUnit agrees: no violation
```

Tree mode reads blobs with `git cat-file --batch` (no lazy fetch) and writes with `git fast-import`, so a sparse or
partial checkout renames the whole tree. The first rename (#2201) worked on the checkout and missed files that were
not checked out and whose paths did not name Synexia; it also dropped CRLF in 4 files and left callers of moved
`check_synexia_*.py` scripts unchanged because its path rules and text rules differed. Paths and text now use the
same rules.

## Gates

`.github/workflows/m3-jdk-naming.yml` runs the recipe tests, the tree fixed point and the JUnit conformance on every
pull request. The standalone check (`m3/release/check_standalone.py`) must not grow a D-kind; its N-kinds drop.
