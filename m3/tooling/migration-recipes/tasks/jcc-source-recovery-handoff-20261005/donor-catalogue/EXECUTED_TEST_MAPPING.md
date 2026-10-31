# Executed donor-test mapping

This receipt summarizes existing completed runs. The catalogue authoring step did not execute tests. Both runs used the JCC verification module at `synexia-openrewrite-recipes/recipes/atom-pattern-mastery-20261005/jcc/`, which pins Java release 21, JUnit 5.10.2, OpenRewrite 8.17.1 and Surefire 3.2.5.

## Completed runs

| Evidence log | Native mode | Result | Scope |
| --- | --- | --- | --- |
| `35-final-main-tests.log` | `off` | 68 tests; 0 failures, errors or skips; build success | 44 `JccUnicodeTest`, 17 `JccMasteryTest`, 3 donor literal and 4 donor regex tests. |
| `37-required-jni-donors.log` | `required` | 7 tests; 0 failures, errors or skips; build success | The same 3 donor literal and 4 donor regex tests in a separate required-native run. |

Log SHA-256 identities:

- `35-final-main-tests.log`: `53fa678fd69a16d2350d63bddb0a2a83fe2163b551e61dd50ddb70d3d3fb58d3`
- `37-required-jni-donors.log`: `6b2360fb3c16138ce34bad981e558ecda477c1c342b639c76c3783741a13f186`

These logs do not establish performance, an instrumented JNI entry count or a replay result. Replay evidence belongs to the implementation PR's separate receipt. Full operational log paths are retained in the JSON authoring input record.

## Exact positive counts per mode

| Family | Logical pairs | Character representations | Character operation checks | UTF-8 slice layouts | UTF-8 text-search checks | Native-search API checks | Regex representation/operation checks |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| ASCII literal | 217 | 1,337 | 6,685 | 868 | 4,340 | 868 | — |
| Scalar-Unicode literal | 30 | 180 | 900 | 120 | 600 | 120 | — |
| **Literal total** | **247** | **1,517** | **7,585** | **988** | **4,940** | **988** | — |
| Restricted regex | 1,333 | — | — | — | — | — | 13,330 |
| Structured regex | 144 | — | — | — | — | — | 1,440 |
| **Regex total** | **1,477** | — | — | — | — | — | **14,770** |

The literal owner checks first index, contains, prefix, suffix and whole literal equality: five operations per character representation or UTF-8 slice. Every literal pair checks the original `String`, an `MIndexString`, every UTF-16 composite seam, and four before/after byte-padding combinations. The padding may contain the needle outside the logical slice. An independent direct-alignment oracle computes character results; a separate byte-alignment oracle computes byte results.

Each regex pair compares `matches` and `find` with JDK `Pattern` across three character representations and two UTF-8 layouts: **10 operation checks per pair**. The positive packet requires successful compilation of all 55 declared patterns by both JDK and the automaton. It records zero parser refusals. A refused positive case fails its JUnit test.

**The 988 native-search API checks are not 988 JNI entries.** `MIndexNativeOps.indexOfBytes` can return before dispatch for empty or longer-than-input needles; native `off` uses its Java path. The tests assert the requested mode and native availability, and never change the mode property. The `required` result establishes the test result with required native availability in this environment. Regex automaton checks remain Java checks even in that run.

## Seven methods and their finite domains

Test sources reside under the JCC module's `src/test/java/com/synexia/mindex/` directory. Each method below passed in both logged modes.

| ID / method | Declared domain and independent observation | Catalogue references |
| --- | --- | --- |
| LIT-ASCII / `MIndexDonorLiteralRegressionTest.everyAsciiPairEverySeamAndEveryPaddedSliceMatchesTheLiteralOracle` | All `x/y` texts of length 0–4 (31) × needles of length 0–2 (7) = 217 pairs. Existing combinator strength 2 covers the complete two-dimension product; assertions bind uniqueness, ordinals and cardinality. Direct alignment supplies expected results. | LC-0028, HR-SPECIFIC-STRING, JAVAC-COMBO-01, JQWIK-FINITE-01 |
| LIT-UNICODE / `MIndexDonorLiteralRegressionTest.scalarUnicodeKeepsUtf16AndUtf8PositionsDistinctAcrossEverySeam` | Five explicit scalar-Unicode texts × six needles = 30 pairs. Includes empty, accented, CJK and supplementary characters. Independent character and byte oracles keep their coordinates separate. | LC-0028, HR-SPECIFIC-STRING; explicit Synexia Unicode extension |
| LIT-BOUNDS / `MIndexDonorLiteralRegressionTest.invalidSlicesAreRejectedBeforeAnOptionalNativeDispatch` | Six expected refusals: two null-input and four invalid-slice checks. These are boundary checks, not positive pairs. | Existing MIndex contracts; no challenge-specific contract adopted |
| RX-RESTRICTED / `MIndexDonorRegexRegressionTest.everyRestrictedTokenCombinationMatchesJdkAcrossRepresentations` | All 0–2-token combinations from `x`, `y`, `.`, `x*`, `y*`, `.*`: 43 distinct patterns × all 31 `x/y` inputs of length 0–4 = 1,333 pairs. JDK `Pattern` is the oracle. | LC-0010, RE2J-SUBSET-01, JQWIK-FINITE-01 |
| RX-STRUCTURED / `MIndexDonorRegexRegressionTest.declaredStructuredPatternsTreatCodeLookingInputAsData` | Twelve explicit supported patterns × twelve explicit inputs = 144 pairs. Covers classes, alternatives, bounded repetition, quoting, absolute anchors, a reluctant quantifier, escapes, whitespace and code-looking data within the declared corpus. | HR-SPECIFIC-STRING, RE2J-SUBSET-01 |
| RX-REFUSAL / `MIndexDonorRegexRegressionTest.validUnsupportedAndOverBudgetPatternsAreExplicitlyRefused` | Eleven JDK-accepted expressions must be refused by the automaton: eight unsupported-form cases plus repetition, nesting and source-length bounds. Matching cases = 0. | HR-PATTERN-SYNTAX, RE2J-SUBSET-01 |
| RX-MALFORMED / `MIndexDonorRegexRegressionTest.malformedPatternsFailSyntaxChecksInsteadOfCountingAsNoMatch` | Six malformed expressions must throw JDK `PatternSyntaxException` and be refused by the automaton. Matching cases = 0. | HR-PATTERN-SYNTAX |

JUnit execution counts and internal loop observations are different units. The seven JUnit methods contain the finite products; the pair and operation counts are not additional discovered JUnit tests. All source strings and fixtures were independently authored; challenge pages supplied category metadata only.

## Source identities

| JCC file | SHA-256 |
| --- | --- |
| `src/test/java/com/synexia/mindex/MIndexDonorLiteralRegressionTest.java` | `ddce76a22c1c4e08725c36595d5f5c232c59c7babb54b96a3d7113e442b59ddc` |
| `src/test/java/com/synexia/mindex/MIndexDonorRegexRegressionTest.java` | `3c89f1cf945db9a05626cdf96016517f7903687cdfa2348a2d0bad6013727925` |
| `pom.xml` | `feb6d0d9b6058016caaed29c00ca9f6dac23d60ec13216c31822bf294ba1cc28` |

These hashes identify the inspected source snapshot. The test logs establish the recorded run outcomes; a hash by itself does not prove behavior. The machine-readable record retains both kinds of evidence.

## Remaining coverage gaps

- The corpus is exhaustive only within its stated finite products. It does not cover every Java regex, string or software atom. Empty and Unicode literal cases are Synexia extensions beyond the cited LeetCode constraints.
- Regex capture contents, match positions, arbitrary flags, alternative line terminators, `$` final-terminator rules, vertical-tab whitespace, quantified quoted runs and class intersection are outside the positive packet. Comparison against the [JDK Pattern contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html) remains necessary before admitting those families.
- Scalar-Unicode literal checks do not establish malformed UTF-8, isolated-surrogate, modified-UTF-8 or all Unicode behavior. Native entry instrumentation, non-byte-array dispatch forms, broader native platforms and throughput are not measured here.
- Wildcard matching, delimiter validation and the HackerRank address validator remain catalogue entries without implementations or executed corpora in this packet. GFG statement-retrieval gaps remain unresolved.
- The seven donor tests treat code-looking strings as data. They do not by themselves establish that a Java recipe preserves attribution, source outside an edit, API shape or execution behavior. Those properties belong to the existing compiler/recipe verification and its separate evidence.
- The earlier eight-suite **1,871-case proposal is unexecuted**. Its count must not be combined with these results. Repeating the donor corpus in a second mode also does not create additional distinct input cases.
