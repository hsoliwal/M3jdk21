<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Executed qualification: Swift deque span copy

The pinned Swift repository is `apple/swift-collections` at
`b905c889ff75059b8ef0bde480fe55948147b1e6`. Eleven file identities and eight category
decisions are recorded. One private deque copy atom is adapted; seven other donor categories
have explicit incumbent/pending/rejected dispositions. Swift's own toolchain/tests were not run.

The exact target predecessor is `7c6b3b51094c50c84746b819d79665e5f003bbdf`.
Local evidence was produced with the recipe output in a dirty worktree at that predecessor,
not a hosted run at the subsequently published commit. TARGET_PROOF.json binds every
production/test/native source byte. RECEIVER.json in the target binds the published recipe.

- Java 21 warnings-as-errors compile of all 134 production files: PASS.
- Frozen baseline and candidate: five focused JUnit tests each pass, no skips/failures/errors.
  The new oracle exercises 5,560 export layouts and 1,008 front/back growth layouts; the three
  existing deque tests also cover mutation, callback failure, streams and exceptional behavior.
- Public/protected descriptors and retained field declarations compare byte-for-byte equal.
- Real OpenRewrite: four tests pass, including named composition, exact LST/text outputs,
  second-pass fixed point, serial/reverse/width-two repeat, missing/drift/occupied/duplicate
  refusal and opaque code-shaped Markdown. The extensionless NOTICE refusal is retained as
  a regression; the permitted NOTICE.txt output is used without weakening the text owner.
- Complete Maven and native build: results below come from the executed owning runner.

```json
{
  "java_sources": 134,
  "test_sources": 162,
  "junit": {
    "java": {
      "tests": 238,
      "failures": 0,
      "errors": 0,
      "skipped": 2
    },
    "native": {
      "tests": 238,
      "failures": 0,
      "errors": 0,
      "skipped": 0
    }
  },
  "native_libraries": [
    "m3_collections_address",
    "m3_collections_mask",
    "m3_collections_pathmask",
    "m3_collections_span",
    "m3_collections_statehist",
    "m3_residency_state_mask",
    "m3bitlane",
    "m3rankbatch",
    "m3signalorder"
  ],
  "jar_classes": 321,
  "runtime_modules": "java.base"
}
```

Nine source-built test dependencies passed source/license/JAR checks. Nine production JNI
libraries compiled against generated headers with strict C17 and fatal UBSan. All 17 explicit
native contracts, the separate existing bitmap bridge fixture, and missing-library refusal ran.
The required-native Maven suite runs with `-Xcheck:jni`; no native method or buffer owner was
added for the deque. All native product source hashes match the predecessor.

## Resource diagnostic and limits

Three separate JVMs per variant use a 256 MiB heap, two active processors and NMT summary.
The harness records first-call wall time per case, warmed wall/CPU time, thread allocation, post-GC heap,
NMT totals and process peak RSS. Raw logs and commands remain in the canonical evidence packet.
It measures toArray, not growth latency. The shared host and small non-JMH experiment make
timing exploratory. First-call results are mixed (later cases may reuse already compiled code); no universal speedup or cross-platform bound is claimed.
The measured harness is retained with its exact source and commands.

| Size / head | Baseline warmed CPU ns | Candidate warmed CPU ns | Allocated bytes per copy (both) |
| --- | ---: | ---: | ---: |
| 0 / 0 | 63.48 | 43.66 | 16.00 |
| 0 / 4 | 55.74 | 25.06 | 16.00 |
| 8 / 0 | 226.87 | 36.79 | 80.00 |
| 8 / 4 | 141.26 | 87.90 | 80.00 |
| 64 / 0 | 253.21 | 239.99 | 528.00 |
| 64 / 32 | 255.77 | 150.10 | 528.00 |
| 1024 / 0 | 5519.60 | 2180.09 | 8208.00 |
| 1024 / 512 | 3664.65 | 2410.07 | 8208.00 |
| 65536 / 0 | 240583.77 | 90362.52 | 524304.00 |
| 65536 / 32768 | 189149.05 | 71058.08 | 524304.00 |

The retained-layout check is separate from noisy process totals: identical instance fields,
one same-capacity long[] owner, and no temporary array in the private helper. Both variants
allocate the same mandatory export array. NMT/RSS include VM/compiler/GC overhead and do not
establish atom-specific native savings. Two contiguous copies still perform O(n) work.

## Reproduction and outstanding gates

The target's existing command remains:

```sh
JAVA_HOME=/path/to/jdk21 MVN=/path/to/mvn python3 m3/collections/qualification/full-java-jni-20261018/verify.py
```

The canonical crate's Maven POM runs the real shared recipe owners; set `parser.classpath`
to the compiled predecessor module plus the pinned JUnit console. The crate's exact templates,
preimages and receipts provide deterministic replay. No framework or Java/JNI runtime
dependency on Swift or Synexia is introduced.

Hosted exact-head CI, full JDK jtreg/VM/platform promotion and the remaining donor categories
are distinct gates. A queued run proves no execution. This evidence does not prove the
19,396 generated-template/JAR assertion or repository-wide migration completion.
