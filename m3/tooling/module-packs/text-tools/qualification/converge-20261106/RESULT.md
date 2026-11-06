<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3_TEXT_TOOLS_CONVERGE v1: com.synexia synexia-unix4j-m3 into text-tools (2026-11-06)

Three-way receive. **ours** is this pack on M3jdk21 master. **base** is the receiving pin in `SOURCE_REF.json` (com.synexia `3b5c4be7630f`); for `StructuredTextTools` it is donor `687f3425588a`, the version ported on 2026-10-15. **theirs** is com.synexia develop (2026-11-06). Before merging, both base and theirs are relocated the way the first receiver did:
- `com.synexia.unix4jm3` -> `jdk.m3.text`, except the structured tools, which go to `jdk.m3.structured`;
- `.io` and `.os` -> `jdk.m3.io`;
- `.nativecore` -> `jdk.m3.text`;
- the donor progress monitor -> `java.util.concurrent.ProgressMonitor`, with the `checkCancelled` spelling;
- JNI symbols and generated header names follow the same map, and the donor's duplicate com.synexia JNI exports are dropped.

| Outcome | Files |
|---|---|
| ADD (new since the pin) | 27: M3NativeAho, M3RegexLiteralPlan, M3RegexRequiredSegments, RegexGrepAtom, RegexRequiredSegmentsAtom, NativeTextAtom, NativeLineBatch, NativeUtf8Lines, M3NativeTextWindow, M3MaskIndexCursor, M3TextOperators, M3TextTool{Catalog,Request,Result}, M3Utf8PackedRows, M3Pipe, M3ProcessPipeline, NativeCommand, NativeProcessPipeline, NativePipelineResult, HostExecutable, SpoolingCapture, HeapCaptureBudget, native m3unixtext.c, m3unixaho.c, m3structured.c, m3_utf8_kmp.h |
| MERGED (clean 3-way) | 18, including StructuredTextTools (the donor's delimited native backend over M3's JPMS port) |
| SAME | 4 |
| UNION (both sides' appended code, duplicate blocks dropped) | TextTools (M3's native line ops plus the donor's `grepLiteral(ByteBuffer, …)`), m3textio.c (M3's literal extension plus the donor's UTF-8 line scans; duplicate C functions de-duplicated, M3's first) |
| OURS (equivalent, header/import differences only) | JsonLinesTools, StructuredCli |
| OURS + RETAINED | NativeUtf16Literal: M3's paged CharSequence kernel stays live, and the donor's String kernel is retained under `m3/qualification/master-landing-20261028/retained-variants/text-tools-20261106/` |
| HOLD (retained, not compiled) | M3TextToolExecutor: it needs both `jdk.m3.structured` and package-private `jdk.m3.text` members, so it fits neither module and needs a module decision |

Module and build changes:
- `jdk.m3.io` gains `requires java.management` (HeapCaptureBudget).
- `NativeProcessPipeline` gains an explicit private constructor, needed because the build uses `-Werror`.
- build.py compiles m3unixtext, m3unixaho and m3structured (with javac `-h` headers) and packages them into jdk.m3.text and jdk.m3.structured.

Gates (Windows, JDK 21.0.9, MinGW-w64 gcc; java.base patched with this repository's own `java.util.concurrent.Progress*`/`Monitored*` sources):
- javac `-Xlint:all,-module -Werror --module-source-path`: master 0 diagnostics, converged 0 diagnostics.
- gcc `-std=c11 -Wall -Wextra -Werror -shared`: all 5 native sources, 0 diagnostics.
- JNI_BIND: 16 native methods, 0 unbound (`JNI_BIND.tsv`). The 3 stale `jdk_internal_m3_unix4jm3` exports were already on master.
- build.py's own proofs, run with build.py's flags and modes, pass on both master and converged: M3NativeTextDifferential (`M3_NATIVE_TEXT_PARITY_PASS runs=450`), M3TextKernelRegression native and java, and M3OptionalNativeFallbackRegression line-load-denied, literal-property-denied and literal-load-denied.
- M3TextSmoke: every text-tool assertion passes up to its FIFO section, which is Linux-only.

Not run: build.py's jmod/jlink image (Linux with an M3 image), the FIFO part of M3TextSmoke, and the donor's 32 unit tests (next leaf).

## Donor harnesses (2026-11-06, follow-up)

`relocate_tests.py` receives the donor's `src/test` with the main-source map; each harness joins the package of the classes it exercises. The relocated sources and test natives are in `fixtures/src/test/{java,c,native}`.

Test-scope adaptations (also in `TESTS_RELOCATED.tsv`):
- CoreSelfTest drops 4 suite calls that cross into `jdk.m3.structured` or into the held executor; each of those harnesses runs as its own entry.
- Not received: NativeLineBatchOrderSelfTest (it uses the donor-only `ProgressState` API) and M3TextToolSelfTest (its owner is HOLD).

Correction to the first receive: `NativeProcessPipeline` gets an explicit **public** constructor. The private one added for `-Xlint:all -Werror` changed the API, and the donor's harness instantiates the class.

`gate4.sh` (Windows, JDK 21.0.9, MinGW gcc):
- javac: 0 errors.
- gcc `-Werror`: main natives plus the m3unixaho/m3unixtext wrong-ABI test libraries, 0 diagnostics.
- C self-test `m3_utf8_kmp_selftest`: rc=0.
- Harnesses (`HARNESS_RESULTS.tsv`): **31/31 pass**, each in the environment it asserts:
  - 24 runs with natives (both DelimitedIndexContract modes included);
  - 4 with no library on `java.library.path` (the Missing/NoNative contracts and NativeUtf16LiteralSelfTest in Java mode);
  - 3 with the wrong-ABI libraries in place of m3unixaho/m3unixtext.
