<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# M3 Image Architecture Contract

## Invariants

1. **Java 21 compatibility is the semantic spine.** The default image executes ordinary class files
   on an M3JDK21/Java 21 runtime. No public API, bytecode, JNI symbol, class-loader, reflection,
   serialization, exception, security, or concurrency behavior is silently removed.
2. **Training adds evidence, never permission.** Observed behavior raises confidence and priority;
   it cannot prove an unobserved path impossible.
3. **Static analysis is monotone.** Adding roots, profile observations, or dynamic metadata may only
   expand the admitted world.
4. **Hybrid fallback is explicit.** Every non-candidate application class remains packaged as
   bytecode unless `CLOSED_WORLD` is selected; that mode packages only reachable/rooted classes and
   refuses signed-archive rewriting.
5. **Dynamic constraints fail closed in closed-world mode.** Reflection, JNI, service loading,
   proxies, resources, method handles, and unresolved dynamic invocation require explicit roots or
   metadata.
6. **Build identity is content-addressed.** Input bytes, normalized profile, plan, selected backend,
   and JDK identity determine reuse. File modification times do not.
7. **No hidden source loading.** Analysis reads class-file bytes; it does not initialize application
   classes or execute application code.
8. **Receipts distinguish attempt from result.** `PASS`, `FALLBACK`, `NOT_RUN`, and `FAIL` are never
   collapsed.
9. **Classpath precedence is locked.** Packaged inputs and launchers preserve the original input
   sequence; wildcard directory enumeration is not a semantic authority.
10. **Training and plans are complete manifests.** Missing, additional, duplicated, reordered,
    symbolic, or hash-drifted evidence is refused before build.
11. **Reachability kernels are semantically interchangeable.** The Java kernel is the reference; the
    optional JNI kernel consumes the same validated CSR arrays and must return the same sorted closure.
12. **Explicit native requests fail closed.** Absence of a native-library property selects Java; an
    invalid configured path, load failure, malformed ABI input, or parity failure is not silently ignored.

## Components

| Owner | Purpose |
| --- | --- |
| `M3ImageTool` | CLI and ordered stage coordinator |
| `M3Trainer` | class-list and JFR training execution |
| `M3JfrProfileReader` | normalized startup/hotness profile |
| `M3ClassPathScanner` | deterministic JAR/directory universe |
| `M3ClassFileParser` | dependency and dynamic-feature extraction |
| `M3ReachabilityAnalyzer` | monotone class-granular closure |
| `M3ReachabilityGraph` | canonical class-name IDs and CSR dependency graph |
| `M3JavaReachabilityKernel` | portable primitive-array/bitset BFS reference |
| `M3JniReachabilityKernel` | explicit-path JNI implementation with no retained heap pointers |
| `M3ModuleResolver` | `jdeps` module closure |
| `M3ImagePlanner` | immutable semantic plan |
| `M3Jdk21ShiftedBackend` | `jlink` + AppCDS + Java launcher |
| `M3NativeImageBackend` | isolated external native-image adapter |
| `M3IncrementalStore` | verified content-addressed reuse |
| `M3ImageVerifier` | complete image hash and runtime proof |

## Dynamic-feature policy

`META-INF/services` provider names become explicit reachability roots. The class-file scanner emits
a conservative feature whenever a class contains a native method, an unknown `invokedynamic` or
dynamic-constant bootstrap, or a constant-pool method reference to reflection, method handles, proxy
creation, service loading, resource lookup, dynamic class loading, or native-library loading. The
parser resolves the standard Java 21 bootstrap owners used by lambda metafactories, string
concatenation, records, switches, and constant bootstraps without executing application code. A
feature is resolved only when its owning class is reachable and the supplied dynamic roots/metadata
cover the runtime target. `HYBRID` retains bytecode fallback; `CLOSED_WORLD` refuses unresolved
features. Java binary-name validation is Unicode-aware and follows Java identifier code-point rules.

## Reachability atom

The atom is categorized as directed unweighted transitive closure. Class names are sorted once, mapped to
stable integer IDs, and dependencies are deduplicated into CSR rows. Java and JNI both use a bounded integer
queue and a packed visited bitset, yielding `O(V + E)` time and `O(V)` closure workspace. Output is scanned
in vertex order, so the closure is sorted and deterministic regardless of traversal order.

LeetCode 1971, HackerRank BFS: Shortest Reach, and GeeksforGeeks BFS traversal are catalogue references for
problem classification and adversarial cases only. No implementation source from those sites is imported.
The JNI ABI is machine-recorded in `contract/JNI_ABI.tsv`; native array views are released with `JNI_ABORT`
before return, and all native allocations are freed within the call.

## Backend boundary

`M3ImageBackend` receives an immutable `M3ImagePlan`; it may not modify plan files. Every backend
writes into a fresh temporary directory, produces `image-manifest.tsv`, verifies it, then atomically
publishes the result. Backend-specific receipts state exactly what was executed.
