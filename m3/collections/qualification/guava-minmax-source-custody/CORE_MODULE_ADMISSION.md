<!-- SPDX-License-Identifier: Apache-2.0 -->
# Java 21 original arrays-module dependency for MinMax source qualification

Rooted in exact [M3JDK21 #2329](https://github.com/hsoliwal/M3jdk21/pull/2329) head `150bf4cd0a504e9260fc64e374b85b6aeff0fc3c` (source-locked review branch). Canonical proposed source owner is Synexia `com.synexia.m3.GuavaMinMaxJpmsCoreAdmission20261102`, stacked on Synexia #15520. This file establishes contract before modifying the original product verification source.

## Observed original compiler failure

Actual CI Guava runs 41442598712 (Java/JNI) and diagnostic run 41442598681 (artifact 13139648642) fail at original selected core130 `javac`: `module-info.java:17: error: module not found: com.m3.arrays`. The original arrays Java module is present in source but not compiled or placed on `--module-path` by `run.py`. Keep original `module-info.java`, strict Java21 lint, full 130-source census, original Guava Maven/JNI test protocols, output caps and source owner.

## Source modifications and exact source custody

- `m3/collections/qualification/owned-guava-minmax-priority-queue-20261018/run.py` is the **existing** original qualification runtime. Add a *bounded explicit* first module build from existing `m3/arrays/src/main/java/**/*.java` into scratch, use original `common` strict javac flags. Supply compiled module through `--module-path` to original `core130` javac and classpath to existing queue fixtures, without skipping any source or test.
- `m3/collections/qualification/owned-guava-minmax-priority-queue-20261018/verify.py` remains original entrypoint and retains full immutable V1/165-row and V2 predecessor source proof. Source successor now admits exactly three specific original packet rows (workflow, verifier, runner), all other 162 unchanged. Do not repin original V1 packet.
- `m3/collections/qualification/guava-minmax-source-custody/source-bindings.json` advances exactly these three rows with full SHA256 and byte counts; original V1 frozen packet remains unchanged; V2 prior image stays reachable by original Git commit and canonical full-byte template.

No public runtime Java, C, C++/JNI or Maven POM is rewritten, and no test is waived. `M3ArrayNative` required-native behavior and previous Apache JNI receipts remain separate.

## Source admission and behavior verification

Receive only after exact Synexia canonical source templates and SHA manifest exist; check original Git blob per modified target and reject unknown targets. Run Python syntax and original packet-refusal corpus, then original Java21 arrays module compilation, original selected core130 (JPMS module-path), original module-plus-owner fixtures, `java` and `jni` provider/owned Guava MinMax protocol tests, and original Maven receiving. Native C17 JNI and memory/disk output caps stay original. No source build or JNI success is inferred from authored commands, hosted queued steps or previous green tests on a different source SHA.

Donor assessment: LeetCode LRU/HashMap, HackerRank heap and GeeksforGeeks min-max heap problem families do not substitute for the original Guava MinMaxPriorityQueue comparator, iterator and dual-bound behavior. No donor website solution copied. **DRAFT/HOLD**; no default `master` mutation, silent merging or rebase.
