# jdk.m3.compiler: six one-package modules folded into one (2026-11-08)

Approved layout (user, 2026-11-08, "Approve as shown"): jdk.m3.compiler = compiler + ast. Step 1 of the family folds
the six compiler modules that no workflow compiles directly into the owner module m3/compiler, package jdk.m3.compiler:

| Module (retired) | Package | Descriptor | Types |
|---|---|---|---|
| m3/compiler/lifetime-slots | jdk.m3.compiler.lifetime | org.m3jdk.compiler.lifetime | 6 |
| m3/compiler/mat-sortable | jdk.m3.compiler.matsortable | org.m3jdk.compiler.matsortable | 4 |
| m3/compiler/nullness-facts | jdk.m3.compiler.nullness | org.m3jdk.compiler.nullness | 8 |
| m3/compiler/parent-owned | jdk.m3.compiler.parentowned | org.m3jdk.compiler.parentowned | 2 |
| m3/compiler/reference-flow | jdk.m3.compiler.reference | org.m3jdk.compiler.reference | 13 |
| m3/compiler/static-use | jdk.m3.compiler.staticuse | org.m3jdk.compiler.staticuse | 8 |

Net effect: 6 roots and 6 packages become 1 of each (package_gate: roots 726 -> 721, product packages 383 -> 378).
The 41 types (40 of them M3-prefixed) dropped the prefix. jdk-standard (jdk.m3.compiler.standard), source-signals
(jdk.m3.compiler.signals) and m3/ast follow in the next step. Seven workflows compile jdk-standard and source-signals
by path and copy their Plugin descriptor, so those workflows move with them.

## Recipe (FOLD_compiler.tsv holds the rows)

    # pass 1: every sibling retires first, so no fold rewrites a sibling's records
    fold_package.py --root . --from-root m3/compiler/<m> --from-package jdk.m3.compiler.<p> --to-root m3/compiler \
      --to-package jdk.m3.compiler --exclude make/m3/tooling/jdk-naming/ --retire-to make/m3/evidence/compiler/<m> \
      --aggregator make/m3/pom.xml --retire-only
    # pass 2: the folds
    fold_package.py ... (same, without --retire-only)
    rename_types.py --root . --package jdk.m3.compiler --exclude make/m3/tooling/jdk-naming/ --doc m3/compiler/ \
      --string-words --comment-words

fold_package v1.1 records three corrections this fold found (RECIPE_MEMORY 2026-11-08):
- SIBLING_RECORD_REWRITE: `--retire-only`;
- REACTOR_RELATIVE_MODULE: make/m3/pom.xml names modules as compiler/<name>;
- NON_PACKAGE_RESOURCES: nullness-facts' META-INF/services javac Plugin descriptor and test fixtures move with the
  fold; service files merge by line union.
Every module's records (README, GATES, PROVENANCE, RELOCATION_MAP, RECIPE_MEMORY, recipe/, verify.sh, pom,
descriptor) keep their bytes in make/m3/evidence/compiler/<module>/. The //FLLDCIM anchors in the moved sources are
unchanged. The one on reference-flow's descriptor sits byte for byte in its evidence copy.

## Inventory and gate

- pom_dependents: 0 dependents for each of the six artifacts.
- A reference scan of m3/, .github/ and make/ found no live consumer of the six modules outside themselves, the
  reactor and the baseline. The workflows name only jdk-standard and source-signals.
- No simple-name collision among the six packages, with or without M3.
- `gate_fold_compiler.sh` (WSL OpenJDK 21; release 21, -Werror, -proc:none; class path as surefire):

| | Before: 6 modules | After: m3/compiler |
|---|---|---|
| javac main / test | rc=0 / rc=0 each (nullness main -Xlint:all,-missing-explicit-ctor; reference-flow tests -lossy-conversions) | rc=0 / rc=0; main -Xlint:all, 0 diagnostics |
| JUnit | 2 + 2 + 4 + 1 + 3 + 8 = 20 found, 20 pass | 20 found, 20 pass |
| per-test status (names normalised, timings removed) | - | identical, 31 rows |
| package_gate on the post-change path list | - | violations=0 |
| rename fixed point | - | second run: no change |

Without the descriptors, nullness no longer needs its main-source lint exception: missing-explicit-ctor reports only
in a named module. The descriptors had 0 Maven dependents and no module-path consumer, and the javac Plugin
registers through META-INF/services, as before.
