# jdk.m3.search: product prefix dropped (2026-11-08)

User rule 2026-11-08: no naming sprawl in the JDK, so types inside M3 packages carry no `M3` prefix.
`make/m3/tooling/jdk-naming/rename_types.py` (DROP_PRODUCT_PREFIX v1) renamed all 51 types of `jdk.m3.search` in
`m3/search` (`RENAME.tsv`). Files, references, qualified names in strings, the JNI symbols and headers of the 9 C
sources, and the sorted-bounds verify scripts follow. One declared edit: the long-bounds script's source glob
`M3*Sorted*Bounds*.java` becomes `*Sorted*Bounds*.java`, matching the same 8 files. One consumer comment in
`m3/indexdb` names the classes. Evidence under `m3/search/qualification/{relocation,forward,consolidate}-*` keeps
its historical names.

The module-pack `make/m3/tooling/module-packs/nlp-search` declares the same package `jdk.m3.search` in its own source
tree (`M3File`, `M3Search`, ...): a split package left for the search module move.

| Gate | Before | After |
|---|---|---|
| m3/search JUnit 5 console with the 7 JNI libraries (Windows JDK 21, MinGW; javac -Xlint:all -Werror) | 164/164 | 164/164 |
| sorted-bounds/verify.sh (WSL, OpenJDK 21, GCC) | rc=0, checks=169494 | rc=0, checks=169494 |
| sorted-bounds/verify-long-bounds.sh | rc=0, checks=2286177 | rc=0, checks=2286177 |
| recipe fixed point (second run) | - | no change |
