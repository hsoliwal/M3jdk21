# Array-mask current-source successor proof

This qualification binds four existing, validated postimages from `com.synexia.m3.ArrayMaskLookup20261018`: bitmap snapshot membership, its differential oracle, the strided JNI histogram, and its boundary oracle. Their bytes originated in Synexia commit `1124f5b72744b306884beedebafd8558d49d4c4c` and M3JDK21 #1408; fleet #1412 at `ac3b5c68b628f7582cb10bedf08ff040deeb694c` contains identical postimages. This change reuses those implementations and repairs their source-admission proofs. It introduces no collection runtime algorithm, callback behavior, JNI symbol, or payload owner.

`SUCCESSOR.tsv` records the exact historical and current SHA-256 plus current Git blob for every changed source. The checker seals the complete four-row manifest, admits only those current postimages, and rejects old, missing, changed, nonregular, and symlink sources. It retains independent bitmap and histogram family checks. The unchanged histogram Java owner and JUnit test remain exact source gates.

## Historical evidence stays historical

The bitmap installer, both original templates, and `synexia-m3-collections-receiver-20261018.tsv` remain unchanged. The old installer still accepts only absent or identical original output, and rejects newer product sources. `--qualify-bitmap` compiles the original installer, executes its original selftest, exercises missing/install/reapply/check against a separate historical fixture, then proves its check/apply reject the exact current fixture without writing it. Old installation preconditions are not broadened.

The histogram's original `manifest.tsv`, `native-manifest.tsv`, all four templates, and `SOURCE_PINS.tsv` retain their original hashes and `ABSENT` claims. The current checker verifies those original seals and separately verifies the exact successor Java oracle/native source. The existing histogram `verify.sh` delegates only its source-admission prelude to this checker; strict Java 21, generated JNI header, Java oracle, strict C17/UBSan build, checked JNI oracle, and explicit owning-module qualification boundary remain unchanged.

The bitmap workflow keeps its existing canonical Maven recipe verification, Java differential oracle, checked JNI parity, and owning-module Maven test. Its selected-source compiler closure adds the already-owned `M3LongAddressKernel` required by `M3LongIndexProjection`. The canonical receiver workflow retains its ancestry anchor, original installer selftests, index/state-array source gates and drift refusals; original bitmap hashes are checked against the original templates, with the successor proof admitting current bitmap sources separately.

## Qualification

The permanent twelve-test admission suite uses small isolated fixtures. It checks exact current/history separation, rejects the original postimage as current, mutates every current and historical input, retains unchanged-owner gates, rejects altered successor manifests, missing/nonregular/symlink sources and parents, checks family boundaries, and refuses escaping paths. Every refusal checks that inspected source bytes and metadata remain unchanged.

The existing Synexia class-backed `M3HashPinnedTextSnapshotRecipe` and collection refinement proof host replay the exact checker, manifests, tests, documentation and four existing gate changes from the current receiver baseline. The host provides exact replay, fixed point, mixed source states, source drift and post-scan refusal, input-order stability, retained SDK Results and receiver no-partial-write proofs. See the accompanying receipt for the exact executed commands, source hashes and retained evidence; full owning-module or JDK success is not inferred from focused qualification.

Run the installed-source check with `python3 verify_successor.py --root <M3JDK21-root>`. Add `--qualify-bitmap <new-output-directory> --java-home <Java-21-home>` to retain the historical installer proof outside that repository. Run `python3 test_verify_successor.py --root <M3JDK21-root> --output <new-output-directory>` for the admission contracts. Existing source/Java/JNI workflows retain their own runtime gates.

No RAM-speed claim follows from hash admission or these correctness tests. Existing bitmap membership uses its declared ordering for binary search, and the histogram batches representable strided selections into its existing bounded scratch buffer; runtime performance qualification remains attached to their original donor evaluations and actual receiving environment.
