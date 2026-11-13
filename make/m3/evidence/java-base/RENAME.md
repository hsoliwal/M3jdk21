# java.base jdk.internal.m3 and jdk.internal.m3.hash: product prefix dropped (2026-11-08)

User rule 2026-11-08: no naming sprawl in the JDK, so types inside M3 packages carry no `M3` prefix.
`make/m3/tooling/jdk-naming/rename_types.py` v1.5 renamed 55 types (`RENAME.tsv`):
- 26 product types in `src/java.base/share/classes/jdk/internal/m3{,/hash}`;
- 29 tests in the default-package jtreg directories under `test/jdk/jdk/internal/m3` (new `--test-dir` option).

What follows the rename:
- references;
- qualified names in strings;
- the JNI symbols of the two libjava sources (`Java_jdk_internal_m3_ObjectMetrics_shallowSize0`,
  `Java_jdk_internal_m3_IndexMaskWindow_*`);
- `make/m3/test-local.sh`;
- four workflows;
- the hashing tools and their type ledger;
- the atom-test specs;
- docs and `doc/m3/name-mapping.json`.

HotSpot names none of these classes. Recipe, qualification, custody, compatibility and history records keep their
bytes.

v1.5 = v1.4 (lane 37: other-package guard, `--string-members`, java.* KEEP rule, evidence-segment skip) plus
`--test-dir` and underscore-tolerant donor names (`CityAndFarmHash_1_1`). Its output is byte-identical to the gated
tree and a second run changes nothing; no name hit the java.* KEEP rule.

## Gate (`gate.sh`, WSL OpenJDK 21 + GCC; before vs after, class names normalised)

The results are identical, apart from a timing value and the sort order of one renamed test:

| Check | Result |
|---|---|
| java.base patch build of `jdk.internal.m3{,.hash}` (+ the M3 `java.util` sources they use), `-Xlint:all,-serial` | rc=0, 2 warnings |
| JNI libraries `ObjectMetrics` and `IndexMaskWindow` (-Werror) | both build |
| patched `jdk.internal.m3` tests | 3 run, rc=0 |
| root tests | 11 of 13 pass (CompositeLiteralChecks 380,535 checks, StateMaskedIndexedViewTest 11,315, IndexMaskWindowJniTest 12,000 cases ...); the same two fail in both trees |
| hash | XxHash64StreamingTest 3/3; StreamingProof PASS; StreamingDump 55,134 cases, 0 mismatches |
| atoms and hash JUnit through the console with patched java.base | fail identically in both trees (harness limit, not the rename) |

## Package gate

Master at `4bf6aed07051` failed its own package gate with 7 `M3`-prefixed test types. They were added after #2817 by
pull requests merged while CI was queued. They are baselined here as marked debt (`# DEBT` block in
`PACKAGE_BASELINE.tsv`): 6 in `m3/collections` tests (owners) and 1 in `m3/arrays` (renamed by the jdk.m3.collections
move). With this change the gate reports 0 violations.
