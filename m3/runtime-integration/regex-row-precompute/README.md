# M3JDK21 — owner-local regex candidate row bitmap receiver

This is the M3JDK21 product receiving side of the **Synexia-first** Maven/OpenRewrite source recipe `com.synexia.m3.Jdk21RegexRowMaskPrecompute20261016` in [Synexia PR #11909](https://github.com/hsoliwal/com.synexia/pull/11909). Exact SHA256 target postimages were checked before receiving. Neither JDK branch history nor existing user String ABI was modified.

The new internal `java.lang.M3StringRegexRowPrecompute` takes a snapshot of canonical immutable `M3String[]` descriptor references and reuses `M3StringFacts.bitSignal64` and `M3StringFacts.codeUnitSignal` to build optional 64-column primitive posting masks for repeated `Pattern.matches` queries. Candidate masks are necessary-only; final verification uses the JDK Matcher. Only zero-flags ASCII literal patterns with optional edge `.*` are specialized, with all other syntax reverting to Pattern. Pattern.compile validation and matched input order are retained. Regex plans use exclusive mutable Matcher contexts and bounded heap-pressure-aware precompute.

No `String.java`, M3String owner/coordinate layout, JNI ABI, regex Pattern/Matcher class, HotSpot, compiled bytecode lowering or native payload store changes are included. No Synexia runtime dependency.

## Qualified local proof
Java21 `--patch-module java.base` compilation and independent stock-JDK reflective oracle passed **10,034 prepared regexes and 637,128 assertions** over 532 mixed inputs, flags, literal shapes, Unicode surrogate edge cases and concurrency. The checked-in `verify.py` binds the exact target source SHA256 and repeats that isolated Java21 execution with first-party proof stubs, without needing a full JDK build.

## Mandatory gate still open
Actual source-built M3JDK21/HotSpot `java.base`, jtreg under both CompactStrings modes and `-XX:+UseM3StringStorage`, GC/native lifetime, JNI boundary, JDK regex conformance and observed CPU/memory benefit have **not** all run. Keep draft, do not auto-merge; this class is an additive internal capability and not an automatic String.matches replacement. The broader 26-shape representation catalogue and filtered/sorted collection wrappers remain separate source stacks.
