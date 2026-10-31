<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Qualification failures and corrections

These are failures observed while reconstructing and qualifying the October 19
postimage. Only successful reruns against the final sealed sources count as
acceptance. No October 18 lost workspace output is reused as acceptance evidence.

| Observation | Correction and verification |
| --- | --- |
| A broad namespace translation changed the token-range test's compatibility method name. | Restored the historical method contract; all ten token-range hash cases pass in the final backend suite. |
| The recipe CLI first failed to load `org.slf4j.LoggerFactory` because its logging provider was only in test scope. | Made the pinned provider a recipe runtime dependency. The failed invocation wrote no receiver files; the clean preimage was checked before the fresh CLI apply, check and zero-change replay. |
| An offline Maven run lacked a pinned Surefire provider; an initial recipe run lacked cached dependencies. | Restored the pinned dependencies through approved network access with TLS verification intact, then ran the final qualifications offline. These were environment failures. |
| Native-gated Java suites could report zero tests when inherited assumptions disabled execution. | The module requires native loading with `m3fs.native=required` and `m3.fs.test=true`, uses `-Xcheck:jni`, and the receipt asserts that every one of the 31 suites contains tests. The final total is 97 tests, with no skips, failures or errors. |
| The sealed catalogue compatibility fixture was absent from the reconstructed tree. | Restored the exact 12,460-byte donor fixture and checked its SHA-256 before the compatibility test repackages it. Current donor additions are checked explicitly. |
| A retained durability test required the existing create-only publication helper. | Restored the helper and routed publication through it; create-only linking, file forcing, parent directory forcing, and winner-byte validation remain exercised. |
| A real concurrent publication run raced between a directory existence check and creation. | Directory creation now attempts the atomic operation, catches `FileAlreadyExistsException`, and then validates a real non-symlink directory. An eight-publisher, 96-shard regression reproduced the preimage failure and passed the corrected implementation. |
| The first sanitizer invocation could not inspect the sandbox process state for leak checking. | Re-ran the same native tests under approved process access; all 12 ASan/UBSan/LSan tests passed. No sanitizer was disabled for the accepted leak-checking run. |
| EFS contract probes exposed URI decoding and filesystem projection differences. | Corrected strict percent-escaped UTF-8 validation, dangling-link metadata, directory merge behavior, absence below a non-directory, and atomic move-without-overwrite. Provider and real Equinox workspace checks pass in normal and linked-module modes. |
| Heap baseline measurements varied between bounded probe runs. | The final receipt records collection-usage snapshots and raw per-sample/process measurements. It describes the observed measurement variation and does not assert an independently proven cause for that variation. |

The accepted source and binary hashes are in the adjacent receipts. Full modified
JDK/HotSpot, jtreg, Eclipse Tycho/GUI, other operating systems, mounted FUSE,
power-loss fault injection, and broad performance qualification remain outside
these local acceptance runs.
