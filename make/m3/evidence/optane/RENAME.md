# jdk.m3.optane and jdk.internal.m3.optane: product prefix dropped (2026-11-08)

`rename_types.py` v1.7 renamed 52 types (`RENAME.tsv`), run with `--string-words --comment-words --doc m3/optane/`.
These follow the rename:
- files and references;
- the JNI symbols of the 5 C sources and the CMake list;
- the module docs;
- PORT-MAPPING.pending.json.
The module's recipe/ directories are history and keep their bytes.

Inventory: pom_dependents gives 0 dependents. A scan of m3/, .github/, make/, src/ and test/ found no live reference to
either package outside m3/optane, and no open PR touches the module.

`DECLARED_EDITS.tsv` lists the one edit outside the recipe, the same as for precompute (#2828). Four replay tests passed
the prefix "M3" to differential dumps that build class names as package + prefix + name; they now pass "". The gate
found it: 4 tests failed with ClassNotFoundException jdk.m3.optane.M3PmemTaskLog until the edit.

## Gate (`gate_optane.sh`, WSL OpenJDK 21), before vs after

| | Before | After |
|---|---|---|
| javac main (-Xlint:all -Werror, -h) | rc=0, 49 classes, 3 headers | same |
| javac test | rc=0, 43 classes | same |
| JUnit | 97 found: 57 pass, 35 fail, 5 skipped | same, per-test status identical (117 rows) |
| C sources (gcc -Werror against the javac -h headers) | 5/5 compile; every Java_ symbol declared | same |

The 35 failures are environment failures on both sides: the CPU host has no PMEM native library
(NoClassDefFoundError M3NativePmem / UnsatisfiedLinkError m3pmemsdk / native Linux capability missing).
