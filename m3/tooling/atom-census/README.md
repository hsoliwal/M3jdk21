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

## Repository-complete file-container census

`M3RepositoryFileCensus` is the first repository-complete coverage leaf. It reads one exact committed Git tree and
emits one `FILE_CONTAINER` atom for every tracked blob or gitlink. It never inventories the mutable working tree.
For regular files it streams the exact Git blob through one `git cat-file --batch` process and records a SHA-256 of
the committed bytes. A path-derived atom ID stays stable when content changes. Reviewed moves preserve identity
through the exact-tree `contract/FILE_IDENTITY_OVERRIDES.tsv` table; unreviewed renames remain visible as delete/add.

```text
./verify-repository-census.sh
```

The verification script compiles the leaf with Java 21 and `-Xlint:all -Werror`, executes the dependency-free fixture
proof, generates the entire repository census twice at `HEAD`, and requires byte-identical outputs. The accepted run
is copied to the repository-root `build/m3-repository-file-census/` directory as an ignored build artifact, avoiding a cryptographic self-reference
between a tracked ledger and its own source hash.

Generated files:

- `FILES.tsv`: exact Git mode, object identity, source byte size/hash, source family, owner and file atom ID;
- `ATOMS.tsv`: strict `M3-ATOM-COVERAGE/1` file-container rows;
- `EVIDENCE.tsv`: all required file-container gates, with only inventory, source identity, family-pattern binding and
  the present normative contract marked `PASS`; every unexecuted gate remains explicit `NOT_RUN`;
- `REPOSITORY_CENSUS_RECEIPT.tsv`: commit, root tree, object format, override count, census counts and output digests
  without timestamps.

This leaf establishes complete file representation and exact source identity. It does not claim language-level atom
coverage, licence closure, correctness coverage, performance proof or product admission. Language-specific atomizers
must consume `FILES.tsv` family by family and add semantic atoms without removing the enclosing file container.
