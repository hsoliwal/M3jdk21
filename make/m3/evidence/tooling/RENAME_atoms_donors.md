# jdk.m3.tooling.atoms(.contract, .repository) and jdk.m3.tooling.donors: product prefix dropped (2026-11-08)

`rename_types.py` (DROP_PRODUCT_PREFIX v1) renamed 48 types (`RENAME_atoms_donors.tsv`) in m3/tooling/atom-census and
m3/tooling/donor-catalogue. Files, references (including atom-jit-gate, which imports them), qualified names in
strings, the module's verify scripts, READMEs and two workflows naming the mains all follow.

A recipe defect showed up in this step and was fixed: imports were read after the qualified rule had rewritten them,
so files in another package that import a renamed type kept the old simple name. That would have failed the compile
gate. Compression and search were unaffected, because all their references are inside their own package.

| Gate (WSL, OpenJDK 21) | Before | After |
|---|---|---|
| verify-contract.sh | checks=31, output hash 68ace689 | identical |
| verify-jni-projection.sh | checks=53, output hash 91776fc1 | identical |
| verify-companion.sh | 86 assertions, output hash 59b33130 | identical |
| donor-catalogue/verify.sh | 36 / 32 / 100005 / 256051 / 113203 checks | identical |
| atom-census + atom-jit-gate main sources, javac -Xlint:all -Werror | rc=0 | rc=0 |
| verify-repository-census.sh, verify-jni-export-join.sh | need a git checkout / clang; not runnable here, before or after | - |
