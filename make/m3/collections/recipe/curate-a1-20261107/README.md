<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# recipe curate-a1-20261107 (CURATE_M3_COLLECTIONS phase A1)

**Decision.** On 2026-11-07 the user decided to curate before absorbing more. The `M3<SynexiaName>` classes in one
flat exported package are not JDK-quality, so the public surface becomes strict: about 100-150 reviewed types. Implementation
detail moves to the non-exported package `jdk.internal.m3.collections`. The plan and inventory are in com.synexia #16833
(`synexia-m3-recipe/recipes/curation-jdk-m3-collections-plan-20261107`).

**Scope of phase A1.** It moves the 435 classes in `MOVING.txt` from `jdk.m3.collections` to
`jdk.internal.m3.collections`. Simple names, code and behaviour are unchanged.
- **Module-local.** Every moved class is used only inside `m3/collections`: no other module, no string in another
  module and no hash-pinned resource names it. The classes other modules use are left for phase A2, which adds qualified exports.
- **What stays.** The curated public set (`PUBLIC_SET.tsv`, 148 types), and the classes coupled to it by non-public access.
  Package-private classes in an exported package are not API.

## How the move set was decided

1. **Rules.** `PHASE_A.tsv` is built from the inventory, the public set and package-coupling components from bytecode. In it, 495
   classes are internal and 471 of those are module-local.
2. **Compiler fixed point** (`curate_a1_loop.sh` + `curate_a1_unmove.py`). Bytecode misses inlined constants and
   some package-private constructors, so the compiler decides. Each round clones the base tree, applies the recipe,
   builds main and test code with `-Xlint:all -Werror`, and un-moves the moved side of every coupling the build reports.
   It also keeps every type an exported signature, supertype or annotation default exposes.
   - `UNMOVED.tsv` records the 36 classes kept and why.
   - Tests are not API, so each sits where it compiles. `TEST_MOVE.txt` / `TEST_STAY.txt` hold the 6 tests the compiler
     placed: 2 moved, 4 stayed. In all, 147 test files moved with their classes.

## The recipe (`curate_apply_a1.py`, v1.3)

The recipe changes these things:
1. The file path and package line of each moved class.
2. A test whose module references are all moved classes (or that the compiler placed) moves with them.
3. Imports, static imports, nested imports and fully qualified names of moved classes, in dot, slash and type-descriptor
   (`Ljdk/.../X;`) form, in code and string literals.
4. Explicit imports for references the split turns into cross-package ones. Test-side helpers are included.
5. JNI symbols `Java_jdk_m3_collections_<X>_` and FindClass paths of moved classes in the C sources.

It is a fixed point: `--check` reports no change after a run.

## Proof (base = M3jdk21 master tree 10899a4; curated = base + recipe)

- **Build.** Main and test code compile with `-Werror`, BUILD SUCCESS.
- **Tests.** The base and the curated tree both run **2278 tests, 0 failures, 0 errors, 135 skipped** (`TESTS.txt`).
- **Bytecode.** All **1317 class files are javap -v identical** once the package rename is mapped back
  (`BYTECODE.tsv`: 716 moved, 601 stayed). The constant pool is compared order-independently, and line-number tables
  are excluded because added imports shift lines.
- **JDK naming.** The plan over m3/collections is unchanged from the base, with the same pre-existing test-side items.
- **Drift.** Every modified or deleted path equals the tested base on master 6540b35f8bb9, every added path is new,
  and no file was added to the module's sources since the base.
