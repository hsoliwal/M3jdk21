# jdk.m3.precompute and jdk.m3.regexcursor: product prefix dropped (2026-11-08)

`rename_types.py` v1.6 renamed 173 types (`RENAME.tsv`) in m3/ports/precompute and m3/ports/regex-cursor, run with
`--string-members --string-words`. What follows the rename:
- files and references;
- the consumer `m3/ports/indexstring` (`IndexTrieNavigation` import, README);
- one workflow;
- the module provenance table and `verify_regex_string_v2.py`;
- string literals that name these classes.

Hash-pinned recipe manifests under `m3/tooling/migration-recipes` are seals of recorded files and keep their bytes,
as do the module's recipe and evidence directories.

Recipe v1.6 adds the opt-in `--string-words`: inside string literals of files of the package (or importing it), every
whole-word `M3Rest` of a renamed type follows. The gate found two forms that need it: class names assembled at runtime
(`pkg + ".M3LiteralBatchCandidate$Plan"`) and record `toString` expectations (`"M3CursorLiteralSearch[97, 98]"`).
`DECLARED_EDITS.tsv` lists the one edit outside the recipe: two planner tests passed the prefix `"M3"` to a differential
dump that builds class names as prefix + name, and now pass `""`.

## Gate (`gate_ports.sh`, WSL OpenJDK 21; before vs after)

| Module | Before | After |
|---|---|---|
| m3/intern (dependency) javac main + test | rc=0 / rc=0 | identical |
| m3/ports/precompute javac main + test; JUnit | rc=0; 125 found, 123 pass, 2 aborted | identical |
| m3/ports/regex-cursor javac main + test; JUnit | rc=0; 54/54 | identical |
| m3/ports/indexstring (consumer) javac main + test | rc=0 | identical |
| JNI: 9 C sources (precompute natives, regex-cursor test native) gcc -Werror against javac -h headers of the module's classes | 9/9 compile; every Java_ symbol in a generated header | identical |
| rename fixed point | - | second run: no change |

Without `--string-words` and the declared edit, the gate showed 3 precompute and 1 regex-cursor failure, which is why
both exist.
