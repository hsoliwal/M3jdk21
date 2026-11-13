# jdk.m3.jni: product prefix dropped (2026-11-08)

`rename_types.py` v1.7 renamed the 24 types of jdk.m3.jni in m3/native (`RENAME.tsv`), run with `--string-words
--comment-words --doc m3/native/`. The JNI symbols of src/main/native/m3_scoped_leases.c follow. The hand-written
header include/m3_jni_core.h and tests/m3_jni_core_contract.c name no Java type. The qualification/ oracle copies
keep their bytes.

Inventory:
- pom_dependents gives 0 dependents, and no open PR touches the module;
- 11 workflows and make/m3/arrays/CMakeLists.txt name only m3/native/ directories (headers, sources), which do not
  change; none names a jdk.m3.jni type.

## Gate (`gate_module.sh <tree> <out> m3/native`, WSL OpenJDK 21; m3-collections by -sourcepath), before vs after

| | Before | After |
|---|---|---|
| javac main / test (-Xlint:all -Werror, -h) | rc=0 / rc=0 | same |
| JUnit | 35 found: 32 pass, 3 aborted | same, per-test status identical |
| m3_scoped_leases.c (gcc -Werror against the javac -h headers) | rc=0, 3 Java_ symbols declared | same |
