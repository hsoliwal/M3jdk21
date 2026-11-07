<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# recipe curate-bc-20261107 (CURATE_M3_COLLECTIONS phases B+C)

**Decision.** On 2026-11-07 the user decided to curate jdk.m3.collections before absorbing more. The rules:
- the public surface is strict, and implementation detail lives in `jdk.internal.m3.collections` (phase A1, M3jdk21 #2781);
- public types go into family subpackages;
- JDK-style names drop the `M3` prefix unless a public `java.*` simple name would clash.

The user approved `RENAME_MAP.tsv` as drafted (com.synexia #16846).

## What changes

- **Public set.** `PUBLIC_SET.tsv` lists 161 public types; 145 of them are renamed or moved.
- **Families.** The new exported packages are `jdk.m3.collections.bits`, `.concurrent`, `.graph`, `.range`, `.sketch`,
  `.text` and `.packed`. Core types stay in `jdk.m3.collections` (e.g. `M3IntArrayList` -> `IntArrayList`). `M3ArrayList`,
  `M3TreeMap`, `M3Collections`, `M3ProgressMonitor` and other names that clash with `java.*` or `javax.*` keep the prefix.
- **Stay-core.** Four types keep their new names but stay in the core package, because the compiler showed
  package-private coupling to core helpers: LongLongRangeMap, ObjectRangeMap, BigArray and BigList (`STAY_CORE.txt`, `STAY_CORE_WHY.tsv`).
- **Module.** `module-info` exports the new packages.

## The recipe (`curate_rename_bc.py` v1.5)

It is a fixed point: `--check` reports no change. Every Java file under `m3/` changes as follows:
1. The owner file moves to the new package and name.
2. Imports and fully qualified names follow, in dot, slash and descriptor form, in code, comments and literals.
3. The simple name follows in code and comments wherever the file sees the type.
4. Cross-package references created by a move get explicit imports.
5. A clash guard falls back to the fully qualified name where the new simple name would collide in a file.

Type names stored as data follow too: a literal that is exactly the old name, or the old name as a `.` `/` `$` segment.

Two things change outside Java files:
- JNI symbols move with their classes.
- The compiler processors' `RELOCATION_MAP.tsv` rows (LITERAL and annotation tokens) follow, and their recipes are re-run to a fixed point. Parent-owned gained a declared TYPE row `M3ParentOwned -> ParentOwned`.

These are never rewritten:
- history: qualification records, retained variants, snapshots, history directories;
- resources, which include hash-pinned snapshots and frozen fixtures;
- other recipes' `RELOCATION_MAP.tsv` files. They stay the record of what those recipes produced. Replay chains them: the recipe first, then this one.

Two tests pin data, so they get declared edits (`BC_TEST_EDITS.json`):
- a sorted list of type names is re-sorted;
- a frozen preimage fixture keeps its pinned sha256, and its test translates the old names when compiling it.

## Proof (base = M3jdk21 master bd58d585c946)

See `GATES.txt`.

| Check | Result |
|---|---|
| Module build and tests | `-Werror`, 2278 tests, 0 failures, 0 errors, 135 skipped, identical before and after |
| Bytecode (`BYTECODE.tsv`) | all 1317 class files of jdk.m3.collections and jdk.internal.m3.collections javap -v identical once every renamed type and source-file name maps back |
| Reactor gate | the affected modules with `-am` are green; the only failing rows (`REACTOR_FAILING.tsv`) are already in master's full-run baseline FULL_VENDORHOLD2_FAILING.tsv (tooling/migration-recipes custody pin) |
| viewport-core (`-Pm3-extended`) | green |
| atom-jit-gate (standalone) | 40 tests green |
| Loose recipe/probe sources (`LOOSE_PARITY.tsv`) | no source that compiled before fails now |
| Drift | the 4 master commits after the base touch none of these paths and reference no renamed type |
