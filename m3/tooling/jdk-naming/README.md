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
| `RENAME_MAP.tsv` | every path move of the rename (`path_before`, `path_after`), written by the tree-mode apply; a scoped apply (`--only`) replaces only the rows of the files it selects and keeps every other row byte for byte |
| `RENAME_RESEAL.tsv` | every carried pin (`kind before after`) and every `AMBIGUOUS` value left alone |
| `test_jdk_naming.py`, `test_reseal.py` | real-Git fixtures; section 5 of the standard is itself a fixture |

## Stages

```text
plan --tree HEAD                       counts moves and rewrites per rule; refuses a name collision; reports
                                       SHADOW declarations named like a renamed package's first segment (jdk)
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

`.github/workflows/m3jdk-naming.yml` runs the recipe tests, the tree fixed point and the JUnit conformance on every
pull request. The standalone check (`m3/release/check_standalone.py`) must not grow a D-kind; its N-kinds drop.


## Historical recipe-memory guard integrity (2026-11-07)

The append-only `RECIPE_MEMORY.tsv` has three historical entries where the
`guard` column contains prose instead of a `file#testMethod` pointer:
`PATTERN:SPLIT_PACKAGE_NAME_COLLISION`,
`PATTERN:JVM_DESCRIPTOR_PREFIX`, and
`PATTERN:DONOR_FACT_AND_ESCAPED_SPANS`.

Keep the original eight-column events unchanged; do not relabel an old
correction as though it had been written correctly at the time. The
`RECIPE_MEMORY_GUARD_BINDINGS.json` file is a **three-record, exact-match
audit overlay**, not another recipe or runtime registration. For each entry it
seals the UTC time, pattern, recipe, original malformed guard, corrected
`file#method` test and the original evidence file.

`test_jdk_naming.py` rejects stale bindings, new malformed/unregistered
guards, nonexistent test methods, missing evidence files, or changes to a
bound entry's timestamp, trigger, recipe or original guard. It checks that all
eight event fields are present; it does not byte-seal every historical row.
The three new checks verify current file-name collision ownership, Java
classfile descriptor strings, and historical donor/sidecar alignment. They
are **source/receipt checks**; they do not rerun historical product proofs or
replace the source-built product, Maven, JUnit, JNI, jtreg or performance gates.

Re-run `python3 -I m3/tooling/jdk-naming/test_jdk_naming.py` and
`python3 -I m3/tooling/jdk-naming/test_reseal.py` on the exact
candidate tree. Never suppress a malformed future guard by adding a wildcard
or by making the parser ignore rows.

## pom_dependents.py (2026-11-08)

The dependents closure of a Maven artifact from the HEAD tree: `python pom_dependents.py <root> <artifactId> --require-checkout` prints every module whose pom depends on the artifact and exits 2 when one of them is not in the working tree. Run it before `rename_types.py`, `jdk_naming.py apply` or any REFS rewrite in a sparse worktree, add the missing modules to the sparse set, and gate them with a consumer build (install the renamed artifact, package each dependent). Guard of the RECIPE_MEMORY row PATTERN:RENAME_COMPLETENESS of 2026-11-08 (#2829).

## fold_package.py (2026-11-08)

FOLD_PACKAGE v1: one package of one module joins another package of another module (approved layout: families fold
into their owner). `python -I fold_package.py --root . --from-root m3/nlp --from-package jdk.m3.nlp --to-root m3/search
--to-package jdk.m3.search --retire-to m3/evidence/search/nlp --aggregator m3/pom.xml` moves the package's sources and
resources. The package declaration, imports, and the dotted, path, full-path and JNI references of its types follow;
a package that only shares the prefix (jdk.m3.nlp.search) does not. The module's records move byte for byte to the
evidence dir, and its reactor line goes. Refused: sub-packages, a split package, a name already in the target package,
native sources in a retired module. Run `rename_types.py --package <target>` afterwards to drop the M3 prefix.
v1.1 (m3/compiler fold): `--retire-only` retires every sibling first (so no fold rewrites a sibling's records),
`--aggregator` matches the module path relative to the reactor, and a module's other resources (META-INF/services,
fixtures) move with the fold, service files merged by line union. Tests:
`test_fold_package.py`, `test_rename_types.py` (both now in m3jdk-naming.yml).

## NAMING_KEEP.tsv (2026-11-08)

Files outside this recipe that name Synexia/com.m3 on purpose are listed here, one row per file:
`path<TAB>reason_class<TAB>reason`. The reason classes are DETECTOR (negative tests), PIN (checkers matched against
pinned TSVs), DONOR_DIFFERENTIAL (harnesses that compile the real com.synexia sources), MAVEN_COORDINATE (the real
groupId com.m3) and LINEAGE_ID (historical ids). `jdk_naming.py` leaves these files out of scope, as it does `SELF`.
A malformed row stops the recipe; a row whose file is gone fails `test_jdk_naming.py`. Every other file still has to
reach the fixed point of `plan --tree HEAD` (user decision 2026-11-08: explicit KEEP list, not a looser rule).

