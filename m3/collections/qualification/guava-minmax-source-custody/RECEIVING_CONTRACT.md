<!-- SPDX-License-Identifier: Apache-2.0 -->
# Guava MinMaxPriorityQueue current-source packet receiving — 2026-11-02

Product parent: [M3JDK21 #2326](https://github.com/hsoliwal/M3jdk21/pull/2326), exact starting head `8885c024371869ab9f5b9fd2d7a9f71a87eb8855`. Source/policy owner: [Synexia canonical recipe #15520](https://github.com/hsoliwal/com.synexia/pull/15520). No master/develop write, source delete, rebase or forced merge; draft/HOLD until actual local and hosted verification.

## Source inventory completed before creation

Existing frozen Guava MinMax source packet `m3/collections/qualification/owned-guava-minmax-priority-queue-20261018/source-bindings.json` is immutable: Git blob `bcd8e0462ef7fcd323c7521979d8c54f1ea563f6`, SHA256 `da37bfacf6b8bfa91e5ded6bb74947c54c8d54a8c70f6b15cc58c1eb94208bea`. It records 131 historical core-source Git pins and 34 packet-file exact source hashes. Actual full inventory: 33 of 34 live packet files match; one differs, the existing Guava workflow.

The historical workflow has Git blob `5fd71e4300b60d37f89bc48c04defff4c7c09e2c` and SHA256 `9543ed2e6b8430e6a80888064750beda48e486b005bf125488cc95b513d3d4b9`, length 2220. The current workflow has blob `656e4d6c0ffe8bdeb7a5eec4e044761252a8d14a`, SHA256 `ed17db300c020733c0edc6b20aa0180a9314bb183a7cf0b7307989fc3e8d277b`, length 2311. An independent complete line diff is **four added lines** for GitHub Actions concurrency, with no changed commands, API, ABI, semantics or test skip. Original historical Git blob is retained, not altered to match current file.

Original `verify.py` blob `35208fa06bead288e90f81e9b0d6098450f0f9f3`, SHA256 `a8fd04cc98bf3f21e5f68958eae8f82e0b5fcedbc7c6d88f92ea778828bb980b`, bytes 4276. Original `run.py` blob `d196b84e01ff375c63e52bd7a05e591797982385` remains **untouched**.

## Exact two-target, source-owned product receiver

1. Review original verifier file in its grandfathered historical path `m3/collections/qualification/owned-guava-minmax-priority-queue-20261018/verify.py` with current Git preimage above. Receive only the Synexia exact postimage Git blob `a9cc7492afcd0f9a3da8366619771356e7cdc2c4`, SHA256 `fa0c31d15d2c936e1dc66a17d712297ad1a5621be975a890c8ef836196acb5de`, bytes 7580.
2. Create exactly one stable, undated and unversioned passive new file `m3/collections/qualification/guava-minmax-source-custody/source-bindings.json`, ABSENT-before, from exact Synexia Git template blob `d33b006b27c3b35c63c29e53a6b20cccc130515b`, SHA256 `09a547681643b5e983d8012d202b9acf20537890f66fb73add7a3a2caf1632bc`, bytes 62699.

The new source packet is exactly the original 165-row document with **only two changed rows**: the original workflow and the verifier. All other 163 rows, roles, paths, metadata and historical core refs are identical, including all original provider and module dependency pinning. Source verifier explicitly checks predecessor SHA before reading candidate, demands exact two-row geometry and expected workflow digest, then checks every original full packet path and historical 131-core Git tree.

No product Java/C/JNI implementation, provider jar, test source, public method, source parser, allocation semantic, memory reserve, kernel, comparator, iterator or `run.py` changes. Existing source workflow and frozen `source-bindings.json` remain **unchanged**. Original owner is retained; no mock/fallback.

## Verification order and explicit HOLD

1. Input SHA/blob checks and exact generated template readback (pre and post).
2. Original plain Python `verify.py` now selects only the fully checked stable successor; the original 165-source, historical Git tree, original module JAR/license checks still execute.
3. Run all existing original `run.py --job receiving`, `--job java`, `--job jni` without bypasses; retain original Maven two receiving tests, provider/owned MinMax protocol, comparator, serialization, strict Java21, native C17 JNI, output cap and 8 GiB reserve.
4. Hosted exact-head source, SDK, and runtime jobs must complete successfully; pending/queued is not success. Independent source negative/positive tests in canonical Synexia recipe #15520 also required.
5. All affected original Git objects remain reachable; do not silently repin `source-bindings.json`, rename public classes or merge without an execution trace.

Challenge donors: LeetCode 146/706, HackerRank priority queue, GeeksforGeeks min-max heap are *problem categories only*, not source-copy authority. They are **not** equivalent to the comparator/iterator/dual-bound Guava MinMaxPriorityQueue contract.

**DRAFT/HOLD:** No source-built JDK/JTReg/JCK/HotSpot/jit, macOS/Windows Java+JNI, full hsoliwal/* coverage or runtime performance improvement is claimed by this bounded source-admission step.

## Strict original javac diagnostic lane (separate additive test-only source)

On actual hosted [run 41441423867](https://github.com/hsoliwal/M3jdk21/actions/runs/41441423867), the unchanged `run.py --job receiving` completed **SUCCESS** after this successor source packet was admitted. The unchanged `run.py --job java` then reached `strict-selected-core130`, where strict Java 21 `javac` exited **1**. Its precise compiler stderr is captured by the original runner under `$RUNNER_TEMP/minmax-java/strict-selected-core130.stderr`, but the historical workflow does not print or upload the file. This is a new independently observed downstream compiler failure; do not infer its cause.

Add one **ABSENT-before test-only workflow** `.github/workflows/m3-guava-minmax-compiler-diagnostics.yml` from the exact canonical Synexia #15520 resource, Git blob `10bd9c78b78ff6746637a778ff4242695c069bab`, SHA256 `3b07fdbc4ae39579b2ca424c9731610ea6ed98210c222ca5d39e23b5bddd29ba`. It must invoke the **unchanged** original `run.py --job java`, preserve the child exit status, print the first bounded lines of original `strict-selected-core130.stderr`, and upload original compiler output to a scoped Actions artifact. Do not relax compiler flags, clone a replacement `run.py`, suppress failures or attempt a speculative Java fix. Keep the original Guava workflow untouched. This lane is an independent third recipe target and does not change the original two source successions.
