# jdk.m3.importer: product prefix dropped (2026-11-09)

`rename_types.py` v1.7 renamed the 16 types of jdk.m3.importer (m3/m3-import; `RENAME.tsv`), run with `--string-words
--comment-words --doc m3/m3-import/`. What follows:
- m3-full-delivery-a3.yml (`-Dtest`);
- .m3/atomize-patternize/public-verify.sh (its source list and main class).
The hash-pinned copies of these classes in the migration-recipes resources are records and keep their bytes.

Inventory: pom_dependents gives 0 dependents. The module compiles with the JDK and JUnit only (its
m3-openrewrite-recipes dependency serves the rewrite plugin). No open PR touches it.

## Gate (WSL OpenJDK 21), before vs after

| | Before | After |
|---|---|---|
| javac main / test (-Xlint:all -Werror), make/m3/evidence/native/gate_module.sh | rc=0 / rc=0 | same |
| JUnit | 50 found: 48 pass, 2 fail | same, per-test status identical (59 rows) |
| .m3/atomize-patternize/public-verify.sh | rc=0 | rc=0 |

The 2 failures need the vendored recipe sources under m3/vendor/m3, which the partial checkout does not hold. They fail
the same way on both sides.
