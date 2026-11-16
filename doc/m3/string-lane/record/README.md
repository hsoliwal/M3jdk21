# Run 20261116-string-converge

String lane, 2026-11-16: converge string sources into the M3JDK string owners, and precompute leaf 1.
Invariants, state and next leaves: [docs/m3/string-lane/HANDOVER.md](../HANDOVER.md).

## Pins

| What | Value |
|---|---|
| com.synexia develop (donor listing) | `a7b7bb639802` |
| M3jdk21 master (owner listing and blobs) | `e3c8bb1d0e3` |
| M3jdk21 master when leaf 1 was committed | `20babfeadd1c` (the three edited blobs equal the pin) |
| Leaf 1 | M3jdk21 #3152, commit `906c2fceeaeb`, merged as `0c6107abd9be` |
| Host | Windows 11, Git Bash, JDK 21 at `C:/Program Files/Java/jdk-21`, Python 3.12 |

## Commands and results

| Step | Command | Exit | Result |
|---|---|---|---|
| Donor names | `git ls-tree -r --name-only origin/develop` filtered by name pattern | 0 | 332,513 Java paths, 5,426 string-named main files |
| Owner names | `git ls-tree -r --name-only e3c8bb1d0e3` | 0 | 90,139 Java paths, 472 string-named main files |
| Names join | `tools/string_join.py <donor list> <owner list> <out>` | 0 | 5,426 rows: 884 live, 3,240 noise copies, 1,302 recipe owners; live: 749 name absent, 93 name in M3JDK, 42 name in java.base |
| Owner planes | atomize kit `atomize.sh` + `atom_planes.py planes` over 132 owner files | 1, then 0 | parsed 132 of 132, 5 atomizer errors, 7,073 atoms |
| Donor routes, first pass | `tools/run_string_apl.sh` over 1,872 indexstring-family files, owner side 132 files | 1 (atomizer: 6 `INVALID_AST` errors, tables written) | 64,928 atoms, 1,813 types: 0 converge, 198 shared logic, 1,615 absorb |
| Donor routes, wider owner side | `tools/run_owner2.sh`, owner side 552 files | 1 (atomizer: 8 errors, tables written) | 25 converge, 495 shared logic, 1,293 absorb; atoms: 2,503 exact, 2,829 logic, 31,322 near, 723 absent (`routes/`) |
| Leetcodify pass 0 | atomize kit, `M3LeetCodifyRecipe` over the 132 owner files | 0 | 231 sites in 44 files: 195 nested loops, 25 manual binary searches, 11 array shifts (`data/LEETCODIFY_SITES_STRING_OWNERS.tsv`) |
| `value()` site guards | `tools/value_site_guards.py` | 0 | 52 sites: 28 behind a guard with return, 3 ternary, 14 other receiver, 5 no guard variable, 1 mixed guard, 1 accessor (`data/STRING_VALUE_SITE_GUARDS.tsv`; lexical proposals) |
| Leaves 2 and 3 proof | `leaf2/CodePointGateProof.java`; `tools/compile_gate.sh` v1.2 | 0 | 97,656 strings, 0 mismatches; compile gate 0 new diagnostics (M3jdk21 #3154, merged `fed60d8a7400`) |
| Leaf 4 proof | `tools/compile_gate.sh` v1.2 | 0 | compile gate 0 new diagnostics; invariant by source reading (M3jdk21 #3155, merged `b7539452eb6c`) |
| Scout workflows | eight workflows, scripts in `workflows/` | 2 complete; 6 cut short by the usage limit | `scout/SCOUT_CANDIDATES.tsv`: 84 candidates, 36 survived a skeptic, 6 refuted, 42 unverified (their skeptics died on the limit); the parsers, univec and javac-optimization scouts returned nothing |
| Leaf 5 | `tools/compile_gate.sh`; new jtreg test run on the stock JDK | 0 | compile gate 0 new diagnostics; 819 checks (M3jdk21 #3158, merged `82131e6ef9e9`) |
| Leaf 6 proof | `leaf6/run_proof.sh` | 0 | 597,871 strings, 7 checks, 0 mismatches, 4 discriminators detected (M3jdk21 #3160, merged `b1c5723683a8`) |
| Leaf 7 proof | `leaf7/run_proof.sh` (kernels run inside a patched stock JVM) | 0 | head = base = oracle; copy bound holds (M3jdk21 #3161, merged `0db78e2e9f58`) |
| Leaf 9 | `tools/compile_gate.sh`; new jtreg test run on the stock JDK | 0 | compile gate 0 new diagnostics; 420 checks; source naming gate green before merge (M3jdk21 #3168, merged `83d1ceca0e07`) |
| Consumer censuses | `maven/census.sh` on the Maven fork, the vendored SLF4J and upstream LSP4J | 0 | Maven 4,365 call sites (47% builder), SLF4J 76 (80% builder), LSP4J 148 (71% builder): `maven/`, `slf4j/`, `lsp4j/` |
| Leaf 10 proof | `leaf10/run_proof.sh`: base and head `StreamEncoder` patched into the stock JDK | 0 | 7,040 cases, 11 charsets: bytes and write sequences identical for stock, base and head; ns per write stock 52/113/164, base 277/318/458, head 48/128/170 (M3jdk21 #3169, merged `fa31ad3ed029`) |
| JUL and library censuses | `git grep` over the M3jdk21 tree at `83d1ceca0e07` | 0 | `jul/` (160 call sites in 9 files), `forks/LIBRARY_STRING_SITES.tsv` (7 vendored libraries), `forks/FORKS.tsv` (81 forks) |
| Leaf 1 proof | `leaf1/run_proof.sh` | 0 | `leaf1/PROOF.tsv`: 11 checks, 0 mismatches, 3 discriminators detected |
| Leaf 1 compile gate | `leaf1/compile_gate.sh` (v1.1) | 0 | `leaf1/COMPILE_GATE.txt`: base and head exit 0, 0 new diagnostics |

Not run: anything inside an M3JDK VM, the jtreg test, timing.

## Files

- `data/STRING_CONVERGE_MODULES.tsv`: per donor root, names in java.base, in M3JDK, absent.
- `data/STRING_CONVERGE_JOIN_LIVE.tsv`: the 884 live rows (donor type, concept key, status, first owner path).
- `data/JAVA_LANG_STRING_BLOBS.tsv`: blob ids of the 28 java.lang string owners at the pin.
- `data/STRING_VALUE_SITES.tsv`: `String` methods that contain a `value()` call site, with counts.
- `leaf1/`: patch, proof harness, compile gate and their outputs.
- `tools/string_join.py`: names-only scout. A name match is a proposal, never a decision.
- `tools/run_string_apl.sh`: donor run over the atomize kit.

Head source hashes of leaf 1 (SHA-256, first 16 hex digits): `M3StringFacts.java` 499d9454d3571584,
`String.java` 6207ee6c0cfd8449.
