<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 JNI declaration projection v1 (2026-11-01)

1. Opt-in `M3AtomCensus --jni` emits legacy TSV, semantic TSV, and additive `JNI_DECLARATIONS_<repo>.tsv`; default and `--semantic` byte outputs do not change.
2. The same `JavacTask.parse()` and `TreePathScanner` used by the semantic Java census must identify Java `native` declarations. No second parsing of Java or native C/C++ sources.
3. Every JNI candidate row references the exact existing semantic JAVA_NATIVE_DECLARATION atom ID and source slice SHA-256; no fabricated source or admission status.
4. For a named top-level/member class with a single declaration of its native method name, compute the JNI short lookup symbol per the JNI source-name mangling rule. For method-name overloads (including ordinary Java overloads) emit `OVERLOAD_REQUIRES_JAVAC_H`, never pretend a short JNI symbol proves the native ABI. Unresolvable local and anonymous class binaries are `UNRESOLVED_LOCAL_CLASS` and `UNRESOLVED_ANONYMOUS_CLASS`, with no computed short symbol.
5. All rows remain `CANDIDATE_ONLY`, not proof of a native export, runtime registration, JNI descriptor equivalence, or contract promotion. Source-hash/Git custody, generated `javac -h` JNI header, native C17 export, and Java21 `-Xcheck:jni` execution independently corroborate the bounded fixture.
6. A compiler diagnostic, nonregular/symlink Java source, duplicate atom ID, or malformed source still refuses before output publication. Baseline Java public API, product JNI ABI, and default legacy census are unchanged.
7. Preserve `master` and `develop` history. Recipe documentation and SHA-256 file seals precede any receiver PR. Published changes remain draft/HOLD until actual Maven/OpenRewrite first apply, fixed point, negative source drift, and full product JDK21 gate.
8. Donor categories: this is JNI source identity binding, not a LeetCode/HackerRank/GfG algorithm substitution. Their implementations are not imported; future optimization candidates must join an already-reviewed source/license/category catalogue before any atom replacement.
