# Qualified current receiving components

**consumer-v5 passed all four ordered gates: lint, compile, test and runtime.** Its unchanged RESULT is `c037e9fd5b622f17d161d8efe1fbb15f2a128f803e27b70292448403dc43bd80`; the exact consumer manifest is `8c3d4e6a28d4314a1de4c3a15c3616a371e04ac2eaf6357852c6ce28c1642db4`.

The frozen lineage remains Synexia source `9963cc08ff13922b92a0e3db7c30f56fddacba7d`, M3 receiving baseline `752191c9291f6467110fb8a7badfbdc4c2d41af2`, and prior publication head `5c1ee9490e76943dfdb3ca27fade0d093d017226`. This report summarizes actual receipts and executes no proof gate.

| Executed checks | Actual result |
|---|---|
| Full migration-recipes Maven clean verify | 220 tests across 58 suites |
| Full A3 Maven clean verify | 19 tests across 6 suites |
| Full backports Maven clean verify | 35 tests |
| Unfiltered host and reconciliation discoveries | 109 + 6 = 115 tests |
| Aggregate failures / errors / skips | 0 / 0 / 0 |

The original 99% line and branch thresholds remain unchanged for the POM’s `com/m3/rewrite/scope/*` and `com/m3/rewrite/atom/*` coverage selection; Maven reported that all coverage checks passed. The original A3LabTest bytes are unchanged, including all 48 fixtures × 6 schedules = 288 combinations and the fixed-point, behavior, contract, lexical-data and regex-matrix stability assertions.

The admitted repair set is **24 outputs: the exact prior 19, two JCC test-owner replacements, and three explicit ABSENT additions**. Overall this is 21 replacements and 3 additions. The qualified publication scope is 221 paths: 24 repair paths plus 197 task paths. The prior 569-path overlay retains 567 byte-identical files; only `test_jcc_handoff_context_profiles.py` and `test_jcc_handoff_packet.py` are the declared exceptions. The separately verified declared-source census remains 387 paths. These counts describe different sets and are not added into an invented final publication count.

The actual installed recipe applied 24 writes, repeated with 0 writes, rolled back 24, and reapplied 24. Stale, mixed, partial and wrong-seal controls refused with exit 2. All six packet methods passed in each of the historical before, historical after and current JCC profiles. Canonical validation exited 0; canonical completion retained its expected exit 2, so no complete-registry admission is claimed.

The original 79-record authority replay reproduced receipt `ad1aa88119d8dbf5ab5a3fc64a43bd67bc67df8aadcde9139c663b15a042f1c2`. The current 81-record authority produced `ae4758723f215fda7db7568df5441d83a773eabc7791f86505ca9dfc7c1416db` twice byte-identically through read-only replays. The original packet refused against the current authority, and the wrong current packet seal refused.

| Preserved trial | Original state | RESULT SHA-256 |
|---|---|---|
| consumer-v1 | STOPPED | `47a1efd8b72eb173d3c48124e549e7ecb28d332be09e97bc63290989ba34ba92` |
| consumer-v2 | STOPPED | `d8591a5652baf9c92902e6b1475b773fec4a1f498c1dbd6fe55dcffc002038d7` |
| consumer-v3 | STOPPED | `0d50bac3af04f4c1b498ca34c3b6c57ad76bf97aa3b51fbba1dc926d4b52d2a7` |
| consumer-v4 | STOPPED | `65ad6376f5e88ca2bd1e33e549c262a50bc58952ede7af8c17c9b0beed05ed73` |
| receiving-python-v1 | PASS_BOUNDED_PYTHON_RECEIVING_COMPONENTS | `be337d009319bab7c528f4ab6e61ada6ef5f407a33d070729e8d5cd68937a63e` |
| receiving-python-v2 | PASS_BOUNDED_PYTHON_RECEIVING_COMPONENTS | `687eed40f8cfd7c93a663dbce1bb9fbf7a80626d798fa2f502e73c1a4bbab0cc` |
| consumer-v5 | PASS_BOUNDED_CURRENT_CONSUMERS | `c037e9fd5b622f17d161d8efe1fbb15f2a128f803e27b70292448403dc43bd80` |

The two Python component PASS results remain scoped preflights; all four prior full-consumer STOPPED receipts remain unchanged. The final consumer-v5 PASS is the completed full receiving qualification.

Java/JNI also has its own successful, separately retained current Linux x86-64 proof: RESULT `1e82bec642c8ea8ece15e8caeeed283416c1b612e22c357b20178524b6ab1605`, covering 31 Java sources and the existing short-flag JNI kernel. Its publication manifest is `633b27c05776d03397eef4950ca7ae56f8f1b4f793666887aa72ab3e4c00cfc2`. Consumer-v5 binds that evidence and does not reexecute the native lane.

`runtimeAccepted=false` concerns broader JDK product/HotSpot admission. It does not negate the successful scoped host, Java and separately recorded JNI test/runtime checks. Product writes and canonical production application remain zero/false; whole-source coverage, complete dependency closure, original toolchain custody revalidation and a sealed host-system-library image remain unclaimed. No whole-JDK, cross-platform or measured-performance result is inferred.

These two publication-only report envelopes stay outside the finite source evidence cohort to avoid a circular census. The source/proof bytes and original receipts remain unchanged. Final packaging, publication counts and remote publication/readback are separate receipts.
