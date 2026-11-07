# Focused actual M3JDK image validation

The final SDK-emitted `M3StringRegexDpTest` and `M3StringRegexPreparedBatchTest` pass on the complete fastdebug M3JDK image. This packet preserves those results and the bounded retained suite, including four unchanged legacy fixture failures. It does not advance the full String stage.

## Final executed scope

| Test and mode | Result | Reported checks |
| --- | --- | --- |
| Whole `M3StringRegexDpTest`, normal VM, CompactStrings enabled | PASS | 56,764 |
| Whole `M3StringRegexDpTest`, normal VM, CompactStrings disabled | PASS | 56,764 |
| Whole `M3StringRegexDpTest`, interpreter, CompactStrings enabled | PASS | 56,764 |
| Whole `M3StringRegexDpTest`, interpreter, CompactStrings disabled | PASS | 56,763 |
| `M3StringRegexPreparedBatchTest`, normal VM | PASS | 231,612; 46,278 strict JNI rows |
| `M3StringRegexPreparedBatchTest`, interpreter | PASS | 231,612; 46,278 strict JNI rows |

Every final critical run used `-Xcheck:jni`. The DP test's declared runs enable M3 String storage in both encodings. The prepared batch test's declared runs require the JNI backend; the actual image's built `libjava` supplied it. No replacement product classes or separately compiled prepared-batch JNI provider were installed for these runs. The native libraries in `native/NATIVE_BUILD.json` are test-only prerequisites for the separate retained suite.

`critical-final/` completed two jtreg tests in 50.369 seconds. `critical-final-interpreted/` completed the whole DP jtreg test in 45.088 seconds. These elapsed times describe qualification duration and are not performance benchmarks. The JTRs preserve the separate declared invocations and their exact counters. Counters are reported per invocation; repeated cases are not represented as additional distinct coverage.

The final test source hashes are:

| Source | SHA-256 |
| --- | --- |
| `M3StringRegexDpTest.java` | `b9bbf14010da526e41d0e78945e728d5e9aa1cab8ccc732a815a9a14a253ee2a` |
| `M3StringRegexPreparedBatchTest.java` | `465e1002df44eb9453b3d6c45670af4b9a9de90e7fc746d9b82a2c85423be0ef` |

The fixture admitted all 15 actual SDK Results, required the exact five passing SDK tests on Java 21, verified the final manifest binding, checked that the three built product core files equal their SDK outputs, and overlaid only the two emitted test files onto exact reviewed Git prerequisites. `identity/SDK_BINDING.json` records the build source, complete image, seven harness jars, all hashes and manifest identities. The image, harness, product core and executed test sources were rechecked unchanged after the final runs. Its historical `runtime_tests: NOT_RUN` field describes the fixture admission moment; the subsequent RUN receipts and `RUNTIME_SUMMARY.json` record the completed executions.

The built image is `/tmp/m3jdk21-product-build/images/jdk`. The harness is `/tmp/m3-toolchain/jtreg-7.3.1+1/jtreg` and the harness launcher is `/tmp/m3-toolchain/jdk-21.0.12.1+1/bin/java`. The repository's `requiredVersion=7.2+1`, `TEST.ROOT` and `TEST.groups` remain unchanged. Exact prerequisite receipts include 415 original files, 134 additional group-referenced files and 152 Git-confirmed directory metadata entries. The unselected contents of those directory entries are not claimed complete.

## Retained suite and explicit holds

The bounded retained selection completed in 69.989 seconds: seven PASS and four FAIL. The full JTR for each is retained under `retained-focused/jtr/`.

The passing tests are `M3StringCanonicalDagTest`, `M3StringFactsCompositionTest`, `M3StringLeafCursorCanonicalTest`, `M3StringRegexNativeBridgeTest`, `M3StringRegexPowerTest`, `M3StringRegexReuseTest` and `M3StringRegexUtf16Test`. Their declared modes are preserved in the JTRs, including M3-disabled modes where the original tests declare them.

| Unchanged failing test | Exact observed failure | Bounded diagnosis |
| --- | --- | --- |
| `M3StringRegexEffectsTest` | `NoSuchFieldException: caches` | Both the reviewed DP preimage and current implementation lack this historical field. |
| `M3StringHierarchicalRegexTest` | `NoSuchFieldException: RELATION_ACCESS` | Both the reviewed DP preimage and current implementation lack this historical field. |
| `M3StringRegexRowPrecomputeTest` | `M3 regex precompute oracle mismatch #1` | The first empty String has no attached M3 owner; its fixture assertion fails before a regex comparison. |
| `M3StringOwnerRangeLifetimeTest` | `nested content` | The nested substring bounds exclude the low surrogate, producing nine UTF-16 units ending in `D83D`. The inherited expected literal has ten units and retains the low surrogate. Stock JDK and actual M3 image independently give the same nine-unit result. |

These four existing tests were not edited, silently skipped or converted to successes. The owner-range test did not reach its later lifetime checks. `diagnosis/legacy-fixture-diagnosis/` preserves the probe source identity, exact commands, stock and actual-image results, preimage field evidence and unchanged test hashes.

The separate final SDK source/JNI contract retained in `source-contract/` is `PASS_WITH_BASELINE_HOLD`: all 16 required commands completed with their declared expectations. The original full String invariant checker reports the same baseline and candidate diagnostic: `M3 adaptive BMH/KMP convergence missing: skip256[pattern.charAt(index) & 255] = length - 1 - index;`. That failure remains explicit and `full_string_stage_accepted` remains false.

## Recipe-input refinements and failed attempts

`refinements/` keeps the original timeout and fixture failure receipts, their exact former source inputs, and the narrow corrections. The historical flat amalgamation proof was preserved. Its active successor retains the pathological absent-Q input and uses an independently justified absent-literal/equivalent-pattern oracle, avoiding the exponential backtracking oracle cost. Ordinary Pattern comparisons remain.

The final jtreg fixture preserves singleton and empty inputs. When a String has no attached M3 owner, it invokes the existing canonicalization API and verifies every UTF-16 unit before calling direct DP; the original public String/Pattern obligations remain. The existing surrogate missing-Q exact-decision assertion now explicitly prepares optional facts after its original possible-match UNKNOWN assertion. No product core changed during these refinements.

`refinements/jtreg-whole-template-qualification-20261017/` is accurately labeled pre-emission qualification of the proposed recipe input: the entire source passed before the final SDK replay. `critical-final/` and `critical-final-interpreted/` are the later execution of SDK-emitted bytes. The earlier `diagnosis/affix-methods/` logs show the new affix method passing before a separate inherited singleton fixture failed; their aggregate exit code remains 1.

The first sparse jtreg setup failure is retained under `failures/initial-sparse-harness/`. Missing prerequisites were then materialized from exact Git objects; no harness version or source admission rule was weakened. The earlier source proof timeout and process-group cleanup evidence are retained separately.

## Reproduction and packet layout

`RUN.json` in each suite records the complete command, working directory, image and test binding, resource bounds and result. `commands/` retains the exact fixture admission, group staging and bounded execution helpers. The final fixture was `regex-work/runtime/fixture-final`; the original Git archive and group archive are identified by digest in `identity/`. The fixture can be recreated from the reviewed repository revision and the qualified SDK crate using these helpers; the binary tools and archives are prerequisites rather than contents of this packet.

For an already admitted final fixture, the critical selection used:

```bash
python3 regex-work/runtime/execute_jtreg.py \
  --fixture regex-work/runtime/fixture-final \
  --jdk /tmp/m3jdk21-product-build/images/jdk \
  --boot /tmp/m3-toolchain/jdk-21.0.12.1+1 \
  --harness /tmp/m3-toolchain/jtreg-7.3.1+1/jtreg \
  --output regex-work/runtime/actual-image \
  --label critical-final --java-option=-Xcheck:jni \
  java/lang/String/M3StringRegexDpTest.java \
  java/lang/String/M3StringRegexPreparedBatchTest.java
```

The separate whole-DP interpreter invocation changes the label to `critical-final-interpreted`, adds `--java-option=-Xint`, and selects only `M3StringRegexDpTest.java`. The helper requires a new output label for a replay and preserves the exact original declared runs. Test JVMs use a 512 MB default maximum heap, two active processors and jtreg concurrency one.

`COPY_INVENTORY.tsv` identifies every retained file, its SHA-256, byte size and original absolute source. It excludes itself to avoid a circular digest. The packet contains only UTF-8 text: no `.class`, native library, JAR, JDK image, complete fixture tree or jtreg work directory. JTR text is selected explicitly from the work directories and retained with a `.jtr.log` suffix; the bytes are unchanged.

The complete regex suite and its 5 GB heap negative-array-size case were not run. The available container limit is 8 GB and this task deliberately froze the meaningful focused selection. No CUDA device run or product throughput experiment is claimed. These limits do not change the observed critical passes or the retained failures above.
