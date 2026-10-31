<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3IndexPathRank: exact Java 21 baseline and candidate proof

This receipt qualifies the exact source bytes at qualified product source commit `4fd2abe76a3a63f5c363f69b4a5f979540a95bdb`. The candidate `Window.toArray()` walks selected bitmap words and emits rows in source order. Its incumbent owner is Git blob `6bc439b69f739608141acf6ee3800e5b5b0163c3`; the candidate owner is `4a60fd1fab763c03dd14f1e4e5abe4ff33a712fa`.

Both owners compiled with the same two existing proof sources under Java 21, `--release 21 -Xlint:all -Werror -proc:none -implicit:none`, an empty source path, and an isolated classes-only classpath. No external dependencies were used. The executed source and tool hashes are recorded in [PLAN.json](PLAN.json) and the prelaunch [INVENTORY.json](INVENTORY.json). Their preparation status is retained unchanged; [RESULT.json](RESULT.json) records the subsequent execution.

Each variant passed 36,476 rank checks over 900 randomized paths and 6,300 independent window cases covering signed rows, duplicates, strides, and masks. These are counts per variant; rank checks are not a count of every assertion. Corresponding baseline/candidate proof output was byte-identical. `javap -protected` output for `M3IndexPathRank` and its nested `Window` was also byte-identical: 859 bytes, SHA256 `43078be89f7f5c61998d7a58ec9c811f0b5609b18cab0bbde1f543bf3cb23a57`.

All eight native jobs exited zero, reached zero active owned processes, and closed their native handles. The acquired global JVM lock was released after terminal evidence was published. Storage protection remained active. The source files were copied unchanged into isolated scratch; this qualification did not transform product sources.

[The catalogue entry](../../../algorithm-catalog/index-path-rank-wordwalk-20261020.invocation.json) is a static selection record for the eight existing JDK command arrays in `PLAN.json`. It accepts no recipe or file-selection parameters and does not register a dispatcher, MCP tool, or executor. A future run needs a fresh reviewed output root, current source/tool checks, and the stated containment and storage gates. Historical commands must not be replayed into this completed directory.

Canonical Synexia recipe `com.synexia.m3.M3IndexPathRankWordWalk20261020`, PR12545 head `8ed3b94b207612bc71fbff6fd02de985bb930107`, remains separate: its actual OpenRewrite/Maven SDK application and recipe tests were not run by this proof. JNI parity, benchmarks, whole-image/JDK integration, downstream compatibility, and merge/promotion admission remain pending. The global active family is unchanged.

Publish this data under `m3/collections/evidence/index-path-rank-wordwalk-20261020/windows-host-20261020/`. Keep Python launchers, classes, caches, and private session/harness material outside the public bundle. Apache-2.0 notices remain in the unchanged source donors.

The frozen INVENTORY.json describes the original K execution inputs, not this public metadata folder. Its README.md row maps to the exact original [PREPARATION_README.md](PREPARATION_README.md), retained byte for byte. Its four Java rows identify the recorded staged inputs and public Git blobs; those source bodies are not duplicated in this proof folder. PLAN.json is published with its original bytes.
