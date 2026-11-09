# Bounded address gather: Windows source-bound evidence

The existing `M3LongAddressBoundedSelfTest` passes on the unchanged Java/JNI owners at `087e9bca5bd38a3606ac2ce1ced73bfb38916564`. Java-only mode completed 11,020,139 checks; the native-enabled main completed 21,975,732 checks and matched fingerprint `-8730037866241443045`. A separate required-native process with an empty library directory failed at the expected pre-corpus guard.

`ACTION.json` records the scoped existing-executor DAG and typed artifact bindings. `QUALIFICATION.json` explains its contract coverage. `SOURCE_PINS.json`, `TOOL_PINS.json` and `COMPILE_OUTPUTS.json` bind source/tool/artifact identity; `evidence/` contains small stdout logs normalized to LF and path-free native/process projections. Raw stdout byte hashes are retained in NATIVE_OBSERVATIONS.json. The fresh DLL was checked as PE x86-64 with one exact JNI export, then its exact fixture load and symbol resolution were observed under `-Xcheck:jni`.

The initial strict javac process succeeded with empty diagnostics and emitted 11 classes. Its plan incorrectly expected eight, so that attempt retained `FAILED_OR_HOLD`. The three additional nested classes are declared in the received source. A new continuation consumed the unchanged 11-class/two-header cohort and ran only the five previously unexecuted native phases; javac was not repeated. Both receipts remain distinguishable by hash.

This main checks offset and duplicate/scattered/contiguous gathers, chunk boundaries, exact prefix writes on bad physical indexes, backend-specific exceptions, range/alias/null cases and detached sorted/mask-selected projections. Native-enabled mode exercises both gather backends; projection filtering remains explicitly Java. It does not admit live filtered/sorted wrappers.

The separate 21-literal/40-profile prospective corpus remains unrun. Full module/JDK/jtreg/JCK, Windows UBSan, other collectors/platforms, UI integration and performance are unrun. Static tool pins are not a whole-OS or per-child loaded-image/cwd claim. No source/test changes, recipe replay or global phase promotion are included.

`EXECUTION_ARGV_PROJECTION.json` exposes all six observed argument arrays with typed installation/retained-input/output tokens, five source-to-repository mappings and explicit storage/environment controls. The original javac checkpoint and the five continuation jobs are already complete and must not be replayed. This data projection is not a registered generic executor; broader acceptance remains held.
