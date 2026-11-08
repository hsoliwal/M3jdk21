# Fix: m3/ports/compiletime after the precompute rename (#2828)

#2828 renamed the `jdk.m3.precompute` types but missed one Maven dependent. `m3/ports/compiletime` declares
`m3-precompute`, and its reference scan was filtered with the compiletime directory excluded, because the rename was
first planned for three packages together. Three compiletime files kept old imports (`M3RegexShape`,
`M3RegexOracleHarness`, `M3RegexOracleCorpus`, `M3Progress`).

This fix is the same recipe (`rename_types.py` v1.6, `--package jdk.m3.precompute --string-members --string-words`)
run over the pre-rename precompute together with master's compiletime. Only the three compiletime files are taken.

Gate (`compiletime_fix_gate.sh`, WSL OpenJDK 21, ASM 9.6): compile the three files against precompute, with the
compiletime source path. compiletime as a whole needs M3JDK's own java.base (`jdk.internal.m3.mindex.compiler.ast`),
so it never compiles on a stock JDK.

| Configuration | Result |
|---|---|
| before #2828 (old compiletime + old precompute) | rc=0, 0 errors |
| master after #2828 (old compiletime + renamed precompute) | rc=1, 17 errors, all on precompute symbols |
| this fix (fixed compiletime + renamed precompute) | rc=0, 0 errors |

From now on, every rename first runs `pom_dependents.py <root> <artifactId> --require-checkout` (#2829,
PATTERN:RENAME_COMPLETENESS) and gates each dependent.
