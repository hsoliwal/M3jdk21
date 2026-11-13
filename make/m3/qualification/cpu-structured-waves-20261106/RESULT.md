# cpu-tools: cpu-structured waves 6-8, 10-12 and 29-32 (census2 P2-086..090, P2-120, P2-127, P2-129, P2-134, P2-151), 2026-11-06

**Source.** com.synexia synexia-openrewrite-recipes hash-pinned crates m3-cpu-structured-wave{6,7,8,9,10,11,12,29,30,31,32} and their follow-ups wave29-native-borrow and wave29-miss-fastpath. They were sealed around the JDK naming rename, so the manifests pin pre-rename paths. crate_chain2.py v2.2 resolves all manifests as one graph: every target is a new file, the follow-up crates chain onto the wave postimages, and jdk_naming gives the paths and bytes.

**Excluded:**
- the 11 com.synexia recipe tests (synexia-openrewrite-recipes/src/test, which is not M3jdk21 code);
- wave6's coverage, verify and README, which master already has in evolved form (DRIFT);
- wave9's M3RollbackUnionFind (DRIFT: master's evolved owner) and its rollback contract, which targets the old API;
- wave9's cpu-tools M3MinCostFlow (one owner: jdk.m3.algorithm.M3MinCostFlow, #2551), with its FlowSpatial contract and verify script;
- M3PackedSpatialIndex, held because its only contract also exercises the excluded M3MinCostFlow;
- WAVE9 coverage and its README.

**New in jdk.m3.tools.cpu (22 classes):**
- M3AbstractRanges, M3DecisionRows, M3TermUnifier
- M3DifferenceConstraints, M3FunctionalDependency, M3MerkleDag
- M3ArcConsistency, M3DagReduction, M3TwoSat
- M3SortedIntSet(+Native), M3SortedIntMultiset(+Native)
- M3ByteSuffixIndex(+Native), M3RankedByteSubstrings(+Native), M3MultiRankedByteSubstrings(+Native), M3ByteRadixTrie(+Native)

**Also new:**
- native kernels m3_sorted_int_set.c, m3_sorted_int_multiset.c, m3_byte_suffix_index.c, m3_ranked_byte_substrings.c, m3_multi_ranked_substrings.c and m3_byte_radix_trie.c, plus 5 C kernel oracles;
- 23 fixtures (oracles and contracts);
- WAVE{7,8,10,11,12,29,30,31,32}_COVERAGE.tsv, verify_wave7.py and verify_wave8.py, and the refinement-proof READMEs.

No new class name collides with a first-party owner on master.

## Gate

waves_gate.sh (CPU_TOOLS_PACK_GATE v2, OVERLAY) mirrors cpu-tools/build.py on stock JDK 21.0.9 + MinGW, master 3beb791e7418.
- **Modules.** javac -Xlint:all,-module -Werror: 0 errors and 0 warnings, live and leaf.
- **build.py FIXTURES.** 15/16 live and 15/16 leaf, no regression. The failing M3CpuToolsTest is the pre-existing Windows CLI case.
- **Kernels.** gcc -std=c11 -Wall -Wextra -Werror: 6 kernels, 0 diagnostics.
- **C kernel oracles.** All 5 PASS:
  - RADIX 836,986 checks
  - MULTI_RANKED 3,184,004
  - RANKED 2,156,613
  - SORTED 1,377,261
  - BYTE_SUFFIX 119,436
- **New fixtures, run with -Xcheck:jni and the six libraries wired through their properties.** 23/23 PASS.
  - Wave contracts:
    - WAVE6_ABSTRACT_DECISION 82,248
    - WAVE6_UNIFIER 269,633
    - WAVE7_CONSTRAINT_FD 498,148
    - WAVE7_MERKLE_DAG 7,965
    - WAVE8_CONSTRAINTS 153,108
    - WAVE8_DAG 171,178
  - Java and JNI oracles for sorted set (+ skew), multiset, byte suffix (+ borrow, run as a jdk.m3.tools.cpu patch), ranked, multi-ranked and radix.
  - Disabled-mode oracles, run without the native property: BYTE_SUFFIX_DISABLED and RANKED_NATIVE_DISABLED.
