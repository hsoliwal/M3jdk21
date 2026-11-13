<!-- SPDX-License-Identifier: Apache-2.0 -->
# census3-gaps-20261107 (m3/ports/precompute)

Census pass 3 member-gap leaf of lane com-synexia-0e on `m3/ports/precompute`: **P3-010** (`M3EditDistancePlan` vs
develop `MIndexStringEditDistancePlan`, `M3TextSignals` vs `MIndexTextSignals`). Census pins: com.synexia develop
`78cf2ad4c7e5`, M3jdk21 master `2274e18709b8` (`S:/m3-scratch/eb-conv/census3`); donor re-read at develop-live head
`78cf2ad4c7e5`, owner at master `08fa7d2e7052`. The owner is the 2026-10-07 M3 port (5e69cd13be0, ad0a1584b10,
72f28bc3c0c, 82abfa561ef) with its own Myers lanes, work budgets and trigram seams; the donor added the legacy
code-unit flag and the exact incremental Frontier API after that port.

| file | content |
|---|---|
| `DECISION.tsv` | one decision per unexplained MEMBER_GAPS row (58): RESTORE 33, PRESENT_EVOLVED 13, HOLD_DEP 12 |
| `apply.py` | recipe CENSUS3_GAPS v1 (from `make/m3/collections/qualification/census-small-batch-20261106/apply.py`): hash-pinned anchored edits, `--check` fixed point, refuses drift (exit 2) |
| `EDITS.tsv` | file, before/after sha256, leaf, donor blob, rule (written by apply.py) |
| `oracle/census3/` | seeded differential driver + `OwnerFrontier`; `oracle/SIDE_MAP.tsv` + `mkdonor.py` generate the donor side |
| `differential.sh` | compiles both sides, runs default JIT and `-Xint`, requires byte-identical output |
| `mutants.py` | 14 mutants of the restored members, replayed through the differential |
| `GATES.tsv` | gates with numbers |

RESTORE (`M3EditDistancePlan`): `compile(CharSequence, boolean)`, `usesUtf16CodeUnits()` (the flag is observable,
both values keep UTF-16 units), `FrontierLimits` (record, `DEFAULT`, validation), `frontier()`,
`frontier(FrontierLimits, M3Progress)`, `Frontier` (plan/limits/inputUnits/cellUpdates/primitivePayloadBytes/
distance/rowCopy, `minimumPossibleDistance`, `within`, the three `append` forms, `admittedLength`, `extend`),
`advanceRow`, `frontierCheck`. `ProgressMonitor` maps to the module's `M3Progress` (null monitor = no-op, as in the
donor). The private constructor takes the flag as its last parameter (private, recorded in GATES 7_javap).

HOLD_DEP (com-synexia-4d): `appendDirect(ByteBuffer, FrontierBackend, ProgressMonitor)`, `FrontierBackend`,
`FrontierNative`, `nativeFrontierAbi`, `nativeAdvanceFrontier` (the JNI_REQUIRED backend needs the indexstring
native frontier kernel and loader; publishing the JAVA half alone would publish a backend that can never load);
`M3TextSignals.characterKeys()/bigramKeys()` (only caller `MIndexCandidateIndex`, no M3 owner).

PRESENT_EVOLVED: `distance(CharSequence, Workspace)` / `within(CharSequence, Workspace, int)` (owner forms take an
`M3Progress`), `withinStringBand`/`restoreBandExterior` (owner `bandedWithin` + Myers), `ensure(int)` (owner
`ensureWorkload` + budgets), `Workspace.prev/cur`, and the `M3TextSignals` lanes (`presence64`, `simHash64`,
`minHash`, `bigram`, the constructor with trigram seams).

Tests: the donor `MIndexStringEditDistancePlanBoundaryTest` and `MIndexDistanceFrontierChecks` (java mode; the
direct-buffer and JNI sections are held) are ported as `M3EditDistancePlanBoundaryTest` and `M3DistanceFrontierTest`.

Build (module README): `mvn -B -ntp -f m3/ports/precompute/pom.xml test` (run here as `clean package
-Dmaven.test.failure.ignore=true`, `-Xlint:all -Werror` from the pom). Replay: `python -I apply.py --check` (exit 0,
changed=0); build the donor classes from develop `78cf2ad4c7e5` (javac -sourcepath over the donor's indexstring,
common, job, native-interop, algo, classic-tools and cop modules + re2j, lombok, commons-codec), then
`bash differential.sh <donor-classes> m3/ports/precompute/target/classes <out> 20000` and
`python -I mutants.py <repo> m3/ports/precompute/target/classes <out> <work> <donor-classes> 3000`.
