# Actual Java 21 frontend runtime qualification

This test-only module exercises the source-qualified frontend and compiler patch
through real `JavacTask` invocations. The recorded run passed **25 tests with no
failures, errors, or skips** on Eclipse Temurin 21.0.2+13, Linux x86_64.
Test sources also passed Java 21 compilation with `-Xlint:all -Werror`.

The complete case inventory, exact source and artifact hashes, and limits of the
qualification are in [evidence/QUALIFICATION.json](evidence/QUALIFICATION.json).
The original Maven output and four Surefire summaries are retained beside it.

## Contract coverage

| Test class | Tests | Contract exercised |
| --- | ---: | --- |
| `M3FileAndContextTest` | 6 | Immutable characters and original bytes, raw UTF16 preservation, bounded strict decoding, actual javac client wrappers, admitted input identity, and compiler context isolation. |
| `M3JavacASTIndexTest` | 10 | Lazy capture, shared consumers, the actual attributed compiler trees, PARSE/ANALYZE/GENERATE phases, callback and thread lifetime, failure cleanup, partial invocation close, and durable projection reload. |
| `M3ASTCodecTest` | 4 | Exact source and structure roundtrip, legacy raw UTF16 digest, atom-owner identity, checksummed malformed records, and independent decoding bounds. |
| `M3ASTCompilerBridgeTest` | 5 | Task-scoped observer ownership, actual release and diagnostic authority, annotation-generated parser input, and plugin success/error receipts. |

The cache integration test uses the actual qualified `SqliteSwapStore` JNI
library with a one-entry resident budget. It captures two files through two
consumers, evicts the first projection, then reloads it from SQLite after the
compiler callback ends. The remaining cache concurrency, pressure, immutable
store, failure, and complete JNI ABI tests belong to the adjacent cache crate.

## Reproduce the runtime gate

First qualify and build the frontend candidates using the frontend crate's
documented SDK workflow, and build the adjacent cache crate's native proof.
From the frontend crate directory, use the approved Java 21 toolchain and Maven
settings for your environment:

```sh
mvn -B -ntp -f runtime-tests/pom.xml test
```

The runner defaults to the qualified local artifacts
`target/m3-ast-sharing.jar`, `target/m3-javac-patch.jar`, and
`../cache/target/native/libsynexia_mindex_swap.so`. Alternative qualified paths
can be supplied using `m3.frontend.candidate.jar`, `m3.frontend.patch.jar`, and
`m3.cache.native`. Missing compiler hooks or the native library fail the gate.

This proof POM's local candidate dependency identifies the source-built artifact
under test. It does not introduce a runtime Maven dependency into the product.
The candidate contains canonical frontend classes for direct contract testing
and the mapped `com.m3.compiletime.ast` facade used by the plugin service.

## Retained refusal and qualification limits

The original Java compiler rejection of the cache loader method reference is
preserved under
`evidence/counterexamples/acquire-syntax-loader-arity/`. The corrected frontend
candidate was regenerated through the actual SDK recipe before this runtime
suite was run.

These finite tests establish the recorded contracts for this platform and
toolchain. They make no whole-JDK, all-platform, throughput, wall-clock, or
heap/RSS claim. Cache residency uses declared projection payload weight; live
javac objects, frozen source custody, caller-owned values, codec scratch space,
and native database memory have separate lifetimes. The plugin receipts retain
`crossProcessSemanticReuse: false`.
