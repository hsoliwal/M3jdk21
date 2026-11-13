# Consolidation: jdk.m3.tooling.search folded into jdk.m3.search

M3jdk21 keeps a few fixed packages; a JDK has one search package, not two. #2793/#2794/#2798 had received the com.synexia
sorted-bounds stack as a separate module `make/m3/tooling/search` (package `jdk.m3.tooling.search`). This change moves
its 14 files into the existing module `m3/search` (package `jdk.m3.search`) and removes the old module root.

`consolidate_search.py` (CONSOLIDATE_PACKAGE v1) is the whole transformation; `MOVES_search.tsv` lists every old
path, new path and the rules each file needed:
- package `jdk.m3.tooling.search` -> `jdk.m3.search`; JNI symbols and javac -h header names
  `jdk_m3_tooling_search_` -> `jdk_m3_search_`;
- sources to `m3/search/src/{main,test}/java/jdk/m3/search/`, the two C sources to `m3/search/src/main/native/`
  (kept out of `CMakeLists.txt`: their qualification scripts generate the JNI header they include);
- READMEs and the two verify scripts to `m3/search/qualification/sorted-bounds/`, re-rooted at the module, the
  long-bounds script compiling only the sorted-bounds sources.
No class name collides in `jdk.m3.search`. The receipt evidence under `make/m3/qualification/receive-m3-tooling-search-*`,
`union-15117-*` and `receive-15186-*` keeps the old paths as a record of what was received.

## Gates

| gate | result |
|---|---|
| m3/search JUnit 5 console with the 7 CMake JNI libraries (`../forward-20261108/gate.sh`), main javac -Xlint:all -Werror now including the sorted-bounds classes | 164/164, as before the move |
| sorted-bounds/verify.sh (WSL, OpenJDK 21, GCC C17) | rc=0, M3_SORTED_INT_BOUNDS_TEST checks=169494 java+native PASS (same as before the move) |
| sorted-bounds/verify-long-bounds.sh | rc=0, M3_SORTED_LONG_BOUNDS_TEST checks=2286177 java+native PASS (same as before the move) |
