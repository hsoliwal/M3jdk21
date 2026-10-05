<!-- SPDX-License-Identifier: Apache-2.0 -->
# Image qualification follow-up

This packet resolves observed CI failures at head
`02ab933680f98ca28b10c151cf61e9fe24cb9200`, tested as merge
`a823bf74247b7cf909a81960ebe4ec78ee5e43d6`. Both complete Linux x64 fastdebug
images built successfully. Their recipe/UBSan/custody jobs also passed.

The retained artifacts show normal C1/C2 compilation and behavioral parity:

| Port | C1 kernel nmethods | C2 kernel nmethods | Image behavior |
| --- | ---: | ---: | --- |
| M3BitLane28 | 17 | 18 | Interpreter, mixed, C1, C2; 64,797 checks and 317 native calls per run |
| M3TQ | 10 | 20 | Same four modes; 103,435 checks, 4,458 independent JNI oracle calls and zero warm payload reads per run |

Both M3-enabled C1 requests then passed behavior in **interpreted mode**. The
VM's existing `UseM3StringStorage` guard deliberately disables compilation.
The old image checker incorrectly required nmethods in that mode and stopped
both workflows. M3 C2 requests were not reached. These are not completed
seven-mode, whole-program, jtreg/TCK or M3-enabled JIT results.

The new checker parses complete HotSpot XML. Normal C1/C2 require the exact
compiler and kernel class. M3 requests require the flag, requested compiler,
actual interpreted VM mode and no nmethods. Missing evidence remains failure.
No HotSpot guard is removed. Compiler/intrinsic/GC/deoptimization support for
the alternate String representation remains separate JVM work. All canonical
mapping records, states and 20 admission gates remain unchanged, including the
open compiled-M3 obligations.

The same packet repairs two malformed backslash character literals in A3
recipe tests, and a malformed quoted Java-code specimen in `A3RegexMatrix` and
its generating template. The existing template manifest hash advances with
those bytes. The backing workflow now builds a complete image with dependency
prerequisites and uses the repository's existing JTReg setup action. The old
`-only` target skipped required build tools; the previous configure log also
confirmed that JTReg was absent.

## Recipe and proof

The existing text snapshot engine produces **29 exact outputs**. This includes
changed scripts, workflows, source/template pairs and the producing crates'
manifests/plans. Previous controls are retained as exact preimages in this
packet. Existing receipts remain historical, unchanged evidence. Recipe engines
and coverage thresholds are unchanged. No direct product source-edit lane is
introduced.

```sh
mvn -B -ntp -f m3/tooling/image-gates/pom.xml verify
python3 m3/tooling/image-gates/verify-plan.py
mvn -B -ntp -f m3/tooling/ci-repair/pom.xml verify
mvn -B -ntp -f m3/tooling/tq/pom.xml verify
mvn -B -ntp -f m3/tooling/lane28/pom.xml verify
```

Five packet tests include compilation/execution of the generated 16-by-17 A3
regex matrix. Eleven checker tests exercise captured logs and rejection cases;
all six original complete CI logs were checked locally as well. The affected
A3 recipe tests compile the module's 34 main and 60 test sources and pass four
selected tests. This is not a full reactor or its 99% coverage result. CI retains
that full reactor invocation, plus rebuilt-image and jtreg checks.

`source-evidence.json` records artifact identities and original log hashes. Six
small XML fixtures retain the original VM metadata and first matching kernel
nmethod (if any); they are explicitly reduced fixtures, not entire logs.
`feedback/` contains recipe-generated updates for the Synexia source owners.
No new speed claim is made from these correctness runs.
