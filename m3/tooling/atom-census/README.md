# M3 atom census (atomize, then patternize, any Java repository)

`M3AtomCensus` lists the atoms of any Java repository and classifies each one into a closed pattern catalogue. It
works the same way on M3JDK's own OpenJDK sources and on every other repository. It is a CPU tool: javac's own parser
(`com.sun.source`, parse only, so no second AST) reads the sources, and no LLM is involved.

- **Atom:** a static method with a body. The census records its type, signature, statements, loops and calls. An
  atom is *pure* when it writes no field and does not synchronize; array-element writes are recorded separately.
- **Pattern:** the first row of `PATTERN_RULES.tsv` whose method-name and body expressions match, otherwise
  `UNKNOWN_PATTERN`. Each row names its catalogue: LeetCode, HackerRank and GeeksforGeeks problem categories, a JDK
  contract, or a GoF pattern.
- **Output:** `ATOM_CENSUS_<repo>.tsv` and `PATTERN_SUMMARY_<repo>.tsv`. Rows are sorted, so a second run is
  byte-identical.

```text
./run.sh <repo-label> <out-dir> <source-root>...
```

## Patternizer passes

Each pass reads the largest `UNKNOWN_PATTERN` clusters of the previous census. It adds catalogue rows ahead of the
generic ones, then reruns every repository. `PASSES.tsv` keeps the history, pinned to each source revision:

| Pass | Rules | M3JDK java.util | commons-collections | eclipse-collections | guava |
|---:|---:|---:|---:|---:|---:|
| 1 | 17 | 65.6% unknown | - | - | - |
| 2 | 27 | 44.5% | 73.3% | 73.8% | 61.3% |
| 3 | 34 | 34.4% (of 1,745 atoms) | 17.9% (of 756) | 24.1% (of 2,313) | 40.2% (of 2,564) |

`census/` holds the pass-3 census of the four repositories.

This census is the catalogue dimension. It joins with the structural L-Hash clustering (com.synexia #14999) on
`path` and `method`.

The two later passes:
- **JIT:** C2 inlining decisions per atom; see the ARRAY_MAP crate's `M3ArrayMapJitTest`.
- **Replace:** an atom may be swapped for a catalogue implementation only under its unchanged contract, through a
  recipe.
