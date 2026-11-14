<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->

# Qualification and remaining gates

## Review outcome

The named Maven/OpenRewrite recipe emits six Java and nine text/native targets from the exact #1029 source baseline, `3bbb4712fb20adba1405741db8457164b9f436f5`. Actual SDK replay and focused Java/JNI contract checks pass. A complete Linux x86_64 fastdebug JDK image built successfully from the same three production Java/C postimages. Product-wide String acceptance, repeatable performance acceptance, and CUDA device qualification remain separate gates.

The transformation retains the existing M3 rope/cursor, immutable Plan, weak Effect ownership, and JDK Pattern fallback. It adds mandatory literal prefix/suffix rejection and bounded preparation of composite full matches. Prefix preparation and two suffix acceptance preimages belong to the existing effect owner. They preserve UTF-16 fragment seams and avoid repeating fixed context for every tuple row. No public String or Pattern method changes its signature or regex contract.

## Executed evidence

| Gate | Result and scope | Evidence |
| --- | --- | --- |
| Actual OpenRewrite SDK | Five tests; zero failures, errors or skips. Named YAML activation, exact replay, reversed source order, mixed pre/post states, fixed point, and drift/refusal cases execute. | `evidence/qualified/SDK_TESTS.xml`, `SDK_INPUTS.tsv`, `SDK_RESULTS.tsv` |
| Focused contract profile | All 16 commands accepted against actual SDK Results and 58 sealed parent files plus 19 required absences. The full-String checker has the explicitly retained identical baseline failure described below. | `evidence/qualified/runtime/SDK_RUNTIME_RECEIPT.json`, `maven.log` |
| Mandatory affixes | 47,904 differential checks with the entire extracted matcher class and the exact cursor implementation. This fixture simulates the M3 owner; it is not a matched-product runtime result. | `evidence/qualified/runtime/affix-complete-source-before-after.stdout.log` |
| Retained regex preparation | 24,604 amalgamation checks, 637,128 row-precompute checks, 15,601 rolling-cursor checks, native UTF-16 oracle with 39,845 checks per run, and nine completeness-ledger refusal tests. Twelve retained regex jtreg sources compile. | Corresponding logs under `evidence/qualified/runtime/` |
| Prepared Java backend | 231,580 checks, explicit Java-only execution and zero native rows. | `evidence/qualified/runtime/prepared-effects-java-jni-contract.stdout.log` |
| Prepared JNI backend | 231,612 checks and 46,278 strict JNI rows per run, including portable C/interpreter and UBSan plus `-Xcheck:jni` configurations. Callback/cancellation, close races, zero rows, caps, reused effects, fixed context and split surrogate seams are exercised. | Same prepared-effects log and `contract/batch/` |
| Native allocation failures | Five allocation sites fail in turn; zero live native allocations remain. | Same prepared-effects log |
| Actual JDK image | Complete `make images JOBS=6` success; generated production Java/C source identities are equal to the final SDK postimages. The image starts successfully. | `evidence/qualified/product-build/BUILD_RECEIPT.json`, `make-images.log`, `image-version.log` |
| Actual image: whole DP test | Final SDK-emitted test passes normal and interpreted execution with CompactStrings enabled and disabled. Three runs report 56,764 checks and one interpreted UTF-16 run reports 56,763; all use `-Xcheck:jni`. | `evidence/qualified/actual-image/RUNTIME_SUMMARY.json`, `critical-final/`, `critical-final-interpreted/` |
| Actual image: prepared batches | Both declared runs pass with 231,612 checks and 46,278 strict JNI rows each, using the built image's `libjava`. No replacement batch provider or product-class patch is installed. | `evidence/qualified/actual-image/critical-final/` |
| Receiving | All targets admitted before the first write, actual SDK bytes applied, then zero changes on reapplication. | `evidence/qualified/receive-*.log` |

Actual-image test results are retained separately from the extracted-source fixture results. The final whole-test runs supersede the diagnostic direct-method probes. The image binaries, seven jtreg harness jars, three production Java/C sources and executed SDK test sources were checked again after testing. The critical selection took 50.369 seconds and the additional whole-DP interpreter selection 45.088 seconds; these are qualification durations, not throughput measurements. The required jtreg version and original test-suite configuration were preserved.

## Oracle refinements

The previous proof used many adjacent `a*` terms and a missing mandatory `Q`. Running Java Pattern on that negative witness can enumerate a combinatorial number of equivalent partitions. The recipe now retains the original text, original specialized expression and expected rejection, proves that `Q` is absent, uses the independent memoized oracle where available, and checks the equivalent collapsed `a*` expression with Pattern. Other Pattern differential cases remain. Positive long-plan cases remain separately exercised.

The previous source-compile gate promised twelve retained tests but assembled only eleven after exclusions. Its inventory now also seals and compiles the existing hierarchical regex test. The obligation remains twelve. Timeout handling owns and terminates the descendant process group, including a child that ignores SIGTERM; the cleanup probe verified that behavior.

Actual-image qualification exposed two more preconditions in the already-modified DP fixture. A singleton `String.join` can retain flat backing. The test now obtains an explicit owner from the existing `M3String.canonicalize` factory when needed, verifies every original UTF-16 unit, and invokes the actual internal DP. Original singleton and empty inputs, public String behavior, and Pattern comparisons remain. The split-surrogate missing-Q witness now explicitly prepares the optional facts required to prove rejection before the exact matcher returns UNKNOWN at the surrogate. Its earlier possible-match UNKNOWN assertion and final rejection assertion both remain. The recipe-owned whole test passed before emission; the actual SDK-emitted whole test then passed in the modes above. Production core bytes did not change during these fixture corrections.

The first portable warm probe rejected the existing fixture's grouped package-private owner classes under `-Xlint:all`. Its compiler arguments now match the pinned fixture runner's `-Xlint:all,-auxiliaryclass -Werror`; production source warnings are not disabled by this fixture-specific selection. Failed attempts and corrected evidence are retained under `evidence/refinements/`.

## Performance interpretation

The deterministic work reduction is strongest when an uncached source has a wrong mandatory boundary. In the 2,057-unit fixture, the relevant cold prefix/suffix near misses reduced code-unit reads from 2,059 to three or six. An exact warm root Effect can return without reading either boundary. These are fixture work counts, not product latency measurements.

The retained local warm probe is mixed: warm short positives measured 26.96 to 17.80 ns, warm long positives 49.81 to 90.67 ns, repeated prefix rejection 30.89 to 34.31 ns, and cache-churn prefix rejection 2,700.62 to 48.33 ns in that one run. The log identifies the simulated owner. Earlier samples also varied. This evidence does not support a universal speedup or automatic product promotion. A product workload must establish a suitable distribution of cold rejection, reuse, rope geometry and pattern sizes, with warmed/forked CPU measurements, retained heap and native process memory recorded together.

The affix Plan addition is two primitive lengths. Across the existing bounded slots, the primitive-array/field arithmetic bound changes from 600,064 to 602,624 bytes, an increase of 2,560 bytes. This is not a measured retained-heap result. Prepared batches separately bound rows/effects, tuple references, primitive bytes and state work; their per-batch residency must be measured for an actual workload.

## Explicit holds

The unmodified full-String invariant checker fails identically on the sealed baseline and candidate:

```text
M3_STRING_INVARIANT_FAIL: M3 adaptive BMH/KMP convergence missing: skip256[pattern.charAt(index) & 255] = length - 1 - index;
```

The focused contract receipt therefore reports `PASS_WITH_BASELINE_HOLD`; it does not declare the complete String family accepted. Any different failure is refused. No completeness flag or ordered String-to-Arrays-to-Collections promotion is advanced.

The additional bounded, unchanged runtime selection reports seven passing tests and four failing fixtures. Its failing suite remains FAIL in the retained receipt:

| Unchanged fixture | Diagnosis and unreached obligation |
| --- | --- |
| `M3StringRegexEffectsTest` | Reflects a historical `caches` field absent in both reviewed DP source versions. |
| `M3StringHierarchicalRegexTest` | Reflects a historical `RELATION_ACCESS` field absent in both versions. |
| `M3StringRegexRowPrecomputeTest` | Its first empty-string case requires an attached owner and fails before the regex comparison. |
| `M3StringOwnerRangeLifetimeTest` | Its nested expected literal includes a low surrogate outside the substring bounds. Stock JDK and the actual M3 image produce the same nine UTF-16 units. Later lifetime assertions were not reached. |

The seven passing retained tests cover canonical DAGs, facts composition, leaf cursors, native regex bridging, powers, reuse and UTF-16 matching. Full diagnosis, original failing JTRs and exact input identities remain under `evidence/qualified/actual-image/`. These unchanged fixture failures are not recast as passing tests, and the complete regex suite was not run.

The Linux image build used the repository's full product build with `--disable-warnings-as-errors` and `--disable-javac-server`; the latter avoids a local compiler-server dependency. The focused generated native implementation separately passes strict C17 warnings-as-errors. Neither configuration claims qualification on other operating systems or architectures.

CUDA source convergence is private and optional. No CUDA compiler, NVIDIA device, kernel execution, transfer/residency measurement or GPU performance result was available. Strict JNI and CUDA requests are distinguishable; a fallback result cannot masquerade as CUDA execution. Product String matching does not automatically load that provider.

## Reproduction and interpretation

Use the commands in `README.md` to rerun the actual SDK and contract profile. Retained source hashes bind each result to its inputs. Evidence paths from this machine describe the recorded execution; they are not runtime configuration. The public mirror contains the unchanged SDK owners so its Maven invocation does not require access to the private convergence workspace.

The receiver validates the full target set before any write and replaces files serially and atomically. It assumes exclusive application access and is not a cross-file transaction if the filesystem fails midway. These are receiving mechanics, independent of the behavioral and performance gates above.
