# jdk.m3.intern: product prefix dropped (2026-11-08)

`rename_types.py` v1.7 renamed the 49 types of jdk.m3.intern (make/m3/intern; `RENAME.tsv`), run with `--string-words
--comment-words --doc make/m3/intern/`. What follows:
- the JNI symbols of the two test natives (src/test/native);
- the one consumer outside the module: m3/ports/indexstring (MIndexClassPool import, README).
The 47 //FLLDCIM anchors are unchanged, and the module's recipe/ records keep their bytes.

Inventory: pom_dependents gives one dependent, m3/ports/indexstring. A scan of m3/, .github/, make/, src/ and test/
found no other live reference, and no open PR touches either module.

## Gate (WSL OpenJDK 21), before vs after

| | Before | After |
|---|---|---|
| make/m3/intern javac main/test (-Xlint:all -Werror, -h), make/m3/evidence/native/gate_module.sh | rc=0 / rc=0 | same |
| make/m3/intern JUnit | 31/31 | same, per-test status identical |
| test natives (gcc -Werror against the javac -h headers) | 2/2, each Java_ symbol declared | same |
| consumer chain, make/m3/evidence/precompute/gate_ports.sh (intern from source -> precompute, regex-cursor, indexstring) | all javac rc=0; precompute 123/125 (2 aborted), regex-cursor 54/54 | same |
