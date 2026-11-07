<!-- SPDX-License-Identifier: Apache-2.0 -->
# census3-gaps-20261107 (m3/arrays)

Census pass 3 member-gap leaves of lane com-synexia-0e on `m3/arrays`: **P3-002** (`M3EventBuffer`), **P3-003**
(`M3SubsetSumBitset`), **P3-004** (`M3JavaModifierBits`), **P3-007** (`M3ArgumentRoleBits`, `M3StringTypeBits`).
Census pins: com.synexia develop `78cf2ad4c7e5`, M3jdk21 master `2274e18709b8` (`S:/m3-scratch/eb-conv/census3`);
donor re-read at develop-live head `78cf2ad4c7e5`, owner at master `e0d642e53b64`.

All five owners are scaffolds of `receive MIndex scaffold batch S01` (7c2accec550): declarations received, atoms not
yet received. The develop blobs of `MIndexArgumentRoleBits` (`7dc48bec582f`) and `MIndexStringTypeBits`
(`52545a951b49`) are the very blobs the scaffolds name in their lineage line.

| file | content |
|---|---|
| `DECISION.tsv` | one decision per unexplained MEMBER_GAPS row (15): RESTORE 4, HOLD_DEP 11 |
| `apply.py` | recipe CENSUS3_GAPS v1 (from `m3/collections/qualification/census-small-batch-20261106/apply.py`): hash-pinned anchored edits, `--check` fixed point, refuses drift (exit 2) |
| `EDITS.tsv` | file, before/after sha256, leaf, donor blob, rule (written by apply.py) |
| `oracle/census3/` | seeded differential driver + `OwnerBits`; `oracle/SIDE_MAP.tsv` + `mkdonor.py` generate the donor side |
| `differential.sh` | compiles both sides, runs default JIT and `-Xint`, requires byte-identical output |
| `mutants.py` | 10 mutants of the restored and received members, replayed through the differential |
| `GATES.tsv` | gates with numbers |

RESTORE (P3-007): `M3StringTypeBits.NUMERIC` and the private `requireKnown`/`add` of both bit sets. A private helper
is only reachable through its scaffold caller, so the same rule receives the atoms of `requireKnown`, `has`,
`hasAny` and `names` from the donor blob; the scaffold constructors still throw `M3NotYetReceived`, and the class
javadoc names what was received. Only the exception text maps the type name (`unknown M3String type bits`).

HOLD_DEP: `M3StringTypeBits.classify` and its four predicates (MIndexString, IndexCharacterFlags,
MIndexStringMetadata: com-synexia-4d), the four `M3EventBuffer` members (MIndexStructuralStore, MIndexNodeRef: 4d),
`M3SubsetSumBitset.canReach(int[], int, MIndexExecutionContext)` (4d), `M3JavaModifierBits.fromElementModifiers`
(needs a `requires java.compiler` edge in `jdk.m3.arrays`, which requires only java.base: a module-graph decision).

Replay: `python -I apply.py --check` (exit 0, changed=0); `mvn package`; build the donor classes from develop
`78cf2ad4c7e5` (javac -sourcepath over synexia-indexstring, synexia-common, synexia-job, synexia-native-interop,
synexia-algo, synexia-classic-tools, synexia-cop + re2j, lombok, commons-codec); then
`bash differential.sh <donor-classes> m3/arrays/target/classes <out> 200000` and
`python -I mutants.py <repo> m3/arrays/target/classes <out> <work> <donor-classes> 20000`.
