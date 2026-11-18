<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->

# M3JDK21 — source-custody gate for optional LLVM 24 ORC compiler backend

This **qualification-only** gate belongs to the *existing* M3JDK21
`m3/native` and `m3/compiler` owners. It adds no second compiler,
JNI runtime, Java language API, CMake target, Maven plugin, or HotSpot
replacement. The original `hsoliwal/llvm-project` fork owns LLVM IR,
code generation, C API headers, ORC LLJIT, native libraries and their
Apache-2.0 WITH LLVM-exception notices.

The first receiver pins:

- Original LLVM fork source `a75b9f54608bf0ccc5a2f2bbf0af4814df4419c7`,
  including original LLVM-C headers, ORC C++ ownership implementation,
  JNI Java/C/CMake owners and the existing **complete 30-file SHA-256
  source inventory**. All 14 key LLVM Git blobs are compared with checked-out
  working bytes and their recorded Git objects.
- Original M3JDK21 compiler/native owners at
  `0aa6625cfdd9c5196e90c652b6ea423aa81017a2`:
  `m3/compiler/pom.xml`, `m3/native/CMakeLists.txt` and its README.
  Their original Git blob IDs must match the receiving checkout. Additional
  qualifications may be committed to a branch without modifying those owners.

The guard checks ordinary Git blob identities, forbids symlink/path drift,
and reexecutes `sha256sum -c SOURCE_PINS.sha256` against the **full original
LLVM JNI subtree**, including all 30 original/new files. Source admission
returns `SOURCE_CUSTODY_PASS_ONLY` and `m3jdk21_product_receiving=HOLD`.
No native archive is downloaded or accepted, and no source is copied into
M3JDK21 from this gate.

## Execute source-only gate

Run from a sibling checkout with the exact pinned LLVM source:

```sh
python3 m3/native/qualification/llvm24-orc-20261118/verify_source.py \
  --m3-root /path/to/M3jdk21 --llvm-root /path/to/llvm-project
```

The repository's independent hosted workflow uses immutable LLVM source
and shallow/sparse checkouts, runs the seven hermetic source tampering tests,
then the same real Git/SHA gate. It additionally compiles the original Java21
LLVM types and executes the existing no-native loader refusal. This is a
Java source compatibility check **without** an LLVM native runtime.

Executed local synthetic-source tests: **7/7 PASS** under normal Python
and `python3 -O`, including source mutation, symlink, hash-manifest drift,
unexpected LLVM revision, changed M3 compiler owner and noncanonical paths.
Synthetic tests are not evidence of genuine LLVM24 or native interoperability.

## CPU dispatch and product admission

Actual LLVM19 host measurements for 65,536-element ADD/SUBTRACT/MULTIPLY
with one ORC JIT per call showed 41–48x higher latency than Java for those
simple operations. **Keep Java/HotSpot as the default.** LLVM ORC is currently
an explicit JNI correctness backend, not automatic optimization for M3
collections, streams or array masks. A reusable bounded JIT session requires
independent lifetime, cost, ABI and performance qualification.

**Hard HOLD until** original source-built LLVM24 JIT/GC/fault CI passes,
Synexia's canonical OpenRewrite source replay/refusal actually executes for
Java **and C**, M3JDK21 Java/native/GC and source/binary/API compatibility
passes, and benchmark evidence supports the intended workload. These
conditions are not satisfied by a source-only receipt or a private recipe
merge. No production API or Maven/Gradle plugin is modified by this work.
