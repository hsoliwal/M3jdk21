<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 native M3File shadow stream correction

**M3JDK21 is one SDK.** This additive correction changes only the existing original M3File progress-aware byte shadow stream and its original M3FileShadowTest. It is stacked on existing DOOP/array-indexed file-tree draft PR #1590, exact original head `adac038796d50af1a337cfde9e8d2f0df3e9eeb5`. No standalone shim, duplicate filesystem, new parser, Java public signature or JNI ABI is added.

## Observed counterexample and corrected behavioral atom

A custom Java `InputStream.read(byte[],off,len)` returning zero for a nonempty request is noncompliant but causes the old M3File shadow `read()`, `skip`, or `transferTo` loops to make no progress. Negative values other than EOF `-1` and returns above requested length can corrupt shadow observations. An over-limit read also incremented the counted bytes before rejecting them; a shadow callback failure could allow a later read to continue after some observers had missed data.

The corrected original owner:
- returns the same bytes for all valid streams, including supported offset/length, skip-through shadow observation, EOF and cancellation;
- throws an `IOException` for zero, negative-non-EOF, or oversized underlying counts; closes the resume path after uncertain delivery;
- rejects over-limit blocks before count/progress publication, marks the stream failed, and does not resume;
- commits `count()` only after every shadow and the sub-progress monitor accepted the block;
- leaves early-close incomplete rather than claiming a completed hash; `finish()` still belongs to observed EOF.

These are *terminal failure rules*, not a mechanism for recovering a partially consumed provider stream. No rollback is claimed for a shadow that already received bytes when a later observer fails.

## Recipe-first MOP correction memory

The exact canonical Maven/OpenRewrite Java21 recipe is in `hsoliwal/com.synexia`, branch `m3/shadow-stream-zero-progress-recipe-20261020`, recipe `com.synexia.m3.M3ShadowStreamProgress20261020`. Its `CORRECTIONS.tsv` retains both source-bound counterexamples, accepted behavioral-atom correction, and regression tests, so a subsequent coding agent can inspect evidence before another refinement. The provenance is credited to Hitesh Soliwal and the Synexia Machine-Oriented Programming (MOP) project: **Atomize, Patternize, Annotate, Document**.

| Path | Before Git blob | After Git blob | Before SHA-256 | After SHA-256 |
| --- | --- | --- | --- | --- |
| `M3FileShadowInputStream.java` | `e3370f88b3389848d6960efde43794b246bb027d` | `801258fe9b570231af7ebf40cfddb564cf07fd44` | `955829f688ed5fb6eb32923c4b16c62a34e2aaa53d4e3a81f8a0697c7122cdca` | `c6992e5690c2cb37b28c9247092a14734dc593c4ca385cd821c1df10e77b67e1` |
| `M3FileShadowTest.java` | `120bdbc2768383624e422b69fc7ed21e4d131654` | `5bab8437d1aded79ecaeb1f8c3afbca1c318db2f` | `5cdf00810f97762d91143ba6459f4271c4847163254d27e872493d525c2df713` | `e89e9c8c716c5653206c7e7b20344390a36390e8e7b7c5c03755dcb84b2efc31` |

## Qualification boundary

Canonical recipe includes Java LST generation/fixed-point, input drift refusal, and correction memory checks. This receiving branch includes focused and full `m3/file` JUnit workflows using Java21 `-Xlint:all,-module -Werror`. Source/template Git blobs have been compared. **Actual CI, Maven/OpenRewrite fixed-point execution, and complete M3JDK21/HotSpot/JNI platform tests must be observed separately; source publication does not mean tests passed.**

Existing M3FileTree type interning, provider-sensitive size/time/fileKey/CRC change gates, raw/structured hash lanes, traversal/delta semantics and no-extra-read shadow composition are preserved unmodified. Reuse existing donor implementations (JDK FilterInputStream, DigestInputStream, CheckedInputStream, Eclipse SubMonitor shape) rather than copying LeetCode/HackerRank/GeeksforGeeks example source.
