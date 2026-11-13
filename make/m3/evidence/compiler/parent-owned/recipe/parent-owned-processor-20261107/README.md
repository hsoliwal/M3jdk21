<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# recipe parent-owned-processor-20261107 (RELOCATE_PARENT_OWNED_PROCESSOR v1)

Relocates the first-party Synexia `M3ParentOwnedProcessor` (synexia-primitives, `com.synexia.primitives`) and its
JUnit test into the new module `m3/compiler/parent-owned`. The donor is `hsoliwal/com.synexia` develop
`eaacff5931ef16a1a52425043bb71b821eda3515`, processor blob `9bb160d48a44`, test blob `3bb0d6528b7b`.

```sh
python -I m3/compiler/parent-owned/recipe/parent-owned-processor-20261107/relocate.py \
  --donor-git <com.synexia git dir> --donor-rev eaacff5931ef16a1a52425043bb71b821eda3515 [--check]
```

- **Engine:** `relocate.py` is the engine of `m3/compiler/mat-sortable/recipe/mat-sortable-processor-20261107` (RELOCATE_MAT_SORTABLE_PROCESSOR v1). Only two lines differ: `RECIPE_PATH` and the first docstring line.
- **Rows:** `RELOCATION_MAP.tsv` declares every change.
  - PACKAGE: `com.synexia.primitives` -> `jdk.m3.compiler.parentowned`.
  - LITERAL: the canonical annotation name in `@SupportedAnnotationTypes` and in the test fixture's import, which become `jdk.m3.collections.M3ParentOwned` (owner landed by M3jdk21 #2743).
  - EXTERNAL: JUnit.
  - Four sha-pinned REPLACE rows:
    - the import of the relocated annotation, which the donor reached through its own package;
    - the explicit form of the implicit public constructor (`-Xlint:all -Werror` in an exported package);
    - removal of the donor's block-comment license header from each of the two files. The engine writes the same text as the M3JDK `//` header.
- **Provenance:** `PROVENANCE.tsv` pins the donor commit, blobs and sha256 of both files.

## Why a new module

The processor was absorbed with `synexia-primitives` wave 27 (M3jdk21 #2744). It was held because `jdk.m3.collections`
has no `java.compiler`, and an annotation processor does not belong in the collections module graph. Peer 0e owns
`m3/compiler/static-use`, which is deliberately java.base-only. Their decision: a sibling module following the
static-use (#2675) and mat-sortable (#2738) pattern. See `DECISION.tsv`.

## Proof

See `GATES.tsv`:
- **Recipe:** `--check` is a fixed point.
- **Package:** passes with `-Xlint:all -Werror` and 0 warnings. The donor's JUnit test is green.
- **Bytecode:** the relocated processor is identical to the donor compiled in its own package, modulo the two
  package names. The comparison uses full `javap -v -p -c -constants` with the constant pool compared order-independently
  (`javap_v_equal.py`), so annotation values and string-concat recipes count. A one-literal mutant is refused.
- **Naming and siblings:** jdk_naming is at a fixed point, and the static-use siblings gate passes for all 7 `m3/compiler` modules.
