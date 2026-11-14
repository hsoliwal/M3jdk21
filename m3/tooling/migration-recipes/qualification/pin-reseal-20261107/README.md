<!-- SPDX-License-Identifier: Apache-2.0 -->
# Hash-pinned crate reseal after the JDK-naming rename

`M3HashPinnedJavaSnapshotRecipe` loads a crate only when two things hold:

- every template named in its `manifest.tsv` exists;
- every template's sha256 equals the pinned postimage.

The rename fe35ea98841 rewrote template text and moved template files. It treats `.tsv` as documentation, though,
so the pins and the template-name column were never carried. `pin_audit.py` finds 23 of 232 crates that the recipe
refuses to load (PIN_AUDIT_BEFORE.tsv).

This change carries only pins that are proven:

- 29 rows where the rename's own rule table (`jdk_naming.rewrite_text`), applied to the pinned pre-rename bytes,
  reproduces today's template byte for byte.
- 4 rows whose template-name column still named the pre-rename file.
- The m3-full-delivery-a3 test template. It receives the identifier rule of #2635: the rename had left
  `Path synexia` / `fixture.synexia()` while the target owners moved on.

Left as listed debt, not changed (RENAME_PROOF.tsv, DRIFT_CLASS.tsv):

- 55 rows where today's rules no longer reproduce the template exactly (later edits or rule changes);
- 9 pins that were already stale before the rename;
- 12 templates that are absent.

Each of these needs its crate owner. The recipe tests were not run, because the module's
`8.17.1-m3-source-1` artifacts and `com.synexia:m3-openrewrite-recipes` are not resolvable here.
