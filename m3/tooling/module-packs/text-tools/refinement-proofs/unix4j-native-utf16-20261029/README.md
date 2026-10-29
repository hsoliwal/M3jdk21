<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 product receiving — Unix4j native UTF-16 / JNI

Canonical Maven/OpenRewrite recipe, donor decisions, complete source-seals
and replay proofs are exclusively in hsoliwal/com.synexia, draft PR #13815.
This receiver contains **only M3JDK21 product code and product tests**, not
transformation recipes or donor experiment projects.

Changes: a package-local bounded 8192-unit UTF-16 native KMP JNI kernel,
direct CharSequence/M3StringPiece-compatible text membership via the existing
TextTools public owner, indexed long[] line selection masks, and compiled C17
symbols in the existing m3textio native library, preserving mkfifo JNI exports.
The public JDK java.lang.String and existing Unix4j upstream APIs are unchanged.

Proof: source Java 21 & C17 (UBSan), loaded JNI -Xcheck:jni and pure Java fallback,
12,076 standalone String.contains oracle cases each in the canonical and
jdk.m3.text package namespaces. The existing smoke now asserts line framing
and nonflattenable CharSequence direct access.

PENDING: exact Maven/OpenRewrite SDK run and fixed-point refusal, full text JMOD
build and installed smoke, complete M3JDK21/jtreg, actual Unix4j 0.7 native
command/option parity, cross-platform and benchmarks. DRAFT ONLY.

## Second native refinement, 29 October 2026

The same first-party KMP kernel now compiles the UTF-16 needle and its
primitive int[] failure table **once per grepLiteral invocation**. An immutable
Compiled plan is shared across all line probes, while scan state and bounded
8192-unit page arrays stay invocation-local. The open donor Unix4j API and
existing JNI C symbols are unchanged. This second source-sealed refinement
lives only in Synexia and must pass its own fixed-point/replay gates.

Independent Java 21/C17 -Xcheck:jni differential: 12,076 tests in each of
Synexia Java, Synexia JNI, JDK text Java and JDK text JNI configurations.
A test also reuses each compiled plan twice, including NUL, surrogate halves
and multi-page UTF-16 text; the count is distinct inputs, not replays.
Full Maven, product JMOD, jtreg, upstream command parity and benchmarks
are still NOT_RUN. Keep draft.
