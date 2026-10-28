<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Bounded regex preparation and owner qualification

The final source-built Linux x86-64 candidate has passing bounded behavior, API, JNI and owner checks against product base `c831c4a77f0e3e10aac94178217efda882ba4be5`. The matched regression comparison records **28 clean batch/test results, three unchanged baseline failures, and no new or changed failures**. This is not an all-green JDK-suite claim. All four added naming records remain **pending**; `STRING_QUALIFICATION`, incomplete family status and incomplete repository status are preserved. The metadata recipe has not run when this document is authored.

[QUALIFICATION.json](QUALIFICATION.json) is the machine-readable public summary: exact source/test identities, stage descriptors, image identities, counts, resource medians, preserved failures and hash-bound custody references. It contains no private implementation body. Raw evidence identifiers are relative to the retained task/session custody root; they are not links to files installed in this public repository. Original raw receipts remain in the private canonical companion.

| Capability | Existing owner and result | Qualification boundary |
| --- | --- | --- |
| `m3.regex.common-class-precompute` | Two masks in M3StringFacts and M3StringRegexDp capture fourteen curated predicates during existing preparation; warm admitted decisions use the known masks | Public String.strip/String.matches acceptance: 672 public comparisons and 128 oracle evaluations per CompactStrings setting; separate facts harness: 1,246,849 checks per setting |
| `m3.regex.pattern-boolean-unicode` | Existing at-most-256-atom compiled chain records actual BMP/code-point widths and both frontiers; exact nullable-prefix reuse repairs the measured all-star regression | Per default/M3 compact/M3 wide: 551,959 finite pairs, 976,677 checks, 371,584 decisions, zero refusals; focused prefix fixture: 940 checks |
| `m3.token.regex-occurrence-reuse` | Exact owner-local row/token Boolean reuse for immutable String and defensive token index; bounded primitive metadata | 55,199 semantic checks per storage mode; each positive/negative 20,000-call block allocates 4,800,000 bytes on baseline and zero on final candidate in interpreter mode |
| `m3.collections.indexed-array-growth-atomicity` | Existing synchronized owner allocates all seven replacement planes before publishing fields | Deterministic pre-copy proof: 21 cases and 4,914 candidate checks per -Xint/-Xmixed mode; final image repeats four collection and one token product jtreg entries successfully |

“Common” names a curated set, not a measured popularity ranking. Cold and unsupported expressions continue through the existing matching paths, with Pattern fallback preserving validation and exceptions. The old dot/star DP remains part of those paths. Prepared-owner eviction can still produce UNKNOWN: the two public concurrency runs record 574/2 and 572/4 admitted/refused outcomes out of 576 queries each. That permitted refusal is not a wrong Boolean result.

The facts oracle uses a test-only java.base harness patch and expressly reports `productImage=false`; it is separate from public String.matches acceptance. The Unicode oracle uses ordinary Matcher on a separate StringBuilder, including representatives from 164 scripts, 512 flag combinations and 4,096 concurrent comparisons per mode. This finite corpus is not all programs or all strings. Unsupported graphs/flags retain fallback, Matcher retains state/capture/coordinate authority, and the optional native ASCII oracle still refuses non-ASCII. Mutable sources and vocabulary-backed token regex rows remain uncached.

## Exact source and stage identity

The union is **17 source/test/docfix paths: five behavioral owners, eleven test sources, and one separate comment-only ChangeJournal repair**. The feature stacks on [doclint companion PR #1756](https://github.com/hsoliwal/M3JDK21/pull/1756), so its stacked source/test diff has sixteen paths and excludes ChangeJournal. Both compared images include the identical comment repair. Nine test files are new; the two existing ASCII/frontier and StateMask fixtures are intentionally adapted, with the legacy StateMask failure preserved.

Apply `combined-c831` before `unicode-prefix-c831`: the latter requires Query preimage `06aadfc5d57fc57f7bbad70a6737694963852e5985e6cb2cf8d3b951980e1d3d`, and its final owner is `190d541c497c344ba5015f38edadf6d6fb359e10010afbb88729905e04b1c1d7`. Both remain in naming-map lineage. Common/token retain `combined-c831`, array growth uses `collections-c831`, and the separate comment repair uses `baseline-doclint-final`.

Each of those four stages has an actual Maven verify result with **five SDK, eight sealing and nine receiver-contract cases**. Runtime receive records apply/check changes of 11/0, 4/0 and 2/0 for combined, collection and prefix stages. A fresh doclint receiver check and zero-change apply pass on both source trees. No separate original doclint apply receipt was persisted; the fresh four-call replay is explicitly not a reconstruction of that original action.

| Product path | Role | Final SHA-256 |
| --- | --- | --- |
| `src/java.base/share/classes/java/lang/M3StringFacts.java` | runtime_owner | `0cc1b4726cb2637b184184a60c75964180d58790530101fcf46900f74f1dc72a` |
| `src/java.base/share/classes/java/lang/M3StringRegexDp.java` | runtime_owner | `faa373f77eef1d2dd3457bfe8c5d65d2dff819bb606e00f7c269e7b08a37cf25` |
| `src/java.base/share/classes/java/util/ChangeJournal.java` | documentation_companion | `2fd9384a9df26ed038bd6663e083586425228815dc7b4d43205af769d3c2544e` |
| `src/java.base/share/classes/java/util/regex/M3PatternQuery.java` | runtime_owner | `190d541c497c344ba5015f38edadf6d6fb359e10010afbb88729905e04b1c1d7` |
| `src/java.base/share/classes/jdk/internal/m3/M3IndexedArray.java` | runtime_owner | `e5b9b0cafa6edb66697783250271fa165c9ccee7ccbf661b7b23843e7e1d7a43` |
| `src/java.base/share/classes/jdk/internal/mindex/M3TokenPredicateCache.java` | runtime_owner | `1aa4e472110c2ca9432efc68cf7bea09c22ade8af670017fa4fe473bebfade88` |
| `test/jdk/java/lang/String/M3CommonRegexFactsHostTest.java` | test_source | `683b84ace57ad7fb606de36946cd5b242821601ebf540387de321bf22534f2a5` |
| `test/jdk/java/lang/String/M3CommonRegexPreparedClasses.java` | test_source | `cb3672f43cf4625e19759424df94158f95882bdb7e753d33ce2ffa3503a36738` |
| `test/jdk/java/lang/String/M3CommonRegexProductTest.java` | test_source | `9dbe0440cd9255370defe2a9b4a53e89bbbb7778d3a25260af08fcd4afac1619` |
| `test/jdk/java/util/regex/M3PatternAsciiFrontierTest.java` | test_source | `412e170cd69eb774407d978ae25531e19b38a83e7739517723941b15118a821b` |
| `test/jdk/java/util/regex/M3PatternNullablePrefixTest.java` | test_source | `780df049c5d8439cde75ef1dbdc5844ea09475b65a3027418d5a9ac75c482486` |
| `test/jdk/java/util/regex/M3PatternStateMaskTest.java` | test_source | `96a7869db39e2776fadd28b3acdc5c16861b18c64d0207b25506d57be1bc0331` |
| `test/jdk/java/util/regex/M3PatternUnicodeTest.java` | test_source | `7484c3036874f78e23c06e2032906578db987df6f9a8127f189b37af451dbc27` |
| `test/jdk/jdk/internal/m3/M3IndexedArrayGrowthAgent.java` | test_source | `0a1957ea4c6db007f13ab9d1ef669e2334eb2dc7121fcc4c0d16602e843aead3` |
| `test/jdk/jdk/internal/m3/M3IndexedArrayGrowthFailureTest.java` | test_source | `3cffeb6f2352e20cbdfa7843b29d6372181d78aff6eed305939c3497315aa750` |
| `test/jdk/jdk/internal/m3/M3IndexedArrayGrowthFault.java` | test_source | `f4f28798f4ef81eb8f49fefb972b90d9daa15b7ff5255515efe6e0ceaf8541a0` |
| `test/jdk/jdk/internal/mindex/M3TokenPredicateCacheReuseTest.java` | test_source | `f89e9acb7065dd6666e60ebcffb0b870e2a162f6528447d793a94a2dde547115` |

The baseline and final image manifests each contain 532 files. Final `lib/modules` is `791952afd090eae52bdff504147897ceedb96b7c33c612ced429e72b1631df32`; baseline is `7923ab2a13a9f6b28d085f1b258b137aaa90fe29568d636eb47e31e42e8e45fb`. The 71 compared native/launcher files are byte-identical. This establishes this image pair, not cross-platform execution. Facts/DP public/protected descriptors, seven Query/Token/Pattern/Matcher views, and the collection contracts match their baseline scopes. All 26 JNI entry signatures across thirteen generated production headers match. The DP header has one added private constant macro (`NEGATED_CLASS=32768`), so all header bytes are not identical. The unchanged direct modified-UTF8 JNI test passes 16,122 cases and 242,452 assertions per compact setting in each image; that runner invokes the test main directly, not jtreg.

## Resource observations and tradeoffs

For the selected 256-star DOTALL plan over sixteen supplementary pairs, each CPU value below is the median of three same-process samples of 128 public calls in interpreter mode. The slow candidate is retained as a separate historical observation.

| Mode | Baseline CPU, µs/call | Slow candidate CPU, µs/call | Final CPU, µs/call | Baseline / slow / final allocated bytes/call |
| --- | ---: | ---: | ---: | ---: |
| default | 35.42 | 379.33 | 24.74 | 552 / 616 / 336 |
| m3-compact | 51.43 | 374.24 | 39.68 | 552 / 616 / 336 |
| m3-wide | 46.53 | 325.94 | 37.04 | 552 / 616 / 336 |

The final run still has **higher public CPU medians in 13 of 84 selected cases**. All case medians are retained in the JSON, including adverse values. No timing threshold or general speed admission is applied. A shared machine and potentially overlapping root-owned JNI/jtreg work limit timing interpretation. Zero-byte control medians do not erase the first empty control samples' startup allocation of 12,976/15,512/15,872 bytes across the three modes.

The nullable-prefix refinement adds no retained metadata beyond the preceding full-codepoint stage. Measured plan-owner shallow size is 40 bytes. Selected plan plus unique direct-array sums per plan are 1,240/1,656/2,768/7,400 bytes at 1/63/64/256 atoms. The width arrays added by the full-codepoint stage account for 24/80/80/272 bytes over baseline. These selected graph sums exclude Pattern objects, compiled-node/predicate graphs, class state, native memory and dominator retention. They are not whole-heap sizes. The current common14 gate does not repeat the earlier image's performance or retained-memory study.

Collection growth allocates the same seven replacement arrays and retains the same final layout. Keeping all old planes until every replacement succeeds increases transient reachability: for compressed references at 681→1,021 capacity, the derived plane peak rises from 31,472 to 47,936 bytes. These are source-lifetime calculations from measured shallow sizes, not sampled heap high-water marks. CPU regressions remain in the collection observations. The final collection owner class is byte-identical to the first qualified installed image and the five final product tests pass; whole-image collection resources were not remeasured after the unrelated Query refinement. The repair preserves existing synchronization and adds no lock-free or multi-field concurrent-publication guarantee. Fault injection is deterministic before each copy, not real allocator exhaustion.

## Mastery, failures and open gates

The existing canonical multipass crate ran actual OpenRewrite/Maven qualification: **134 tests across eight suites, zero failures/errors/skips**. This includes bounded language and native bridge fixtures. A separate attempt to compile 271 pinned sources for the full canonical wrapper stopped at incomplete module/parser/Lombok closure; its wrapper test is **NOT_RUN**, not passed and not a proven upstream semantic compiler defect. No stubs or weakened gates were used.

Preserved failed attempts include missing engine-source/effective-engine seals, missing RecipeDag closure, evidence-directory refusals before the successful mastery run, the original ChangeJournal documentation-warning build failure, and jlink refusing a reused interim-image directory. The final build uses fresh interim and final packaging paths without relaxing compiler flags. The slow Unicode resource result and its expected allocation-red fixture remain hash-bound alongside the final correction.

The three existing regression failures are unchanged: `M3StringFactsCompositionTest` expects a scalar atom but receives M3StringImmediate; `M3StringRegexDpTest` expects canonical M3 storage for `cc`; `M3PatternStateMaskTest` cannot find `wordPlan`. Twenty-eight clean batch/test results cover 23 distinct passing paths; the clean and failing shared results together cover 25 distinct paths because batches overlap. Two newly added regex tests and separate common14 fixtures pass; repeated executions are not additional distinct tests.

The unchanged String-phase checker fails on both source trees with M3String owner drift: expected Git blob `35ed998540e044eed8cb7596e2e2c019ae369bd3`, actual `8a57ca4f085e4e8f1f1d9f2ec0fdb2fc5888597e`. Twelve global closure items remain open. Passing policy/closure-format checks validate an incomplete state; they do not close those items or qualify this not-yet-received metadata payload.

The separately reviewed upstream policy head `7b1b7af779683418be9a2a5bf8ed203a0e316422` adds a strict naming census. Its documented `python3 m3/tooling/name-contract/audit.py --strict` invocation exits 2 before map reading because of a literal argv comparison. Nine helper tests pass but do not cover that invocation. The checker is unchanged; correction and merge-result rerun remain pending. This c831 image pair does not qualify later unrelated upstream runtime changes.

The [naming map](../../../docs/name-mapping.json) preserves all sixty prior records and adds four pending records; all three existing naming operations remain queued. The [catalogue](../../algorithm-catalog/regex-language-mastery-20261021/README.md) retains LeetCode→HackerRank→GeeksforGeeks category lookup and exact reviewed donor licence/revision links. No challenge solution or donor implementation is copied. Existing product/component notices are preserved, and private canonical implementation bodies remain in their private companion. Whole-JDK, full-language frontend, native Unicode, M3 JIT/CDS/JFR, universal regex, general speed and whole-memory completion are outside these bounded results.
